package com.koodoagent.agent;

import com.koodoagent.dto.SourceDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OutputValidatorTest {

    private final OutputValidator validator = new OutputValidator();

    @Test
    void shouldDropFakeSourceId() {

        Evidence realEvidence = new Evidence(
                "S1",
                EvidenceType.WEB,
                "测试来源",
                "纪德于1947年获得诺贝尔文学奖",
                "https://example.com",
                "example.com",
                2
        );

        List<SourceDTO> result = validator.validateAndResolve(
                List.of("S1", "S999"),
                List.of(realEvidence)
        );

        assertEquals(1, result.size());
        assertEquals("S1", result.get(0).sourceId());
    }

    @Test
    void shouldReturnEmptyWhenAllSourceIdsAreFake() {

        Evidence realEvidence = new Evidence(
                "S1",
                EvidenceType.WEB,
                "测试来源",
                "正常内容",
                "https://example.com",
                "example.com",
                2
        );

        List<SourceDTO> result = validator.validateAndResolve(
                List.of("S999", "S888"),
                List.of(realEvidence)
        );

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldRemoveDuplicateSourceIds() {

        Evidence realEvidence = new Evidence(
                "S1",
                EvidenceType.WEB,
                "测试来源",
                "正常内容",
                "https://example.com",
                "example.com",
                2
        );

        List<SourceDTO> result = validator.validateAndResolve(
                List.of("S1", "S1", "S1"),
                List.of(realEvidence)
        );

        assertEquals(1, result.size());
    }
}