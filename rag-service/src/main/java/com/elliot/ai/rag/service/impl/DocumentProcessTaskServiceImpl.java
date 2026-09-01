package com.elliot.ai.rag.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.elliot.ai.common.enums.ResultCode;
import com.elliot.ai.common.exception.BusinessException;
import com.elliot.ai.rag.dto.DocumentProcessTaskDto;
import com.elliot.ai.rag.dto.DocumentProcessTaskPageDto;
import com.elliot.ai.rag.entity.DocumentProcessTask;
import com.elliot.ai.rag.entity.KbDocument;
import com.elliot.ai.rag.enums.DocumentTaskStatus;
import com.elliot.ai.rag.enums.DocumentTaskStep;
import com.elliot.ai.rag.enums.KbDocumentStatus;
import com.elliot.ai.rag.mapper.DocumentProcessTaskMapper;
import com.elliot.ai.rag.mapper.KbDocumentMapper;
import com.elliot.ai.rag.service.DocumentProcessTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.List;

/**
 * 文档处理任务业务服务实现。
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class DocumentProcessTaskServiceImpl
        extends ServiceImpl<DocumentProcessTaskMapper, DocumentProcessTask>
        implements DocumentProcessTaskService {

    private final KbDocumentMapper documentMapper;

    /**
     * 创建文档完整处理任务，并将文档标记为待处理。
     *
     * <p>查询活动任务和创建任务处于同一事务中。并发请求同时通过首次查询时，
     * 由数据库的活动任务唯一索引兜底；捕获唯一键冲突后返回已创建的活动任务。</p>
     *
     * @param documentId 待处理文档 ID
     * @return 新建任务或已有活动任务
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public DocumentProcessTask createFullProcessTask(UUID documentId) {
        log.info("创建文档完整处理任务: documentId={}", documentId);
        KbDocument kbDocument = documentMapper.selectById(documentId);
        if (kbDocument == null) {
            log.warn("创建文档处理任务失败，文档不存在: documentId={}", documentId);
            throw new BusinessException(ResultCode.FAIL, "文档不存在");
        }
        DocumentProcessTask activeTask = baseMapper.selectActiveTask(documentId);
        if (activeTask != null) {
            log.info("文档已有活动处理任务，直接返回: documentId={}, taskId={}, status={}",
                    documentId, activeTask.getId(), activeTask.getStatus());
            return activeTask;
        }

        DocumentProcessTask task = new DocumentProcessTask();
        task.setId(UUID.randomUUID());
        task.setDocumentId(kbDocument.getId());
        task.setTaskType("FULL_PROCESS");
        task.setStatus(DocumentTaskStatus.PENDING);
        task.setCurrentStep(DocumentTaskStep.PARSE);
        task.setProgress(0);
        task.setRetryCount(0);
        task.setMaxRetries(3);
        try {
            this.save(task);
        } catch (DuplicateKeyException e) {
            log.info("并发创建文档处理任务发生唯一键冲突，查询已有活动任务: documentId={}", documentId);
            DocumentProcessTask existing = baseMapper.selectActiveTask(kbDocument.getId());
            if (existing != null) {
                log.info("返回并发创建的活动任务: documentId={}, taskId={}, status={}",
                        documentId, existing.getId(), existing.getStatus());
                return existing;
            }
            log.error("创建文档处理任务发生唯一键冲突，但未查询到活动任务: documentId={}", documentId, e);
            throw e;
        }
        kbDocument.setStatus(KbDocumentStatus.QUEUED);
        kbDocument.setErrorMessage(null);
        documentMapper.updateById(kbDocument);
        log.info("文档完整处理任务创建成功: documentId={}, taskId={}, status={}, step={}",
                documentId, task.getId(), task.getStatus(), task.getCurrentStep());
        return task;
    }

    @Override
    public DocumentProcessTaskPageDto pageTaskHistory(UUID documentId, int page, int size) {
        long total = baseMapper.countByDocumentId(documentId);
        long offset = (long) (page - 1) * size;
        List<DocumentProcessTaskDto> records = baseMapper.selectPageByDocumentId(documentId, offset, size)
                .stream()
                .map(DocumentProcessTaskDto::from)
                .toList();
        return new DocumentProcessTaskPageDto(page, size, total, records);
    }

}
