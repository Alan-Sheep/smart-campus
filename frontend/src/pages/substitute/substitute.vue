<template>
  <!-- 代课服务?-->
  <view class="substitute-page">
    <!-- 顶部Tab -->
    <view class="tab-bar">
      <view 
        class="tab-item" 
        :class="{ active: currentTab === 'hall' }"
        @click="switchTab('hall')"
      >
        代课大厅
      </view>
      <view 
        class="tab-item" 
        :class="{ active: currentTab === 'my' }"
        @click="switchTab('my')"
      >
        我的发布
      </view>
    </view>

    <!-- 列表 -->
    <scroll-view scroll-y class="substitute-list">
      <view 
        class="substitute-card" 
        v-for="item in substituteList" 
        :key="item.id"
        @click="goToDetail(item.id)"
      >
        <view class="card-header">
          <view class="course-tag">{{ item.courseName }}</view>
          <text class="status-tag" :class="'status-' + item.status">
            {{ statusText(item.status) }}
          </text>
        </view>

        <view class="course-info">
          <view class="info-row">
            <text class="info-icon">📅</text>
            <text class="info-text">{{ item.day }} 第{{ item.section }}</text>
          </view>
          <view class="info-row">
            <text class="info-icon">📍</text>
            <text class="info-text">{{ item.location }}</text>
          </view>
          <view class="info-row">
            <text class="info-icon">👤</text>
            <text class="info-text">代课老师：{{ item.teacher }}</text>
          </view>
          <view class="info-row" v-if="item.requirement">
            <text class="info-icon">📝</text>
            <text class="info-text">要求：{{ item.requirement }}</text>
          </view>
        </view>

        <view class="card-footer">
          <view class="reward">
            <text class="reward-num">¥{{ item.reward }}</text>
            <text class="reward-label">代课：</text>
          </view>
          <view class="footer-btn" v-if="currentTab === 'hall' && item.status === 0" @click.stop="acceptSubstitute(item)">
            我来代课
          </view>
        </view>
      </view>

      <view style="height: 40rpx;"></view>
    </scroll-view>

    <!-- 发布按钮 -->
    <view class="publish-btn" @click="goPublish">
      <text class="publish-icon">+</text>
      <text>发布代课</text>
    </view>
  </view>
</template>

<script setup>
/**
 * 代课服务? * 发布代课需求或承接代课任务
 */
import { ref, onMounted, onUnmounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { substituteApi } from '@/api/index'
import { checkLogin } from '@/utils/request'
import { guardWrite, onSchoolChanged } from '@/utils/school-permission'

const currentTab = ref('hall')

const substituteList = ref([])

// 加载状?
const loading = ref(false)

onMounted(() => {
  loadList()
})
// ---- 切换学校后刷新 ----
// 用户切换学校返回本页时，重新拉取当前学校的数据
// hasLoadedOnce 避免与 onMounted 首屏加载重复请求
let hasLoadedOnce = false
onShow(() => {
  onSchoolChanged()
  if (hasLoadedOnce) { loadList() }
  hasLoadedOnce = true
})

/**
 * 加载代课订单列表
 */
async function loadList() {
  loading.value = true
  try {
    const data = await substituteApi.getSubstituteList({ type: currentTab.value, pageNum: 1, pageSize: 10 })
    substituteList.value = data.records || data.list || data || []
  } catch (err) {
    console.warn('代课订单列表接口请求失败:', err)
    substituteList.value = []
  } finally {
    loading.value = false
  }
}

function switchTab(tab) {
  currentTab.value = tab
  loadList()
}

function statusText(status) {
  const map = { 0: '待接单', 1: '已接单', 2: '已完成', 3: '已取消' }
  return map[status] || '未知'
}

function goToDetail(id) {
  uni.navigateTo({ url: '/pages/substitute/detail?id=' + id })
}

/**
 * 接代? * 调用真实接口
 */
async function acceptSubstitute(item) {
  if (guardWrite()) return
  if (!checkLogin()) return
  uni.showModal({
    title: '确认代课',
    content: '确定要接这个代课吗？费用¥' + item.reward,
    success: async (res) => {
      if (res.confirm) {
        try {
          // 调用接单接口
          await substituteApi.acceptSubstitute(item.id)
          uni.showToast({ title: '接单成功', icon: 'success' })
          // 刷新列表
          loadList()
        } catch (err) {
          console.warn('接单失败:', err)
        }
      }
    }
  })
}
function goPublish() {
  if (guardWrite()) return
  if (!checkLogin()) return
  uni.navigateTo({ url: '/pages/substitute/publish' })
}
</script>

<style lang="scss" scoped>
.substitute-page {
  min-height: 100vh;
  background: #f5f5f7;
  display: flex;
  flex-direction: column;
}

.tab-bar {
  display: flex;
  background: #fff;
}

.tab-item {
  flex: 1;
  text-align: center;
  padding: 28rpx 0;
  font-size: 28rpx;
  color: #666;
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
      width: 50rpx;
      height: 6rpx;
      background: #6c4ee6;
      border-radius: 3rpx;
    }
  }
}

.substitute-list {
  flex: 1;
  padding: 20rpx 24rpx;
  box-sizing: border-box;
}

.substitute-card {
  background: #fff;
  border-radius: 20rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;
}

.course-tag {
  padding: 8rpx 20rpx;
  background: linear-gradient(135deg, #ffeaa7, #fdcb6e);
  color: #8b6914;
  font-size: 24rpx;
  font-weight: bold;
  border-radius: 10rpx;
}

.status-tag {
  padding: 6rpx 16rpx;
  font-size: 22rpx;
  border-radius: 20rpx;

  &.status-0 { background: #fff7e6; color: #fa8c16; }
  &.status-1 { background: #e6f7ff; color: #1890ff; }
  &.status-2 { background: #f6ffed; color: #52c41a; }
  &.status-3 { background: #fff1f0; color: #ff4d4f; }
}

.course-info {
  margin-bottom: 20rpx;
}

.info-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 12rpx;

  &:last-child { margin-bottom: 0; }
}

.info-icon {
  font-size: 24rpx;
}

.info-text {
  font-size: 26rpx;
  color: #666;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 20rpx;
  border-top: 1rpx solid #f0f0f0;
}

.reward {
  display: flex;
  align-items: baseline;
  gap: 8rpx;
}

.reward-num {
  font-size: 36rpx;
  font-weight: bold;
  color: #ff4d4f;
}

.reward-label {
  font-size: 24rpx;
  color: #999;
}

.footer-btn {
  padding: 14rpx 36rpx;
  background: linear-gradient(135deg, #6c4ee6, #8b75f0);
  color: #fff;
  font-size: 26rpx;
  border-radius: 30rpx;
}

.publish-btn {
  position: fixed;
  right: 40rpx;
  bottom: 120rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 110rpx;
  height: 110rpx;
  background: linear-gradient(135deg, #6c4ee6, #8b75f0);
  color: #fff;
  border-radius: 50%;
  box-shadow: 0 8rpx 24rpx rgba(108, 78, 230, 0.4);
  font-size: 20rpx;
  z-index: 100;
}

.publish-icon {
  font-size: 40rpx;
  line-height: 1;
}
</style>
