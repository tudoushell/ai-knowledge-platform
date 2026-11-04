package com.elliot.ai.rag.router;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatIntentRouter {
    private final ChatClientRouter chatClientRouter;
    private static final String CHAT_INTENT_SYSTEM_MESSAGE = """
            角色：
            你是企业知识库系统的意图分类器。
            职责：
            根据用户问题，将意图分为2类
            DOCUMENT_COUNT: 仅当用户只要求获取当前知识库的文档数量时使用。
            RAG: 其他所有与知识库内容相关的问题。包含文档数量与内容分析的混合问题也归为 RAG。
            输出结构：
            只能返回以下两个值之一：
            DOCUMENT_COUNT
            RAG
            不要解释，不要输出 Markdown。

            """;

    public ChatIntent route(String modelCode,
                            String standaloneQuestion) {
        String content = chatClientRouter.get(modelCode)
                .prompt()
                .system(CHAT_INTENT_SYSTEM_MESSAGE)
                .user(standaloneQuestion)
                .call()
                .content();
        if (StringUtils.isEmpty(content)) {
            return ChatIntent.RAG;
        }
        return ChatIntent.from(content);
    }
}
