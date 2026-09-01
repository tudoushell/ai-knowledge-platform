package com.elliot.ai.rag.worker;

import com.elliot.ai.rag.entity.DocumentProcessTask;
import com.elliot.ai.rag.enums.DocumentTaskStatus;
import com.elliot.ai.rag.enums.DocumentTaskStep;
import com.elliot.ai.rag.mapper.DocumentProcessTaskMapper;
import com.elliot.ai.rag.service.DocumentChunkService;
import com.elliot.ai.rag.service.DocumentTaskStateService;
import com.elliot.ai.rag.service.KbDocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class DocumentTaskWorker {

    private final KbDocumentService documentService;
    private final DocumentProcessTaskMapper taskMapper;
    private final DocumentChunkService chunkService;
    private final DocumentTaskStateService stateService;

    public void process(UUID taskId) {
        UUID documentId = null;
        DocumentTaskStep currentStep = null;
        log.info("开始处理文档任务: taskId={}", taskId);
        try {
            while (true) {
                DocumentProcessTask task = taskMapper.selectById(taskId);
                if (task == null) {
                    log.warn("文档任务不存在，结束处理: taskId={}", taskId);
                    return;
                }
                if (!DocumentTaskStatus.RUNNING.equals(task.getStatus())) {
                    log.info("文档任务不再处于运行状态，结束处理: taskId={}, status={}", taskId, task.getStatus());
                    return;
                }
                documentId = task.getDocumentId();
                currentStep = task.getCurrentStep();
                stateService.touch(taskId);
                switch (currentStep) {
                    case PARSE -> {
                        log.info("开始解析文档: taskId={}, documentId={}", taskId, documentId);
                        documentService.parse(documentId);
                        log.info("文档解析完成: taskId={}, documentId={}", taskId, documentId);
                        stateService.advance(taskId,
                                DocumentTaskStep.CHUNK,
                                35);
                    }
                    case CHUNK -> {
                        log.info("开始切分文档: taskId={}, documentId={}", taskId, documentId);
                        int chunkCount = chunkService.chunk(documentId);
                        log.info("文档切分完成: taskId={}, documentId={}, chunkCount={}", taskId, documentId, chunkCount);
                        stateService.advance(
                                taskId,
                                DocumentTaskStep.INDEX,
                                65);
                    }
                    case INDEX -> {
                        log.info("开始生成文档向量: taskId={}, documentId={}", taskId, documentId);
                        int vectorCount = documentService.index(documentId).getVectorCount();
                        log.info("文档向量生成完成: taskId={}, documentId={}, vectorCount={}", taskId, documentId, vectorCount);
                        stateService.succeed(taskId);
                        log.info("文档任务处理完成: taskId={}, documentId={}", taskId, documentId);
                        return;
                    }
                    case COMPLETE -> {
                        log.info("文档任务已处于完成步骤，直接标记成功: taskId={}, documentId={}", taskId, documentId);
                        stateService.succeed(taskId);
                        return;
                    }
                }
            }
        } catch (Exception e) {
            log.error("文档任务处理异常，准备标记失败或等待重试: taskId={}, documentId={}, step={}",
                    taskId, documentId, currentStep, e);
            stateService.failOrRetry(taskId, e);
        }
    }
}
