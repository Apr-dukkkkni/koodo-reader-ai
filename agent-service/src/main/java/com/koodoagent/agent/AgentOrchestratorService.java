package com.koodoagent.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.koodoagent.config.AgentProperties;
import com.koodoagent.dto.*;
import com.koodoagent.exception.AgentException;
import com.koodoagent.llm.DeepSeekClient;
import com.koodoagent.llm.DeepSeekResult;
import com.koodoagent.llm.PromptBuilder;
import com.koodoagent.llm.QueryRewriteService;
import com.koodoagent.llm.dto.LlmRawAnswer;
import com.koodoagent.memory.MemoryService;
import com.koodoagent.memory.ProfileService;
import com.koodoagent.persistence.QaHistoryRepository;
import com.koodoagent.retrieval.WebSearchService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;


import java.util.*;

@Service
@RequiredArgsConstructor
public class AgentOrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(AgentOrchestratorService.class);

    private final QueryRouterService queryRouterService;
    private final WebSearchService webSearchService;
    private final PromptBuilder promptBuilder;
    private final DeepSeekClient deepSeekClient;
    private final OutputValidator outputValidator;
    private final QaHistoryRepository qaHistoryRepository;
    private final ObjectMapper objectMapper;
    private final MemoryService memoryService;
    private final EvidenceJudgerService evidenceJudgerService;
    private final QueryRewriteService queryRewriteService;
    private final AgentProperties agentProperties;
    private final ProfileService profileService;




    public AgentAnswer ask(AskRequest request) {

        AgentContext ctx = new AgentContext(agentProperties);
        String qaId = UUID.randomUUID().toString();
        long start = System.currentTimeMillis();

        RouteDecisionDTO decision = queryRouterService.route(
                request.term(),
                request.question(),
                request.context()
        );

        AgentRoute effective = degradeIfNeeded(decision.route());

        log.info("qaId={} route={} effective={} reason={}",
                qaId, decision.route(), effective, decision.reason());

        return switch (effective) {
            case DIRECT -> handleDirect(qaId, request, decision, start, ctx);
            case MEMORY -> handleMemory(qaId, request, decision, start, ctx);
            case WEB    -> handleWeb(qaId, request, decision, start, ctx);
            default     -> throw new IllegalStateException("unreachable route: " + effective);
        };
    }

    /**
     * 尚未实现的路由暂时降级：
     * - MEMORY / BOOK_RAG → DIRECT（Day 3 / Day 6 补）
     * - HYBRID            → WEB   （Day 6 补）
     */
    private AgentRoute degradeIfNeeded(AgentRoute route) {
        return switch (route) {
            case BOOK_RAG -> AgentRoute.DIRECT;
            case HYBRID -> AgentRoute.WEB;
            default -> route;
        };
    }

    // ---------------------------------------------------------------------
    // DIRECT
    // ---------------------------------------------------------------------

    private AgentAnswer handleDirect(String qaId, AskRequest request,
                                     RouteDecisionDTO decision, long start,
                                     AgentContext ctx) {
        int familiarity = profileService.getFamiliarity(request.term());
        String systemPrompt = promptBuilder.buildSystemPrompt(familiarity);
        String userPrompt = promptBuilder.buildDirectUserPrompt(
                request.term(),
                request.question(),
                request.context()
        );

        DeepSeekResult llmResult = deepSeekClient.chat(systemPrompt, userPrompt);
        LlmRawAnswer raw = parseLlmAnswer(llmResult.content());

        long latencyMs = System.currentTimeMillis() - start;

        AgentAnswer answer = new AgentAnswer(
                qaId,
                AgentRoute.DIRECT,
                request.term(),
                raw.oneLine(),
                raw.explanation(),
                raw.keyPoint(),
                List.of(),
                List.of(),
                EvidenceLevel.LOW,
                new UsageDTO(llmResult.inputTokens(), llmResult.outputTokens(), latencyMs)
        );

        qaHistoryRepository.save(request, answer);
        return answer;
    }

    // ---------------------------------------------------------------------
    // WEB
    // ---------------------------------------------------------------------


    private AgentAnswer handleWeb(String qaId, AskRequest request,
                                  RouteDecisionDTO decision, long start,
                                  AgentContext ctx) {
        String query = buildSearchQuery(request.term(), request.question());
        List<Evidence> evidences = webSearchService.searchAsEvidence(query);

        ctx.recordWebSearch();   // 第一次搜索

        if (!evidenceJudgerService.isEnough(evidences) && ctx.canWebSearch()) {

            String rewritten = queryRewriteService.rewrite(request.term(), request.question());
            if (rewritten != null && !rewritten.isBlank()
                    && !rewritten.equals(query)) {
                log.info("qaId={} 证据不足，触发二次搜索: '{}' -> '{}'",
                        qaId, query, rewritten);
                List<Evidence> second = webSearchService.searchAsEvidence(rewritten);
                ctx.recordWebSearch();
            } else {
                log.info("qaId={} 证据不足但改写结果无效，跳过二次搜索", qaId);
            }
        }

        // ↓ 以下保持 Day 2 逻辑不变，从 systemPrompt 到 qaHistoryRepository.save
        int familiarity = profileService.getFamiliarity(request.term());
        String systemPrompt = promptBuilder.buildSystemPrompt(familiarity);
        String userPrompt = promptBuilder.buildUserPrompt(
                request.term(), request.question(), request.context(), evidences);

        DeepSeekResult llmResult = deepSeekClient.chat(systemPrompt, userPrompt);

        LlmRawAnswer raw;
        try {
            raw = parseLlmAnswer(llmResult.content());
        } catch (AgentException firstFailure) {
            String retryPrompt = userPrompt
                    + "\n\n[CRITICAL] 上一次输出无法解析为 JSON，错误："
                    + firstFailure.getMessage()
                    + "\n请只输出合法 JSON，不要任何 Markdown 包裹。";
            DeepSeekResult retryResult = deepSeekClient.chat(systemPrompt, retryPrompt);
            try {
                raw = parseLlmAnswer(retryResult.content());
            } catch (AgentException secondFailure) {
                throw new AgentException("A002", "模型输出两次均无法解析为 JSON");
            }
            llmResult = retryResult;
        }

        List<SourceDTO> sources = outputValidator.validateAndResolve(raw.sourceIds(), evidences);
        List<String> validatedSourceIds = sources.stream()
                .map(SourceDTO::sourceId)
                .toList();
        EvidenceLevel evidenceLevel = computeEvidenceLevel(sources);

        long latencyMs = System.currentTimeMillis() - start;

        AgentAnswer answer = new AgentAnswer(
                qaId,
                AgentRoute.WEB,
                request.term(),
                raw.oneLine(),
                raw.explanation(),
                raw.keyPoint(),
                validatedSourceIds,
                sources,
                evidenceLevel,
                new UsageDTO(llmResult.inputTokens(), llmResult.outputTokens(), latencyMs)
        );

        qaHistoryRepository.save(request, answer);
        return answer;
    }



    private List<Evidence> mergeEvidence(List<Evidence> first, List<Evidence> second) {
        List<Evidence> merged = new ArrayList<>(first);
        Set<String> seenUrls = new HashSet<>();
        for (Evidence e : first) {
            if (e.url() != null && !e.url().isBlank()) seenUrls.add(e.url());
        }
        for (Evidence e : second) {
            if (e.url() == null || e.url().isBlank()) {
                merged.add(e);
                continue;
            }
            if (seenUrls.add(e.url())) {
                merged.add(e);
            }
        }
        // 重新编号 id，保证 id 唯一
        List<Evidence> reindexed = new ArrayList<>();
        int idx = 1;
        for (Evidence e : merged) {
            reindexed.add(new Evidence(
                    "S" + idx,
                    e.type(),
                    e.title(),
                    e.content(),
                    e.url(),
                    e.domain(),
                    e.trustLevel()
            ));
            idx++;
        }
        return reindexed;
    }

    // ---------------------------------------------------------------------
    // 私有工具方法
    // ---------------------------------------------------------------------

    private String buildSearchQuery(String term, String question) {
        String t = term == null ? "" : term.trim();
        String q = question == null ? "" : question.trim();
        return (t + " " + q).trim();
    }

    private LlmRawAnswer parseLlmAnswer(String content) {
        try {
            String cleaned = cleanJson(content);
            return objectMapper.readValue(cleaned, LlmRawAnswer.class);
        } catch (Exception e) {
            throw new AgentException("A002", "模型输出无法解析为 JSON: " + e.getMessage());
        }
    }

    private String cleanJson(String content) {
        if (content == null) return "";
        String s = content.trim();
        if (s.startsWith("```json")) s = s.substring(7);
        else if (s.startsWith("```")) s = s.substring(3);
        if (s.endsWith("```")) s = s.substring(0, s.length() - 3);
        return s.trim();
    }

    private EvidenceLevel computeEvidenceLevel(List<SourceDTO> sources) {
        if (sources == null || sources.isEmpty()) return EvidenceLevel.LOW;
        if (sources.size() >= 2) return EvidenceLevel.HIGH;
        return EvidenceLevel.MEDIUM;
    }


    private AgentAnswer handleMemory(String qaId, AskRequest request,
                                     RouteDecisionDTO decision, long start,
                                     AgentContext ctx) {
        List<Evidence> memories = memoryService.searchAsEvidence(
                request.term(), request.question(), 3);

        int familiarity = profileService.getFamiliarity(request.term());
        String systemPrompt = promptBuilder.buildSystemPrompt(familiarity);
        String userPrompt = promptBuilder.buildMemoryUserPrompt(
                request.term(),
                request.question(),
                request.context(),
                memories
        );

        DeepSeekResult llmResult = deepSeekClient.chat(systemPrompt, userPrompt);

        LlmRawAnswer raw;
        try {
            raw = parseLlmAnswer(llmResult.content());
        } catch (AgentException firstFailure) {
            String retryPrompt = userPrompt
                    + "\n\n[CRITICAL] 上一次输出无法解析为 JSON，错误："
                    + firstFailure.getMessage()
                    + "\n请只输出合法 JSON，不要任何 Markdown 包裹。";
            DeepSeekResult retryResult = deepSeekClient.chat(systemPrompt, retryPrompt);
            try {
                raw = parseLlmAnswer(retryResult.content());
            } catch (AgentException secondFailure) {
                throw new AgentException("A002", "模型输出两次均无法解析为 JSON");
            }
            llmResult = retryResult;
        }

        List<SourceDTO> sources = outputValidator.validateAndResolve(raw.sourceIds(), memories);
        List<String> validatedSourceIds = sources.stream()
                .map(SourceDTO::sourceId)
                .toList();

        long latencyMs = System.currentTimeMillis() - start;

        AgentAnswer answer = new AgentAnswer(
                qaId,
                AgentRoute.MEMORY,
                request.term(),
                raw.oneLine(),
                raw.explanation(),
                raw.keyPoint(),
                validatedSourceIds,
                sources,
                sources.isEmpty() ? EvidenceLevel.LOW : EvidenceLevel.MEDIUM,
                new UsageDTO(llmResult.inputTokens(), llmResult.outputTokens(), latencyMs)
        );

        qaHistoryRepository.save(request, answer);
        return answer;
    }
}