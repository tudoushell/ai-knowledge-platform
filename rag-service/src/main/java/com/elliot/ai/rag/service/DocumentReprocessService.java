package com.elliot.ai.rag.service;

import com.elliot.ai.rag.dto.DocumentProcessTaskDto;

import java.util.UUID;

public interface DocumentReprocessService {
    /**
     * 重新处理文档
     *
     * @param documentId
     * @return
     */
    DocumentProcessTaskDto reprocess(UUID documentId);
}
