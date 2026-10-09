package com.campus.smart.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.smart.common.Result;
import com.campus.smart.entity.Category;
import com.campus.smart.service.CategoryService;
import com.campus.smart.utils.SchoolContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 分类模块控制器
 */
@Tag(name = "分类管理")
@RestController
@RequestMapping("/api/category")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    /**
     * 二手商品分类列表
     */
    @Operation(summary = "分类列表")
    @GetMapping("/list")
    public Result<List<Category>> list() {
        Long schoolId = SchoolContext.getSchoolId();
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        if (schoolId != null) {
            wrapper.eq(Category::getSchoolId, schoolId);
        }
        wrapper.orderByAsc(Category::getSort);
        List<Category> list = categoryService.list(wrapper);
        return Result.success(list);
    }
}
