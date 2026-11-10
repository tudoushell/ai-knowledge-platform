package com.elliot.ai.rag.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.model.tool.ToolCallingManager;
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
    public ChatClient aiChatClient(ToolCallingManager toolCallingManager) {
        OpenAiChatModel openAiChatModel = OpenAiChatModel.builder()
                .options(OpenAiChatOptions.builder()
                        .baseUrl("https://llm-ljefv1argjxdoupn.cn-beijing.maas.aliyuncs.com/compatible-mode/v1")
                        .apiKey(System.getenv("alibaba_key"))
                        .model("qwen3.8-flash")
                        .build()).build();

        return ChatClient.builder(openAiChatModel)
                .defaultAdvisors(ToolCallingAdvisor.builder()
                        .toolCallingManager(toolCallingManager)
                        .build()
                )
                .build();
    }


    /**
     * 创建工具调用管理器，负责解析、执行模型请求的工具调用，并限制重复调用次数。
     *
     *
     * <p>限制按单轮模型与工具交互累计，不是跨请求的全局计数：
     * {@code countDocuments} 最多调用一次，所有工具合计最多调用五次。
     * 超过限制时，默认抛出工具调用次数超限异常，避免模型陷入反复调用工具的循环。</p>
     *
     * <p>调用次数限制不负责注册工具；本轮可用工具仍由请求显式提供。
     * 手动通过 {@link ChatClient#create} 创建的客户端不会自动采用此 Bean，
     * 需要在其工具调用 Advisor 中显式接入该管理器才能应用这些限制。</p>
     *
     * @return 配置了单工具及总调用次数上限的工具调用管理器
     */
    @Bean
    public ToolCallingManager toolCallingManager() {

        return ToolCallingManager.builder()
                .maxCallsPerTool("countDocuments", 1)
                .maxTotalToolCalls(5)
                .build();
    }
}
