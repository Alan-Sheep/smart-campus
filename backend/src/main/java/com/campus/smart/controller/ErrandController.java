package com.campus.smart.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.smart.common.Result;
import com.campus.smart.entity.ErrandOrder;
import com.campus.smart.common.PageQuery;
import com.campus.smart.common.ReqParams;
import com.campus.smart.service.ErrandOrderService;
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
 * 校园跑腿模块控制器
 */
@Tag(name = "校园跑腿")
@RestController
@RequestMapping("/api/errand")
public class ErrandController {

    @Autowired
    private ErrandOrderService errandOrderService;

    /**
     * 创建跑腿订单
     */
    @Operation(summary = "创建跑腿订单")
    @PostMapping("/createOrder")
    public Result<ErrandOrder> createOrder(@RequestBody ErrandOrder order) {
        Long userId = UserContext.getUserId();
        Long schoolId = SchoolContext.getSchoolId();

        if (userId == null) {
            return Result.error("请先登录");
        }
        order.setUserId(userId);
        order.setSchoolId(schoolId);
        // 生成订单号
        order.setOrderNo("ERR" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase());
        // 状态：待接单
        order.setStatus(1);

        // ---- 服务端补全必填字段，避免依赖前端传值（receiver_name / total_amount 为 NOT NULL）----
        if (order.getReceiverName() == null || order.getReceiverName().trim().isEmpty()) {
            order.setReceiverName("未填写");
        }
        // 跑腿费 + 商品费 + 小费 汇总为总金额
        BigDecimal serviceFee = order.getServiceFee() == null ? BigDecimal.ZERO : order.getServiceFee();
        BigDecimal goodsFee = order.getGoodsFee() == null ? BigDecimal.ZERO : order.getGoodsFee();
        BigDecimal tip = order.getTip() == null ? BigDecimal.ZERO : order.getTip();
        order.setTotalAmount(serviceFee.add(goodsFee).add(tip));

        errandOrderService.save(order);
        return Result.success(order);
    }

    /**
     * 订单列表（支持全部/我发布的/我接单的）
     */
    @Operation(summary = "订单列表")
    @GetMapping("/orderList")
    public Result<Page<ErrandOrder>> orderList(PageQuery pageQuery,
                                                @RequestParam(required = false) Integer status,
                                                @RequestParam(required = false) String type,
                                                @RequestParam(required = false) Integer errandType) {
        Long userId = UserContext.getUserId();
        Long schoolId = SchoolContext.getSchoolId();
        LambdaQueryWrapper<ErrandOrder> wrapper = new LambdaQueryWrapper<>();

        // 按学校筛选
        if (schoolId != null) {
            wrapper.eq(ErrandOrder::getSchoolId, schoolId);
        }

        // 按跑腿类型筛选（代买/代送/代取/其他）
        if (errandType != null) {
            wrapper.eq(ErrandOrder::getErrandType, errandType);
        }

        // type: all-全部大厅, publish-我发布的, accept-我接单的
        if ("publish".equals(type)) {
            wrapper.eq(ErrandOrder::getUserId, userId);
        } else if ("accept".equals(type)) {
            wrapper.eq(ErrandOrder::getRunnerId, userId);
        } else {
            // 大厅：显示待接单的（status=1 且还没人接单）
            wrapper.eq(ErrandOrder::getStatus, 1);
            wrapper.isNull(ErrandOrder::getRunnerId);
        }

        if (status != null) {
            wrapper.eq(ErrandOrder::getStatus, status);
        }

        wrapper.orderByDesc(ErrandOrder::getCreateTime);

        Page<ErrandOrder> page = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());
        Page<ErrandOrder> result = errandOrderService.page(page, wrapper);
        return Result.success(result);
    }

    /**
     * 订单详情
     */
    @Operation(summary = "订单详情")
    @GetMapping("/orderDetail")
    public Result<ErrandOrder> orderDetail(@RequestParam Long id) {
        ErrandOrder order = errandOrderService.getById(id);
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
        ErrandOrder order = errandOrderService.getById(id);
        if (order == null) {
            return Result.error("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            return Result.error("只能取消自己发布的订单");
        }
        if (order.getStatus() != 1) {
            return Result.error("当前状态不能取消");
        }

        ErrandOrder update = new ErrandOrder();
        update.setId(id);
        // 注意：本模块（含前端 adaptErrand）统一采用 0=已取消 1=待接单 2=进行中 3=已完成，
        // 与建表注释 0:待接单...4:已取消 不一致，改动会破坏前端状态映射，故保持一致。
        update.setStatus(0); // 已取消
        errandOrderService.updateById(update);
        return Result.success();
    }

    /**
     * 接单
     */
    @Operation(summary = "接单")
    @PostMapping("/acceptOrder")
    public Result<Void> acceptOrder(HttpServletRequest request) {
        Long id = ReqParams.requireLong(request, "id");
        Long userId = UserContext.getUserId();
        ErrandOrder order = errandOrderService.getById(id);
        if (order == null) {
            return Result.error("订单不存在");
        }
        if (order.getUserId() != null && order.getUserId().equals(userId)) {
            return Result.error("不能接自己发布的订单");
        }
        if (order.getStatus() != 1) {
            return Result.error("订单已被接单或已取消");
        }

        ErrandOrder update = new ErrandOrder();
        update.setId(id);
        update.setRunnerId(userId);
        update.setStatus(2); // 进行中
        errandOrderService.updateById(update);
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
        ErrandOrder order = errandOrderService.getById(id);
        if (order == null) {
            return Result.error("订单不存在");
        }
        if (!Objects.equals(order.getRunnerId(), userId)) {
            return Result.error("只能完成自己接单的订单");
        }
        if (order.getStatus() != 2) {
            return Result.error("当前状态不能完成");
        }

        ErrandOrder update = new ErrandOrder();
        update.setId(id);
        update.setStatus(3); // 已完成
        errandOrderService.updateById(update);
        return Result.success();
    }
}
