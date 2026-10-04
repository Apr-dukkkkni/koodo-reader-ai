package com.koodoagent.agent;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RvideenceJudgerServiceTest {

    private final EvidenceJudgerService judger = new EvidenceJudgerService();

    private Evidence web(String id, int trustLevel) {
        return new Evidence(id, EvidenceType.WEB, "t", "c",
                "https://x/" + id, "x.com", trustLevel);
    }

    @Test
    void shouldReturnFalseWhenEmpty() {
        assertFalse(judger.isEnough(List.of()));
        assertFalse(judger.isEnough(null));
    }

    @Test
    void shouldReturnTrueWhenHasTierA() {
        assertTrue(judger.isEnough(List.of(web("S1", 3))));
    }

    @Test
    void shouldReturnTrueWhenTwoUsableEvenIfNoTierA() {
        assertTrue(judger.isEnough(List.of(web("S1", 2), web("S2", 2))));
    }

    @Test
    void shouldReturnFalseWhenOnlyOneTierB() {
        assertFalse(judger.isEnough(List.of(web("S1", 2))));
    }

    @Test
    void shouldReturnFalseWhenOnlyTierC() {
        assertFalse(judger.isEnough(List.of(web("S1", 1), web("S2", 1))));
    }
}