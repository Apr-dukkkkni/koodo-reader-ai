package com.koodoagent.dto;

import java.util.List;

public record AgentAnswer(
        String qaId,
        String term,
        String oneLine,
        String explanation,
        List<String> keyPoint,
        List<SourceDTO> sources,
        UsageDTO usage
) {}