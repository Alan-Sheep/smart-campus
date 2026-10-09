package com.campus.smart.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("activity")
public class Activity extends BaseEntity {

    private Long schoolId;

    private Long userId;

    private String title;

    private String description;

    private String images;

    private Integer activityType;

    private LocalDate activityDate;

    private LocalTime startTime;

    private LocalTime endTime;

    private String location;

    private Integer maxParticipants;

    private Integer currentParticipants;

    private BigDecimal fee;

    private String contactWay;

    private Integer auditStatus;

    private String rejectReason;

    private Integer status;

    private Integer viewCount;

    private Integer isTop;
}
