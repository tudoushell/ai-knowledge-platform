package com.elliot.ai.rag.dto;

import java.util.List;

public record RagStreamEvent(
        String type,
        String content,
        List<RagSourceDto> sources,
        TokenUsageDto usage
) {

    public static RagStreamEvent sources(List<RagSourceDto> sources) {
        return new RagStreamEvent("sources", null, sources, null);
    }

    public static RagStreamEvent delta(String content) {
        return new RagStreamEvent("delta", content, List.of(), null);
    }

    public static RagStreamEvent done(TokenUsageDto usage) {
        return new RagStreamEvent("done", null, List.of(), usage);
    }

    public static RagStreamEvent error(String message) {
        return new RagStreamEvent("error", message, List.of(), null);
    }

}
