package com.campus.smart.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import javax.servlet.http.HttpServletRequest;
import java.sql.SQLException;

/**
 * 全局异常处理
 *
 * <p>背景：项目原先没有任何异常处理器，任何数据库/参数异常都会直接抛出 Spring Boot
 * 默认的 500 JSON（{@code {timestamp,status,error,path}}），前端 {@code request.js}
 * 只能显示笼统的「服务器开小差了」，无法定位问题。</p>
 *
 * <p>统一改为返回项目自身的 {@code {code,msg,data}} 结构，使前端能展示真实错误信息。</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 参数校验失败（业务代码主动抛出）
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegalArgument(IllegalArgumentException e, HttpServletRequest request) {
        log.warn("参数错误 {}: {}", request.getRequestURI(), e.getMessage());
        return Result.error(ResultCode.PARAM_ERROR.getCode(), e.getMessage());
    }

    /**
     * 缺少必要请求参数
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissingParam(MissingServletRequestParameterException e, HttpServletRequest request) {
        log.warn("缺少请求参数 {}: {}", request.getRequestURI(), e.getParameterName());
        return Result.error(ResultCode.PARAM_ERROR.getCode(), "缺少必要参数：" + e.getParameterName());
    }

    /**
     * 参数类型不匹配
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        log.warn("参数类型错误 {}: {}", request.getRequestURI(), e.getMessage());
        return Result.error(ResultCode.PARAM_ERROR.getCode(), "参数格式不正确：" + e.getName());
    }

    /**
     * 请求体格式错误（JSON 无法解析 / 字段类型不匹配）
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleNotReadable(HttpMessageNotReadableException e, HttpServletRequest request) {
        log.warn("请求体解析失败 {}: {}", request.getRequestURI(), e.getMessage());
        return Result.error(ResultCode.PARAM_ERROR.getCode(), "请求数据格式不正确");
    }

    /**
     * 数据库完整性约束冲突（必填字段缺失、枚举类型不符、外键失败等）
     *
     * <p>把底层 SQL 原因翻译成用户可读的提示，同时保留完整日志便于排查。</p>
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public Result<Void> handleDataIntegrity(DataIntegrityViolationException e, HttpServletRequest request) {
        log.error("数据完整性异常 {}", request.getRequestURI(), e);
        return Result.error("数据保存失败：" + humanize(e));
    }

    /**
     * 其余未捕获异常
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常 {}", request.getRequestURI(), e);
        return Result.error(ResultCode.ERROR.getCode(), "服务器繁忙，请稍后重试");
    }

    /**
     * 把底层异常信息转成简短可读的提示
     */
    private String humanize(Throwable e) {
        Throwable root = e;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String msg = root.getMessage();
        if (msg == null) {
            return "数据校验未通过";
        }
        if (msg.contains("doesn't have a default value")) {
            int start = msg.indexOf("Field '");
            if (start >= 0) {
                int end = msg.indexOf('\'', start + 7);
                if (end > start) {
                    return "缺少必填字段 " + msg.substring(start + 7, end);
                }
            }
            return "缺少必填字段";
        }
        if (msg.contains("Incorrect") && msg.contains("value")) {
            return "字段类型不正确";
        }
        if (msg.contains("foreign key constraint fails")) {
            return "关联数据不存在";
        }
        if (msg.contains("Duplicate entry")) {
            return "数据已存在，请勿重复提交";
        }
        if (msg.contains("cannot be null")) {
            int start = msg.indexOf("Column '");
            if (start >= 0) {
                int end = msg.indexOf('\'', start + 8);
                if (end > start) {
                    return "缺少必填字段 " + msg.substring(start + 8, end);
                }
            }
            return "缺少必填字段";
        }
        if (root instanceof SQLException) {
            return "数据保存失败";
        }
        return "数据校验未通过";
    }
}
