package com.campus.smart.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 打印下单请求体
 *
 * <p>为什么需要这个 DTO：打印页面上报的字段名与实体/数据库列名<b>不一致</b>，
 * 而 Jackson 默认忽略未知字段（{@code FAIL_ON_UNKNOWN_PROPERTIES=false}），
 * 导致 {@code fileUrl / address / phone / price} 被静默丢弃，
 * 最终 INSERT 缺少 {@code file_urls / delivery_address / receiver_phone / total_amount}
 * 等 NOT NULL 列而报 500。</p>
 *
 * <p>这里显式声明前端真实字段名，由控制器完成到实体的映射，兼顾可读性与兼容性。</p>
 */
@Data
public class PrintOrderRequest implements Serializable {

    /** 打印类型：document / photo / poster / other（前端值） */
    private String printType;

    /** 纸张规格：A4 / A3 / B5 */
    private String paperSize;

    /** 份数 */
    private Integer copies;

    /** 是否双面 */
    private Boolean isDouble;

    /** 是否彩色 */
    private Boolean isColor;

    /** 文件名（仅展示用，不入库） */
    private String fileName;

    /** 文件地址 → 映射到 fileUrls */
    private String fileUrl;

    /** 收货地址 → 映射到 deliveryAddress */
    private String address;

    /** 联系电话 → 映射到 receiverPhone */
    private String phone;

    /** 收件人姓名（可选，缺省填「未填写」） */
    private String receiverName;

    /** 备注 */
    private String remark;

    /** 总页数（可选） */
    private Integer totalPages;

    /** 订单金额 → 映射到 totalAmount */
    private BigDecimal price;
}
