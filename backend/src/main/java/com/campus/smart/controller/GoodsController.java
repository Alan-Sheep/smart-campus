package com.campus.smart.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.smart.common.Result;
import com.campus.smart.entity.Category;
import com.campus.smart.entity.Goods;
import com.campus.smart.common.PageQuery;
import com.campus.smart.common.ReqParams;
import com.campus.smart.service.CategoryService;
import com.campus.smart.service.GoodsService;
import com.campus.smart.utils.RedisUtils;
import com.campus.smart.utils.SchoolContext;
import com.campus.smart.utils.UserContext;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * 二手市场模块控制器
 */
@Tag(name = "二手市场")
@RestController
@RequestMapping("/api/goods")
public class GoodsController {

    @Autowired
    private GoodsService goodsService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private RedisUtils redisUtils;

    /**
     * 商品分类列表
     */
    @Operation(summary = "商品分类列表")
    @GetMapping("/categories")
    public Result<java.util.List<Category>> categories() {
        Long schoolId = SchoolContext.getSchoolId();
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        if (schoolId != null) {
            wrapper.eq(Category::getSchoolId, schoolId);
        }
        wrapper.orderByAsc(Category::getSort);
        return Result.success(categoryService.list(wrapper));
    }

    /**
     * 商品列表（分页+分类+搜索）
     */
    @Operation(summary = "商品列表")
    @GetMapping("/list")
    public Result<Page<Goods>> list(PageQuery pageQuery,
                                     @RequestParam(required = false) String categoryId,
                                     @RequestParam(required = false) String keyword,
                                     @RequestParam(required = false) String sort) {
        Long schoolId = SchoolContext.getSchoolId();
        LambdaQueryWrapper<Goods> wrapper = new LambdaQueryWrapper<>();

        if (schoolId != null) {
            wrapper.eq(Goods::getSchoolId, schoolId);
        }
        // categoryId 兼容：传 "all" 或空表示全部，传数字按分类过滤
        if (categoryId != null && !categoryId.isEmpty() && !"all".equalsIgnoreCase(categoryId)) {
            try {
                wrapper.eq(Goods::getCategoryId, Long.parseLong(categoryId));
            } catch (NumberFormatException e) {
                // 格式不对就忽略分类过滤
            }
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(Goods::getTitle, keyword);
        }

        // 状态：已上架
        wrapper.eq(Goods::getStatus, 1);

        // 排序
                  // 排序：价格升降序 / 最新（时间倒序）。
          // 次级排序键用 id：演示数据 create_time 相同秒级时保证稳定顺序
          if ("price_asc".equals(sort)) {
              wrapper.orderByAsc(Goods::getPrice).orderByDesc(Goods::getId);
          } else if ("price_desc".equals(sort)) {
              wrapper.orderByDesc(Goods::getPrice).orderByDesc(Goods::getId);
          } else {
              wrapper.orderByDesc(Goods::getId);  // 最新：id 自增即时间序
          }

        Page<Goods> page = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());
        Page<Goods> result = goodsService.page(page, wrapper);
        return Result.success(result);
    }

    /**
     * 商品详情
     * 加入 Redis 浏览量计数：每次访问详情，Redis 自增 1
     * 返回的浏览量 = 数据库基础值 + Redis 增量（实时+准确）
     */
    @Operation(summary = "商品详情")
    @GetMapping("/detail")
    public Result<Goods> detail(@RequestParam Long id) {
        Goods goods = goodsService.getById(id);
        if (goods == null) {
            return Result.error("商品不存在");
        }

        // Redis 浏览量自增，并合并返回
        String viewKey = "campus:goods:view:" + id;
        try {
            // 使用 Redis increment 自增浏览量
            long incrementCount = redisUtils.increment(viewKey);

            // 如果 increment 返回 -1 表示 Redis 操作失败，直接返回数据库的值
            if (incrementCount >= 0) {
                // 浏览量 = 数据库的基础值 + Redis 中的增量
                // 注意：数据库里的 viewCount 是历史累计值，Redis 里是新增的增量
                // 实际项目中可以定时将 Redis 增量同步到数据库
                int baseView = goods.getViewCount() != null ? goods.getViewCount() : 0;
                goods.setViewCount(baseView + (int) incrementCount);
            }
        } catch (Exception e) {
            // Redis 操作失败，不影响主业务，返回数据库中的原始浏览量
            System.err.println("商品浏览量计数异常: " + e.getMessage());
        }

        return Result.success(goods);
    }

    /**
     * 发布商品
     */
    @Operation(summary = "发布商品")
    @PostMapping("/create")
    public Result<Goods> create(@RequestBody Goods goods) {
        Long userId = UserContext.getUserId();
        Long schoolId = SchoolContext.getSchoolId();

        goods.setUserId(userId);
        goods.setSchoolId(schoolId);
        // 状态：直接上架
        goods.setStatus(1);
        // 浏览量初始为0
        goods.setViewCount(0);

        goodsService.save(goods);
        return Result.success(goods);
    }

    /**
     * 我发布的商品
     */
    @Operation(summary = "我发布的商品")
    @GetMapping("/myList")
    public Result<Page<Goods>> myList(PageQuery pageQuery,
                                       @RequestParam(required = false) Integer status) {
        Long userId = UserContext.getUserId();
        LambdaQueryWrapper<Goods> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Goods::getUserId, userId);

        if (status != null) {
            wrapper.eq(Goods::getStatus, status);
        }

        wrapper.orderByDesc(Goods::getCreateTime);

        Page<Goods> page = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());
        Page<Goods> result = goodsService.page(page, wrapper);
        return Result.success(result);
    }

    /**
     * 更新商品状态（下架等）
     */
    @Operation(summary = "更新商品状态")
    @PostMapping("/updateStatus")
    public Result<Void> updateStatus(HttpServletRequest request) {
        Long id = ReqParams.requireLong(request, "id");
        Integer status = ReqParams.getInteger(request, "status");
        if (status == null) {
            throw new IllegalArgumentException("缺少必要参数：status");
        }
        Long userId = UserContext.getUserId();
        Goods goods = goodsService.getById(id);
        if (goods == null) {
            return Result.error("商品不存在");
        }
        if (!goods.getUserId().equals(userId)) {
            return Result.error("只能操作自己发布的商品");
        }

        Goods update = new Goods();
        update.setId(id);
        update.setStatus(status);
        goodsService.updateById(update);
        return Result.success();
    }
}
