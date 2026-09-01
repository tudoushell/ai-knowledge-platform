package com.elliot.ai.rag.router;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Component
public class ChatClientRouter {
    private final Map<String, ChatClient> chatClientMap;

    public ChatClient get(String modelCode) {
        ChatClient chatClient = chatClientMap.get(modelCode);
        if (chatClient == null) {
            throw new IllegalArgumentException("不支持的模型：" + modelCode);
        }
        return chatClient;
    }

    /**
     * 获取当前已经注册并可供路由的模型编码。
     *
     * @return 按模型编码排序的只读列表
     */
    public List<String> getModelCodes() {
        return chatClientMap.keySet()
                .stream()
                .sorted()
                .toList();
    }
}
