<template>
  <!-- 聊天详情页 -->
  <view class="chat-page">
    <!-- 自定义导航栏 -->
    <view class="nav-bar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-back" @click="goBack">
        <text class="back-icon">‹</text>
      </view>
      <view class="nav-title">{{ targetNickname }}</view>
      <view class="nav-right"></view>
    </view>

    <!-- 消息列表区 -->
    <scroll-view 
      scroll-y 
      class="chat-scroll"
      :style="{ paddingTop: navHeight + 'px', paddingBottom: inputBarHeight + 'px' }"
      :scroll-into-view="scrollToId"
      scroll-with-animation
      refresher-enabled
      :refresher-triggered="isRefreshing"
      @refresherrefresh="loadOlder"
    >
      <view class="chat-container">
        <view class="load-more-tip" v-if="loadingOlder">
          <text>加载中...</text>
        </view>
        <view class="load-more-tip" v-else-if="noMore && messageList.length > 0">
          <text>没有更多消息了</text>
        </view>

        <!-- 消息列表（注意：后端返回是倒序，前端显示要正序） -->
        <view 
          v-for="(item, index) in displayList" 
          :key="'msg-' + item.id"
          :id="'msg-' + item.id"
          class="msg-row"
          :class="{ 'is-mine': item.isMine }"
        >
          <!-- 时间显示（每5条或间隔超过5分钟显示一次） -->
          <view v-if="shouldShowTime(index)" class="msg-time">
            <text>{{ formatMsgTime(item.createTime) }}</text>
          </view>

          <!-- 头像 -->
          <view class="avatar-box">
            <view v-if="item.isMine" class="avatar my-avatar">
              <text>{{ myAvatarText }}</text>
            </view>
            <view v-else class="avatar other-avatar">
              <text>{{ otherAvatarText }}</text>
            </view>
          </view>

          <!-- 消息气泡 -->
          <view class="bubble-wrap">
            <view class="bubble" :class="{ 'mine-bubble': item.isMine }">
              <text class="bubble-text">{{ item.content }}</text>
            </view>
          </view>
        </view>

        <!-- 发送中状态 -->
        <view v-if="sending" class="msg-row is-mine" id="msg-sending">
          <view class="avatar-box">
            <view class="avatar my-avatar">
              <text>{{ myAvatarText }}</text>
            </view>
          </view>
          <view class="bubble-wrap">
            <view class="bubble mine-bubble sending-bubble">
              <text class="bubble-text">{{ sendingContent }}</text>
            </view>
          </view>
        </view>

        <view id="msg-bottom"></view>
      </view>
    </scroll-view>

    <!-- 底部输入栏 -->
    <view class="input-bar" :style="{ paddingBottom: safeBottom + 'px' }">
      <input 
        class="msg-input" 
        v-model="inputContent" 
        placeholder="说点什么..." 
        confirm-type="send"
        @confirm="sendMessage"
        @focus="onInputFocus"
      />
      <view 
        class="send-btn" 
        :class="{ 'can-send': inputContent.trim().length > 0 }"
        @click="sendMessage"
      >
        <text>发送</text>
      </view>
    </view>
  </view>
</template>

<script setup>
/**
 * 聊天详情页
 * 支持发送文字消息、接收消息、加载历史消息
 */
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { onShow, onHide } from '@dcloudio/uni-app'
import { messageApi } from '@/api/index'
import { checkLogin } from '@/utils/request'

const statusBarHeight = ref(20)
const navHeight = ref(88)
const safeBottom = ref(0)
const inputBarHeight = ref(120)
const isRefreshing = ref(false)
const loadingOlder = ref(false)
const noMore = ref(false)
const sending = ref(false)
const sendingContent = ref('')
const inputContent = ref('')
const scrollToId = ref('msg-bottom')

const conversationId = ref('')
const targetUserId = ref(null)
const targetNickname = ref('')
const currentUserId = ref(null)
const pageNum = ref(1)
const pageSize = 20

