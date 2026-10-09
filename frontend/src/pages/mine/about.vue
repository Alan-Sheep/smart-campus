<template>
  <view class="about-page">
    <!-- Logo 区域 -->
    <view class="logo-section">
      <view class="logo">🏫</view>
      <text class="app-name">校园帮手</text>
      <text class="app-version">v1.0.0</text>
    </view>

    <!-- 应用介绍 -->
    <view class="info-section">
      <view class="info-card">
        <text class="info-title">关于校园帮手</text>
        <text class="info-desc">
          校园帮手是一款面向大学生的一站式校园生活服务小程序，
          涵盖快递代取、校园跑腿、代课服务、二手市场、校园活动、
          失物招领、游戏搭子等多种实用功能，让校园生活更便捷、更精彩。
        </text>
      </view>
    </view>

    <!-- 功能列表 -->
    <view class="feature-section">
      <view class="section-title">我们提供</view>
      <view class="feature-grid">
        <view class="feature-item" v-for="item in features" :key="item.name">
          <text class="feature-icon">{{ item.icon }}</text>
          <text class="feature-name">{{ item.name }}</text>
        </view>
      </view>
    </view>

    <!-- 联系我们 -->
    <view class="contact-section">
      <view class="section-title">联系我们</view>
      <view class="contact-list">
        <view class="contact-item">
          <text class="contact-icon">📧</text>
          <view class="contact-info">
            <text class="contact-label">邮箱</text>
            <text class="contact-value">support@campus.com</text>
          </view>
        </view>
        <view class="contact-item">
          <text class="contact-icon">💬</text>
          <view class="contact-info">
            <text class="contact-label">客服微信</text>
            <text class="contact-value">CampusHelper</text>
          </view>
        </view>
        <view class="contact-item">
          <text class="contact-icon">🏢</text>
          <view class="contact-info">
            <text class="contact-label">公司地址</text>
            <text class="contact-value">北京市海淀区中关村科技园</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 版权信息 -->
    <view class="copyright">
      <text>© 2024 校园帮手 All Rights Reserved</text>
      <text class="icp">京ICP备xxxxxxxx号</text>
    </view>
  </view>
</template>

<script setup>
const features = [
  { icon: '📦', name: '快递代取' },
  { icon: '🏃', name: '校园跑腿' },
  { icon: '📚', name: '代课服务' },
  { icon: '🛒', name: '二手市场' },
  { icon: '🎉', name: '校园活动' },
  { icon: '🔍', name: '失物招领' },
  { icon: '🎮', name: '游戏搭子' },
  { icon: '📝', name: '校园频道' }
]
</script>

<style lang="scss" scoped>
.about-page {
  min-height: 100vh;
  background: #f5f5f7;
  padding-bottom: 60rpx;
}

/* Logo 区域 */
.logo-section {
  background: linear-gradient(135deg, #6c4ee6, #8b5cf6);
  padding: 80rpx 0 60rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.logo {
  width: 160rpx;
  height: 160rpx;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 40rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 80rpx;
  margin-bottom: 24rpx;
}

.app-name {
  font-size: 40rpx;
  font-weight: bold;
  color: #fff;
  margin-bottom: 8rpx;
}

.app-version {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.7);
}

/* 介绍卡片 */
.info-section {
  padding: 24rpx;
  margin-top: -30rpx;
}

.info-card {
  background: #fff;
  border-radius: 20rpx;
  padding: 32rpx;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.05);
}

.info-title {
  display: block;
  font-size: 30rpx;
  font-weight: bold;
  color: #333;
  margin-bottom: 16rpx;
}

.info-desc {
  font-size: 26rpx;
  color: #666;
  line-height: 1.8;
}

/* 功能列表 */
.feature-section {
  padding: 0 24rpx;
  margin-top: 20rpx;
}

.section-title {
  font-size: 28rpx;
  font-weight: bold;
  color: #333;
  margin-bottom: 20rpx;
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 30rpx 0;
  background: #fff;
  border-radius: 20rpx;
  padding: 30rpx 0;
}

.feature-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10rpx;
}

.feature-icon {
  font-size: 44rpx;
}

.feature-name {
  font-size: 22rpx;
  color: #666;
}

/* 联系我们 */
.contact-section {
  padding: 0 24rpx;
  margin-top: 30rpx;
}

.contact-list {
  background: #fff;
  border-radius: 20rpx;
  overflow: hidden;
}

.contact-item {
  display: flex;
  align-items: center;
  padding: 28rpx 24rpx;
  gap: 20rpx;
  border-bottom: 1rpx solid #f5f5f7;

  &:last-child {
    border-bottom: none;
  }
}

.contact-icon {
  font-size: 40rpx;
}

.contact-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6rpx;
}

.contact-label {
  font-size: 24rpx;
  color: #999;
}

.contact-value {
  font-size: 28rpx;
  color: #333;
}

/* 版权 */
.copyright {
  text-align: center;
  margin-top: 60rpx;
  display: flex;
  flex-direction: column;
  gap: 8rpx;
  font-size: 22rpx;
  color: #bbb;
}
</style>
