<template>
  <!-- 搜索页 -->
  <view class="search-page">
    <!-- 搜索栏 -->
    <view class="search-bar">
      <view class="search-input-wrap">
        <text class="search-icon">🔍</text>
        <input
          class="search-input"
          placeholder="搜索商品、跑腿、活动..."
          v-model="keyword"
          focus
          @confirm="doSearch"
        />
        <text class="clear-icon" v-if="keyword" @click="clearKeyword">×</text>
      </view>
      <text class="cancel-btn" @click="goBack">取消</text>
    </view>

    <!-- 搜索历史 -->
    <view class="history-section" v-if="!hasSearched && searchHistory.length > 0">
      <view class="section-header">
        <text class="section-title">搜索历史</text>
        <text class="clear-history" @click="clearHistory">清空</text>
      </view>
      <view class="history-tags">
        <view
          class="history-tag"
          v-for="(item, index) in searchHistory"
          :key="index"
          @click="searchByHistory(item)"
        >
          {{ item }}
        </view>
      </view>
    </view>

    <!-- 热门搜索 -->
    <view class="hot-section" v-if="!hasSearched">
      <view class="section-header">
        <text class="section-title">热门搜索</text>
      </view>
      <view class="hot-tags">
        <view
          class="hot-tag"
          v-for="(item, index) in hotKeywords"
          :key="index"
          @click="searchByHistory(item)"
        >
          <text class="hot-rank" :class="'rank-' + (index + 1)">{{ index + 1 }}</text>
          <text class="hot-text">{{ item }}</text>
        </view>
      </view>
    </view>

    <!-- 搜索结果 -->
    <view class="result-section" v-if="hasSearched">
      <!-- 分类切换 -->
      <scroll-view scroll-x class="result-tabs">
        <view
          class="result-tab"
          :class="{ active: currentTab === item.id }"
          v-for="item in tabList"
          :key="item.id"
          @click="switchTab(item.id)"
        >
          {{ item.name }}
        </view>
      </scroll-view>

      <!-- 无结果提示 -->
      <view class="empty-result" v-if="searchResult.length === 0 && !loading">
        <text class="empty-icon">🔍</text>
        <text class="empty-text">没有找到相关结果</text>
        <text class="empty-tip">换个关键词试试吧</text>
      </view>

      <!-- 搜索结果列表 -->
      <scroll-view scroll-y class="result-list" v-if="searchResult.length > 0">
        <view
          class="result-item"
          v-for="item in searchResult"
          :key="item.sourceModule + '-' + item.id"
          @click="goToDetail(item)"
        >
          <image v-if="item.cover || item.image" :src="item.cover || item.image" class="result-cover" mode="aspectFill"></image>
          <view class="result-info">
            <view class="result-title-row">
              <text class="result-source-tag" :class="'tag-' + item.sourceModule">{{ item.sourceLabel }}</text>
              <text class="result-title">{{ item.title }}</text>
            </view>
            <text class="result-desc" v-if="item.description || item.desc">{{ item.description || item.desc }}</text>
            <view class="result-meta">
              <text class="result-price" v-if="item.price">¥{{ item.price }}</text>
              <text class="result-time" v-if="item.createTime || item.time">{{ item.createTime || item.time }}</text>
            </view>
          </view>
        </view>
        <view style="height: 40rpx;"></view>
      </scroll-view>
    </view>
  </view>
</template>

<script setup>
/**
 * 搜索页
 * 支持搜索二手商品、跑腿、活动等
 */
import { ref, onMounted } from 'vue'
import { goodsApi, errandApi, activityApi } from '@/api/index'

const keyword = ref('')
const hasSearched = ref(false)
const loading = ref(false)
const searchResult = ref([])
const currentTab = ref('all')

const tabList = ref([
  { id: 'all', name: '全部' },
  { id: 'goods', name: '二手商品' },
  { id: 'errand', name: '校园跑腿' },
  { id: 'activity', name: '校园活动' }
])

const searchHistory = ref([])

const hotKeywords = ref([
  'iPad',
  '快递代取',
  '奶茶代购',
  '考研资料',
  '自行车',
  '王者荣耀',
  '校园活动',
  '二手书'
])

onMounted(() => {
  // 从本地缓存加载搜索历史
  const history = uni.getStorageSync('searchHistory')
  if (history) {
    try {
      searchHistory.value = JSON.parse(history)
    } catch (e) {
      searchHistory.value = []
    }
  }
})

/**
 * 执行搜索（用户主动搜索，保存历史）
 */
async function doSearch() {
  const kw = keyword.value.trim()
  if (!kw) {
    uni.showToast({ title: '请输入搜索关键词', icon: 'none' })
    return
  }

  // 保存搜索历史
  saveHistory(kw)

  await fetchResults(kw)
}

