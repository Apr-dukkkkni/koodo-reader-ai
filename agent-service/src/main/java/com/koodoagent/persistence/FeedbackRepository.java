package com.koodoagent.persistence;


//专门负责把用户提交的反馈数据写入数据库的 feedback 表，是标准的 Spring JDBC 持久化代码
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository    //只做数据库读写，不包含业务逻辑，符合分层架构规范。
public class FeedbackRepository {

    private final JdbcTemplate jdbcTemplate;

    public FeedbackRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(String qaId, String rating, String comment) {
        String sql = """
            INSERT INTO feedback (qa_id, rating, comment)
            VALUES (?, ?, ?)
        """;
        jdbcTemplate.update(sql, qaId, rating, comment);
    }


    /**
     * 按 term 反查最近 N 条 feedback 的 rating，时间倒序。
     * 通过 qa_id join qa_history 拿到 term。
     */
    public List<String> findRecentRatingsByTerm(String term, int limit) {
        String sql = """
        SELECT f.rating
        FROM feedback f
        JOIN qa_history q ON f.qa_id = q.id
        WHERE q.term = ?
        ORDER BY f.created_at DESC, f.id DESC
        LIMIT ?
    """;
        return jdbcTemplate.queryForList(sql, String.class, term, limit);
    }
}