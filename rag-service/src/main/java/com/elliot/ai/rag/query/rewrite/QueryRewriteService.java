package com.elliot.ai.rag.query.rewrite;

import com.elliot.ai.rag.query.model.QueryRewriteResult;

public interface QueryRewriteService {
    /**
     * 对用户原始问题进行改写
     *
     * @param query
     * @return
     */
    QueryRewriteResult rewrite(String query);
}
