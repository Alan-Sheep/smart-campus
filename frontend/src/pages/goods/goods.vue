<template>
  <!-- 二手市场列表?-->
  <view class="goods-page">
    <!-- 搜索?-->
    <view class="search-bar">
      <input 
        class="search-input" 
        placeholder="搜索二手商品" 
        v-model="keyword"
        @confirm="searchGoods"
      />
    </view>

    <!-- 分类筛?-->
    <scroll-view scroll-x class="category-list">
      <view 
        class="category-item"
        :class="{ active: currentCategory === item.id }"
        v-for="item in categories"
        :key="item.id"
        @click="switchCategory(item.id)"
      >
        {{ item.name }}
      </view>
    </scroll-view>

    <!-- 排序 -->
    <view class="sort-bar">
      <view class="sort-item" :class="{ active: sortBy === 'default' }" @click="changeSort('default')">
        综合
      </view>
      <view class="sort-item" :class="{ active: sortBy === 'price' }" @click="changeSort('price')">
        价格
        <text>{{ priceAsc ? '↑' : '↓' }}</text>
      </view>
      <view class="sort-item" :class="{ active: sortBy === 'new' }" @click="changeSort('new')">
        最新
      </view>
    </view>

    <!-- 商品列表 -->
    <view class="goods-grid">
      <view 
        class="goods-card" 
        v-for="item in goodsList" 
        :key="item.id"
        @click="goToDetail(item.id)"
      >
        <image :src="item.images && item.images[0]" class="goods-image" mode="aspectFill"></image>
        <view class="goods-info">
          <text class="goods-title">{{ item.title }}</text>
          <text class="goods-price">¥{{ item.price }}</text>
          <view class="goods-footer">
            <text class="goods-seller">{{ item.sellerName }}</text>
            <text class="goods-time">{{ item.createTime }}</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 发布按钮 -->
    <view class="publish-btn" @click="goPublish">
      <text class="publish-icon">+</text>
      <text>发布</text>
    </view>
  </view>
</template>

<script setup>
/**
 * 二手市场列表? */
import { ref, onMounted, onUnmounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { goodsApi } from '@/api/index'
import { checkLogin } from '@/utils/request'
import { guardWrite, onSchoolChanged } from '@/utils/school-permission'

const keyword = ref('')
const currentCategory = ref('all')
const sortBy = ref('default')
const priceAsc = ref(true)

// 分类数据：从后端拉取，失败用默认?
const categories = ref([
  { id: 'all', name: '全部' }
])

const goodsList = ref([])
const loading = ref(false)

onMounted(() => {
  loadCategories()
  loadGoodsList()
})
// ---- 切换学校后刷新 ----
// 用户切换学校返回本页时，重新拉取当前学校的数据
// hasLoadedOnce 避免与 onMounted 首屏加载重复请求
let hasLoadedOnce = false
onShow(() => {
  onSchoolChanged()
  if (hasLoadedOnce) { loadGoodsList() }
  hasLoadedOnce = true
})

// 从后端加载分?
async function loadCategories() {
  try {
    const data = await goodsApi.getGoodsCategory()
    const list = data.records || data.list || data || []
    categories.value = [
      { id: 'all', name: '全部' },
      ...list.map(item => ({ id: item.id, name: item.name }))
    ]
  } catch (err) {
    console.warn('商品分类加载失败，使用默认分?', err)
    categories.value = [
      { id: 'all', name: '全部' },
      { id: 1, name: '数码电子' },
      { id: 2, name: '书籍教材' },
      { id: 3, name: '生活用品' },
      { id: 4, name: '服装鞋包' },
      { id: 5, name: '运动器材' },
      { id: 6, name: '其他' }
    ]
  }
}

async function loadGoodsList() {
  loading.value = true
  try {
    const data = await goodsApi.getGoodsList({
      keyword: keyword.value,
      category: currentCategory.value,
      sortBy: sortBy.value,
      priceAsc: priceAsc.value,
      pageNum: 1,
      pageSize: 20
    })
    goodsList.value = data.records || data.list || data || []
  } catch (err) {
    console.warn('二手商品列表接口请求失败:', err)
    goodsList.value = []
  } finally {
    loading.value = false
  }
}

function switchCategory(id) {
  currentCategory.value = id
  loadGoodsList()
}

function changeSort(sort) {
  if (sortBy.value === sort && sort === 'price') {
    priceAsc.value = !priceAsc.value
  } else {
    sortBy.value = sort
    if (sort === 'price') priceAsc.value = true
  }
  loadGoodsList()
}

function searchGoods() {
  loadGoodsList()
}

function goToDetail(id) {
  uni.navigateTo({ url: '/pages/goods/detail?id=' + id })
}
function goPublish() {
  if (guardWrite()) return
  if (!checkLogin()) return
  uni.navigateTo({ url: '/pages/goods/publish' })
}

function loadMore() {
  // 加载更多
}
</script>

<style lang="scss" scoped>
.goods-page {
  min-height: 100vh;
  background: #f5f5f7;
  padding-bottom: 40rpx;
}

.search-bar {
  padding: 20rpx 24rpx;
  background: #fff;
}

.search-input {
  width: 100%;
  height: 72rpx;
  background: #f5f5f7;
  border-radius: 36rpx;
  padding: 0 30rpx;
  font-size: 28rpx;
  box-sizing: border-box;
}

.category-list {
  background: #fff;
  white-space: nowrap;
  padding: 16rpx 24rpx;
  border-bottom: 1rpx solid #eee;
}

.category-item {
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

.sort-bar {
  display: flex;
  background: #fff;
  padding: 20rpx 24rpx;
  border-bottom: 1rpx solid #eee;
}

.sort-item {
  flex: 1;
  text-align: center;
  font-size: 26rpx;
  color: #666;

  &.active {
    color: #6c4ee6;
    font-weight: bold;
  }
}

.goods-grid {
  display: flex;
  flex-wrap: wrap;
  padding: 20rpx 16rpx;
  gap: 16rpx;
}

.goods-card {
  width: calc(50% - 8rpx);
  background: #fff;
  border-radius: 16rpx;
  overflow: hidden;
  margin-bottom: 16rpx;
}

.goods-image {
  width: 100%;
  height: 340rpx;
}

.goods-info {
  padding: 16rpx;
}

.goods-title {
  font-size: 26rpx;
  color: #333;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
  line-height: 1.4;
  height: 72rpx;
  margin-bottom: 12rpx;
}

.goods-price {
  font-size: 32rpx;
  font-weight: bold;
  color: #ff4d4f;
  display: block;
  margin-bottom: 12rpx;
}

.goods-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.goods-seller {
  font-size: 22rpx;
  color: #999;
}

.goods-time {
  font-size: 22rpx;
  color: #bbb;
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
  background: linear-gradient(135deg, #6c4ee6, #8b5cf6);
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