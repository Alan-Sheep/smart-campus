package com.campus.smart.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.smart.common.Result;
import com.campus.smart.entity.GameBuddy;
import com.campus.smart.common.PageQuery;
import com.campus.smart.service.GameBuddyService;
import com.campus.smart.utils.SchoolContext;
import com.campus.smart.utils.UserContext;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 游戏搭子模块控制器
 */
@Tag(name = "游戏搭子")
@RestController
@RequestMapping("/api/game")
public class GameController {

    @Autowired
    private GameBuddyService gameBuddyService;

    /**
     * 游戏搭子列表（分页+按游戏筛选）
     */
    @Operation(summary = "游戏搭子列表")
    @GetMapping("/list")
    public Result<Page<GameBuddy>> list(PageQuery pageQuery,
                                           @RequestParam(required = false) String gameName,
                                           @RequestParam(required = false) String keyword) {
        Long schoolId = SchoolContext.getSchoolId();
        LambdaQueryWrapper<GameBuddy> wrapper = new LambdaQueryWrapper<>();

        if (schoolId != null) {
            wrapper.eq(GameBuddy::getSchoolId, schoolId);
        }
        // gameName 兼容：传 "all" 或空表示全部
        if (gameName != null && !gameName.isEmpty() && !"all".equalsIgnoreCase(gameName)) {
            wrapper.eq(GameBuddy::getGameName, gameName);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(GameBuddy::getTitle, keyword);
        }

        // 状态：有效
        wrapper.eq(GameBuddy::getStatus, 1);
        wrapper.orderByDesc(GameBuddy::getCreateTime);

        Page<GameBuddy> page = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());
        Page<GameBuddy> result = gameBuddyService.page(page, wrapper);
        return Result.success(result);
    }

    /**
     * 详情
     */
    @Operation(summary = "游戏搭子详情")
    @GetMapping("/detail")
    public Result<GameBuddy> detail(@RequestParam Long id) {
        GameBuddy gameBuddy = gameBuddyService.getById(id);
        if (gameBuddy == null) {
            return Result.error("记录不存在");
        }
        return Result.success(gameBuddy);
    }

    /**
     * 发布找搭子
     */
    @Operation(summary = "发布找搭子")
    @PostMapping("/create")
    public Result<GameBuddy> create(@RequestBody GameBuddy gameBuddy) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        Long schoolId = SchoolContext.getSchoolId();

        gameBuddy.setUserId(userId);
        gameBuddy.setSchoolId(schoolId);
        // 状态：有效
        gameBuddy.setStatus(1);

        // title 为 NOT NULL 且无默认值，而发布页没有独立的标题输入项，
        // 原实现未赋值导致 "Field 'title' doesn't have a default value"（HTTP 500）。
        // 这里用「游戏名 + 段位」派生一个可读标题。
        if (gameBuddy.getTitle() == null || gameBuddy.getTitle().trim().isEmpty()) {
            String gameName = gameBuddy.getGameName() == null ? "" : gameBuddy.getGameName().trim();
            String level = gameBuddy.getGameLevel() == null ? "" : gameBuddy.getGameLevel().trim();
            StringBuilder title = new StringBuilder();
            if (!gameName.isEmpty()) {
                title.append(gameName);
            }
            if (!level.isEmpty()) {
                title.append(title.length() > 0 ? " " : "").append(level);
            }
            gameBuddy.setTitle(title.length() > 0 ? title.append(" 找搭子").toString() : "找游戏搭子");
        }

        gameBuddyService.save(gameBuddy);
        return Result.success(gameBuddy);
    }

    /**
     * 我发布的
     */
    @Operation(summary = "我发布的")
    @GetMapping("/myList")
    public Result<Page<GameBuddy>> myList(PageQuery pageQuery) {
        Long userId = UserContext.getUserId();
        LambdaQueryWrapper<GameBuddy> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GameBuddy::getUserId, userId);
        wrapper.orderByDesc(GameBuddy::getCreateTime);

        Page<GameBuddy> page = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());
        Page<GameBuddy> result = gameBuddyService.page(page, wrapper);
        return Result.success(result);
    }

    /**
     * 游戏分类列表
     * 从现有数据中去重获取所有游戏名称
     */
    @Operation(summary = "游戏分类列表")
    @GetMapping("/gameTypes")
    public Result<List<String>> gameTypes() {
        Long schoolId = SchoolContext.getSchoolId();
        LambdaQueryWrapper<GameBuddy> wrapper = new LambdaQueryWrapper<>();
        if (schoolId != null) {
            wrapper.eq(GameBuddy::getSchoolId, schoolId);
        }
        wrapper.eq(GameBuddy::getStatus, 1);
        wrapper.select(GameBuddy::getGameName);
        wrapper.groupBy(GameBuddy::getGameName);
        wrapper.orderByAsc(GameBuddy::getGameName);

        List<GameBuddy> list = gameBuddyService.list(wrapper);
        List<String> gameNames = list.stream()
                .map(GameBuddy::getGameName)
                .filter(name -> name != null && !name.isEmpty())
                .collect(java.util.stream.Collectors.toList());
        return Result.success(gameNames);
    }
}
