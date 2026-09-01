package com.elliot.ai.rag.storage.parsed;

/**
 * 文档解析后得到的一个内容块，用于保留文本及其所在位置、层级等结构化信息。
 *
 * @param sequence     内容块在原始文档中的顺序，从 {@code 0} 开始
 * @param blockType    内容块类型，例如 {@code paragraph}、{@code title}、{@code table}
 * @param pageNumber   内容块所在页码；不适用或无法确定时为 {@code null}
 * @param sectionTitle 内容块所属章节标题；未识别到章节时为 {@code null}
 * @param text         内容块的实际文本内容
 */
public record ParsedBlock(
        /** 内容块在原始文档中的顺序，从 0 开始。 */
        int sequence,
        /** 内容块类型，例如 paragraph、title、table。 */
        String blockType,
        /** 内容块所在页码；不适用或无法确定时为 null。 */
        Integer pageNumber,
        /** 内容块所属章节标题；未识别到章节时为 null。 */
        String sectionTitle,
        /** 内容块的实际文本内容。 */
        String text
) {
}
