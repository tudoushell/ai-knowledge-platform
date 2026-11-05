package com.elliot.ai.rag.tool;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.elliot.ai.common.enums.ResultCode;
import com.elliot.ai.common.exception.BusinessException;
import com.elliot.ai.rag.entity.KbDocument;
import com.elliot.ai.rag.entity.KnowledgeBase;
import com.elliot.ai.rag.service.KbDocumentService;
import com.elliot.ai.rag.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeBaseTools {
    private final KbDocumentService kbDocumentService;
    private final KnowledgeBaseService knowledgeBaseService;

    @Tool(description = "根据关键词搜索文档")
    public List<String> searchDocuments(
            @ToolParam(description = "搜索关键词")
            String keyword,

            @ToolParam(
                    description = "最大结果数量",
                    required = false
            )
            Integer limit,

            ToolContext context
    ) {

        if (!StringUtils.hasText(keyword)) {
            throw new BusinessException(
                    ResultCode.PARAM_ERROR,
                    "搜索关键词不能为空"
            );
        }

        int effectiveLimit = limit == null ? 5 : limit;

        if (effectiveLimit < 1 || effectiveLimit > 20) {
            throw new BusinessException(
                    ResultCode.PARAM_ERROR,
                    "结果数量必须在 1～20 之间"
            );
        }
        return null;
    }

    @Tool(
            name = "countDocuments",
            description = "查询当前知识库中的文档总数量"
    )
    public long countDocuments(ToolContext toolContext) {
        Object object = toolContext.getContext().get("knowledgeBaseId");
        if (object == null) {
            throw new BusinessException(
                    ResultCode.PARAM_ERROR,
                    "知识库上下文缺失"
            );
        }
        if (!(object instanceof UUID)) {
            throw new BusinessException(
                    ResultCode.PARAM_ERROR,
                    "知识库上下文无效"
            );
        }
        UUID knowledgeBaseId = UUID.fromString(String.valueOf(object));

        KnowledgeBase knowledgeBase = knowledgeBaseService.getKnowledgeBase(knowledgeBaseId);
        if (knowledgeBase == null) {
            throw new BusinessException(
                    ResultCode.NOT_FOUND,
                    "知识库不存在"
            );
        }
        return kbDocumentService.count(new LambdaQueryWrapper<KbDocument>()
                .eq(KbDocument::getKnowledgeBaseId, knowledgeBaseId));
    }
}
