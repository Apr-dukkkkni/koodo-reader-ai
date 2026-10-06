package com.koodoagent.retrieval;

import com.koodoagent.config.AgentProperties;
import com.koodoagent.config.TavilyProperties;
import com.koodoagent.exception.AgentException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.net.URI;

@Service
public class JsoupExtractor {


    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/120.0 Safari/537.36";

    // 这些标签里的内容不是正文，直接删
    private static final String[] REMOVE_SELECTORS = {
            "script", "style", "noscript",
            "iframe", "form", "input", "button",
            "nav", "header", "footer",
            "aside", "advertisement", ".ad", ".ads",
            ".sidebar", ".comment", ".comments",
            ".share", ".social", ".related"
    };

    private final AgentProperties agentProps;    //本项目抓取相关配置（超时、最大文档 KB、最大字符数）
    private final TavilyProperties tavilyProps;   //TavilyProperties：读取代理配置，网页抓取和 tavily 搜索共用一套代理。
    private final UrlSafetyChecker urlSafetyChecker;

    public JsoupExtractor(AgentProperties agentProps,
                          TavilyProperties tavilyProps,
                          UrlSafetyChecker urlSafetyChecker) {
        this.agentProps = agentProps;
        this.tavilyProps = tavilyProps;
        this.urlSafetyChecker = urlSafetyChecker;
    }

    /**
     * 抓取 URL 正文。
     * 失败抛 A005。
     */
    public String extract(String url) {

        String currentUrl = url;
        int maxRedirects = 2;

        try {
            for (int redirect = 0; redirect <= maxRedirects; redirect++) {

                // 每一次真正请求前都进行 SSRF 检查
                urlSafetyChecker.check(currentUrl);

                var conn = Jsoup.connect(currentUrl)
                        .userAgent(USER_AGENT)
                        .timeout(agentProps.fetchTimeoutSeconds() * 1000)
                        .maxBodySize(agentProps.fetchMaxBodyKb() * 1024)
                        .followRedirects(false)
                        .ignoreHttpErrors(false)
                        .ignoreContentType(false);

                if (tavilyProps.proxy() != null
                        && tavilyProps.proxy().enabled()) {
                    conn = conn.proxy(
                            tavilyProps.proxy().host(),
                            tavilyProps.proxy().port()
                    );
                }

                var response = conn.execute();

                int status = response.statusCode();

                if (isRedirect(status)) {

                    if (redirect == maxRedirects) {
                        throw new AgentException(
                                "A005",
                                "Too many redirects"
                        );
                    }

                    String location = response.header("Location");

                    if (location == null || location.isBlank()) {
                        throw new AgentException(
                                "A005",
                                "Redirect without Location"
                        );
                    }

                    currentUrl = URI.create(currentUrl)
                            .resolve(location)
                            .toString();

                    // 下一轮顶部会再次：
                    // urlSafetyChecker.check(currentUrl)

                    continue;
                }

                Document doc = response.parse();

                for (String selector : REMOVE_SELECTORS) {
                    for (Element el : doc.select(selector)) {
                        el.remove();
                    }
                }

                Element root = doc.selectFirst("article");
                if (root == null) root = doc.selectFirst("main");
                if (root == null) root = doc.body();

                if (root == null) {
                    throw new AgentException(
                            "A005",
                            "Empty document: " + currentUrl
                    );
                }

                String text = root.text()
                        .replaceAll("\\s+", " ")
                        .trim();

                int max = agentProps.sourceMaxChars();

                if (text.length() > max) {
                    text = text.substring(0, max) + "...";
                }

                if (text.isBlank()) {
                    throw new AgentException(
                            "A005",
                            "No content extracted: " + currentUrl
                    );
                }

                return text;
            }

            throw new AgentException("A005", "Fetch failed");

        } catch (AgentException e) {
            throw e;
        } catch (Exception e) {
            throw new AgentException(
                    "A005",
                    "Fetch failed: " + currentUrl + " - " + e.getMessage()
            );
        }
    }

    private boolean isRedirect(int status) {
        return status == 301
                || status == 302
                || status == 303
                || status == 307
                || status == 308;
    }

}