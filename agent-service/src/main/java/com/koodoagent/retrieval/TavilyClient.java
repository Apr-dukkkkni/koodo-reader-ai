package com.koodoagent.retrieval;

import com.koodoagent.config.TavilyProperties;
import com.koodoagent.exception.AgentException;
import com.koodoagent.retrieval.dto.SearchResult;
import com.koodoagent.retrieval.dto.TavilySearchResponse;
import io.netty.channel.ChannelOption;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.ProxyProvider;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

//调用 Tavily 联网搜索的 Java 客户端 Service 类。
//职责：接收用户的查询字符串，调用 Tavily 联网搜索 API，把第三方返回结果转换成你项目内部统一的List<SearchResult>，并做校验、超时、异常处理。
@Service
public class TavilyClient {

    private final WebClient webClient;
    private final TavilyProperties props;

    public TavilyClient(TavilyProperties props) {
        this.props = props;

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000)
                .responseTimeout(Duration.ofSeconds(props.timeoutSeconds()));

        // TavilyClient 构造中
        if (props.proxy() != null && props.proxy().enabled()) {
            httpClient = httpClient.proxy(p -> p
                    .type(ProxyProvider.Proxy.HTTP)
                    .host(props.proxy().host())
                    .port(props.proxy().port()));
        }
// enabled=false 时，这段不执行，WebClient 走系统默认网络

        this.webClient = WebClient.builder()
                .baseUrl(props.baseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public List<SearchResult> search(String query) {
        String k = props.apiKey();
        System.out.println("TAVILY key prefix = "
                + (k == null ? "null" : k.substring(0, Math.min(8, k.length())))
                + ", length = " + (k == null ? 0 : k.length()));
        if (props.apiKey() == null || props.apiKey().isBlank()) {
            throw new AgentException("A003", "TAVILY_API_KEY 未配置");
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("query", query);
        body.put("search_depth", props.searchDepth());
        body.put("max_results", props.maxResults());
        body.put("include_answer", false);
        body.put("include_raw_content", false);

        try {
            TavilySearchResponse resp = webClient.post()
                    .uri("/search")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + props.apiKey())
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(TavilySearchResponse.class)
                    .timeout(Duration.ofSeconds(props.timeoutSeconds()))
                    .block();

            if (resp == null || resp.results() == null) {
                return List.of();
            }

            return resp.results().stream()
                    .map(r -> new SearchResult(
                            r.title(),
                            r.url(),
                            r.content(),
                            r.score() == null ? 0.0 : r.score()
                    ))
                    .toList();

        } catch (WebClientRequestException e){
            if(e.getCause() instanceof TimeoutException){
                throw new AgentException("A003", "Web search timeout");
            }else{
                throw new AgentException("A003", "Web search failed: " + e.getMessage());
            }
        }catch (Exception e) {
            throw new AgentException("A003", "Web search failed: " + e.getMessage());
        }
    }
}