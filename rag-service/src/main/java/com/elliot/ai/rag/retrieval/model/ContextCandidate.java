package com.elliot.ai.rag.retrieval.model;

import java.util.UUID;

/**
 * 用于构建 RAG Context 的统一命中结果。
 * <p>
 * 上游无论来自 Vector、Hybrid 或未来 Rerank，
 * 在进入相邻 Chunk 扩展阶段前统一转换成该模型。
 */
public record ContextCandidate(
        int rank,
        /**
         * 最终检索分数
         *
         * Vector: similarity
         * Hybrid: rrfScore
         * 后续 Rerank: rerank score
         */
        Double score,
        UUID chunkId,
        UUID documentId,
        String documentName,
        Integer chunkIndex,
        String sectionTitle,
        Integer pageNumber,
        String content
) {
}
