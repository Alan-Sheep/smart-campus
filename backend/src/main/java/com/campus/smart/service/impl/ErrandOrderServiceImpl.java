package com.campus.smart.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.smart.entity.ErrandOrder;
import com.campus.smart.mapper.ErrandOrderMapper;
import com.campus.smart.service.ErrandOrderService;
import org.springframework.stereotype.Service;

@Service
public class ErrandOrderServiceImpl extends ServiceImpl<ErrandOrderMapper, ErrandOrder> implements ErrandOrderService {
}
