package com.koodoagent.retrieval;

import com.koodoagent.config.AgentProperties;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.List;

@Service
public class DomainPolicy {

    private final AgentProperties props;

    public DomainPolicy(AgentProperties props) {
        this.props = props;
    }

    /**
     * 从 URL 提取域名，并给出可信等级。
     * 解析失败或 scheme 非 http/https 时返回 BLOCKED。
     */
    public TrustLevel evaluate(String url) {
        String host = extractHost(url);
        if (host == null) {
            return TrustLevel.BLOCKED;
        }

        AgentProperties.DomainPolicyConfig cfg = props.domainPolicy();

        if (matches(host, cfg.blocked())) {
            return TrustLevel.BLOCKED;
        }
        if (matches(host, cfg.tierA())) {
            return TrustLevel.TIER_A;
        }
        if (matches(host, cfg.tierB())) {
            return TrustLevel.TIER_B;
        }
        return TrustLevel.TIER_C;
    }

    /**
     * 提取域名。非 http/https 返回 null。
     */
    public String extractHost(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        try {
            URI uri = URI.create(url);
            String scheme = uri.getScheme();
            if (scheme == null
                    || (!scheme.equalsIgnoreCase("http")
                    && !scheme.equalsIgnoreCase("https"))) {
                return null;
            }
            String host = uri.getHost();
            if (host == null) {
                return null;
            }
            return host.toLowerCase();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 后缀匹配，支持子域。
     * 配置 wikipedia.org 会匹配 zh.wikipedia.org / en.wikipedia.org。
     * 配置 example.com 不会匹配 notexample.com。
     */
    private boolean matches(String host, List<String> domains) {
        if (domains == null || domains.isEmpty()) {
            return false;
        }
        for (String d : domains) {
            if (d == null || d.isBlank()) {
                continue;
            }
            String suffix = d.toLowerCase();
            if (host.equals(suffix) || host.endsWith("." + suffix)) {
                return true;
            }
        }
        return false;
    }
}