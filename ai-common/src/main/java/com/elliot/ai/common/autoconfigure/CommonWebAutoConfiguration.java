package com.elliot.ai.common.autoconfigure;

import com.elliot.ai.common.exception.GlobalExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/** 通用 Web 自动配置。 */
@AutoConfiguration
public class CommonWebAutoConfiguration {

    /**
     * 注册通用全局异常处理器。
     *
     * @return 全局异常处理器
     */
    @Bean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }
}
