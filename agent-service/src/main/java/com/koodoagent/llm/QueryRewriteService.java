package com.koodoagent.llm;

import com.koodoagent.exception.AgentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class QueryRewriteService {

    private static final Logger log = LoggerFactory.getLogger(QueryRewriteService.class);

    private static final String SYSTEM_PROMPT = """
            你是搜索查询改写助手。用户给出一个中文问题，你需要输出一个更适合搜索引擎的查询句。
            规则：
            - 只输出查询句本身，不要解释，不要引号，不要 Markdown。
            - 保留核心实体，补充同义别名、历史文献名、学名等。
            - 长度控制在 30 字以内。
            """;

    private final DeepSeekClient deepSeekClient;

    public QueryRewriteService(DeepSeekClient deepSeekClient) {
        this.deepSeekClient = deepSeekClient;
    }

    public String rewrite(String term, String question) {
        String userPrompt = "术语：" + (term == null ? "" : term)
                + "\n问题：" + (question == null ? "" : question);

        try {
            DeepSeekResult result = deepSeekClient.chatPlain(SYSTEM_PROMPT, userPrompt);
            String rewritten = result.content() == null ? "" : result.content().trim();
            // 去掉可能被模型加上的引号
            rewritten = rewritten.replaceAll("^[\"'“”‘’]+|[\"'“”‘’]+$", "");
            if (rewritten.isBlank()) {
                return (term + " " + question).trim();
            }
            log.info("QueryRewrite: {} -> {}", question, rewritten);
            return rewritten;
        } catch (AgentException e) {
            log.warn("QueryRewrite 失败，回退原 query: {}", e.getMessage());
            return (term == null ? "" : term) + " " + (question == null ? "" : question);
        }
    }
}