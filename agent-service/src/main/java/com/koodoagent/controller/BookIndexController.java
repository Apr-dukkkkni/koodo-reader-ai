package com.koodoagent.controller;

import com.koodoagent.memory.BookIndexService;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/agent/book")
public class BookIndexController {

    private final BookIndexService bookIndexService;

    public BookIndexController(BookIndexService bookIndexService) {
        this.bookIndexService = bookIndexService;
    }

    /**
     * 索引一本书。
     * 默认行为：已有数据 → 返回缓存，不重跑。
     * 传 force=true 才强制重建。
     */
    @PostMapping("/index")
    public Map<String, Object> index(@RequestParam String bookId,
                                     @RequestParam String filePath,
                                     @RequestParam(defaultValue = "false") boolean force) {
        BookIndexService.IndexResult result =
                bookIndexService.indexBook(bookId, Path.of(filePath), force);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("bookId", result.bookId());
        response.put("title", result.title());
        response.put("chunkCount", result.chunkCount());
        response.put("elapsedMs", result.elapsedMs());
        response.put("cached", "(cached)".equals(result.title()));
        return response;
    }
}//http://127.0.0.1:8080/api/v1/agent/book/parse-test?filePath=E%3A%2FCODE%2Fcode%2Fkoodo-reader-ai%2Fagent-service%2Fsrc%2Ftest%2Fresources%2Fbooks%2Fsample.epub