/**
 * 根据当前选中的分类 tab 拉取搜索结果
 * - 全部：Promise.all 同时调用商品、跑腿、活动三个接口，合并结果
 * - 二手商品 / 校园跑腿 / 校园活动：只调用对应模块接口
 * 每个模块独立 try/catch，某模块失败不影响其他模块
 */
async function fetchResults(kw) {
  hasSearched.value = true
  loading.value = true
  searchResult.value = []

  try {
    if (currentTab.value === 'all') {
      // 同时调用三个接口，互不影响（各 fetch 内部已捕获异常）
      const [goodsList, errandList, activityList] = await Promise.all([
        fetchGoods(kw),
        fetchErrand(kw),
        fetchActivity(kw)
      ])
      searchResult.value = [...goodsList, ...errandList, ...activityList]
    } else if (currentTab.value === 'goods') {
      searchResult.value = await fetchGoods(kw)
    } else if (currentTab.value === 'errand') {
      searchResult.value = await fetchErrand(kw)
    } else if (currentTab.value === 'activity') {
      searchResult.value = await fetchActivity(kw)
    }
  } catch (err) {
    console.warn('搜索失败:', err)
    searchResult.value = []
  } finally {
    loading.value = false
  }
}

/**
 * 搜索二手商品
 */
async function fetchGoods(kw) {
  try {
    const data = await goodsApi.getGoodsList({ keyword: kw, pageNum: 1, pageSize: 20 })
    const list = data.records || data.list || data || []
    return list.map(normalizeGoods)
  } catch (err) {
    console.warn('商品搜索失败:', err)
    return []
  }
}

/**
 * 搜索校园跑腿
 */
async function fetchErrand(kw) {
  try {
    const data = await errandApi.getErrandList({ type: 'hall', keyword: kw, pageNum: 1, pageSize: 20 })
    const list = data.records || data.list || data || []
    return list.map(normalizeErrand)
  } catch (err) {
    console.warn('跑腿搜索失败:', err)
    return []
  }
}

/**
 * 搜索校园活动
 */
async function fetchActivity(kw) {
  try {
    const data = await activityApi.getActivityList({ keyword: kw, pageNum: 1, pageSize: 20 })
    const list = data.records || data.list || data || []
    return list.map(normalizeActivity)
  } catch (err) {
    console.warn('活动搜索失败:', err)
    return []
  }
}

/**
 * 统一商品结果字段，标注来源模块
 */
function normalizeGoods(item) {
  return {
    ...item,
    id: item.id,
    sourceModule: 'goods',
    sourceLabel: '商品',
    cover: (item.images && item.images[0]) || item.cover || item.image || '',
    title: item.title || '未命名商品',
    description: item.description || item.desc || '',
    price: item.price,
    time: item.createTime || item.time || ''
  }
}

/**
 * 统一跑腿结果字段，标注来源模块
 */
function normalizeErrand(item) {
  return {
    ...item,
    id: item.id,
    sourceModule: 'errand',
    sourceLabel: '跑腿',
    cover: item.cover || item.image || '',
    title: item.title || '跑腿订单',
    description: item.description || item.desc || '',
    price: item.reward,
    time: item.estimateTime || item.createTime || item.time || ''
  }
}

/**
 * 统一活动结果字段，标注来源模块
 */
function normalizeActivity(item) {
  return {
    ...item,
    id: item.id,
    sourceModule: 'activity',
    sourceLabel: '活动',
    cover: item.cover || item.image || '',
    title: item.title || '校园活动',
    description: item.description || item.desc || '',
    price: item.price,
    time: item.time || item.date || item.createTime || ''
  }
}

/**
 * 保存搜索历史
 */
function saveHistory(kw) {
  const history = searchHistory.value.filter(item => item !== kw)
  history.unshift(kw)
  // 最多保留 10 条
  if (history.length > 10) {
    history.length = 10
  }
  searchHistory.value = history
  uni.setStorageSync('searchHistory', JSON.stringify(history))
}

/**
 * 通过历史记录搜索
 */
function searchByHistory(kw) {
  keyword.value = kw
  doSearch()
}

/**
 * 清空搜索历史
 */
function clearHistory() {
  uni.showModal({
    title: '提示',
    content: '确定要清空搜索历史吗？',
    success: (res) => {
      if (res.confirm) {
        searchHistory.value = []
        uni.removeStorageSync('searchHistory')
      }
    }
  })
}

/**
 * 清空关键词
 */
function clearKeyword() {
  keyword.value = ''
  hasSearched.value = false
  searchResult.value = []
}

/**
 * 返回
 */
function goBack() {
  uni.navigateBack()
}

