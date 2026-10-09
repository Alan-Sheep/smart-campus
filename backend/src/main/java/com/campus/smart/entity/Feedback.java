package com.campus.smart.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("feedback")
public class Feedback extends BaseEntity {

    private Long schoolId;

    private Long userId;

    /**
     * 反馈类型（库列为 TINYINT：0:功能建议 1:BUG反馈 2:投诉举报 3:其他）
     *
     * <p>原先声明为 String，而前端传 'suggestion' 等字符串，MySQL 严格模式报
     * {@code Incorrect integer value}（HTTP 500）。改为 Integer 与列类型一致。</p>
     */
    private Integer type;

    private String title;

    private String content;

    private String images;

    private String contact;

    private Integer status;

    private String reply;

    private LocalDateTime replyTime;

    private Long replyAdminId;
}
