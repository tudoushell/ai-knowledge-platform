package com.elliot.ai.rag.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


import javax.validation.constraints.NotNull;
import java.util.UUID;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RetrievalSearchDto {
    @NotNull(message = "知识库id不能为空")
    private UUID knowledgeBaseId;
    @NotBlank(message = "查询内容不能为空")
    @Size(max = 1000, message = "查询内容不能超过 1000 个字符")
    private String query;
    @Min(value = 1, message = "topK 不能小于 1")
    @Max(value = 20, message = "topK 不能超过 20")
    private Integer topK;
    @DecimalMin(
            value = "0.0",
            message = "相似度阈值不能小于 0"
    )
    @DecimalMax(
            value = "1.0",
            message = "相似度阈值不能大于 1"
    )
    private Double similarityThreshold;

}
