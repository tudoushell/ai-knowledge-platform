package com.elliot.ai.rag.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatConfig {

    @Bean(name = "local")
    public ChatClient chatClient() {
        OpenAiChatModel openAiChatModel = OpenAiChatModel.builder().openAiApi(OpenAiApi.builder()
                        .baseUrl("http://localhost:11434")
                        .apiKey("ollama")
                        .build())
                .defaultOptions(OpenAiChatOptions.builder()
                        .model("qwen3-vl:2b")
                        .build()).build();
        return ChatClient.create(openAiChatModel);
    }

    @Bean(name = "qwen3.7")
    public ChatClient aiChatClient() {
        OpenAiChatModel openAiChatModel = OpenAiChatModel.builder().openAiApi(OpenAiApi.builder()
                        .baseUrl("https://llm-ljefv1argjxdoupn.cn-beijing.maas.aliyuncs.com")
                        .completionsPath("/compatible-mode/v1/chat/completions")
                        .apiKey(System.getenv("alibaba_key"))
                        .build())
                .defaultOptions(OpenAiChatOptions.builder()
                        .model("qwen3.7-plus")
                        .build()).build();
        return ChatClient.create(openAiChatModel);
    }
}
