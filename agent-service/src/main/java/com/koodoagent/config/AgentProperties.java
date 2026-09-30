package com.koodoagent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "agent")
public record AgentProperties(
        int maxToolCalls,
        int maxWebSearchRounds,
        int maxSources,
        int sourceMaxChars,
        int fetchTimeoutSeconds,
        int fetchMaxBodyKb,
        DomainPolicyConfig domainPolicy
) {
    public record DomainPolicyConfig(
            List<String> tierA,
            List<String> tierB,
            List<String> blocked
    ) {}
}