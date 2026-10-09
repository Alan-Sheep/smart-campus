<template>
  <!-- 校园活动列表?-->
  <view class="activity-page">
    <!-- 顶部Banner -->
    <view class="top-banner">
      <text class="banner-title">🎉 校园活动</text>
      <text class="banner-desc">发现精彩校园生活</text>
    </view>

    <!-- 分类标签 -->
    <view class="tab-list">
      <view 
        class="tab-item" 
        :class="{ active: currentTab === item.id }"
        v-for="item in tabList" 
        :key="item.id"
        @click="switchTab(item.id)"
      >
        {{ item.name }}
      </view>
    </view>

    <!-- 活动列表 -->
    <view class="activity-list">
      <view 
        class="activity-card" 
        v-for="item in activityList" 
        :key="item.id"
        @click="goToDetail(item.id)"
      >
        <image :src="item.cover" class="activity-cover" mode="aspectFill"></image>
        <view class="activity-info">
          <view class="activity-header">
            <text class="activity-title">{{ item.title }}</text>
            <view class="activity-status" :class="'status-' + item.status">
              {{ item.status === 0 ? '报名中' : item.status === 1 ? '进行中' : '已结束' }}
            </view>
          </view>
          <view class="activity-meta">
            <text class="meta-item">📅 {{ item.startTime }}</text>
            <text class="meta-item">📍 {{ item.location }}</text>
          </view>
          <view class="activity-footer">
            <text class="signup-count">已报名 {{ item.currentParticipants }}/{{ item.maxParticipants }} 人</text>
            <view class="signup-btn" @click.stop="signupActivity(item)">
              立即报名
            </view>
          </view>
        </view>
      </view>
    </view>

    <view style="height: 40rpx;"></view>
  </view>
</template>

<script setup>
/**
 * 校园活动? * 展示校园内的各种活动，支持报名参? */
import { ref, onMounted, onUnmounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { activityApi } from '@/api/index'
import { checkLogin } from '@/utils/request'
import { guardWrite, onSchoolChanged } from '@/utils/school-permission'

const activityList = ref([])
// 加载状?
const loading = ref(false)

const currentTab = ref('all')
// 活动类型映射：前端显示名 ?后端数字?
const tabList = ref([
  { id: 'all', name: '全部' },
  { id: '0', name: '学术' },
  { id: '1', name: '文娱' },
  { id: '2', name: '体育' },
  { id: '3', name: '其他' }
])

onMounted(() => {
  loadActivityList()
})
// ---- 切换学校后刷新 ----
// 用户切换学校返回本页时，重新拉取当前学校的数据
// hasLoadedOnce 避免与 onMounted 首屏加载重复请求
let hasLoadedOnce = false
onShow(() => {
  onSchoolChanged()
  if (hasLoadedOnce) { loadActivityList() }
  hasLoadedOnce = true
})

/**
 * 加载活动列表
 */
async function loadActivityList() {
  loading.value = true
  try {
    const data = await activityApi.getActivityList({ type: currentTab.value, pageNum: 1, pageSize: 10 })
    activityList.value = data.records || data.list || data || []
  } catch (err) {
    console.warn('活动列表接口请求失败:', err)
    activityList.value = []
  } finally {
    loading.value = false
  }
}

function switchTab(id) {
  currentTab.value = id
  loadActivityList()
}

function goToDetail(id) {
  uni.navigateTo({ url: '/pages/activity/detail?id=' + id })
}
function signupActivity(item) {
  if (guardWrite()) return
  if (!checkLogin()) return
  uni.showModal({
    title: '确认报名',
    content: '确定要报名参加「' + item.title + '」吗？',
    success: async (res) => {
      if (res.confirm) {
        try {
          await activityApi.joinActivity(item.id)
          uni.showToast({ title: '报名成功', icon: 'success' })
          loadActivityList()
        } catch (err) {
          console.warn('报名失败:', err)
        }
      }
    }
  })
}
</script>

<style lang="scss" scoped>
.activity-page {
  min-height: 100vh;
  background: #f5f5f7;
}

.top-banner {
  padding: 40rpx 24rpx;
  background: linear-gradient(135deg, #6c4ee6, #8b5cf6);
  color: #fff;
}

.banner-title {
  font-size: 40rpx;
  font-weight: bold;
  display: block;
  margin-bottom: 8rpx;
}

.banner-desc {
  font-size: 26rpx;
  opacity: 0.9;
}

.tab-list {
  display: flex;
  justify-content: space-around;
  align-items: center;
  background: #fff;
  padding: 20rpx 24rpx;
}

.tab-item {
  flex: 1;
  text-align: center;
  font-size: 28rpx;
  color: #666;
  padding: 8rpx 0;
  position: relative;

  &.active {
    color: #6c4ee6;
    font-weight: bold;

    &::after {
      content: '';
      position: absolute;
      bottom: 0;
      left: 50%;
      transform: translateX(-50%);
      width: 40rpx;
      height: 6rpx;
      background: #6c4ee6;
      border-radius: 3rpx;
    }
  }
}

.activity-list {
  padding: 20rpx 24rpx;
}

.activity-card {
  background: #fff;
  border-radius: 20rpx;
  overflow: hidden;
  margin-bottom: 20rpx;
}

.activity-cover {
  width: 100%;
  height: 300rpx;
}

.activity-info {
  padding: 24rpx;
}

.activity-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16rpx;
}

.activity-title {
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
  flex: 1;
}

.activity-status {
  padding: 6rpx 16rpx;
  border-radius: 20rpx;
  font-size: 22rpx;
  flex-shrink: 0;
  margin-left: 16rpx;

  &.status-0 {
    background: #f6ffed;
    color: #52c41a;
  }
  &.status-1 {
    background: #e6f7ff;
    color: #1890ff;
  }
  &.status-2 {
    background: #f5f5f5;
    color: #999;
  }
}

.activity-meta {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
  margin-bottom: 20rpx;
}

.meta-item {
  font-size: 26rpx;
  color: #666;
}

.activity-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 20rpx;
  border-top: 1rpx solid #f0f0f0;
}

.signup-count {
  font-size: 24rpx;
  color: #999;
}

.signup-btn {
  padding: 14rpx 36rpx;
  background: linear-gradient(135deg, #6c4ee6, #8b5cf6);
  color: #fff;
  font-size: 26rpx;
  border-radius: 30rpx;
}
</style>