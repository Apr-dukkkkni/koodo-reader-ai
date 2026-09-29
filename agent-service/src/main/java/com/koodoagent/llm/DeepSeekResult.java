package com.koodoagent.llm;

public record DeepSeekResult (
        String content,
        int inputTokens,
        int outputTokens
){
}
