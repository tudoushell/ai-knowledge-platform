package com.elliot.ai.rag.evaluation;

import java.util.List;
import java.util.Set;
import java.util.UUID;

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
 * RR@K（Reciprocal Rank）：第一个相关 Chunk 在 Top K 结果中的倒数排名。
 * <p>
 * 若第一个相关 Chunk 的排名为 r，则 RR@K = 1 / r；
 * 若 Top K 中不存在相关 Chunk，则 RR@K = 0。
 * <p>
 * 例如：第一个相关 Chunk 排名第 2，则 RR@5 = 1 / 2 = 0.5。
 * <p>
 * MRR@K（Mean Reciprocal Rank）：多个 Query 的 RR@K 平均值。
 */
public class RetrievalMetricsEvaluator {

    public RetrievalMetrics evaluate(
            List<UUID> rankedChunkIds,
            Set<UUID> relevantChunkIds,
            int k
    ) {
        int limit = Math.min(k, rankedChunkIds.size());
        List<UUID> topK = rankedChunkIds.subList(0, limit);
        //hit@k
        boolean hit = topK.stream().anyMatch(relevantChunkIds::contains);
        double hitValue = hit ? 1.0 : 0.0;
        //recall@k
        long relevantRetrievalCount = topK.stream().filter(relevantChunkIds::contains).distinct().count();
        double recall = relevantChunkIds.isEmpty() ? 0 : (double) relevantRetrievalCount / relevantChunkIds.size();
        //RR
        double reciprocalRank = 0.0;
        for (int index = 0; index < topK.size(); index++) {
            UUID chunkId = topK.get(index);
            if (relevantChunkIds.contains(chunkId)) {
                reciprocalRank = 1.0 / (index + 1);
                break;
            }
        }
        return new RetrievalMetrics(hitValue,
                recall,
                reciprocalRank);
    }

}
