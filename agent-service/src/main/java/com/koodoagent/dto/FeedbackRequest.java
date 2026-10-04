package com.koodoagent.dto;

public record FeedbackRequest(
        String qaId,    //问答唯一标识，精准定位到 qa_history 表里的某一条问答记录。
        String rating,   //用户读这条回答的质量打分，是反馈的核心量化标准
        String comment   //补充文字评论，用户填写具体意见
) {
}