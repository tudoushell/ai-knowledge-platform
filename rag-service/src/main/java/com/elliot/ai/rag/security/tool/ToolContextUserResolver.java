package com.elliot.ai.rag.security.tool;

import com.elliot.ai.common.enums.ResultCode;
import com.elliot.ai.common.exception.BusinessException;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ToolContextUserResolver {

    public String requireUserId(ToolContext toolContext) {

        if (toolContext == null || toolContext.getContext() == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        Object value = toolContext.getContext()
                .get(ToolContextKey.USER_ID);

        if (!(value instanceof String userId)
                || !StringUtils.hasText(userId)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        return userId;
    }
}