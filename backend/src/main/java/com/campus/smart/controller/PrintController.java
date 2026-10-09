package com.campus.smart.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.smart.common.Result;
import com.campus.smart.dto.PrintOrderRequest;
import com.campus.smart.entity.PrintOrder;
import com.campus.smart.common.PageQuery;
import com.campus.smart.common.ReqParams;
import com.campus.smart.service.PrintOrderService;
import com.campus.smart.utils.SchoolContext;
import com.campus.smart.utils.UserContext;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * 打印服务模块控制器
 */
@Tag(name = "打印服务")
@RestController
@RequestMapping("/api/print")
public class PrintController {

    @Autowired
    private PrintOrderService printOrderService;

    /**
     * 创建打印订单
     *
     * <p>前端字段名与实体不一致（fileUrl/address/phone/price），
     * 统一在这里完成映射，并补全所有 NOT NULL 列。</p>
     */
    @Operation(summary = "创建打印订单")
    @PostMapping("/createOrder")
    public Result<PrintOrder> createOrder(@RequestBody PrintOrderRequest req) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        Long schoolId = SchoolContext.getSchoolId();

        PrintOrder order = new PrintOrder();
        order.setUserId(userId);
        order.setSchoolId(schoolId);
        // 生成订单号
        order.setOrderNo("PRT" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase());
        // 状态：待打印
        order.setStatus(1);

        // ---- 字段映射（前端名 → 实体/数据库列名）----
        order.setPaperSize(req.getPaperSize());
        order.setCopies(req.getCopies() == null || req.getCopies() < 1 ? 1 : req.getCopies());
        order.setRemark(req.getRemark());
        order.setTotalPages(req.getTotalPages());
        // print_type 为 TINYINT（0:黑白 1:彩色）：由字符串枚举 + 是否彩色共同决定
        order.setPrintType(toPrintType(req));
        // is_double_side 为 TINYINT（0:否 1:是）
        order.setIsDoubleSide(Boolean.TRUE.equals(req.getIsDouble()) ? 1 : 0);

        // ---- 补全 NOT NULL 且无默认值的列 ----
        order.setFileUrls(isBlank(req.getFileUrl()) ? "" : req.getFileUrl());
        order.setDeliveryAddress(isBlank(req.getAddress()) ? "未填写" : req.getAddress());
        order.setReceiverName(isBlank(req.getReceiverName()) ? "未填写" : req.getReceiverName());
        order.setReceiverPhone(isBlank(req.getPhone()) ? "" : req.getPhone());
        order.setTotalAmount(req.getPrice() == null ? BigDecimal.ZERO : req.getPrice());

        printOrderService.save(order);
        return Result.success(order);
    }

    /**
     * 前端打印类型字符串 → print_type 数值
     *
     * <p>库中语义为 0:黑白 1:彩色；前端 printType 为 document/photo/poster/other，
     * 是否彩色由独立的 isColor 开关控制，因此这里优先取 isColor。</p>
     */
    private Integer toPrintType(PrintOrderRequest req) {
        if (Boolean.TRUE.equals(req.getIsColor())) {
            return 1;
        }
        if (req.getPrintType() != null && !req.getPrintType().trim().isEmpty()) {
            try {
                // 兼容直接传数字的情况
                return Integer.valueOf(req.getPrintType().trim());
            } catch (NumberFormatException ignored) {
                // 传的是 document/photo 等字符串：默认黑白
            }
        }
        return 0;
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    /**
     * 我的订单列表
     */
    @Operation(summary = "我的订单列表")
    @GetMapping("/orderList")
    public Result<Page<PrintOrder>> orderList(PageQuery pageQuery,
                                               @RequestParam(required = false) Integer status) {
        Long userId = UserContext.getUserId();
        Long schoolId = SchoolContext.getSchoolId();
        LambdaQueryWrapper<PrintOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PrintOrder::getUserId, userId);

        // 按学校筛选：与其他模块（快递/跑腿/代课）保持一致
        if (schoolId != null) {
            wrapper.eq(PrintOrder::getSchoolId, schoolId);
        }

        if (status != null) {
            wrapper.eq(PrintOrder::getStatus, status);
        }

        wrapper.orderByDesc(PrintOrder::getCreateTime);

        Page<PrintOrder> page = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());
        Page<PrintOrder> result = printOrderService.page(page, wrapper);
        return Result.success(result);
    }

    /**
     * 订单详情
     */
    @Operation(summary = "订单详情")
    @GetMapping("/orderDetail")
    public Result<PrintOrder> orderDetail(@RequestParam Long id) {
        Long userId = UserContext.getUserId();
        PrintOrder order = printOrderService.getById(id);
        if (order == null) {
            return Result.error("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            return Result.error("无权查看此订单");
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
        PrintOrder order = printOrderService.getById(id);
        if (order == null) {
            return Result.error("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            return Result.error("只能取消自己的订单");
        }
        if (order.getStatus() != 1) {
            return Result.error("当前状态不能取消");
        }

        PrintOrder update = new PrintOrder();
        update.setId(id);
        // 状态语义与管理端 AdminOrderController.printCancel 保持一致：4=已取消
        // （原先写 0，而前端/管理端按 0..5 的约定筛选，导致「已取消」列表查不到刚取消的订单）
        update.setStatus(4); // 已取消
        update.setCancelReason("用户取消");
        printOrderService.updateById(update);
        return Result.success();
    }
}
