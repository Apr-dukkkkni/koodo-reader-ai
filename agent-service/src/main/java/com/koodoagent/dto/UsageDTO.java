package com.koodoagent.dto;

public record UsageDTO(
        long inputTokens,
        long outputTokens,
        long latencyMs
) {}