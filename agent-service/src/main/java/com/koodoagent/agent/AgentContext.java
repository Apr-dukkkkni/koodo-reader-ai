package com.koodoagent.agent;

import com.koodoagent.config.AgentProperties;

/**
 * 单次 /ask 的工具调用预算与计数。不可跨请求复用。
 */
public class AgentContext {

    private final AgentProperties props;

    private int toolCalls = 0;
    private int webSearchRounds = 0;

    public AgentContext(AgentProperties props) {
        this.props = props;
    }

    public boolean canCallTool() {
        return toolCalls < props.maxToolCalls();
    }

    public void recordToolCall() {
        toolCalls++;
    }

    public boolean canWebSearch() {
        return webSearchRounds < props.maxWebSearchRounds();
    }

    public void recordWebSearch() {
        webSearchRounds++;
        toolCalls++;
    }

    public int toolCalls() { return toolCalls; }
    public int webSearchRounds() { return webSearchRounds; }
}