package com.elliot.ai.rag.storage.parsed;

import com.elliot.ai.rag.enums.ParsedFormat;
import lombok.Getter;

/** 文档解析结果的统一元数据。 */
@Getter
public class ParsedArtifact {

    /** 解析结果相对于解析文件根目录的存储路径。 */
    private final String relativePath;

    /** 解析结果的存储格式。 */
    private final ParsedFormat parsedFormat;

    /** 用于列表展示的解析内容预览。 */
    private final String preview;

    /** 解析结果包含的字符数。 */
    private final long charCount;

    /** JSONL 中解析块的数量；纯文本格式时为 {@code null}。 */
    private final Integer blockCount;

    /** 原始文档总页数 PDF。 */
    private final Integer pageCount;

    public ParsedArtifact(
            String relativePath,
            ParsedFormat parsedFormat,
            String preview,
            long charCount,
            Integer blockCount,
            Integer pageCount
    ) {
        this.relativePath = relativePath;
        this.parsedFormat = parsedFormat;
        this.preview = preview;
        this.charCount = charCount;
        this.blockCount = blockCount;
        this.pageCount = pageCount;
    }
}
