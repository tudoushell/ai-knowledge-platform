package com.elliot.ai.rag.controller;

import com.elliot.ai.common.dto.Result;
import com.elliot.ai.common.enums.ResultCode;
import com.elliot.ai.common.exception.BusinessException;
import com.elliot.ai.rag.dto.IndexResultDto;
import com.elliot.ai.rag.dto.DocumentChunkPageDto;
import com.elliot.ai.rag.dto.KbDocumentDetailDto;
import com.elliot.ai.rag.dto.KbDocumentDto;
import com.elliot.ai.rag.entity.KbDocument;
import com.elliot.ai.rag.service.DocumentChunkService;
import com.elliot.ai.rag.service.DocumentDeleteService;
import com.elliot.ai.rag.service.KbDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * 知识库文档管理接口。
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/knowledge-bases/{knowledgeBaseId}/documents")
@Tag(name = "知识库文档管理", description = "知识库文档的上传与维护接口")
public class KbDocumentController {

    private final KbDocumentService kbDocumentService;
    private final DocumentChunkService documentChunkService;
    private final DocumentDeleteService documentDeleteService;

    /**
     * 查询指定知识库下的文档列表。
     *
     * @param knowledgeBaseId 知识库 ID
     * @return 文档列表
     */
    @GetMapping
    @Operation(summary = "查询知识库文档列表", description = "按创建时间倒序查询指定知识库下的全部文档。")
    public Result<List<KbDocumentDto>> list(
            @Parameter(
                    name = "knowledgeBaseId",
                    description = "知识库 ID",
                    in = ParameterIn.PATH,
                    required = true
            )
            @PathVariable("knowledgeBaseId") UUID knowledgeBaseId
    ) {
        return Result.buildSuccess(kbDocumentService.listDocuments(knowledgeBaseId));
    }

    /**
     * 查询指定知识库下的文档详情。
     *
     * @param knowledgeBaseId 所属知识库 ID
     * @param documentId      文档 ID
     * @return 文档详情
     */
    @GetMapping("/{documentId}")
    @Operation(summary = "查询知识库文档详情", description = "根据知识库 ID 和文档 ID 查询文档详情。")
    public Result<KbDocumentDetailDto> getDocument(
            @Parameter(
                    name = "knowledgeBaseId",
                    description = "所属知识库 ID",
                    in = ParameterIn.PATH,
                    required = true
            )
            @PathVariable("knowledgeBaseId") UUID knowledgeBaseId,
            @Parameter(
                    name = "documentId",
                    description = "文档 ID",
                    in = ParameterIn.PATH,
                    required = true
            )
            @PathVariable("documentId") UUID documentId
    ) {
        return Result.buildSuccess(kbDocumentService.getDocument(knowledgeBaseId, documentId));
    }

    /**
     * 按 Chunk 顺序分页查询文档内容。
     *
     * @param knowledgeBaseId 所属知识库 ID
     * @param documentId      文档 ID
     * @param page            页码，从 1 开始
     * @param size            每页数量，最大 100
     * @return Chunk 分页数据
     */
    @GetMapping("/{documentId}/chunks")
    @Operation(summary = "分页查询文档 Chunk", description = "按 Chunk 在原文中的顺序分页返回文本片段。")
    public Result<DocumentChunkPageDto> pageChunks(
            @Parameter(name = "knowledgeBaseId", description = "所属知识库 ID", in = ParameterIn.PATH, required = true)
            @PathVariable("knowledgeBaseId") UUID knowledgeBaseId,
            @Parameter(name = "documentId", description = "文档 ID", in = ParameterIn.PATH, required = true)
            @PathVariable("documentId") UUID documentId,
            @Parameter(description = "页码，从 1 开始", example = "1")
            @RequestParam(name = "page", defaultValue = "1") int page,
            @Parameter(description = "每页数量，最大 100", example = "20")
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        kbDocumentService.getDocument(knowledgeBaseId, documentId);
        int normalizedPage = Math.max(page, 1);
        int normalizedSize = Math.min(Math.max(size, 1), 100);
        return Result.buildSuccess(documentChunkService.pageChunks(documentId, normalizedPage, normalizedSize));
    }

