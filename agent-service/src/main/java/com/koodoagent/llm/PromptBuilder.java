package com.koodoagent.llm;

import com.koodoagent.dto.AskRequest;
import org.springframework.stereotype.Component;

@Component
public class PromptBuilder {

    public String buildSystemPrompt() {
        return """
                [ROLE]
                你是私人伴读助手。请根据用户提供的划词和上下文，给出准确、通俗的解答。
                
                [OUTPUT]
                必须输出严格的 json 格式，不要输出 markdown 代码块（不要带 ```json），不要输出任何多余解释。
                json 字段必须包含：oneLine (一句话总结), explanation (详细解释), keyPoint (关键要点)。
                """;
    }

    public String buildUserPrompt(AskRequest request) {
        return """
                <CURRENT_CONTEXT>
                term: %s
                question: %s
                context: %s
                bookTitle: %s
                </CURRENT_CONTEXT>
                """.formatted(
                request.term(),
                request.question(),
                request.context(),
                request.bookTitle()
        );
    }
}