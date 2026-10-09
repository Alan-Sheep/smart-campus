package com.campus.smart.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.smart.entity.LikeRecord;
import com.campus.smart.mapper.LikeRecordMapper;
import com.campus.smart.service.LikeRecordService;
import org.springframework.stereotype.Service;

@Service
public class LikeRecordServiceImpl extends ServiceImpl<LikeRecordMapper, LikeRecord> implements LikeRecordService {
}
