package com.elliot.ai.rag.retrieval.context;

import com.elliot.ai.rag.dto.ExpandedSource;
import com.elliot.ai.rag.retrieval.model.ContextCandidate;

public interface ChunkContextExpansionService {

    /**
     * 将一个命中的 Chunk 扩展为带相邻上下文的引用来源。
     *
     * <p>以命中 Chunk 为中心，按 {@code adjacentChunkRadius} 查询前后相邻 Chunk；
     * 返回结果保留哪一个 Chunk 是向量命中，供 Prompt 构建和前端引用详情展示。
     * 如果没有查到相邻 Chunk，或扩展结果不包含原始命中 Chunk，则回退到原始检索结果。</p>
     *
     * @param candidate 检索命中的 Chunk
     * @return 包含命中 Chunk 及其相邻上下文的引用来源
     */
    ExpandedSource expand(ContextCandidate candidate);
}
