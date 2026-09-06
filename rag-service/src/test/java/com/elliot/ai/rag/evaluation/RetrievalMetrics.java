package com.elliot.ai.rag.evaluation;

/**
 * 单个 Query 在指定 K 值下的检索评测结果。
 *
 * <p>该对象不包含数据集聚合指标：多个 Query 的 {@code hit}、{@code recall} 和
 * {@code reciprocalRank} 分别取平均后，才会得到 HitRate@K、Mean Recall@K 和 MRR@K。</p>
 *
 * @param hit             Hit@K：Top K 中至少命中一个相关 Chunk 时为 {@code 1.0}，否则为 {@code 0.0}
 * @param recall          Recall@K：Top K 命中的相关 Chunk 数量除以该 Query 全部相关 Chunk 数量
 * @param reciprocalRank  RR@K（Reciprocal Rank）：第一个相关 Chunk 排名为 {@code r} 时为 {@code 1.0 / r}；
 *                        Top K 未命中相关 Chunk 时为 {@code 0.0}
 */
public record RetrievalMetrics(
        double hit,
        double recall,
        double reciprocalRank
) {
}
