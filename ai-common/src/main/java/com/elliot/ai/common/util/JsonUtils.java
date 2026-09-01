package com.elliot.ai.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * JSON 序列化与反序列化工具。
 *
 * <p>内部的 {@link ObjectMapper} 会自动注册已引入的模块，因此支持 Java 时间类型等常见类型。</p>
 */
public final class JsonUtils {

    /** 用于 JSON 读写的线程安全对象。 */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();

    private JsonUtils() {
    }

    /**
     * 将对象序列化为 JSON 字符串。
     *
     * @param value 待序列化的对象
     * @return JSON 字符串
     * @throws IllegalArgumentException 对象无法序列化时抛出
     */
    public static String toJson(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        }
        catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("对象序列化为 JSON 失败", exception);
        }
    }

    /**
     * 将 JSON 字符串反序列化为指定类型的对象。
     *
     * @param json JSON 字符串
     * @param valueType 目标对象类型
     * @param <T> 目标对象泛型类型
     * @return 反序列化后的对象
     * @throws IllegalArgumentException JSON 不合法或无法转换为目标类型时抛出
     */
    public static <T> T fromJson(String json, Class<T> valueType) {
        try {
            return OBJECT_MAPPER.readValue(json, valueType);
        }
        catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("JSON 反序列化失败", exception);
        }
    }

    /**
     * 将 JSON 字符串反序列化为带泛型的对象，例如 {@code List<ParsedBlock>}。
     *
     * @param json JSON 字符串
     * @param typeReference 目标泛型类型引用
     * @param <T> 目标对象泛型类型
     * @return 反序列化后的对象
     * @throws IllegalArgumentException JSON 不合法或无法转换为目标类型时抛出
     */
    public static <T> T fromJson(String json, TypeReference<T> typeReference) {
        try {
            return OBJECT_MAPPER.readValue(json, typeReference);
        }
        catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("JSON 反序列化失败", exception);
        }
    }
}
