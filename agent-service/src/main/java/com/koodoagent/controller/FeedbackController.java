package com.koodoagent.controller;

import com.koodoagent.dto.FeedbackRequest;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/agent")
public class FeedbackController {

    @PostMapping("/feedback")
    public Map<String, Object> feedback(@RequestBody FeedbackRequest request) {

        System.out.println("收到 feedback：");
        System.out.println("qaId = " + request.qaId());
        System.out.println("rating = " + request.rating());
        System.out.println("comment = " + request.comment());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);

        return result;
    }
}