package com.campus.smart.config;

import com.campus.smart.interceptor.AdminJwtInterceptor;
import com.campus.smart.interceptor.JwtInterceptor;
import com.campus.smart.interceptor.SchoolIdInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private JwtInterceptor jwtInterceptor;

    @Autowired
    private SchoolIdInterceptor schoolIdInterceptor;

    @Autowired
    private AdminJwtInterceptor adminJwtInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 学校ID拦截器 - 所有接口都需要获取学校ID
        registry.addInterceptor(schoolIdInterceptor)
                .addPathPatterns("/api/**");

        // 管理员JWT拦截器 - 管理端接口
        registry.addInterceptor(adminJwtInterceptor)
                .addPathPatterns("/api/admin/**")
                .excludePathPatterns(
                        // 管理员登录接口放行
                        "/api/admin/auth/login"
                );

        // 用户JWT拦截器 - 需要登录的用户端接口
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        // 管理端接口由管理员拦截器处理
                        "/api/admin/**",
                        // 用户模块 - 登录相关
                        "/api/user/login",
                        "/api/user/register",
                        "/api/user/send-code",
                        "/api/user/sendCode",
                        "/api/user/sendEmailCode",
                        // 学校模块 - 公开
                        "/api/school/list",
                        "/api/school/current",
                        "/api/school/search",
                        // 切换学校：游客也应能切换（学校归属由 School-Id 请求头承载，
                        // 强制登录反而导致未登录时前端切学校报 401、只能走本地降级）
                        "/api/school/switch",
                        // 首页模块 - 公开
                        "/api/home/index",
                        "/api/home/bannerList",
                        "/api/home/noticeList",
                        // 活动模块 - 列表和详情公开
                        "/api/activity/list",
                        "/api/activity/detail",
                        // 商品模块 - 列表和详情公开
                        "/api/goods/list",
                        "/api/goods/detail",
                        // 分类模块 - 公开
                        "/api/category/list",
                        // 游戏搭子 - 列表和详情公开
                        "/api/game/list",
                        "/api/game/detail",
                        "/api/game/gameTypes",
                        // 失物招领 - 列表和详情公开
                        "/api/lostfound/list",
                        "/api/lostfound/detail",
                        // 校园频道 - 列表和详情公开
                        "/api/channel/postList",
                        "/api/channel/postDetail",
                        "/api/channel/commentList",
                        // 快递代取 - 大厅列表公开
                        "/api/express/hallList",
                        "/api/express/orderDetail",
                        // 校园跑腿 - 列表和详情公开
                        "/api/errand/orderList",
                        "/api/errand/orderDetail",
                        // 代课服务 - 列表和详情公开
                        "/api/substitute/orderList",
                        "/api/substitute/orderDetail",
                        // 打印服务 - 价格配置公开
                        "/api/print/priceConfig",
                        // 文件上传
                        "/api/file/upload",
                        // swagger相关
                        "/doc.html",
                        "/swagger-resources/**",
                        "/webjars/**",
                        "/v3/api-docs/**",
                        "/favicon.ico"
                );
    }
}
