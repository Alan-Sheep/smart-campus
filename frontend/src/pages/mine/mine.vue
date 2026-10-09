<template>
  <!-- 我的页面 -->
  <view class="mine-page">
    <!-- 顶部用户信息区域 -->
    <view class="user-header" :style="{ paddingTop: statusBarHeight + 'px' }">
      <!-- 学校选择 -->
      <view class="school-bar">
        <view class="school-btn" @click="goToSchool">
          <text class="school-icon">🏫</text>
          <text class="school-name">{{ currentSchool.name }}</text>
          <text class="arrow">▼</text>
        </view>
        <view class="setting-btn" @click="goToSetting">
          <text>⚙️</text>
        </view>
      </view>

      <!-- 用户信息卡片 -->
      <view class="user-card" @click="handleUserClick">
        <image 
          :src="userStore.userInfo?.avatar || defaultAvatar" 
          class="user-avatar"
        ></image>
        <view class="user-info">
          <text class="nickname">{{ userStore.isLogin ? userStore.nickname : '点击登录' }}</text>
          <text class="user-desc">{{ userStore.isLogin ? '校园ID: ' + userStore.userInfo.id : '登录后体验更多功能' }}</text>
        </view>
        <text class="arrow-right">›</text>
      </view>
    </view>

    <!-- 功能菜单区域 -->
    <scroll-view scroll-y class="scroll-content">
      <!-- 我的订单 -->
      <view class="menu-section">
        <view class="section-title">我的订单</view>
        <view class="order-grid">
          <view class="order-item" @click="goToOrder('express')">
            <text class="order-icon">📦</text>
            <text class="order-text">快递代取</text>
          </view>
          <view class="order-item" @click="goToOrder('print')">
            <text class="order-icon">🖨️</text>
            <text class="order-text">打印服务</text>
          </view>
          <view class="order-item" @click="goToOrder('errand')">
            <text class="order-icon">🏃</text>
            <text class="order-text">校园跑腿</text>
          </view>
          <view class="order-item" @click="goToOrder('substitute')">
            <text class="order-icon">📚</text>
            <text class="order-text">代课服务</text>
          </view>
          <view class="order-item" @click="goToOrder('activity')">
            <text class="order-icon">🎉</text>
            <text class="order-text">我的活动</text>
          </view>
          <view class="order-item" @click="goToOrder('goods')">
            <text class="order-icon">🛒</text>
            <text class="order-text">我的闲置</text>
          </view>
          <view class="order-item" @click="goToOrder('game')">
            <text class="order-icon">🎮</text>
            <text class="order-text">游戏搭子</text>
          </view>
          <view class="order-item" @click="goToOrder('lostfound')">
            <text class="order-icon">🔍</text>
            <text class="order-text">失物招领</text>
          </view>
        </view>
      </view>

      <!-- 其他功能 -->
      <view class="menu-section">
        <view class="menu-list">
          <view class="menu-item" @click="goToMyPosts">
            <text class="menu-icon">📝</text>
            <text class="menu-text">我的帖子</text>
            <text class="menu-arrow">›</text>
          </view>
          <view class="menu-item" @click="goToMyCollection">
            <text class="menu-icon">⭐</text>
            <text class="menu-text">我的收藏</text>
            <text class="menu-arrow">›</text>
          </view>
          <view class="menu-item" @click="goToAddress">
            <text class="menu-icon">📍</text>
            <text class="menu-text">地址管理</text>
            <text class="menu-arrow">›</text>
          </view>
        </view>
      </view>

      <!-- 设置相关 -->
      <view class="menu-section">
        <view class="menu-list">
          <view class="menu-item" @click="goToSetting">
            <text class="menu-icon">⚙️</text>
            <text class="menu-text">设置</text>
            <text class="menu-arrow">›</text>
          </view>
          <view class="menu-item" @click="goToFeedback">
            <text class="menu-icon">💬</text>
            <text class="menu-text">意见反馈</text>
            <text class="menu-arrow">›</text>
          </view>
          <view class="menu-item" @click="goToAbout">
            <text class="menu-icon">ℹ️</text>
            <text class="menu-text">关于我们</text>
            <text class="menu-arrow">›</text>
          </view>
        </view>
      </view>

      <!-- 退出登录 -->
      <view class="menu-section" v-if="userStore.isLogin">
        <view class="logout-btn" @click="handleLogout">
          退出登录
        </view>
      </view>

      <view style="height: 60rpx;"></view>
    </scroll-view>
  </view>
</template>

<script setup>
/**
 * 我的页面
 * 用户中心，展示用户信息和各种功能入口
 * 已登录时自动获取最新用户信息
 */
