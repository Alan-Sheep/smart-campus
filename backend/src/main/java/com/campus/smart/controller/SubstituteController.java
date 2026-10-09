package com.campus.smart.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.smart.common.Result;
import com.campus.smart.entity.SubstituteOrder;
import com.campus.smart.common.PageQuery;
import com.campus.smart.common.ReqParams;
import com.campus.smart.service.SubstituteOrderService;
import com.campus.smart.utils.SchoolContext;
import com.campus.smart.utils.UserContext;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Objects;
import java.util.UUID;

/**
 * 代课服务模块控制器
 */
@Tag(name = "代课服务")
@RestController
@RequestMapping("/api/substitute")
public class SubstituteController {

    @Autowired
    private SubstituteOrderService substituteOrderService;

    /**
     * 发布代课需求
     */
    @Operation(summary = "发布代课需求")
    @PostMapping("/createOrder")
    public Result<SubstituteOrder> createOrder(@RequestBody SubstituteOrder order) {
        Long userId = UserContext.getUserId();
        Long schoolId = SchoolContext.getSchoolId();

        order.setUserId(userId);
        order.setSchoolId(schoolId);
        // 生成订单号
        order.setOrderNo("SUB" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase());
        // 状态：待接单
        order.setStatus(1);

        substituteOrderService.save(order);
        return Result.success(order);
    }

    /**
     * 订单列表
     */
    @Operation(summary = "订单列表")
    @GetMapping("/orderList")
    public Result<Page<SubstituteOrder>> orderList(PageQuery pageQuery,
                                                    @RequestParam(required = false) Integer status,
                                                    @RequestParam(required = false) String type) {
        Long userId = UserContext.getUserId();
        Long schoolId = SchoolContext.getSchoolId();
        LambdaQueryWrapper<SubstituteOrder> wrapper = new LambdaQueryWrapper<>();

        // 按学校筛选
        if (schoolId != null) {
            wrapper.eq(SubstituteOrder::getSchoolId, schoolId);
        }

        // type: all-全部大厅, publish-我发布的, accept-我接单的
        if ("publish".equals(type)) {
            wrapper.eq(SubstituteOrder::getUserId, userId);
        } else if ("accept".equals(type)) {
            wrapper.eq(SubstituteOrder::getSubstituteId, userId);
        } else {
            // 大厅：显示待接单的（status=1 且还没人接单）
            wrapper.eq(SubstituteOrder::getStatus, 1);
            wrapper.isNull(SubstituteOrder::getSubstituteId);
        }

        if (status != null) {
            wrapper.eq(SubstituteOrder::getStatus, status);
        }

        wrapper.orderByDesc(SubstituteOrder::getCreateTime);

        Page<SubstituteOrder> page = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());
        Page<SubstituteOrder> result = substituteOrderService.page(page, wrapper);
        return Result.success(result);
    }

    /**
     * 订单详情
     */
    @Operation(summary = "订单详情")
    @GetMapping("/orderDetail")
    public Result<SubstituteOrder> orderDetail(@RequestParam Long id) {
        SubstituteOrder order = substituteOrderService.getById(id);
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
        SubstituteOrder order = substituteOrderService.getById(id);
        if (order == null) {
            return Result.error("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            return Result.error("只能取消自己发布的订单");
        }
        if (order.getStatus() != 1) {
            return Result.error("当前状态不能取消");
        }

        SubstituteOrder update = new SubstituteOrder();
        update.setId(id);
        update.setStatus(0); // 已取消
        substituteOrderService.updateById(update);
        return Result.success();
    }

    /**
     * 接单
     */
    /**
     * 完成订单
     *
     * <p>前端 {@code pages/substitute/detail.vue} 会调用
     * {@code /api/substitute/completeOrder}，但后端原先缺少该接口（返回 404），
     * 导致接单后无法把订单置为「已完成」。此处补齐。</p>
     */
    @Operation(summary = "完成订单")
    @PostMapping("/completeOrder")
    public Result<Void> completeOrder(HttpServletRequest request) {
        Long id = ReqParams.requireLong(request, "id");
        Long userId = UserContext.getUserId();
        SubstituteOrder order = substituteOrderService.getById(id);
        if (order == null) {
            return Result.error("订单不存在");
        }
        // 只有接单人可以标记完成
        if (!Objects.equals(order.getSubstituteId(), userId)) {
            return Result.error("只能完成自己接单的订单");
        }
        if (order.getStatus() == null || order.getStatus() != 2) {
            return Result.error("当前状态不能完成");
        }

        SubstituteOrder update = new SubstituteOrder();
        update.setId(id);
        update.setStatus(3); // 已完成
        update.setCompleteTime(java.time.LocalDateTime.now());
        substituteOrderService.updateById(update);
        return Result.success();
    }

    @Operation(summary = "接单")
    @PostMapping("/acceptOrder")
    public Result<Void> acceptOrder(HttpServletRequest request) {
        Long id = ReqParams.requireLong(request, "id");
        Long userId = UserContext.getUserId();
        SubstituteOrder order = substituteOrderService.getById(id);
        if (order == null) {
            return Result.error("订单不存在");
        }
        if (Objects.equals(order.getUserId(), userId)) {
            return Result.error("不能接自己发布的订单");
        }
        if (order.getStatus() != 1) {
            return Result.error("订单已被接单或已取消");
        }

        SubstituteOrder update = new SubstituteOrder();
        update.setId(id);
        update.setSubstituteId(userId);
        update.setStatus(2); // 进行中
        substituteOrderService.updateById(update);
        return Result.success();
    }
}