/**
 * 切换分类，重新拉取对应模块结果
 */
function switchTab(id) {
  currentTab.value = id
  const kw = keyword.value.trim()
  if (kw && hasSearched.value) {
    fetchResults(kw)
  }
}

/**
 * 跳转到详情（根据来源模块路由）
 */
function goToDetail(item) {
  const module = item.sourceModule
  if (module === 'errand') {
    uni.navigateTo({ url: '/pages/errand/detail?id=' + item.id })
  } else if (module === 'activity') {
    uni.navigateTo({ url: '/pages/activity/detail?id=' + item.id })
  } else {
    // goods 或未知类型默认商品详情
    uni.navigateTo({ url: '/pages/goods/detail?id=' + item.id })
  }
}
</script>

<style lang="scss" scoped>
.search-page {
  min-height: 100vh;
  background: #f5f5f7;
}

/* 搜索栏 */
.search-bar {
  display: flex;
  align-items: center;
  padding: 20rpx 24rpx;
  background: #fff;
  gap: 20rpx;
  position: sticky;
  top: 0;
  z-index: 10;
}

.search-input-wrap {
  flex: 1;
  display: flex;
  align-items: center;
  background: #f5f5f7;
  border-radius: 36rpx;
  padding: 0 24rpx;
  height: 72rpx;
  gap: 12rpx;
}

.search-icon {
  font-size: 28rpx;
}

.search-input {
  flex: 1;
  height: 72rpx;
  font-size: 28rpx;
  color: #333;
}

.clear-icon {
  font-size: 36rpx;
  color: #ccc;
  line-height: 1;
}

.cancel-btn {
  font-size: 28rpx;
  color: #6c4ee6;
  flex-shrink: 0;
}

/* 通用 section 样式 */
.history-section,
.hot-section {
  background: #fff;
  margin-top: 20rpx;
  padding: 24rpx;
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
}

.clear-history {
  font-size: 24rpx;
  color: #999;
}

/* 搜索历史标签 */
.history-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}

.history-tag {
  padding: 12rpx 28rpx;
  background: #f5f5f7;
  border-radius: 30rpx;
  font-size: 26rpx;
  color: #666;
}

/* 热门搜索 */
.hot-tags {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.hot-tag {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.hot-rank {
  width: 36rpx;
  height: 36rpx;
  line-height: 36rpx;
  text-align: center;
  font-size: 22rpx;
  font-weight: bold;
  border-radius: 8rpx;
  background: #f0f0f0;
  color: #999;

  &.rank-1 {
    background: #ff4d4f;
    color: #fff;
  }
  &.rank-2 {
    background: #fa8c16;
    color: #fff;
  }
  &.rank-3 {
    background: #fadb14;
    color: #fff;
  }
}

.hot-text {
  font-size: 28rpx;
  color: #333;
}

/* 搜索结果 */
.result-section {
  flex: 1;
}

.result-tabs {
  background: #fff;
  white-space: nowrap;
  padding: 16rpx 24rpx;
  position: sticky;
  top: 112rpx;
  z-index: 9;
}

.result-tab {
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

.result-list {
  padding: 20rpx 24rpx;
  height: calc(100vh - 200rpx);
  box-sizing: border-box;
}

.result-item {
  display: flex;
  gap: 20rpx;
  background: #fff;
  border-radius: 16rpx;
  padding: 20rpx;
  margin-bottom: 16rpx;
}

.result-cover {
  width: 160rpx;
  height: 160rpx;
  border-radius: 12rpx;
  flex-shrink: 0;
  background: #f5f5f7;
}

.result-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-width: 0;
}

.result-title-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.result-source-tag {
  flex-shrink: 0;
  padding: 2rpx 12rpx;
  border-radius: 6rpx;
  font-size: 20rpx;
  line-height: 1.6;
  color: #fff;
  background: #999;

  &.tag-goods {
    background: #6c4ee6;
  }
  &.tag-errand {
    background: #fa8c16;
  }
  &.tag-activity {
    background: #13c2c2;
  }
}

.result-title {
  flex: 1;
  min-width: 0;
  font-size: 28rpx;
  font-weight: 500;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.result-desc {
  font-size: 24rpx;
  color: #999;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
  line-height: 1.4;
}

.result-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.result-price {
  font-size: 32rpx;
  font-weight: bold;
  color: #ff4d4f;
}

.result-time {
  font-size: 22rpx;
  color: #bbb;
}

/* 空结果 */
.empty-result {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 120rpx 0;
  gap: 16rpx;
}

.empty-icon {
  font-size: 80rpx;
}

.empty-text {
  font-size: 30rpx;
  color: #666;
}

.empty-tip {
  font-size: 24rpx;
  color: #999;
}
</style>
