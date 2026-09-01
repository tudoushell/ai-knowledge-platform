package com.elliot.ai.rag.retrieval.hybrid;

import com.elliot.ai.rag.dto.HybridSearchDto;
import com.elliot.ai.rag.dto.HybridSearchRequestDto;
import com.elliot.ai.rag.retrieval.model.HybridCandidate;
import com.elliot.ai.rag.retrieval.model.RetrievalQuery;

import java.util.List;

/**
 * 知识库混合检索服务。
 *
 * <p>同时执行向量检索和关键词检索，并按文本片段 ID 合并两路召回结果，
 * 保留各自的排名和分数，供后续 RRF 等融合排序策略使用。</p>
 */
public interface HybridRetrievalService {


    /**
     * 面向接口执行混合检索。
     *
     * <p>方法会分别执行向量检索和关键词检索，并使用 RRF 将两路候选融合排序，
     * 最终返回不超过请求 {@code topK} 的结果，同时保留两路检索的排名和原始分数。</p>
     *
     * @param requestDto 知识库 ID、查询文本、最终返回数量和向量相似度阈值
     * @return 包含查询参数、召回配置和混合检索命中片段的结果
     */
    HybridSearchDto search(HybridSearchRequestDto requestDto);

    /**
     * 执行混合检索。
     *
     *                     RetrievalQuery
     *                            │
     *                            │ topK = 5
     *                            ▼
     *                 HybridRetrievalService
     *                            │
     *                recallTopK = 20
     *                     /             \
     *                    /               \
     *                   ▼                 ▼
     *           Vector Top20       Keyword Top20
     *                   \                 /
     *                    \               /
     *                     ▼             ▼
     *                     chunkId 聚合
     *                          │
     *                          ▼
     *                  HybridCandidate
     *                          │
     *              ┌───────────┴───────────┐
     *              │                       │
     *         vectorRank              keywordRank
     *              │                       │
     *              └───────────┬───────────┘
     *                          ▼
     *                         RRF
     *                          │
     *                          ▼
     *                    rrfScore
     *                          │
     *                          ▼
     *                 rrfScore DESC
     *                          │
     *                          ▼
     *             RRF Candidate TopN
     *          (rerank-candidate-top-k)
     *                          │
     *                          ▼
     *                     rrfRank
     *                     1..N
     *
     * <p>服务内部通常会使用独立的召回数量调用向量检索和关键词检索，
     * 然后将两路命中的同一文本片段合并为一个候选对象。RRF 粗排后保留
     * {@code rerank-candidate-top-k} 个候选，供后续 Rerank 使用；最终
     * {@code topK} 的截断由 Rerank 阶段负责。</p>
     *
     * @param query 包含知识库 ID、查询文本、最终 {@code topK} 和相似度阈值的统一查询对象
     * @return 经 RRF 粗排后的候选片段，数量不超过 {@code rerank-candidate-top-k}；
     * 候选中可能只包含向量检索或关键词检索的一路结果
     */
    List<HybridCandidate> retrieve(RetrievalQuery query);
}