// 消息列表（后端返回是倒序，存储也是倒序，显示时反转）
const messageList = ref([])

// 显示用的正序列表

// 消息写入统一入口：isMine 在写入时按「发送者是否=我」一次算死
// 我=当前登录uid（与 targetUserId 相异的那个）
function normalizeMessage(m) {
  // 与「对方ID」比较：targetUserId 从 URL 参数来，聊天页内恒定可靠
  // （从 storage 读 myId 在个别时机会拿到空值 → NaN → 全部误判为对方靠左）
  const other = Number(targetUserId.value)
  const from = Number(m.fromUserId)
  return { ...m, fromUserId: from, isMine: other ? from !== other : false }
}

const displayList = computed(() => {
  // 消息方向在【写入时】一次性算好存为 isMine（渲染期零计算零比较），
  // 从根源避免任何类型/时序问题导致的左右混乱
  return [...messageList.value].reverse()
})

// 我的头像文字
const myAvatarText = computed(() => {
  // 可以从用户信息里取，这里简单用"我"
  return '我'
})

// 对方头像文字
const otherAvatarText = computed(() => {
  return targetNickname.value ? targetNickname.value.charAt(0) : '用'
})

onMounted(() => {
  const sysInfo = uni.getSystemInfoSync()
  statusBarHeight.value = sysInfo.statusBarHeight || 20
  navHeight.value = statusBarHeight.value + 44
  safeBottom.value = sysInfo.safeAreaInsets?.bottom || 0
  inputBarHeight.value = 110 + safeBottom.value

  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1]
  const options = currentPage.options || {}
  
  conversationId.value = options.conversationId || ''
  targetUserId.value = options.targetUserId ? parseInt(options.targetUserId) : null
  targetNickname.value = decodeURIComponent(options.targetNickname || '聊天')

  // 获取当前用户ID
  const userInfo = uni.getStorageSync('userInfo')
  if (userInfo && userInfo.id) {
    currentUserId.value = Number(userInfo.id)
  } else {
    currentUserId.value = null // 无法获取时保持空，比较逻辑不再依赖它
  }

  // 如果没有 conversationId，根据双方用户ID生成
  if (!conversationId.value && targetUserId.value && currentUserId.value) {
    const a = currentUserId.value
    const b = targetUserId.value
    conversationId.value = a < b ? a + '_' + b : b + '_' + a
  }

  loadMessages()

  // ---- 新消息轮询 ----
  // 修复：B 在聊天界面时，A 发来的消息不会立刻出现，必须切换/刷新页面才能看到。
  // 项目未引入 WebSocket，这里用定时轮询兜底（3 秒一次），
  // 只增量拉取第一页（最新的消息），有新消息才更新列表，开销很小。
  pollTimer = setInterval(pollNewMessages, 3000)
})

/** 轮询定时器 */
let pollTimer = null

/**
 * 增量拉取最新消息：
 * 对比本地最新一条的 id，若后端有更新的消息则合并进列表并滚动到底部
 */
async function pollNewMessages() {
  if (!conversationId.value) return
  try {
    const data = await messageApi.getChatRecords(conversationId.value, {
      pageNum: 1,
      pageSize: 10
    })
    const records = data.records || data.list || []
    if (!records.length) return

    const cur = messageList.value
    if (!cur.length) {
      messageList.value = records.map(normalizeMessage)
      nextTick(() => scrollToBottom())
      return
    }

    // 列表是「新的在前」倒序存储，第一条即最新
    const latestId = cur[0].id
    const fresh = []
    for (const r of records) {
      if (r.id <= latestId) break   // 倒序遍历，遇到已存在的就停
      fresh.push(r)
    }
    if (!fresh.length) return

    // 合并去重后放回列表头部
    const existIds = new Set(cur.map(m => m.id))
    const toAdd = fresh.filter(m => !existIds.has(m.id))
    if (toAdd.length) {
      messageList.value = [...toAdd.map(normalizeMessage), ...cur]
      nextTick(() => scrollToBottom())
    }
  } catch (err) {
    // 轮询失败静默，不打扰用户
  }
}

