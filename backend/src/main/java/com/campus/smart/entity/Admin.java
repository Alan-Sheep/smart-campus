package com.campus.smart.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("admin")
public class Admin extends BaseEntity {

    private String username;

    private String password;

    private String nickname;

    private String avatar;

    private Long schoolId;

    private String role;

    private Integer status;

    private LocalDateTime lastLoginTime;
}
