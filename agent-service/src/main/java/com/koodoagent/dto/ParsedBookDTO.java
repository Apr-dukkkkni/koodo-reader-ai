package com.koodoagent.dto;

import java.util.List;

/**
 * 书籍解析结果。
 */
public record ParsedBookDTO(
        String title,
        String format,
        int chapterCount,
        int totalChars,
        List<ParsedChapterDTO> chapters
) {
    public static ParsedBookDTO of(String title, String format, List<ParsedChapterDTO> chapters) {
        int totalChars = chapters.stream()
                .mapToInt(c -> c.content() == null ? 0 : c.content().length())
                .sum();
        return new ParsedBookDTO(title, format, chapters.size(), totalChars, chapters);
    }
}