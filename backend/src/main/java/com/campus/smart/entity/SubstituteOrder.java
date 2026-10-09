package com.campus.smart.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("substitute_order")
public class SubstituteOrder extends BaseEntity {

    private String orderNo;

    private Long schoolId;

    private Long userId;

    private Long substituteId;

    private String courseName;

    private String courseTime;

    private LocalDate courseDate;

    private String classroom;

    private String teacher;

    private String major;

    private String requirement;

    private BigDecimal reward;

    private String contactQq;

    private String contactWechat;

    private Integer status;

    private String cancelReason;

    private LocalDateTime acceptTime;

    private LocalDateTime completeTime;
}
