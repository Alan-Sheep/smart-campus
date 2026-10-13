<template>
  <!-- 消息页 -->
  <view class="message-page">
    <!-- 自定义导航栏 -->
    <view class="nav-bar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-title">消息</view>
    </view>

    <scroll-view 
      scroll-y 
      class="scroll-content"
      :style="{ paddingTop: navHeight + 'px' }"
      refresher-enabled
      :refresher-triggered="isRefreshing"
      @refresherrefresh="onRefresh"
    >
      <!-- 顶部三个消息分类 -->
      <view class="category-cards">
        <view class="cat-card" @click="goToMessageList(0)">
          <view class="cat-icon system-icon">
            <text class="iconfont">📢</text>
          </view>
          <view class="cat-info">
            <text class="cat-title">系统通知</text>
            <text class="cat-desc">{{ systemLatest || '暂无新通知' }}</text>
          </view>
          <view v-if="unreadCount.system > 0" class="cat-badge">{{ unreadCount.system > 99 ? '99+' : unreadCount.system }}</view>
          <text class="cat-arrow">›</text>
        </view>

        <view class="cat-card" @click="goToMessageList(1)">
          <view class="cat-icon order-icon">
            <text class="iconfont">📦</text>
          </view>
          <view class="cat-info">
            <text class="cat-title">订单消息</text>
            <text class="cat-desc">{{ orderLatest || '暂无订单消息' }}</text>
          </view>
          <view v-if="unreadCount.order > 0" class="cat-badge">{{ unreadCount.order > 99 ? '99+' : unreadCount.order }}</view>
          <text class="cat-arrow">›</text>
        </view>

        <view class="cat-card" @click="goToMessageList(2)">
          <view class="cat-icon interact-icon">
            <text class="iconfont">💬</text>
          </view>
          <view class="cat-info">
            <text class="cat-title">互动消息</text>
            <text class="cat-desc">{{ interactLatest || '暂无互动消息' }}</text>
          </view>
          <view v-if="unreadCount.interact > 0" class="cat-badge">{{ unreadCount.interact > 99 ? '99+' : unreadCount.interact }}</view>
          <text class="cat-arrow">›</text>
        </view>
      </view>

      <!-- 私聊列表 -->
      <view class="chat-section">
        <view class="section-header">
          <text class="section-title">私聊消息</text>
          <text v-if="unreadCount.chat > 0" class="section-badge">{{ unreadCount.chat }} 条未读</text>
        </view>

        <view class="chat-list">
          <view 
            class="chat-item" 
            v-for="item in chatList" 
            :key="item.conversationId"
            @click="goToChat(item)"
          >
            <view class="chat-avatar-wrap">
              <view v-if="!item.targetAvatar" class="chat-avatar-default">
                <text>{{ item.targetNickname ? item.targetNickname.charAt(0) : '用' }}</text>
              </view>
              <image v-else :src="item.targetAvatar" class="chat-avatar"></image>
              <view v-if="item.unread > 0" class="chat-badge">{{ item.unread > 99 ? '99+' : item.unread }}</view>
            </view>
            <view class="chat-info">
              <view class="chat-top">
                <text class="chat-name">{{ item.targetNickname || '用户' + item.targetUserId }}</text>
                <text class="chat-time">{{ formatTime(item.lastMessageTime) }}</text>
              </view>
              <text class="chat-last">{{ item.lastMessage || '暂无消息' }}</text>
            </view>
          </view>
        </view>

        <view class="empty-tip" v-if="chatList.length === 0">
          <text>暂无私聊消息</text>
        </view>
      </view>

      <view class="bottom-space"></view>
    </scroll-view>
      <!-- AI 助手悬浮球 -->
    <view class="ai-fab" :class="{ 'is-expanded': aiFabExpanded }" @click="onAiFabClick">
      <text class="ai-fab-icon">🤖</text>
    </view>
  </view>
</template>

<script setup>
/**
 * 消息页
 * 顶部：系统通知、订单消息、互动消息三个分类
 * 底部：私聊会话列表
 */
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { onShow, onHide } from '@dcloudio/uni-app'
import { messageApi } from '@/api/index'
import { checkLogin } from '@/utils/request'

const statusBarHeight = ref(20)
const navHeight = ref(88)
const isRefreshing = ref(false)
const chatList = ref([])
const systemLatest = ref('')
const orderLatest = ref('')
const interactLatest = ref('')
const unreadCount = reactive({
  system: 0,
  order: 0,
  interact: 0,
  chat: 0,
  total: 0
})

