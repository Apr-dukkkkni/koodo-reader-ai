package com.koodoagent.retrieval;

import com.koodoagent.exception.AgentException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UrlSafetyCheckerTest {

    private UrlSafetyChecker checker;

    @BeforeEach
    void setUp() {
        checker = new UrlSafetyChecker();
    }

    // ===== 应该通过 =====

    @Test
    void allowsHttpsPublicUrl() {
        assertDoesNotThrow(() -> checker.check("https://www.wikipedia.org/"));
    }

    @Test
    void allowsHttpPublicUrl() {
        assertDoesNotThrow(() -> checker.check("http://example.com/page"));
    }
//    @Test
//    void allowsPublicIpLiteral() {
//        assertDoesNotThrow(() -> checker.check("http://8.8.8.8/"));
//    }

    // ===== 协议 =====

    @Test
    void rejectsFileScheme() {
        assertThrows(AgentException.class, () -> checker.check("file:///etc/passwd"));
    }

    @Test
    void rejectsFtpScheme() {
        assertThrows(AgentException.class, () -> checker.check("ftp://example.com/file"));
    }

    @Test
    void rejectsJavascriptScheme() {
        assertThrows(AgentException.class, () -> checker.check("javascript:alert(1)"));
    }

    @Test
    void rejectsNoScheme() {
        assertThrows(AgentException.class, () -> checker.check("example.com"));
    }

    // ===== 内网 / 回环 =====

    @Test
    void rejectsLocalhost() {
        assertThrows(AgentException.class, () -> checker.check("http://localhost:8080/admin"));
    }

    @Test
    void rejectsLoopbackIp() {
        assertThrows(AgentException.class, () -> checker.check("http://127.0.0.1:8080/"));
    }

    @Test
    void rejectsPrivate10() {
        assertThrows(AgentException.class, () -> checker.check("http://10.0.0.1/"));
    }

    @Test
    void rejectsPrivate192() {
        assertThrows(AgentException.class, () -> checker.check("http://192.168.1.1/"));
    }

    @Test
    void rejectsPrivate172() {
        assertThrows(AgentException.class, () -> checker.check("http://172.16.0.1/"));
    }

    @Test
    void rejectsCgnat() {
        assertThrows(AgentException.class, () -> checker.check("http://100.64.0.1/"));
    }

    @Test
    void rejectsCloudMetadata() {
        assertThrows(AgentException.class,
                () -> checker.check("http://169.254.169.254/latest/meta-data/"));
    }

    // ===== 特殊域名 =====

    @Test
    void rejectsDotLocal() {
        assertThrows(AgentException.class, () -> checker.check("http://printer.local/"));
    }

    @Test
    void rejectsDotInternal() {
        assertThrows(AgentException.class, () -> checker.check("http://api.internal/"));
    }

    // ===== 边界 =====

    @Test
    void rejectsEmpty() {
        assertThrows(AgentException.class, () -> checker.check(""));
        assertThrows(AgentException.class, () -> checker.check(null));
    }

    @Test
    void rejectsMalformedUrl() {
        assertThrows(AgentException.class, () -> checker.check("not a url at all"));
    }
}