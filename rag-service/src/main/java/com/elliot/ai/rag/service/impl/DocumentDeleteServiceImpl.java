package com.elliot.ai.rag.service.impl;

import com.elliot.ai.common.enums.ResultCode;
import com.elliot.ai.common.exception.BusinessException;
import com.elliot.ai.rag.entity.KbDocument;
import com.elliot.ai.rag.enums.KbDocumentStatus;
import com.elliot.ai.rag.mapper.DocumentProcessTaskMapper;
import com.elliot.ai.rag.mapper.KbDocumentMapper;
import com.elliot.ai.rag.service.DocumentDeleteService;
import com.elliot.ai.rag.service.DocumentDerivedDataService;
import com.elliot.ai.rag.service.LocalFilesStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class DocumentDeleteServiceImpl implements DocumentDeleteService {

    private final KbDocumentMapper documentMapper;

    private final DocumentProcessTaskMapper taskMapper;

    private final DocumentDerivedDataService derivedDataService;

    private final TransactionTemplate transactionTemplate;

    private final LocalFilesStorageService localFilesStorageService;


    @Override
    public void deleteDocument(UUID documentId) {
        KbDocument document = documentMapper.selectById(documentId);
        if (document == null) {
            return;
        }
        Long runningCount = taskMapper.countRunningTasks(documentId);
        if (runningCount != null && runningCount > 0) {
            throw new BusinessException(ResultCode.FAIL,
                    "文档正在处理中，暂时不能删除");
        }
        markDeleting(document);
        derivedDataService.clearDerivedData(document);
        localFilesStorageService.delete(document.getStoragePath());
        deleteDatabaseRecord(documentId);
    }

    private void deleteDatabaseRecord(UUID documentId) {
        transactionTemplate.executeWithoutResult(transactionStatus -> {
            documentMapper.deleteById(documentId);
        });
    }

    private void markDeleting(KbDocument document) {
        transactionTemplate.executeWithoutResult(status -> {
            OffsetDateTime now = OffsetDateTime.now();
            taskMapper.cancelWaitingTasks(document.getId(), now);
            document.setStatus(KbDocumentStatus.DELETING);
            documentMapper.updateById(document);
        });
    }
}
