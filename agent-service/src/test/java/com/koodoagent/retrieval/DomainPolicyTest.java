package com.koodoagent.retrieval;

import com.koodoagent.config.AgentProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

class DomainPolicyTest {

    private DomainPolicy policy;

    @BeforeEach
    void setUp() {
        AgentProperties.DomainPolicyConfig cfg = new AgentProperties.DomainPolicyConfig(
                List.of("wikipedia.org", "baike.baidu.com"),
                List.of("zhihu.com"),
                List.of("csdn.net")
        );
        AgentProperties props = new AgentProperties(
                4,      // maxToolCalls
                2,      // maxWebSearchRounds
                5,      // maxSources
                6000,   // sourceMaxChars
                20,     // fetchTimeoutSeconds
                1024,   // fetchMaxBodyKb
                cfg     // domainPolicy
        );
        policy = new DomainPolicy(props);
    }

    @Test
    void tierA_exact() {
        assertEquals(TrustLevel.TIER_A, policy.evaluate("https://baike.baidu.com/item/xxx"));
    }

    @Test
    void tierA_subdomain() {
        assertEquals(TrustLevel.TIER_A, policy.evaluate("https://zh.wikipedia.org/wiki/xxx"));
    }

    @Test
    void tierB() {
        assertEquals(TrustLevel.TIER_B, policy.evaluate("https://www.zhihu.com/question/123"));
    }

    @Test
    void blocked() {
        assertEquals(TrustLevel.BLOCKED, policy.evaluate("https://blog.csdn.net/xxx"));
    }

    @Test
    void unknownDomain() {
        assertEquals(TrustLevel.TIER_C, policy.evaluate("https://some-random-blog.com/post"));
    }

    @Test
    void nonHttpSchemeIsBlocked() {
        assertEquals(TrustLevel.BLOCKED, policy.evaluate("file:///etc/passwd"));
        assertEquals(TrustLevel.BLOCKED, policy.evaluate("ftp://example.com"));
    }

    @Test
    void trickySuffix() {
        // 不应该把 notwikipedia.org 当成 wikipedia.org
        assertEquals(TrustLevel.TIER_C, policy.evaluate("https://notwikipedia.org/x"));
    }
}
