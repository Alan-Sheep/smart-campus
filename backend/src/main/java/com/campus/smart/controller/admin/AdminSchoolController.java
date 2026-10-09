package com.campus.smart.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.smart.common.Result;
import com.campus.smart.entity.School;
import com.campus.smart.service.SchoolService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理端-学校管理控制器
 */
@Tag(name = "【管理端】学校管理")
@RestController
@RequestMapping("/api/admin/school")
public class AdminSchoolController {

    @Autowired
    private SchoolService schoolService;

    /**
     * 学校列表（分页）
     */
    @Operation(summary = "学校列表")
    @GetMapping("/list")
    public Result<IPage<School>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer status) {

        LambdaQueryWrapper<School> wrapper = new LambdaQueryWrapper<>();
        if (name != null && !name.isEmpty()) {
            wrapper.like(School::getName, name);
        }
        if (status != null) {
            wrapper.eq(School::getStatus, status);
        }
        wrapper.orderByAsc(School::getSort);
        wrapper.orderByDesc(School::getCreateTime);

        Page<School> page = new Page<>(pageNum, pageSize);
        IPage<School> result = schoolService.page(page, wrapper);
        return Result.success(result);
    }

    /**
     * 全部学校（不分页，下拉选择用）
     */
    @Operation(summary = "全部学校列表")
    @GetMapping("/all")
    public Result<List<School>> all() {
        LambdaQueryWrapper<School> wrapper = new LambdaQueryWrapper<>();
        // 只返回启用的学校
        wrapper.eq(School::getStatus, 1);
        wrapper.orderByAsc(School::getSort);
        List<School> list = schoolService.list(wrapper);
        return Result.success(list);
    }

    /**
     * 新增学校
     */
    @Operation(summary = "新增学校")
    @PostMapping("/add")
    public Result<Void> add(@RequestBody School school) {
        if (school.getName() == null || school.getName().isEmpty()) {
            return Result.error("请输入学校名称");
        }

        // 检查名称是否重复
        LambdaQueryWrapper<School> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(School::getName, school.getName());
        if (schoolService.count(wrapper) > 0) {
            return Result.error("学校名称已存在");
        }

        // 默认值
        if (school.getSort() == null) {
            school.setSort(0);
        }
        if (school.getStatus() == null) {
            school.setStatus(1);
        }

        schoolService.save(school);
        return Result.success();
    }

    /**
     * 修改学校
     */
    @Operation(summary = "修改学校")
    @PutMapping("/update")
    public Result<Void> update(@RequestBody School school) {
        if (school.getId() == null) {
            return Result.error("ID不能为空");
        }

        School exist = schoolService.getById(school.getId());
        if (exist == null) {
            return Result.error("学校不存在");
        }

        // 如果修改了名称，检查是否重复
        if (school.getName() != null && !school.getName().equals(exist.getName())) {
            LambdaQueryWrapper<School> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(School::getName, school.getName());
            wrapper.ne(School::getId, school.getId());
            if (schoolService.count(wrapper) > 0) {
                return Result.error("学校名称已存在");
            }
        }

        schoolService.updateById(school);
        return Result.success();
    }

    /**
     * 删除学校
     */
    @Operation(summary = "删除学校")
    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        School school = schoolService.getById(id);
        if (school == null) {
            return Result.error("学校不存在");
        }

        schoolService.removeById(id);
        return Result.success();
    }
}