/** 清理轮询定时器 */
function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

onUnmounted(() => {
  stopPolling()
})

// 页面隐藏/显示时暂停与恢复轮询，避免后台白白请求
onHide(() => stopPolling())
onShow(() => {
  if (!pollTimer && conversationId.value) {
    pollTimer = setInterval(pollNewMessages, 3000)
  }
})

/**
 * 加载消息（第一页）
 */
async function loadMessages() {
  try {
    const data = await messageApi.getChatRecords(conversationId.value, {
      pageNum: 1,
      pageSize: pageSize
    })
    
    const records = data.records || data.list || []
      messageList.value = records.map(normalizeMessage)

    if (records.length < pageSize) {
      noMore.value = true
    } else {
      pageNum.value = 2
    }

    // 滚动到底部
    nextTick(() => {
      scrollToBottom()
    })
  } catch (err) {
    console.warn('加载聊天记录失败:', err)
  }
}

/**
 * 加载更多历史消息（下拉刷新）
 */
async function loadOlder() {
  if (noMore.value || loadingOlder.value) {
    isRefreshing.value = false
    return
  }

  loadingOlder.value = true
  
  try {
    const data = await messageApi.getChatRecords(conversationId.value, {
      pageNum: pageNum.value,
      pageSize: pageSize
    })
    
    const records = data.records || data.list || []
    
    if (records.length > 0) {
      // 历史消息加到列表头部（因为列表是倒序存储的，新的在前面）
      messageList.value = [...messageList.value, ...records.map(normalizeMessage)]
      pageNum.value++
    }
    
    if (records.length < pageSize) {
      noMore.value = true
    }
  } catch (err) {
    console.warn('加载历史消息失败:', err)
  } finally {
    loadingOlder.value = false
    isRefreshing.value = false
  }
}

/**
 * 发送消息
 */
async function sendMessage() {
  const content = inputContent.value.trim()
  if (!content) return
  if (!checkLogin()) return

  // 保存要发送的内容，清空输入框
  sendingContent.value = content
  inputContent.value = ''
  sending.value = true

  // 滚动到底部
  nextTick(() => {
    scrollToBottom()
  })

  try {
    const data = await messageApi.sendMessage({
      toUserId: targetUserId.value,
      conversationId: conversationId.value,
      content: content,
      // 后端 chat_message.msg_type 为 TINYINT（0:文本 1:图片 2:系统），需传数字
      msgType: 0
    })

    // 发送成功，加入消息列表（加到前面，因为列表是倒序的）
    if (data) {
      // fromUserId 归一化 Number：与 currentUserId 比较一致，否则自己发的第一条消息被判为对方（靠左）
      messageList.value = [normalizeMessage(data), ...messageList.value]
    }
  } catch (err) {
    console.warn('发送消息失败:', err)
    uni.showToast({ title: '发送失败', icon: 'none' })
  } finally {
    sending.value = false
    sendingContent.value = ''
    nextTick(() => {
      scrollToBottom()
    })
  }
}

/**
 * 滚动到底部
 */
function scrollToBottom() {
  scrollToId.value = ''
  nextTick(() => {
    scrollToId.value = 'msg-bottom'
  })
}

/**
 * 输入框获得焦点
 */
function onInputFocus() {
  setTimeout(() => {
    scrollToBottom()
  }, 300)
}

/**
 * 是否显示时间（每5条显示一次，或者间隔超过5分钟）
 */
function shouldShowTime(index) {
  const list = displayList.value
  if (index === 0) return true
  
  const current = list[index]
  const prev = list[index - 1]
  
  if (!current.createTime || !prev.createTime) return false
  
  const currTime = new Date(current.createTime).getTime()
  const prevTime = new Date(prev.createTime).getTime()
  
  // 间隔超过5分钟显示时间
  return currTime - prevTime > 5 * 60 * 1000
}

/**
 * 格式化消息时间
 */
