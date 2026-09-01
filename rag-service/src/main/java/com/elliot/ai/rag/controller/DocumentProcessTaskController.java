package com.elliot.ai.rag.controller;

import com.elliot.ai.common.dto.Result;
import com.elliot.ai.common.enums.ResultCode;
import com.elliot.ai.common.exception.BusinessException;
import com.elliot.ai.rag.dto.DocumentProcessTaskDto;
import com.elliot.ai.rag.dto.DocumentProcessTaskPageDto;
import com.elliot.ai.rag.entity.DocumentProcessTask;
import com.elliot.ai.rag.entity.KbDocument;
import com.elliot.ai.rag.service.DocumentProcessTaskService;
import com.elliot.ai.rag.service.DocumentReprocessService;
import com.elliot.ai.rag.service.KbDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * 文档异步处理任务接口。
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/knowledge-bases/{knowledgeBaseId}/documents/{documentId}/process-tasks")
@Tag(name = "文档处理任务", description = "创建并跟踪文档解析、切块、索引任务")
public class DocumentProcessTaskController {

    private final KbDocumentService kbDocumentService;
    private final DocumentProcessTaskService documentProcessTaskService;
    private final DocumentReprocessService documentReprocessService;

    /**
     * 创建文档完整处理任务。
     *
     * <p>任务依次完成解析、切块和向量索引；文档已有活动任务时，
     * 返回已有任务而不会重复创建。</p>
     *
     * @param knowledgeBaseId 文档所属知识库 ID
     * @param documentId      待处理文档 ID
     * @return 新建或已存在的活动处理任务
     */
    @PostMapping
    @Operation(summary = "创建文档处理任务", description = "为文档创建解析、切块、索引的异步处理任务；重复提交会返回已有活动任务。")
    public Result<DocumentProcessTaskDto> createFullProcessTask(
            @Parameter(
                    name = "knowledgeBaseId",
                    description = "所属知识库 ID",
                    in = ParameterIn.PATH,
                    required = true
            )
            @PathVariable("knowledgeBaseId") UUID knowledgeBaseId,
            @Parameter(
                    name = "documentId",
                    description = "待处理文档 ID",
                    in = ParameterIn.PATH,
                    required = true
            )
            @PathVariable("documentId") UUID documentId
    ) {
        KbDocument document = kbDocumentService.getById(documentId);
        if (document == null || !knowledgeBaseId.equals(document.getKnowledgeBaseId())) {
            throw new BusinessException(ResultCode.FAIL, "文档不存在或不属于该知识库");
        }
        return Result.buildSuccess(
                DocumentProcessTaskDto.from(documentProcessTaskService.createFullProcessTask(documentId))
        );
    }

    /**
     * 清理文档已有的派生数据，并重新创建完整处理任务。
     *
     * <p>会删除旧的 Chunk 和向量，重置解析、切分、索引状态，然后从解析步骤重新执行。
     * 文档存在活动任务时不允许重新处理。</p>
     *
     * @param knowledgeBaseId 文档所属知识库 ID
     * @param documentId      待重新处理的文档 ID
     * @return 新创建的完整处理任务
     */
    @PostMapping("/reprocess")
    @Operation(summary = "重新处理文档", description = "清除已有解析结果、Chunk 和向量后，重新创建完整文档处理任务。")
    public Result<DocumentProcessTaskDto> reprocess(
            @Parameter(
                    name = "knowledgeBaseId",
                    description = "所属知识库 ID",
                    in = ParameterIn.PATH,
                    required = true
            )
            @PathVariable("knowledgeBaseId") UUID knowledgeBaseId,
            @Parameter(
                    name = "documentId",
                    description = "待重新处理文档 ID",
                    in = ParameterIn.PATH,
                    required = true
            )
            @PathVariable("documentId") UUID documentId
    ) {
        KbDocument document = kbDocumentService.getById(documentId);
        if (document == null || !knowledgeBaseId.equals(document.getKnowledgeBaseId())) {
            throw new BusinessException(ResultCode.FAIL, "文档不存在或不属于该知识库");
        }
        return Result.buildSuccess(documentReprocessService.reprocess(documentId));
    }

    /**
     * 分页查询指定文档的处理任务历史。
     *
     * @param knowledgeBaseId 文档所属知识库 ID
     * @param documentId      文档 ID
     * @param page            页码，从 1 开始
     * @param size            每页数量，最大 100
     * @return 文档任务历史分页数据
     */
    @GetMapping
    @Operation(summary = "查询文档处理历史", description = "按创建时间倒序分页查询指定文档的处理任务历史。")
    public Result<DocumentProcessTaskPageDto> pageTaskHistory(
            @Parameter(name = "knowledgeBaseId", description = "所属知识库 ID", in = ParameterIn.PATH, required = true)
            @PathVariable("knowledgeBaseId") UUID knowledgeBaseId,
            @Parameter(name = "documentId", description = "文档 ID", in = ParameterIn.PATH, required = true)
            @PathVariable("documentId") UUID documentId,
            @Parameter(description = "页码，从 1 开始", example = "1")
            @RequestParam(name = "page", defaultValue = "1") int page,
            @Parameter(description = "每页数量，最大 100", example = "20")
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        KbDocument document = kbDocumentService.getById(documentId);
        if (document == null || !knowledgeBaseId.equals(document.getKnowledgeBaseId())) {
            throw new BusinessException(ResultCode.FAIL, "文档不存在或不属于该知识库");
        }
        int normalizedPage = Math.max(page, 1);
        int normalizedSize = Math.min(Math.max(size, 1), 100);
        return Result.buildSuccess(documentProcessTaskService.pageTaskHistory(
                documentId,
                normalizedPage,
                normalizedSize
        ));
    }

    /**
     * 查询指定文档处理任务的实时进度。
     *
     * @param knowledgeBaseId 文档所属知识库 ID
     * @param documentId      文档 ID
     * @param taskId          文档处理任务 ID
     * @return 任务状态、当前步骤、进度及失败信息
     */
    @GetMapping("/{taskId}")
    @Operation(summary = "查询文档处理进度", description = "查询文档处理任务的状态、当前步骤、进度和失败信息。")
    public Result<DocumentProcessTaskDto> getTaskProgress(
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
            @PathVariable("documentId") UUID documentId,
            @Parameter(
                    name = "taskId",
                    description = "文档处理任务 ID",
                    in = ParameterIn.PATH,
                    required = true
            )
            @PathVariable("taskId") UUID taskId
    ) {
        KbDocument document = kbDocumentService.getById(documentId);
        if (document == null || !knowledgeBaseId.equals(document.getKnowledgeBaseId())) {
            throw new BusinessException(ResultCode.FAIL, "文档不存在或不属于该知识库");
        }
        DocumentProcessTask task = documentProcessTaskService.getById(taskId);
        if (task == null || !documentId.equals(task.getDocumentId())) {
            throw new BusinessException(ResultCode.FAIL, "处理任务不存在或不属于该文档");
        }
        return Result.buildSuccess(DocumentProcessTaskDto.from(task));
    }
}
