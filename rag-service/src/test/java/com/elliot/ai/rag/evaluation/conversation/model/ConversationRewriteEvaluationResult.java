package com.elliot.ai.rag.evaluation.conversation.model;

/**
 * 单条会话查询改写用例的评估结果，记录预期输出、实际输出及两项评估指标。
 *
 * <p>改写决策正确仅表示是否改写符合预期，不代表改写后的内容正确；
 * 完全匹配采用字符串精确比较，不进行空白、标点归一化或语义相似度判断。</p>
 *
 * @param caseId 评估用例的唯一标识
 * @param category 用例所属的改写场景分类
 * @param currentQuery 用户本轮的原始查询
 * @param expectedStandaloneQuery 用例预期的、无需依赖会话历史即可理解的查询
 * @param actualStandaloneQuery 改写服务实际返回的独立查询
 * @param rewriteRequired 用例标注的是否需要改写
 * @param rewritten 改写服务返回的是否已改写
 * @param rewriteDecisionCorrect 是否改写的实际结果与用例标注是否一致，即 {@code rewriteRequired == rewritten}
 * @param exactMatch 实际独立查询与预期独立查询的字符串是否完全一致
 */
public record ConversationRewriteEvaluationResult(
        String caseId,
        ConversationRewriteCategory category,
        String currentQuery,
        String expectedStandaloneQuery,
        String actualStandaloneQuery,
        boolean rewriteRequired,
        boolean rewritten,
        boolean rewriteDecisionCorrect,
        boolean exactMatch
) {
}
