package com.koodoagent.retrieval;

import com.koodoagent.config.AgentProperties;
import com.koodoagent.config.TavilyProperties;
import com.koodoagent.exception.AgentException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class JsoupExtractorSsrfTest {

    private AgentProperties createAgentProperties() {
        return new AgentProperties(
                10,     // maxToolCalls
                2,      // maxWebSearchRounds
                5,      // maxSources
                6000,   // sourceMaxChars
                2,      // fetchTimeoutSeconds
                128,    // fetchMaxBodyKb
                new AgentProperties.DomainPolicyConfig(
                        List.of(),
                        List.of(),
                        List.of()
                )
        );
    }

    private TavilyProperties createTavilyProperties() {
        return new TavilyProperties(
                "http://unused",
                "unused",
                5,
                "basic",
                2,
                null        // 测试不使用代理
        );
    }

    /**
     * 直接访问 127.0.0.1 时，
     * 必须在真正发起网络请求前被 UrlSafetyChecker 拦截。
     */
    @Test
    void shouldBlockDirectPrivateUrlBeforeNetworkRequest() {

        UrlSafetyChecker checker = new UrlSafetyChecker();

        JsoupExtractor extractor = new JsoupExtractor(
                createAgentProperties(),
                createTavilyProperties(),
                checker
        );

        assertThrows(
                AgentException.class,
                () -> extractor.extract(
                        "http://127.0.0.1:65535/private"
                )
        );
    }

    /**
     * 验证重定向以后会再次执行 UrlSafetyChecker。
     *
     * 这里不测试 127.0.0.1 本身是否危险，
     * 因为 UrlSafetyCheckerTest 的 17 条已经测过。
     *
     * 这里专门测试：
     *
     * 第一次 URL -> 允许
     *       ↓ 302
     * 第二次 URL -> 再次 check -> BLOCK
     */
    @Test
    void shouldRecheckRedirectAndBlockPrivateTarget() throws Exception {

        AtomicInteger checkCount = new AtomicInteger(0);
        AtomicInteger privateEndpointHits = new AtomicInteger(0);

        HttpServer server = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0),
                0
        );

        int port = server.getAddress().getPort();

        String startUrl =
                "http://127.0.0.1:" + port + "/start";

        String privateUrl =
                "http://127.0.0.1:" + port + "/private";

        /*
         * /start 是第一跳：
         * 返回 302，把请求重定向到 /private。
         */
        server.createContext("/start", exchange -> {

            exchange.getResponseHeaders()
                    .add("Location", privateUrl);

            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });

        /*
         * 如果这个接口真的被访问，
         * 就说明重定向后的安全检查没有生效。
         */
        server.createContext("/private", exchange -> {

            privateEndpointHits.incrementAndGet();

            byte[] body =
                    "<html><body>SECRET INTERNAL DATA</body></html>"
                            .getBytes(StandardCharsets.UTF_8);

            exchange.sendResponseHeaders(
                    200,
                    body.length
            );

            exchange.getResponseBody().write(body);
            exchange.close();
        });

        server.start();

        try {

            /*
             * 不用 Mockito。
             *
             * 这里继承 UrlSafetyChecker：
             *
             * 第一次 check -> 放行
             * 第二次 check -> 模拟发现内网地址并阻断
             *
             * 这样可以专门验证 JsoupExtractor
             * 是否在 302 后真的重新调用 check()。
             */
            UrlSafetyChecker checker = new UrlSafetyChecker() {

                @Override
                public void check(String url) {

                    int count = checkCount.incrementAndGet();

                    if (count >= 2) {
                        throw new AgentException(
                                "A004",
                                "Redirect target blocked: " + url
                        );
                    }
                }
            };

            JsoupExtractor extractor = new JsoupExtractor(
                    createAgentProperties(),
                    createTavilyProperties(),
                    checker
            );

            assertThrows(
                    AgentException.class,
                    () -> extractor.extract(startUrl)
            );

            /*
             * 必须至少调用两次：
             *
             * 1. 请求 /start 前
             * 2. 请求 /private 前
             */
            assertEquals(
                    2,
                    checkCount.get(),
                    "重定向后应该再次执行 SSRF 检查"
            );

            /*
             * 第二次检查发生在真正请求 /private 之前，
             * 所以 private endpoint 应该一次都没被访问。
             */
            assertEquals(
                    0,
                    privateEndpointHits.get(),
                    "被重定向到的内网地址不应该被真正访问"
            );

        } finally {
            server.stop(0);
        }
    }
}