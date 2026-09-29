package com.koodoagent.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.koodoagent.dto.AgentAnswer;
import com.koodoagent.dto.AskRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class QaHistoryRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public QaHistoryRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public void save(AskRequest request, AgentAnswer answer) {
        String sql = """
            INSERT INTO qa_history 
            (id, term, question, context_text, book_id, book_title, cfi, route, answer_json, evidence_level, input_tokens, output_tokens, latency_ms)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try {
            // 把完整的 AgentAnswer 序列化成 JSON 存起来，方便后续做反馈和复盘
            String answerJson = objectMapper.writeValueAsString(answer);

            // 目前 route 和 evidence_level 还没做，先写死
            String route = "DIRECT";
            String evidenceLevel = "LOW";

            jdbcTemplate.update(sql,
                    answer.qaId(),
                    request.term(),
                    request.question(),
                    request.context(),
                    request.bookId(),
                    request.bookTitle(),
                    request.cfi(),
                    route,
                    answerJson,
                    evidenceLevel,
                    answer.usage() != null ? answer.usage().inputTokens() : 0,
                    answer.usage() != null ? answer.usage().outputTokens() : 0,
                    answer.usage() != null ? answer.usage().latencyMs() : 0
            );
        } catch (Exception e) {
            // 落库失败不应该阻断主流程，只记录日志
            System.err.println("写入 qa_history 失败: " + e.getMessage());
        }
    }
}