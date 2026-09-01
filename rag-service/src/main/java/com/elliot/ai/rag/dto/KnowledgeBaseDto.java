package com.elliot.ai.rag.dto;

import com.elliot.ai.rag.entity.KnowledgeBase;
import com.elliot.ai.rag.enums.KnowledgeBaseStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 知识库响应对象。
 *
 * @param id 知识库 ID
 * @param name 知识库名称
 * @param description 知识库描述
 * @param status 知识库状态
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record KnowledgeBaseDto(
        UUID id,
        String name,
        String description,
        KnowledgeBaseStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    /**
     * 将知识库实体转换为接口响应对象。
     *
     * @param knowledgeBase 知识库实体
     * @return 知识库响应对象
     */
    public static KnowledgeBaseDto from(KnowledgeBase knowledgeBase) {
        return new KnowledgeBaseDto(
                knowledgeBase.getId(),
                knowledgeBase.getName(),
                knowledgeBase.getDescription(),
                knowledgeBase.getStatus(),
                knowledgeBase.getCreatedAt(),
                knowledgeBase.getUpdatedAt()
        );
    }
}
