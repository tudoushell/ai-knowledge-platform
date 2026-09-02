package com.elliot.ai.rag.query.expansion;

import com.elliot.ai.rag.query.model.QueryExpansionResult;

public interface QueryExpansionService {

    /**
     * 扩展 Rewrite 后的问题
     *
     * @param query
     * @return
     */
    QueryExpansionResult expand(String query);
}
