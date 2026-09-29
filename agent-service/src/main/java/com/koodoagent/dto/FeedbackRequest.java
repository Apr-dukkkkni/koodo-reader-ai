package com.koodoagent.dto;

public record FeedbackRequest(
        String qaId,
        String rating,
        String comment
) {
}