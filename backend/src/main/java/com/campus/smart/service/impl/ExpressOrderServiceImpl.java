package com.campus.smart.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.smart.entity.ExpressOrder;
import com.campus.smart.mapper.ExpressOrderMapper;
import com.campus.smart.service.ExpressOrderService;
import org.springframework.stereotype.Service;

@Service
public class ExpressOrderServiceImpl extends ServiceImpl<ExpressOrderMapper, ExpressOrder> implements ExpressOrderService {
}
