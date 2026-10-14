package com.campus.smart.service.ai.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.smart.entity.ErrandOrder;
import com.campus.smart.mapper.ErrandOrderMapper;
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
 * 工具：查询当前用户的校园跑腿订单
 *
 * @author 万盛博
 */
@Slf4j
@Component
public class QueryErrandOrderTool implements AiTool {

    @Autowired
    private ErrandOrderMapper errandOrderMapper;

    private static final int LIMIT = 5;

    @Override
    public String name() {
        return "query_errand_order";
    }

    @Override
    public String description() {
        return "查询当前登录用户的校园跑腿订单（代买、代取、代送、代排队）。"
                + "当用户询问自己的跑腿订单状态、跑腿进度、谁接了单等问题时调用。"
                + "只能查询用户自己的订单。";
    }

    @Override
    public Map<String, Object> parameters() {
        Map<String, Object> props = new LinkedHashMap<>();

        Map<String, Object> status = new LinkedHashMap<>();
        status.put("type", "string");
        status.put("description", "按状态筛选：pending=待接单，processing=进行中，done=已完成，all=全部");
        status.put("enum", Arrays.asList("pending", "processing", "done", "all"));
        props.put("status", status);

        Map<String, Object> keyword = new LinkedHashMap<>();
        keyword.put("type", "string");
        keyword.put("description", "按标题关键词模糊搜索，例如\"午饭\"\"快递\"（可选）");
        props.put("keyword", keyword);

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", props);
        schema.put("required", Arrays.asList());
        return schema;
    }

    @Override
    public String execute(Long userId, Map<String, Object> args) {
        LambdaQueryWrapper<ErrandOrder> qw = new LambdaQueryWrapper<>();
        qw.eq(ErrandOrder::getUserId, userId);

        Object statusArg = args.get("status");
        if (statusArg != null && !"all".equals(String.valueOf(statusArg))) {
            Integer status = AiToolSupport.mapStatus(String.valueOf(statusArg));
            if (status != null) {
                qw.eq(ErrandOrder::getStatus, status);
            }
        } else {
            qw.ne(ErrandOrder::getStatus, 0);
        }

        Object keywordArg = args.get("keyword");
        if (keywordArg != null && !String.valueOf(keywordArg).trim().isEmpty()) {
            qw.like(ErrandOrder::getTitle, String.valueOf(keywordArg).trim());
        }

        qw.orderByDesc(ErrandOrder::getId).last("LIMIT " + LIMIT);
        List<ErrandOrder> list = errandOrderMapper.selectList(qw);

        if (list == null || list.isEmpty()) {
            return "查询结果：当前用户没有符合条件的跑腿订单。请如实告知，不要编造。";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("查询结果：共找到 ").append(list.size()).append(" 条跑腿订单：\n");
        int i = 1;
        for (ErrandOrder o : list) {
            sb.append(i++).append(". 订单号 ").append(o.getOrderNo())
              .append(" | 事项：").append(AiToolSupport.textOrNone(o.getTitle()))
              .append(" | 状态：").append(AiToolSupport.statusText(o.getStatus()))
              .append(" | 取货：").append(AiToolSupport.textOrNone(o.getPickupAddress()))
              .append(" | 送达：").append(AiToolSupport.textOrNone(o.getDeliveryAddress()))
              .append(" | 期望时间：").append(AiToolSupport.textOrNone(o.getEstimatedTime()))
              .append(" | 金额：￥").append(AiToolSupport.amountOrZero(o.getTotalAmount()))
              .append("\n");
        }
        sb.append("请基于以上真实数据回答用户，不要添加数据中没有的信息。");
        return sb.toString();
    }

}
