<template>
  <!-- 首页 -->
  <view class="home-page">
    <!-- 自定义导航栏 - 包含学校选择和搜索框 -->
    <view class="nav-bar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-content">
        <!-- 左上角学校选择 -->
        <view class="school-select" @click="goToSchool">
          <text class="school-name">{{ currentSchool.name }}</text>
          <text class="arrow">›</text>
        </view>
        <!-- 搜索?-->
        <view class="search-box" @click="handleSearch">
          <text class="search-icon">🔍</text>
          <text class="search-placeholder">优惠活动/跑腿/代取</text>
        </view>
      </view>
    </view>

    <!-- 滚动内容区域 -->
    <scroll-view 
      scroll-y 
      class="scroll-content"
      :style="{ paddingTop: navHeight + 'px' }"
      @scrolltolower="loadMore"
      refresher-enabled
      :refresher-triggered="isRefreshing"
      @refresherrefresh="onRefresh"
    >
      <!-- 轮播?-->
      <view class="banner-wrapper">
        <swiper 
          class="banner-swiper" 
          :autoplay="true" 
          :interval="3000" 
          :circular="true"
          indicator-dots
          indicator-color="rgba(255,255,255,0.5)"
          indicator-active-color="#fff"
        >
          <swiper-item v-for="item in bannerList" :key="item.id" @click="onBannerClick(item)">
            <image :src="item.image" mode="aspectFill" lazy-load class="banner-image"></image>
          </swiper-item>
        </swiper>
      </view>

      <!-- 公告?-->
      <view class="notice-bar">
        <view class="notice-icon">📢</view>
        <swiper class="notice-swiper" :vertical="true" :autoplay="true" :interval="3000" :circular="true" :display-multiple-items="1">
          <swiper-item v-for="item in noticeList" :key="item.id">
            <text class="notice-text">{{ item.title }}</text>
          </swiper-item>
        </swiper>
      </view>

      <!-- 功能模块 - 8个功能入?-->
      <view class="module-section">
        <view 
          class="module-item" 
          v-for="item in moduleList" 
          :key="item.id"
          @click="goToModule(item)"
        >
          <view class="module-icon" :style="{ backgroundColor: item.color + '20' }">
            <text class="module-emoji">{{ item.icon }}</text>
          </view>
          <text class="module-name">{{ item.name }}</text>
        </view>
      </view>

      <!-- 校园活动区域 -->
      <view class="section">
        <view class="section-header">
          <text class="section-title">校园活动~</text>
          <view class="section-more" @click="goToActivity">
            <text>全部</text>
            <text class="more-arrow">›</text>
          </view>
        </view>

        <view class="activity-list">
          <view 
            class="activity-item" 
            v-for="item in activityList" 
            :key="item.id"
            @click="goToActivityDetail(item.id)"
          >
            <image :src="item.cover" mode="aspectFill" lazy-load class="activity-cover"></image>
            <view class="activity-info">
              <text class="activity-title">{{ item.title }}</text>
              <text class="activity-meta">🕐{{ item.time }}</text>
              <text class="activity-meta">📍 {{ item.location }}</text>
              <text class="activity-meta">👤 发起人：{{ item.initiator }}</text>
            </view>
            <view class="activity-btn" @click.stop="joinActivity(item)">
              <text>立即报名</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 底部留白 -->
      <view style="height: 40rpx;"></view>
    </scroll-view>
      <!-- AI 助手悬浮球 -->
    <view class="ai-fab" :class="{ 'is-expanded': aiFabExpanded }" @click="onAiFabClick">
      <text class="ai-fab-icon">🤖</text>
    </view>
  </view>
</template>

<script setup>
/**
 * 首页
 * 包含：学校选择、搜索、轮播图、公告?个功能模块、校园活动列? */
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useSchoolStore } from '@/store/school'
import { homeApi, activityApi } from '@/api/index'

// 学校状态管?
const schoolStore = useSchoolStore()
const currentSchool = computed(() => schoolStore.currentSchool)

// 获取状态栏高度，用于自定义导航?
const statusBarHeight = ref(20)
const navHeight = ref(88) // 导航栏总高?
// 轮播图数?
const bannerList = ref([])
// 公告数据
const noticeList = ref([])
// 功能模块数据
const moduleList = ref([])
// 活动列表
const activityList = ref([])
// 下拉刷新状?
const isRefreshing = ref(false)
// 加载中状?
const loading = ref(false)

