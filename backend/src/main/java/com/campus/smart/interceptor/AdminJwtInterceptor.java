package com.campus.smart.interceptor;

import com.campus.smart.common.Result;
import com.campus.smart.common.ResultCode;
import com.campus.smart.utils.AdminContext;
import com.campus.smart.utils.JwtUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 管理员JWT拦截器
 * 用于拦截管理端接口，验证管理员token并将管理员ID存入AdminContext
 */
@Component
public class AdminJwtInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 放行OPTIONS请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 从请求头获取token
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        // token为空，返回未登录
        if (token == null || token.isEmpty()) {
            writeError(response, ResultCode.UNAUTHORIZED.getCode(), "请先登录");
            return false;
        }

        try {
            // 验证token是否过期
            if (jwtUtils.isExpired(token)) {
                writeError(response, ResultCode.UNAUTHORIZED.getCode(), "token已过期，请重新登录");
                return false;
            }

            // 解析token
            Claims claims = jwtUtils.parseToken(token);

            // 验证角色是否为管理员
            String role = claims.get("role") != null ? claims.get("role").toString() : "user";
            if (!"admin".equals(role)) {
                writeError(response, ResultCode.FORBIDDEN.getCode(), "无管理员权限");
                return false;
            }

            // 获取管理员ID并存入上下文
            Long adminId = Long.valueOf(claims.get("adminId").toString());
            AdminContext.setAdminId(adminId);
            return true;
        } catch (Exception e) {
            writeError(response, ResultCode.UNAUTHORIZED.getCode(), "无效的token");
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 请求完成后清除当前线程的管理员ID
        AdminContext.remove();
    }

    /**
     * 输出错误信息
     */
    private void writeError(HttpServletResponse response, Integer code, String msg) throws Exception {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(code);
        response.getWriter().write(objectMapper.writeValueAsString(Result.error(code, msg)));
    }
}
