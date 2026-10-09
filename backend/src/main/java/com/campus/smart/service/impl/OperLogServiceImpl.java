package com.campus.smart.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.smart.entity.OperLog;
import com.campus.smart.mapper.OperLogMapper;
import com.campus.smart.service.OperLogService;
import org.springframework.stereotype.Service;

@Service
public class OperLogServiceImpl extends ServiceImpl<OperLogMapper, OperLog> implements OperLogService {
}
