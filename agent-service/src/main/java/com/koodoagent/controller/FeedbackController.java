package com.koodoagent.controller;

import com.koodoagent.dto.FeedbackRequest;
import com.koodoagent.memory.FeedbackService;
import org.springframework.web.bind.annotation.*;
import java.util.Map;


/**
 * FeedbackController 是反馈功能的 HTTP 接口控制器（Controller 层）
 * 职责：接收前端发来的 HTTP POST 请求，把请求交给业务层 FeedbackService 处理，最后给前端返回 JSON 响应。
 * Controller 只做两件事：接收网络请求、返回响应；不写业务逻辑、不直接操作数据库。
 */
@RestController
@RequestMapping("/api/v1/agent")
public class FeedbackController {

    //业务逻辑层，真正实现反馈保存、画像更新的逻辑，构造器注入由 Spring 自动装配。
    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }


    @PostMapping("/feedback")
    public Map<String, Object> feedback(@RequestBody FeedbackRequest request) {
        feedbackService.record(request);
        return Map.of("success", true);
    }
}