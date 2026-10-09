<template>
  <view class="collection-page">
    <!-- 分类 tab -->
    <view class="tab-bar">
      <view
        class="tab-item"
        :class="{ active: currentTab === 'goods' }"
        @click="switchTab('goods')"
      >
        <text>商品收藏</text>
      </view>
      <view
        class="tab-item"
        :class="{ active: currentTab === 'post' }"
        @click="switchTab('post')"
      >
        <text>帖子收藏</text>
      </view>
      <view
        class="tab-item"
        :class="{ active: currentTab === 'activity' }"
        @click="switchTab('activity')"
      >
        <text>活动收藏</text>
      </view>
    </view>

    <!-- 列表内容 -->
    <view class="list-content" v-if="list.length > 0">
      <view
        class="list-item"
        v-for="item in list"
        :key="item.id"
        @click="goDetail(item)"
      >
        <view class="item-thumb" v-if="item.targetCover">
          <image :src="item.targetCover" mode="aspectFill" class="thumb-img"></image>
        </view>
        <view class="item-content">
          <text class="item-title">{{ item.targetTitle || '（内容已删除）' }}</text>
          <text class="item-desc">{{ item.targetDesc || '' }}</text>
          <view class="item-footer">
            <text class="item-time">{{ item.createTime || '' }}</text>
          </view>
        </view>
        <view class="cancel-collect" @click.stop="cancelCollect(item)">
          <text>取消收藏</text>
        </view>
      </view>
    </view>

    <!-- 空状态 -->
    <view class="empty-state" v-else-if="!loading">
      <text class="empty-icon">⭐</text>
      <text class="empty-title">暂无收藏</text>
      <text class="empty-desc">快去发现你喜欢的内容吧~</text>
      <button class="go-btn" @click="goExplore">去逛逛</button>
    </view>

    <!-- 加载中 -->
    <view class="loading-state" v-if="loading">
      <text>加载中...</text>
    </view>
  </view>
</template>

<script setup>
/**
 * 我的收藏页面
 * 支持商品、帖子、活动收藏的查看和取消收藏
 *
 * 数据来源：/api/channel/myCollects（collect 表，targetType 区分类型）
 * 目标标题/封面由后端回填到 targetTitle / targetCover / targetDesc
 */
import { ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { channelApi } from '@/api/index'
import { checkLogin } from '@/utils/request'

// tab 与后端 targetType 的对应关系：0商品 1帖子 2活动
const TAB_TYPE = { goods: 0, post: 1, activity: 2 }

const list = ref([])
const loading = ref(false)
const currentTab = ref('post')

onLoad(() => {
  currentTab.value = 'post'
})

// 用 onShow：从详情页取消收藏返回时能同步刷新
onShow(() => {
  if (checkLogin(false)) {
    loadList()
  }
})

/**
 * 加载收藏列表
 */
async function loadList() {
  loading.value = true
  try {
    const data = await channelApi.getMyCollects({
      targetType: TAB_TYPE[currentTab.value]
    })
    list.value = Array.isArray(data) ? data : (data.records || data.list || [])
  } catch (err) {
    console.warn('收藏列表加载失败:', err)
    list.value = []
  } finally {
    loading.value = false
  }
}

function switchTab(tab) {
  if (currentTab.value === tab) return
  currentTab.value = tab
  loadList()
}

function goDetail(item) {
  const pageMap = {
    goods: '/pages/goods/detail?id=',
    post: '/pages/channel/detail?id=',
    activity: '/pages/activity/detail?id='
  }
  const url = pageMap[currentTab.value]
  if (url && item.targetId) {
    uni.navigateTo({ url: url + item.targetId })
  }
}

/**
 * 取消收藏
 * 直接调用收藏接口做切换（已收藏状态再调一次即取消）
 */
function cancelCollect(item) {
  uni.showModal({
    title: '提示',
    content: '确定要取消收藏吗？',
    success: async (res) => {
      if (!res.confirm) return
      try {
        // 目前帖子收藏已实现；商品收藏接口后端尚未提供
        if (currentTab.value === 'post') {
          await channelApi.toggleCollect(item.targetId)
        }
        list.value = list.value.filter(i => i.id !== item.id)
        uni.showToast({ title: '已取消收藏', icon: 'success' })
      } catch (err) {
        console.warn('取消收藏失败:', err)
      }
    }
  })
}

function goExplore() {
  uni.switchTab({ url: '/pages/index/index' })
}
</script>

<style lang="scss" scoped>
.collection-page {
  min-height: 100vh;
  background: #f5f5f7;
}

/* 顶部 tab */
.tab-bar {
  display: flex;
  background: #fff;
  position: sticky;
  top: 0;
  z-index: 10;
}

.tab-item {
  flex: 1;
  text-align: center;
  padding: 24rpx 0;
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
      width: 48rpx;
      height: 6rpx;
      background: #6c4ee6;
      border-radius: 3rpx;
    }
  }
}

/* 列表内容 */
.list-content {
  padding: 20rpx 24rpx;
}

.list-item {
  display: flex;
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 16rpx;
  gap: 20rpx;
  position: relative;
}

.item-thumb {
  flex-shrink: 0;
  width: 160rpx;
  height: 160rpx;
  border-radius: 12rpx;
  overflow: hidden;
}

.thumb-img {
  width: 100%;
  height: 100%;
}

.item-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 10rpx;
  min-width: 0;
}

.item-title {
  font-size: 30rpx;
  font-weight: bold;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-desc {
  font-size: 24rpx;
  color: #666;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.4;
}

.item-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: auto;
}

.item-price {
  font-size: 28rpx;
  color: #ff4d4f;
  font-weight: bold;
}

.item-time {
  font-size: 22rpx;
  color: #999;
}

.cancel-collect {
  position: absolute;
  right: 24rpx;
  bottom: 24rpx;
  padding: 8rpx 20rpx;
  font-size: 22rpx;
  color: #999;
  border: 1rpx solid #ddd;
  border-radius: 20rpx;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 160rpx 60rpx;
  gap: 20rpx;
}

.empty-icon {
  font-size: 120rpx;
}

.empty-title {
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
  margin-top: 20rpx;
}

.empty-desc {
  font-size: 26rpx;
  color: #999;
}

.go-btn {
  margin-top: 40rpx;
  width: 300rpx;
  height: 80rpx;
  line-height: 80rpx;
  background: #6c4ee6;
  color: #fff;
  font-size: 28rpx;
  border-radius: 40rpx;

  &::after {
    border: none;
  }
}

/* 加载中 */
.loading-state {
  text-align: center;
  padding: 60rpx 0;
  font-size: 26rpx;
  color: #999;
}
</style>
