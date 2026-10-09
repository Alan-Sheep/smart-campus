<template>
  <!-- 校园跑腿?-->
  <view class="errand-page">
    <!-- 顶部Tab -->
    <view class="tab-bar">
      <view 
        class="tab-item" 
        :class="{ active: currentTab === 'hall' }"
        @click="switchTab('hall')"
      >
        跑腿大厅
      </view>
      <view 
        class="tab-item" 
        :class="{ active: currentTab === 'my' }"
        @click="switchTab('my')"
      >
        我的发布
      </view>
    </view>

    <!-- 分类筛?-->
    <scroll-view scroll-x class="filter-bar">
      <view 
        class="filter-item" 
        :class="{ active: currentCategory === item.id }"
        v-for="item in categories" 
        :key="item.id"
        @click="selectCategory(item.id)"
      >
        {{ item.name }}
      </view>
    </scroll-view>

    <!-- 订单列表 -->
    <scroll-view scroll-y class="errand-list">
      <view 
        class="errand-card" 
        v-for="item in errandList" 
        :key="item.id"
        @click="goToDetail(item.id)"
      >
        <view class="card-header">
          <view class="type-tag" :style="{ background: getCategoryColor(item.type) + '20', color: getCategoryColor(item.type) }">
            {{ getCategoryName(item.type) }}
          </view>
          <text class="status-tag" :class="'status-' + item.status">
            {{ statusText(item.status) }}
          </text>
        </view>

        <text class="errand-title">{{ item.title }}</text>
        <text class="errand-desc">{{ item.description }}</text>

        <view class="errand-info">
          <view class="info-item">
            <text>📍 {{ item.pickupLocation }}</text>
          </view>
          <view class="info-item">
            <text>🏠 {{ item.deliveryLocation }}</text>
          </view>
          <view class="info-item">
            <text>🕐{{ item.estimateTime }}</text>
          </view>
        </view>

        <view class="card-footer">
          <view class="reward">
            <text class="reward-num">¥{{ item.reward }}</text>
            <text class="reward-label">报酬</text>
          </view>
          <view class="footer-btn" v-if="currentTab === 'hall' && item.status === 0" @click.stop="acceptErrand(item)">
            我来跑腿
          </view>
        </view>
      </view>

      <view style="height: 40rpx;"></view>
    </scroll-view>

    <!-- 发布按钮 -->
    <view class="publish-btn" @click="goPublish">
      <text class="publish-icon">+</text>
      <text>发布跑腿</text>
    </view>
  </view>
</template>

<script setup>
/**
 * 校园跑腿? * 可以发布各种跑腿需求，也可以接跑腿订单赚钱
 */
import { ref, onMounted, onUnmounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { errandApi } from '@/api/index'
import { checkLogin } from '@/utils/request'
import { guardWrite, onSchoolChanged } from '@/utils/school-permission'

const currentTab = ref('hall')
const currentCategory = ref('all')

// 跑腿分类：id 与数据库 errand_type 字段对应?-3?
const categories = ref([
  { id: 'all', name: '全部', color: '#6c4ee6' },
  { id: '0', name: '代买', color: '#52c41a' },
  { id: '1', name: '代取', color: '#1890ff' },
  { id: '2', name: '代取', color: '#fa8c16' },
  { id: '3', name: '其他', color: '#722ed1' }
])

// 跑腿列表数据
const errandList = ref([])

// 加载状?
const loading = ref(false)

onMounted(() => {
  loadErrandList()
})
// ---- 切换学校后刷新 ----
// 用户切换学校返回本页时，重新拉取当前学校的数据
// hasLoadedOnce 避免与 onMounted 首屏加载重复请求
let hasLoadedOnce = false
onShow(() => {
  onSchoolChanged()
  if (hasLoadedOnce) { loadErrandList() }
  hasLoadedOnce = true
})

/**
 * 加载跑腿订单列表
 */
async function loadErrandList() {
  loading.value = true
  try {
    const params = { category: currentCategory.value, type: currentTab.value, pageNum: 1, pageSize: 10 }
    const data = await errandApi.getErrandList(params)
    errandList.value = data.records || data.list || data || []
  } catch (err) {
    console.warn('跑腿订单列表接口请求失败:', err)
    errandList.value = []
  } finally {
    loading.value = false
  }
}

function switchTab(tab) {
  currentTab.value = tab
  loadErrandList()
}

function selectCategory(id) {
  currentCategory.value = id
  loadErrandList()
}

function getCategoryName(type) {
  const item = categories.value.find(c => c.id === type)
  return item ? item.name : '其他'
}

function getCategoryColor(type) {
  const item = categories.value.find(c => c.id === type)
  return item ? item.color : '#999'
}

function statusText(status) {
  const map = { 0: '待接单', 1: '进行中', 2: '已完成', 3: '已取消' }
  return map[status] || '未知'
}

function goToDetail(id) {
  uni.navigateTo({ url: '/pages/errand/detail?id=' + id })
}

/**
 * 接跑腿订? * 调用真实接口
 */
async function acceptErrand(item) {
  if (guardWrite()) return
  uni.showModal({
    title: '确认接单',
    content: '确定要接这个跑腿订单吗？报酬¥' + item.reward,
    success: async (res) => {
      if (res.confirm) {
        try {
          // 调用接单接口
          await errandApi.acceptErrand(item.id)
          uni.showToast({ title: '接单成功', icon: 'success' })
          // 刷新列表
          loadErrandList()
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
  uni.navigateTo({ url: '/pages/errand/publish' })
}
</script>

<style lang="scss" scoped>
.errand-page {
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

.filter-bar {
  background: #fff;
  white-space: nowrap;
  padding: 16rpx 24rpx;
  border-top: 1rpx solid #f0f0f0;
}

.filter-item {
  display: inline-block;
  padding: 12rpx 28rpx;
  margin-right: 16rpx;
  background: #f5f5f7;
  border-radius: 30rpx;
  font-size: 26rpx;
  color: #666;

  &.active {
    background: #6c4ee6;
    color: #fff;
  }
}

.errand-list {
  flex: 1;
  padding: 20rpx 24rpx;
  box-sizing: border-box;
}

.errand-card {
  background: #fff;
  border-radius: 20rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16rpx;
}

.type-tag {
  padding: 6rpx 16rpx;
  font-size: 22rpx;
  border-radius: 20rpx;
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

.errand-title {
  font-size: 30rpx;
  font-weight: bold;
  color: #333;
  margin-bottom: 12rpx;
  display: block;
}

.errand-desc {
  font-size: 26rpx;
  color: #666;
  line-height: 1.5;
  margin-bottom: 16rpx;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}

.errand-info {
  margin-bottom: 20rpx;
}

.info-item {
  font-size: 24rpx;
  color: #999;
  margin-bottom: 8rpx;

  &:last-child { margin-bottom: 0; }
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
