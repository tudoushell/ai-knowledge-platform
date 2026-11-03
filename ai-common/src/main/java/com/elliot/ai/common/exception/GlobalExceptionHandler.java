package com.elliot.ai.common.exception;

import com.elliot.ai.common.dto.Result;
import com.elliot.ai.common.enums.ResultCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 全局异常处理。 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理业务异常。
     *
     * @param exception 业务异常
     * @return 统一失败响应
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException exception) {
        if (exception.getResultCode() == ResultCode.FAIL) {
            log.error("业务处理失败: {}", exception.getMessage(), exception);
        } else {
            log.warn("业务请求被拒绝: code={}, message={}",
                    exception.getResultCode().getCode(), exception.getMessage());
        }
        return Result.buildFail(exception.getResultCode(), exception.getMessage());
    }

    /**
     * 处理请求体参数校验异常。
     *
     * @param exception 参数校验异常
     * @return 统一失败响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        FieldError fieldError = exception.getBindingResult().getFieldError();
        String message = fieldError == null ? ResultCode.PARAM_ERROR.getMessage() : fieldError.getDefaultMessage();
        log.warn("请求参数校验失败: field={}, message={}",
                fieldError == null ? "未知" : fieldError.getField(), message);
        return Result.buildFail(ResultCode.PARAM_ERROR, message);
    }

    /**
     * 处理未捕获的系统异常。
     *
     * @param exception 系统异常
     * @return 统一失败响应
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception exception) {
        log.error("未处理的系统异常", exception);
        return Result.buildFail(ResultCode.FAIL, exception.getMessage());
    }
}
