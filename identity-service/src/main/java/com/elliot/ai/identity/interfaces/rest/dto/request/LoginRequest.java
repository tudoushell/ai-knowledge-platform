package com.elliot.ai.identity.interfaces.rest.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

@Schema(description = "用户名密码登录请求")
public record LoginRequest(

        @Schema(description = "登录用户名", example = "alice")
        @NotBlank(message = "用户名不能为空")
        @Size(max = 64, message = "用户名长度不能超过64")
        String username,

        @NotEmpty(message = "密码不能为空")
        @Schema(description = "登录密码", format = "password", accessMode = Schema.AccessMode.WRITE_ONLY)
        String password

) {
}
