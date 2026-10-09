package com.campus.smart.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.smart.common.Result;
import com.campus.smart.entity.Admin;
import com.campus.smart.service.AdminService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理端-管理员管理控制器
 */
@Tag(name = "【管理端】管理员管理")
@RestController
@RequestMapping("/api/admin/manage")
public class AdminManageController {

    @Autowired
    private AdminService adminService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 管理员列表
     */
    @Operation(summary = "管理员列表")
    @GetMapping("/list")
    public Result<IPage<Admin>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Long schoolId) {

        LambdaQueryWrapper<Admin> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(username)) {
            wrapper.like(Admin::getUsername, username);
        }
        if (schoolId != null) {
            wrapper.eq(Admin::getSchoolId, schoolId);
        }
        wrapper.orderByDesc(Admin::getCreateTime);

        Page<Admin> page = new Page<>(pageNum, pageSize);
        IPage<Admin> result = adminService.page(page, wrapper);

        // 密码字段不返回
        for (Admin admin : result.getRecords()) {
            admin.setPassword(null);
        }

        return Result.success(result);
    }

    /**
     * 新增管理员
     */
    @Operation(summary = "新增管理员")
    @PostMapping("/add")
    public Result<Void> add(@RequestBody Admin admin) {
        // 参数校验
        if (admin.getUsername() == null || admin.getUsername().isEmpty()) {
            return Result.error("请输入用户名");
        }
        if (admin.getPassword() == null || admin.getPassword().isEmpty()) {
            return Result.error("请输入密码");
        }
        if (admin.getPassword().length() < 6) {
            return Result.error("密码长度不能少于6位");
        }

        // 检查用户名是否已存在
        LambdaQueryWrapper<Admin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Admin::getUsername, admin.getUsername());
        if (adminService.count(wrapper) > 0) {
            return Result.error("用户名已存在");
        }

        // BCrypt加密密码
        admin.setPassword(passwordEncoder.encode(admin.getPassword()));

        // 默认值
        if (admin.getStatus() == null) {
            admin.setStatus(1);
        }
        if (admin.getRole() == null) {
            admin.setRole("admin");
        }

        adminService.save(admin);
        return Result.success();
    }

    /**
     * 修改管理员
     */
    @Operation(summary = "修改管理员")
    @PutMapping("/update")
    public Result<Void> update(@RequestBody Admin admin) {
        if (admin.getId() == null) {
            return Result.error("ID不能为空");
        }

        Admin exist = adminService.getById(admin.getId());
        if (exist == null) {
            return Result.error("管理员不存在");
        }

        // 如果修改了用户名，检查是否重复
        if (admin.getUsername() != null && !admin.getUsername().equals(exist.getUsername())) {
            LambdaQueryWrapper<Admin> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Admin::getUsername, admin.getUsername());
            wrapper.ne(Admin::getId, admin.getId());
            if (adminService.count(wrapper) > 0) {
                return Result.error("用户名已存在");
            }
        }

        // 不允许直接修改密码，密码走重置密码接口
        admin.setPassword(null);

        adminService.updateById(admin);
        return Result.success();
    }

    /**
     * 删除管理员
     */
    @Operation(summary = "删除管理员")
    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Admin admin = adminService.getById(id);
        if (admin == null) {
            return Result.error("管理员不存在");
        }

        // 超级管理员不允许删除（假设id=1是超级管理员）
        if (id != null && id == 1L) {
            return Result.error("超级管理员不允许删除");
        }

        adminService.removeById(id);
        return Result.success();
    }

    /**
     * 重置密码
     */
    @Operation(summary = "重置密码")
    @PutMapping("/resetPassword/{id}")
    public Result<Void> resetPassword(@PathVariable Long id, @RequestBody(required = false) Map<String, String> params) {
        Admin admin = adminService.getById(id);
        if (admin == null) {
            return Result.error("管理员不存在");
        }

        String newPassword = "123456"; // 默认重置密码
        if (params != null && params.get("password") != null && !params.get("password").isEmpty()) {
            newPassword = params.get("password");
        }
        if (newPassword.length() < 6) {
            return Result.error("密码长度不能少于6位");
        }

        Admin update = new Admin();
        update.setId(id);
        update.setPassword(passwordEncoder.encode(newPassword));
        adminService.updateById(update);
        return Result.success();
    }
}
