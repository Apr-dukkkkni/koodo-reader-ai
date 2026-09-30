package com.koodoagent.retrieval.dto;

//这个对象以后会变成 Evidence 的来源。
//统一的检索结果 DTO（数据载体）。
//不管是：
//Tavily 联网网页搜索
//本地向量库 RAG 检索文档
//都用这个同一个 SearchResult 来装一条检索到的结果。
public record SearchResult(
        String title,
        String url,
        String snippet,
        double score
) {}