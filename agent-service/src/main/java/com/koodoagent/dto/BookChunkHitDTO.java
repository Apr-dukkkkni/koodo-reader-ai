package com.koodoagent.dto;

/**
 * 一次 book_chunk 检索命中的结果。
 */
public record BookChunkHitDTO(
        long id,
        String bookId,
        String bookTitle,
        String chapter,
        int chunkIndex,
        String content,
        double similarity
) {
}