package com.elliot.ai.rag.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI aiRagOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AI RAG Knowledge Base API")
                        .description("基于 Spring AI、pgvector 的知识库写入与语义检索接口文档")
                        .version("v0.0.1"));
    }
}
