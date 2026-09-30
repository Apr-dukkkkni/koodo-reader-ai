package com.koodoagent.controller;

import com.koodoagent.retrieval.JsoupExtractor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/agent")
public class ExtractTestController {

    private final JsoupExtractor extractor;

    public ExtractTestController(JsoupExtractor extractor) {
        this.extractor = extractor;
    }

    @GetMapping("/extract-test")
    public Map<String, Object> extractTest(@RequestParam("url") String url) {
        String text = extractor.extract(url);
        return Map.of(
                "url", url,
                "length", text.length(),
                "text", text
        );
    }
}