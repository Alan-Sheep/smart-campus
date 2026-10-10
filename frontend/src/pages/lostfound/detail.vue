<template>
  <!-- 失物招领详情页 -->
  <view class="detail-page" v-if="item">
    <!-- 图片 -->
    <image :src="item.image" mode="aspectFill" class="item-image"></image>

    <!-- 基本信息 -->
    <view class="info-section">
      <view class="type-tag" :class="item.type">
        {{ item.type === 'lost' ? '寻物启事' : '失物招领' }}
      </view>
      <text class="item-title">{{ item.title }}</text>
      
      <view class="meta-list">
        <view class="meta-item">
          <text class="meta-icon">📍</text>
          <text class="meta-text">{{ item.location }}</text>
        </view>
        <view class="meta-item">
          <text class="meta-icon">⏰</text>
          <text class="meta-text">{{ item.createTime }}</text>
        </view>
        <view class="meta-item">
          <text class="meta-icon">👤</text>
          <text class="meta-text">{{ item.publisher }}</text>
        </view>
      </view>
    </view>

    <!-- 详细描述 -->
    <view class="desc-section">
      <view class="section-title">详细描述</view>
      <text class="desc-text">{{ item.description }}</text>
    </view>

    <!-- AI匹配结果（只有丢失的物品才有） -->
    <view class="ai-section" v-if="item.type === 'lost' && matchList.length">
      <view class="section-header">
        <text class="section-title">🤖 AI智能匹配</text>
        <text class="match-count">找到 {{ matchList.length }} 个相似物品</text>
      </view>
      <view class="match-list">
        <view 
          class="match-item" 
          v-for="match in matchList" 
          :key="match.id"
          @click="goToMatchDetail(match.id)"
        >
          <image :src="match.image" class="match-image"></image>
          <view class="match-info">
            <text class="match-title">{{ match.title }}</text>
            <text class="match-location">📍 {{ match.location }}</text>
          </view>
          <view class="match-rate">
            <text class="rate-text">{{ match.similarity }}%</text>
            <text class="rate-label">相似</text>
          </view>
        </view>
      </view>
    </view>
            + '&prefill=' + encodeURIComponent('你好，关于「' + (item.value.title || '') + '」')
    <!-- 底部操作栏 -->
    <view class="bottom-bar">
      <view class="bar-item" @click="contactPublisher">
        <text class="bar-icon">💬</text>
        <text class="bar-text">联系</text>
      </view>
      <view class="primary-btn" @click="handleAction">
        {{ item.type === 'lost' ? '我看到过' : '这是我的' }}
      </view>
    </view>
  </view>
</template>

<script setup>
/**
 * 失物招领详情页
 */
import { ref, onMounted } from 'vue'
import { lostFoundApi } from '@/api/index'

const item = ref(null)
const matchList = ref([])

onMounted(() => {
  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1]
  const id = currentPage.options?.id || 1

  loadDetail(id)
})

async function loadDetail(id) {
  try {
    const res = await lostFoundApi.getLostFoundDetail(id)
    item.value = {
      ...res,
      image: (res.images || '').split(',').filter(Boolean)[0] || '',
      publisher: res.nickname || ('同学' + String(res.userId || '').slice(-4))
    }
    item.value.userId = res.userId   // 发布人ID：私信跳转用
  } catch (err) {
    console.warn('失物详情加载失败:', err)
    uni.showToast({ title: '详情加载失败', icon: 'none' })
  }
}

/**
 * 联系发布人（B方案：主推站内私信，避免手机号公开被骚扰）
 * 未登录 → 先登录；自己是发布者 → 不需要联系
 * 发布者填了联系电话 → 提供「拨打电话」备选项
 */
