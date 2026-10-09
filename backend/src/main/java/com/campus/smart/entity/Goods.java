package com.campus.smart.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("goods")
public class Goods extends BaseEntity {

    private Long schoolId;

    private Long userId;

    private String title;

    private String description;

    private BigDecimal price;

    private String images;

    private Long categoryId;

    private Integer status;

    private Integer auditStatus;

    private String rejectReason;

    private Integer viewCount;
}
