package com.elliot.ai.rag.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.elliot.ai.rag.enums.DocumentTaskStatus;
import com.elliot.ai.rag.enums.DocumentTaskStep;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/** 对应 {@code document_process_task} 表的文档异步处理任务。 */
@Getter
@Setter
@TableName("document_process_task")
public class DocumentProcessTask {

    /** 任务唯一标识。 */
    @TableId(value = "id", type = IdType.INPUT)
    private UUID id;

    /** 待处理文档 ID。 */
    private UUID documentId;

    /** 任务类型，例如文档解析、重建索引。 */
    private String taskType;

    /** 任务当前状态。 */
    private DocumentTaskStatus status;

    /** 任务当前执行步骤。 */
    private DocumentTaskStep currentStep;

    /** 任务整体进度，取值范围为 0 至 100。 */
    private Integer progress;

    /** 已执行的重试次数。 */
    private Integer retryCount;

    /** 允许的最大重试次数。 */
    private Integer maxRetries;

    /** 下一次允许重试的时间；未等待重试时为 {@code null}。 */
    private OffsetDateTime nextRetryAt;

    /** 当前执行该任务的 Worker 标识。 */
    private String workerId;

    /** Worker 获取任务锁的时间。 */
    private OffsetDateTime lockedAt;

    /** Worker 最近一次上报存活状态的时间。 */
    private OffsetDateTime heartbeatAt;

    /** 失败时记录的业务错误码。 */
    private String errorCode;

    /** 失败时记录的错误详情。 */
    private String errorMessage;

    /** 任务实际开始执行的时间。 */
    private OffsetDateTime startedAt;

    /** 任务结束执行的时间，无论成功、失败或取消。 */
    private OffsetDateTime finishedAt;

    /** 创建时间，插入记录时自动填充。 */
    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    /** 更新时间，插入和更新记录时自动填充。 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;
}
