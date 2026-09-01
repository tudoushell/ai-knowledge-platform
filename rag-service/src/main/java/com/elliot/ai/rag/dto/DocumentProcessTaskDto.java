package com.elliot.ai.rag.dto;

import com.elliot.ai.rag.entity.DocumentProcessTask;
import com.elliot.ai.rag.enums.DocumentTaskStatus;
import com.elliot.ai.rag.enums.DocumentTaskStep;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 文档异步处理任务响应对象。
 *
 * <p>面向客户端返回任务进度和错误信息，不暴露 Worker 标识、锁时间、心跳等内部调度字段。</p>
 */
@Schema(description = "文档异步处理任务响应对象")
public record DocumentProcessTaskDto(
        @Schema(description = "任务 ID")
        UUID id,

        @Schema(description = "待处理文档 ID")
        UUID documentId,

        @Schema(description = "任务类型", example = "FULL_PROCESS")
        String taskType,

        @Schema(description = "任务状态")
        DocumentTaskStatus status,

        @Schema(description = "当前执行步骤")
        DocumentTaskStep currentStep,

        @Schema(description = "整体进度，范围为 0 到 100", example = "35")
        Integer progress,

        @Schema(description = "已执行重试次数", example = "0")
        Integer retryCount,

        @Schema(description = "最大重试次数", example = "3")
        Integer maxRetries,

        @Schema(description = "下次允许重试时间")
        OffsetDateTime nextRetryAt,

        @Schema(description = "失败错误码")
        String errorCode,

        @Schema(description = "失败原因")
        String errorMessage,

        @Schema(description = "实际开始执行时间")
        OffsetDateTime startedAt,

        @Schema(description = "任务结束时间")
        OffsetDateTime finishedAt,

        @Schema(description = "任务创建时间")
        OffsetDateTime createdAt,

        @Schema(description = "任务更新时间")
        OffsetDateTime updatedAt
) {

    /**
     * 将持久化任务实体转换为接口响应对象。
     *
     * @param task 文档处理任务实体
     * @return 客户端可见的任务信息
     */
    public static DocumentProcessTaskDto from(DocumentProcessTask task) {
        return new DocumentProcessTaskDto(
                task.getId(),
                task.getDocumentId(),
                task.getTaskType(),
                task.getStatus(),
                task.getCurrentStep(),
                task.getProgress(),
                task.getRetryCount(),
                task.getMaxRetries(),
                task.getNextRetryAt(),
                task.getErrorCode(),
                task.getErrorMessage(),
                task.getStartedAt(),
                task.getFinishedAt(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
