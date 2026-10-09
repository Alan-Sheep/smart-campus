package com.campus.smart.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.smart.common.Result;
import com.campus.smart.entity.ChatMessage;
import com.campus.smart.entity.Message;
import com.campus.smart.common.PageQuery;
import com.campus.smart.service.ChatMessageService;
import com.campus.smart.service.MessageService;
import com.campus.smart.utils.UserContext;
import com.campus.smart.entity.User;
import com.campus.smart.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 消息模块控制器
 */
@Tag(name = "消息模块")
@RestController
@RequestMapping("/api/message")
public class MessageController {

    @Autowired
    private MessageService messageService;

    @Autowired
    private ChatMessageService chatMessageService;

    @Autowired
    private UserService userService;

    /**
     * 消息列表（按类型）
     * type: 1-系统消息, 2-订单消息, 3-互动消息
     */
    @Operation(summary = "消息列表")
    @GetMapping("/list")
    public Result<Page<Message>> list(PageQuery pageQuery,
                                       @RequestParam(required = false) Integer type) {
        Long userId = UserContext.getUserId();
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getUserId, userId);

        if (type != null) {
            wrapper.eq(Message::getType, type);
        }

        wrapper.orderByDesc(Message::getCreateTime);

        Page<Message> page = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());
        Page<Message> result = messageService.page(page, wrapper);
        return Result.success(result);
    }

    /**
     * 未读消息数
     */
    @Operation(summary = "未读消息数")
    @GetMapping("/unreadCount")
    public Result<Map<String, Long>> unreadCount() {
        Long userId = UserContext.getUserId();

        // 统计各类型未读消息数
        // type: 0-系统通知, 1-订单消息, 2-互动消息
        LambdaQueryWrapper<Message> systemWrapper = new LambdaQueryWrapper<>();
        systemWrapper.eq(Message::getUserId, userId);
        systemWrapper.eq(Message::getType, 0);
        systemWrapper.eq(Message::getIsRead, 0);
        long systemCount = messageService.count(systemWrapper);

        LambdaQueryWrapper<Message> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.eq(Message::getUserId, userId);
        orderWrapper.eq(Message::getType, 1);
        orderWrapper.eq(Message::getIsRead, 0);
        long orderCount = messageService.count(orderWrapper);

        LambdaQueryWrapper<Message> interactWrapper = new LambdaQueryWrapper<>();
        interactWrapper.eq(Message::getUserId, userId);
        interactWrapper.eq(Message::getType, 2);
        interactWrapper.eq(Message::getIsRead, 0);
        long interactCount = messageService.count(interactWrapper);

        // 统计聊天消息未读数
        LambdaQueryWrapper<ChatMessage> chatWrapper = new LambdaQueryWrapper<>();
        chatWrapper.eq(ChatMessage::getToUserId, userId);
        chatWrapper.eq(ChatMessage::getIsRead, 0);
        long chatCount = chatMessageService.count(chatWrapper);

        Map<String, Long> result = new HashMap<>();
        result.put("system", systemCount);
        result.put("order", orderCount);
        result.put("interact", interactCount);
        // chat 单独返回，用于消息页「聊天」区块的未读角标
        result.put("chat", chatCount);
        // total 只累加三类消息，不再叠加 chat：
        // 私聊已同步生成 type=2 的互动消息，叠加会导致同一个私聊被重复计数。
        result.put("total", systemCount + orderCount + interactCount);

        return Result.success(result);
    }

    /**
     * 标记已读
     */
    @Operation(summary = "标记已读")
    @PostMapping("/read")
    public Result<Void> read(@RequestParam(required = false) Long id,
                              @RequestParam(required = false) Integer type) {
        Long userId = UserContext.getUserId();

        if (id != null) {
            // 标记单条消息已读
            Message update = new Message();
            update.setId(id);
            update.setIsRead(1);
            messageService.updateById(update);
        } else if (type != null) {
            // 标记某类型全部已读
            LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Message::getUserId, userId);
            wrapper.eq(Message::getType, type);
            wrapper.eq(Message::getIsRead, 0);

            Message update = new Message();
            update.setIsRead(1);
            messageService.update(update, wrapper);
        } else {
            // 标记全部已读
            LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Message::getUserId, userId);
            wrapper.eq(Message::getIsRead, 0);

            Message update = new Message();
            update.setIsRead(1);
            messageService.update(update, wrapper);
        }

        return Result.success();
    }

    /**
     * 消息详情
     */
    @Operation(summary = "消息详情")
    @GetMapping("/detail")
    public Result<Message> detail(@RequestParam Long id) {
        Long userId = UserContext.getUserId();
        Message msg = messageService.getById(id);
        if (msg == null || !msg.getUserId().equals(userId)) {
            return Result.error("消息不存在");
        }
        // 标记为已读
        if (msg.getIsRead() == 0) {
            msg.setIsRead(1);
            messageService.updateById(msg);
        }
        return Result.success(msg);
    }

    /**
     * 聊天会话列表
     * 查询当前用户参与的所有会话，按会话ID分组，返回每个会话的最后一条消息和对方用户信息
     */
    @Operation(summary = "聊天会话列表")
    @GetMapping("/chatList")
    public Result<List<Map<String, Object>>> chatList() {
        Long userId = UserContext.getUserId();

        // 查询当前用户参与的所有聊天消息
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(ChatMessage::getFromUserId, userId)
                .or().eq(ChatMessage::getToUserId, userId));
        wrapper.orderByDesc(ChatMessage::getCreateTime);

        List<ChatMessage> allMessages = chatMessageService.list(wrapper);

        // 按 conversationId 分组，保留每个会话的最新一条
        Map<String, ChatMessage> latestMap = new HashMap<>();
        for (ChatMessage msg : allMessages) {
            String convId = msg.getConversationId();
            if (!latestMap.containsKey(convId)) {
                latestMap.put(convId, msg);
            }
        }

        // 组装返回结果
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, ChatMessage> entry : latestMap.entrySet()) {
            ChatMessage msg = entry.getValue();
            // 确定对方用户ID
            Long targetUserId = msg.getFromUserId().equals(userId) ? msg.getToUserId() : msg.getFromUserId();
            // 查询对方用户信息
            User targetUser = userService.getById(targetUserId);
            // 统计该会话未读数
            LambdaQueryWrapper<ChatMessage> unreadWrapper = new LambdaQueryWrapper<>();
            unreadWrapper.eq(ChatMessage::getConversationId, entry.getKey());
            unreadWrapper.eq(ChatMessage::getToUserId, userId);
            unreadWrapper.eq(ChatMessage::getIsRead, 0);
            long unreadCount = chatMessageService.count(unreadWrapper);

            Map<String, Object> item = new HashMap<>();
            item.put("conversationId", msg.getConversationId());
            item.put("targetUserId", targetUserId);
            item.put("targetNickname", targetUser != null ? targetUser.getNickname() : "用户" + targetUserId);
            item.put("targetAvatar", targetUser != null ? targetUser.getAvatar() : "");
            item.put("lastMessage", msg.getContent());
            item.put("lastMessageTime", msg.getCreateTime());
            item.put("unread", unreadCount);
            result.add(item);
        }

        return Result.success(result);
    }

    /**
     * 某个会话的聊天记录
     * 查看时自动标记对方发来的消息为已读
     */
    @Operation(summary = "聊天记录")
    @GetMapping("/chatMessages")
    public Result<Page<ChatMessage>> chatMessages(PageQuery pageQuery,
                                                   @RequestParam String conversationId) {
        Long userId = UserContext.getUserId();

        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getConversationId, conversationId);
        // 验证会话归属：当前用户必须是发送方或接收方
        wrapper.and(w -> w.eq(ChatMessage::getFromUserId, userId)
                .or().eq(ChatMessage::getToUserId, userId));
        wrapper.orderByDesc(ChatMessage::getCreateTime);

        Page<ChatMessage> page = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());
        Page<ChatMessage> result = chatMessageService.page(page, wrapper);

        // 自动标记对方发来的消息为已读
        LambdaQueryWrapper<ChatMessage> readWrapper = new LambdaQueryWrapper<>();
        readWrapper.eq(ChatMessage::getConversationId, conversationId);
        readWrapper.eq(ChatMessage::getToUserId, userId);
        readWrapper.eq(ChatMessage::getIsRead, 0);
        ChatMessage update = new ChatMessage();
        update.setIsRead(1);
        chatMessageService.update(update, readWrapper);

        return Result.success(result);
    }

    /**
     * 发送聊天消息
     * 如果不传 conversationId，自动根据收发双方生成
     */
    @Operation(summary = "发送聊天消息")
    @PostMapping("/sendChat")
    public Result<ChatMessage> sendChat(@RequestBody ChatMessage chatMessage) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        chatMessage.setFromUserId(userId);
        chatMessage.setIsRead(0);

        // 接收人必须存在：原先仅在 conversationId 为空时才校验，
        // 若前端传了 conversationId 但没传 toUserId，会插入 NULL 触发
        // "Column 'to_user_id' cannot be null"（HTTP 500）。
        if (chatMessage.getToUserId() == null) {
            return Result.error("接收用户不能为空");
        }

        // 如果没传 conversationId，自动生成（小ID在前，大ID在后）
        if (chatMessage.getConversationId() == null || chatMessage.getConversationId().isEmpty()) {
            Long toUserId = chatMessage.getToUserId();
            String convId = userId < toUserId ? userId + "_" + toUserId : toUserId + "_" + userId;
            chatMessage.setConversationId(convId);
        }

        // msg_type 为 TINYINT（0:文本 1:图片 2:系统）
        if (chatMessage.getMsgType() == null) {
            chatMessage.setMsgType(0);
        }

        chatMessageService.save(chatMessage);

        // ---- 私聊消息同步生成一条「互动消息」 ----
        // 背景：私聊此前只写入 chat_message，既不出现在「互动消息」列表里，
        // 也不计入互动未读数，用户在消息模块看不到任何私聊提醒。
        // 这里在接收方的 message 表中补一条 type=2（互动消息）的记录，
        // relatedId 指向发送者，relatedType 标记来源为私聊，便于跳转会话。
        try {
            User fromUser = userService.getById(userId);
            String fromName = "用户" + userId;
            if (fromUser != null) {
                if (fromUser.getNickname() != null && !fromUser.getNickname().isEmpty()) {
                    fromName = fromUser.getNickname();
                }
            }

            Message notify = new Message();
            notify.setUserId(chatMessage.getToUserId());     // 接收方
            notify.setType(2);                                // 2=互动消息
            notify.setTitle(fromName + " 给你发了一条私信");
            // 列表里展示的摘要，过长截断
            String content = chatMessage.getContent() == null ? "" : chatMessage.getContent();
            notify.setContent(content.length() > 50 ? content.substring(0, 50) + "..." : content);
            notify.setRelatedId(userId);                      // 发送者ID，前端用于打开会话
            notify.setRelatedType("chat");
            notify.setIsRead(0);
            messageService.save(notify);
        } catch (Exception e) {
            // 通知写入失败不应影响私聊主流程
            System.err.println("私聊互动消息写入异常: " + e.getMessage());
        }

        return Result.success(chatMessage);
    }
}
