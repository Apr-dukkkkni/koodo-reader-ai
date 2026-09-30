package com.koodoagent.agent;

public record Evidence(
        String id,               // S1, S2, S3...
        EvidenceType type,
        String title,
        String content,
        String url,              // BOOK / MEMORY 可能为 null
        String domain,
        int trustLevel           // TIER_A=3 / TIER_B=2 / TIER_C=1
) {}