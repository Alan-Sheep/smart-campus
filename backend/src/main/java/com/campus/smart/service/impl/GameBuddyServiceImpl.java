package com.campus.smart.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.smart.entity.GameBuddy;
import com.campus.smart.mapper.GameBuddyMapper;
import com.campus.smart.service.GameBuddyService;
import org.springframework.stereotype.Service;

@Service
public class GameBuddyServiceImpl extends ServiceImpl<GameBuddyMapper, GameBuddy> implements GameBuddyService {
}
