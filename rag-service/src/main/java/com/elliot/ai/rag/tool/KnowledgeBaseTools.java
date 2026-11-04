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
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeBaseTools {
    private final KbDocumentService kbDocumentService;
    private final KnowledgeBaseService knowledgeBaseService;

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
