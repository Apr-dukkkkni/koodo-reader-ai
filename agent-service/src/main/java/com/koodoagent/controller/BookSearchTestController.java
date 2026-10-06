package com.koodoagent.controller;

import com.koodoagent.agent.Evidence;
import com.koodoagent.dto.SearchTestRequestDTO;
import com.koodoagent.memory.BookRagService;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/agent/book")
public class BookSearchTestController {

    private final BookRagService bookRagService;

    public BookSearchTestController(BookRagService bookRagService) {
        this.bookRagService = bookRagService;
    }

    @PostMapping("/search-test")
    public Map<String, Object> searchTest(@RequestBody SearchTestRequestDTO request) {
        List<Evidence> evidences =
                bookRagService.searchAsEvidence(request.getBookId(), "(test)", request.getQuery());

        List<Map<String, Object>> items = evidences.stream()
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", e.id());
                    m.put("type", e.type().name());
                    m.put("title", e.title());
                    m.put("domain", e.domain());
                    m.put("trustLevel", e.trustLevel());
                    m.put("contentPreview", e.content().length() > 200
                            ? e.content().substring(0, 200) + "..."
                            : e.content());
                    return m;
                })
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("bookId", request.getBookId());
        result.put("query",  request.getQuery());
        result.put("hitCount", evidences.size());
        result.put("hits", items);
        return result;
    }
}