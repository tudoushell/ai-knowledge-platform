package com.elliot.ai.rag.tool;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.model.tool.ToolCallLimitExceededException;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.Tool;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


public class ToolCallLimitTest {

    static class CountingTool {

        private final AtomicInteger counter =
                new AtomicInteger();

        @Tool(
                name = "countDocuments",
                description = "查询文档数量"
        )
        public int countDocuments() {
            return counter.incrementAndGet();
        }
    }


    @Test
    void shouldRejectSecondToolCall() {
        CountingTool tools = new CountingTool();

        ToolCallback callback =
                ToolCallbacks.from(tools)[0];

        ToolCallingChatOptions options =
                ToolCallingChatOptions.builder()
                        .toolCallbacks(callback)
                        .build();

        Prompt prompt = new Prompt(
                new UserMessage("查询文档数量"),
                options
        );

        AssistantMessage.ToolCall call1 =
                new AssistantMessage.ToolCall(
                        "call-1",
                        "function",
                        "countDocuments",
                        "{}"
                );

        AssistantMessage.ToolCall call2 =
                new AssistantMessage.ToolCall(
                        "call-2",
                        "function",
                        "countDocuments",
                        "{}"
                );

        AssistantMessage assistantMessage =
                AssistantMessage.builder()
                        .toolCalls(List.of(call1, call2))
                        .build();

        ChatResponse response = new ChatResponse(
                List.of(new Generation(assistantMessage))
        );
        // 创建限制为 1 次的 ToolCallingManager
        DefaultToolCallingManager countDocuments = ToolCallingManager.builder().maxCallsPerTool("countDocuments", 1).build();
        //断言 executeToolCalls 抛出超限异常
        assertThatThrownBy(() -> countDocuments.executeToolCalls(prompt, response)).isInstanceOf(ToolCallLimitExceededException.class);
        // 断言 tools.counter.get() == 1
        assertThat(tools.counter.get()).isEqualTo(1);
    }
}
