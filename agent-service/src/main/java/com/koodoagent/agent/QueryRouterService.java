package com.koodoagent.agent;

import com.koodoagent.dto.RouteDecisionDTO;
import org.springframework.stereotype.Service;

@Service
public class QueryRouterService {

    public RouteDecisionDTO route(String term, String question, String context) {
        String q = ((term == null ? "" : term) + " " + (question == null ? "" : question)).trim();

        boolean needMemory = containsAny(q,
                "刚才", "之前", "我问过", "上面", "前面你说",
                "按我的水平", "再简单", "再深入");

        boolean needBookRag = containsAny(q,
                "书里", "前文", "作者", "这一章", "本章",
                "文中", "原文", "这段");

        boolean needWeb = containsAny(q,
                "最新", "目前", "现在", "今天", "史料", "文献",
                "历史学家", "学界", "来源", "依据", "考证");

        if (needBookRag && needWeb) {
            return new RouteDecisionDTO(
                    AgentRoute.HYBRID,
                    needMemory,
                    true,
                    true,
                    "同时需要书内证据和外部资料"
            );
        }

        if (needMemory && !needBookRag && !needWeb) {
            return new RouteDecisionDTO(
                    AgentRoute.MEMORY,
                    true,
                    false,
                    false,
                    "问题涉及历史对话或用户画像"
            );
        }

        if (needBookRag) {
            return new RouteDecisionDTO(
                    AgentRoute.BOOK_RAG,
                    needMemory,
                    true,
                    false,
                    "问题指向当前书籍内容"
            );
        }

        if (needWeb) {
            return new RouteDecisionDTO(
                    AgentRoute.WEB,
                    needMemory,
                    false,
                    true,
                    "问题需要外部可信来源"
            );
        }

        return new RouteDecisionDTO(
                AgentRoute.DIRECT,
                needMemory,
                false,
                false,
                "当前上下文已足够，默认直接回答"
        );
    }

    private boolean containsAny(String text, String... keywords) {
        if (text == null || text.isBlank()) {
            return false;
        }
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}