function contactPublisher() {
  if (!item.value) return
  const userInfo = uni.getStorageSync('userInfo')
  if (!userInfo || !userInfo.id) {
    uni.showModal({ title: '提示', content: '请先登录后再联系发布者', confirmText: '去登录',
      success: (r) => { if (r.confirm) uni.navigateTo({ url: '/pages/login/login' }) } })
    return
  }
  if (userInfo.id === item.value.userId) {
    uni.showToast({ title: '这是你发布的', icon: 'none' })
    return
  }
  const hasPhone = item.value.contact && !item.value.contact.includes('*')
  uni.showActionSheet({
    itemList: hasPhone ? ['发站内私信', '拨打电话'] : ['发站内私信'],
    success: (r) => {
      if (r.tapIndex === 0) {
        // 跳转聊天页：带对方ID和昵称，预填一条消息
        const nickname = encodeURIComponent(item.value.publisher || '失物发布者')
        uni.navigateTo({
          url: '/pages/message/chat?targetUserId=' + item.value.userId
            + '&targetNickname=' + nickname
            + '&prefill=' + encodeURIComponent('你好，关于「' + (item.value.title || '') + '」')
        })
      } else if (r.tapIndex === 1 && hasPhone) {
        uni.makePhoneCall({ phoneNumber: item.value.contact })
      }
    }
  })
}


function handleAction() {
  if (item.value.type === 'lost') {
    uni.showToast({ title: '感谢提供线索！', icon: 'success' })
  } else {
    uni.showModal({
      title: '认领确认',
      content: '请详细描述物品特征，确认是你的物品后发布者会联系你。',
      success: (res) => {
        if (res.confirm) {
          uni.showToast({ title: '已通知发布者', icon: 'success' })
        }
      }
    })
  }
}

function goToMatchDetail(id) {
  uni.navigateTo({ url: '/pages/lostfound/detail?id=' + id })
}
</script>

<style lang="scss" scoped>
.detail-page {
  min-height: 100vh;
  background: #f5f5f7;
  padding-bottom: 140rpx;
}

.item-image {
  width: 100%;
  height: 500rpx;
}

.info-section {
  background: #fff;
  padding: 24rpx;
  margin-bottom: 20rpx;
}

.type-tag {
  display: inline-block;
  padding: 8rpx 20rpx;
  font-size: 24rpx;
  border-radius: 10rpx;
  margin-bottom: 16rpx;

  &.lost {
    background: #fff1f0;
    color: #ff4d4f;
  }
  &.found {
    background: #f6ffed;
    color: #52c41a;
  }
}

.item-title {
  font-size: 36rpx;
  font-weight: bold;
  color: #333;
  margin-bottom: 20rpx;
  display: block;
}

.meta-list {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.meta-icon {
  font-size: 26rpx;
}

.meta-text {
  font-size: 26rpx;
  color: #666;
}

.desc-section,
.ai-section {
  background: #fff;
  padding: 24rpx;
  margin-bottom: 20rpx;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;
}

.section-title {
  font-size: 30rpx;
  font-weight: bold;
  color: #333;
  margin-bottom: 20rpx;

  .section-header & {
    margin-bottom: 0;
  }
}

.match-count {
  font-size: 24rpx;
  color: #6c4ee6;
}

.desc-text {
  font-size: 28rpx;
  color: #666;
  line-height: 1.8;
}

.match-list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.match-item {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 16rpx;
  background: #f9f9ff;
  border-radius: 16rpx;
}

.match-image {
  width: 100rpx;
  height: 100rpx;
  border-radius: 12rpx;
  flex-shrink: 0;
}

.match-info {
  flex: 1;
  min-width: 0;
}

.match-title {
  font-size: 26rpx;
  color: #333;
  margin-bottom: 6rpx;
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.match-location {
  font-size: 22rpx;
  color: #999;
}

.match-rate {
  text-align: center;
  flex-shrink: 0;
}

.rate-text {
  display: block;
  font-size: 32rpx;
  font-weight: bold;
  color: #6c4ee6;
}

.rate-label {
  font-size: 20rpx;
  color: #999;
}

.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  padding: 16rpx 24rpx;
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

.primary-btn {
  flex: 1;
  height: 88rpx;
  line-height: 88rpx;
  text-align: center;
  background: #333;
  color: #fff;
  font-size: 30rpx;
  font-weight: bold;
  border-radius: 44rpx;
}
</style>