// 页面加载时获取数据
onMounted(() => {
  const sysInfo = uni.getSystemInfoSync()
  statusBarHeight.value = sysInfo.statusBarHeight || 20
  navHeight.value = statusBarHeight.value + 44 // 状态栏 + 导航栏内容高度
  loadHomeData()
})

// ---- 切换学校后刷新 ----
let hasLoadedOnce = false
onShow(() => {
  if (hasLoadedOnce) { loadHomeData() }
  hasLoadedOnce = true
})

// 功能模块定义（抽成常量，避免多处重复定义）
const ALL_MODULES = [
  { id: 'express', name: '快递代取', icon: '📦', path: '/pages/express/express' },
  { id: 'print', name: '打印服务', icon: '🖨️', path: '/pages/print/print' },
  { id: 'errand', name: '校园跑腿', icon: '🏃', path: '/pages/errand/errand' },
  { id: 'substitute', name: '代课服务', icon: '📚', path: '/pages/substitute/substitute' },
  { id: 'activity', name: '校园活动', icon: '🎉', path: '/pages/activity/activity' },
  { id: 'goods', name: '二手市场', icon: '🛒', path: '/pages/goods/goods' },
  { id: 'game', name: '游戏搭子', icon: '🎮', path: '/pages/game/game' },
  { id: 'lostfound', name: '失物招领', icon: '🔍', path: '/pages/lostfound/lostfound' }
]

/**
 * 加载首页数据
 * 性能优化：首页综合数据与活动列表并行请求，活动列表失败不影响首屏
 */
async function loadHomeData() {
  loading.value = true
  // 两个请求并行发起
const indexPromise = homeApi.getIndex()
  const activityPromise = activityApi
    .getActivityList({ pageNum: 1, pageSize: 3 })
    .catch((e) => {
      console.warn('活动列表获取失败:', e)
      return null
    })

  try {
    const [data, actData] = await Promise.all([indexPromise, activityPromise])

    bannerList.value = (data.bannerList || []).map((item, index) => ({
      ...item,
      image: '/static/banner/banner' + ((index % 5) + 1) + '.svg'
    }))
    noticeList.value = data.noticeList || []

    const modulesObj = data.modules || {}
    moduleList.value = ALL_MODULES.filter(m => modulesObj[m.id] !== false)

    activityList.value = actData
      ? (actData.records || actData.list || actData || [])
      : []
  } catch (err) {
    console.warn('首页接口请求失败:', err)
    bannerList.value = []
    noticeList.value = []
    moduleList.value = ALL_MODULES
    activityList.value = []
  } finally {
    loading.value = false
  }
}

/**
 * 跳转到学校选择? */
function goToSchool() {
  uni.navigateTo({ url: '/pages/school/school' })
}

/**
 * 搜索点击
 */
function handleSearch() {
  uni.navigateTo({ url: '/pages/search/search' })
}

/**
 * 轮播图点? */
function onBannerClick(item) {
  if (item.link) {
    uni.navigateTo({ url: item.link })
  }
}

/**
 * 跳转到功能模块页? */
function goToModule(item) {
  uni.navigateTo({ url: item.path })
}

/**
 * 跳转到活动列表页
 */
function goToActivity() {
  uni.navigateTo({ url: '/pages/activity/activity' })
}

/**
 * 跳转到活动详情
 */
function goToActivityDetail(id) {
  uni.navigateTo({ url: '/pages/activity/detail?id=' + id })
}

/**
 * 报名活动
 */
async function joinActivity(item) {
  uni.showModal({
    title: '确认报名',
    content: '确定要报名参加「' + item.title + '」吗？',
    success: async (res) => {
      if (res.confirm) {
        try {
          await activityApi.joinActivity(item.id)
          uni.showToast({ title: '报名成功', icon: 'success' })
          loadHomeData()
        } catch (err) {
          console.error('报名失败:', err)
        }
      }
    }
  })
}

/**
 * 下拉刷新
 */
async function onRefresh() {
  isRefreshing.value = true
  try {
    await loadHomeData()
  } finally {
    isRefreshing.value = false
  }
}

/**
 * 上拉加载更多
 */
function loadMore() {
  // 这里可以加载更多活动
  console.log('加载更多')
}
let aiFabExpanded = false
function onAiFabClick() {
  if (!aiFabExpanded) {
    // 第一次点击：只冒头完整显示，不跳转
    aiFabExpanded = true
  } else {
    // 已展开：进入 AI 助手
    uni.navigateTo({ url: '/pages/ai/chat' })
  }
}
</script>

