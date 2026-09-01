package com.elliot.ai.rag.service;

import com.elliot.ai.rag.enums.DocumentTaskStep;

import java.util.UUID;

public interface DocumentTaskStateService {

    void advance(UUID taskId, DocumentTaskStep nextStep, int progress);

    void touch(UUID taskId);

    void succeed(UUID taskId);

    void failOrRetry(UUID taskId, Throwable exception);

    void resetToPending(UUID taskId);
}
