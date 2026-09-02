package com.elliot.ai.rag.query.expansion;

import com.elliot.ai.rag.query.model.QueryExpansionResult;
import com.elliot.ai.rag.router.ChatClientRouter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmQueryExpansionService implements QueryExpansionService {

    private final ChatClientRouter chatClientRouter;

    @Override
    public QueryExpansionResult expand(String query) {
        try {
            ChatClient chatClient = chatClientRouter.get("qwen3.8-flash");
            ExpansionOutput output = chatClient.prompt()
                    .system("""
                                你是一个知识库检索查询扩展器。
                            
                                   你的任务是围绕给定的基础检索查询，
                                   生成 2 个额外的检索查询，用于提高知识库召回率。
        
                                   要求：
                                   1. 保持相同的核心检索意图
                                   2. 使用不同关键词、同义词、专业术语或表达方式
                                   3. 不得添加无法确定的事实
                                   4. 尽量保留重要的精确技术术语
                                   5. 不要拆成不同子问题
                                   6. 不要回答问题
                                   7. 不要返回与基础查询完全相同的查询
                                   8. 保持原语言
                                   9. 只生成 2 个扩展查询
                            """)
                    .user(query)
                    .call()
                    .entity(ExpansionOutput.class);
            if (output == null || output.queries == null) {
                return QueryExpansionResult.noExpansion(query);
            }
            List<String> expandedQueries = output.queries().stream()
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .filter(expandedQuery -> !query.equals(expandedQuery))
                    .distinct()
                    .limit(2)
                    .toList();
            if (expandedQueries.isEmpty()) {
                return QueryExpansionResult.noExpansion(query);
            }
            return new QueryExpansionResult(query, expandedQueries);
        } catch (RuntimeException e) {
            log.warn(
                    "Query expansion failed, fallback to base query. errorType={}",
                    e.getClass().getSimpleName()
            );
            return QueryExpansionResult.noExpansion(query);
        }
    }

    private record ExpansionOutput(
            List<String> queries
    ) {

    }
}
