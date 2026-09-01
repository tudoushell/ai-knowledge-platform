package com.elliot.ai.rag.retrieval.rerank.model;

/**
 * Rerank 模型返回的单条评分结果
 *
 * @param index 对应请求 documents 中的位置
 * @param score Query 与 Document 的相关性评分
 */
public record RerankModelResult(
        int index,
        double score
) {
}
