<template>
  <view class="ai-page">
    <!-- 自定义导航栏 -->
    <view class="nav-bar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-back" @click="goBack">
        <text class="back-icon">‹</text>
      </view>
      <view class="nav-title">
        <text>🤖 AI 校园助手</text>
      </view>
      <view class="nav-right"></view>
    </view>

    <!-- 消息列表 -->
    <scroll-view
      scroll-y
      class="chat-scroll"
      :style="{ paddingTop: navHeight + 'px' }"
      :scroll-into-view="scrollInto"
      scroll-with-animation
    >
      <!-- 欢迎语 -->
      <view class="welcome" v-if="messages.length === 0">
        <view class="welcome-icon">🤖</view>
        <view class="welcome-title">你好，我是校园助手小智</view>
        <view class="welcome-desc">可以问我快递、跑腿、打印、二手等校园相关问题</view>
        <view class="quick-questions">
          <view
            class="quick-q"
            v-for="(q, i) in quickQuestions"
            :key="i"
            @click="askQuick(q)"
          >{{ q }}</view>
        </view>
      </view>

      <template v-for="(msg, index) in messages" :key="index">
        <!-- 时间：独立居中行 -->
        <view class="msg-time" v-if="msg.time">{{ msg.time }}</view>

        <view class="msg-row" :class="{ 'is-mine': msg.isMine }">
        <!-- 头像 -->
        <view class="avatar-box">
          <view v-if="msg.isMine" class="avatar my-avatar">
            <text>{{ myAvatarText }}</text>
          </view>
          <view v-else class="avatar ai-avatar">
            <text>🤖</text>
          </view>
        </view>

        <!-- 气泡 -->
        <view class="bubble-wrap">
          <view class="bubble" :class="{ 'ai-bubble': !msg.isMine }">
            <text class="bubble-text">{{ msg.content }}<text v-if="msg.typing" class="cursor">▌</text></text>
          </view>
        </view>
        </view>
      </template>

      <view style="height: 30rpx"></view>
    </scroll-view>

    <!-- 输入栏 -->
    <view class="input-bar" :style="{ paddingBottom: safeAreaBottom + 'px' }">
      <input
        class="chat-input"
        v-model="inputContent"
        placeholder="有问题就问小智～"
        confirm-type="send"
        @confirm="sendMessage"
        :disabled="loading"
      />
      <button class="send-btn" :class="{ active: inputContent.trim() && !loading }" @click="sendMessage" :disabled="loading">
        {{ loading ? '…' : '发送' }}
      </button>
    </view>
  </view>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { BASE_URL } from '@/utils/request'

// 状态
const messages = ref([])
const inputContent = ref('')
const loading = ref(false)
const statusBarHeight = ref(20)
const navHeight = ref(64)
const safeAreaBottom = ref(10)
const scrollInto = ref('')
const myAvatarText = ref('我')

// 快捷问题
const quickQuestions = [
  '学校快递怎么取？',
  '打印多少钱一页？',
  '跑腿费怎么算？',
  '二手交易有什么规则？'
]

onMounted && null
import { onMounted } from 'vue'
onMounted(() => {
  const sys = uni.getSystemInfoSync()
  statusBarHeight.value = sys.statusBarHeight || 20
  navHeight.value = (sys.statusBarHeight || 20) + 44
  safeAreaBottom.value = sys.safeAreaInsets ? (sys.safeAreaInsets.bottom || 10) : 10
})

function goBack() {
  uni.navigateBack()
}

function askQuick(q) {
  inputContent.value = q
  sendMessage()
}

function nowTime() {
  const d = new Date()
  const pad = n => String(n).padStart(2, '0')
  return pad(d.getHours()) + ':' + pad(d.getMinutes())
}

function scrollToBottom() {
  nextTick(() => {
    scrollInto.value = 'msg-bottom'
    setTimeout(() => { scrollInto.value = '' }, 100)
  })
}

/**
 * 发送问题：走 SSE 流式接口
 * EventSource 小程序端不支持 → 用 uni.request 的 enableChunked（H5 用 fetch + ReadableStream）
 */