function formatMsgTime(timeStr) {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const oneDay = 24 * 60 * 60 * 1000

  const hour = date.getHours().toString().padStart(2, '0')
  const minute = date.getMinutes().toString().padStart(2, '0')
  const timeStr2 = hour + ':' + minute

  if (diff < oneDay) {
    return timeStr2
  }
  
  const yesterday = new Date(now.getTime() - oneDay)
  if (date.toDateString() === yesterday.toDateString()) {
    return '昨天 ' + timeStr2
  }
  
  if (diff < oneDay * 7) {
    const weekDays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
    return weekDays[date.getDay()] + ' ' + timeStr2
  }
  
  const month = (date.getMonth() + 1).toString().padStart(2, '0')
  const day = date.getDate().toString().padStart(2, '0')
  return month + '-' + day + ' ' + timeStr2
}

/**
 * 返回
 */
function goBack() {
  uni.navigateBack()
}
</script>

<style lang="scss" scoped>
.chat-page {
  min-height: 100vh;
  background: #ededed;
}

.nav-bar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  background: #fff;
  border-bottom: 1rpx solid #eee;
  display: flex;
  align-items: center;
}

.nav-back {
  width: 88rpx;
  height: 88rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.back-icon {
  font-size: 48rpx;
  color: #333;
  line-height: 1;
}

.nav-title {
  flex: 1;
  text-align: center;
  font-size: 34rpx;
  font-weight: bold;
  color: #333;
  height: 88rpx;
  line-height: 88rpx;
  padding-right: 88rpx;
}

.nav-right {
  width: 88rpx;
  flex-shrink: 0;
}

.chat-scroll {
  height: 100vh;
  box-sizing: border-box;
}

.chat-container {
  padding: 20rpx 0;
}

.load-more-tip {
  text-align: center;
  padding: 20rpx 0;
  color: #999;
  font-size: 22rpx;
}

.msg-row {
  display: flex;
  align-items: flex-start;
  padding: 12rpx 24rpx;

  &.is-mine {
    flex-direction: row-reverse;

    .bubble {
      background: #95ec69;
      color: #000;
    }

    .bubble-text {
      color: #000;
    }

    .avatar-box {
      margin-left: 20rpx;
      margin-right: 0;
    }
  }
}

.msg-time {
  width: 100%;
  text-align: center;
  padding: 10rpx 0 20rpx;

  text {
    font-size: 22rpx;
    color: #999;
    background: rgba(0, 0, 0, 0.1);
    padding: 6rpx 16rpx;
    border-radius: 8rpx;
    display: inline-block;
  }
}

.avatar-box {
  margin-right: 20rpx;
  flex-shrink: 0;
}

.avatar {
  width: 72rpx;
  height: 72rpx;
  border-radius: 8rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 28rpx;
}

.my-avatar {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.other-avatar {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.bubble-wrap {
  max-width: 70%;
  min-width: 0;
}

.bubble {
  background: #fff;
  border-radius: 12rpx;
  padding: 18rpx 24rpx;
  position: relative;
  word-break: break-all;
  line-height: 1.5;
}

.bubble-text {
  font-size: 30rpx;
  color: #333;
  line-height: 1.5;
}

.sending-bubble {
  opacity: 0.7;
}

/* 底部输入栏 */
.input-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: #f7f7f7;
  border-top: 1rpx solid #e0e0e0;
  padding: 16rpx 20rpx;
  display: flex;
  align-items: center;
  gap: 16rpx;
  z-index: 99;
}

.msg-input {
  flex: 1;
  height: 72rpx;
  background: #fff;
  border-radius: 8rpx;
  padding: 0 24rpx;
  font-size: 28rpx;
  color: #333;
}

.send-btn {
  height: 72rpx;
  padding: 0 32rpx;
  background: #ccc;
  color: #fff;
  font-size: 28rpx;
  border-radius: 8rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;

  &.can-send {
    background: #07c160;
  }

  &:active {
    opacity: 0.8;
  }
}
</style>