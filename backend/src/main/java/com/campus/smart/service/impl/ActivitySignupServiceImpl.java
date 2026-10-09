package com.campus.smart.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.smart.entity.ActivitySignup;
import com.campus.smart.mapper.ActivitySignupMapper;
import com.campus.smart.service.ActivitySignupService;
import org.springframework.stereotype.Service;

@Service
public class ActivitySignupServiceImpl extends ServiceImpl<ActivitySignupMapper, ActivitySignup> implements ActivitySignupService {
}