onMounted(() => {
  const sysInfo = uni.getSystemInfoSync()
  statusBarHeight.value = sysInfo.statusBarHeight || 20
  navHeight.value = statusBarHeight.value + 44

  loadData()

  // ---- 未读数轮询 ----
  // 让聊天/互动消息的未读角标在停留在本页时也能更新
  msgTimer = setInterval(loadData, 5000)
})

/** 未读数轮询定时器 */
let msgTimer = null

/** 页面显示时立即刷新并恢复轮询 */
onShow(() => {
  loadData()
  if (!msgTimer) {
    msgTimer = setInterval(loadData, 5000)
  }
})

/** 页面隐藏时暂停轮询 */
onHide(() => {
  if (msgTimer) {
    clearInterval(msgTimer)
    msgTimer = null
  }
})

onUnmounted(() => {
  if (msgTimer) {
    clearInterval(msgTimer)
    msgTimer = null
  }
})

/**
 * 加载所有数据
 */
async function loadData() {
  const isLoggedIn = checkLogin(false)
  
  if (!isLoggedIn) {
    // 未登录：显示空状态
    chatList.value = []
    systemLatest.value = '登录后查看系统通知'
    orderLatest.value = '登录后查看订单消息'
    interactLatest.value = '登录后查看互动消息'
    return
  }

  try {
    // 并行加载：未读数 + 聊天列表 + 各类型最新一条
    const [unreadRes, chatRes, systemRes, orderRes, interactRes] = await Promise.all([
      messageApi.getUnreadCount().catch(() => ({})),
      messageApi.getChatList().catch(() => []),
      messageApi.getNoticeList({ pageNum: 1, pageSize: 1 }).catch(() => ({ records: [] })),
      messageApi.getOrderMessageList({ pageNum: 1, pageSize: 1 }).catch(() => ({ records: [] })),
      messageApi.getMessageList({ pageNum: 1, pageSize: 1, type: 2 }).catch(() => ({ records: [] }))
    ])

    // 未读数
    if (unreadRes) {
      unreadCount.system = unreadRes.system || 0
      unreadCount.order = unreadRes.order || 0
      unreadCount.interact = unreadRes.interact || 0
      unreadCount.chat = unreadRes.chat || 0
      unreadCount.total = unreadRes.total || 0
    }

    // 聊天列表
    chatList.value = Array.isArray(chatRes) ? chatRes : (chatRes.records || chatRes.list || [])

    // 各类型最新消息
    const sysList = systemRes.records || systemRes.list || []
    if (sysList.length > 0) {
      systemLatest.value = sysList[0].title || sysList[0].content || ''
    }

    const ordList = orderRes.records || orderRes.list || []
    if (ordList.length > 0) {
      orderLatest.value = ordList[0].title || ordList[0].content || ''
    }

    const intList = interactRes.records || interactRes.list || []
    if (intList.length > 0) {
      interactLatest.value = intList[0].title || intList[0].content || ''
    }
  } catch (err) {
    console.warn('加载消息数据失败:', err)
  }
}

/**
 * 跳转到对应类型的消息列表
 */
function goToMessageList(type) {
  if (!checkLogin()) return
  const typeNames = ['系统通知', '订单消息', '互动消息']
  uni.navigateTo({
    url: '/pages/message/list?type=' + type + '&title=' + encodeURIComponent(typeNames[type])
  })
}

/**
 * 跳转到聊天详情
 */
function goToChat(item) {
  if (!checkLogin()) return
  uni.navigateTo({
    url: '/pages/message/chat?conversationId=' + item.conversationId + 
         '&targetUserId=' + item.targetUserId +
         '&targetNickname=' + encodeURIComponent(item.targetNickname || '')
  })
}

/**
 * 格式化时间
 */
function formatTime(timeStr) {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const oneDay = 24 * 60 * 60 * 1000

  if (diff < 60 * 1000) return '刚刚'
  if (diff < 60 * 60 * 1000) return Math.floor(diff / (60 * 1000)) + '分钟前'
  if (diff < oneDay) return Math.floor(diff / (60 * 60 * 1000)) + '小时前'
  if (diff < oneDay * 7) return Math.floor(diff / oneDay) + '天前'
  
  const month = (date.getMonth() + 1).toString().padStart(2, '0')
  const day = date.getDate().toString().padStart(2, '0')
  return month + '-' + day
}

/**
 * 下拉刷新
 */
function onRefresh() {
  isRefreshing.value = true
  loadData().finally(() => {
    isRefreshing.value = false
  })
}
let aiFabExpanded = false
function onAiFabClick() {
  // 第一次点击：滑出悬浮球；已展开则直接进入
  if (!aiFabExpanded) {
    aiFabExpanded = true
    // 模板里用的响应式状态在 setup 中需要 ref—— uni-app vue3 setup 中直接改普通变量不触发视图
    // 因此用 class 切换通过 DOM 操作兜底（或声明 ref）。此处简单起见：展开后 600ms 跳转
    setTimeout(() => { uni.navigateTo({ url: '/pages/ai/chat' }) }, 600)
  } else {
    uni.navigateTo({ url: '/pages/ai/chat' })
  }
}
</script>