<style lang="scss" scoped>
.home-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #6c4ee6 0%, #f5f5f7 300rpx);
}

/* 自定义导航栏 */
.nav-bar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  background: transparent; // 透明背景，和渐变融合
}

.nav-content {
  display: flex;
  align-items: center;
  padding: 0 24rpx 0 24rpx;
  height: 88rpx;
  gap: 16rpx;
  /* 右侧留出胶囊按钮的安全距离，避免被微信右上角菜单遮住 */
  padding-right: 200rpx;
}

/* 学校选择 */
.school-select {
  display: flex;
  align-items: center;
  padding: 6rpx 14rpx;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 28rpx;
  flex-shrink: 0;
}

.school-name {
  color: #fff;
  font-size: 24rpx;
  font-weight: 500;
  max-width: 120rpx;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.arrow {
  color: #fff;
  font-size: 18rpx;
  margin-left: 6rpx;
}

/* 搜索?*/
.search-box {
  flex: 1;
  display: flex;
  align-items: center;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 36rpx;
  padding: 12rpx 20rpx;
  gap: 10rpx;
}

.search-icon {
  font-size: 26rpx;
}

.search-placeholder {
  color: #999;
  font-size: 24rpx;
}

/* 滚动内容 */
.scroll-content {
  height: 100vh;
  box-sizing: border-box;
}

/* 轮播?*/
.banner-wrapper {
  padding: 20rpx 24rpx 0;
}

.banner-swiper {
  width: 100%;
  height: 320rpx;
  border-radius: 20rpx;
  overflow: hidden;
}

.banner-image {
  width: 100%;
  height: 100%;
  border-radius: 20rpx;
}

/* 公告?*/
.notice-bar {
  display: flex;
  align-items: center;
  margin: 20rpx 24rpx;
  padding: 20rpx 24rpx;
  background: #fff;
  border-radius: 16rpx;
  gap: 16rpx;
}

.notice-icon {
  font-size: 32rpx;
  flex-shrink: 0;
}

.notice-swiper {
  flex: 1;
  height: 40rpx;
}

.notice-text {
  color: #333;
  font-size: 26rpx;
  line-height: 40rpx;
}

/* 功能模块 */
.module-section {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 30rpx 0;
  padding: 30rpx 24rpx;
  background: #fff;
  margin: 0 24rpx;
  border-radius: 20rpx;
}

.module-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12rpx;
}

.module-icon {
  width: 96rpx;
  height: 96rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.module-emoji {
  font-size: 44rpx;
}

.module-name {
  font-size: 24rpx;
  color: #333;
}

/* 通用区块 */
.section {
  margin: 24rpx;
  padding: 24rpx;
  background: #fff;
  border-radius: 20rpx;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24rpx;
}

.section-title {
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
}

.section-more {
  display: flex;
  align-items: center;
  color: #999;
  font-size: 26rpx;
  gap: 4rpx;
}

.more-arrow {
  font-size: 30rpx;
}

/* 活动列表 */
.activity-list {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.activity-item {
  display: flex;
  gap: 20rpx;
  padding-bottom: 24rpx;
  border-bottom: 1rpx solid #f0f0f0;

  &:last-child {
    padding-bottom: 0;
    border-bottom: none;
  }
}

.activity-cover {
  width: 160rpx;
  height: 160rpx;
  border-radius: 12rpx;
  flex-shrink: 0;
}

.activity-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-width: 0;
}

.activity-title {
  font-size: 28rpx;
  font-weight: 500;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.activity-meta {
  font-size: 24rpx;
  color: #666;
}

.activity-btn {
  align-self: center;
  padding: 12rpx 24rpx;
  background: linear-gradient(135deg, #6c4ee6, #8b75f0);
  border-radius: 30rpx;
  color: #fff;
  font-size: 24rpx;
  flex-shrink: 0;
}

/* AI 助手悬浮球：默认半隐藏（一半露出屏幕），点击滑出后进入 */
.ai-fab {
  position: fixed;
  right: -48rpx;
  bottom: 160rpx;
  width: 96rpx;
  height: 96rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #12b7f5, #6c4ee6);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8rpx 24rpx rgba(108, 78, 230, 0.4);
  z-index: 999;
  transition: right 0.3s ease;
}

.ai-fab.is-expanded {
  right: 30rpx;
}

.ai-fab .ai-fab-icon {
  font-size: 44rpx;
  margin-left: 24rpx;
  transition: margin-left 0.3s ease;
}

.ai-fab.is-expanded .ai-fab-icon {
  margin-left: 0;
}
</style>