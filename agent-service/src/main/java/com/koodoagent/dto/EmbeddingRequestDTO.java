package com.koodoagent.llm.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record EmbeddingRequestDTO(
        String model,
        List<String> input,
        @JsonProperty("encoding_format") String encodingFormat
) {
}