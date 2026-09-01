package com.elliot.ai.rag.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.elliot.ai.common.enums.ResultCode;
import com.elliot.ai.common.exception.BusinessException;
import com.elliot.ai.rag.entity.KnowledgeBase;
import com.elliot.ai.rag.enums.KnowledgeBaseStatus;
import com.elliot.ai.rag.mapper.KnowledgeBaseMapper;
import com.elliot.ai.rag.service.KnowledgeBaseService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** 知识库业务服务实现。 */
@Service
public class KnowledgeBaseServiceImpl extends ServiceImpl<KnowledgeBaseMapper, KnowledgeBase>
        implements KnowledgeBaseService {

    /**
     * 创建知识库。
     *
     * <p>创建前会统一处理名称和描述，保证名称非空、长度合法，描述为空时保存为 {@code null}。</p>
     *
     * @param name 知识库名称
     * @param description 知识库描述
     */
    @Transactional(rollbackFor = Exception.class)
    public void create(String name, String description) {
        String normalizedName = normalizeName(name);
        String normalizedDescription = normalizeDescription(description);

        OffsetDateTime now = OffsetDateTime.now();

        KnowledgeBase knowledgeBase = new KnowledgeBase();
        knowledgeBase.setId(UUID.randomUUID());
        knowledgeBase.setName(normalizedName);
        knowledgeBase.setDescription(normalizedDescription);
        knowledgeBase.setStatus(
                KnowledgeBaseStatus.ENABLED
        );
        knowledgeBase.setCreatedAt(now);
        knowledgeBase.setUpdatedAt(now);
        this.save(knowledgeBase);
    }

    /**
     * 查询全部知识库。
     *
     * @return 知识库列表
     */
    public List<KnowledgeBase> listKnowledgeBases() {
        return this.list();
    }

    /**
     * 根据 ID 查询知识库。
     *
     * @param id 知识库 ID
     * @return 知识库实体
     */
    public KnowledgeBase getKnowledgeBase(UUID id) {
        KnowledgeBase knowledgeBase = this.getById(id);
        if (knowledgeBase == null) {
            throw new BusinessException(
                    ResultCode.NOT_FOUND,
                    "知识库不存在"
            );
        }
        return knowledgeBase;
    }

    /**
     * 标准化知识库名称。
     *
     * @param name 原始知识库名称
     * @return 去除首尾空白后的知识库名称
     */
    private String normalizeName(String name) {
        if (!StringUtils.hasText(name)) {
            throw new BusinessException(
                    ResultCode.PARAM_ERROR,
                    "知识库名称不能为空"
            );
        }

        String normalized = name.trim();

        if (normalized.length() > 128) {
            throw new BusinessException(
                    ResultCode.PARAM_ERROR,
                    "知识库名称不能超过 128 个字符"
            );
        }
        return normalized;
    }

    /**
     * 标准化知识库描述。
     *
     * @param description 原始知识库描述
     * @return 去除首尾空白后的知识库描述；空白描述返回 {@code null}
     */
    private String normalizeDescription(String description) {
        if (!StringUtils.hasText(description)) {
            return null;
        }

        String normalized = description.trim();

        if (normalized.length() > 500) {
            throw new BusinessException(
                    ResultCode.PARAM_ERROR,
                    "知识库描述不能超过 500 个字符"
            );
        }

        return normalized;
    }
}
