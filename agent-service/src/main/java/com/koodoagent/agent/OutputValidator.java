package com.koodoagent.agent;

import com.koodoagent.dto.SourceDTO;
import com.koodoagent.exception.AgentException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class OutputValidator {

    /**
     * 校验 LLM 返回的 sourceIds 必须全部存在于证据集合中，
     * 并返回对应的 SourceDTO 列表。
     *
     * 规则：
     * - 引用不存在的 id → 丢弃该 id（不阻断整体，只删掉假的）
     * - 全都非法 → 返回空列表（也合法，等于没引用来源）
     * - 去重、保持顺序
     */
    public List<SourceDTO> validateAndResolve(List<String> llmSourceIds,
                                              List<Evidence> evidences) {
        if (llmSourceIds == null || llmSourceIds.isEmpty()) {
            return List.of();
        }

        Map<String, Evidence> index = evidences.stream()
                .collect(Collectors.toMap(Evidence::id, Function.identity()));

        List<SourceDTO> resolved = new ArrayList<>();
        List<String> seen = new ArrayList<>();

        for (String id : llmSourceIds) {
            if (id == null || id.isBlank()) continue;
            if (seen.contains(id)) continue;      // 去重
            seen.add(id);

            Evidence e = index.get(id);
            if (e == null) {
                // 非法 id 静默丢弃，可 log.warn
                continue;
            }
            resolved.add(new SourceDTO(
                    e.id(),
                    e.title(),
                    e.url(),
                    e.domain()
            ));
        }

        return resolved;
    }
}