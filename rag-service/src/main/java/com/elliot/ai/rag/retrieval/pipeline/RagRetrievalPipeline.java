package com.elliot.ai.rag.retrieval.pipeline;

import com.elliot.ai.rag.retrieval.pipeline.model.RagRetrievalPipelineResult;

import java.util.UUID;

public interface RagRetrievalPipeline {

    /**
     * 执行完整的 RAG 检索流水线：Query Rewrite、Query Expansion、多查询混合检索、
     * 跨查询候选合并及 RRF 粗排，最后对候选进行 Rerank 精排。
     *
     * @param knowledgeBaseId     要检索的知识库 ID
     * @param originalQuery       用户输入的原始问题
     * @param topK                每个检索查询的召回数量，也是 Rerank 返回的最终最大数量
     * @param similarityThreshold 向量检索的最低相似度阈值
     * @return Rerank 后的最终候选片段；未召回任何候选时返回空列表
     */
    RagRetrievalPipelineResult retrieve(
            UUID knowledgeBaseId,
            String originalQuery,
            int topK,
            double similarityThreshold
    );
}
