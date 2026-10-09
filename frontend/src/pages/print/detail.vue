<template>
  <view class="detail-page">
    <view class="nav-bar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-back" @click="goBack">
        <text class="back-icon">‹</text>
      </view>
      <view class="nav-title">打印详情</view>
      <view class="nav-right"></view>
    </view>

    <view class="loading-wrap" v-if="loading" :style="{ paddingTop: navHeight + 'px' }">
      <text class="loading-text">加载中...</text>
    </view>

    <view class="empty-wrap" v-else-if="!detail" :style="{ paddingTop: navHeight + 'px' }">
      <text class="empty-text">暂无订单信息</text>
    </view>

    <view class="detail-content" v-else :style="{ paddingTop: navHeight + 'px' }">
      <view class="top-banner">
        <view class="banner-icon">🖨️</view>
        <view class="banner-status" :class="'status-' + detail.status">
          {{ statusMap[detail.status] || '未知' }}
        </view>
        <view class="banner-reward">
          <text class="reward-label">报酬</text>
          <text class="reward-amount">¥{{ detail.totalPrice || detail.price || 0 }}</text>
        </view>
      </view>

      <view class="info-card">
        <view class="card-title">打印信息</view>
        <view class="info-row">
          <text class="info-label">文件名称</text>
          <text class="info-value">{{ detail.fileName || '-' }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">打印类型</text>
          <text class="info-value">{{ printTypeMap[detail.printType] || detail.printType || '-' }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">纸张大小</text>
          <text class="info-value">{{ detail.paperSize || 'A4' }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">打印份数</text>
          <text class="info-value">{{ detail.copies || 1 }}份</text>
        </view>
        <view class="info-row">
          <text class="info-label">双面打印</text>
          <text class="info-value">{{ detail.isDouble ? '是' : '否' }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">彩色打印</text>
          <text class="info-value">{{ detail.isColor ? '是' : '否' }}</text>
        </view>
        <view class="info-row" v-if="detail.remark">
          <text class="info-label">备注</text>
          <text class="info-value">{{ detail.remark }}</text>
        </view>
      </view>

      <view class="info-card">
        <view class="card-title">配送信息</view>
        <view class="info-row">
          <text class="info-label">收货地址</text>
          <text class="info-value">{{ detail.address || '-' }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">联系电话</text>
          <text class="info-value">{{ detail.phone || '-' }}</text>
        </view>
      </view>

      <view class="publisher-card">
        <view class="publisher-avatar">👤</view>
        <view class="publisher-info">
          <text class="publisher-name">{{ detail.publisher || '校园用户' }}</text>
          <text class="publisher-time">下单于 {{ detail.createTime || '' }}</text>
        </view>
      </view>

      <view class="bottom-space"></view>
    </view>

    <view class="bottom-bar" v-if="detail">
      <template v-if="isOwner && detail.status === 0">
        <view class="action-btn cancel-btn" @click="cancelOrder">
          取消订单
        </view>
      </template>
      <template v-else-if="isOwner && detail.status === 1">
        <view class="action-btn contact-btn" @click="contactPublisher">
          联系打印店
        </view>
      </template>
      <template v-else>
        <view class="action-btn contact-btn" @click="contactPublisher">
          联系发布人
        </view>
      </template>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { printApi } from '@/api/index'

const statusBarHeight = ref(20)
const navHeight = ref(88)
const loading = ref(false)
const detail = ref(null)
const orderId = ref(null)
const isOwner = ref(false)

const statusMap = {
  0: '待打印',
  1: '打印中',
  2: '已完成',
  3: '已取消'
}

const printTypeMap = {
  document: '文档打印',
  photo: '照片打印',
  poster: '海报打印',
  other: '其他'
}

onMounted(() => {
  const sysInfo = uni.getSystemInfoSync()
  statusBarHeight.value = sysInfo.statusBarHeight || 20
  navHeight.value = statusBarHeight.value + 44

  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1]
  const id = currentPage.options?.id
  orderId.value = id

  if (id) {
    loadDetail(id)
  } else {
    uni.showToast({ title: '参数错误', icon: 'none' })
  }
})

async function loadDetail(id) {
  loading.value = true
  try {
    const data = await printApi.getPrintDetail(id)
    detail.value = data
    const currentUserId = uni.getStorageSync('userInfo')?.id
    if (currentUserId) {
      isOwner.value = String(data.userId) === String(currentUserId)
    }
  } catch (err) {
    console.warn('打印详情加载失败:', err)
    detail.value = null
  } finally {
    loading.value = false
  }
}

function goBack() {
  uni.navigateBack()
}

function contactPublisher() {
  if (!detail.value) return
  const targetUserId = detail.value.userId
  if (targetUserId) {
    uni.navigateTo({
      url: '/pages/message/chat?targetUserId=' + targetUserId + '&targetNickname=' + encodeURIComponent('打印订单')
    })
  } else {
    uni.showToast({ title: '无法获取发布人信息', icon: 'none' })
  }
}

async function cancelOrder() {
  uni.showModal({
    title: '取消订单',
    content: '确定要取消这个打印订单吗？',
    confirmColor: '#ff4d4f',
    success: async (res) => {
      if (res.confirm) {
        try {
          await printApi.cancelOrder(orderId.value)
          uni.showToast({ title: '已取消订单', icon: 'success' })
          loadDetail(orderId.value)
        } catch (err) {
          console.warn('取消订单失败:', err)
          uni.showToast({ title: '取消失败，请重试', icon: 'none' })
        }
      }
    }
  })
}
</script>

<style lang="scss" scoped>
$primary-color: #6c4ee6;

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

.detail-content {
  padding-bottom: 180rpx;
}

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

  &.status-0 { background: rgba(255, 193, 7, 0.9); }
  &.status-1 { background: rgba(24, 144, 255, 0.9); }
  &.status-2 { background: rgba(82, 196, 26, 0.9); }
  &.status-3 { background: rgba(153, 153, 153, 0.9); }
}

.banner-reward {
  display: flex;
  align-items: baseline;
  gap: 10rpx;
}

.reward-label {
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.85);
}

.reward-amount {
  font-size: 52rpx;
  font-weight: bold;
  color: #fff;
}

.info-card {
  background: #fff;
  margin: 20rpx;
  border-radius: 16rpx;
  padding: 28rpx 24rpx;
}

.card-title {
  font-size: 30rpx;
  font-weight: bold;
  color: #333;
  margin-bottom: 24rpx;
  padding-bottom: 16rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.info-row {
  display: flex;
  padding: 16rpx 0;

  &:last-child { padding-bottom: 0; }
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

.bottom-space {
  height: 40rpx;
}

.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 20rpx 24rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  background: #fff;
  border-top: 1rpx solid #eee;
  z-index: 100;
}

.action-btn {
  height: 88rpx;
  line-height: 88rpx;
  text-align: center;
  border-radius: 44rpx;
  font-size: 30rpx;
  font-weight: bold;
}

.cancel-btn {
  background: #fff;
  color: #ff4d4f;
  border: 2rpx solid #ff4d4f;
}

.contact-btn {
  background: linear-gradient(135deg, $primary-color, #8b6ff0);
  color: #fff;
}
</style>
