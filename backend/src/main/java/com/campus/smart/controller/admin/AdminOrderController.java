package com.campus.smart.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.smart.common.Result;
import com.campus.smart.entity.ErrandOrder;
import com.campus.smart.entity.ExpressOrder;
import com.campus.smart.entity.PrintOrder;
import com.campus.smart.entity.SubstituteOrder;
import com.campus.smart.service.ErrandOrderService;
import com.campus.smart.service.ExpressOrderService;
import com.campus.smart.service.PrintOrderService;
import com.campus.smart.service.SubstituteOrderService;
import com.campus.smart.utils.AdminContext;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 管理端-订单管理控制器
 * 包含快递代取、打印服务、校园跑腿、代课服务四种订单的管理
 */
@Tag(name = "【管理端】订单管理")
@RestController
@RequestMapping("/api/admin/order")
public class AdminOrderController {

    @Autowired
    private ExpressOrderService expressOrderService;

    @Autowired
    private PrintOrderService printOrderService;

    @Autowired
    private ErrandOrderService errandOrderService;

    @Autowired
    private SubstituteOrderService substituteOrderService;

    // ==================== 快递代取订单 ====================

    /**
     * 快递代取订单列表
     * 分页 + 状态筛选
     */
    @Operation(summary = "快递代取订单列表")
    @GetMapping("/expressList")
    public Result<IPage<ExpressOrder>> expressList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long schoolId) {

        LambdaQueryWrapper<ExpressOrder> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(ExpressOrder::getStatus, status);
        }
        if (schoolId != null) {
            wrapper.eq(ExpressOrder::getSchoolId, schoolId);
        }
        wrapper.orderByDesc(ExpressOrder::getCreateTime);

        Page<ExpressOrder> page = new Page<>(pageNum, pageSize);
        IPage<ExpressOrder> result = expressOrderService.page(page, wrapper);
        return Result.success(result);
    }

    /**
     * 快递代取订单详情
     */
    @Operation(summary = "快递代取订单详情")
    @GetMapping("/expressDetail/{id}")
    public Result<ExpressOrder> expressDetail(@PathVariable Long id) {
        ExpressOrder order = expressOrderService.getById(id);
        if (order == null) {
            return Result.error("订单不存在");
        }
        return Result.success(order);
    }

    /**
     * 快递代取订单接单
     */
    @Operation(summary = "快递代取订单接单")
    @PutMapping("/expressAccept/{id}")
    public Result<Void> expressAccept(@PathVariable Long id) {
        ExpressOrder order = expressOrderService.getById(id);
        if (order == null) {
            return Result.error("订单不存在");
        }
        // 状态校验：待接单(1)才能接单
        if (order.getStatus() != 1) {
            return Result.error("当前订单状态不允许接单");
        }

        ExpressOrder update = new ExpressOrder();
        update.setId(id);
        update.setStatus(2); // 已接单
        update.setPickerId(AdminContext.getAdminId()); // 管理员作为接单人
        update.setAcceptTime(LocalDateTime.now());
        expressOrderService.updateById(update);
        return Result.success();
    }

    /**
     * 快递代取订单完成
     */
    @Operation(summary = "快递代取订单完成")
    @PutMapping("/expressComplete/{id}")
    public Result<Void> expressComplete(@PathVariable Long id) {
        ExpressOrder order = expressOrderService.getById(id);
        if (order == null) {
            return Result.error("订单不存在");
        }
        // 状态校验：已接单(2)才能完成
        if (order.getStatus() != 2) {
            return Result.error("当前订单状态不允许完成");
        }

        ExpressOrder update = new ExpressOrder();
        update.setId(id);
        update.setStatus(3); // 已完成
        update.setCompleteTime(LocalDateTime.now());
        expressOrderService.updateById(update);
        return Result.success();
    }

    /**
     * 快递代取订单取消
     */
    @Operation(summary = "快递代取订单取消")
    @PutMapping("/expressCancel/{id}")
    public Result<Void> expressCancel(@PathVariable Long id, @RequestParam(required = false) String reason) {
        ExpressOrder order = expressOrderService.getById(id);
        if (order == null) {
            return Result.error("订单不存在");
        }
        // 状态校验：已完成/已取消的不能取消
        if (order.getStatus() == 3 || order.getStatus() == 4) {
            return Result.error("当前订单状态不允许取消");
        }

        ExpressOrder update = new ExpressOrder();
        update.setId(id);
        update.setStatus(4); // 已取消
        update.setCancelReason(reason != null ? reason : "管理员取消");
        expressOrderService.updateById(update);
        return Result.success();
    }

    /**
     * 快递代取订单退款
     */
    @Operation(summary = "快递代取订单退款")
    @PutMapping("/expressRefund/{id}")
    public Result<Void> expressRefund(@PathVariable Long id) {
        ExpressOrder order = expressOrderService.getById(id);
        if (order == null) {
            return Result.error("订单不存在");
        }
        // 状态校验：已取消的才能退款
        if (order.getStatus() != 4) {
            return Result.error("请先取消订单再退款");
        }

        ExpressOrder update = new ExpressOrder();
        update.setId(id);
        update.setStatus(5); // 已退款
        expressOrderService.updateById(update);
        return Result.success();
    }

    // ==================== 打印服务订单 ====================

    /**
     * 打印服务订单列表
     * 分页 + 状态筛选
     */
    @Operation(summary = "打印服务订单列表")
    @GetMapping("/printList")
    public Result<IPage<PrintOrder>> printList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long schoolId) {

        LambdaQueryWrapper<PrintOrder> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(PrintOrder::getStatus, status);
        }
        if (schoolId != null) {
            wrapper.eq(PrintOrder::getSchoolId, schoolId);
        }
        wrapper.orderByDesc(PrintOrder::getCreateTime);

        Page<PrintOrder> page = new Page<>(pageNum, pageSize);
        IPage<PrintOrder> result = printOrderService.page(page, wrapper);
        return Result.success(result);
    }

    /**
     * 打印服务订单详情
     */
    @Operation(summary = "打印服务订单详情")
    @GetMapping("/printDetail/{id}")
    public Result<PrintOrder> printDetail(@PathVariable Long id) {
        PrintOrder order = printOrderService.getById(id);
        if (order == null) {
            return Result.error("订单不存在");
        }
        return Result.success(order);
    }

    /**
     * 打印服务订单接单
     */
    @Operation(summary = "打印服务订单接单")
    @PutMapping("/printAccept/{id}")
    public Result<Void> printAccept(@PathVariable Long id) {
        PrintOrder order = printOrderService.getById(id);
        if (order == null) {
            return Result.error("订单不存在");
        }
        if (order.getStatus() != 1) {
            return Result.error("当前订单状态不允许接单");
        }

        PrintOrder update = new PrintOrder();
        update.setId(id);
        update.setStatus(2); // 已接单
        update.setPrinterId(AdminContext.getAdminId());
        update.setAcceptTime(LocalDateTime.now());
        printOrderService.updateById(update);
        return Result.success();
    }

    /**
     * 打印服务订单完成
     */
    @Operation(summary = "打印服务订单完成")
    @PutMapping("/printComplete/{id}")
    public Result<Void> printComplete(@PathVariable Long id) {
        PrintOrder order = printOrderService.getById(id);
        if (order == null) {
            return Result.error("订单不存在");
        }
        if (order.getStatus() != 2) {
            return Result.error("当前订单状态不允许完成");
        }

        PrintOrder update = new PrintOrder();
        update.setId(id);
        update.setStatus(3); // 已完成
        update.setCompleteTime(LocalDateTime.now());
        printOrderService.updateById(update);
        return Result.success();
    }

    /**
     * 打印服务订单取消
     */
    @Operation(summary = "打印服务订单取消")
    @PutMapping("/printCancel/{id}")
    public Result<Void> printCancel(@PathVariable Long id, @RequestParam(required = false) String reason) {
        PrintOrder order = printOrderService.getById(id);
        if (order == null) {
            return Result.error("订单不存在");
        }
        if (order.getStatus() == 3 || order.getStatus() == 4) {
            return Result.error("当前订单状态不允许取消");
        }

        PrintOrder update = new PrintOrder();
        update.setId(id);
        update.setStatus(4); // 已取消
        update.setCancelReason(reason != null ? reason : "管理员取消");
        printOrderService.updateById(update);
        return Result.success();
    }

    /**
     * 打印服务订单退款
     */
    @Operation(summary = "打印服务订单退款")
    @PutMapping("/printRefund/{id}")
    public Result<Void> printRefund(@PathVariable Long id) {
        PrintOrder order = printOrderService.getById(id);
        if (order == null) {
            return Result.error("订单不存在");
        }
        if (order.getStatus() != 4) {
            return Result.error("请先取消订单再退款");
        }

        PrintOrder update = new PrintOrder();
        update.setId(id);
        update.setStatus(5); // 已退款
        printOrderService.updateById(update);
        return Result.success();
    }

    // ==================== 校园跑腿订单 ====================

    /**
     * 校园跑腿订单列表
     * 分页 + 状态筛选
     */
    @Operation(summary = "校园跑腿订单列表")
    @GetMapping("/errandList")
    public Result<IPage<ErrandOrder>> errandList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long schoolId) {

        LambdaQueryWrapper<ErrandOrder> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(ErrandOrder::getStatus, status);
        }
        if (schoolId != null) {
            wrapper.eq(ErrandOrder::getSchoolId, schoolId);
        }
        wrapper.orderByDesc(ErrandOrder::getCreateTime);

        Page<ErrandOrder> page = new Page<>(pageNum, pageSize);
        IPage<ErrandOrder> result = errandOrderService.page(page, wrapper);
        return Result.success(result);
    }

    /**
     * 校园跑腿订单详情
     */
    @Operation(summary = "校园跑腿订单详情")
    @GetMapping("/errandDetail/{id}")
    public Result<ErrandOrder> errandDetail(@PathVariable Long id) {
        ErrandOrder order = errandOrderService.getById(id);
        if (order == null) {
            return Result.error("订单不存在");
        }
        return Result.success(order);
    }

    /**
     * 校园跑腿订单接单
     */
    @Operation(summary = "校园跑腿订单接单")
    @PutMapping("/errandAccept/{id}")
    public Result<Void> errandAccept(@PathVariable Long id) {
        ErrandOrder order = errandOrderService.getById(id);
        if (order == null) {
            return Result.error("订单不存在");
        }
        if (order.getStatus() != 1) {
            return Result.error("当前订单状态不允许接单");
        }

        ErrandOrder update = new ErrandOrder();
        update.setId(id);
        update.setStatus(2); // 已接单
        update.setRunnerId(AdminContext.getAdminId());
        update.setAcceptTime(LocalDateTime.now());
        errandOrderService.updateById(update);
        return Result.success();
    }

    /**
     * 校园跑腿订单完成
     */
    @Operation(summary = "校园跑腿订单完成")
    @PutMapping("/errandComplete/{id}")
    public Result<Void> errandComplete(@PathVariable Long id) {
        ErrandOrder order = errandOrderService.getById(id);
        if (order == null) {
            return Result.error("订单不存在");
        }
        if (order.getStatus() != 2) {
            return Result.error("当前订单状态不允许完成");
        }

        ErrandOrder update = new ErrandOrder();
        update.setId(id);
        update.setStatus(3); // 已完成
        update.setCompleteTime(LocalDateTime.now());
        errandOrderService.updateById(update);
        return Result.success();
    }

    /**
     * 校园跑腿订单取消
     */
    @Operation(summary = "校园跑腿订单取消")
    @PutMapping("/errandCancel/{id}")
    public Result<Void> errandCancel(@PathVariable Long id, @RequestParam(required = false) String reason) {
        ErrandOrder order = errandOrderService.getById(id);
        if (order == null) {
            return Result.error("订单不存在");
        }
        if (order.getStatus() == 3 || order.getStatus() == 4) {
            return Result.error("当前订单状态不允许取消");
        }

        ErrandOrder update = new ErrandOrder();
        update.setId(id);
        update.setStatus(4); // 已取消
        update.setCancelReason(reason != null ? reason : "管理员取消");
        errandOrderService.updateById(update);
        return Result.success();
    }

    /**
     * 校园跑腿订单退款
     */
    @Operation(summary = "校园跑腿订单退款")
    @PutMapping("/errandRefund/{id}")
    public Result<Void> errandRefund(@PathVariable Long id) {
        ErrandOrder order = errandOrderService.getById(id);
        if (order == null) {
            return Result.error("订单不存在");
        }
        if (order.getStatus() != 4) {
            return Result.error("请先取消订单再退款");
        }

        ErrandOrder update = new ErrandOrder();
        update.setId(id);
        update.setStatus(5); // 已退款
        errandOrderService.updateById(update);
        return Result.success();
    }

    // ==================== 代课服务订单 ====================

    /**
     * 代课服务订单列表
     * 分页 + 状态筛选
     */
    @Operation(summary = "代课服务订单列表")
    @GetMapping("/substituteList")
    public Result<IPage<SubstituteOrder>> substituteList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long schoolId) {

        LambdaQueryWrapper<SubstituteOrder> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(SubstituteOrder::getStatus, status);
        }
        if (schoolId != null) {
            wrapper.eq(SubstituteOrder::getSchoolId, schoolId);
        }
        wrapper.orderByDesc(SubstituteOrder::getCreateTime);

        Page<SubstituteOrder> page = new Page<>(pageNum, pageSize);
        IPage<SubstituteOrder> result = substituteOrderService.page(page, wrapper);
        return Result.success(result);
    }

    /**
     * 代课服务订单详情
     */
    @Operation(summary = "代课服务订单详情")
    @GetMapping("/substituteDetail/{id}")
    public Result<SubstituteOrder> substituteDetail(@PathVariable Long id) {
        SubstituteOrder order = substituteOrderService.getById(id);
        if (order == null) {
            return Result.error("订单不存在");
        }
        return Result.success(order);
    }

    /**
     * 代课服务订单接单
     */
    @Operation(summary = "代课服务订单接单")
    @PutMapping("/substituteAccept/{id}")
    public Result<Void> substituteAccept(@PathVariable Long id) {
        SubstituteOrder order = substituteOrderService.getById(id);
        if (order == null) {
            return Result.error("订单不存在");
        }
        if (order.getStatus() != 1) {
            return Result.error("当前订单状态不允许接单");
        }

        SubstituteOrder update = new SubstituteOrder();
        update.setId(id);
        update.setStatus(2); // 已接单
        update.setSubstituteId(AdminContext.getAdminId());
        update.setAcceptTime(LocalDateTime.now());
        substituteOrderService.updateById(update);
        return Result.success();
    }

    /**
     * 代课服务订单完成
     */
    @Operation(summary = "代课服务订单完成")
    @PutMapping("/substituteComplete/{id}")
    public Result<Void> substituteComplete(@PathVariable Long id) {
        SubstituteOrder order = substituteOrderService.getById(id);
        if (order == null) {
            return Result.error("订单不存在");
        }
        if (order.getStatus() != 2) {
            return Result.error("当前订单状态不允许完成");
        }

        SubstituteOrder update = new SubstituteOrder();
        update.setId(id);
        update.setStatus(3); // 已完成
        update.setCompleteTime(LocalDateTime.now());
        substituteOrderService.updateById(update);
        return Result.success();
    }

    /**
     * 代课服务订单取消
     */
    @Operation(summary = "代课服务订单取消")
    @PutMapping("/substituteCancel/{id}")
    public Result<Void> substituteCancel(@PathVariable Long id, @RequestParam(required = false) String reason) {
        SubstituteOrder order = substituteOrderService.getById(id);
        if (order == null) {
            return Result.error("订单不存在");
        }
        if (order.getStatus() == 3 || order.getStatus() == 4) {
            return Result.error("当前订单状态不允许取消");
        }

        SubstituteOrder update = new SubstituteOrder();
        update.setId(id);
        update.setStatus(4); // 已取消
        update.setCancelReason(reason != null ? reason : "管理员取消");
        substituteOrderService.updateById(update);
        return Result.success();
    }

    /**
     * 代课服务订单退款
     */
    @Operation(summary = "代课服务订单退款")
    @PutMapping("/substituteRefund/{id}")
    public Result<Void> substituteRefund(@PathVariable Long id) {
        SubstituteOrder order = substituteOrderService.getById(id);
        if (order == null) {
            return Result.error("订单不存在");
        }
        if (order.getStatus() != 4) {
            return Result.error("请先取消订单再退款");
        }

        SubstituteOrder update = new SubstituteOrder();
        update.setId(id);
        update.setStatus(5); // 已退款
        substituteOrderService.updateById(update);
        return Result.success();
    }
}