async function sendMessage() {
  const question = inputContent.value.trim()
  if (!question || loading.value) return

  // 登录检查
  const userInfo = uni.getStorageSync('userInfo')
  if (!userInfo || !userInfo.id) {
    uni.showModal({
      title: '提示',
      content: '请先登录后再使用 AI 助手',
      confirmText: '去登录',
      success: (r) => { if (r.confirm) uni.navigateTo({ url: '/pages/login/login' }) }
    })
    return
  }

  // 用户消息入列
  messages.value.push({ isMine: true, content: question, time: nowTime() })
  inputContent.value = ''
  loading.value = true

  // AI 占位消息（流式填充）
  const aiMsg = { isMine: false, content: '', typing: true, time: nowTime() }
  messages.value.push(aiMsg)
  scrollToBottom()

  const currentSchool = uni.getStorageSync('currentSchool')
  const token = uni.getStorageSync('token') || ''
  const base = getCurrentBase()
  const url = base + '/api/ai/chat?question=' + encodeURIComponent(question)

  try {
    // #ifdef H5
    await h5StreamChat(url, token, aiMsg)
    // #endif
    // #ifndef H5
    await mpStreamChat(url, token, currentSchool, aiMsg)
    // #endif
  } catch (e) {
    console.warn('AI 请求异常:', e)
    if (aiMsg.content === '') {
      aiMsg.content = 'AI 助手开小差了，请稍后再试～'
    }
  } finally {
    aiMsg.typing = false
    loading.value = false
    scrollToBottom()
  }
}

/** H5 端：fetch + ReadableStream 逐段读取 */
async function h5StreamChat(url, token, aiMsg) {
  const resp = await fetch(url, {
    headers: { 'Authorization': 'Bearer ' + token }
  })
  if (!resp.ok) {
    throw new Error('HTTP ' + resp.status)
  }
  const reader = resp.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''
  while (true) {
    const { done, value } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })
    // 解析 SSE 事件
    const events = buffer.split('\n\n')
    buffer = events.pop() || ''
    for (const ev of events) {
      const lines = ev.split('\n')
      let eventName = 'message'
      let data = ''
      for (const line of lines) {
        if (line.startsWith('event:')) eventName = line.substring(6).trim()
        if (line.startsWith('data:')) data += line.substring(5)
      }
      if (eventName === 'message' && data) {
        aiMsg.content += data
        scrollToBottom()
      } else if (eventName === 'error') {
        aiMsg.content = data || 'AI 助手开小差了'
        return
      } else if (eventName === 'done') {
        aiMsg.content = data.replace(/\\n/g, '\n') // 用完整文本覆盖（保证一致性）
        return
      }
    }
  }
}

/** 小程序端：uni.request enableChunked（微信基础库 2.20+ 支持） */
function mpStreamChat(url, token, currentSchool, aiMsg) {
  return new Promise((resolve, reject) => {
    const task = uni.request({
      url,
      method: 'GET',
      header: {
        'Authorization': 'Bearer ' + token,
        'School-Id': currentSchool ? (currentSchool.id || 1) : 1
      },
      enableChunked: true,
      success: () => resolve(),
      fail: (err) => reject(err)
    })
    task.onChunkReceived((res) => {
      // 分块解析 SSE
      const text = arrayBufferToString(res.data)
      const events = text.split('\n\n')
      for (const ev of events) {
        const lines = ev.split('\n')
        let eventName = 'message'
        let data = ''
        for (const line of lines) {
          if (line.startsWith('event:')) eventName = line.substring(6).trim()
          if (line.startsWith('data:')) data += line.substring(5)
        }
        if (eventName === 'message' && data) {
          aiMsg.content += data
          scrollToBottom()
        } else if (eventName === 'error') {
          aiMsg.content = data || 'AI 助手开小差了'
        } else if (eventName === 'done') {
        aiMsg.content = data.replace(/\\n/g, '\n')
        }
      }
      resolve()
    })
  })
}

/** ArrayBuffer 转 UTF-8 字符串 */
function arrayBufferToString(buffer) {
  // #ifdef H5
  return new TextDecoder('utf-8').decode(buffer)
  // #endif
  // #ifndef H5
  try {
    const uint8 = new Uint8Array(buffer)
    let str = ''
    let i = 0
    while (i < uint8.length) {
      const b = uint8[i]
      if (b < 0x80) { str += String.fromCharCode(b); i++ }
      else if (b < 0xE0) { str += String.fromCharCode(((b & 0x1F) << 6) | (uint8[i+1] & 0x3F)); i += 2 }
      else { str += String.fromCharCode(((b & 0x0F) << 12) | ((uint8[i+1] & 0x3F) << 6) | (uint8[i+2] & 0x3F)); i += 3 }
    }
    return decodeURIComponent(escape(str))
  } catch (e) {
    return String(buffer)
  }
  // #endif
}

