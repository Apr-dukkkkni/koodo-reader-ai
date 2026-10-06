package com.koodoagent.llm.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record EmbeddingResponseDTO(
        String object,
        List<Item> data,
        String model,
        Usage usage
) {
    public record Item(
            String object,
            int index,
            List<Float> embedding
    ) {}

    public record Usage(
            @JsonProperty("prompt_tokens") Integer promptTokens,
            @JsonProperty("total_tokens") Integer totalTokens
    ) {}
}