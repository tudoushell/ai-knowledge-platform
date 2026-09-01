package com.elliot.ai.rag;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.elliot.ai.rag.mapper")
public class AiRagServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiRagServiceApplication.class, args);
    }
}
