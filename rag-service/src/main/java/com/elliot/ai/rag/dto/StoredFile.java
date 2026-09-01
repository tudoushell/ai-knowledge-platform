package com.elliot.ai.rag.dto;

public record StoredFile(
        String originalName,
        String storedName,
        String relativePath,
        String contentType,
        String extension,
        long sizeBytes,
        String sha256
) {
}