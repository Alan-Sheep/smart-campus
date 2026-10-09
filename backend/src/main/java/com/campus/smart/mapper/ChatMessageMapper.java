package com.campus.smart.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.smart.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
}
