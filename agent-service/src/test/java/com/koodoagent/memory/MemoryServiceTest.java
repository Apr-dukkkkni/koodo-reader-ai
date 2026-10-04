package com.koodoagent.memory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.koodoagent.agent.Evidence;
import com.koodoagent.persistence.QaHistoryRepository;
import com.koodoagent.persistence.dto.QaHistoryRecordDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MemoryServiceTest {

    private final QaHistoryRepository repo = mock(QaHistoryRepository.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final MemoryService service = new MemoryService(repo, mapper);

    private String answerJson(String oneLine, String keyPoint) {
        return """
            {"qaId":"x","route":"DIRECT","term":"t","oneLine":"%s",
             "explanation":"","keyPoint":"%s","sourceIds":[],
             "sources":[],"evidenceLevel":"LOW","usage":null}
            """.formatted(oneLine, keyPoint);
    }

    @Test
    void shouldReturnEmptyWhenNoHistory() {
        when(repo.findRecentByTerm(anyString(), anyInt())).thenReturn(List.of());
        when(repo.findRecent(anyInt())).thenReturn(List.of());

        List<Evidence> result = service.searchAsEvidence("郡县制", "再简单讲一下", 3);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldHitByTermFirst() {
        QaHistoryRecordDTO row = new QaHistoryRecordDTO(
                "qa-1", "郡县制", "什么是郡县制",
                "DIRECT", "LOW", answerJson("一句话", "要点"), "2026-09-30");
        when(repo.findRecentByTerm(eq("郡县制"), anyInt())).thenReturn(List.of(row));
        when(repo.findRecent(anyInt())).thenReturn(List.of());

        List<Evidence> result = service.searchAsEvidence("郡县制", "再简单讲一下", 3);

        assertEquals(1, result.size());
        assertEquals("M1", result.get(0).id());
        assertTrue(result.get(0).content().contains("一句话"));
    }

    @Test
    void shouldFillWithRecentWhenTermMisses() {
        QaHistoryRecordDTO row = new QaHistoryRecordDTO(
                "qa-2", "其他", "其他问题",
                "DIRECT", "LOW", answerJson("历史回答", "要点"), "2026-09-30");
        when(repo.findRecentByTerm(anyString(), anyInt())).thenReturn(List.of());
        when(repo.findRecent(anyInt())).thenReturn(List.of(row));

        List<Evidence> result = service.searchAsEvidence("新概念", "问一下", 3);
        assertEquals(1, result.size());
        assertEquals("M1", result.get(0).id());
    }
}