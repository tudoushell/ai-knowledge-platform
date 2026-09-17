package com.elliot.ai.rag.query.conversation.model;

public record ConversationMessage(
        Role role,
        String content
) {

    public enum Role {
        USER,
        ASSISTANT
    }
}
