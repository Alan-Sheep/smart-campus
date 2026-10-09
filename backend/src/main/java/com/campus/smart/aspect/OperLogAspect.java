package com.campus.smart.aspect;

import com.campus.smart.annotation.OperLog;
import com.campus.smart.utils.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
@Slf4j
public class OperLogAspect {

    @Around("@annotation(operLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperLog operLog) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = signature.getMethod().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        Long userId = UserContext.getUserId();

        log.info("[操作日志] 用户ID={}, 操作={}, 类={}, 方法={}, 参数={}",
                userId, operLog.value(), className, methodName, Arrays.toString(joinPoint.getArgs()));

        long startTime = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long costTime = System.currentTimeMillis() - startTime;

        log.info("[操作日志] 操作={}, 耗时={}ms", operLog.value(), costTime);
        return result;
    }
}
