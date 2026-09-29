package com.koodoagent.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.koodoagent.dto.AgentAnswer;
import com.koodoagent.dto.AskRequest;
import com.koodoagent.dto.UsageDTO; // 新增 import
import com.koodoagent.exception.AgentException;
import com.koodoagent.llm.DeepSeekClient;
import com.koodoagent.llm.DeepSeekResult;
import com.koodoagent.llm.PromptBuilder;
import com.koodoagent.llm.dto.DeepSeekChatRequest;
import com.koodoagent.persistence.QaHistoryRepository;
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

    public AgentController(DeepSeekClient deepSeekClient, PromptBuilder promptBuilder, ObjectMapper objectMapper, QaHistoryRepository qaHistoryRepository) {
        this.deepSeekClient = deepSeekClient;
        this.promptBuilder = promptBuilder;
        this.objectMapper = objectMapper;
        this.qaHistoryRepository = qaHistoryRepository;
    }

    @PostMapping("/ask")
    public AgentAnswer ask(@RequestBody AskRequest request) {
        // 1. 记录开始时间
        long startTime = System.currentTimeMillis();

        // 2. 预先生成 qaId (重试时复用同一个 qaId)
        String qaId = UUID.randomUUID().toString();

        String systemPrompt = promptBuilder.buildSystemPrompt();
        String userPrompt = promptBuilder.buildUserPrompt(request);

        // 3. 第一次调用，接收 DeepSeekResult
        DeepSeekResult result = callModel(systemPrompt, userPrompt);
        String jsonStr = result.content();

        // 4. 解析 JSON
        AgentAnswer answer;
        try {
            answer = objectMapper.readValue(cleanJson(jsonStr), AgentAnswer.class);
        } catch (Exception e) {
            System.err.println("首次 JSON 解析失败，准备重试。错误: " + e.getMessage() + "，原始返回: " + jsonStr);

            String retrySystemPrompt = systemPrompt + "\n\n[CRITICAL] 你上一次的回复不是有效的 JSON，报错信息：" + e.getMessage() + "。请务必只返回合法的 JSON 格式，不要包含任何 markdown 代码块或多余文字。";

            // 重试时，覆盖原来的 result（注意：重试的 token 消耗目前暂不累加，保持逻辑简单）
            result = callModel(retrySystemPrompt, userPrompt);
            try {
                answer = objectMapper.readValue(cleanJson(result.content()), AgentAnswer.class);
            } catch (Exception retryException) {
                throw new AgentException("A002", "模型返回的 JSON 格式非法，重试后仍然失败");
            }
        }

        // 5. 计算延迟
        long latencyMs = System.currentTimeMillis() - startTime;

        // 6. 构建 UsageDTO
        UsageDTO usage = new UsageDTO(
                result.inputTokens(),
                result.outputTokens(),
                latencyMs
        );

        // 7. 组装最终返回
        // 修改 ask 方法最后的 return 部分
        AgentAnswer finalAnswer = new AgentAnswer(
                qaId,
                request.term(),
                answer.oneLine(),
                answer.explanation(),
                answer.keyPoint(),
                null,
                usage
        );

// 落库
        qaHistoryRepository.save(request, finalAnswer);

        return finalAnswer;
    }

    // 改造 callModel，返回 DeepSeekResult 而不是 String
    private DeepSeekResult callModel(String systemPrompt, String userPrompt) {
        return deepSeekClient.chat(List.of(
                new DeepSeekChatRequest.Message("system", systemPrompt),
                new DeepSeekChatRequest.Message("user", userPrompt)
        ));
    }

    private String cleanJson(String raw) {
        if (raw == null) return "{}";
        String trimmed = raw.trim();
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed.trim();
    }
}