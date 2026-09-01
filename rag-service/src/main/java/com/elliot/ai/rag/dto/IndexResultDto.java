package com.elliot.ai.rag.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class IndexResultDto {
    private UUID documentId;
    private int vectorCount;
    private String embeddingModel;
    private String status;

}
