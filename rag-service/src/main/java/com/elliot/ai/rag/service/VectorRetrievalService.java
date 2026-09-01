package com.elliot.ai.rag.service;

import com.elliot.ai.rag.retrieval.model.RetrievalCandidate;
import com.elliot.ai.rag.dto.RetrievalSearchDto;
import com.elliot.ai.rag.dto.RetrievalSearchResultDto;
import com.elliot.ai.rag.retrieval.model.RetrievalQuery;
import org.springframework.ai.document.Document;

import java.util.List;

/**
 * 知识库向量检索服务。
 *
 * <p>将查询文本转换为向量，并在指定知识库的向量索引中召回语义相近的文本片段。</p>
 */
public interface VectorRetrievalService {

    /**
     * 在指定知识库内执行向量相似度检索。
     *
     * <p>先校验知识库处于启用状态，再基于 {@code knowledgeBaseId} 构造 metadata
     * 过滤条件，确保只从目标知识库的向量中召回结果。最后将命中的 Spring AI
     * {@link Document} 转换为包含排序、分数和来源信息的检索结果。</p>
     *
     * @param searchDto 查询文本、知识库 ID、返回数量和相似度阈值
     * @return 包含命中片段及其相似度分数的检索结果
     */
    RetrievalSearchResultDto search(RetrievalSearchDto searchDto);

    /**
     * 召回向量检索候选片段，供检索编排或结果融合使用。
     *
     * @param query 包含知识库、查询文本、返回数量和相似度阈值的统一查询对象
     * @return 按向量相似度排序的候选片段
     */
    List<RetrievalCandidate> retrieve(
            RetrievalQuery query
    );
}
