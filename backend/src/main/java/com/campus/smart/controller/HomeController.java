package com.campus.smart.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.smart.common.Result;
import com.campus.smart.entity.Banner;
import com.campus.smart.entity.Notice;
import com.campus.smart.common.PageQuery;
import com.campus.smart.service.BannerService;
import com.campus.smart.service.NoticeService;
import com.campus.smart.utils.RedisUtils;
import com.campus.smart.utils.SchoolContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 首页模块控制器
 */
@Tag(name = "首页模块")
@RestController
@RequestMapping("/api/home")
public class HomeController {

    @Autowired
    private BannerService bannerService;

    @Autowired
    private NoticeService noticeService;

    @Autowired
    private RedisUtils redisUtils;

    /**
     * 支持 Java 8 时间类型的 Jackson ObjectMapper
     * 用于 Redis 缓存数据的类型转换
     */
      private final ObjectMapper objectMapper = com.campus.smart.config.RedisConfig.buildObjectMapper();

    /**
     * 首页数据（轮播图+公告+功能模块配置）
     * 加入 Redis 缓存：bannerList 和 noticeList 分别缓存，过期时间 1 小时
     */
    @Operation(summary = "首页数据")
    @GetMapping("/index")
    public Result<Map<String, Object>> index() {
        Long schoolId = SchoolContext.getSchoolId();
        // schoolId 为 null 时用 "default" 作为缓存 key 的一部分
        String schoolKey = schoolId != null ? String.valueOf(schoolId) : "default";

        // ===================== 轮播图缓存 =====================
        List<Banner> bannerList = null;
        String bannerCacheKey = "campus:home:banner:" + schoolKey;
        try {
            // 先从 Redis 缓存中读取
            Object cacheBannerObj = redisUtils.get(bannerCacheKey);
            if (cacheBannerObj != null) {
                // 缓存命中，将 Object 转换为 List<Banner>
                bannerList = objectMapper.convertValue(cacheBannerObj,
                        new TypeReference<List<Banner>>() {});
            }
        } catch (Exception e) {
            // Redis 读取失败，打印日志，继续走数据库查询
            System.err.println("首页轮播图缓存读取异常: " + e.getMessage());
        }

        // 缓存未命中，查询数据库
        if (bannerList == null) {
            LambdaQueryWrapper<Banner> bannerWrapper = new LambdaQueryWrapper<>();
            if (schoolId != null) {
                bannerWrapper.eq(Banner::getSchoolId, schoolId);
            }
            bannerWrapper.eq(Banner::getStatus, 1);
            bannerWrapper.orderByDesc(Banner::getSort);
            bannerList = bannerService.list(bannerWrapper);

            // 写入 Redis 缓存，过期时间 1 小时（3600秒）
            try {
                redisUtils.setWithExpire(bannerCacheKey, bannerList, 3600);
            } catch (Exception e) {
                System.err.println("首页轮播图缓存写入异常: " + e.getMessage());
            }
        }

        // ===================== 公告缓存 =====================
        List<Notice> noticeList = null;
        String noticeCacheKey = "campus:home:notice:" + schoolKey;
        try {
            // 先从 Redis 缓存中读取
            Object cacheNoticeObj = redisUtils.get(noticeCacheKey);
            if (cacheNoticeObj != null) {
                // 缓存命中，将 Object 转换为 List<Notice>
                noticeList = objectMapper.convertValue(cacheNoticeObj,
                        new TypeReference<List<Notice>>() {});
            }
        } catch (Exception e) {
            // Redis 读取失败，打印日志，继续走数据库查询
            System.err.println("首页公告缓存读取异常: " + e.getMessage());
        }

        // 缓存未命中，查询数据库（取最新5条）
        if (noticeList == null) {
            LambdaQueryWrapper<Notice> noticeWrapper = new LambdaQueryWrapper<>();
            if (schoolId != null) {
                noticeWrapper.eq(Notice::getSchoolId, schoolId);
            }
            noticeWrapper.eq(Notice::getStatus, 1);
            noticeWrapper.orderByDesc(Notice::getCreateTime);
            noticeWrapper.last("limit 5");
            noticeList = noticeService.list(noticeWrapper);

            // 写入 Redis 缓存，过期时间 1 小时（3600秒）
            try {
                redisUtils.setWithExpire(noticeCacheKey, noticeList, 3600);
            } catch (Exception e) {
                System.err.println("首页公告缓存写入异常: " + e.getMessage());
            }
        }

        // 功能模块配置（模拟数据）
        Map<String, Object> modules = new HashMap<>();
        modules.put("express", true);
        modules.put("print", true);
        modules.put("errand", true);
        modules.put("substitute", true);
        modules.put("activity", true);
        modules.put("goods", true);
        modules.put("game", true);
        modules.put("lostfound", true);
        modules.put("channel", true);

        Map<String, Object> result = new HashMap<>();
        result.put("bannerList", bannerList);
        result.put("noticeList", noticeList);
        result.put("modules", modules);

        return Result.success(result);
    }

    /**
     * 轮播图列表
     * 加入 Redis 缓存，过期时间 1 小时
     */
    @Operation(summary = "轮播图列表")
    @GetMapping("/bannerList")
    public Result<List<Banner>> bannerList() {
        Long schoolId = SchoolContext.getSchoolId();
        String schoolKey = schoolId != null ? String.valueOf(schoolId) : "default";
        String cacheKey = "campus:home:banner:" + schoolKey;

        List<Banner> list = null;
        try {
            // 先从 Redis 缓存中读取
            Object cacheObj = redisUtils.get(cacheKey);
            if (cacheObj != null) {
                list = objectMapper.convertValue(cacheObj,
                        new TypeReference<List<Banner>>() {});
            }
        } catch (Exception e) {
            System.err.println("轮播图列表缓存读取异常: " + e.getMessage());
        }

        // 缓存未命中，查询数据库
        if (list == null) {
            LambdaQueryWrapper<Banner> wrapper = new LambdaQueryWrapper<>();
            if (schoolId != null) {
                wrapper.eq(Banner::getSchoolId, schoolId);
            }
            wrapper.eq(Banner::getStatus, 1);
            wrapper.orderByDesc(Banner::getSort);
            list = bannerService.list(wrapper);

            // 写入缓存，1小时过期
            try {
                redisUtils.setWithExpire(cacheKey, list, 3600);
            } catch (Exception e) {
                System.err.println("轮播图列表缓存写入异常: " + e.getMessage());
            }
        }

        return Result.success(list);
    }

    /**
     * 公告列表
     */
    @Operation(summary = "公告列表")
    @GetMapping("/noticeList")
    public Result<Page<Notice>> noticeList(PageQuery pageQuery) {
        Long schoolId = SchoolContext.getSchoolId();
        LambdaQueryWrapper<Notice> wrapper = new LambdaQueryWrapper<>();
        if (schoolId != null) {
            wrapper.eq(Notice::getSchoolId, schoolId);
        }
        wrapper.eq(Notice::getStatus, 1);
        wrapper.orderByDesc(Notice::getCreateTime);

        Page<Notice> page = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());
        Page<Notice> result = noticeService.page(page, wrapper);
        return Result.success(result);
    }
}
