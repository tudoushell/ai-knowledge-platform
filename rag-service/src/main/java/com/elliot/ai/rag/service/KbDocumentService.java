package com.elliot.ai.rag.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.elliot.ai.rag.dto.IndexResultDto;
import com.elliot.ai.rag.dto.KbDocumentDetailDto;
import com.elliot.ai.rag.dto.KbDocumentDto;
import com.elliot.ai.rag.entity.KbDocument;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/** 知识库文档业务服务。 */
public interface KbDocumentService extends IService<KbDocument> {

    /**
     * 为已完成文本切分的文档生成向量，并写入向量数据库。
     *
     * @param documentId 待索引的文档唯一标识
     * @return 向量写入数量、使用的 embedding 模型和最终索引状态
     */
    IndexResultDto index(UUID documentId);

    /**
     * 上传文件并创建知识库文档记录。
     *
     * @param knowledgeBaseId 目标知识库 ID
     * @param file 待上传的文件
     * @return 创建后的知识库文档信息
     */
    KbDocumentDto upload(UUID knowledgeBaseId, MultipartFile file);

    /**
     * 查询指定知识库下的全部文档。
     *
     * @param knowledgeBaseId 知识库 ID
     * @return 按创建时间倒序排列的文档列表
     */
    List<KbDocumentDto> listDocuments(UUID knowledgeBaseId);

    /**
     * 查询指定知识库下的文档详情。
     *
     * @param knowledgeBaseId 知识库 ID
     * @param documentId      文档 ID
     * @return 文档详情
     */
    KbDocumentDetailDto getDocument(UUID knowledgeBaseId, UUID documentId);

    /**
     * 删除指定知识库下的文档及其 Chunk、向量和存储文件。
     *
     * @param knowledgeBaseId 知识库 ID
     * @param documentId 文档 ID
     */
    void deleteDocument(UUID knowledgeBaseId, UUID documentId);

    /**
     * 解析文档
     *
     * @param documentId
     * @return
     */
    KbDocument parse(UUID documentId);
}
