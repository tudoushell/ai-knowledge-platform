package com.elliot.ai.rag.retrieval.model;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/**
 * Hybrid 检索过程中的候选片段。
 *
 * <p>该对象用于合并向量检索和关键词检索的结果，保留候选片段的来源信息，
 * 以及它在两路检索中的排名和原始分数，供后续 RRF 等融合算法计算最终排名。</p>
 *
 * <p>某个候选片段可能只命中其中一路检索，因此对应的另一组排名和分数可以为空。</p>
 */
@Data
@Builder
public class HybridCandidate {
    /** 文本片段 ID。 */
    private UUID chunkId;

    /** 所属文档 ID。 */
    private UUID documentId;

    /** 所属文档名称。 */
    private String documentName;

    /** 文本片段在文档中的顺序索引。 */
    private Integer chunkIndex;

    /** 文本片段所属章节标题。 */
    private String sectionTitle;

    /** 文本片段所在页码。 */
    private Integer pageNumber;

    /** 文本片段正文。 */
    private String content;

    /** 候选片段在向量检索结果中的排名，从 1 开始。 */
    private Integer vectorRank;

    /** 候选片段在向量检索中的原始相似度分数。 */
    private Double vectorScore;

    /** 候选片段在关键词检索结果中的排名，从 1 开始。 */
    private Integer keywordRank;

    /** 候选片段在关键词检索中的原始相关性分数。 */
    private Double keywordScore;

    /**
     * RRF 融合分数
     */
    private Double rrfScore;

    /**
     * RRF 排名
     */
    private Integer rrfRank;

    /**
     * Hybrid 最终排名
     */
    private Integer hybridRank;

}
