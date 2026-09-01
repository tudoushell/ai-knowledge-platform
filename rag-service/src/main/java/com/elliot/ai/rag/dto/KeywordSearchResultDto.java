package com.elliot.ai.rag.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Setter
@Getter
public class KeywordSearchResultDto {
    private UUID chunkId;
    private UUID documentId;
    private String documentName;
    private Integer chunkIndex;
    private String sectionTitle;
    private Integer pageNumber;
    private String content;
    private Double score;
}
