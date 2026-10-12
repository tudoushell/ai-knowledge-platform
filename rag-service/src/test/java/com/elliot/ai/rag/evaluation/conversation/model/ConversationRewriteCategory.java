package com.elliot.ai.rag.evaluation.conversation.model;

/**
 * 会话查询改写评估用例的分类，用于区分查询对历史上下文的依赖方式。
 */
public enum ConversationRewriteCategory {
    /**
     * 指代消解：根据历史上下文，将“它”“这个”等指代替换为明确的对象。
     */
    PRONOUN,

    /**
     * 省略补全：根据历史上下文，补全当前查询中省略的主体或条件。
     */
    ELLIPSIS,

    /**
     * 无需改写：当前查询已完整、独立，应保留原始语义，避免引入无关历史信息。
     */
    NO_REWRITE,

    /**
     * 话题切换：当前查询转向新话题，不应将之前话题的信息带入查询。
     */
    TOPIC_SWITCH
}
