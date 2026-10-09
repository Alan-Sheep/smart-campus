<template>
  <!-- 消息详情页 -->
  <view class="detail-page">
    <!-- 自定义导航栏 -->
    <view class="nav-bar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-back" @click="goBack">
        <text class="back-icon">‹</text>
      </view>
      <view class="nav-title">消息详情</view>
      <view class="nav-right"></view>
    </view>

    <scroll-view 
      scroll-y 
      class="scroll-content"
      :style="{ paddingTop: navHeight + 'px' }"
    >
      <view class="detail-card" v-if="detail">
        <view class="detail-header">
          <text class="detail-title">{{ detail.title }}</text>
          <text class="detail-time">{{ formatTime(detail.createTime) }}</text>
        </view>
        <view class="detail-body">
          <text class="detail-content">{{ detail.content }}</text>
        </view>
      </view>

      <view class="loading-tip" v-if="loading">
        <text>加载中...</text>
      </view>

      <view class="empty-tip" v-if="!detail && !loading">
        <text>消息不存在</text>
      </view>
    </scroll-view>
  </view>
</template>

<script setup>
/**
 * 消息详情页
 */
import { ref, onMounted } from 'vue'
import { messageApi } from '@/api/index'

const statusBarHeight = ref(20)
const navHeight = ref(88)
const loading = ref(false)
const detail = ref(null)

onMounted(() => {
  const sysInfo = uni.getSystemInfoSync()
  statusBarHeight.value = sysInfo.statusBarHeight || 20
  navHeight.value = statusBarHeight.value + 44

  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1]
  const options = currentPage.options || {}
  
  if (options.id) {
    loadDetail(options.id)
  }
})

/**
 * 加载消息详情
 */
async function loadDetail(id) {
  loading.value = true
  try {
    const data = await messageApi.getMessageDetail(id)
    detail.value = data
  } catch (err) {
    console.warn('加载消息详情失败:', err)
  } finally {
    loading.value = false
  }
}

/**
 * 返回
 */
function goBack() {
  uni.navigateBack()
}

/**
 * 格式化时间
 */
function formatTime(timeStr) {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  const year = date.getFullYear()
  const month = (date.getMonth() + 1).toString().padStart(2, '0')
  const day = date.getDate().toString().padStart(2, '0')
  const hour = date.getHours().toString().padStart(2, '0')
  const minute = date.getMinutes().toString().padStart(2, '0')
  return year + '-' + month + '-' + day + ' ' + hour + ':' + minute
}
</script>

<style lang="scss" scoped>
.detail-page {
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

.scroll-content {
  height: 100vh;
  box-sizing: border-box;
}

.detail-card {
  background: #fff;
  margin: 20rpx;
  border-radius: 16rpx;
  padding: 32rpx;
}

.detail-header {
  padding-bottom: 24rpx;
  border-bottom: 1rpx solid #f0f0f0;
  margin-bottom: 24rpx;
}

.detail-title {
  font-size: 34rpx;
  font-weight: 600;
  color: #333;
  line-height: 1.4;
  display: block;
  margin-bottom: 12rpx;
}

.detail-time {
  font-size: 24rpx;
  color: #999;
}

.detail-body {
  line-height: 1.8;
}

.detail-content {
  font-size: 28rpx;
  color: #333;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-all;
}

.loading-tip,
.empty-tip {
  text-align: center;
  padding: 200rpx 0;
  color: #bbb;
  font-size: 28rpx;
}
</style>
