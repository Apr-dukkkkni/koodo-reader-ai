package com.koodoagent.dto;

public record AskRequest(
        String term,
        String question,
        String context,
        String bookId,
        String bookTitle,
        String cfi
) {
}