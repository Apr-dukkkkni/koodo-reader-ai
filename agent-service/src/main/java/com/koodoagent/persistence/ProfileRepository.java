package com.koodoagent.persistence;

import com.koodoagent.dto.ConceptProfileDTO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ProfileRepository {

    private final JdbcTemplate jdbcTemplate;

    public ProfileRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<ConceptProfileDTO> ROW_MAPPER = (rs, rowNum) ->
            new ConceptProfileDTO(
                    rs.getLong("id"),
                    rs.getString("concept_name"),
                    rs.getInt("familiarity_level"),
                    rs.getInt("ask_count"),
                    rs.getInt("too_shallow_count"),
                    rs.getInt("just_right_count"),
                    rs.getInt("too_deep_count"),
                    rs.getString("last_seen")
            );

    public Optional<ConceptProfileDTO> findByConceptName(String conceptName) {
        String sql = """
            SELECT id, concept_name, familiarity_level, ask_count,
                   too_shallow_count, just_right_count, too_deep_count, last_seen
            FROM concept_profile
            WHERE concept_name = ?
        """;
        List<ConceptProfileDTO> rows = jdbcTemplate.query(sql, ROW_MAPPER, conceptName);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    /**
     * 不存在则插入，存在则只更新 ask_count + last_seen。
     * familiarity_level 不在这里动——由 ProfileService 根据反馈改。
     */
    public void upsertOnAsk(String conceptName) {
        String insert = """
            INSERT INTO concept_profile
                (concept_name, familiarity_level, ask_count,
                 too_shallow_count, just_right_count, too_deep_count, last_seen)
            VALUES (?, 0, 1, 0, 0, 0, CURRENT_TIMESTAMP)
            ON CONFLICT(concept_name) DO UPDATE SET
                ask_count = ask_count + 1,
                last_seen = CURRENT_TIMESTAMP
        """;
        jdbcTemplate.update(insert, conceptName);
    }

    public void updateFamiliarity(String conceptName, int level) {
        String sql = """
            UPDATE concept_profile
            SET familiarity_level = ?
            WHERE concept_name = ?
        """;
        jdbcTemplate.update(sql, level, conceptName);
    }

    private static final java.util.Set<String> ALLOWED_COLUMNS = java.util.Set.of(
            "too_shallow_count", "just_right_count", "too_deep_count"
    );

    public void deleteByConceptName(String conceptName) {
        jdbcTemplate.update("DELETE FROM concept_profile WHERE concept_name = ?", conceptName);
    }

    public void deleteFeedbackByTerm(String term) {
        jdbcTemplate.update("""
        DELETE FROM feedback
        WHERE qa_id IN (SELECT id FROM qa_history WHERE term = ?)
    """, term);
    }


    public void incrementFeedbackCounter(String conceptName, String column) {
        // column 是白名单校验过的常量，不来自用户输入
        if (!ALLOWED_COLUMNS.contains(column)) {
            throw new IllegalArgumentException("illegal column: " + column);
        }
        String sql = "UPDATE concept_profile SET " + column + " = " + column + " + 1 WHERE concept_name = ?";
        jdbcTemplate.update(sql, conceptName);
    }

    public List<ConceptProfileDTO> findAll() {
        String sql = """
            SELECT id, concept_name, familiarity_level, ask_count,
                   too_shallow_count, just_right_count, too_deep_count, last_seen
            FROM concept_profile
            ORDER BY last_seen DESC NULLS LAST
        """;
        return jdbcTemplate.query(sql, ROW_MAPPER);
    }
}