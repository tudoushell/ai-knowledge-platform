package com.elliot.ai.common.enums;

/** 接口响应状态码。 */
public enum ResultCode {

    /** 请求处理成功。 */
    SUCCESS(200, "成功"),

    /** 请求参数错误。 */
    PARAM_ERROR(400, "参数错误"),

    /** 请求资源不存在。 */
    NOT_FOUND(404, "资源不存在"),

    /** 服务端处理失败。 */
    FAIL(500, "失败");

    /** 业务状态码。 */
    private final Integer code;

    /** 状态码默认消息。 */
    private final String message;

    /**
     * 构造响应状态码。
     *
     * @param code 业务状态码
     * @param message 状态码默认消息
     */
    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 获取业务状态码。
     *
     * @return 业务状态码
     */
    public Integer getCode() {
        return code;
    }

    /**
     * 获取状态码默认消息。
     *
     * @return 状态码默认消息
     */
    public String getMessage() {
        return message;
    }
}
