package com.koodoagent.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.koodoagent.dto.AgentAnswer;
import com.koodoagent.dto.AskRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import com.koodoagent.persistence.dto.QaHistoryRecordDTO;
import java.util.List;

//问答历史数据访问层（DAO/Repository），专门负责数据库 qa_history 表的读写操作，属于持久层。
@Repository
public class QaHistoryRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public QaHistoryRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }


    //把一次完整的问答（请求 + 回答）写入 qa_history 表，做全量历史归档。
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
            String route = answer.route() != null ? answer.route().name() : "DIRECT";
            String evidenceLevel = answer.evidenceLevel() != null ? answer.evidenceLevel().name() : "LOW";

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


    //按主题关键词 term 筛选，查询最近的 limit 条历史问答记录，按创建时间倒序。
    public List<QaHistoryRecordDTO> findRecentByTerm(String term, int limit) {
        if (term == null || term.isBlank()) return List.of();
        String sql = """
        SELECT id, term, question, route, evidence_level, answer_json, created_at
        FROM qa_history
        WHERE term = ?
        ORDER BY created_at DESC
        LIMIT ?
    """;
        return jdbcTemplate.query(sql, ROW_MAPPER, term.trim(), limit);
    }

    //不做筛选，直接查询全表最近的 limit 条问答记录，按创建时间倒序。
    public List<QaHistoryRecordDTO> findRecent(int limit) {
        String sql = """
        SELECT id, term, question, route, evidence_level, answer_json, created_at
        FROM qa_history
        ORDER BY created_at DESC
        LIMIT ?
    """;
        return jdbcTemplate.query(sql, ROW_MAPPER, limit);
    }

    //行映射器：把 JDBC 查询返回的 ResultSet（结果集）的每一行，转换成 QaHistoryRecordDTO Java 对象。
    private static final org.springframework.jdbc.core.RowMapper<QaHistoryRecordDTO> ROW_MAPPER =
            (rs, rowNum) -> new QaHistoryRecordDTO(
                    rs.getString("id"),
                    rs.getString("term"),
                    rs.getString("question"),
                    rs.getString("route"),
                    rs.getString("evidence_level"),
                    rs.getString("answer_json"),
                    rs.getString("created_at")
            );


    //根据问答 ID qaId，反查这条记录对应的主题关键词 term。
    public String findTermByQaId(String qaId) {
        String sql = "SELECT term FROM qa_history WHERE id = ?";
        try {
            return jdbcTemplate.queryForObject(sql, String.class, qaId);
        } catch (Exception e) {
            return null;
        }
    }
}