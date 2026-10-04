package com.koodoagent.agent;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EvidenceJudgerService {

    /**
     * 判断当前证据是否"够用"。
     * 规则（第一版，刻意简单）：
     *   1. 至少有 1 条 TIER_A 证据 → 够
     *   2. 至少有 2 条任意 evidence 且其中 1 条非 TIER_C → 够
     *   3. 其余 → 不够
     */
    public boolean isEnough(List<Evidence> evidences) {
        if (evidences == null || evidences.isEmpty()) {
            return false;
        }

        long tierA = evidences.stream()
                .filter(e -> e.type() == EvidenceType.WEB)
                .filter(e -> e.trustLevel() >= 3)
                .count();
        if (tierA >= 1) {
            return true;
        }

        long usable = evidences.stream()
                .filter(e -> e.trustLevel() >= 2)
                .count();
        if (usable >= 2) {
            return true;
        }

        return false;
    }
}