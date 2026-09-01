package com.elliot.ai.common.exception;

import com.elliot.ai.common.dto.Result;
import com.elliot.ai.common.enums.ResultCode;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 全局异常处理。 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常。
     *
     * @param exception 业务异常
     * @return 统一失败响应
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException exception) {
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
        return Result.buildFail(ResultCode.FAIL, exception.getMessage());
    }
}
