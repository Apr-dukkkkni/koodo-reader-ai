package com.koodoagent.retrieval;

import com.koodoagent.config.TavilyProperties;
import com.koodoagent.exception.AgentException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class TavilyClientTimeoutTest {

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void shouldConvertTimeoutToA003() throws Exception {

        AtomicInteger requestHits =
                new AtomicInteger(0);

        HttpServer server = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0),
                0
        );

        server.createContext("/search", exchange -> {

            requestHits.incrementAndGet();

            try {
                // 故意超过 TavilyClient 的 1 秒超时
                Thread.sleep(3000);

                byte[] body = """
                        {
                          "results": []
                        }
                        """.getBytes(StandardCharsets.UTF_8);

                exchange.getResponseHeaders()
                        .add(
                                "Content-Type",
                                "application/json"
                        );

                exchange.sendResponseHeaders(
                        200,
                        body.length
                );

                exchange.getResponseBody()
                        .write(body);

            } catch (Exception ignored) {
                // 客户端超时断开后，
                // 服务端继续写响应可能抛异常，测试里忽略。
            } finally {
                exchange.close();
            }
        });

        server.start();

        try {

            int port =
                    server.getAddress().getPort();

            TavilyProperties props =
                    new TavilyProperties(
                            "http://127.0.0.1:" + port,
                            "test-api-key",
                            5,
                            "basic",
                            1,      // 1 秒超时
                            null
                    );

            TavilyClient client =
                    new TavilyClient(props);

            AgentException ex =
                    assertThrows(
                            AgentException.class,
                            () -> client.search("test query")
                    );

            assertTrue(
                    ex.getMessage()
                            .contains("Web search timeout")
            );

            assertEquals(
                    1,
                    requestHits.get(),
                    "请求应该真实到达本地测试服务器"
            );

        } finally {
            server.stop(0);
        }
    }
}