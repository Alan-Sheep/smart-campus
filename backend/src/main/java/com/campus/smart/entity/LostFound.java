package com.campus.smart.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("lost_found")
public class LostFound extends BaseEntity {

    private Long schoolId;

    private Long userId;

    /**
     * 类型（库列为 TINYINT：0:寻物 1:招领）
     *
     * <p>原先声明为 String，而前端传 'lost'/'found'，导致 MySQL 严格模式报
     * {@code Incorrect integer value: 'lost' for column 'type'}（HTTP 500）。
     * 改为 Integer 后与列类型一致，并可由 Jackson 直接接收数字或数字字符串。</p>
     */
    private Integer type;

    private String itemName;

    private String description;

    private String images;

    private String location;

    private Integer status;

    private Integer auditStatus;

    private String rejectReason;
}
