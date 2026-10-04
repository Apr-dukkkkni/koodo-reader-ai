package com.koodoagent.llm.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.logging.log4j.message.Message;

import java.util.List;

public record DeepSeekChatRequest(
        String model,
        List<Message> messages,
        boolean stream,
        Double temperature,
        @JsonProperty("response_format") ResponseFormat responseFormat

){
    public record Message(String role,String content){}
    public record ResponseFormat(String type) {}

}