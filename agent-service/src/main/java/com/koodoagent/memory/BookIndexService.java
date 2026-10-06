package com.koodoagent.memory;

import com.koodoagent.dto.ParsedBookDTO;
import com.koodoagent.dto.ParsedChapterDTO;
import com.koodoagent.llm.EmbeddingClient;
import com.koodoagent.persistence.BookChunkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class BookIndexService {

    private static final Logger log = LoggerFactory.getLogger(BookIndexService.class);

    private final BookParserService bookParserService;
    private final TextChunkerService textChunkerService;
    private final EmbeddingClient embeddingClient;
    private final BookChunkRepository bookChunkRepository;

    public BookIndexService(BookParserService bookParserService,
                            TextChunkerService textChunkerService,
                            EmbeddingClient embeddingClient,
                            BookChunkRepository bookChunkRepository) {
        this.bookParserService = bookParserService;
        this.textChunkerService = textChunkerService;
        this.embeddingClient = embeddingClient;
        this.bookChunkRepository = bookChunkRepository;
    }

    /**
     * 完整的索引流程：解析 → 切 chunk → 批量 embedding → 写入 pgvector
     *
     * @param force 为 false 时，如果 bookId 已有 chunk，直接返回缓存结果，
     *              不重新解析、不重新 embedding（避免浪费 token）
     */
    public IndexResult indexBook(String bookId, Path file, boolean force) {
        long start = System.currentTimeMillis();
        log.info("开始索引: bookId={} file={} force={}", bookId, file, force);

        // 0. 已有数据保护
        int existing = bookChunkRepository.countByBookId(bookId);
        if (existing > 0 && !force) {
            log.info("bookId={} 已有 {} 个 chunk，跳过索引（force=true 可强制重建）",
                    bookId, existing);
            return new IndexResult(bookId, "(cached)", existing, 0);
        }

        // 1. 解析
        ParsedBookDTO book = bookParserService.parse(file);
        log.info("解析完成: title={} chapters={} chars={}",
                book.title(), book.chapterCount(), book.totalChars());

        // 2. 切 chunk
        List<ChunkWithMeta> allChunks = new ArrayList<>();
        for (ParsedChapterDTO chapter : book.chapters()) {
            List<String> chunks = textChunkerService.chunk(chapter.content());
            for (int i = 0; i < chunks.size(); i++) {
                allChunks.add(new ChunkWithMeta(
                        chapter.index(),
                        chapter.title(),
                        i,
                        chunks.get(i)
                ));
            }
        }
        log.info("切分完成: 共 {} 个 chunk", allChunks.size());

        if (allChunks.isEmpty()) {
            throw new IllegalStateException("没有可索引的 chunk");
        }

        // 预估 token（中文约 1 字符 1 token，英文约 4 字符 1 token，粗略按字符数估）
        long totalChars = allChunks.stream()
                .mapToLong(c -> c.content().length())
                .sum();
        long estimatedCalls = (allChunks.size() + 24) / 25;   // BATCH_SIZE = 25
        log.info("本次索引预计消耗: chunks={} chars≈{} 预估 {} token, {} 次 API 调用",
                allChunks.size(), totalChars, totalChars, estimatedCalls);

        // 3. 清空旧数据（force=true 时走到这里）
        bookChunkRepository.deleteByBookId(bookId);

        // 4. 批量 embedding
        List<String> texts = allChunks.stream().map(ChunkWithMeta::content).toList();
        List<float[]> vectors = embeddingClient.embedBatch(texts);
        log.info("Embedding 完成: {} 个向量", vectors.size());

        if (vectors.size() != allChunks.size()) {
            throw new IllegalStateException("Embedding 数量与 chunk 数量不一致");
        }

        // 5. 逐条写入 pgvector
        for (int i = 0; i < allChunks.size(); i++) {
            ChunkWithMeta c = allChunks.get(i);
            bookChunkRepository.insert(
                    bookId,
                    book.title(),
                    c.chapterTitle(),
                    i,
                    c.content(),
                    vectors.get(i)
            );
        }

        long elapsed = System.currentTimeMillis() - start;
        int count = bookChunkRepository.countByBookId(bookId);

        log.info("索引完成: bookId={} chunks={} elapsedMs={}", bookId, count, elapsed);

        return new IndexResult(bookId, book.title(), count, elapsed);
    }

    /**
     * 兼容旧调用：默认不强制重建。
     */
    public IndexResult indexBook(String bookId, Path file) {
        return indexBook(bookId, file, false);
    }

    private record ChunkWithMeta(
            int chapterIndex,
            String chapterTitle,
            int localIndex,
            String content
    ) {}

    public record IndexResult(
            String bookId,
            String title,
            int chunkCount,
            long elapsedMs
    ) {}
}