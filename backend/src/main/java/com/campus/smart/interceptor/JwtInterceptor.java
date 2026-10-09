package com.campus.smart.interceptor;

import com.campus.smart.common.Result;
import com.campus.smart.common.ResultCode;
import com.campus.smart.utils.JwtUtils;
import com.campus.smart.utils.UserContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        if (token == null || token.isEmpty()) {
            writeError(response, ResultCode.UNAUTHORIZED.getCode(), "请先登录");
            return false;
        }

        try {
            if (jwtUtils.isExpired(token)) {
                writeError(response, ResultCode.UNAUTHORIZED.getCode(), "token已过期，请重新登录");
                return false;
            }
            Claims claims = jwtUtils.parseToken(token);
            Long userId = Long.valueOf(claims.get("userId").toString());
            UserContext.setUserId(userId);
            return true;
        } catch (Exception e) {
            writeError(response, ResultCode.UNAUTHORIZED.getCode(), "无效的token");
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.remove();
    }

    private void writeError(HttpServletResponse response, Integer code, String msg) throws Exception {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(code);
        response.getWriter().write(objectMapper.writeValueAsString(Result.error(code, msg)));
    }
}
