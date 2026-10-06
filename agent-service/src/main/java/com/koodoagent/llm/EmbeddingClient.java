package com.koodoagent.llm;

import com.koodoagent.config.DeepSeekProperties;
import com.koodoagent.exception.AgentException;
import com.koodoagent.llm.dto.EmbeddingRequestDTO;
import com.koodoagent.llm.dto.EmbeddingResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class EmbeddingClient {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingClient.class);

    private static final String MODEL = "text-embedding-v3";
    private static final int BATCH_SIZE = 10;      // 阿里百炼上限

    private final WebClient webClient;
    private final DeepSeekProperties props;

    public EmbeddingClient(WebClient deepSeekWebClient, DeepSeekProperties props) {
        this.webClient = deepSeekWebClient;
        this.props = props;
    }

    /**
     * 批量 embedding。返回顺序和输入一致。
     */
    public List<float[]> embedBatch(List<String> texts) {
        List<float[]> result = new ArrayList<>();
        for (int i = 0; i < texts.size(); i += BATCH_SIZE) {
            List<String> batch = texts.subList(i, Math.min(i + BATCH_SIZE, texts.size()));
            result.addAll(callOnce(batch));
        }
        return result;
    }


    /**
     * 单条文本 embedding（用于检索时对问题编码）。
     */
    public float[] embedOne(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("embedOne 输入不能为空");
        }
        List<float[]> result = embedBatch(List.of(text));
        if (result.isEmpty()) {
            throw new IllegalStateException("Embedding 返回为空");
        }
        return result.get(0);
    }

    private List<float[]> callOnce(List<String> batch) {
        if (props.apiKey() == null || props.apiKey().isBlank()) {
            throw new IllegalStateException("DEEPSEEK_API_KEY 未设置");
        }

        EmbeddingRequestDTO request = new EmbeddingRequestDTO(MODEL, batch, "float");

        try {
            EmbeddingResponseDTO response = webClient.post()
                    .uri("/embeddings")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(EmbeddingResponseDTO.class)
                    .block(Duration.ofSeconds(props.timeoutSeconds()));

            if (response == null || response.data() == null || response.data().isEmpty()) {
                throw new IllegalStateException("Embedding 返回为空");
            }

            // 按 index 排序，保证顺序
            List<EmbeddingResponseDTO.Item> items = new ArrayList<>(response.data());
            items.sort(Comparator.comparingInt(EmbeddingResponseDTO.Item::index));

            List<float[]> vectors = new ArrayList<>();
            for (EmbeddingResponseDTO.Item item : items) {
                List<Float> emb = item.embedding();
                float[] arr = new float[emb.size()];
                for (int i = 0; i < emb.size(); i++) {
                    arr[i] = emb.get(i);
                }
                vectors.add(arr);
            }

            if (vectors.get(0).length != 1024) {
                log.warn("Embedding 维度不是 1024，实际={}，会导致写入 pgvector 失败",
                        vectors.get(0).length);
            }

            return vectors;

        } catch (Exception e) {
            if (e.getCause() instanceof java.util.concurrent.TimeoutException) {
                throw new AgentException("A001", "Embedding 响应超时");
            }
            throw new AgentException("A003", "Embedding 调用失败: " + e.getMessage());
        }
    }
}