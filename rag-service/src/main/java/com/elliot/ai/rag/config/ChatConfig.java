package com.elliot.ai.rag.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatConfig {

    @Bean(name = "local")
    public ChatClient chatClient() {
        OpenAiChatModel openAiChatModel = OpenAiChatModel.builder()
                .options(OpenAiChatOptions.builder()
                        .baseUrl("http://localhost:11434/v1")
                        .apiKey("ollama")
                        .model("qwen3-vl:2b")
                        .build()).build();
        return ChatClient.create(openAiChatModel);
    }

    @Bean(name = "qwen3.8-flash")
    public ChatClient aiChatClient() {
        OpenAiChatModel openAiChatModel = OpenAiChatModel.builder()
                .options(OpenAiChatOptions.builder()
                        .baseUrl("https://llm-ljefv1argjxdoupn.cn-beijing.maas.aliyuncs.com/compatible-mode/v1")
                        .apiKey(System.getenv("alibaba_key"))
                        .model("qwen3.8-flash")
                        .build()).build();
        return ChatClient.create(openAiChatModel);
    }
}
