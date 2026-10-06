package com.koodoagent.persistence;

import com.koodoagent.dto.BookChunkHitDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class BookChunkRepository {

    private final JdbcTemplate pgVectorJdbcTemplate;
    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(BookChunkRepository.class);

    public BookChunkRepository(
            @Qualifier("pgVectorJdbcTemplate") JdbcTemplate pgVectorJdbcTemplate) {
        this.pgVectorJdbcTemplate = pgVectorJdbcTemplate;
    }

    public void deleteByBookId(String bookId) {
        pgVectorJdbcTemplate.update("DELETE FROM book_chunk WHERE book_id = ?", bookId);
    }

    public void insert(String bookId, String bookTitle, String chapter,
                       int chunkIndex, String content, float[] embedding) {
        String vectorStr = toVectorString(embedding);
        String sql = """
            INSERT INTO book_chunk
                (book_id, book_title, chapter, chunk_index, content, embedding)
            VALUES (?, ?, ?, ?, ?, ?::vector)
        """;
        pgVectorJdbcTemplate.update(sql,
                bookId, bookTitle, chapter, chunkIndex, content, vectorStr);
    }

    public int countByBookId(String bookId) {
        Integer n = pgVectorJdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM book_chunk WHERE book_id = ?",
                Integer.class, bookId);
        return n == null ? 0 : n;
    }

    public int countAll() {
        Integer n = pgVectorJdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM book_chunk", Integer.class);
        return n == null ? 0 : n;
    }

    public List<Map<String, Object>> previewByBookId(String bookId, int limit) {
        return pgVectorJdbcTemplate.queryForList("""
            SELECT chunk_index, chapter, LENGTH(content) AS chars,
                   SUBSTRING(content, 1, 120) AS preview
            FROM book_chunk
            WHERE book_id = ?
            ORDER BY chunk_index
            LIMIT ?
        """, bookId, limit);
    }

    /**
     * float[] → pgvector 字面量格式：[0.1,0.2,0.3]
     */
    private String toVectorString(float[] v) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < v.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(v[i]);
        }
        sb.append("]");
        return sb.toString();
    }



    /**
     * 按 bookId 过滤 + 余弦相似度 Top-K 检索。
     *
     * @param bookId     书 ID（必填，避免跨书污染）
     * @param queryVec   问题向量
     * @param topK       返回条数上限
     * @param minScore   最小余弦相似度（0~1，低于此值丢弃）
     */

    public List<BookChunkHitDTO> search(String bookId, float[] queryVec,
                                        int topK, double minScore) {
        log.debug("BookChunkRepository.search bookId={} topK={} minScore={}"
                ,bookId, topK, minScore);
        String vectorStr = toVectorString(queryVec);

        String sql = """
        SELECT id, book_id, book_title, chapter, chunk_index, content,
               1 - (embedding <=> ?::vector) AS similarity
        FROM book_chunk
        WHERE book_id = ?
          AND 1 - (embedding <=> ?::vector) >= ?
        ORDER BY similarity DESC
        LIMIT ?
    """;

        return pgVectorJdbcTemplate.query(sql, (rs, rowNum) -> new BookChunkHitDTO(
                rs.getLong("id"),
                rs.getString("book_id"),
                rs.getString("book_title"),
                rs.getString("chapter"),
                rs.getInt("chunk_index"),
                rs.getString("content"),
                rs.getDouble("similarity")
        ), vectorStr, bookId, vectorStr, minScore, topK);
    }
}