package com.elliot.ai.common.autoconfigure;

import com.elliot.ai.common.prompt.PromptTemplateConfig;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/** 自动注册通用提示词模板加载器。 */
@AutoConfiguration
public class PromptTemplateAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public PromptTemplateConfig promptTemplateConfig() {
        return new PromptTemplateConfig();
    }
}
