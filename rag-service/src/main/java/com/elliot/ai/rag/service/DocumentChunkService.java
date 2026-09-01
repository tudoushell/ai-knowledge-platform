package com.elliot.ai.rag.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.elliot.ai.rag.dto.DocumentChunkPageDto;
import com.elliot.ai.rag.entity.DocumentChunk;

import java.util.List;
import java.util.UUID;

/** 文档文本片段业务服务。 */
public interface DocumentChunkService extends IService<DocumentChunk> {

    /**
     * 查询相邻的chunk
     *
     * @param documentId
     * @param startIndex
     * @param endIndex
     * @return
     */
    List<DocumentChunk> selectNeighborChunks(UUID documentId, int startIndex, int endIndex);

    /**
     * 按 Chunk 顺序分页查询文档的文本片段。
     *
     * @param documentId 文档 ID
     * @param page       页码，从 1 开始
     * @param size       每页数量
     * @return Chunk 分页结果
     */
    DocumentChunkPageDto pageChunks(UUID documentId, int page, int size);

    /**
     * 文档内容进行chunk
     *
     * @param documentId
     * @return
     */
    int chunk(UUID documentId);
}
