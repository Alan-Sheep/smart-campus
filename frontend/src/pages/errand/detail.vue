<template>
  <!-- 校园跑腿详情页 -->
  <view class="detail-page">
    <!-- 自定义导航栏 -->
    <view class="nav-bar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-back" @click="goBack">
        <text class="back-icon">‹</text>
      </view>
      <view class="nav-title">跑腿详情</view>
      <view class="nav-right"></view>
    </view>

    <!-- 加载状态 -->
    <view class="loading-wrap" v-if="loading" :style="{ paddingTop: navHeight + 'px' }">
      <text class="loading-text">加载中...</text>
    </view>

    <!-- 空状态 -->
    <view class="empty-wrap" v-else-if="!detail" :style="{ paddingTop: navHeight + 'px' }">
      <text class="empty-text">暂无订单信息</text>
    </view>

    <!-- 详情内容 -->
    <view class="detail-content" v-else :style="{ paddingTop: navHeight + 'px' }">
      <!-- 顶部横幅区 -->
      <view class="top-banner">
        <view class="banner-icon">🏃</view>
        <view class="banner-status" :class="'status-' + detail.status">
          {{ statusMap[detail.status] || '未知' }}
        </view>
        <view class="banner-reward">
          <text class="reward-label">报酬</text>
          <text class="reward-amount">¥{{ detail.reward || 0 }}</text>
        </view>
      </view>

      <!-- 跑腿信息 -->
      <view class="info-card">
        <view class="card-title">订单信息</view>
        <view class="info-row">
          <text class="info-label">标题</text>
          <text class="info-value">{{ detail.title || '-' }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">跑腿类型</text>
          <text class="info-value">{{ typeMap[detail.type] || detail.type || '-' }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">取件地址</text>
          <text class="info-value">{{ detail.pickupLocation || '-' }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">送达地址</text>
          <text class="info-value">{{ detail.deliveryLocation || '-' }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">预计时间</text>
          <text class="info-value">{{ detail.estimateTime || '尽快' }}</text>
        </view>
      </view>

      <!-- 描述 -->
      <view class="desc-card" v-if="detail.description">
        <view class="card-title">详细描述</view>
        <text class="desc-text">{{ detail.description }}</text>
      </view>

      <!-- 发布人信息 -->
      <view class="publisher-card" @click="contactPublisher">
        <view class="publisher-avatar">👤</view>
        <view class="publisher-info">
          <text class="publisher-name">{{ detail.publisher || detail.publisherName || '校园用户' }}</text>
          <text class="publisher-time">发布于 {{ detail.createTime || '' }}</text>
        </view>
        <text class="arrow">›</text>
      </view>

      <view class="bottom-space"></view>
    </view>

    <!-- 底部操作栏 -->
    <view class="bottom-bar" v-if="detail">
      <!-- 发布人 + 待接单：取消订单 -->
      <template v-if="isPublisher && detail.status === 0">
        <view class="action-btn cancel-btn" @click="cancelOrder">
          取消订单
        </view>
      </template>

      <!-- 接单大厅（待接单状态，非发布人）：立即接单 -->
      <template v-else-if="detail.status === 0 && !isPublisher">
        <view class="action-btn primary-btn" @click="acceptOrder">
          立即接单
        </view>
      </template>

      <!-- 进行中 + 接单人：完成订单 -->
      <template v-else-if="detail.status === 1 && isAcceptor">
        <view class="action-btn primary-btn" @click="finishOrder">
          完成订单
        </view>
      </template>

      <!-- 其他状态：联系发布人 -->
      <template v-else>
        <view class="action-btn contact-btn" @click="contactPublisher">
          联系发布人
        </view>
      </template>
    </view>
  </view>
</template>

<script setup>
/**
 * 校园跑腿详情页
 */
import { ref, onMounted } from 'vue'
import { errandApi } from '@/api/index'

const statusBarHeight = ref(20)
const navHeight = ref(88)
const loading = ref(false)
const detail = ref(null)
const orderId = ref(null)

// 状态映射：0=待接单, 1=进行中, 2=已完成, 3=已取消
const statusMap = {
  0: '待接单',
  1: '进行中',
  2: '已完成',
  3: '已取消'
}

// 跑腿类型映射
const typeMap = {
  '1': '代购物品',
  '2': '代取快递',
  '3': '代送文件',
  '4': '其他跑腿'
}

// 是否为发布人（简化判断，实际需结合用户信息）
const isPublisher = ref(false)
// 是否为接单人（简化判断，实际需结合用户信息）
const isAcceptor = ref(false)

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

/**
 * 加载详情
 */
async function loadDetail(id) {
  loading.value = true
  try {
    const data = await errandApi.getErrandDetail(id)
    detail.value = data
    const currentUserId = uni.getStorageSync('userInfo')?.id
    if (currentUserId) {
      isPublisher.value = String(data.userId) === String(currentUserId)
      isAcceptor.value = String(data.runnerId || 0) === String(currentUserId)
    }
  } catch (err) {
    console.warn('跑腿详情加载失败:', err)
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
 * 联系发布人
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
 * 接单
 */
async function acceptOrder() {
  uni.showModal({
    title: '确认接单',
    content: '确定要接这个跑腿订单吗？接单后请尽快完成。',
    success: async (res) => {
      if (res.confirm) {
        try {
          await errandApi.acceptErrand(orderId.value)
          uni.showToast({ title: '接单成功', icon: 'success' })
          loadDetail(orderId.value)
        } catch (err) {
          console.warn('接单失败:', err)
          uni.showToast({ title: '接单失败，请重试', icon: 'none' })
        }
      }
    }
  })
}

/**
 * 完成订单
 */
async function finishOrder() {
  uni.showModal({
    title: '确认完成',
    content: '确认已完成跑腿任务并送达指定地址吗？',
    success: async (res) => {
      if (res.confirm) {
        try {
          await errandApi.finishErrand(orderId.value)
          uni.showToast({ title: '订单已完成', icon: 'success' })
          loadDetail(orderId.value)
        } catch (err) {
          console.warn('完成订单失败:', err)
          uni.showToast({ title: '操作失败，请重试', icon: 'none' })
        }
      }
    }
  })
}

/**
 * 取消订单
 */
async function cancelOrder() {
  uni.showModal({
    title: '取消订单',
    content: '确定要取消这个跑腿订单吗？',
    confirmColor: '#ff4d4f',
    success: async (res) => {
      if (res.confirm) {
        try {
          await errandApi.cancelErrand(orderId.value)
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
    background: rgba(255, 193, 7, 0.9);
    color: #fff;
  }
  &.status-1 {
    background: rgba(24, 144, 255, 0.9);
    color: #fff;
  }
  &.status-2 {
    background: rgba(82, 196, 26, 0.9);
    color: #fff;
  }
  &.status-3 {
    background: rgba(153, 153, 153, 0.9);
    color: #fff;
  }
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

/* 信息卡片 */
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

.primary-btn {
  background: linear-gradient(135deg, $primary-color, #8b6ff0);
  color: #fff;
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
