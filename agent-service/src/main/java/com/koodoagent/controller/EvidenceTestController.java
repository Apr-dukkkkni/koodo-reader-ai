package com.koodoagent.controller;

import com.koodoagent.agent.Evidence;
import com.koodoagent.retrieval.WebSearchService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/agent")
public class EvidenceTestController {

    private final WebSearchService webSearchService;

    public EvidenceTestController(WebSearchService webSearchService) {
        this.webSearchService = webSearchService;
    }

    @GetMapping("/evidence-test")
    public List<Map<String, Object>> evidenceTest(@RequestParam("query") String query) {
        return webSearchService.searchAsEvidence(query).stream()
                .map(e -> Map.<String, Object>of(
                        "id", e.id(),
                        "type", e.type().name(),
                        "title", e.title(),
                        "url", e.url(),
                        "domain", e.domain(),
                        "trustLevel", e.trustLevel(),
                        "contentPreview", e.content().substring(
                                0, Math.min(200, e.content().length())) + "..."
                ))
                .collect(Collectors.toList());
    }
}