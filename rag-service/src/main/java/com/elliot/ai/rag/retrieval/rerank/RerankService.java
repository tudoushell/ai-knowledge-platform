package com.elliot.ai.rag.retrieval.rerank;

import com.elliot.ai.rag.retrieval.model.HybridCandidate;
import com.elliot.ai.rag.retrieval.model.RerankCandidate;
import com.elliot.ai.rag.retrieval.model.RetrievalQuery;

import java.util.List;

/**
 * Rerank 精排服务
 */
public interface RerankService {
    /**
     * 对 Hybrid Retrieval 的候选结果进行精排
     *
     * @param query      原始检索请求
     * @param candidates RRF 粗排后的候选结果
     * @return Rerank 后的最终候选结果
     */
    List<RerankCandidate> rerank(RetrievalQuery query,
                                 List<HybridCandidate> candidates);
}
