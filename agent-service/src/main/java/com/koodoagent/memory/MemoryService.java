package com.koodoagent.memory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.koodoagent.agent.Evidence;
import com.koodoagent.agent.EvidenceType;
import com.koodoagent.dto.AgentAnswer;
import com.koodoagent.persistence.QaHistoryRepository;
import com.koodoagent.persistence.dto.QaHistoryRecordDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class MemoryService {

    private static final Logger log = LoggerFactory.getLogger(MemoryService.class);

    private final QaHistoryRepository qaHistoryRepository;
    private final ObjectMapper objectMapper;

    public MemoryService(QaHistoryRepository qaHistoryRepository, ObjectMapper objectMapper) {
        this.qaHistoryRepository = qaHistoryRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * 把历史问答包装成 Evidence，供 PromptBuilder 使用。
     * id 使用 M1/M2/... 前缀，与 Web 的 S1/S2 区分。
     */
    public List<Evidence> searchAsEvidence(String term, String question, int limit) {
        List<QaHistoryRecordDTO> rows = new ArrayList<>();

        // 1) 先按 term 精确匹配
        if (term != null && !term.isBlank()) {
            rows.addAll(qaHistoryRepository.findRecentByTerm(term, limit));
        }

        // 2) 不够则补最近 N 条（去重）
        if (rows.size() < limit) {
            Set<String> seen = new HashSet<>();
            for (QaHistoryRecordDTO r : rows) seen.add(r.id());
            for (QaHistoryRecordDTO r : qaHistoryRepository.findRecent(limit)) {
                if (rows.size() >= limit) break;
                if (seen.add(r.id())) rows.add(r);
            }
        }

        List<Evidence> evidences = new ArrayList<>();
        int idx = 1;
        for (QaHistoryRecordDTO row : rows) {
            AgentAnswer past = parseAnswer(row.answerJson());
            if (past == null) continue;

            String content = "问：" + safe(row.question())
                    + "\n答：" + safe(past.oneLine())
                    + "\n要点：" + safe(past.keyPoint());

            evidences.add(new Evidence(
                    "M" + idx,
                    EvidenceType.MEMORY,
                    "历史问答：" + safe(row.term()),
                    content,
                    "",       // MEMORY 不给前端展示 URL
                    "memory",
                    3
            ));
            idx++;
        }

        log.info("MemoryService 命中 {} 条历史记忆 (term={})", evidences.size(), term);
        return evidences;
    }

    private AgentAnswer parseAnswer(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, AgentAnswer.class);
        } catch (Exception e) {
            log.warn("解析历史 answer_json 失败: {}", e.getMessage());
            return null;
        }
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}