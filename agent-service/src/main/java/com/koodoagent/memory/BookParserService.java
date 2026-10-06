package com.koodoagent.memory;

import com.koodoagent.dto.ParsedBookDTO;
import com.koodoagent.dto.ParsedChapterDTO;
import com.koodoagent.exception.AgentException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class BookParserService {

    private static final Logger log = LoggerFactory.getLogger(BookParserService.class);

    private static final int MAX_CHAPTER_CHARS = 50_000;

    public ParsedBookDTO parse(Path file) {
        if (file == null || !Files.exists(file)) {
            throw new AgentException("A007", "文件不存在: " + file);
        }

        String name = file.getFileName().toString().toLowerCase();
        String title = stripExtension(file.getFileName().toString());

        if (name.endsWith(".epub")) {
            return parseEpub(file, title);
        } else if (name.endsWith(".txt")) {
            return parseTxt(file, title);
        } else if (name.endsWith(".md") || name.endsWith(".markdown")) {
            return parseMarkdown(file, title);
        }

        throw new AgentException("A007", "不支持的文件格式: " + name);
    }

    // ---------------------------------------------------------------------
    // EPUB
    // ---------------------------------------------------------------------

    private ParsedBookDTO parseEpub(Path file, String title) {
        List<ParsedChapterDTO> chapters = new ArrayList<>();

        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(file))) {
            ZipEntry entry;

            while ((entry = zip.getNextEntry()) != null) {
                String entryName = entry.getName().toLowerCase();
                if (entryName.endsWith(".xhtml") || entryName.endsWith(".html")
                        || entryName.endsWith(".htm")) {
                    if (entryName.contains("cover")
                            || entryName.contains("nav.")
                            || entryName.contains("toc")) {
                        continue;
                    }
                    String content = readEntryContent(zip, entry);
                    if (content == null || content.isBlank()) continue;

                    String plain = cleanHtml(content);
                    if (plain.isBlank()) continue;

                    chapters.add(new ParsedChapterDTO(
                            chapters.size(),
                            entry.getName(),
                            truncate(plain, MAX_CHAPTER_CHARS)
                    ));
                }
            }

            chapters.sort(Comparator.comparing(ParsedChapterDTO::title));
            List<ParsedChapterDTO> reindexed = new ArrayList<>();
            for (int i = 0; i < chapters.size(); i++) {
                ParsedChapterDTO c = chapters.get(i);
                reindexed.add(new ParsedChapterDTO(i, c.title(), c.content()));
            }
            chapters = reindexed;

        } catch (IOException e) {
            throw new AgentException("A007", "EPUB 解析失败: " + e.getMessage());
        }

        log.info("EPUB 解析完成: title={} chapters={}", title, chapters.size());
        return ParsedBookDTO.of(title, "EPUB", chapters);
    }

    private String readEntryContent(ZipInputStream zip, ZipEntry entry) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = zip.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            return out.toString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("读取 ZIP entry 失败: {} - {}", entry.getName(), e.getMessage());
            return null;
        }
    }

    private String cleanHtml(String html) {
        try {
            Document doc = Jsoup.parse(html);
            doc.select("script, style, iframe, form, nav, header, footer, .nav, .toc").remove();
            String text = doc.body() != null ? doc.body().text() : doc.text();
            return text.replaceAll("\\s+", " ").trim();
        } catch (Exception e) {
            return "";
        }
    }

    // ---------------------------------------------------------------------
    // TXT
    // ---------------------------------------------------------------------

    private ParsedBookDTO parseTxt(Path file, String title) {
        try {
            String content = Files.readString(file, StandardCharsets.UTF_8);
            if (content.startsWith("\uFEFF")) {
                content = content.substring(1);
            }
            content = content.replaceAll("\\s+", " ").trim();

            List<ParsedChapterDTO> chapters = splitBySize(content, MAX_CHAPTER_CHARS);
            log.info("TXT 解析完成: title={} chapters={} chars={}",
                    title, chapters.size(), content.length());
            return ParsedBookDTO.of(title, "TXT", chapters);
        } catch (IOException e) {
            throw new AgentException("A007", "TXT 读取失败: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------------
    // Markdown
    // ---------------------------------------------------------------------

    private ParsedBookDTO parseMarkdown(Path file, String title) {
        try {
            String content = Files.readString(file, StandardCharsets.UTF_8);
            if (content.startsWith("\uFEFF")) {
                content = content.substring(1);
            }

            List<ParsedChapterDTO> chapters = new ArrayList<>();
            String[] lines = content.split("\\r?\\n");
            StringBuilder current = new StringBuilder();
            String currentTitle = "前言";
            int index = 0;

            for (String line : lines) {
                if (line.startsWith("# ")) {
                    if (current.length() > 0) {
                        chapters.add(new ParsedChapterDTO(
                                index++, currentTitle,
                                current.toString().replaceAll("\\s+", " ").trim()
                        ));
                        current.setLength(0);
                    }
                    currentTitle = line.substring(2).trim();
                } else {
                    current.append(line).append("\n");
                }
            }
            if (current.length() > 0) {
                chapters.add(new ParsedChapterDTO(
                        index, currentTitle,
                        current.toString().replaceAll("\\s+", " ").trim()
                ));
            }

            log.info("Markdown 解析完成: title={} chapters={}", title, chapters.size());
            return ParsedBookDTO.of(title, "MD", chapters);
        } catch (IOException e) {
            throw new AgentException("A007", "Markdown 读取失败: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------------
    // 工具
    // ---------------------------------------------------------------------

    private List<ParsedChapterDTO> splitBySize(String content, int chunkSize) {
        List<ParsedChapterDTO> result = new ArrayList<>();
        if (content.isBlank()) {
            return result;
        }
        int idx = 0;
        int offset = 0;
        while (offset < content.length()) {
            int end = Math.min(offset + chunkSize, content.length());
            result.add(new ParsedChapterDTO(idx++, "part-" + (idx), content.substring(offset, end)));
            offset = end;
        }
        return result;
    }

    private String stripExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max);
    }
}
