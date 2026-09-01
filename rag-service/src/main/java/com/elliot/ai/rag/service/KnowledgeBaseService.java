package com.elliot.ai.rag.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.elliot.ai.rag.entity.KnowledgeBase;

import java.util.List;
import java.util.UUID;

/** 知识库业务服务。 */
public interface KnowledgeBaseService extends IService<KnowledgeBase> {

    /**
     * 创建知识库。
     *
     * @param name 知识库名称
     * @param description 知识库描述
     */
    void create(String name, String description);

    /**
     * 查询全部知识库。
     *
     * @return 知识库列表
     */
    List<KnowledgeBase> listKnowledgeBases();

    /**
     * 根据 ID 查询知识库。
     *
     * @param id 知识库 ID
     * @return 知识库实体
     */
    KnowledgeBase getKnowledgeBase(UUID id);
}
