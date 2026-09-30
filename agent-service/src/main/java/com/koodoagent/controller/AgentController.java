package com.koodoagent.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.koodoagent.agent.AgentRoute;
import com.koodoagent.agent.Evidence;
import com.koodoagent.agent.OutputValidator;
import com.koodoagent.dto.AgentAnswer;
import com.koodoagent.dto.AskRequest;
import com.koodoagent.dto.EvidenceLevel;
import com.koodoagent.dto.SourceDTO;
import com.koodoagent.dto.UsageDTO;
import com.koodoagent.exception.AgentException;
import com.koodoagent.llm.DeepSeekClient;
import com.koodoagent.llm.DeepSeekResult;
import com.koodoagent.llm.PromptBuilder;
import com.koodoagent.llm.dto.DeepSeekChatRequest;
import com.koodoagent.llm.dto.LlmRawAnswer;
import com.koodoagent.persistence.QaHistoryRepository;
import com.koodoagent.retrieval.WebSearchService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/agent")
public class AgentController {

    private final DeepSeekClient deepSeekClient;
    private final PromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;
    private final QaHistoryRepository qaHistoryRepository;
    private final WebSearchService webSearchService;
    private final OutputValidator outputValidator;

    public AgentController(DeepSeekClient deepSeekClient,
                           PromptBuilder promptBuilder,
                           ObjectMapper objectMapper,
                           QaHistoryRepository qaHistoryRepository,
                           WebSearchService webSearchService,
                           OutputValidator outputValidator) {
        this.deepSeekClient = deepSeekClient;
        this.promptBuilder = promptBuilder;
        this.objectMapper = objectMapper;
        this.qaHistoryRepository = qaHistoryRepository;
        this.webSearchService = webSearchService;
        this.outputValidator = outputValidator;
    }

    @PostMapping("/ask")
    public AgentAnswer ask(@RequestBody AskRequest request) {
        long startTime = System.currentTimeMillis();
        String qaId = UUID.randomUUID().toString();

        // 1. 搜索 + 抓取 → Evidence
        String query = (request.term() == null ? "" : request.term())
                + " "
                + (request.question() == null ? "" : request.question());
        List<Evidence> evidences = webSearchService.searchAsEvidence(query.trim());

        // 2. 构造 Prompt
        String systemPrompt = promptBuilder.buildSystemPrompt();
        String userPrompt = promptBuilder.buildUserPrompt(
                request.term(),
                request.question(),
                request.context(),
                evidences
        );

        // 3. 调 LLM（含一次重试）
        DeepSeekResult result = callModel(systemPrompt, userPrompt);
        LlmRawAnswer raw = parseLlmAnswer(result.content());

        if (raw == null) {
            String retrySystem = systemPrompt
                    + "\n\n[CRITICAL] 上一次回复不是有效 JSON。只返回合法 JSON，"
                    + "不要 markdown 代码块，不要多余文字。";
            result = callModel(retrySystem, userPrompt);
            raw = parseLlmAnswer(result.content());
            if (raw == null) {
                throw new AgentException("A002", "模型返回的 JSON 非法，重试后仍失败");
            }
        }

        // 4. 校验 sourceIds，映射真实 SourceDTO
        List<SourceDTO> sources = outputValidator.validateAndResolve(
                raw.sourceIds(), evidences);

        // 5. 计算 EvidenceLevel 和 Usage
        EvidenceLevel evidenceLevel = computeEvidenceLevel(sources);
        long latencyMs = System.currentTimeMillis() - startTime;
        UsageDTO usage = new UsageDTO(
                result.inputTokens(), result.outputTokens(), latencyMs);

        // 6. 组装最终答案
        AgentAnswer answer = new AgentAnswer(
                qaId,
                AgentRoute.WEB,          // Day 5 先固定，Week 4 接 QueryRouter
                request.term(),
                raw.oneLine(),
                raw.explanation(),
                raw.keyPoint(),
                raw.sourceIds(),
                sources,
                evidenceLevel,
                usage
        );

        // 7. 落库
        qaHistoryRepository.save(request, answer);

        return answer;
    }

    // ---------- 内部方法 ----------

    private DeepSeekResult callModel(String systemPrompt, String userPrompt) {
        return deepSeekClient.chat(List.of(
                new DeepSeekChatRequest.Message("system", systemPrompt),
                new DeepSeekChatRequest.Message("user", userPrompt)
        ));
    }

    /**
     * 解析 LLM 返回的 JSON 为 LlmRawAnswer。失败返回 null。
     */
    private LlmRawAnswer parseLlmAnswer(String raw) {
        try {
            return objectMapper.readValue(cleanJson(raw), LlmRawAnswer.class);
        } catch (Exception e) {
            System.err.println("LlmRawAnswer 解析失败: " + e.getMessage()
                    + "，原文: " + raw);
            return null;
        }
    }

    private String cleanJson(String raw) {
        if (raw == null) return "{}";
        String t = raw.trim();
        if (t.startsWith("```json")) t = t.substring(7);
        else if (t.startsWith("```")) t = t.substring(3);
        if (t.endsWith("```")) t = t.substring(0, t.length() - 3);
        return t.trim();
    }

    private EvidenceLevel computeEvidenceLevel(List<SourceDTO> sources) {
        if (sources == null || sources.isEmpty()) return EvidenceLevel.LOW;
        if (sources.size() == 1) return EvidenceLevel.MEDIUM;
        return EvidenceLevel.HIGH;
    }
}