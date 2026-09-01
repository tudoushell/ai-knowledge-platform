package com.elliot.ai.rag.service;

import com.elliot.ai.rag.service.impl.StructuredParsedStorageServiceImpl;
import com.elliot.ai.rag.storage.parsed.ParsedBlock;

import java.nio.file.Path;
import java.util.function.Consumer;

public interface StructuredParsedStorageService {

    StructuredParsedStorageServiceImpl.StructuredParseWriter createWriter(Path filePath);

    void forEachBlock(Path filePath, Consumer<ParsedBlock> consumer);
}
