package com.elliot.ai.rag.evaluation;

import java.util.List;

public record RetrievalEvaluationResult(
        RetrievalEvaluationSummary summary,
        List<RetrievalEvaluationCaseResult> caseResults
) {
}
