package com.elliot.ai.rag.tool;

import com.elliot.ai.rag.router.ChatIntent;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class KnowledgeBaseToolRegistry {
    private final KnowledgeBaseTools knowledgeBaseTools;


    public ToolCallback[] forIntent(ChatIntent chatIntent) {
        if (chatIntent == null) {
            throw new IllegalArgumentException(
                    "ChatIntent 不能为空"
            );
        }
        switch (chatIntent) {
            case DOCUMENT_COUNT -> {
                return forDocumentCount();
            }
            default -> {
                return new ToolCallback[]{};
            }
        }
    }


    public ToolCallback[] forDocumentCount() {
        //获取knowledgeBaseTools所有tools
        ToolCallback[] callbacks = Arrays.stream(ToolCallbacks.from(knowledgeBaseTools))
                .filter(each -> "countDocuments".equals(each.getToolDefinition().name()))
                .toArray(ToolCallback[]::new);
        if (callbacks.length == 0) {
            throw new IllegalStateException(
                    "countDocuments 工具不存在"
            );
        }
        return callbacks;
    }

    /**
     * ToolCallbackProvider 可以获取类中的所有工具
     *
     * @return
     */
    public ToolCallback[] allTools() {
        MethodToolCallbackProvider provider = MethodToolCallbackProvider
                .builder()
                .toolObjects(knowledgeBaseTools)
                .build();
        return provider.getToolCallbacks();
    }
}