<style lang="scss" scoped>
.message-page {
  min-height: 100vh;
  background: #f5f5f7;
}

.nav-bar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  background: #fff;
  border-bottom: 1rpx solid #eee;
}

.nav-title {
  height: 88rpx;
  line-height: 88rpx;
  text-align: center;
  font-size: 34rpx;
  font-weight: bold;
  color: #333;
}

.scroll-content {
  height: 100vh;
  box-sizing: border-box;
}

/* 分类卡片 */
.category-cards {
  background: #fff;
  margin: 20rpx;
  border-radius: 16rpx;
  overflow: hidden;
}

.cat-card {
  display: flex;
  align-items: center;
  padding: 28rpx 24rpx;
  border-bottom: 1rpx solid #f0f0f0;
  position: relative;

  &:last-child {
    border-bottom: none;
  }

  &:active {
    background: #f9f9f9;
  }
}

.cat-icon {
  width: 80rpx;
  height: 80rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36rpx;
  margin-right: 20rpx;
  flex-shrink: 0;
}

.system-icon {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.order-icon {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.interact-icon {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.cat-info {
  flex: 1;
  min-width: 0;
}

.cat-title {
  font-size: 30rpx;
  font-weight: 500;
  color: #333;
  display: block;
  margin-bottom: 8rpx;
}

.cat-desc {
  font-size: 24rpx;
  color: #999;
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cat-badge {
  position: absolute;
  right: 60rpx;
  top: 50%;
  transform: translateY(-50%);
  min-width: 32rpx;
  height: 32rpx;
  line-height: 32rpx;
  padding: 0 10rpx;
  background: #ff4d4f;
  color: #fff;
  font-size: 20rpx;
  border-radius: 16rpx;
  text-align: center;
  box-sizing: border-box;
}

.cat-arrow {
  color: #ccc;
  font-size: 36rpx;
  margin-left: 12rpx;
  flex-shrink: 0;
}

/* 私聊列表 */
.chat-section {
  background: #fff;
  margin: 20rpx;
  border-radius: 16rpx;
  overflow: hidden;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.section-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #333;
}

.section-badge {
  font-size: 22rpx;
  color: #ff4d4f;
}

.chat-list {
  padding: 0;
}

.chat-item {
  display: flex;
  align-items: center;
  padding: 24rpx;
  border-bottom: 1rpx solid #f5f5f5;

  &:last-child {
    border-bottom: none;
  }

  &:active {
    background: #f9f9f9;
  }
}

.chat-avatar-wrap {
  position: relative;
  margin-right: 20rpx;
  flex-shrink: 0;
}

.chat-avatar-default {
  width: 88rpx;
  height: 88rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  font-size: 32rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.chat-avatar {
  width: 88rpx;
  height: 88rpx;
  border-radius: 50%;
}

.chat-badge {
  position: absolute;
  top: -8rpx;
  right: -8rpx;
  min-width: 32rpx;
  height: 32rpx;
  line-height: 32rpx;
  padding: 0 8rpx;
  background: #ff4d4f;
  color: #fff;
  font-size: 20rpx;
  border-radius: 16rpx;
  text-align: center;
  box-sizing: border-box;
}

.chat-info {
  flex: 1;
  min-width: 0;
}

.chat-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10rpx;
}

.chat-name {
  font-size: 30rpx;
  font-weight: 500;
  color: #333;
}

.chat-time {
  font-size: 22rpx;
  color: #999;
}

.chat-last {
  font-size: 26rpx;
  color: #999;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: block;
}

.empty-tip {
  text-align: center;
  padding: 80rpx 0;
  color: #bbb;
  font-size: 26rpx;
}

.bottom-space {
  height: 180rpx;
}

/* AI 助手悬浮球：默认半隐藏（一半露出屏幕），点击滑出后进入 */
.ai-fab {
  position: fixed;
  right: -48rpx;
  bottom: 160rpx;
  width: 96rpx;
  height: 96rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #12b7f5, #6c4ee6);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8rpx 24rpx rgba(108, 78, 230, 0.4);
  z-index: 999;
  transition: right 0.3s ease;
}

.ai-fab.is-expanded {
  right: 30rpx;
}

.ai-fab .ai-fab-icon {
  font-size: 44rpx;
  margin-left: 24rpx;
  transition: margin-left 0.3s ease;
}

.ai-fab.is-expanded .ai-fab-icon {
  margin-left: 0;
}
</style>
