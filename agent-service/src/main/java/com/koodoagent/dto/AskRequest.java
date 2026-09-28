package com.koodoagent.dto;

public record AskRequest(
        String question,
        String context,
        String bookId,
        String bookTitle
) {
}