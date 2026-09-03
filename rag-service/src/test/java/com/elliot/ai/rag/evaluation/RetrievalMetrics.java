package com.elliot.ai.rag.evaluation;

/**
 * Hit@K：对于单个 Query，若检索结果的前 K 个 Chunk 中至少包含一个相关 Chunk，
 * 则 Hit@K = 1；否则 Hit@K = 0。
 * <p>
 * 例如：前 5 个 Chunk 中出现至少一个正确 Chunk，则 Hit@5 = 1。
 * <p>
 * HitRate@K：在全部评测 Query 中，Hit@K = 1 的 Query 所占比例。
 * <p>
 * 例如：100 个 Query 中有 80 个的前 5 个 Chunk 至少命中一个正确 Chunk，
 * 则 HitRate@5 = 80 / 100 = 0.8。
 * <p>
 * Recall@K：检索结果前 K 个 Chunk 中命中的相关 Chunk 数量，
 * 占该 Query 全部相关 Chunk 数量的比例。
 * Recall@K = 前 K 个结果中的相关 Chunk 数 / 全部相关 Chunk 数
 * 例如： 相关的chunk为 A、B，召回的为 A、C、D、E、F
 * <p>
 * 则 Recall@5 = 1/2
 * <p>
 * MMR 是 Maximal Marginal Relevance（最大边际相关性）
 * 召回的chunk，相关chunk在召回chunk中的排名
 * 排名越高，RR 越大0～1
 *
 * @param hit
 * @param recall
 * @param reciprocalRank
 */
public record RetrievalMetrics(
        double hit,
        double recall,
        double reciprocalRank
) {
}
