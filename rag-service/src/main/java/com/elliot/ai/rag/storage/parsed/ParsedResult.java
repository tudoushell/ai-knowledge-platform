package com.elliot.ai.rag.storage.parsed;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ParsedResult {
    /**
     * 部分限制文本
     */
    private String limitContent;

    /** 解析结果是否为空。 */
    private boolean hasText;

    /** 解析结果的字符数。 */
    private long charCount;
}
