package com.campus.smart.service.ai.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.smart.entity.ExpressOrder;
import com.campus.smart.mapper.ExpressOrderMapper;
import com.campus.smart.service.ai.AiTool;
import com.campus.smart.service.ai.AiToolSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具：查询当前用户的快递代取订单
 *
 * <p>这是"Agent 能办事"的核心示例——用户问"我的快递到哪了"，
 * 模型识别出需要实时数据，调用本工具查库，拿到真实订单后再组织回答。</p>
 *
 * @author 万盛博
 */
@Slf4j
@Component
public class QueryExpressOrderTool implements AiTool {

    @Autowired
    private ExpressOrderMapper expressOrderMapper;

    /** 单次返回上限（避免把大量数据塞进上下文浪费 token） */
    private static final int LIMIT = 5;

    @Override
    public String name() {
        return "query_express_order";
    }

    @Override
    public String description() {
        return "查询当前登录用户的快递代取订单。当用户询问自己的快递订单状态、取件码、"
                + "快递到没到、订单进度等问题时调用。只能查询用户自己的订单，"
                + "不要用于查询他人的订单，也不要用于查询快递公司物流轨迹（本系统不含物流轨迹数据）。";
    }

    @Override
    public Map<String, Object> parameters() {
        Map<String, Object> props = new LinkedHashMap<>();

        Map<String, Object> status = new LinkedHashMap<>();
        status.put("type", "string");
        status.put("description", "按状态筛选：pending=待接单，processing=进行中，done=已完成，all=全部");
        status.put("enum", Arrays.asList("pending", "processing", "done", "all"));
        props.put("status", status);

        Map<String, Object> orderNo = new LinkedHashMap<>();
        orderNo.put("type", "string");
        orderNo.put("description", "订单号（用户明确提供了订单号时才填，否则不要填）");
        props.put("orderNo", orderNo);

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", props);
        schema.put("required", Arrays.asList());  // 两个参数都是可选的
        return schema;
    }

    @Override
    public String execute(Long userId, Map<String, Object> args) {
        LambdaQueryWrapper<ExpressOrder> qw = new LambdaQueryWrapper<>();
        // userId 由后端注入，模型无法指定 → 从设计上杜绝越权查询
        qw.eq(ExpressOrder::getUserId, userId);

        Object statusArg = args.get("status");
        if (statusArg != null && !"all".equals(String.valueOf(statusArg))) {
            Integer status = AiToolSupport.mapStatus(String.valueOf(statusArg));
            if (status != null) {
                qw.eq(ExpressOrder::getStatus, status);
            }
        } else {
            qw.ne(ExpressOrder::getStatus, 0); // 默认不展示已取消的
        }

        Object orderNoArg = args.get("orderNo");
        if (orderNoArg != null && !String.valueOf(orderNoArg).trim().isEmpty()) {
            qw.eq(ExpressOrder::getOrderNo, String.valueOf(orderNoArg).trim());
        }

        qw.orderByDesc(ExpressOrder::getId).last("LIMIT " + LIMIT);
        List<ExpressOrder> list = expressOrderMapper.selectList(qw);

        if (list == null || list.isEmpty()) {
            return "查询结果：当前用户没有符合条件的快递代取订单。"
                    + "请如实告知用户没有找到订单，不要编造订单信息。";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("查询结果：共找到 ").append(list.size()).append(" 条快递代取订单（最多显示 ").append(LIMIT).append(" 条）：\n");
        int i = 1;
        for (ExpressOrder o : list) {
            sb.append(i++).append(". 订单号 ").append(o.getOrderNo())
              .append(" | 快递公司：").append(AiToolSupport.textOrNone(o.getExpressCompany()))
              .append(" | 状态：").append(AiToolSupport.statusText(o.getStatus()))
              .append(" | 取件码：").append(AiToolSupport.textOrNone(o.getPickupCode()))
              .append(" | 取件点：").append(AiToolSupport.textOrNone(o.getPickupAddress()))
              .append(" | 送达地址：").append(AiToolSupport.textOrNone(o.getDeliveryAddress()))
              .append(" | 重量：").append(o.getWeight() == null ? "未知" : o.getWeight() + "kg")
              .append(" | 金额：￥").append(AiToolSupport.amountOrZero(o.getTotalAmount()))
              .append(" | 下单时间：").append(AiToolSupport.timeText(o.getCreateTime()))
              .append("\n");
        }
        sb.append("请基于以上真实数据回答用户，不要添加数据中没有的信息。");
        return sb.toString();
    }

}
