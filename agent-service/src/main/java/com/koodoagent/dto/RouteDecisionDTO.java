package com.koodoagent.dto;

import com.koodoagent.agent.AgentRoute;

public record RouteDecisionDTO(
        AgentRoute route,
        boolean needMemory,
        boolean needBookRag,
        boolean needWeb,
        String reason
) {
}