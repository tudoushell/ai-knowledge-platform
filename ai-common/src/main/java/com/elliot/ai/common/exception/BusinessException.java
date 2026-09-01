package com.elliot.ai.common.exception;

import com.elliot.ai.common.enums.ResultCode;

/** 通用业务异常。 */
public class BusinessException extends RuntimeException {

    /** 业务异常对应的响应状态码。 */
    private final ResultCode resultCode;

    /**
     * 使用响应状态码默认消息创建业务异常。
     *
     * @param resultCode 响应状态码
     */
    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.resultCode = resultCode;
    }

    /**
     * 使用自定义消息创建业务异常。
     *
     * @param resultCode 响应状态码
     * @param message 自定义异常消息
     */
    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.resultCode = resultCode;
    }

    /**
     * 获取响应状态码。
     *
     * @return 响应状态码
     */
    public ResultCode getResultCode() {
        return resultCode;
    }
}
