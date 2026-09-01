package com.elliot.ai.rag.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.elliot.ai.rag.entity.DocumentChunk;
import com.elliot.ai.rag.entity.KbDocument;
import com.elliot.ai.rag.mapper.DocumentChunkMapper;
import com.elliot.ai.rag.service.DocumentDerivedDataService;
import com.elliot.ai.rag.service.LocalFilesStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class DocumentDerivedDataServiceImpl implements DocumentDerivedDataService {

    private final DocumentChunkMapper documentChunkMapper;
    private final VectorStore vectorStore;
    private final LocalFilesStorageService localFilesStorageService;


    @Override
    public void clearDerivedData(KbDocument kbDocument) {
        //1. 删除pgvector
        vectorStore.delete(" documentId == '" + kbDocument.getId() + "'");
        //2. 删除chunk
        documentChunkMapper.delete(Wrappers.lambdaQuery(DocumentChunk.class)
                .eq(DocumentChunk::getDocumentId, kbDocument.getId()));
        //3. 删除解析结果
        localFilesStorageService.deleteParsed(kbDocument.getParsedStoragePath());
    }
}
