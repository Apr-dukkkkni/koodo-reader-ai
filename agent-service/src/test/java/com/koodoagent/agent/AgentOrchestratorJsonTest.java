package com.koodoagent.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.koodoagent.exception.AgentException;
import com.koodoagent.llm.dto.LlmRawAnswer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class AgentOrchestratorJsonTest {

    private AgentOrchestratorService service;
    private Method parseMethod;

    @BeforeEach
    void setUp() throws Exception {

        service = new AgentOrchestratorService(
                null,   // queryRouterService
                null,   // webSearchService
                null,   // promptBuilder
                null,   // deepSeekClient
                null,   // outputValidator
                null,   // qaHistoryRepository
                new ObjectMapper(),
                null,   // memoryService
                null,   // evidenceJudgerService
                null,   // queryRewriteService
                null,   // agentProperties
                null,   // profileService
                null    // bookRagService
        );

        parseMethod = AgentOrchestratorService.class
                .getDeclaredMethod(
                        "parseLlmAnswer",
                        String.class
                );

        parseMethod.setAccessible(true);
    }

    private LlmRawAnswer parse(String content) {

        try {
            return (LlmRawAnswer) parseMethod.invoke(
                    service,
                    content
            );

        } catch (InvocationTargetException e) {

            Throwable cause = e.getCause();

            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }

            throw new RuntimeException(cause);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void shouldParseValidJson() {

        String json = """
                {
                  "oneLine": "一句话",
                  "explanation": "解释",
                  "keyPoint": "要点",
                  "sourceIds": []
                }
                """;

        LlmRawAnswer result = parse(json);

        assertEquals("一句话", result.oneLine());
        assertEquals("解释", result.explanation());
        assertEquals("要点", result.keyPoint());
        assertNotNull(result.sourceIds());
    }

    @Test
    void shouldParseJsonMarkdownFence() {

        String json = """
                ```json
                {
                  "oneLine": "一句话",
                  "explanation": "解释",
                  "keyPoint": "要点",
                  "sourceIds": []
                }
                ```
                """;

        LlmRawAnswer result = parse(json);

        assertEquals("一句话", result.oneLine());
    }

    @Test
    void shouldParsePlainMarkdownFence() {

        String json = """
                ```
                {
                  "oneLine": "一句话",
                  "explanation": "解释",
                  "keyPoint": "要点",
                  "sourceIds": []
                }
                ```
                """;

        LlmRawAnswer result = parse(json);

        assertEquals("一句话", result.oneLine());
    }

    @Test
    void shouldRejectMalformedJson() {

        AgentException ex = assertThrows(
                AgentException.class,
                () -> parse("""
                        {
                          "oneLine": "一句话",
                          "explanation":
                        }
                        """)
        );

        assertTrue(
                ex.getMessage()
                        .contains("模型输出无法解析为 JSON")
        );
    }

    @Test
    void shouldRejectNonJsonText() {

        assertThrows(
                AgentException.class,
                () -> parse("这不是 JSON")
        );
    }

    @Test
    void shouldRejectEmptyString() {

        assertThrows(
                AgentException.class,
                () -> parse("")
        );
    }

    @Test
    void shouldRejectNull() {

        assertThrows(
                AgentException.class,
                () -> parse(null)
        );
    }

    @Test
    void shouldRejectJsonWithExtraText() {

        assertThrows(
                AgentException.class,
                () -> parse("""
                        这是模型的回答：
                        {
                          "oneLine": "一句话",
                          "explanation": "解释",
                          "keyPoint": "要点",
                          "sourceIds": []
                        }
                        """)
        );
    }
}