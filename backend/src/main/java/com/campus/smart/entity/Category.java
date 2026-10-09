package com.campus.smart.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("category")
public class Category extends BaseEntity {

    /**
     * 所属学校（可为空表示全局分类）
     *
     * <p>该列在库中默认值为 0，但存在外键 {@code fk_category_school → school(id)}，
     * 并不存在 id=0 的学校。MyBatis-Plus 默认 NOT_NULL 策略会把 null 字段从 INSERT 省略，
     * 于是数据库套用默认值 0 → 外键冲突报 500。
     * 声明 IGNORED 后 null 会写成真正的 NULL，绕开该默认值。</p>
     */
    @TableField(insertStrategy = FieldStrategy.IGNORED, updateStrategy = FieldStrategy.IGNORED)
    private Long schoolId;

    private String name;

    private Integer sort;
}
