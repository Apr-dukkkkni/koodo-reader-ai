package com.koodoagent.dto;

/**
 * 书籍解析后的单个章节。
 */
public record ParsedChapterDTO(
        int index,
        String title,
        String content
) {
}