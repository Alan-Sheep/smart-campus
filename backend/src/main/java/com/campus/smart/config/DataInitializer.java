package com.campus.smart.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.smart.entity.Admin;
import com.campus.smart.entity.School;
import com.campus.smart.service.AdminService;
import com.campus.smart.service.SchoolService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 数据初始化器
 * 项目启动时执行，确保基础数据存在
 */
@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private AdminService adminService;

    @Autowired
    private SchoolService schoolService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        initSchool();
        initAdmin();
        System.out.println("============ 数据初始化完成 ============");
    }

    /**
     * 初始化学校数据
     */
    private void initSchool() {
        long count = schoolService.count();
        if (count == 0) {
            School school1 = new School();
            school1.setName("示例大学");
            school1.setProvince("北京市");
            school1.setCity("北京市");
            school1.setSort(1);
            school1.setStatus(1);
            schoolService.save(school1);

            School school2 = new School();
            school2.setName("北京大学");
            school2.setProvince("北京市");
            school2.setCity("北京市");
            school2.setSort(2);
            school2.setStatus(1);
            schoolService.save(school2);

            School school3 = new School();
            school3.setName("清华大学");
            school3.setProvince("北京市");
            school3.setCity("北京市");
            school3.setSort(3);
            school3.setStatus(1);
            schoolService.save(school3);

            System.out.println("初始化学校数据完成，共3所学校");
        }
    }

    /**
     * 初始化超级管理员
     * 确保 admin 账号存在，密码为 admin123
     */
    private void initAdmin() {
        LambdaQueryWrapper<Admin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Admin::getUsername, "admin");
        Admin admin = adminService.getOne(wrapper);

        if (admin == null) {
            // 不存在则创建
            admin = new Admin();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setNickname("超级管理员");
            admin.setRole("2"); // 超级管理员
            admin.setStatus(1);
            adminService.save(admin);
            System.out.println("初始化超级管理员成功，账号：admin / admin123");
        } else {
            // 已存在则校验密码，如果不对就重置（防止SQL里的hash不正确）
            try {
                if (!passwordEncoder.matches("admin123", admin.getPassword())) {
                    Admin update = new Admin();
                    update.setId(admin.getId());
                    update.setPassword(passwordEncoder.encode("admin123"));
                    adminService.updateById(update);
                    System.out.println("重置管理员密码为：admin123");
                }
            } catch (Exception e) {
                // 密码格式不对，直接重置
                Admin update = new Admin();
                update.setId(admin.getId());
                update.setPassword(passwordEncoder.encode("admin123"));
                adminService.updateById(update);
                System.out.println("管理员密码格式异常，已重置为：admin123");
            }
        }
    }
}