import { ref, computed, onMounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/store/user'
import { useSchoolStore } from '@/store/school'
import { userApi } from '@/api/index'

const userStore = useUserStore()
const schoolStore = useSchoolStore()
const currentSchool = computed(() => schoolStore.currentSchool)

const statusBarHeight = ref(20)
// 默认头像
const defaultAvatar = 'https://picsum.photos/200/200?random=99'

onMounted(() => {
  const sysInfo = uni.getSystemInfoSync()
  statusBarHeight.value = sysInfo.statusBarHeight || 20
})

// 每次页面显示时刷新用户信息
onShow(() => {
  if (userStore.isLogin) {
    loadUserInfo()
  }
})

/**
 * 获取用户信息
 * 已登录时调用接口获取最新用户信息
 */
async function loadUserInfo() {
  try {
    const userInfo = await userApi.getUserInfo()
    if (userInfo) {
      userStore.setUserInfo(userInfo)
    }
  } catch (err) {
    // 获取失败不影响页面展示，使用本地缓存的信息
    console.warn('获取用户信息失败:', err)
  }
}

/**
 * 点击用户区域
 */
function handleUserClick() {
  if (!userStore.isLogin) {
    uni.navigateTo({ url: '/pages/login/login' })
  } else {
    uni.navigateTo({ url: '/pages/mine/profile' })
  }
}

/**
 * 跳转到学校选择
 */
function goToSchool() {
  uni.navigateTo({ url: '/pages/school/school' })
}

/**
 * 跳转到订单列表
 */
function goToOrder(type) {
  if (!userStore.isLogin) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    return
  }
  uni.navigateTo({ url: `/pages/mine/my-list?type=${type}` })
}

function goToMyPosts() {
  if (!userStore.isLogin) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    return
  }
  uni.navigateTo({ url: '/pages/mine/my-list?type=post' })
}

function goToMyCollection() {
  if (!userStore.isLogin) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    return
  }
  uni.navigateTo({ url: '/pages/mine/collection' })
}

function goToAddress() {
  if (!userStore.isLogin) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    return
  }
  uni.navigateTo({ url: '/pages/mine/address' })
}

function goToSetting() {
  uni.navigateTo({ url: '/pages/mine/setting' })
}

function goToFeedback() {
  uni.navigateTo({ url: '/pages/mine/feedback' })
}

function goToAbout() {
  uni.navigateTo({ url: '/pages/mine/about' })
}

/**
 * 退出登录
 */
function handleLogout() {
  uni.showModal({
    title: '提示',
    content: '确定要退出登录吗？',
    success: async (res) => {
      if (res.confirm) {
        try {
          await userApi.logout()
        } catch (e) {
          // 退出登录接口失败不影响本地退出
        }
        userStore.logout()
        uni.showToast({ title: '已退出登录', icon: 'success' })
      }
    }
  })
}
</script>

<style lang="scss" scoped>
.mine-page {
  min-height: 100vh;
  background: #f5f5f7;
}

/* 顶部用户区域 */
.user-header {
  background: linear-gradient(135deg, #6c4ee6, #8b75f0);
  padding-bottom: 40rpx;
}

.school-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20rpx 24rpx;
}

.school-btn {
  display: flex;
  align-items: center;
  gap: 8rpx;
  padding: 10rpx 20rpx;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 30rpx;
}

.school-icon {
  font-size: 28rpx;
}

.school-name {
  color: #fff;
  font-size: 26rpx;
}

.arrow {
  color: #fff;
  font-size: 18rpx;
}

.setting-btn {
  padding: 10rpx;
  font-size: 32rpx;
}

/* 用户卡片 */
.user-card {
  display: flex;
  align-items: center;
  margin: 20rpx 24rpx 0;
  padding: 30rpx;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 20rpx;
  gap: 24rpx;
  backdrop-filter: blur(10px);
}

.user-avatar {
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  border: 4rpx solid rgba(255, 255, 255, 0.5);
}

.user-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 10rpx;
}

.nickname {
  font-size: 34rpx;
  font-weight: bold;
  color: #fff;
}

.user-desc {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.8);
}

.arrow-right {
  color: rgba(255, 255, 255, 0.7);
  font-size: 40rpx;
}

/* 滚动内容 */
.scroll-content {
  height: calc(100vh - 320rpx);
  padding-top: 20rpx;
  box-sizing: border-box;
}

/* 菜单区块 */
.menu-section {
  background: #fff;
  margin: 20rpx 24rpx;
  border-radius: 20rpx;
  padding: 24rpx;
}

.section-title {
  font-size: 28rpx;
  font-weight: bold;
  color: #333;
  margin-bottom: 24rpx;
}

/* 订单网格 */
.order-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 30rpx 0;
}

.order-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12rpx;
}

.order-icon {
  font-size: 40rpx;
}

.order-text {
  font-size: 22rpx;
  color: #666;
}

/* 菜单列表 */
.menu-list {
  margin: 0 -24rpx;
}

.menu-item {
  display: flex;
  align-items: center;
  padding: 24rpx;
  gap: 20rpx;
  border-bottom: 1rpx solid #f5f5f7;

  &:last-child {
    border-bottom: none;
  }
}

.menu-icon {
  font-size: 36rpx;
}

.menu-text {
  flex: 1;
  font-size: 28rpx;
  color: #333;
}

.menu-arrow {
  font-size: 32rpx;
  color: #ccc;
}

/* 退出登录按钮 */
.logout-btn {
  text-align: center;
  padding: 24rpx;
  color: #ff4d4f;
  font-size: 30rpx;
}
</style>
