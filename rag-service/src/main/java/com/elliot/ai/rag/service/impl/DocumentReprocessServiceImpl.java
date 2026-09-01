package com.elliot.ai.rag.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.elliot.ai.common.enums.ResultCode;
import com.elliot.ai.common.exception.BusinessException;
import com.elliot.ai.rag.dto.DocumentProcessTaskDto;
import com.elliot.ai.rag.entity.DocumentProcessTask;
import com.elliot.ai.rag.entity.KbDocument;
import com.elliot.ai.rag.enums.KbDocumentStatus;
import com.elliot.ai.rag.mapper.DocumentProcessTaskMapper;
import com.elliot.ai.rag.mapper.KbDocumentMapper;
import com.elliot.ai.rag.service.DocumentDerivedDataService;
import com.elliot.ai.rag.service.DocumentProcessTaskService;
import com.elliot.ai.rag.service.DocumentReprocessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class DocumentReprocessServiceImpl implements DocumentReprocessService {

    private final KbDocumentMapper documentMapper;

    private final DocumentProcessTaskMapper taskMapper;

    private final DocumentDerivedDataService derivedDataService;

    private final DocumentProcessTaskService taskService;

    private final TransactionTemplate transactionTemplate;
    

    @Override
    public DocumentProcessTaskDto reprocess(UUID documentId) {
        KbDocument document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new BusinessException(
                    ResultCode.NOT_FOUND,
                    "文档不存在：" + documentId
            );
        }
        DocumentProcessTask activeTask = taskMapper.selectActiveTask(documentId);
        if (activeTask != null) {
            throw new BusinessException(
                    ResultCode.FAIL,
                    "文档当前存在未完成的处理任务"
            );
        }
        //清除派生数据
        derivedDataService.clearDerivedData(document);
        resetDocument(document);
        return DocumentProcessTaskDto.from(taskService.createFullProcessTask(documentId));
    }

    private void resetDocument(KbDocument document) {
        transactionTemplate.executeWithoutResult(transactionStatus -> {
            documentMapper.update(null, new LambdaUpdateWrapper<KbDocument>()
                    .eq(KbDocument::getId, document.getId())
                    .set(KbDocument::getStatus, KbDocumentStatus.UPLOADED)
                    .set(KbDocument::getParsedStoragePath, null)
                    .set(KbDocument::getParsedFormat, null)
                    .set(KbDocument::getParsedPreview, null)
                    .set(KbDocument::getParsedCharCount, null)
                    .set(KbDocument::getParsedBlockCount, 0)
                    .set(KbDocument::getPageCount, null)
                    .set(KbDocument::getChunkCount, 0)
                    .set(KbDocument::getChunkStrategy, null)
                    .set(KbDocument::getChunkSize, null)
                    .set(KbDocument::getChunkOverlap, 0)
                    .set(KbDocument::getVectorCount, 0)
                    .set(KbDocument::getEmbeddingModel, null)
                    .set(KbDocument::getIndexedAt, null)
                    .set(KbDocument::getErrorMessage, null)
                    .set(KbDocument::getUpdatedAt, OffsetDateTime.now()));
        });

    }
}
