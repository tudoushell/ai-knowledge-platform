package com.elliot.ai.rag.query.conversation.model;

/**
 * 会话查询重写结果
 *
 * @param originQuery 原始查询
 * @param standaloneQuery 根据上下文记忆构建的查询
 * @param rewritten 问题是否被重写
 * @param historyUsed 问题是否使用历史记录
 */
public record ConversationQueryRewriteResult(
        String originQuery,
        String standaloneQuery,
        boolean rewritten,
        boolean historyUsed
) {
}
