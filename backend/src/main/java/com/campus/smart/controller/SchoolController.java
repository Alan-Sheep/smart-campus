package com.campus.smart.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.smart.common.Result;
import com.campus.smart.common.ReqParams;
import com.campus.smart.entity.School;
import com.campus.smart.service.SchoolService;
import com.campus.smart.utils.SchoolContext;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 学校模块控制器
 */
@Tag(name = "学校模块")
@RestController
@RequestMapping("/api/school")
public class SchoolController {

    @Autowired
    private SchoolService schoolService;

    /**
     * 获取学校列表（全部，支持搜索）
     */
    @Operation(summary = "获取学校列表")
    @GetMapping("/list")
    public Result<List<School>> list(@RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<School> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(School::getName, keyword);
        }
        wrapper.orderByDesc(School::getSort);
        List<School> list = schoolService.list(wrapper);
        return Result.success(list);
    }

    /**
     * 获取当前学校信息
     */
    @Operation(summary = "获取当前学校信息")
    @GetMapping("/current")
    public Result<School> current() {
        Long schoolId = SchoolContext.getSchoolId();
        if (schoolId == null) {
            // 如果没有传学校ID，返回第一个学校
            LambdaQueryWrapper<School> wrapper = new LambdaQueryWrapper<>();
            wrapper.orderByAsc(School::getId);
            wrapper.last("limit 1");
            School school = schoolService.getOne(wrapper);
            return Result.success(school);
        }
        School school = schoolService.getById(schoolId);
        return Result.success(school);
    }

    /**
     * 切换学校
     *
     * <p>前端 {@code pages/school/school.vue} 切换学校时会调用
     * {@code /api/school/switch}，后端原先缺少该接口（404），
     * 页面只能走「降级为仅切换本地状态」的分支。此处补齐。</p>
     *
     * <p>学校归属由请求头 {@code School-Id} 承载（{@link SchoolContext}），
     * 属于无状态设计，服务端无需持久化；这里校验学校存在且启用后，
     * 回传学校信息供前端写入本地缓存。</p>
     */
    @Operation(summary = "切换学校")
    @PostMapping("/switch")
    public Result<School> switchSchool(HttpServletRequest request) {
        Long schoolId = ReqParams.requireLong(request, "schoolId");
        School school = schoolService.getById(schoolId);
        if (school == null) {
            return Result.error("学校不存在");
        }
        if (school.getStatus() != null && school.getStatus() != 1) {
            return Result.error("该学校已停用");
        }
        // 同步到当前线程上下文，便于本次请求后续逻辑使用
        SchoolContext.setSchoolId(schoolId);
        return Result.success(school);
    }

    /**
     * 搜索学校（按名称模糊匹配）
     *
     * <p>前端 {@code schoolApi.searchSchool} 调用 {@code /api/school/search}，
     * 后端原先缺少该接口（404）。与 /list 的 keyword 能力一致，单独提供便于语义清晰。</p>
     */
    @Operation(summary = "搜索学校")
    @GetMapping("/search")
    public Result<List<School>> search(@RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<School> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.like(School::getName, keyword.trim());
        }
        wrapper.eq(School::getStatus, 1);
        wrapper.orderByDesc(School::getSort);
        return Result.success(schoolService.list(wrapper));
    }
}
