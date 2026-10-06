package com.koodoagent.controller;

import com.koodoagent.dto.ParsedBookDTO;
import com.koodoagent.memory.BookParserService;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/agent/book")
public class BookParseTestController {

    private final BookParserService bookParserService;

    public BookParseTestController(BookParserService bookParserService) {
        this.bookParserService = bookParserService;
    }

    /**
     * 调试用：传入本地文件路径，返回解析结果摘要。
     * 不返回全部章节内容，避免响应过大。
     */
    @PostMapping("/parse-test")
    public Map<String, Object> parseTest(@RequestParam String filePath) {
        ParsedBookDTO book = bookParserService.parse(Path.of(filePath));

        List<Map<String, Object>> chapterSummary = book.chapters().stream()
                .map(c -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("index", c.index());
                    m.put("title", c.title());
                    m.put("chars", c.content().length());
                    m.put("preview", c.content().substring(0, Math.min(200, c.content().length())));
                    return m;
                })
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("title", book.title());
        result.put("format", book.format());
        result.put("chapterCount", book.chapterCount());
        result.put("totalChars", book.totalChars());
        result.put("chapters", chapterSummary);
        return result;
    }
}