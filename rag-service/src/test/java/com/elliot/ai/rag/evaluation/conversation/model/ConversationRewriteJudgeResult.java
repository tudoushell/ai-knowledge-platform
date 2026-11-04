package com.elliot.ai.rag.evaluation.conversation.model;

/**
 * 重新会话评测结果
 *
 * @param contextCorrect    上下文正确
 * @param intentPreserved   意图保持
 * @param standalone        独立可理解
 * @param noUnsupportedInfo 没有无依据补充
 * @param reason
 */
public record ConversationRewriteJudgeResult(
        boolean contextCorrect,
        boolean intentPreserved,
        boolean standalone,
        boolean noUnsupportedInfo,
        String reason
) {

    public boolean passed() {
        return contextCorrect && intentPreserved && standalone && noUnsupportedInfo;
    }
}
