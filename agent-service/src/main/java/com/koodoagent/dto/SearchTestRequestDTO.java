package com.koodoagent.dto;

import lombok.Data;

@Data
public class SearchTestRequestDTO {
    private String bookId;
    private String query;
    private Integer topK;
}
