package com.campus.smart.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.smart.entity.Comment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {
}
