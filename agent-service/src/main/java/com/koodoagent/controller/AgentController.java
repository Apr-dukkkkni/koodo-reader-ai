package com.koodoagent.controller;

import com.koodoagent.dto.AskRequest;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/agent")
public class AgentController {

    @PostMapping("/ask")
    public Map<String, Object> ask(@RequestBody AskRequest request) {

        System.out.println("收到 Koodo 请求：" + request.question());

        Map<String, Object> result = new LinkedHashMap<>();

        result.put("qaId", UUID.randomUUID().toString());
        result.put("route", "DIRECT");
        result.put("question", request.question());
        result.put("oneLine", "这是 Spring Boot Agent 返回的测试回答。");
        result.put(
                "explanation",
                "当前还没有接入 DeepSeek，这一步只是验证 Koodo Reader 和 Agent 后端能否通信。"
        );
        result.put(
                "keyPoint",
                "如果 Koodo 能显示这段内容，说明前后端链路已经打通。"
        );
        result.put("evidenceLevel", "LOW");
        result.put("sources", List.of());

        return result;
    }
}