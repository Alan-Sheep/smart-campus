<template>
  <!-- 消息列表页（系统通知 / 订单消息 / 互动消息） -->
  <view class="msg-list-page">
    <!-- 自定义导航栏 -->
    <view class="nav-bar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-back" @click="goBack">
        <text class="back-icon">‹</text>
      </view>
      <view class="nav-title">{{ pageTitle }}</view>
      <view class="nav-right" @click="markAllRead">
        <text class="read-all">全部已读</text>
      </view>
    </view>

    <scroll-view 
      scroll-y 
      class="scroll-content"
      :style="{ paddingTop: navHeight + 'px' }"
      refresher-enabled
      :refresher-triggered="isRefreshing"
      @refresherrefresh="onRefresh"
      @scrolltolower="loadMore"
    >
      <view class="msg-list">
        <view 
          class="msg-item" 
          v-for="item in list" 
          :key="item.id"
          @click="goToDetail(item)"
        >
          <view class="msg-icon-wrap">
            <text class="msg-icon">{{ getIcon() }}</text>
          </view>
          <view class="msg-content">
            <view class="msg-top">
              <text class="msg-title">{{ item.title }}</text>
              <text class="msg-time">{{ formatTime(item.createTime) }}</text>
            </view>
            <text class="msg-desc">{{ item.content }}</text>
          </view>
          <view v-if="item.isRead === 0" class="unread-dot"></view>
        </view>
      </view>

      <view class="empty-tip" v-if="list.length === 0 && !loading">
        <text>暂无消息</text>
      </view>

      <view class="load-more" v-if="list.length > 0">
        <text v-if="loading">加载中...</text>
        <text v-else-if="noMore">没有更多了</text>
        <text v-else>上拉加载更多</text>
      </view>

      <view class="bottom-space"></view>
    </scroll-view>
  </view>
</template>

<script setup>
/**
 * 消息列表页
 * type: 0-系统通知, 1-订单消息, 2-互动消息
 */
import { ref, onMounted } from 'vue'
import { messageApi } from '@/api/index'

const statusBarHeight = ref(20)
const navHeight = ref(88)
const isRefreshing = ref(false)
const loading = ref(false)
const noMore = ref(false)
const pageNum = ref(1)
const pageSize = 20
const list = ref([])
const pageTitle = ref('消息')
const msgType = ref(0)

onMounted(() => {
  const sysInfo = uni.getSystemInfoSync()
  statusBarHeight.value = sysInfo.statusBarHeight || 20
  navHeight.value = statusBarHeight.value + 44

  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1]
  const options = currentPage.options || {}
  
  msgType.value = parseInt(options.type) || 0
  pageTitle.value = decodeURIComponent(options.title || '消息')

  loadList()
})

/**
 * 加载消息列表
 */
async function loadList(isRefresh = false) {
  if (loading.value) return
  loading.value = true

  if (isRefresh) {
    pageNum.value = 1
    noMore.value = false
  }

  try {
    const data = await messageApi.getMessageList({ 
      pageNum: pageNum.value, 
      pageSize: pageSize,
      type: msgType.value 
    })
    
    const records = data.records || data.list || []
    
    if (isRefresh) {
      list.value = records
    } else {
      list.value = [...list.value, ...records]
    }

    if (records.length < pageSize) {
      noMore.value = true
    } else {
      pageNum.value++
    }
  } catch (err) {
    console.warn('加载消息列表失败:', err)
    uni.showToast({ title: '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

/**
 * 下拉刷新
 */
function onRefresh() {
  isRefreshing.value = true
  loadList(true).finally(() => {
    isRefreshing.value = false
  })
}

/**
 * 加载更多
 */
function loadMore() {
  if (!noMore.value && !loading.value) {
    loadList()
  }
}

/**
 * 全部标记已读
 */
async function markAllRead() {
  try {
    await messageApi.markAllAsRead(msgType.value)
    list.value.forEach(item => {
      item.isRead = 1
    })
    uni.showToast({ title: '已全部标记已读', icon: 'none' })
  } catch (err) {
    console.warn('标记已读失败:', err)
  }
}

/**
 * 跳转到消息详情
 */
function goToDetail(item) {
  uni.navigateTo({
    url: '/pages/message/detail?id=' + item.id
  })
}

/**
 * 返回
 */
function goBack() {
  uni.navigateBack()
}

/**
 * 获取图标
 */
function getIcon() {
  const icons = ['📢', '📦', '💬']
  return icons[msgType.value] || '📨'
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
</script>

<style lang="scss" scoped>
.msg-list-page {
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
  position: absolute;
  right: 24rpx;
  top: 50%;
  transform: translateY(-50%);
  padding: 12rpx 20rpx;
}

.read-all {
  font-size: 26rpx;
  color: #667eea;
}

.scroll-content {
  height: 100vh;
  box-sizing: border-box;
}

.msg-list {
  background: #fff;
  margin: 20rpx;
  border-radius: 16rpx;
  overflow: hidden;
}

.msg-item {
  display: flex;
  align-items: flex-start;
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

.msg-icon-wrap {
  width: 72rpx;
  height: 72rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 20rpx;
  flex-shrink: 0;
}

.msg-icon {
  font-size: 32rpx;
}

.msg-content {
  flex: 1;
  min-width: 0;
}

.msg-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10rpx;
}

.msg-title {
  font-size: 30rpx;
  font-weight: 500;
  color: #333;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-right: 20rpx;
}

.msg-time {
  font-size: 22rpx;
  color: #999;
  flex-shrink: 0;
}

.msg-desc {
  font-size: 26rpx;
  color: #666;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.unread-dot {
  position: absolute;
  right: 24rpx;
  top: 50%;
  transform: translateY(-50%);
  width: 16rpx;
  height: 16rpx;
  background: #ff4d4f;
  border-radius: 50%;
}

.empty-tip {
  text-align: center;
  padding: 200rpx 0;
  color: #bbb;
  font-size: 28rpx;
}

.load-more {
  text-align: center;
  padding: 30rpx 0;
  color: #999;
  font-size: 24rpx;
}

.bottom-space {
  height: 60rpx;
}
</style>
