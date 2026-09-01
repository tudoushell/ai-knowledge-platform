package com.elliot.ai.rag.parse;

import com.elliot.ai.common.enums.ResultCode;
import com.elliot.ai.common.exception.BusinessException;
import com.elliot.ai.rag.config.StorageProperties;
import com.elliot.ai.rag.service.StructuredParsedStorageService;
import com.elliot.ai.rag.service.impl.StructuredParsedStorageServiceImpl;
import com.elliot.ai.rag.storage.parsed.ParsedArtifact;
import com.elliot.ai.rag.storage.parsed.ParsedBlock;
import lombok.Getter;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFStyle;
import org.apache.poi.xwpf.usermodel.XWPFStyles;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * DOCX 文档解析器。
 *
 * <p>按 Word 正文中的段落、表格出现顺序读取内容；以标题作为章节边界，
 * 将章节内容写成 JSONL 格式的 {@link ParsedBlock}。</p>
 */
@Component
public class DocxDocumentParse extends AbstractDocumentParse {

    /*
     * 避免单个标题章节过大。
     * 超过后会拆成多个结构块，但 sectionTitle 保持一致。
     */
    private static final int MAX_SECTION_CHARS = 20_000;

    private final StructuredParsedStorageService structuredParsedStorageService;


    public DocxDocumentParse(StorageProperties properties, StructuredParsedStorageService structuredParsedStorageService) {
        super(properties);
        this.structuredParsedStorageService = structuredParsedStorageService;
    }

    @Override
    public boolean isSupport(String extension) {
        return "docx".equalsIgnoreCase(extension);
    }

    @Override
    protected String parsedFileExtension() {
        return ".jsonl";
    }

    @Override
    protected ParsedArtifact parseFile(Path sourcePath, Path targetPath, String parsedRelativePath) throws IOException {
        try (InputStream inputStream = Files.newInputStream(sourcePath);
             XWPFDocument document = new XWPFDocument(inputStream);
             StructuredParsedStorageServiceImpl.StructuredParseWriter writer
                     = structuredParsedStorageService.createWriter(targetPath)
        ) {
            // 保存遍历过程中的章节标题、累积文本和块序号。
            ParseState parseState = new ParseState();
            for (IBodyElement element : document.getBodyElements()) {
                if (element instanceof XWPFParagraph paragraph) {
                    processParagraph(document, paragraph, parseState, writer);
                } else if (element instanceof XWPFTable table) {
                    // 表格内容归入当前章节，单元格之间使用竖线分隔。
                    appendText(extractTable(table), parseState, writer);
                }
            }
            // 文档结束后，写出尚未因标题切换而刷新的最后一个章节。
            flushSection(parseState, writer);
            if (writer.getBlockCount() == 0) {
                throw new BusinessException(ResultCode.FAIL,
                        "Word 文档中没有提取到可用文本");
            }
            return writer.complete(parsedRelativePath,null);
        }  catch (BusinessException e) {
            throw e;
        }
    }

    /**
     * 解析一个 Word 段落：标题会切换章节，普通段落则追加到当前章节。
     *
     * @param document  当前 DOCX 文档，用于查询段落样式
     * @param paragraph 当前段落
     * @param state     当前解析状态
     * @param writer    JSONL 块写入器
     */
    private void processParagraph(XWPFDocument document,
                                  XWPFParagraph paragraph,
                                  ParseState state,
                                  StructuredParsedStorageServiceImpl.StructuredParseWriter writer) {
        String text = paragraph.getText();
        if (!StringUtils.hasText(text)) {
            return;
        }
        if (isHeading(document, paragraph)) {
            flushSection(state, writer);
            state.sectionTitle = text;
            state.builder.append(text).append("\n");
            return;
        }
        appendText(text, state, writer);
    }

    private void appendText(String text,
                            ParseState state,
                            StructuredParsedStorageServiceImpl.StructuredParseWriter writer) {
        if (!StringUtils.hasText(text)) {
            return;
        }
        // 同一章节过大时提前写出，避免单个 JSONL 块占用过多内存或 Token。
        if (!state.builder.isEmpty()
                && state.builder.length() + text.length() > MAX_SECTION_CHARS) {
            flushSection(state, writer);
        }
        state.builder.append(text).append("\n");
    }

    private void flushSection(ParseState state,
                              StructuredParsedStorageServiceImpl.StructuredParseWriter writer) {
        String content = state.builder.toString().trim();
        if (content.isBlank()) {
            state.builder.setLength(0);
            return;
        }
        // 一个章节对应一个结构化块，标题写入 sectionTitle 以供后续检索展示。
        writer.write(new ParsedBlock(state.sequence++,
                "SECTION",
                null,
                state.sectionTitle,
                content));
    }

    /** 根据段落样式名称判断其是否为文档标题。 */
    private boolean isHeading(XWPFDocument document,
                              XWPFParagraph paragraph) {
        String styleId = paragraph.getStyleID();
        if (!StringUtils.hasText(styleId)) {
            return false;
        }
        String styleName = styleId;
        XWPFStyles styles = document.getStyles();
        if (styles != null) {
            XWPFStyle style = styles.getStyle(styleId);
            if (style != null && StringUtils.hasText(style.getName())) {
                styleName = style.getName();
            }
        }

        String normalized = styleName.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith("heading")
                || normalized.startsWith("title")
                || normalized.startsWith("标题");
    }

    /**
     * 将表格转换为可检索文本：同一行的单元格用 {@code |} 分隔，不同行用换行分隔。
     */
    private String extractTable(XWPFTable table) {
        return table.getRows()
                .stream()
                .map(row -> row.getTableCells()
                        .stream()
                        .map(cell -> normalizeInline(cell.getText())).collect(Collectors.joining(" | "))
                ).filter(StringUtils::hasText)
                .collect(Collectors.joining("\n"));
    }

    /** 规范化多行文本的换行符。 */
    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\r\n", "\n")
                .replace("\r", "\n")
                .trim();
    }

    /** 将单元格内容压缩为单行文本，避免表格内部换行破坏行列结构。 */
    private String normalizeInline(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\n", " ")
                .replace("\r", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /** DOCX 遍历期间的临时状态，不是 Spring Bean。 */
    @Getter
    private static final class ParseState {
        /** 下一个写出块的顺序号。 */
        private int sequence;
        /** 最近识别到的标题，作为后续内容块的章节归属。 */
        private String sectionTitle;
        /** 当前章节尚未写入的文本内容。 */
        private final StringBuilder builder = new StringBuilder();
    }
}