/** 当前 API 基地址（与 request.js 同逻辑） */
function getCurrentBase() {
  return BASE_URL || ''
}
</script>

<style lang="scss" scoped>
.ai-page {
  height: 100vh;
  background: #f5f5f7;
  display: flex;
  flex-direction: column;
}

.nav-bar {
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 16rpx;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;

  .nav-back {
    width: 88rpx;
    text-align: center;

    .back-icon {
      font-size: 48rpx;
      color: #333;
    }
  }

  .nav-title {
    font-size: 32rpx;
    font-weight: 600;
    color: #333;
  }

  .nav-right {
    width: 88rpx;
  }
}

.chat-scroll {
  flex: 1;
  overflow: hidden;
}

.welcome {
  text-align: center;
  padding: 100rpx 40rpx 60rpx;

  .welcome-icon {
    font-size: 100rpx;
    margin-bottom: 24rpx;
  }

  .welcome-title {
    font-size: 34rpx;
    font-weight: 600;
    color: #333;
    margin-bottom: 12rpx;
  }

  .welcome-desc {
    font-size: 26rpx;
    color: #999;
    margin-bottom: 48rpx;
  }

  .quick-questions {
    display: flex;
    flex-direction: column;
    gap: 16rpx;
    align-items: center;

    .quick-q {
      padding: 16rpx 36rpx;
      background: #fff;
      border-radius: 32rpx;
      font-size: 26rpx;
      color: #6c4ee6;
      border: 1rpx solid #e0d8ff;
    }
  }
}

.msg-row {
  display: flex;
  align-items: flex-start;
  padding: 12rpx 24rpx;

  &.is-mine {
    flex-direction: row-reverse;

    .bubble-wrap {
      display: flex;
      flex-direction: row-reverse;
    }

    .avatar-box {
      margin-left: 20rpx;
      margin-right: 0;
    }

    .bubble {
      background: #95ec69;
      color: #000;
    }
  }

  .avatar-box {
    margin-right: 20rpx;

    .avatar {
      width: 72rpx;
      height: 72rpx;
      border-radius: 12rpx;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 28rpx;

      &.my-avatar {
        background: linear-gradient(135deg, #6c4ee6, #8b6cf0);
        color: #fff;
      }

      &.ai-avatar {
        background: linear-gradient(135deg, #12b7f5, #0a8fd8);
        font-size: 40rpx;
      }
    }
  }

  .bubble-wrap {
    max-width: 70%;

    .bubble {
      background: #fff;
      border-radius: 16rpx;
      padding: 20rpx 24rpx;
      margin-top: 8rpx;

      &.ai-bubble {
        background: #fff;
      }

      .bubble-text {
        font-size: 28rpx;
        line-height: 1.6;
        word-break: break-all;
        white-space: pre-wrap;

        .cursor {
          animation: blink 1s infinite;
          color: #6c4ee6;
        }
      }
    }
  }
}

@keyframes blink {
  50% { opacity: 0; }
}

.msg-time {
  width: 100%;
  text-align: center;
  padding: 10rpx 0 20rpx;

  text {
    font-size: 22rpx;
    color: #b0b0b0;
    background: #e8e8e8;
    padding: 6rpx 16rpx;
    border-radius: 8rpx;
  }
}

.input-bar {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 20rpx 24rpx;
  background: #fff;
  border-top: 1rpx solid #eee;

  .chat-input {
    flex: 1;
    height: 72rpx;
    background: #f5f5f7;
    border-radius: 36rpx;
    padding: 0 32rpx;
    font-size: 28rpx;
  }

  .send-btn {
    width: 140rpx;
    height: 72rpx;
    line-height: 72rpx;
    text-align: center;
    background: #c5b8f5;
    color: #fff;
    font-size: 28rpx;
    border-radius: 36rpx;
    padding: 0;

    &.active {
      background: #6c4ee6;
    }
  }
}
</style>
