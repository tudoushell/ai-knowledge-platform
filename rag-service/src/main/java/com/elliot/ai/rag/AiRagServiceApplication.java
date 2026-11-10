package com.elliot.ai.rag;

import com.elliot.ai.identity.config.IdentitySecurityConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@ConfigurationPropertiesScan
@MapperScan("com.elliot.ai.rag.mapper")
@Import({IdentitySecurityConfiguration.class})
public class AiRagServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiRagServiceApplication.class, args);
    }
}
