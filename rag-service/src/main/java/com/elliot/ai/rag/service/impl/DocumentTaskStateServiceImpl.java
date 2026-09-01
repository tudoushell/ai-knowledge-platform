package com.elliot.ai.rag.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.elliot.ai.rag.entity.DocumentProcessTask;
import com.elliot.ai.rag.entity.KbDocument;
import com.elliot.ai.rag.enums.DocumentTaskStatus;
import com.elliot.ai.rag.enums.DocumentTaskStep;
import com.elliot.ai.rag.enums.KbDocumentStatus;
import com.elliot.ai.rag.mapper.DocumentProcessTaskMapper;
import com.elliot.ai.rag.mapper.KbDocumentMapper;
import com.elliot.ai.rag.service.DocumentTaskStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.apache.commons.lang3.StringUtils.abbreviate;

@Slf4j
@RequiredArgsConstructor
@Service
public class DocumentTaskStateServiceImpl implements DocumentTaskStateService {

    private final DocumentProcessTaskMapper taskMapper;

    private final KbDocumentMapper documentMapper;


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void advance(UUID taskId, DocumentTaskStep nextStep, int progress) {
        DocumentProcessTask task = taskMapper.selectById(taskId);
        task.setCurrentStep(nextStep);
        task.setProgress(progress);
        task.setHeartbeatAt(OffsetDateTime.now());
        taskMapper.updateById(task);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void touch(UUID taskId) {
        DocumentProcessTask task = taskMapper.selectById(taskId);
        if (task == null) {
            return;
        }
        task.setHeartbeatAt(OffsetDateTime.now());
        task.setUpdatedAt(OffsetDateTime.now());
        taskMapper.updateById(task);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void succeed(UUID taskId) {
        OffsetDateTime now = OffsetDateTime.now();
        DocumentProcessTask task = taskMapper.selectById(taskId);
        task.setStatus(DocumentTaskStatus.SUCCEEDED);
        task.setCurrentStep(DocumentTaskStep.COMPLETE);
        task.setProgress(100);
        task.setWorkerId(null);
        task.setLockedAt(null);
        task.setHeartbeatAt(now);
        task.setErrorCode(null);
        task.setErrorMessage(null);
        task.setFinishedAt(now);
        task.setUpdatedAt(now);
        taskMapper.updateById(task);

        /*
         * MyBatis-Plus 默认可能忽略值为 null 的实体字段。
         * 因此这里使用 UpdateWrapper 显式把历史错误信息更新为 NULL，
         * 避免任务已成功但接口仍返回上一次失败原因。
         */
        taskMapper.update(null, new LambdaUpdateWrapper<DocumentProcessTask>()
                .eq(DocumentProcessTask::getId, taskId)
                .set(DocumentProcessTask::getErrorCode, null)
                .set(DocumentProcessTask::getErrorMessage, null));

        // 整个处理链路成功后，文档自身也不应保留历史错误信息。
        documentMapper.update(null, new LambdaUpdateWrapper<KbDocument>()
                .eq(KbDocument::getId, task.getDocumentId())
                .set(KbDocument::getErrorMessage, null)
                .set(KbDocument::getUpdatedAt, now));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void failOrRetry(UUID taskId, Throwable exception) {
        DocumentProcessTask task = taskMapper.selectById(taskId);
        if (task == null) {
            return;
        }
        int nextRetryCount = task.getRetryCount() + 1;
        OffsetDateTime now = OffsetDateTime.now();

        task.setRetryCount(nextRetryCount);
        task.setWorkerId(null);
        task.setLockedAt(null);
        task.setHeartbeatAt(null);
        task.setErrorCode(
                exception.getClass().getSimpleName()
        );
        task.setErrorMessage(abbreviate(exception.getMessage(), 2000));
        task.setUpdatedAt(now);
        if (nextRetryCount <= task.getMaxRetries()) {
            task.setStatus(DocumentTaskStatus.RETRY_WAIT);
            long delaySeconds = calculateRetryDelay(nextRetryCount);
            task.setNextRetryAt(now.plusSeconds(delaySeconds));
        } else {
            task.setStatus(DocumentTaskStatus.FAILED);
            task.setFinishedAt(now);

            KbDocument kbDocument = documentMapper.selectById(task.getDocumentId());
            if (kbDocument != null) {
                kbDocument.setStatus(KbDocumentStatus.PROCESS_FAILED);
                kbDocument.setErrorMessage(task.getErrorMessage());
                kbDocument.setUpdatedAt(now);
                documentMapper.updateById(kbDocument);
            }
        }
        taskMapper.updateById(task);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void resetToPending(UUID taskId) {
        DocumentProcessTask task = taskMapper.selectById(taskId);
        if (task == null) {
            return;
        }
        task.setStatus(DocumentTaskStatus.PENDING);
        task.setWorkerId(null);
        task.setLockedAt(null);
        task.setHeartbeatAt(null);
        task.setNextRetryAt(null);
        task.setUpdatedAt(OffsetDateTime.now());
        taskMapper.updateById(task);
    }


    private long calculateRetryDelay(int retryCount) {
        /*
         * 第一次 10 秒
         * 第二次 20 秒
         * 第三次 40 秒
         * 最大 5 分钟
         */
        long delay = 10L * (1L << (retryCount - 1));

        return Math.min(delay, 300L);
    }
}
