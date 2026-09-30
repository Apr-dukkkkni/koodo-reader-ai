package com.koodoagent.dto;

import com.koodoagent.agent.AgentRoute;

import java.util.List;

public record AgentAnswer(
        String qaId,
        AgentRoute route,
        String term,
        String oneLine,
        String explanation,
        String keyPoint,
        List<String> sourceIds,
        List<SourceDTO> sources,
        EvidenceLevel evidenceLevel,
        UsageDTO usage
) {}