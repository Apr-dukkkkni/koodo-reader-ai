package com.koodoagent.agent;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EvidenceJudgerServiceTest {

    private final EvidenceJudgerService judger = new EvidenceJudgerService();

    private Evidence web(String id, int trustLevel) {
        return new Evidence(
                id,
                EvidenceType.WEB,
                "title-" + id,
                "content-" + id,
                "https://example.com/" + id,
                "example.com",
                trustLevel
        );
    }

    @Test
    void shouldReturnFalseWhenNullOrEmpty() {
        assertFalse(judger.isEnough(null));
        assertFalse(judger.isEnough(List.of()));
    }

    @Test
    void shouldReturnTrueWhenHasTierA() {
        // 一条 TIER_A（trustLevel >= 3）就够
        assertTrue(judger.isEnough(List.of(web("S1", 3))));
    }

    @Test
    void shouldReturnTrueWhenTwoUsableEvenIfNoTierA() {
        // 两条 TIER_B（trustLevel >= 2）也够
        assertTrue(judger.isEnough(List.of(web("S1", 2), web("S2", 2))));
    }

    @Test
    void shouldReturnFalseWhenOnlyOneTierB() {
        // 只有一条 TIER_B → 不够
        assertFalse(judger.isEnough(List.of(web("S1", 2))));
    }

    @Test
    void shouldReturnFalseWhenOnlyTierC() {
        // 全是 TIER_C → 不够
        assertFalse(judger.isEnough(List.of(web("S1", 1), web("S2", 1))));
    }
}