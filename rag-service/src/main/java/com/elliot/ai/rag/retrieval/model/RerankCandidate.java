package com.elliot.ai.rag.retrieval.model;

import java.util.UUID;

/**
 * Reranker 精排阶段的候选文本片段。
 *
 * <p>候选通常来自 RRF 粗排结果，经过 Reranker 对查询文本与片段内容的相关性
 * 重新评分后，生成最终排名。该对象同时保留 RRF 阶段的排名和分数，便于比较
 * 粗排与精排结果。</p>
 *
 * @param rerankRank   Rerank 后的最终排名，从 1 开始
 * @param rerankScore  Reranker 计算出的查询与片段相关性分数，具体取值范围由模型决定
 * @param reranked     是否真正执行并成功完成Rerank
 * @param rrfRank      RRF 粗排排名，从 1 开始；未经过 RRF 时可以为空
 * @param rrfScore     RRF 粗排融合分数；未经过 RRF 时可以为空
 * @param chunkId      文本片段 ID
 * @param documentId   所属文档 ID
 * @param documentName 所属文档名称
 * @param chunkIndex   文本片段在文档中的顺序索引
 * @param sectionTitle 文本片段所属章节标题
 * @param pageNumber   文本片段所在页码
 * @param content      文本片段正文
 */
public record RerankCandidate(
        int rerankRank,
        Double rerankScore,
        boolean reranked,
        Integer rrfRank,
        Double rrfScore,
        UUID chunkId,
        UUID documentId,
        String documentName,
        Integer chunkIndex,
        String sectionTitle,
        Integer pageNumber,
        String content
) {

    public Double effectiveScore() {
        return reranked ? rerankScore : rrfScore;
    }
}
