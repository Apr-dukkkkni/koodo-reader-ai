package com.koodoagent.controller;

import com.koodoagent.retrieval.TavilyClient;
import com.koodoagent.retrieval.WebSearchService;
import com.koodoagent.retrieval.dto.SearchResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/agent")
public class SearchTestController {

    private final WebSearchService webSearchService;

    public SearchTestController(WebSearchService webSearchService) {
        this.webSearchService = webSearchService;
    }

    @GetMapping("/search-test")
    public List<SearchResult> searchTest(@RequestParam("query") String query) {
        return webSearchService.search(query);
    }
}