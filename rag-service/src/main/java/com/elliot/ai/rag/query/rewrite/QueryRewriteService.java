package com.elliot.ai.rag.query.rewrite;

import com.elliot.ai.rag.query.model.QueryRewriteResult;

public interface QueryRewriteService {
    /**
     * 对用户原始问题进行改写
     *
     * @param query 用户的问题
     * @return 用户原始问题和改写的问题
     */
    QueryRewriteResult rewrite(String query);
}
