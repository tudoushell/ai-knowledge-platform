package com.elliot.ai.rag.query.conversation;

import com.elliot.ai.rag.query.conversation.config.ConversationQueryRewriteProperties;
import com.elliot.ai.rag.query.conversation.model.ConversationMessage;
import com.elliot.ai.rag.query.conversation.model.ConversationQueryRewriteResult;
import com.elliot.ai.rag.query.conversation.prompt.ConversationQueryRewritePrompt;
import com.elliot.ai.rag.router.ChatClientRouter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationQueryRewriteServiceImpl implements ConversationQueryRewriteService {

    private final ChatClientRouter chatClientRouter;

    private final ConversationQueryRewriteProperties properties;


    @Override
    public ConversationQueryRewriteResult rewrite(List<ConversationMessage> history, String currentQuery) {
        if (!properties.enabled()) {
            return fallback(currentQuery);
        }
        if (history == null || history.isEmpty()) {
            return fallback(currentQuery);
        }
        //构建会话历史记录
        String historyText = buildHistoryText(history);
        String userPrompt = """
                   历史对话：
                   %s
                   用户问题：
                   %s
                   请输出改写后的独立问题。
                """.formatted(historyText, currentQuery);
        try {
            String standaloneQuery = chatClientRouter.get(properties.modelCode())
                     .prompt()
                    .system(ConversationQueryRewritePrompt.SYSTEM_PROMPT)
                    .user(userPrompt)
                    .call()
                    .content();
            if (!StringUtils.hasText(standaloneQuery)) {
                return fallback(currentQuery);
            }
            standaloneQuery = standaloneQuery.trim();
            boolean rewritten = !currentQuery.equals(standaloneQuery);
            return new ConversationQueryRewriteResult(currentQuery,
                    standaloneQuery,
                    rewritten,
                    rewritten);
        } catch (Exception e) {
            log.warn(
                    "Conversation query rewrite failed, fallback to original query. query={}",
                    currentQuery,
                    e
            );
            return fallback(currentQuery);
        }
    }


    private ConversationQueryRewriteResult fallback(String currentQuery) {
        return new ConversationQueryRewriteResult(
                currentQuery,
                currentQuery,
                false,
                false
        );
    }


    private String buildHistoryText(List<ConversationMessage> history) {
        StringBuilder builder = new StringBuilder();
        for (ConversationMessage conversationMessage : history) {
            builder.append(conversationMessage.role())
                    .append(": ")
                    .append(conversationMessage.content())
                    .append("\n");
        }
        return builder.toString();
    }
}
