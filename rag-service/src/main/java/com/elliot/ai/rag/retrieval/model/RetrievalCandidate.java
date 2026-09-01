package com.elliot.ai.rag.retrieval.model;

import java.util.UUID;

/**
 * 向量检索阶段召回的候选文档片段。
 *
 * <p>记录候选片段的排序、相似度及其来源信息，供后续重排或组装检索响应使用。</p>
 *
 * @param rank          候选片段在本次检索结果中的排名，从 1 开始
 * @param score         候选片段与查询的相似度分数
 * @param chunkId       文本片段 ID
 * @param documentId    所属文档 ID
 * @param documentName  所属文档名称
 * @param chunkIndex    文本片段在文档中的顺序索引
 * @param sectionTitle  文本片段所属章节标题
 * @param pageNumber    文本片段所在页码
 * @param content       文本片段正文
 */
public record RetrievalCandidate(
        int rank,
        Double score,
        UUID chunkId,
        UUID documentId,
        String documentName,
        Integer chunkIndex,
        String sectionTitle,
        Integer pageNumber,
        String content
) {
}
