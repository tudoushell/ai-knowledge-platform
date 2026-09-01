package com.elliot.ai.rag.parse;

import com.elliot.ai.common.enums.ResultCode;
import com.elliot.ai.common.exception.BusinessException;
import com.elliot.ai.rag.config.StorageProperties;
import com.elliot.ai.rag.enums.ParsedFormat;
import com.elliot.ai.rag.storage.parsed.ParsedArtifact;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Set;

@Component
public class TextDocumentParse extends AbstractDocumentParse {


    private static final Set<String> SUPPORTED_TYPES =
            Set.of("txt", "md");

    public TextDocumentParse(StorageProperties properties) {
        super(properties);
    }

    @Override
    public boolean isSupport(String extension) {
        return SUPPORTED_TYPES.contains(extension);
    }

    @Override
    protected String parsedFileExtension() {
        return ".txt";
    }

    @Override
    protected ParsedArtifact parseFile(Path sourcePath, Path targetPath, String parsedRelationPath) throws IOException {
        StringBuilder preview = new StringBuilder(PREVIEW_LENGTH);
        long charCount = 0;
        boolean hasText = false;
        boolean firstCharacter = true;
        boolean skipNextLineFeed = false;
        try (BufferedReader reader = Files.newBufferedReader(sourcePath, StandardCharsets.UTF_8);
             BufferedWriter writer = Files.newBufferedWriter(
                     targetPath,
                     StandardCharsets.UTF_8,
                     StandardOpenOption.CREATE,
                     StandardOpenOption.TRUNCATE_EXISTING)) {
            char[] buffer = new char[8192];
            int readCount;
            while ((readCount = reader.read(buffer)) != -1) {
                for (int i = 0; i < readCount; i++) {
                    char character = buffer[i];
                    if (firstCharacter) {
                        firstCharacter = false;
                        if (isUTF8BOM(character)) {
                            continue;
                        }
                    }
                    if (character == '\n' && skipNextLineFeed) {
                        skipNextLineFeed = false;
                        continue;
                    }
                    char normalizedCharacter = character == '\r' ? '\n' : character;
                    skipNextLineFeed = character == '\r';
                    writer.write(normalizedCharacter);
                    charCount++;
                    if (!Character.isWhitespace(normalizedCharacter)) {
                        hasText = true;
                    }
                    if (preview.length() < PREVIEW_LENGTH) {
                        preview.append(normalizedCharacter);
                    }
                }
            }
        }
        if (!hasText) {
            throw new BusinessException(ResultCode.FAIL, "文件内容为空");
        }
        return new ParsedArtifact(
                parsedRelationPath,
                ParsedFormat.PLAIN_TEXT,
                preview.toString(),
                charCount,
                null,
                null
        );
    }
}
