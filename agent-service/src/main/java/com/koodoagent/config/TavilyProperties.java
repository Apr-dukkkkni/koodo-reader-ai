package com.koodoagent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
//在原来 TavilyProperties 的基础上，增加了代理（http 代理）配置。
//因为国内访问 tavily 外网 API 直连经常超时连不上，所以增加代理配置项，可以在 yml 里配置 HTTP 代理，让 WebClient 走代理访问 tavily 接口。
@ConfigurationProperties(prefix = "tavily")
public record TavilyProperties(
        String baseUrl,
        String apiKey,
        int maxResults,
        String searchDepth,
        int timeoutSeconds,
        ProxyConfig proxy
) {
    public record ProxyConfig(boolean enabled, String host, int port, String type) {}
}