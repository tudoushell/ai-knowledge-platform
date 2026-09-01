package com.elliot.ai.rag.service;

import com.elliot.ai.rag.dto.KeywordSearchDto;
import com.elliot.ai.rag.dto.KeywordSearchRequestDto;
import com.elliot.ai.rag.retrieval.model.RetrievalCandidate;
import com.elliot.ai.rag.retrieval.model.RetrievalQuery;

import java.util.List;

/**
 * 知识库关键词检索服务。
 *
 * <p>基于正文和章节标题中的关键词匹配召回文本片段，适合查找明确出现的术语、类名或配置项。</p>
 */
public interface KeywordRetrievalService {

    /**
     * 执行面向接口的关键词检索。
     *
     * @param request 知识库 ID、搜索关键词和返回数量
     * @return 包含查询信息、命中数量和命中片段的关键词检索结果
     */
    KeywordSearchDto search(KeywordSearchRequestDto request);

    /**
     * 召回关键词检索候选片段，供检索编排或结果融合使用。
     *
     * @param query 包含知识库、查询文本和检索参数的统一查询对象
     * @return 按关键词匹配结果排序的候选片段
     */
    List<RetrievalCandidate> retrieve(
            RetrievalQuery query
    );
}
