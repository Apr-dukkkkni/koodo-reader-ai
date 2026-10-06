package com.koodoagent.dto;

import com.koodoagent.agent.AgentRoute;

import java.util.List;

//它是整条链路的"出口"。 前面所有工作（搜索、抓取、Prompt、校验），最后都汇聚到这个对象上
public record AgentAnswer(
        String qaId,   //提交反馈时带上，用来关联这条问答
        AgentRoute route,   //显示"本次走了 Web 检索"（debug / 演示用）
        String term,        //划词的那个词，回显
        String oneLine,         //弹窗最上面的一句话结论
        String explanation,     //弹窗中间的详细解释
        String keyPoint,        //弹窗的关键点
        List<String> sourceIds,     //日志/调试用，模型原始输出的 id
        List<SourceDTO> sources,       //前端真正显示的来源列表，URL 都是真的
        EvidenceLevel evidenceLevel,    //显示证据等级（HIGH/MEDIUM/LOW）
        UsageDTO usage
) {}