package com.elliot.ai.common.dto;

import com.elliot.ai.common.enums.ResultCode;

/**
 * 统一接口响应封装。
 *
 * @param code 业务状态码
 * @param message 响应消息
 * @param data 响应数据
 * @param <T> 响应数据类型
 */
public record Result<T>(
        Integer code,
        String message,
        T data
) {

    /**
     * 构建带数据的成功响应。
     *
     * @param data 响应数据
     * @param <T> 响应数据类型
     * @return 成功响应
     */
    public static <T> Result<T> buildSuccess(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    /**
     * 构建无数据的成功响应。
     *
     * @param <T> 响应数据类型
     * @return 成功响应
     */
    public static <T> Result<T> buildSuccess() {
        return buildSuccess(null);
    }

    /**
     * 根据状态码构建失败响应。
     *
     * @param resultCode 响应状态码枚举
     * @param <T> 响应数据类型
     * @return 失败响应
     */
    public static <T> Result<T> buildFail(ResultCode resultCode) {
        return new Result<>(resultCode.getCode(), resultCode.getMessage(), null);
    }

    /**
     * 根据状态码和自定义消息构建失败响应。
     *
     * @param resultCode 响应状态码枚举
     * @param message 自定义错误消息
     * @param <T> 响应数据类型
     * @return 失败响应
     */
    public static <T> Result<T> buildFail(ResultCode resultCode, String message) {
        return new Result<>(resultCode.getCode(), message, null);
    }
}
