package com.campus.smart.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.smart.entity.LostFound;
import com.campus.smart.mapper.LostFoundMapper;
import com.campus.smart.service.LostFoundService;
import org.springframework.stereotype.Service;

@Service
public class LostFoundServiceImpl extends ServiceImpl<LostFoundMapper, LostFound> implements LostFoundService {
}
