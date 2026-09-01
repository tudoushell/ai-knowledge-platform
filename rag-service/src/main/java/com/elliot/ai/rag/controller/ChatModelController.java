package com.elliot.ai.rag.controller;

import com.elliot.ai.common.dto.Result;
import com.elliot.ai.rag.router.ChatClientRouter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 对外提供当前可用的聊天模型。
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/models")
@Tag(name = "聊天模型", description = "查询系统当前注册的聊天模型")
public class ChatModelController {

    private final ChatClientRouter chatClientRouter;

    /**
     * 获取所有已经注册的聊天模型编码。
     *
     * @return 可用于发起聊天请求的模型编码列表
     */
    @GetMapping
    @Operation(summary = "获取聊天模型", description = "返回当前已经注册并可用于路由的聊天模型编码。")
    public Result<List<String>> getModels() {
        return Result.buildSuccess(chatClientRouter.getModelCodes());
    }
}
