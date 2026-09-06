package com.elliot.ai.rag.evaluation;

/**
 * 一批检索评测样本在同一 K 值下的汇总指标。
 *
 * <p>调用方应结合执行评测时传入的 K 解读各项指标，例如 K 为 5 时，
 * {@code hitRate}、{@code meanRecall} 和 {@code mrr} 分别对应
 * HitRate@5、Mean Recall@5 和 MRR@5。</p>
 *
 * @param caseCount   参与评测的 Query 样本数量
 * @param hitRate     全部样本 Hit@K 的平均值，即至少命中一个相关 Chunk 的 Query 比例
 * @param meanRecall  全部样本 Recall@K 的平均值，每个 Query 的权重相同
 * @param mrr         全部样本 RR@K 的平均值（Mean Reciprocal Rank）
 */
public record RetrievalEvaluationSummary(
        int caseCount,
        double hitRate,
        double meanRecall,
        double mrr
) {
}
