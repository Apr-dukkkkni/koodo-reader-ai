package com.koodoagent.agent;

import com.koodoagent.dto.RouteDecisionDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QueryRouterServiceTest {

    private final QueryRouterService router = new QueryRouterService();

    @Test
    void shouldRouteToMemoryWhenQuestionRefersToPreviousTurn() {
        RouteDecisionDTO d = router.route("郡县制", "刚才那个再简单讲一下", "上下文");
        assertEquals(AgentRoute.MEMORY, d.route());
        assertTrue(d.needMemory());
        assertFalse(d.needBookRag());
        assertFalse(d.needWeb());
    }

    @Test
    void shouldRouteToBookRagWhenQuestionRefersToBook() {
        RouteDecisionDTO d = router.route("王安石", "作者前文是怎么评价王安石的？", "上下文");
        assertEquals(AgentRoute.BOOK_RAG, d.route());
        assertTrue(d.needBookRag());
        assertFalse(d.needWeb());
    }

    @Test
    void shouldRouteToWebWhenQuestionNeedsExternalSource() {
        RouteDecisionDTO d = router.route("孟姜女", "孟姜女最早见于哪些文献？", "上下文");
        assertEquals(AgentRoute.WEB, d.route());
        assertTrue(d.needWeb());
    }

    @Test
    void shouldRouteToHybridWhenBookAndWebBothNeeded() {
        RouteDecisionDTO d = router.route("王安石", "作者书里对王安石的评价和史料一致吗？", "上下文");
        assertEquals(AgentRoute.HYBRID, d.route());
        assertTrue(d.needBookRag());
        assertTrue(d.needWeb());
    }

    @Test
    void shouldRouteToDirectWhenNoToolNeeded() {
        RouteDecisionDTO d = router.route("这句话", "这句话换成白话是什么意思？", "上下文");
        assertEquals(AgentRoute.DIRECT, d.route());
        assertFalse(d.needMemory());
        assertFalse(d.needBookRag());
        assertFalse(d.needWeb());
    }
}