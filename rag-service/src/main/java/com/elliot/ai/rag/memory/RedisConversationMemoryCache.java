package com.elliot.ai.rag.memory;

import com.elliot.ai.common.util.JsonUtils;
import com.elliot.ai.rag.config.ConversationMemoryProperties;
import com.elliot.ai.rag.query.conversation.model.ConversationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;


@Slf4j
@Component
@RequiredArgsConstructor
public class RedisConversationMemoryCache implements ConversationMemoryCache {

    private final static String KEY_PREFIX = "rag:conversation:history:";

    private final StringRedisTemplate redisTemplate;

    private final ConversationMemoryProperties properties;

    @Override
    public ConversationMemoryCacheReadResult get(UUID conversationId) {
        String key = buildKey(conversationId);
        try {
            List<String> values = redisTemplate.opsForList()
                    .range(key, 0, -1);
            if (values == null || values.isEmpty()) {
                return ConversationMemoryCacheReadResult.miss();
            }
            List<ConversationMessage> conversationMessages = values.stream()
                    .map(each -> JsonUtils.fromJson(each, ConversationMessage.class))
                    .toList();
            return ConversationMemoryCacheReadResult.hit(conversationMessages);
        } catch (Exception e) {
            log.warn(
                    "Read conversation memory from Redis failed, conversationId={}",
                    conversationId,
                    e
            );
            return ConversationMemoryCacheReadResult.error();
        }
    }

    @Override
    public void put(UUID conversationId, List<ConversationMessage> messages) {
        String key = buildKey(conversationId);
        if (messages == null || messages.isEmpty()) {
            return;
        }
        List<String> values = messages.stream().map(JsonUtils::toJson).toList();
        try {
            redisTemplate.delete(key);
            redisTemplate.opsForList().rightPushAll(key, values);
            redisTemplate.opsForList().trim(key, -properties.getMaxMessage(), -1);
            redisTemplate.expire(key, properties.getTtl());
        } catch (Exception e) {
            log.warn(
                    "Write conversation memory to Redis failed, evict cached, conversationId={}",
                    conversationId,
                    e
            );
            evict(conversationId);
        }
    }

    @Override
    public void append(UUID conversationId, ConversationMessage message) {
        String key = buildKey(conversationId);
        if (!redisTemplate.hasKey(key)) {
            return;
        }
        try {
            redisTemplate.opsForList().rightPush(key, JsonUtils.toJson(message));
            redisTemplate.opsForList().trim(key, -properties.getMaxMessage(), -1);
            redisTemplate.expire(key, properties.getTtl());
        } catch (Exception e) {
            log.warn(
                    "Append conversation memory to Redis failed, evict cache, conversationId={}",
                    conversationId,
                    e
            );
            evict(conversationId);
        }
    }

    @Override
    public void evict(UUID conversationId) {
        try {
            redisTemplate.delete(buildKey(conversationId));
        } catch (Exception e) {
            log.warn(
                    "delete conversation memory to Redis failed, conversationId={}",
                    conversationId,
                    e
            );
        }
    }

    private String buildKey(UUID conversationId) {
        return KEY_PREFIX + conversationId.toString();
    }
}
