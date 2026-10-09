package com.campus.smart.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.smart.common.Result;
import com.campus.smart.entity.ErrandOrder;
import com.campus.smart.entity.ExpressOrder;
import com.campus.smart.entity.Message;
import com.campus.smart.entity.PrintOrder;
import com.campus.smart.entity.SubstituteOrder;
import com.campus.smart.entity.User;
import com.campus.smart.service.ErrandOrderService;
import com.campus.smart.service.ExpressOrderService;
import com.campus.smart.service.MessageService;
import com.campus.smart.service.PrintOrderService;
import com.campus.smart.service.SubstituteOrderService;
import com.campus.smart.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理端-数据统计控制器
 */
@Tag(name = "【管理端】数据统计")
@RestController
@RequestMapping("/api/admin/stats")
public class AdminStatsController {

    @Autowired
    private UserService userService;

    @Autowired
    private ExpressOrderService expressOrderService;

    @Autowired
    private PrintOrderService printOrderService;

    @Autowired
    private ErrandOrderService errandOrderService;

    @Autowired
    private SubstituteOrderService substituteOrderService;

    @Autowired
    private MessageService messageService;

    /**
     * 首页数据概览
     * 用户总数、今日新增用户、订单总数、今日订单数、交易额等
     */
    @Operation(summary = "首页数据概览")
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview(@RequestParam(required = false) Long schoolId) {
        Map<String, Object> result = new HashMap<>();

        // 当日开始和结束时间
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime todayEnd = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);

        // ========== 用户统计 ==========
        LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
        if (schoolId != null) {
            userWrapper.eq(User::getSchoolId, schoolId);
        }
        long userTotal = userService.count(userWrapper);
        result.put("userTotal", userTotal);

        // 今日新增用户
        LambdaQueryWrapper<User> todayUserWrapper = new LambdaQueryWrapper<>();
        if (schoolId != null) {
            todayUserWrapper.eq(User::getSchoolId, schoolId);
        }
        todayUserWrapper.ge(User::getCreateTime, todayStart);
        todayUserWrapper.le(User::getCreateTime, todayEnd);
        long todayUserCount = userService.count(todayUserWrapper);
        result.put("todayUserCount", todayUserCount);

        // ========== 订单统计 ==========
        // 计算各类型订单总数和今日订单数
        long expressTotal = countOrders(expressOrderService, schoolId, null);
        long printTotal = countOrders(printOrderService, schoolId, null);
        long errandTotal = countOrders(errandOrderService, schoolId, null);
        long substituteTotal = countOrders(substituteOrderService, schoolId, null);
        long orderTotal = expressTotal + printTotal + errandTotal + substituteTotal;
        result.put("orderTotal", orderTotal);

        // 今日订单数
        long todayExpress = countOrders(expressOrderService, schoolId, todayStart);
        long todayPrint = countOrders(printOrderService, schoolId, todayStart);
        long todayErrand = countOrders(errandOrderService, schoolId, todayStart);
        long todaySubstitute = countOrders(substituteOrderService, schoolId, todayStart);
        long todayOrderCount = todayExpress + todayPrint + todayErrand + todaySubstitute;
        result.put("todayOrderCount", todayOrderCount);

        // ========== 交易额统计 ==========
        // 已完成订单的总金额
        BigDecimal expressAmount = getTotalAmount(expressOrderService, schoolId, "express");
        BigDecimal printAmount = getTotalAmount(printOrderService, schoolId, "print");
        BigDecimal errandAmount = getTotalAmount(errandOrderService, schoolId, "errand");
        BigDecimal substituteAmount = getTotalAmount(substituteOrderService, schoolId, "substitute");
        BigDecimal totalAmount = expressAmount.add(printAmount).add(errandAmount).add(substituteAmount);
        result.put("totalAmount", totalAmount);

        // 今日交易额
        BigDecimal todayExpressAmount = getTodayAmount(expressOrderService, schoolId, "express");
        BigDecimal todayPrintAmount = getTodayAmount(printOrderService, schoolId, "print");
        BigDecimal todayErrandAmount = getTodayAmount(errandOrderService, schoolId, "errand");
        BigDecimal todaySubstituteAmount = getTodayAmount(substituteOrderService, schoolId, "substitute");
        BigDecimal todayAmount = todayExpressAmount.add(todayPrintAmount).add(todayErrandAmount).add(todaySubstituteAmount);
        result.put("todayAmount", todayAmount);

