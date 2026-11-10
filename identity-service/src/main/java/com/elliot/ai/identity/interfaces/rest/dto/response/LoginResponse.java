package com.elliot.ai.identity.interfaces.rest.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "访问令牌信息；刷新令牌仅通过 HttpOnly Cookie 返回")
public record LoginResponse(

        @Schema(description = "访问令牌，业务请求通过 Authorization: Bearer <token> 携带")
        String accessToken,

        @Schema(description = "访问令牌类型", example = "Bearer")
        String tokenType,

        @Schema(description = "Access Token 有效期，单位为秒", example = "900")
        long expiresIn,

        @Schema(description = "当前登录用户名", example = "admin")
        String username

) {
}
