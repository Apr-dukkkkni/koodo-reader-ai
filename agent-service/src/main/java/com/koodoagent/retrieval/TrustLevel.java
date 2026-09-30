package com.koodoagent.retrieval;

public enum TrustLevel {
    TIER_A(3),   // 百科、政府、大学、博物馆、正式学术机构
    TIER_B(2),   // 高质量媒体、专业数据库
    TIER_C(1),   // 普通网页，只能作为辅助
    BLOCKED(0);  // 直接丢弃

    private final int weight;

    TrustLevel(int weight) {
        this.weight = weight;
    }

    public int weight() {
        return weight;
    }
}