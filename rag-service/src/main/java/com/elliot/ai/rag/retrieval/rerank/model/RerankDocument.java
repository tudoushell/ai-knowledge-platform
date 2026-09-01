package com.elliot.ai.rag.retrieval.rerank.model;

import java.util.UUID;

/**
 * 提交给 Rerank 模型的单个文档
 *
 * @param index
 * @param chunkId
 * @param content
 */
public record RerankDocument(
        int index,
        UUID chunkId,
        String content
) {
}
