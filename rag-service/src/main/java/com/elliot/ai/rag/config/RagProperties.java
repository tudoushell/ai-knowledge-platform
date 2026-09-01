package com.elliot.ai.rag.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.rag.retrieval")
@Setter
@Getter
public class RagProperties {

    /**
     * Rerank 完成后最终保留的候选数量
     * <p>
     * 最终进入 RAG Context 的核心命中 Chunk
     * 最多保留该数量
     *
     */
    private int topK = 5;

    /**
     * 默认相似度阈值。
     * <p>当请求未指定阈值时，低于该阈值的向量检索结果不会进入 RAG 上下文。</p>
     */
    private double similarityThreshold = 0.5;

    /**
     * 发送给大模型的检索上下文最大字符数。
     * <p>用于限制 Prompt 长度，避免召回内容过长导致 Token 消耗和模型调用成本上升。</p>
     */
    private int maxContextChars = 20000;

    /**
     * 命中 Chunk 的相邻扩展半径。
     * <p>例如值为 1 时，计划同时读取命中 Chunk 前后各一个 Chunk，以补足上下文。
     * 当前由 ChunkContextExpansionService 的后续实现使用。</p>
     */
    @Min(0)
    @Max(3)
    private int adjacentChunkRadius = 1;

    /**
     * 单条引用资料允许携带的最大字符数。
     * <p>用于限制扩展后的来源文本长度，防止单个来源占满 RAG 上下文。</p>
     */
    @Min(1000)
    private int maxSourceChars = 8000;

    /**
     * Hybrid Search 单路召回候选数量
     * <p>
     * Vector 和 Keyword 会分别召回该数量的候选
     * 后续经过 RRF(倒数排名融合) 融合再获取最终topK
     * <p>
     * RRF 1/(k + rank)  k = 60
     *
     *
     */
    @Min(1)
    @Max(100)
    private int recallTopK = 20;

    /**
     * RRF 排名平滑常数
     * <p>
     * 值越大，不同排名之间的分数差距越平缓
     */
    @Min(1)
    private int rrfK = 60;

    /**
     * RRF 粗排后送给 Reranker 的候选数量
     *
     *
     */
    @Min(1)
    @Max(50)
    private int rerankCandidateTopK = 10;
}
