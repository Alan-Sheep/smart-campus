package com.campus.smart.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("game_buddy")
public class GameBuddy extends BaseEntity {

    private Long schoolId;

    private Long userId;

    private String gameName;

    private String title;

    private String description;

    private String gameLevel;

    private String playMode;

    private String contactWay;

    private String playTime;

    private String gender;

    private Integer status;

    private Integer viewCount;
}
