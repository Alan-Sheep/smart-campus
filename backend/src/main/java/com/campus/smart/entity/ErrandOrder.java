package com.campus.smart.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("errand_order")
public class ErrandOrder extends BaseEntity {

    private String orderNo;

    private Long schoolId;

    private Long userId;

    private Long runnerId;

    private String errandType;

    private String title;

    private String description;

    private String images;

    private String pickupAddress;

    private String deliveryAddress;

    private String receiverName;

    private String receiverPhone;

    private String estimatedTime;

    private BigDecimal serviceFee;

    private BigDecimal goodsFee;

    private BigDecimal tip;

    private BigDecimal totalAmount;

    private Integer status;

    private String cancelReason;

    private LocalDateTime acceptTime;

    private LocalDateTime completeTime;
}
