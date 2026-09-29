package com.koodoagent.llm;

import com.koodoagent.config.DeepSeekProperties;
import com.koodoagent.exception.AgentException;
import com.koodoagent.llm.dto.DeepSeekChatRequest;
import com.koodoagent.llm.dto.DeepSeekChatResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;

@Component
public class DeepSeekClient {

    private final WebClient webClient;
    private final DeepSeekProperties props;

    public DeepSeekClient(WebClient deepSeekWebClient, DeepSeekProperties props) {
        this.webClient = deepSeekWebClient;
        this.props = props;
    }

    public DeepSeekResult chat(List<DeepSeekChatRequest.Message> messages) {
        if (props.apiKey() == null || props.apiKey().isBlank()) {
            throw new IllegalStateException("DEEPSEEK_API_KEY 未设置");
        }

        DeepSeekChatRequest request = new DeepSeekChatRequest(
                props.model(),
                messages,
                false,
                0.3,
                new DeepSeekChatRequest.ResponseFormat("json_object")
        );

        try {
            // 只调用一次 API，并放在 try 块内
            DeepSeekChatResponse response = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(DeepSeekChatResponse.class)
                    .block(Duration.ofSeconds(props.timeoutSeconds()));

            if (response == null || response.choices() == null || response.choices().isEmpty()) {
                throw new IllegalStateException("DeepSeek 返回为空");
            }

            String content = response.choices().get(0).message().content();
            int inputTokens = response.usage() != null ? response.usage().promptTokens() : 0;
            int outputTokens = response.usage() != null ? response.usage().completionTokens() : 0;

            return new DeepSeekResult(content, inputTokens, outputTokens);

        } catch (Exception e) {
            // 处理超时
            if (e.getCause() instanceof java.util.concurrent.TimeoutException) {
                throw new AgentException("A001", "模型响应超时，请稍后重试");
            }
            // 处理其他 API 错误（401/402/400 等）
            throw new AgentException("A003", "模型调用失败: " + e.getMessage());
        }
    }
}