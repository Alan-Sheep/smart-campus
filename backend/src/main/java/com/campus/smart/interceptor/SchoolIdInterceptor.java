package com.campus.smart.interceptor;

import com.campus.smart.utils.JwtUtils;
import com.campus.smart.utils.SchoolContext;
import com.campus.smart.utils.UserContext;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 学校ID拦截器
 * 从请求头 School-Id 中获取当前学校ID，存入 SchoolContext
 *
 * <p>同时做「尽力而为」的用户识别：对于未登录也可访问的公开接口
 * （帖子列表/详情、商品列表/详情、快递大厅等），JWT 拦截器是被排除的，
 * 因此 UserContext 不会被填充。</p>
 *
 * <p>由此产生一个真实缺陷：帖子详情会返回当前用户是否已点赞/已收藏
 * （isLiked / isCollected），但公开接口拿不到用户身份，导致这些字段
 * 恒为 false，前端按钮永远显示未点赞、未收藏。</p>
 *
 * <p>这里改为：请求带了合法 token 就解析出 userId 存入 UserContext；
 * 没带或 token 无效则跳过（不拦截、不报错），保持接口的匿名可访问性。</p>
 */
@Component
public class SchoolIdInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtils jwtUtils;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // ---- 低频接口 HTTP 缓存（弱网/隧道场景优化） ----
        String uri = request.getRequestURI();
        if (uri.equals("/api/school/list") || uri.equals("/api/home/bannerList")
                || uri.equals("/api/home/noticeList") || uri.equals("/api/category/list")) {
            response.setHeader("Cache-Control", "public, max-age=300");
        }


        // ---- 学校ID ----
        String schoolIdStr = request.getHeader("School-Id");
        if (schoolIdStr != null && !schoolIdStr.isEmpty()) {
            try {
                Long schoolId = Long.valueOf(schoolIdStr);
                SchoolContext.setSchoolId(schoolId);
            } catch (NumberFormatException e) {
                // 学校ID格式不正确，忽略
            }
        }

        // ---- 尽力识别用户身份（公开接口的登录态） ----
        // JWT 拦截器已对需要登录的接口设置过 UserContext，这里避免重复解析
        if (UserContext.getUserId() == null) {
            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            if (token != null && !token.isEmpty()) {
                try {
                    if (!jwtUtils.isExpired(token)) {
                        Claims claims = jwtUtils.parseToken(token);
                        Object uid = claims.get("userId");
                        if (uid != null) {
                            UserContext.setUserId(Long.valueOf(uid.toString()));
                        }
                    }
                } catch (Exception e) {
                    // token 无效：匿名访问，不拦截
                }
            }
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        SchoolContext.remove();
        UserContext.remove();
    }
}
