package com.koodoagent.memory;

import com.koodoagent.agent.Evidence;
import com.koodoagent.agent.EvidenceType;
import com.koodoagent.config.AgentProperties;
import com.koodoagent.dto.BookChunkHitDTO;
import com.koodoagent.llm.EmbeddingClient;
import com.koodoagent.persistence.BookChunkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class BookRagService {

    private static final Logger log = LoggerFactory.getLogger(BookRagService.class);

    /** 默认 Top-K */
    private static final int DEFAULT_TOP_K = 5;

    /** 默认最小相似度（cosine similarity） */
    private static final double DEFAULT_MIN_SCORE = 0.35;

    private final EmbeddingClient embeddingClient;
    private final BookChunkRepository bookChunkRepository;
    private final AgentProperties agentProperties;

    public BookRagService(EmbeddingClient embeddingClient,
                          BookChunkRepository bookChunkRepository,
                          AgentProperties agentProperties) {
        this.embeddingClient = embeddingClient;
        this.bookChunkRepository = bookChunkRepository;
        this.agentProperties = agentProperties;
    }

    /**
     * 对用户问题在指定书籍内做检索，返回 Evidence 列表（type=BOOK）。
     * Evidence id 用 B1/B2/...，避免与 Web 的 S1 和 Memory 的 M1 冲突。
     */
    public List<Evidence> searchAsEvidence(String bookId, String bookTitle, String query) {
        if (bookId == null || bookId.isBlank()) {
            log.warn("BookRagService: bookId 为空，跳过检索");
            return List.of();
        }
        if (query == null || query.isBlank()) {
            return List.of();
        }

        float[] queryVec = embeddingClient.embedOne(query);
        List<BookChunkHitDTO> hits = bookChunkRepository.search(
                bookId, queryVec, DEFAULT_TOP_K, DEFAULT_MIN_SCORE);

        log.info("BookRagService 命中 {} 条 (bookId={}, query={})",
                hits.size(), bookId, truncate(query, 40));

        List<Evidence> evidences = new ArrayList<>();
        int idx = 1;
        for (BookChunkHitDTO hit : hits) {
            String title = hit.chapter() == null || hit.chapter().isBlank()
                    ? "chunk-" + hit.chunkIndex()
                    : hit.chapter();
            String content = "[相似度 %.2f] %s".formatted(hit.similarity(), hit.content());

            evidences.add(new Evidence(
                    "B" + idx,
                    EvidenceType.BOOK,
                    title,
                    content,
                    "",                  // BOOK 不给前端展示 URL
                    "book:" + bookId,
                    3                    // BOOK 证据算高可信
            ));
            idx++;
        }

        return evidences;
    }

    private String truncate(String s, int n) {
        if (s == null) return "";
        return s.length() <= n ? s : s.substring(0, n) + "...";
    }
}