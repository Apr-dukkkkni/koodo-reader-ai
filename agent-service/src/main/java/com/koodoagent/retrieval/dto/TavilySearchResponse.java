package com.koodoagent.retrieval.dto;

import java.util.List;

public record TavilySearchResponse(
        List<TavilyResult> results
) {
    public record TavilyResult(
            String title,
            String url,
            String content,
            Double score
    ) {}
}