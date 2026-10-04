package com.koodoagent.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.koodoagent.config.AgentProperties;
import com.koodoagent.dto.*;
import com.koodoagent.llm.DeepSeekClient;
import com.koodoagent.llm.DeepSeekResult;
import com.koodoagent.llm.PromptBuilder;
import com.koodoagent.llm.QueryRewriteService;
import com.koodoagent.memory.MemoryService;
import com.koodoagent.memory.ProfileService;
import com.koodoagent.persistence.QaHistoryRepository;
import com.koodoagent.retrieval.WebSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AgentOrchestratorServiceTest {

    private QueryRouterService queryRouterService;
    private WebSearchService webSearchService;
    private PromptBuilder promptBuilder;
    private DeepSeekClient deepSeekClient;
    private OutputValidator outputValidator;
    private QaHistoryRepository qaHistoryRepository;
    private ObjectMapper objectMapper;
    private MemoryService memoryService;
    private EvidenceJudgerService evidenceJudgerService;
    private QueryRewriteService queryRewriteService;
    private AgentProperties agentProperties;
    private ProfileService profileService;

    private AgentOrchestratorService service;

    @BeforeEach
    void setUp() {
        queryRouterService = mock(QueryRouterService.class);
        webSearchService = mock(WebSearchService.class);
        promptBuilder = mock(PromptBuilder.class);
        deepSeekClient = mock(DeepSeekClient.class);
        outputValidator = mock(OutputValidator.class);
        qaHistoryRepository = mock(QaHistoryRepository.class);
        objectMapper = new ObjectMapper();
        memoryService = mock(MemoryService.class);
        evidenceJudgerService = mock(EvidenceJudgerService.class);
        queryRewriteService = mock(QueryRewriteService.class);

        // 用真实 AgentProperties（简单 record，直接构造）
        agentProperties = new AgentProperties(
                4,     // maxToolCalls
                2,     // maxWebSearchRounds
                5,     // maxSources
                6000,  // sourceMaxChars
                20,    // fetchTimeoutSeconds
                1024,  // fetchMaxBodyKb
                null   // domainPolicy，测试里不用
        );
        profileService = mock(ProfileService.class);

        service = new AgentOrchestratorService(
                queryRouterService, webSearchService, promptBuilder,
                deepSeekClient, outputValidator, qaHistoryRepository,
                objectMapper, memoryService, evidenceJudgerService,
                queryRewriteService, agentProperties, profileService
        );
    }

    private String validJson() {
        return """
            {"oneLine":"一句话","explanation":"解释","keyPoint":"要点","sourceIds":[]}
            """;
    }

    // ---- 1. DIRECT 不调用 Tavily ----

    @Test
    void directRouteShouldNotCallWebSearch() {
        when(queryRouterService.route(any(), any(), any()))
                .thenReturn(new RouteDecisionDTO(AgentRoute.DIRECT, false, false, false, "test"));
        when(promptBuilder.buildSystemPrompt(anyInt())).thenReturn("sys");
        when(promptBuilder.buildDirectUserPrompt(any(), any(), any())).thenReturn("user");
        when(deepSeekClient.chat(anyString(), anyString()))
                .thenReturn(new DeepSeekResult(validJson(), 10, 20));

        AskRequest req = new AskRequest("term", "q", "ctx", "book", "title", "cfi");
        AgentAnswer ans = service.ask(req);

        verify(webSearchService, never()).searchAsEvidence(anyString());
        verify(memoryService, never()).searchAsEvidence(any(), any(), anyInt());
        assert ans.route() == AgentRoute.DIRECT;
    }

    // ---- 2. MEMORY 只调 MemoryService ----

    @Test
    void memoryRouteShouldCallMemoryServiceOnly() {
        when(queryRouterService.route(any(), any(), any()))
                .thenReturn(new RouteDecisionDTO(AgentRoute.MEMORY, true, false, false, "test"));
        when(memoryService.searchAsEvidence(any(), any(), anyInt())).thenReturn(List.of());
        when(promptBuilder.buildSystemPrompt(anyInt())).thenReturn("sys");
        when(promptBuilder.buildMemoryUserPrompt(any(), any(), any(), any())).thenReturn("user");
        when(deepSeekClient.chat(anyString(), anyString()))
                .thenReturn(new DeepSeekResult(validJson(), 10, 20));

        AskRequest req = new AskRequest("term", "q", "ctx", "book", "title", "cfi");
        AgentAnswer ans = service.ask(req);

        verify(webSearchService, never()).searchAsEvidence(anyString());
        verify(memoryService, times(1)).searchAsEvidence(any(), any(), anyInt());
        assert ans.route() == AgentRoute.MEMORY;
    }

    // ---- 3. WEB 证据足够 → 只搜一次 ----

    @Test
    void webRouteShouldSearchOnceWhenEvidenceEnough() {
        when(queryRouterService.route(any(), any(), any()))
                .thenReturn(new RouteDecisionDTO(AgentRoute.WEB, false, false, true, "test"));
        when(webSearchService.searchAsEvidence(anyString()))
                .thenReturn(List.of(new Evidence("S1", EvidenceType.WEB, "t", "c",
                        "https://x", "x.com", 3)));
        when(evidenceJudgerService.isEnough(any())).thenReturn(true);
        when(promptBuilder.buildSystemPrompt(anyInt())).thenReturn("sys");
        when(promptBuilder.buildUserPrompt(any(), any(), any(), any())).thenReturn("user");
        when(deepSeekClient.chat(anyString(), anyString()))
                .thenReturn(new DeepSeekResult(validJson(), 10, 20));
        when(outputValidator.validateAndResolve(any(), any())).thenReturn(List.of());

        AskRequest req = new AskRequest("term", "q", "ctx", "book", "title", "cfi");
        service.ask(req);

        verify(webSearchService, times(1)).searchAsEvidence(anyString());
        verify(queryRewriteService, never()).rewrite(any(), any());
    }

    // ---- 4. WEB 证据不足 → 触发一次二次搜索 ----

    @Test
    void webRouteShouldSearchTwiceWhenEvidenceNotEnough() {
        when(queryRouterService.route(any(), any(), any()))
                .thenReturn(new RouteDecisionDTO(AgentRoute.WEB, false, false, true, "test"));
        when(webSearchService.searchAsEvidence(anyString()))
                .thenReturn(List.of());
        when(evidenceJudgerService.isEnough(any()))
                .thenReturn(false)
                .thenReturn(true);
        when(queryRewriteService.rewrite(any(), any())).thenReturn("rewritten query");
        when(promptBuilder.buildSystemPrompt(anyInt())).thenReturn("sys");
        when(promptBuilder.buildUserPrompt(any(), any(), any(), any())).thenReturn("user");
        when(deepSeekClient.chat(anyString(), anyString()))
                .thenReturn(new DeepSeekResult(validJson(), 10, 20));
        when(outputValidator.validateAndResolve(any(), any())).thenReturn(List.of());

        AskRequest req = new AskRequest("term", "q", "ctx", "book", "title", "cfi");
        service.ask(req);

        verify(webSearchService, times(2)).searchAsEvidence(anyString());
        verify(queryRewriteService, times(1)).rewrite(any(), any());
    }

    // ---- 5. WEB 证据不足但改写无效 → 不二次搜索 ----

    @Test
    void webRouteShouldNotSearchTwiceWhenRewriteIsInvalid() {
        when(queryRouterService.route(any(), any(), any()))
                .thenReturn(new RouteDecisionDTO(AgentRoute.WEB, false, false, true, "test"));
        when(webSearchService.searchAsEvidence(anyString())).thenReturn(List.of());
        when(evidenceJudgerService.isEnough(any())).thenReturn(false);
        when(queryRewriteService.rewrite(any(), any())).thenReturn(""); // 空
        when(promptBuilder.buildSystemPrompt(anyInt())).thenReturn("sys");
        when(promptBuilder.buildUserPrompt(any(), any(), any(), any())).thenReturn("user");
        when(deepSeekClient.chat(anyString(), anyString()))
                .thenReturn(new DeepSeekResult(validJson(), 10, 20));
        when(outputValidator.validateAndResolve(any(), any())).thenReturn(List.of());

        AskRequest req = new AskRequest("term", "q", "ctx", "book", "title", "cfi");
        service.ask(req);

        verify(webSearchService, times(1)).searchAsEvidence(anyString());
    }

    // ---- 6. WEB 二次搜索仍不够 → 不再搜第三次 ----

    @Test
    void webRouteShouldStopAfterMaxRounds() {
        when(queryRouterService.route(any(), any(), any()))
                .thenReturn(new RouteDecisionDTO(AgentRoute.WEB, false, false, true, "test"));
        when(webSearchService.searchAsEvidence(anyString())).thenReturn(List.of());
        when(evidenceJudgerService.isEnough(any())).thenReturn(false); // 一直不够
        when(queryRewriteService.rewrite(any(), any())).thenReturn("rewritten");
        when(promptBuilder.buildSystemPrompt(anyInt())).thenReturn("sys");
        when(promptBuilder.buildUserPrompt(any(), any(), any(), any())).thenReturn("user");
        when(deepSeekClient.chat(anyString(), anyString()))
                .thenReturn(new DeepSeekResult(validJson(), 10, 20));
        when(outputValidator.validateAndResolve(any(), any())).thenReturn(List.of());

        AskRequest req = new AskRequest("term", "q", "ctx", "book", "title", "cfi");
        service.ask(req);

        // 上限 = 2 次，不会搜第三次
        verify(webSearchService, times(2)).searchAsEvidence(anyString());
    }
}