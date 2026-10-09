<template>
  <!-- 游戏搭子详情页 -->
  <view class="detail-page">
    <!-- 自定义导航栏 -->
    <view class="nav-bar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-back" @click="goBack">
        <text class="back-icon">‹</text>
      </view>
      <view class="nav-title">找搭子详情</view>
      <view class="nav-right"></view>
    </view>

    <!-- 加载状态 -->
    <view class="loading-wrap" v-if="loading" :style="{ paddingTop: navHeight + 'px' }">
      <text class="loading-text">加载中...</text>
    </view>

    <!-- 空状态 -->
    <view class="empty-wrap" v-else-if="!detail" :style="{ paddingTop: navHeight + 'px' }">
      <text class="empty-text">暂无帖子信息</text>
    </view>

    <!-- 详情内容 -->
    <view class="detail-content" v-else :style="{ paddingTop: navHeight + 'px' }">
      <!-- 顶部横幅区 -->
      <view class="top-banner">
        <view class="banner-icon">🎮</view>
        <view class="banner-status" :class="'status-' + detail.status">
          {{ statusMap[detail.status] || '招募中' }}
        </view>
        <view class="banner-game">
          <text class="game-name">{{ detail.gameName || detail.gameType || '游戏搭子' }}</text>
        </view>
      </view>

      <!-- 帖子信息 -->
      <view class="info-card">
        <view class="card-title">{{ detail.title || '找个游戏搭子' }}</view>
        <view class="info-row">
          <text class="info-label">游戏名称</text>
          <text class="info-value">{{ detail.gameName || detail.gameType || '-' }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">段位/等级</text>
          <text class="info-value">{{ detail.level || detail.gameLevel || '不限' }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">联系方式</text>
          <text class="info-value">{{ detail.contact || detail.contactWay || '-' }}</text>
        </view>
      </view>

      <!-- 描述 -->
      <view class="desc-card" v-if="detail.content || detail.description">
        <view class="card-title">详细描述</view>
        <text class="desc-text">{{ detail.content || detail.description }}</text>
      </view>

      <!-- 发布人信息 -->
      <view class="publisher-card" @click="contactPublisher">
        <view class="publisher-avatar">👤</view>
        <view class="publisher-info">
          <text class="publisher-name">{{ detail.publisher || detail.publisherName || '校园玩家' }}</text>
          <text class="publisher-time">发布于 {{ detail.createTime || '' }}</text>
        </view>
        <text class="arrow">›</text>
      </view>

      <view class="bottom-space"></view>
    </view>

    <!-- 底部操作栏 -->
    <view class="bottom-bar" v-if="detail">
      <view class="bar-item" @click="contactPublisher">
        <text class="bar-icon">💬</text>
        <text class="bar-text">联系TA</text>
      </view>
      <view class="team-btn" @click="teamUp">
        组队
      </view>
    </view>
  </view>
</template>

<script setup>
/**
 * 游戏搭子详情页
 */
import { ref, onMounted } from 'vue'
import { gameApi } from '@/api/index'

const statusBarHeight = ref(20)
const navHeight = ref(88)
const loading = ref(false)
const detail = ref(null)
const postId = ref(null)

// 状态映射：0=招募中, 1=已组队
const statusMap = {
  0: '招募中',
  1: '已组队'
}

onMounted(() => {
  const sysInfo = uni.getSystemInfoSync()
  statusBarHeight.value = sysInfo.statusBarHeight || 20
  navHeight.value = statusBarHeight.value + 44

  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1]
  const id = currentPage.options?.id
  postId.value = id

  if (id) {
    loadDetail(id)
  } else {
    uni.showToast({ title: '参数错误', icon: 'none' })
  }
})

/**
 * 加载详情
 */
async function loadDetail(id) {
  loading.value = true
  try {
    const data = await gameApi.getGameDetail(id)
    detail.value = data
  } catch (err) {
    console.warn('游戏搭子详情加载失败:', err)
    detail.value = null
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
 * 联系TA
 */
function contactPublisher() {
  if (!detail.value) return
  const targetUserId = detail.value.userId
  if (targetUserId) {
    uni.navigateTo({
      url: '/pages/message/chat?targetUserId=' + targetUserId + '&targetNickname=' + encodeURIComponent('发布人')
    })
  } else {
    uni.showToast({ title: '无法获取发布人信息', icon: 'none' })
  }
}

/**
 * 组队
 */
function teamUp() {
  if (!detail.value) return
  const targetUserId = detail.value.userId
  if (targetUserId) {
    uni.navigateTo({
      url: '/pages/message/chat?targetUserId=' + targetUserId + '&targetNickname=' + encodeURIComponent('发布人')
    })
  } else {
    uni.showToast({ title: '无法获取发布人信息', icon: 'none' })
  }
}
</script>

<style lang="scss" scoped>
$primary-color: #6c4ee6;

.detail-page {
  min-height: 100vh;
  background: #f5f5f7;
}

/* 自定义导航栏 */
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

/* 加载/空状态 */
.loading-wrap,
.empty-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
}

.loading-text,
.empty-text {
  font-size: 28rpx;
  color: #999;
}

/* 详情内容 */
.detail-content {
  padding-bottom: 180rpx;
}

/* 顶部横幅 */
.top-banner {
  background: linear-gradient(135deg, $primary-color 0%, #8b6ff0 100%);
  padding: 40rpx 32rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20rpx;
}

.banner-icon {
  font-size: 80rpx;
}

.banner-status {
  font-size: 30rpx;
  color: #fff;
  padding: 8rpx 28rpx;
  border-radius: 30rpx;
  background: rgba(255, 255, 255, 0.25);

  &.status-0 {
    background: rgba(82, 196, 26, 0.9);
    color: #fff;
  }
  &.status-1 {
    background: rgba(153, 153, 153, 0.9);
    color: #fff;
  }
}

.banner-game {
  margin-top: 8rpx;
}

.game-name {
  font-size: 36rpx;
  font-weight: bold;
  color: #fff;
}

/* 信息卡片 */
.info-card {
  background: #fff;
  margin: 20rpx;
  border-radius: 16rpx;
  padding: 28rpx 24rpx;
}

.card-title {
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
  margin-bottom: 24rpx;
  padding-bottom: 16rpx;
  border-bottom: 1rpx solid #f0f0f0;
  line-height: 1.5;
}

.info-row {
  display: flex;
  padding: 16rpx 0;

  &:last-child {
    padding-bottom: 0;
  }
}

.info-label {
  width: 160rpx;
  flex-shrink: 0;
  font-size: 28rpx;
  color: #999;
}

.info-value {
  flex: 1;
  font-size: 28rpx;
  color: #333;
  line-height: 1.5;
}

/* 描述卡片 */
.desc-card {
  background: #fff;
  margin: 20rpx;
  border-radius: 16rpx;
  padding: 28rpx 24rpx;
}

.desc-text {
  font-size: 28rpx;
  color: #666;
  line-height: 1.8;
}

/* 发布人卡片 */
.publisher-card {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 28rpx 24rpx;
  background: #fff;
  margin: 0 20rpx;
  border-radius: 16rpx;
}

.publisher-avatar {
  width: 80rpx;
  height: 80rpx;
  line-height: 80rpx;
  text-align: center;
  background: linear-gradient(135deg, $primary-color, #8b6ff0);
  border-radius: 50%;
  font-size: 40rpx;
}

.publisher-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.publisher-name {
  font-size: 30rpx;
  font-weight: 500;
  color: #333;
}

.publisher-time {
  font-size: 24rpx;
  color: #999;
}

.arrow {
  font-size: 36rpx;
  color: #ccc;
}

.bottom-space {
  height: 40rpx;
}

/* 底部操作栏 */
.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  padding: 16rpx 24rpx;
  padding-bottom: calc(16rpx + env(safe-area-inset-bottom));
  background: #fff;
  border-top: 1rpx solid #eee;
  z-index: 100;
  gap: 30rpx;
}

.bar-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4rpx;
}

.bar-icon {
  font-size: 36rpx;
}

.bar-text {
  font-size: 20rpx;
  color: #666;
}

.team-btn {
  flex: 1;
  height: 88rpx;
  line-height: 88rpx;
  text-align: center;
  background: linear-gradient(135deg, $primary-color, #8b6ff0);
  color: #fff;
  font-size: 30rpx;
  font-weight: bold;
  border-radius: 44rpx;
}
</style>
