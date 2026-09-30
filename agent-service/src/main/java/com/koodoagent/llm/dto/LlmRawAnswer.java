package com.koodoagent.llm.dto;

import java.util.List;

public record LlmRawAnswer(
        String oneLine,
        String explanation,
        String keyPoint,
        List<String> sourceIds
) {}