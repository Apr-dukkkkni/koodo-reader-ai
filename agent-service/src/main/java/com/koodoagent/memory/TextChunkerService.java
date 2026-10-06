package com.koodoagent.memory;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TextChunkerService {

    private static final int TARGET_SIZE = 600;   // 目标块大小（字符）
    private static final int OVERLAP = 100;       // 块间重叠
    private static final int MIN_CHUNK = 50;      // 小于此长度直接丢弃

    /**
     * 把一段文本切成重叠的多个 chunk。
     */
    public List<String> chunk(String text) {
        if (text == null || text.isBlank()) return List.of();
        String normalized = text.replaceAll("\\s+", " ").trim();
        if (normalized.length() < MIN_CHUNK) return List.of();
        if (normalized.length() <= TARGET_SIZE) return List.of(normalized);

        List<String> sentences = splitBySentence(normalized);
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String sentence : sentences) {
            if (current.length() + sentence.length() > TARGET_SIZE && current.length() > 0) {
                String chunk = current.toString().trim();
                if (chunk.length() >= MIN_CHUNK) {
                    chunks.add(chunk);
                }
                // 保留尾部作为下一块的开头
                String tail = tail(current.toString(), OVERLAP);
                current.setLength(0);
                current.append(tail);
            }
            current.append(sentence);
        }

        if (current.length() >= MIN_CHUNK) {
            chunks.add(current.toString().trim());
        }

        return chunks;
    }

    /**
     * 按中英文句末标点切句。
     */
    private List<String> splitBySentence(String text) {
        List<String> sentences = new ArrayList<>();
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            buf.append(c);
            if (c == '。' || c == '！' || c == '？' || c == '；'
                    || c == '.' || c == '!' || c == '?' || c == ';'
                    || c == '\n') {
                sentences.add(buf.toString());
                buf.setLength(0);
            }
        }
        if (buf.length() > 0) {
            sentences.add(buf.toString());
        }
        return sentences;
    }

    private String tail(String s, int n) {
        if (s.length() <= n) return s;
        return s.substring(s.length() - n);
    }
}