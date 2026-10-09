<template>
  <!-- 活动详情页 -->
  <view class="detail-page" v-if="activity">
    <!-- 活动封面 -->
    <image :src="activity.cover" mode="aspectFill" class="cover"></image>

    <!-- 活动信息 -->
    <view class="info-section">
      <text class="title">{{ activity.title }}</text>
      
      <view class="meta-list">
        <view class="meta-item">
          <text class="meta-icon">⏰</text>
          <text class="meta-text">{{ activity.time }}</text>
        </view>
        <view class="meta-item">
          <text class="meta-icon">📍</text>
          <text class="meta-text">{{ activity.location }}</text>
        </view>
        <view class="meta-item">
          <text class="meta-icon">👤</text>
          <text class="meta-text">发起人：{{ activity.initiator }}</text>
        </view>
        <view class="meta-item">
          <text class="meta-icon">👥</text>
          <text class="meta-text">{{ activity.joinCount }}/{{ activity.maxCount }}人已报名</text>
        </view>
      </view>
    </view>

    <!-- 活动介绍 -->
    <view class="desc-section">
      <view class="section-title">活动介绍</view>
      <text class="desc-text">{{ activity.description || '这是一个有趣的校园活动，欢迎大家积极参加！活动内容丰富多彩，你一定不会失望的。' }}</text>
    </view>

    <!-- 报名人员 -->
    <view class="joiners-section">
      <view class="section-title">已报名人员</view>
      <view class="joiner-list">
        <view class="joiner-item" v-for="i in Math.min(activity.joinCount, 6)" :key="i">
          <text class="joiner-avatar">😀</text>
        </view>
        <view class="joiner-more" v-if="activity.joinCount > 6">
          <text>+{{ activity.joinCount - 6 }}</text>
        </view>
      </view>
    </view>

    <!-- 底部报名栏 -->
    <view class="bottom-bar">
      <view class="join-info">
        <text class="join-count">已有 {{ activity.joinCount }} 人报名</text>
      </view>
      <view 
        class="join-btn" 
        :class="{ joined: activity.isJoined, full: !activity.isJoined && activity.joinCount >= activity.maxCount }"
        @click="handleJoin"
      >
        {{ activity.isJoined ? '取消报名' : (activity.joinCount >= activity.maxCount ? '已满员' : '立即报名') }}
      </view>
    </view>
  </view>
</template>

<script setup>
/**
 * 活动详情页
 * 调用真实后端接口获取活动详情
 */
import { ref, onMounted } from 'vue'
import { activityApi } from '@/api/index'

const activity = ref(null)
// 加载状态
const loading = ref(false)

onMounted(() => {
  // 获取页面参数
  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1]
  const id = currentPage.options?.id || 1

  loadDetail(id)
})

/**
 * 加载活动详情
 */
async function loadDetail(id) {
  loading.value = true
  try {
    const data = await activityApi.getActivityDetail(id)
    activity.value = data
  } catch (err) {
    console.warn('活动详情接口请求失败:', err)
    activity.value = null
  } finally {
    loading.value = false
  }
}

/**
 * 报名/取消报名
 * 调用真实接口
 */
async function handleJoin() {
  const item = activity.value
  if (item.isJoined) {
    uni.showModal({
      title: '取消报名',
      content: '确定要取消报名吗？',
      success: async (res) => {
        if (res.confirm) {
          try {
            await activityApi.cancelJoin(item.id)
            item.isJoined = false
            item.joinCount--
            uni.showToast({ title: '已取消', icon: 'success' })
          } catch (err) {
            console.warn('取消报名失败:', err)
          }
        }
      }
    })
  } else if (item.joinCount >= item.maxCount) {
    uni.showToast({ title: '活动已满员', icon: 'none' })
  } else {
    uni.showModal({
      title: '确认报名',
      content: '确定要报名参加这个活动吗？',
      success: async (res) => {
        if (res.confirm) {
          try {
            await activityApi.joinActivity(item.id)
            item.isJoined = true
            item.joinCount++
            uni.showToast({ title: '报名成功', icon: 'success' })
          } catch (err) {
            console.warn('报名失败:', err)
          }
        }
      }
    })
  }
}
</script>

<style lang="scss" scoped>
.detail-page {
  min-height: 100vh;
  background: #f5f5f7;
  padding-bottom: 140rpx;
}

.cover {
  width: 100%;
  height: 400rpx;
}

.info-section {
  background: #fff;
  padding: 30rpx 24rpx;
  margin-bottom: 20rpx;
}

.title {
  font-size: 36rpx;
  font-weight: bold;
  color: #333;
  margin-bottom: 24rpx;
  display: block;
}

.meta-list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.meta-icon {
  font-size: 28rpx;
}

.meta-text {
  font-size: 26rpx;
  color: #666;
}

.desc-section,
.joiners-section {
  background: #fff;
  padding: 24rpx;
  margin-bottom: 20rpx;
}

.section-title {
  font-size: 30rpx;
  font-weight: bold;
  color: #333;
  margin-bottom: 20rpx;
}

.desc-text {
  font-size: 28rpx;
  color: #666;
  line-height: 1.8;
}

.joiner-list {
  display: flex;
  flex-wrap: wrap;
  gap: 20rpx;
}

.joiner-item,
.joiner-more {
  width: 80rpx;
  height: 80rpx;
  border-radius: 50%;
  background: #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36rpx;
}

.joiner-more {
  background: #f0f0f0;
  font-size: 22rpx;
  color: #999;
}

.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  padding: 20rpx 24rpx;
  background: #fff;
  border-top: 1rpx solid #eee;
  z-index: 100;
}

.join-info {
  flex: 1;
}

.join-count {
  font-size: 26rpx;
  color: #666;
}

.join-btn {
  padding: 24rpx 60rpx;
  background: linear-gradient(135deg, #6c4ee6, #8b75f0);
  color: #fff;
  font-size: 30rpx;
  font-weight: bold;
  border-radius: 48rpx;

  &.joined {
    background: #f0f0f0;
    color: #666;
  }

  &.full {
    background: #ffccc7;
    color: #ff4d4f;
  }
}
</style>
