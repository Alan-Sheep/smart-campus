package com.campus.smart.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("print_order")
public class PrintOrder extends BaseEntity {

    private String orderNo;

    private Long schoolId;

    private Long userId;

    private Long printerId;

    private String fileUrls;

    /**
     * 打印类型（库列为 TINYINT：0:黑白 1:彩色）
     *
     * <p>原先声明为 String，而前端传 'document'，MySQL 严格模式报
     * {@code Incorrect integer value}（HTTP 500）。改为 Integer 与列类型一致。</p>
     */
    private Integer printType;

    private String paperSize;

    private Integer copies;

    private Integer isDoubleSide;

    private String binding;

    private String deliveryAddress;

    private String receiverName;

    private String receiverPhone;

    private String remark;

    private Integer totalPages;

    private BigDecimal totalAmount;

    private Integer status;

    private String cancelReason;

    private LocalDateTime acceptTime;

    private LocalDateTime completeTime;
}
