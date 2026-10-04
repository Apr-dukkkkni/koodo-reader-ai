package com.koodoagent.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.koodoagent.agent.AgentOrchestratorService;
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
@RestController
@RequestMapping("/api/v1/agent")
@CrossOrigin(origins = "*")
public class AgentController {

    // 只注入编排服务，其他全部移除
    private final AgentOrchestratorService agentOrchestratorService;
    private final ObjectMapper objectMapper;

    // 构造器：仅仅注入这一个
    public AgentController(AgentOrchestratorService agentOrchestratorService, ObjectMapper objectMapper) {
        this.agentOrchestratorService = agentOrchestratorService;
        this.objectMapper = objectMapper;
    }

    // 唯一接口入口，全部业务交给service处理
    @PostMapping("/ask")
    public AgentAnswer ask(@RequestBody AskRequest request) {
        return agentOrchestratorService.ask(request);
    }

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
        if (s.startsWith("```")) s = s.substring(3);
        if (s.endsWith("```")) s = s.substring(0, s.length() - 3);
        return s.trim();
    }

    private EvidenceLevel computeEvidenceLevel(List<SourceDTO> sources) {
        if (sources == null || sources.isEmpty()) return EvidenceLevel.LOW;
        if (sources.size() >= 2) return EvidenceLevel.HIGH;
        return EvidenceLevel.MEDIUM;
    }
}