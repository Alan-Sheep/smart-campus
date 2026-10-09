package com.campus.smart.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("express_order")
public class ExpressOrder extends BaseEntity {

    private String orderNo;

    private Long schoolId;

    private Long userId;

    private Long pickerId;

    private String expressCompany;

    private String pickupCode;

    private String pickupAddress;

    private String deliveryAddress;

    private BigDecimal weight;

    private String receiverName;

    private String receiverPhone;

    private String remark;

    private BigDecimal serviceFee;

    private BigDecimal tip;

    private BigDecimal totalAmount;

    private Integer status;

    private String cancelReason;

    private LocalDateTime acceptTime;

    private LocalDateTime completeTime;
}
