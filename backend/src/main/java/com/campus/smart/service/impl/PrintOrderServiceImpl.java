package com.campus.smart.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.smart.entity.PrintOrder;
import com.campus.smart.mapper.PrintOrderMapper;
import com.campus.smart.service.PrintOrderService;
import org.springframework.stereotype.Service;

@Service
public class PrintOrderServiceImpl extends ServiceImpl<PrintOrderMapper, PrintOrder> implements PrintOrderService {
}
