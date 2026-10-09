package com.campus.smart.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user")
public class User extends BaseEntity {

    private String openid;

    private String nickname;

    private String avatar;

    private String phone;

    /** 登录邮箱（邮箱验证码登录的唯一标识，唯一索引） */
    private String email;

    private Long schoolId;

    private Integer status;
}
