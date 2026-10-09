package com.campus.smart.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("like_record")
public class LikeRecord implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long targetId;

    private String targetType;

    private Long userId;
}
