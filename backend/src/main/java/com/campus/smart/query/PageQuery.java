package com.campus.smart.query;

import lombok.Data;

import java.io.Serializable;

/**
 * 分页查询基础参数
 */
@Data
public class PageQuery implements Serializable {

    /**
     * 页码
     */
    private Integer pageNum = 1;

    /**
     * 每页数量
     */
    private Integer pageSize = 10;
}
