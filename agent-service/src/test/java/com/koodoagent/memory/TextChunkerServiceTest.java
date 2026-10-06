package com.koodoagent.memory;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TextChunkerServiceTest {

    private final TextChunkerService service = new TextChunkerService();

    @Test
    void shouldReturnEmptyForBlank() {
        assertTrue(service.chunk("").isEmpty());
        assertTrue(service.chunk("   ").isEmpty());
        assertTrue(service.chunk(null).isEmpty());
    }

    @Test
    void shouldReturnSingleChunkForShortText() {
        String text = "这是一段用于测试的短文本，它的长度需要超过最小块长度，但又明显小于目标块大小。" +
                "这里继续补充一些内容，用来验证这种文本应该直接作为一个完整的文本块返回，而不需要进一步切分。";

        List<String> chunks = service.chunk(text);

        System.out.println("short text length = " + text.length());
        System.out.println("short chunks = " + chunks);
        System.out.println("short chunk size = " + chunks.size());

        assertEquals(1, chunks.size());
        assertEquals(text, chunks.get(0));
    }

    @Test
    void shouldSplitLongTextIntoMultipleChunks() {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < 100; i++) {
            sb.append("这是第").append(i).append("句话，用来测试切分逻辑。");
        }

        List<String> chunks = service.chunk(sb.toString());

        System.out.println("long text length = " + sb.length());
        System.out.println("chunk count = " + chunks.size());

        for (int i = 0; i < chunks.size(); i++) {
            System.out.println("chunk " + i + " length = " + chunks.get(i).length());
        }

        assertTrue(chunks.size() > 1);

        for (String c : chunks) {
            assertTrue(c.length() <= 900, "chunk 过长: " + c.length());
        }
    }

    @Test
    void shouldDiscardTooShortChunk() {
        List<String> chunks = service.chunk("短。");

        System.out.println("too short chunks = " + chunks);

        assertTrue(chunks.isEmpty());
    }
}