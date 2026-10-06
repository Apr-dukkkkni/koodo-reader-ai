package com.koodoagent.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.koodoagent.exception.AgentException;
import com.koodoagent.llm.DeepSeekResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class AgentOrchestratorRetryTest {

    private AgentOrchestratorService service;

    @BeforeEach
    void setUp() {

        service = new AgentOrchestratorService(
                null,               // queryRouterService
                null,               // webSearchService
                null,               // promptBuilder
                null,               // deepSeekClient
                null,               // outputValidator
                null,               // qaHistoryRepository
                new ObjectMapper(),
                null,               // memoryService
                null,               // evidenceJudgerService
                null,               // queryRewriteService
                null,               // agentProperties
                null,               // profileService
                null                // bookRagService
        );
    }

    private String validJson() {
        return """
                {
                  "oneLine": "重试成功",
                  "explanation": "第二次返回了合法 JSON",
                  "keyPoint": "最多重试一次",
                  "sourceIds": []
                }
                """;
    }

    /**
     * 第一次非法，第二次合法：
     * 应成功返回第二次结果，并且只重试一次。
     */
    @Test
    void shouldSucceedWhenRetryReturnsValidJson() {

        DeepSeekResult firstResult =
                new DeepSeekResult(
                        "this is not json",
                        10,
                        20
                );

        AtomicInteger retryCount =
                new AtomicInteger(0);

        AgentOrchestratorService.ParsedLlmResult result =
                service.parseWithRetry(
                        "qa-test-1",
                        "system",
                        "user",
                        firstResult,
                        retryPrompt -> {

                            retryCount.incrementAndGet();

                            assertTrue(
                                    retryPrompt.contains(
                                            "上一次输出无法解析为 JSON"
                                    )
                            );

                            return new DeepSeekResult(
                                    validJson(),
                                    30,
                                    40
                            );
                        }
                );

        assertEquals(
                1,
                retryCount.get(),
                "只能重试一次"
        );

        assertEquals(
                "重试成功",
                result.raw().oneLine()
        );

        /*
         * 必须返回第二次调用结果，
         * 防止 token 仍然统计第一次失败请求。
         */
        assertEquals(
                30,
                result.llmResult().inputTokens()
        );

        assertEquals(
                40,
                result.llmResult().outputTokens()
        );
    }

    /**
     * 第一次非法，第二次仍非法：
     * 必须抛 A002，并且不能进行第三次调用。
     */
    @Test
    void shouldThrowA002WhenRetryAlsoReturnsInvalidJson() {

        DeepSeekResult firstResult =
                new DeepSeekResult(
                        "invalid-json-first",
                        10,
                        20
                );

        AtomicInteger retryCount =
                new AtomicInteger(0);

        AgentException ex = assertThrows(
                AgentException.class,
                () -> service.parseWithRetry(
                        "qa-test-2",
                        "system",
                        "user",
                        firstResult,
                        retryPrompt -> {

                            retryCount.incrementAndGet();

                            return new DeepSeekResult(
                                    "invalid-json-second",
                                    30,
                                    40
                            );
                        }
                )
        );

        assertEquals(
                1,
                retryCount.get(),
                "第二次仍非法后不能继续第三次重试"
        );

        assertTrue(
                ex.getMessage()
                        .contains("模型输出两次均无法解析为 JSON")
        );
    }
}