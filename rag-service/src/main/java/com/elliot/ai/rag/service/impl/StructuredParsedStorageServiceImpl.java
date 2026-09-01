package com.elliot.ai.rag.service.impl;

import com.elliot.ai.common.enums.ResultCode;
import com.elliot.ai.common.exception.BusinessException;
import com.elliot.ai.common.util.JsonUtils;
import com.elliot.ai.rag.enums.ParsedFormat;
import com.elliot.ai.rag.service.StructuredParsedStorageService;
import com.elliot.ai.rag.storage.parsed.ParsedArtifact;
import com.elliot.ai.rag.storage.parsed.ParsedBlock;
import lombok.Getter;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.function.Consumer;

@Service
public class StructuredParsedStorageServiceImpl implements StructuredParsedStorageService {
    @Override
    public StructuredParseWriter createWriter(Path filePath) {
        try {
            BufferedWriter writer = Files.newBufferedWriter(
                filePath,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);
            return new StructuredParseWriter(writer);
        } catch (IOException e) {
            throw new BusinessException(ResultCode.FAIL,
                    "写入结构化解析文件失败");
        }
    }

    public void forEachBlock(Path filePath, Consumer<ParsedBlock> consumer) {
        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                ParsedBlock parsedBlock = JsonUtils.fromJson(line, ParsedBlock.class);
                consumer.accept(parsedBlock);
            }
        } catch (IOException e) {
            throw new BusinessException(
                    ResultCode.FAIL,
                    "读取结构化解析文件失败"
            );
        }
    }

    @Getter
    public static final class StructuredParseWriter implements AutoCloseable {
        private final StringBuilder preview = new StringBuilder(PREVIEW_LIMIT);
        private final BufferedWriter writer;
        private long charCount;
        private int blockCount;
        private boolean completed;
        private static final int PREVIEW_LIMIT = 500;

        public StructuredParseWriter(BufferedWriter writer) {
            this.writer = writer;
        }


        public void write(ParsedBlock block) {
            if (block == null || !StringUtils.hasText(block.text())) {
                return;
            }
            try {
                writer.write(JsonUtils.toJson(block));
                writer.newLine();
                blockCount++;
                charCount += block.text().length();
                appendPreview(block.text());
            } catch (IOException e) {
                throw new BusinessException(ResultCode.FAIL,
                        "解析块写入失败");
            }
        }

        public ParsedArtifact complete(String relativePath, Integer pageCount) {
            try {
                writer.flush();
                completed = true;
                return new ParsedArtifact(
                        relativePath,
                        ParsedFormat.STRUCTURED_JSONL,
                        preview.toString(),
                        charCount,
                        blockCount,
                        pageCount);
            } catch (IOException e) {
                throw new BusinessException(
                        ResultCode.FAIL,
                        "结构化解析文件写入失败"
                );
            }
        }

        private void appendPreview(String text) throws IOException {
            if (preview.length() >= PREVIEW_LIMIT) {
                return;
            }
            if (!preview.isEmpty()) {
                preview.append("\n");
            }
            int remaining = PREVIEW_LIMIT - preview.length();
            preview.append(text, 0, Math.min(text.length(), remaining));
        }

        @Override
        public void close() throws IOException {
            writer.close();
        }
    }
}
