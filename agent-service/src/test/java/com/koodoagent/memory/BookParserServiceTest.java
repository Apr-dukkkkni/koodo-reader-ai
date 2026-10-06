package com.koodoagent.memory;

import com.koodoagent.dto.ParsedBookDTO;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class BookParserServiceTest {

    private final BookParserService service = new BookParserService();

    private Path resource(String name) throws URISyntaxException {
        return Paths.get(getClass().getClassLoader().getResource("books/" + name).toURI());
    }

    @Test
    void shouldParseTxt() throws Exception {
        ParsedBookDTO book = service.parse(resource("sample.txt"));
        assertEquals("sample", book.title());
        assertEquals("TXT", book.format());
        assertTrue(book.chapterCount() >= 1);
        assertTrue(book.totalChars() > 0);
    }

    @Test
    void shouldParseMarkdown() throws Exception {
        ParsedBookDTO book = service.parse(resource("sample.md"));
        assertEquals("MD", book.format());
        assertEquals(3, book.chapterCount());
        assertEquals("第一章 缘起", book.chapters().get(0).title());
        assertEquals("第二章 发展", book.chapters().get(1).title());
        assertEquals("第三章 结论", book.chapters().get(2).title());
    }

    @Test
    void shouldRejectUnsupportedFormat() {
        Path fake = Path.of("nonexistent.pdf");
        assertThrows(RuntimeException.class, () -> service.parse(fake));
    }

    @Test
    void shouldParseEpub() throws Exception {
        ParsedBookDTO book = service.parse(resource("sample.epub"));
        assertEquals("EPUB", book.format());
        assertTrue(book.chapterCount() > 0);
        assertTrue(book.totalChars() > 0);
    }
}