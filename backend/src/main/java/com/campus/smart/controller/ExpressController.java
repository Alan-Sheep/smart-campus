package com.campus.smart.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.smart.common.Result;
import com.campus.smart.entity.ExpressOrder;
import com.campus.smart.common.PageQuery;
import com.campus.smart.common.ReqParams;
import com.campus.smart.service.ExpressOrderService;
import com.campus.smart.utils.SchoolContext;
import com.campus.smart.utils.UserContext;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * 快递代取模块控制器
 */
@Tag(name = "快递代取")
@RestController
@RequestMapping("/api/express")
public class ExpressController {

    @Autowired
    private ExpressOrderService expressOrderService;

    /**
     * 创建快递代取订单
     */
    @Operation(summary = "创建快递代取订单")
    @PostMapping("/createOrder")
    public Result<ExpressOrder> createOrder(@RequestBody ExpressOrder order) {
        Long userId = UserContext.getUserId();
        Long schoolId = SchoolContext.getSchoolId();

        if (userId == null) {
            return Result.error("请先登录");
        }
        order.setUserId(userId);
        order.setSchoolId(schoolId);
        // 生成订单号
        order.setOrderNo("EXP" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase());
        // 状态：待接单
        order.setStatus(1);

        // ---- 服务端补全必填字段（total_amount 为 NOT NULL）----
        BigDecimal serviceFee = order.getServiceFee() == null ? BigDecimal.ZERO : order.getServiceFee();
        BigDecimal tip = order.getTip() == null ? BigDecimal.ZERO : order.getTip();
        order.setTotalAmount(serviceFee.add(tip));

        expressOrderService.save(order);
        return Result.success(order);
    }

    /**
     * 快递大厅列表（所有待接单订单，无需登录）
     */
    @Operation(summary = "快递大厅列表")
    @GetMapping("/hallList")
    public Result<Page<ExpressOrder>> hallList(PageQuery pageQuery) {
        Long schoolId = SchoolContext.getSchoolId();
        LambdaQueryWrapper<ExpressOrder> wrapper = new LambdaQueryWrapper<>();
        // 按学校筛选
        if (schoolId != null) {
            wrapper.eq(ExpressOrder::getSchoolId, schoolId);
        }
        // 只显示待接单的订单（状态1且还没人接单）
        wrapper.eq(ExpressOrder::getStatus, 1);
        wrapper.isNull(ExpressOrder::getPickerId);
        wrapper.orderByDesc(ExpressOrder::getCreateTime);

        Page<ExpressOrder> page = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());
        Page<ExpressOrder> result = expressOrderService.page(page, wrapper);
        return Result.success(result);
    }

    /**
     * 我的订单列表（按状态筛选）
     */
    @Operation(summary = "我的订单列表")
    @GetMapping("/orderList")
    public Result<Page<ExpressOrder>> orderList(PageQuery pageQuery,
                                                 @RequestParam(required = false) Integer status,
                                                 @RequestParam(required = false) String type) {
        Long userId = UserContext.getUserId();
        Long schoolId = SchoolContext.getSchoolId();
        LambdaQueryWrapper<ExpressOrder> wrapper = new LambdaQueryWrapper<>();

        // 按学校筛选：与大厅列表、跑腿/代课保持一致。
        // 说明：跑腿/代课的「我的订单」本来就按学校隔离，快递漏了该校验，
        //       导致跨校数据混在一起。这里补上，保证四个模块行为统一。
        if (schoolId != null) {
            wrapper.eq(ExpressOrder::getSchoolId, schoolId);
        }

        // type: publish-我发布的, pick-我接单的, 不传则全部
        if ("publish".equals(type)) {
            wrapper.eq(ExpressOrder::getUserId, userId);
        } else if ("pick".equals(type)) {
            wrapper.eq(ExpressOrder::getPickerId, userId);
        } else {
            wrapper.and(w -> w.eq(ExpressOrder::getUserId, userId)
                    .or().eq(ExpressOrder::getPickerId, userId));
        }

        if (status != null) {
            wrapper.eq(ExpressOrder::getStatus, status);
        }

        wrapper.orderByDesc(ExpressOrder::getCreateTime);

        Page<ExpressOrder> page = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());
        Page<ExpressOrder> result = expressOrderService.page(page, wrapper);
        return Result.success(result);
    }

    /**
     * 订单详情
     */
    @Operation(summary = "订单详情")
    @GetMapping("/orderDetail")
    public Result<ExpressOrder> orderDetail(@RequestParam Long id) {
        ExpressOrder order = expressOrderService.getById(id);
        if (order == null) {
            return Result.error("订单不存在");
        }
        return Result.success(order);
    }

    /**
     * 取消订单
     */
    @Operation(summary = "取消订单")
    @PostMapping("/cancelOrder")
    public Result<Void> cancelOrder(HttpServletRequest request) {
        Long id = ReqParams.requireLong(request, "id");
        Long userId = UserContext.getUserId();
        ExpressOrder order = expressOrderService.getById(id);
        if (order == null) {
            return Result.error("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            return Result.error("只能取消自己发布的订单");
        }
        if (order.getStatus() != 1) {
            return Result.error("当前状态不能取消");
        }

        ExpressOrder update = new ExpressOrder();
        update.setId(id);
        update.setStatus(0); // 已取消
        expressOrderService.updateById(update);
        return Result.success();
    }

    /**
     * 接单
     */
    @Operation(summary = "接单")
    @PostMapping("/pickOrder")
    public Result<Void> pickOrder(HttpServletRequest request) {
        Long id = ReqParams.requireLong(request, "id");
        Long userId = UserContext.getUserId();
        ExpressOrder order = expressOrderService.getById(id);
        if (order == null) {
            return Result.error("订单不存在");
        }
        if (order.getUserId().equals(userId)) {
            return Result.error("不能接自己发布的订单");
        }
        if (order.getStatus() != 1) {
            return Result.error("订单已被接单或已取消");
        }

        ExpressOrder update = new ExpressOrder();
        update.setId(id);
        update.setPickerId(userId);
        update.setStatus(2); // 进行中
        expressOrderService.updateById(update);
        return Result.success();
    }

    /**
     * 完成订单
     */
    @Operation(summary = "完成订单")
    @PostMapping("/completeOrder")
    public Result<Void> completeOrder(HttpServletRequest request) {
        Long id = ReqParams.requireLong(request, "id");
        Long userId = UserContext.getUserId();
        ExpressOrder order = expressOrderService.getById(id);
        if (order == null) {
            return Result.error("订单不存在");
        }
        if (!Objects.equals(order.getPickerId(), userId)) {
            return Result.error("只能完成自己接单的订单");
        }
        if (order.getStatus() != 2) {
            return Result.error("当前状态不能完成");
        }

        ExpressOrder update = new ExpressOrder();
        update.setId(id);
        update.setStatus(3); // 已完成
        expressOrderService.updateById(update);
        return Result.success();
    }
}
