package com.campus.smart.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.smart.common.Result;
import com.campus.smart.entity.User;
import com.campus.smart.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端-用户管理控制器
 */
@Tag(name = "【管理端】用户管理")
@RestController
@RequestMapping("/api/admin/user")
public class AdminUserController {

    @Autowired
    private UserService userService;

    /**
     * 用户列表
     * 分页 + 搜索：按昵称、手机号、学校ID筛选
     */
    @Operation(summary = "用户列表")
    @GetMapping("/list")
    public Result<IPage<User>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) Long schoolId) {

        // 构建查询条件
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        // 按昵称模糊搜索
        if (StringUtils.hasText(nickname)) {
            wrapper.like(User::getNickname, nickname);
        }
        // 按手机号模糊搜索
        if (StringUtils.hasText(phone)) {
            wrapper.like(User::getPhone, phone);
        }
        // 按学校ID筛选
        if (schoolId != null) {
            wrapper.eq(User::getSchoolId, schoolId);
        }
        // 按创建时间倒序排列
        wrapper.orderByDesc(User::getCreateTime);

        // 分页查询
        Page<User> page = new Page<>(pageNum, pageSize);
        IPage<User> result = userService.page(page, wrapper);
        return Result.success(result);
    }

    /**
     * 用户详情
     */
    @Operation(summary = "用户详情")
    @GetMapping("/detail/{id}")
    public Result<User> detail(@PathVariable Long id) {
        User user = userService.getById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }
        return Result.success(user);
    }

    /**
     * 禁用/启用用户
     * status: 1-启用, 0-禁用
     */
    @Operation(summary = "禁用/启用用户")
    @PutMapping("/updateStatus/{id}")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        User user = userService.getById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }

        User updateUser = new User();
        updateUser.setId(id);
        updateUser.setStatus(status);
        userService.updateById(updateUser);
        return Result.success();
    }

    /**
     * 修改用户信息
     */
    @Operation(summary = "修改用户信息")
    @PutMapping("/update")
    public Result<Void> update(@RequestBody User user) {
        if (user.getId() == null) {
            return Result.error("用户ID不能为空");
        }

        User existUser = userService.getById(user.getId());
        if (existUser == null) {
            return Result.error("用户不存在");
        }

        // 只允许修改的字段
        User updateUser = new User();
        updateUser.setId(user.getId());
        if (user.getNickname() != null) {
            updateUser.setNickname(user.getNickname());
        }
        if (user.getAvatar() != null) {
            updateUser.setAvatar(user.getAvatar());
        }
        if (user.getPhone() != null) {
            updateUser.setPhone(user.getPhone());
        }
        if (user.getSchoolId() != null) {
            updateUser.setSchoolId(user.getSchoolId());
        }
        if (user.getStatus() != null) {
            updateUser.setStatus(user.getStatus());
        }

        userService.updateById(updateUser);
        return Result.success();
    }
}
