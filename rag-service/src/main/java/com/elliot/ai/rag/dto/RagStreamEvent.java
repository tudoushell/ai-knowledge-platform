package com.elliot.ai.rag.dto;

import java.util.List;
import java.util.UUID;

public record RagStreamEvent(
        String type,
        UUID conversationId,
        String content,
        List<RagSourceDto> sources,
        TokenUsageDto usage
) {

    public static RagStreamEvent sources(List<RagSourceDto> sources) {
        return new RagStreamEvent("sources", null, null, sources, null);
    }

    public static RagStreamEvent delta(String content) {
        return new RagStreamEvent("delta", null, content, List.of(), null);
    }

    public static RagStreamEvent done(TokenUsageDto usage) {
        return new RagStreamEvent("done", null, null, List.of(), usage);
    }

    public static RagStreamEvent error(String message) {
        return new RagStreamEvent("error", null, message, List.of(), null);
    }

    public static RagStreamEvent conversation(UUID conversationId) {
        return new RagStreamEvent("conversation", conversationId, null, List.of(), null);
    }
}
