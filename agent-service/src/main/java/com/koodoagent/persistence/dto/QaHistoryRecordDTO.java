package com.koodoagent.persistence.dto;

public record QaHistoryRecordDTO(
        String id,
        String term,
        String question,
        String route,
        String evidenceLevel,
        String answerJson,
        String createdAt
) {
}