package com.elliot.ai.rag.query.conversation;

import com.elliot.ai.rag.query.conversation.model.ConversationMessage;
import com.elliot.ai.rag.query.conversation.model.ConversationQueryRewriteResult;

import java.util.List;

public interface ConversationQueryRewriteService {
    /**
     *  根据上下文的语镜重写提问会话
     *
     * @param history
     * @param currentQuery
     * @return
     */
    ConversationQueryRewriteResult rewrite(
            List<ConversationMessage> history,
            String currentQuery
    );
}
