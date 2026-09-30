package com.koodoagent.retrieval;

import com.koodoagent.config.AgentProperties;
import com.koodoagent.config.TavilyProperties;
import com.koodoagent.exception.AgentException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

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

    public JsoupExtractor(AgentProperties agentProps, TavilyProperties tavilyProps) {
        this.agentProps = agentProps;
        this.tavilyProps = tavilyProps;
    }

    /**
     * 抓取 URL 正文。
     * 失败抛 A005。
     */
    public String extract(String url) {
        // 在 JsoupExtractor 的 extract 方法开头，设置全局代理
        System.setProperty("socksProxyHost", tavilyProps.proxy().host());
        System.setProperty("socksProxyPort", String.valueOf(tavilyProps.proxy().port()));
        try {
            var conn = Jsoup.connect(url)   //创建网页连接，配置请求参数UA、超时、最大大小、重定向
                    .userAgent(USER_AGENT)
                    .timeout(agentProps.fetchTimeoutSeconds() * 1000)
                    .maxBodySize(agentProps.fetchMaxBodyKb() * 1024)
                    .followRedirects(true)
                    .ignoreHttpErrors(false)
                    .ignoreContentType(false);

            // 复用 Tavily 的代理配置（同一台机器的代理）
            // JsoupExtractor 中
            if (tavilyProps.proxy() != null && tavilyProps.proxy().enabled()) {
                conn = conn.proxy(tavilyProps.proxy().host(), tavilyProps.proxy().port());
            }
// enabled=false 时，不设置代理，Jsoup 走系统默认网络

            Document doc = conn.get();  //发送HTTP请求，拿到完整HTML文档 Document 对象

            // 清洗
            for (String selector : REMOVE_SELECTORS) {   // 删除噪音标签
                for (Element el : doc.select(selector)) {
                    el.remove();
                }
            }

            // 优先取 article / main，没有就取 body
            Element root = doc.selectFirst("article");
            if (root == null) root = doc.selectFirst("main");
            if (root == null) root = doc.body();
            if (root == null) {
                throw new AgentException("A005", "Empty document: " + url);
            }

            String text = root.text(); //只提取纯文本，丢掉全部 HTML 标签。

            // 折叠连续空白
            text = text.replaceAll("\\s+", " ").trim();  //把换行、制表符、多个空格全部压缩成单个空格，文本变平整，方便喂给大模型

            // 截断
            int max = agentProps.sourceMaxChars();
            if (text.length() > max) {
                text = text.substring(0, max) + "...";
            }

            if (text.isBlank()) {
                throw new AgentException("A005", "No content extracted: " + url);
            }

            return text;

        } catch (AgentException e) {
            throw e;
        } catch (Exception e) {
            throw new AgentException("A005",
                    "Fetch failed: " + url + " - " + e.getMessage());
        }
    }
}