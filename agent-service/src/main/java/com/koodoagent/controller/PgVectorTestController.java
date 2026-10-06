package com.koodoagent.controller;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/agent/pgvector")
public class PgVectorTestController {

    private final JdbcTemplate pgVectorJdbcTemplate;

    public PgVectorTestController(
            @Qualifier("pgVectorJdbcTemplate") JdbcTemplate pgVectorJdbcTemplate) {
        this.pgVectorJdbcTemplate = pgVectorJdbcTemplate;
    }

    @GetMapping("/ping")
    public Map<String, Object> ping() {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            Integer one = pgVectorJdbcTemplate.queryForObject("SELECT 1", Integer.class);
            result.put("connected", one != null && one == 1);

            String version = pgVectorJdbcTemplate.queryForObject(
                    "SELECT extversion FROM pg_extension WHERE extname = 'vector'",
                    String.class);
            result.put("pgvectorVersion", version);

            Integer chunkCount = pgVectorJdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM book_chunk", Integer.class);
            result.put("bookChunkCount", chunkCount);
        } catch (Exception e) {
            result.put("connected", false);
            result.put("error", e.getMessage());
        }
        return result;
    }
}