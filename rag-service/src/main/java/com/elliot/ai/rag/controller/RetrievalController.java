package com.elliot.ai.rag.controller;

import com.elliot.ai.common.dto.Result;
import com.elliot.ai.rag.dto.HybridSearchDto;
import com.elliot.ai.rag.dto.HybridSearchRequestDto;
import com.elliot.ai.rag.dto.KeywordSearchDto;
import com.elliot.ai.rag.dto.KeywordSearchRequestDto;
import com.elliot.ai.rag.dto.RetrievalSearchDto;
import com.elliot.ai.rag.dto.RetrievalSearchResultDto;
import com.elliot.ai.rag.service.HybridRetrievalService;
import com.elliot.ai.rag.service.KeywordRetrievalService;
import com.elliot.ai.rag.service.VectorRetrievalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 知识库向量检索接口。
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/retrieval")
@Tag(name = "知识库检索", description = "基于查询文本从指定知识库召回相关片段")
public class RetrievalController {

    private final VectorRetrievalService vectorRetrievalService;

    private final KeywordRetrievalService keywordRetrievalService;

    private final HybridRetrievalService hybridRetrievalService;

    /**
     * 在指定知识库中执行相似度检索。
     *
     * @param searchDto 知识库 ID、查询文本和检索参数
     * @return 命中的文本片段及其相似度分数
     */
    @PostMapping("/search")
    @Operation(summary = "检索知识库", description = "根据查询文本和 metadata 过滤条件召回最相关的文档片段。")
    public Result<RetrievalSearchResultDto> search(
            @Valid @RequestBody RetrievalSearchDto searchDto
    ) {
        return Result.buildSuccess(vectorRetrievalService.search(searchDto));
    }

    /**
     * 在指定知识库中执行关键词检索。
     *
     * @param request 知识库 ID、关键词和检索数量
     * @return 命中的文本片段及其关键词匹配分数
     */
    @PostMapping("/keyword-search")
    @Operation(summary = "关键词检索知识库", description = "根据关键词从指定知识库召回最相关的文档片段。")
    public Result<KeywordSearchDto> keywordSearch(
            @Valid @RequestBody KeywordSearchRequestDto request
    ) {
        return Result.buildSuccess(keywordRetrievalService.search(request));
    }

    /**
     * 在指定知识库中执行混合检索。
     *
     * @param request 知识库 ID、查询文本、最终返回数量和相似度阈值
     * @return 向量检索与关键词检索经 RRF 融合后的命中片段
     */
    @PostMapping("/hybrid-search")
    @Operation(summary = "混合检索知识库", description = "同时使用向量检索和关键词检索，并通过 RRF 融合返回相关文档片段。")
    public Result<HybridSearchDto> hybridSearch(
            @Valid @RequestBody HybridSearchRequestDto request
    ) {
        return Result.buildSuccess(hybridRetrievalService.search(request));
    }
}
