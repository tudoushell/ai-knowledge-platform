package com.elliot.ai.rag.evaluation.conversation.judge;

import com.elliot.ai.rag.evaluation.conversation.model.ConversationRewriteEvaluationCase;
import com.elliot.ai.rag.evaluation.conversation.model.ConversationRewriteJudgeResult;
import com.elliot.ai.rag.router.ChatClientRouter;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.template.NoOpTemplateRenderer;

import java.util.stream.Collectors;

@RequiredArgsConstructor
public class LlmConversationRewriteJudge implements ConversationRewriteJudge {

    private final ChatClientRouter chatClientRouter;
    private String judgeModelCode = "qwen3.8-flash";

    @Override
    public ConversationRewriteJudgeResult judge(
            ConversationRewriteEvaluationCase evaluationCase,
            String actualStandaloneQuery
    ) {

        String userPrompt = buildUserPrompt(
                evaluationCase,
                actualStandaloneQuery
        );

        ConversationRewriteJudgeResult result =
                chatClientRouter.get(judgeModelCode)
                        .prompt()
                        .templateRenderer(new NoOpTemplateRenderer())
                        .system(ConversationRewriteJudgePrompt.SYSTEM_PROMPT)
                        .user(userPrompt)
                        .call()
                        .entity(ConversationRewriteJudgeResult.class);

        if (result == null
                || result.reason() == null
                || result.reason().isBlank()) {

            throw new IllegalStateException(
                    "Judge returned an invalid result"
            );
        }

        return result;
    }

    private String buildUserPrompt(
            ConversationRewriteEvaluationCase evaluationCase,
            String actualStandaloneQuery
    ) {

        String historyText = evaluationCase.history().stream()
                .map(message ->
                        message.role().name() + ": " + message.content()
                )
                .collect(Collectors.joining("\n"));

        return """
                请根据评分标准评估以下会话改写结果。

                <history>
                %s
                </history>

                <currentQuery>
                %s
                </currentQuery>

                <expectedStandaloneQuery>
                %s
                </expectedStandaloneQuery>

                <actualStandaloneQuery>
                %s
                </actualStandaloneQuery>
                """.formatted(
                historyText,
                evaluationCase.currentQuery(),
                evaluationCase.expectedStandaloneQuery(),
                actualStandaloneQuery
        );
    }
}
