package com.elliot.ai.rag.query.model;

import java.util.List;
import java.util.stream.Stream;

public record QueryExpansionResult(
        String baseQuery,
        List<String> expandedQueries
) {

    public List<String> retrievalQueries() {
        return Stream.concat(
                        Stream.of(baseQuery),
                        expandedQueries.stream()
                )
                .distinct()
                .toList();
    }

    public static QueryExpansionResult noExpansion(String baseQuery) {
        return new QueryExpansionResult(
                baseQuery,
                List.of());
    }
}
