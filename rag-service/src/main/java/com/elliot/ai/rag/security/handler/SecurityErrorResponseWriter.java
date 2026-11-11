package com.elliot.ai.rag.security.handler;

import com.elliot.ai.common.dto.Result;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** 将安全过滤链中的错误直接写成 Result，避免 sendError 转发到 /error。 */
@Component
@RequiredArgsConstructor
public class SecurityErrorResponseWriter {

    private final ObjectMapper objectMapper;

    /**
     * 写入 HTTP 状态及同值业务状态码；响应已提交时不再修改，也不关闭输出流。
     * 保留调用方设置的 WWW-Authenticate 等响应头。
     */
    public void write(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.resetBuffer();
        response.setStatus(status.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        response.getWriter().write(objectMapper.writeValueAsString(new Result<>(status.value(), message, null)));
    }
}
