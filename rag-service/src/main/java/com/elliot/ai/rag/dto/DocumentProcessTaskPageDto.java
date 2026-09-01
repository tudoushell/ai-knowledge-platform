package com.elliot.ai.rag.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 文档处理任务历史分页响应对象。
 */
@Schema(description = "文档处理任务历史分页响应对象")
public record DocumentProcessTaskPageDto(
        @Schema(description = "当前页码，从 1 开始", example = "1")
        int page,

        @Schema(description = "每页数量", example = "20")
        int size,

        @Schema(description = "任务总数", example = "5")
        long total,

        @Schema(description = "当前页任务列表，按创建时间倒序")
        List<DocumentProcessTaskDto> records
) {
}
