package com.koodoagent.retrieval;

import com.koodoagent.agent.Evidence;
import com.koodoagent.agent.EvidenceType;
import com.koodoagent.config.AgentProperties;
import com.koodoagent.exception.AgentException;
import com.koodoagent.retrieval.dto.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class WebSearchService {

    private static final Logger log = LoggerFactory.getLogger(WebSearchService.class);

    private final TavilyClient tavilyClient;
    private final DomainPolicy domainPolicy;
    private final UrlSafetyChecker urlSafetyChecker;
    private final JsoupExtractor jsoupExtractor;
    private final AgentProperties props;

    public WebSearchService(TavilyClient tavilyClient,
                            DomainPolicy domainPolicy,
                            UrlSafetyChecker urlSafetyChecker,
                            JsoupExtractor jsoupExtractor,
                            AgentProperties props) {
        this.tavilyClient = tavilyClient;
        this.domainPolicy = domainPolicy;
        this.urlSafetyChecker = urlSafetyChecker;
        this.jsoupExtractor = jsoupExtractor;
        this.props = props;
    }

    /**
     * 搜索并抓取，返回统一 Evidence 列表。
     * sourceId 格式：S1, S2, S3...
     */
    public List<Evidence> searchAsEvidence(String query) {
        List<SearchResult> raw = tavilyClient.search(query);

        // 过滤 + 排序
        List<ScoredResult> candidates = raw.stream()
                .filter(this::isSafe)
                .map(r -> new ScoredResult(r, domainPolicy.evaluate(r.url())))
                .filter(sr -> sr.level() != TrustLevel.BLOCKED)
                .sorted(Comparator
                        .comparingInt((ScoredResult sr) -> sr.level().weight())
                        .reversed()
                        .thenComparing(sr -> -sr.result().score()))
                .limit(props.maxSources())
                .toList();

        // 抓取正文
        List<Evidence> evidences = new ArrayList<>();
        int idx = 1;
        for (ScoredResult sr : candidates) {
            try {
                String content = jsoupExtractor.extract(sr.result().url());
                String domain = domainPolicy.extractHost(sr.result().url());

                evidences.add(new Evidence(
                        "S" + idx++,
                        EvidenceType.WEB,
                        sr.result().title(),
                        content,
                        sr.result().url(),
                        domain,
                        sr.level().weight()
                ));
            } catch (AgentException e) {
                // 单个 URL 抓取失败不影响整体，跳过
                log.warn("Skip source {}: {}", sr.result().url(), e.getMessage());
            }
        }

        return evidences;
    }

    // 保留旧的 search() 供 search-test 用
    public List<SearchResult> search(String query) {
        return tavilyClient.search(query).stream()
                .filter(this::isSafe)
                .map(r -> new ScoredResult(r, domainPolicy.evaluate(r.url())))
                .filter(sr -> sr.level() != TrustLevel.BLOCKED)
                .sorted(Comparator
                        .comparingInt((ScoredResult sr) -> sr.level().weight())
                        .reversed()
                        .thenComparing(sr -> -sr.result().score()))
                .limit(props.maxSources())
                .map(ScoredResult::result)
                .toList();
    }

    private boolean isSafe(SearchResult r) {
        try {
            urlSafetyChecker.check(r.url());
            return true;
        } catch (AgentException e) {
            log.warn("Unsafe URL skipped: {} - {}", r.url(), e.getMessage());
            return false;
        }
    }

    private record ScoredResult(SearchResult result, TrustLevel level) {}
}