        // ========== 消息统计 ==========
        LambdaQueryWrapper<Message> msgWrapper = new LambdaQueryWrapper<>();
        if (schoolId != null) {
            // 消息表没有school_id，通过用户关联学校（这里简化处理，消息统计暂不按学校过滤）
        }
        long msgTotal = messageService.count(msgWrapper);
        result.put("messageTotal", msgTotal);

        // 未读消息总数
        LambdaQueryWrapper<Message> unreadMsgWrapper = new LambdaQueryWrapper<>();
        unreadMsgWrapper.eq(Message::getIsRead, 0);
        long unreadMsgCount = messageService.count(unreadMsgWrapper);
        result.put("unreadMessageCount", unreadMsgCount);

        // 今日消息数
        LambdaQueryWrapper<Message> todayMsgWrapper = new LambdaQueryWrapper<>();
        todayMsgWrapper.ge(Message::getCreateTime, todayStart);
        todayMsgWrapper.le(Message::getCreateTime, todayEnd);
        long todayMsgCount = messageService.count(todayMsgWrapper);
        result.put("todayMessageCount", todayMsgCount);

        return Result.success(result);
    }

    /**
     * 订单统计（按日/周/月）
     * type: day-按日, week-按周, month-按月
     */
    @Operation(summary = "订单统计")
    @GetMapping("/orderStats")
    public Result<Map<String, Object>> orderStats(
            @RequestParam(defaultValue = "day") String type,
            @RequestParam(required = false) Long schoolId) {

        Map<String, Object> result = new HashMap<>();
        List<String> dates = new ArrayList<>();
        List<Long> expressCounts = new ArrayList<>();
        List<Long> printCounts = new ArrayList<>();
        List<Long> errandCounts = new ArrayList<>();
        List<Long> substituteCounts = new ArrayList<>();

        int days = 7; // 默认按周7天
        if ("month".equals(type)) {
            days = 30;
        } else if ("day".equals(type)) {
            days = 7;
        }

        LocalDate today = LocalDate.now();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            dates.add(date.toString());

            LocalDateTime dayStart = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime dayEnd = LocalDateTime.of(date, LocalTime.MAX);

            expressCounts.add(countOrdersByDate(expressOrderService, schoolId, dayStart, dayEnd));
            printCounts.add(countOrdersByDate(printOrderService, schoolId, dayStart, dayEnd));
            errandCounts.add(countOrdersByDate(errandOrderService, schoolId, dayStart, dayEnd));
            substituteCounts.add(countOrdersByDate(substituteOrderService, schoolId, dayStart, dayEnd));
        }

        result.put("dates", dates);
        result.put("expressCounts", expressCounts);
        result.put("printCounts", printCounts);
        result.put("errandCounts", errandCounts);
        result.put("substituteCounts", substituteCounts);

        return Result.success(result);
    }

    /**
     * 用户增长统计
     * type: day-按日, week-按周, month-按月
     */
    @Operation(summary = "用户增长统计")
    @GetMapping("/userGrowth")
    public Result<Map<String, Object>> userGrowth(
            @RequestParam(defaultValue = "day") String type,
            @RequestParam(required = false) Long schoolId) {

        Map<String, Object> result = new HashMap<>();
        List<String> dates = new ArrayList<>();
        List<Long> userCounts = new ArrayList<>();

        int days = 7;
        if ("month".equals(type)) {
            days = 30;
        }

        LocalDate today = LocalDate.now();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            dates.add(date.toString());

            LocalDateTime dayStart = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime dayEnd = LocalDateTime.of(date, LocalTime.MAX);

            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            if (schoolId != null) {
                wrapper.eq(User::getSchoolId, schoolId);
            }
            wrapper.ge(User::getCreateTime, dayStart);
            wrapper.le(User::getCreateTime, dayEnd);
            userCounts.add(userService.count(wrapper));
        }

        result.put("dates", dates);
        result.put("userCounts", userCounts);

        return Result.success(result);
    }

    /**
     * 各模块订单占比
     */
    @Operation(summary = "各模块订单占比")
    @GetMapping("/moduleRatio")
    public Result<List<Map<String, Object>>> moduleRatio(@RequestParam(required = false) Long schoolId) {
        List<Map<String, Object>> result = new ArrayList<>();

        long expressTotal = countOrders(expressOrderService, schoolId, null);
        long printTotal = countOrders(printOrderService, schoolId, null);
        long errandTotal = countOrders(errandOrderService, schoolId, null);
        long substituteTotal = countOrders(substituteOrderService, schoolId, null);
        long total = expressTotal + printTotal + errandTotal + substituteTotal;

        result.add(buildModuleRatio("快递代取", expressTotal, total));
        result.add(buildModuleRatio("打印服务", printTotal, total));
        result.add(buildModuleRatio("校园跑腿", errandTotal, total));
        result.add(buildModuleRatio("代课服务", substituteTotal, total));

        return Result.success(result);
    }

    /**
     * 消息类型分布统计
     * 统计系统通知、订单消息、互动消息的数量占比
     */
    @Operation(summary = "消息类型分布")
    @GetMapping("/messageTypeRatio")
    public Result<List<Map<String, Object>>> messageTypeRatio() {
        List<Map<String, Object>> result = new ArrayList<>();

        // 系统通知
        LambdaQueryWrapper<Message> systemWrapper = new LambdaQueryWrapper<>();
        systemWrapper.eq(Message::getType, 0);
        long systemCount = messageService.count(systemWrapper);

        // 订单消息
        LambdaQueryWrapper<Message> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.eq(Message::getType, 1);
        long orderCount = messageService.count(orderWrapper);

        // 互动消息
        LambdaQueryWrapper<Message> interactWrapper = new LambdaQueryWrapper<>();
        interactWrapper.eq(Message::getType, 2);
        long interactCount = messageService.count(interactWrapper);

        long total = systemCount + orderCount + interactCount;

        result.add(buildModuleRatio("系统通知", systemCount, total));
        result.add(buildModuleRatio("订单消息", orderCount, total));
        result.add(buildModuleRatio("互动消息", interactCount, total));

        return Result.success(result);
    }

    /**
     * 消息发送趋势（按日）
     */
    @Operation(summary = "消息发送趋势")
    @GetMapping("/messageTrend")
    public Result<Map<String, Object>> messageTrend(
            @RequestParam(defaultValue = "7") Integer days) {

        Map<String, Object> result = new HashMap<>();
        List<String> dates = new ArrayList<>();
        List<Long> counts = new ArrayList<>();

        if (days == null || days < 1) days = 7;
        if (days > 30) days = 30;

        LocalDate today = LocalDate.now();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            dates.add(date.toString());

            LocalDateTime dayStart = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime dayEnd = LocalDateTime.of(date, LocalTime.MAX);

            LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
            wrapper.ge(Message::getCreateTime, dayStart);
            wrapper.le(Message::getCreateTime, dayEnd);
            counts.add(messageService.count(wrapper));
        }

        result.put("dates", dates);
        result.put("counts", counts);

        return Result.success(result);
    }

    // ==================== 辅助方法 ====================

    /**
     * 统计订单总数
     */
    private long countOrders(Object service, Long schoolId, LocalDateTime startTime) {
        // 由于各订单Service继承IService，这里通过不同类型处理
        if (service instanceof com.campus.smart.service.ExpressOrderService) {
            LambdaQueryWrapper<ExpressOrder> wrapper = new LambdaQueryWrapper<>();
            if (schoolId != null) wrapper.eq(ExpressOrder::getSchoolId, schoolId);
            if (startTime != null) wrapper.ge(ExpressOrder::getCreateTime, startTime);
            return ((com.campus.smart.service.ExpressOrderService) service).count(wrapper);
        } else if (service instanceof com.campus.smart.service.PrintOrderService) {
            LambdaQueryWrapper<PrintOrder> wrapper = new LambdaQueryWrapper<>();
            if (schoolId != null) wrapper.eq(PrintOrder::getSchoolId, schoolId);
            if (startTime != null) wrapper.ge(PrintOrder::getCreateTime, startTime);
            return ((com.campus.smart.service.PrintOrderService) service).count(wrapper);
        } else if (service instanceof com.campus.smart.service.ErrandOrderService) {
            LambdaQueryWrapper<ErrandOrder> wrapper = new LambdaQueryWrapper<>();
            if (schoolId != null) wrapper.eq(ErrandOrder::getSchoolId, schoolId);
            if (startTime != null) wrapper.ge(ErrandOrder::getCreateTime, startTime);
            return ((com.campus.smart.service.ErrandOrderService) service).count(wrapper);
        } else if (service instanceof com.campus.smart.service.SubstituteOrderService) {
            LambdaQueryWrapper<SubstituteOrder> wrapper = new LambdaQueryWrapper<>();
            if (schoolId != null) wrapper.eq(SubstituteOrder::getSchoolId, schoolId);
            if (startTime != null) wrapper.ge(SubstituteOrder::getCreateTime, startTime);
            return ((com.campus.smart.service.SubstituteOrderService) service).count(wrapper);
        }
        return 0;
    }

    /**
     * 按日期统计订单数
     */
    private long countOrdersByDate(Object service, Long schoolId, LocalDateTime start, LocalDateTime end) {
        if (service instanceof com.campus.smart.service.ExpressOrderService) {
            LambdaQueryWrapper<ExpressOrder> wrapper = new LambdaQueryWrapper<>();
            if (schoolId != null) wrapper.eq(ExpressOrder::getSchoolId, schoolId);
            wrapper.ge(ExpressOrder::getCreateTime, start);
            wrapper.le(ExpressOrder::getCreateTime, end);
            return ((com.campus.smart.service.ExpressOrderService) service).count(wrapper);
        } else if (service instanceof com.campus.smart.service.PrintOrderService) {
            LambdaQueryWrapper<PrintOrder> wrapper = new LambdaQueryWrapper<>();
            if (schoolId != null) wrapper.eq(PrintOrder::getSchoolId, schoolId);
            wrapper.ge(PrintOrder::getCreateTime, start);
            wrapper.le(PrintOrder::getCreateTime, end);
            return ((com.campus.smart.service.PrintOrderService) service).count(wrapper);
        } else if (service instanceof com.campus.smart.service.ErrandOrderService) {
            LambdaQueryWrapper<ErrandOrder> wrapper = new LambdaQueryWrapper<>();
            if (schoolId != null) wrapper.eq(ErrandOrder::getSchoolId, schoolId);
            wrapper.ge(ErrandOrder::getCreateTime, start);
            wrapper.le(ErrandOrder::getCreateTime, end);
            return ((com.campus.smart.service.ErrandOrderService) service).count(wrapper);
        } else if (service instanceof com.campus.smart.service.SubstituteOrderService) {
            LambdaQueryWrapper<SubstituteOrder> wrapper = new LambdaQueryWrapper<>();
            if (schoolId != null) wrapper.eq(SubstituteOrder::getSchoolId, schoolId);
            wrapper.ge(SubstituteOrder::getCreateTime, start);
            wrapper.le(SubstituteOrder::getCreateTime, end);
            return ((com.campus.smart.service.SubstituteOrderService) service).count(wrapper);
        }
        return 0;
    }

    /**
     * 获取已完成订单总金额
     */
    private BigDecimal getTotalAmount(Object service, Long schoolId, String type) {
        BigDecimal total = BigDecimal.ZERO;
        if ("express".equals(type)) {
            LambdaQueryWrapper<ExpressOrder> wrapper = new LambdaQueryWrapper<>();
            if (schoolId != null) wrapper.eq(ExpressOrder::getSchoolId, schoolId);
            wrapper.eq(ExpressOrder::getStatus, 3); // 已完成
            List<ExpressOrder> list = ((com.campus.smart.service.ExpressOrderService) service).list(wrapper);
            for (ExpressOrder order : list) {
                if (order.getTotalAmount() != null) {
                    total = total.add(order.getTotalAmount());
                }
            }
        } else if ("print".equals(type)) {
            LambdaQueryWrapper<PrintOrder> wrapper = new LambdaQueryWrapper<>();
            if (schoolId != null) wrapper.eq(PrintOrder::getSchoolId, schoolId);
            wrapper.eq(PrintOrder::getStatus, 3);
            List<PrintOrder> list = ((com.campus.smart.service.PrintOrderService) service).list(wrapper);
            for (PrintOrder order : list) {
                if (order.getTotalAmount() != null) {
                    total = total.add(order.getTotalAmount());
                }
            }
        } else if ("errand".equals(type)) {
            LambdaQueryWrapper<ErrandOrder> wrapper = new LambdaQueryWrapper<>();
            if (schoolId != null) wrapper.eq(ErrandOrder::getSchoolId, schoolId);
            wrapper.eq(ErrandOrder::getStatus, 3);
            List<ErrandOrder> list = ((com.campus.smart.service.ErrandOrderService) service).list(wrapper);
            for (ErrandOrder order : list) {
                if (order.getTotalAmount() != null) {
                    total = total.add(order.getTotalAmount());
                }
            }
        } else if ("substitute".equals(type)) {
            LambdaQueryWrapper<SubstituteOrder> wrapper = new LambdaQueryWrapper<>();
            if (schoolId != null) wrapper.eq(SubstituteOrder::getSchoolId, schoolId);
            wrapper.eq(SubstituteOrder::getStatus, 3);
            List<SubstituteOrder> list = ((com.campus.smart.service.SubstituteOrderService) service).list(wrapper);
            for (SubstituteOrder order : list) {
                if (order.getReward() != null) {
                    total = total.add(order.getReward());
                }
            }
        }
        return total;
    }

    /**
     * 获取今日已完成订单金额
     */
    private BigDecimal getTodayAmount(Object service, Long schoolId, String type) {
        BigDecimal total = BigDecimal.ZERO;
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);

        if ("express".equals(type)) {
            LambdaQueryWrapper<ExpressOrder> wrapper = new LambdaQueryWrapper<>();
            if (schoolId != null) wrapper.eq(ExpressOrder::getSchoolId, schoolId);
            wrapper.eq(ExpressOrder::getStatus, 3);
            wrapper.ge(ExpressOrder::getCompleteTime, todayStart);
            List<ExpressOrder> list = ((com.campus.smart.service.ExpressOrderService) service).list(wrapper);
            for (ExpressOrder order : list) {
                if (order.getTotalAmount() != null) {
                    total = total.add(order.getTotalAmount());
                }
            }
        } else if ("print".equals(type)) {
            LambdaQueryWrapper<PrintOrder> wrapper = new LambdaQueryWrapper<>();
            if (schoolId != null) wrapper.eq(PrintOrder::getSchoolId, schoolId);
            wrapper.eq(PrintOrder::getStatus, 3);
            wrapper.ge(PrintOrder::getCompleteTime, todayStart);
            List<PrintOrder> list = ((com.campus.smart.service.PrintOrderService) service).list(wrapper);
            for (PrintOrder order : list) {
                if (order.getTotalAmount() != null) {
                    total = total.add(order.getTotalAmount());
                }
            }
        } else if ("errand".equals(type)) {
            LambdaQueryWrapper<ErrandOrder> wrapper = new LambdaQueryWrapper<>();
            if (schoolId != null) wrapper.eq(ErrandOrder::getSchoolId, schoolId);
            wrapper.eq(ErrandOrder::getStatus, 3);
            wrapper.ge(ErrandOrder::getCompleteTime, todayStart);
            List<ErrandOrder> list = ((com.campus.smart.service.ErrandOrderService) service).list(wrapper);
            for (ErrandOrder order : list) {
                if (order.getTotalAmount() != null) {
                    total = total.add(order.getTotalAmount());
                }
            }
        } else if ("substitute".equals(type)) {
            LambdaQueryWrapper<SubstituteOrder> wrapper = new LambdaQueryWrapper<>();
            if (schoolId != null) wrapper.eq(SubstituteOrder::getSchoolId, schoolId);
            wrapper.eq(SubstituteOrder::getStatus, 3);
            wrapper.ge(SubstituteOrder::getCompleteTime, todayStart);
            List<SubstituteOrder> list = ((com.campus.smart.service.SubstituteOrderService) service).list(wrapper);
            for (SubstituteOrder order : list) {
                if (order.getReward() != null) {
                    total = total.add(order.getReward());
                }
            }
        }
        return total;
    }

    /**
     * 构建模块占比数据
     */
    private Map<String, Object> buildModuleRatio(String name, long count, long total) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("value", count);
        // 计算百分比
        double ratio = total > 0 ? (double) count / total * 100 : 0;
        map.put("ratio", Math.round(ratio * 100.0) / 100.0);
        return map;
    }
}