    /**
     * 向指定知识库上传文档。
     *
     * @param knowledgeBaseId 目标知识库 ID
     * @param file            待上传的 TXT、Markdown、PDF 或 DOCX 文件
     * @return 已创建的知识库文档信息
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传知识库文档", description = "向指定知识库上传 TXT、Markdown、PDF 或 DOCX 文档。")
    public Result<KbDocumentDto> upload(
            @Parameter(
                    name = "knowledgeBaseId",
                    description = "目标知识库 ID",
                    in = ParameterIn.PATH,
                    required = true
            )
            @PathVariable("knowledgeBaseId") UUID knowledgeBaseId,
            @Parameter(description = "文件") @RequestPart("file") MultipartFile file
    ) {
        return Result.buildSuccess(kbDocumentService.upload(knowledgeBaseId, file));
    }

    /**
     * 将已解析的文档按 Token 切分为多个文本片段。
     *
     * @param knowledgeBaseId 所属知识库 ID
     * @param documentId      待切分的文档 ID
     * @return 实际生成并保存的 Chunk 数量
     */
    @PostMapping("/{documentId}/chunks")
    @Operation(summary = "切分知识库文档", description = "读取已解析文本，按 Token 切分并保存文档片段。")
    public Result<Integer> chunk(
            @Parameter(
                    name = "knowledgeBaseId",
                    description = "所属知识库 ID",
                    in = ParameterIn.PATH,
                    required = true
            )
            @PathVariable("knowledgeBaseId") UUID knowledgeBaseId,
            @Parameter(
                    name = "documentId",
                    description = "待切分的文档 ID",
                    in = ParameterIn.PATH,
                    required = true
            )
            @PathVariable("documentId") UUID documentId
    ) {
        KbDocument document = kbDocumentService.getById(documentId);
        if (document == null || !knowledgeBaseId.equals(document.getKnowledgeBaseId())) {
            throw new BusinessException(ResultCode.FAIL, "文档不存在或不属于该知识库");
        }
        return Result.buildSuccess(documentChunkService.chunk(documentId));
    }

    /**
     * 为已切分的文档生成向量并写入向量库。
     *
     * @param knowledgeBaseId 所属知识库 ID
     * @param documentId      待索引的文档 ID
     * @return 向量写入结果
     */
    @PostMapping("/{documentId}/index")
    @Operation(summary = "索引知识库文档", description = "为已切分的文本生成向量，并写入向量数据库。")
    public Result<IndexResultDto> index(
            @Parameter(
                    name = "knowledgeBaseId",
                    description = "所属知识库 ID",
                    in = ParameterIn.PATH,
                    required = true
            )
            @PathVariable("knowledgeBaseId") UUID knowledgeBaseId,
            @Parameter(
                    name = "documentId",
                    description = "待索引的文档 ID",
                    in = ParameterIn.PATH,
                    required = true
            )
            @PathVariable("documentId") UUID documentId
    ) {
        KbDocument document = kbDocumentService.getById(documentId);
        if (document == null || !knowledgeBaseId.equals(document.getKnowledgeBaseId())) {
            throw new BusinessException(ResultCode.FAIL, "文档不存在或不属于该知识库");
        }
        return Result.buildSuccess(kbDocumentService.index(documentId));
    }

    /**
     * 删除指定知识库下的文档。
     *
     * @param knowledgeBaseId 所属知识库 ID
     * @param documentId 待删除的文档 ID
     * @return 删除成功响应
     */
    @DeleteMapping("/{documentId}")
    @Operation(summary = "删除知识库文档", description = "删除文档记录及关联的 Chunk、向量和存储文件。")
    public Result<Void> delete(
            @Parameter(
                    name = "knowledgeBaseId",
                    description = "所属知识库 ID",
                    in = ParameterIn.PATH,
                    required = true
            )
            @PathVariable("knowledgeBaseId") UUID knowledgeBaseId,
            @Parameter(
                    name = "documentId",
                    description = "待删除的文档 ID",
                    in = ParameterIn.PATH,
                    required = true
            )
            @PathVariable("documentId") UUID documentId
    ) {
        documentDeleteService.deleteDocument(documentId);
        return Result.buildSuccess();
    }
}
