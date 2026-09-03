package com.elliot.ai.rag.retrieval.pipeline.model;

import java.util.List;

/**
 * RAG 检索流水线的执行轨迹。
 *
 * <p>用于记录查询改写、查询扩展、多查询合并和 Rerank 各阶段的输入或候选数量，
 * 便于日志记录、问题排查和向调用方展示检索过程。</p>
 *
 * @param originalQuery             用户输入的原始查询
 * @param rewrittenQuery            Query Rewrite 后用于后续检索的查询；未改写时通常等于原始查询
 * @param rewritten                 是否实际使用了改写后的查询
 * @param retrievalQueries          最终参与检索的查询列表，包含基础查询及其扩展查询
 * @param multiQueryCandidateCount  多查询检索并合并后的候选数量
 * @param rerankInputCount          进入 Rerank 精排前的候选数量
 * @param rerankOutCount            Rerank 精排后返回的最终候选数量
 */
public record RetrievalTrace(
        String originalQuery,
        String rewrittenQuery,
        boolean rewritten,
        List<String> retrievalQueries,
        int multiQueryCandidateCount,
        int rerankInputCount,
        int rerankOutCount
) {
}
