package com.elliot.ai.rag.retrieval.rerank;

import com.elliot.ai.rag.retrieval.rerank.model.RerankDocument;
import com.elliot.ai.rag.retrieval.rerank.model.RerankModelResult;

import java.util.List;

/**
 * Rerank 模型调用抽象
 * <p>
 * 具体实现可以是：
 * - 第三方 Rerank API
 * - 本地 Cross-Encoder
 * - LLM Rerank
 */
public interface RerankModelClient {
    /**
     * 对 Query 与候选文档进行相关性评分。
     * <p>
     * 后期可接
     * BGE Reranker
     * Jina Reranker
     * Cohere-compatible rerank
     * 本地 Python Cross-Encoder 服务
     */
    List<RerankModelResult> rerank(
            String query,
            List<RerankDocument> documents
    );
}
