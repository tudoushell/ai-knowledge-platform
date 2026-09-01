package com.elliot.ai.rag.service;

import com.elliot.ai.rag.entity.KbDocument;

public interface DocumentDerivedDataService {
    void clearDerivedData(KbDocument kbDocument);
}
