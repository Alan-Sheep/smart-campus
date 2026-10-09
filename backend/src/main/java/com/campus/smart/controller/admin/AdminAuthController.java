package com.campus.smart.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.smart.common.Result;
import com.campus.smart.entity.Admin;
import com.campus.smart.service.AdminService;
import com.campus.smart.utils.AdminContext;
import com.campus.smart.utils.JwtUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 管理端-登录授权控制器
 */
@Tag(name = "【管理端】登录授权")
@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private JwtUtils jwtUtils;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 管理员登录
     * 用户名密码登录，查询admin表，BCrypt密码校验
     */
    @Operation(summary = "管理员登录")
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> params) {
        String username = params.get("username");
        String password = params.get("password");

        // 参数校验
        if (username == null || username.isEmpty()) {
            return Result.error("请输入用户名");
        }
        if (password == null || password.isEmpty()) {
            return Result.error("请输入密码");
        }

        // 查询管理员
        LambdaQueryWrapper<Admin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Admin::getUsername, username);
        Admin admin = adminService.getOne(wrapper);

        if (admin == null) {
            return Result.error("用户名或密码错误");
        }

        // 校验状态
        if (admin.getStatus() != null && admin.getStatus() == 0) {
            return Result.error("账号已被禁用");
        }

        // BCrypt密码校验
        if (!passwordEncoder.matches(password, admin.getPassword())) {
            return Result.error("用户名或密码错误");
        }

        // 更新最后登录时间
        Admin updateAdmin = new Admin();
        updateAdmin.setId(admin.getId());
        updateAdmin.setLastLoginTime(LocalDateTime.now());
        adminService.updateById(updateAdmin);

        // 生成管理员token
        String token = jwtUtils.generateAdminToken(admin.getId(), admin.getUsername());

        // 返回token和管理员信息（不返回密码）
        admin.setPassword(null);
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("adminInfo", admin);
        return Result.success(result);
    }

    /**
     * 获取当前管理员信息
     */
    @Operation(summary = "获取管理员信息")
    @GetMapping("/info")
    public Result<Admin> getInfo() {
        Long adminId = AdminContext.getAdminId();
        Admin admin = adminService.getById(adminId);
        if (admin == null) {
            return Result.error("管理员不存在");
        }
        // 不返回密码
        admin.setPassword(null);
        return Result.success(admin);
    }

    /**
     * 退出登录
     */
    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public Result<Void> logout() {
        // 清除当前线程的管理员ID
        AdminContext.remove();
        return Result.success();
    }

    /**
     * 修改密码
     */
    @Operation(summary = "修改密码")
    @PutMapping("/updatePassword")
    public Result<Void> updatePassword(@RequestBody Map<String, String> params) {
        Long adminId = AdminContext.getAdminId();
        String oldPassword = params.get("oldPassword");
        String newPassword = params.get("newPassword");

        // 参数校验
        if (oldPassword == null || oldPassword.isEmpty()) {
            return Result.error("请输入原密码");
        }
        if (newPassword == null || newPassword.isEmpty()) {
            return Result.error("请输入新密码");
        }
        if (newPassword.length() < 6) {
            return Result.error("新密码长度不能少于6位");
        }

        // 查询管理员
        Admin admin = adminService.getById(adminId);
        if (admin == null) {
            return Result.error("管理员不存在");
        }

        // 校验原密码
        if (!passwordEncoder.matches(oldPassword, admin.getPassword())) {
            return Result.error("原密码错误");
        }

        // 更新密码
        Admin updateAdmin = new Admin();
        updateAdmin.setId(adminId);
        updateAdmin.setPassword(passwordEncoder.encode(newPassword));
        adminService.updateById(updateAdmin);

        return Result.success();
    }
}
