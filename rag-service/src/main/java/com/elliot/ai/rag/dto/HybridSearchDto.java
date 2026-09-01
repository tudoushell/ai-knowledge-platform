package com.elliot.ai.rag.dto;

import java.util.List;
import java.util.UUID;

public record HybridSearchDto(

        UUID knowledgeBaseId,

        String query,

        /**
         * Hybrid 最终 TopK。
         */
        int topK,

        /**
         * 每一路 Retriever 的召回数量。
         */
        int recallTopK,

        /**
         * Vector 相似度阈值。
         */
        double similarityThreshold,

        /**
         * RRF rank constant。
         */
        int rrfK,

        /**
         * 实际返回数量。
         */
        int hitCount,

        List<HybridSearchHitDto> hits
) {
}