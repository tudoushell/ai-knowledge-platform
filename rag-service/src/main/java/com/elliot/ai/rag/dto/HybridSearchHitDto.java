package com.elliot.ai.rag.dto;

import java.util.UUID;

public record HybridSearchHitDto(

        /**
         * Hybrid 最终排名。
         */
        Integer rank,

        /**
         * RRF 最终融合分数。
         */
        Double score,

        UUID chunkId,

        UUID documentId,

        String documentName,

        Integer chunkIndex,

        String sectionTitle,

        Integer pageNumber,

        String content,

        /**
         * Vector Retriever 中的排名。
         * null 表示 Vector 未命中。
         */
        Integer vectorRank,

        /**
         * Vector 原始相似度。
         */
        Double vectorScore,

        /**
         * Keyword Retriever 中的排名。
         * null 表示 Keyword 未命中。
         */
        Integer keywordRank,

        /**
         * Keyword 原始匹配分数。
         */
        Double keywordScore
) {
}