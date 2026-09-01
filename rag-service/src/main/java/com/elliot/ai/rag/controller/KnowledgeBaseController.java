package com.elliot.ai.rag.controller;

import com.elliot.ai.common.dto.Result;
import com.elliot.ai.rag.dto.CreateKnowledgeBaseDto;
import com.elliot.ai.rag.dto.KnowledgeBaseDto;
import com.elliot.ai.rag.service.KnowledgeBaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * 知识库管理接口。
 *
 * <p>负责知识库的创建、列表查询和详情查询，统一使用 {@link Result} 封装响应。</p>
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/knowledge-bases")
@Tag(name = "知识库管理", description = "知识库的创建与维护接口")
public class KnowledgeBaseController {

    /** 知识库业务服务。 */
    private final KnowledgeBaseService knowledgeBaseService;

    /**
     * 创建知识库。
     *
     * @param knowledgeBaseDto 创建知识库请求参数
     * @return 创建成功响应
     */
    @PostMapping
    @Operation(summary = "创建知识库", description = "根据名称和描述创建一个新的知识库。")
    public Result<Void> create(@Valid @RequestBody CreateKnowledgeBaseDto knowledgeBaseDto) {
        knowledgeBaseService.create(knowledgeBaseDto.name(), knowledgeBaseDto.description());
        return Result.buildSuccess();
    }

    /**
     * 查询知识库列表。
     *
     * @return 知识库列表响应
     */
    @GetMapping
    @Operation(summary = "查询知识库列表", description = "查询所有知识库。")
    public Result<List<KnowledgeBaseDto>> list() {
        List<KnowledgeBaseDto> knowledgeBases = knowledgeBaseService.listKnowledgeBases()
                .stream()
                .map(KnowledgeBaseDto::from)
                .toList();
        return Result.buildSuccess(knowledgeBases);
    }

    /**
     * 查询知识库详情。
     *
     * @param id 知识库 ID
     * @return 知识库详情响应
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询知识库详情", description = "根据知识库 ID 查询知识库详情。")
    public Result<KnowledgeBaseDto> getById(@PathVariable("id") UUID id) {
        return Result.buildSuccess(
                KnowledgeBaseDto.from(knowledgeBaseService.getKnowledgeBase(id))
        );
    }

}
