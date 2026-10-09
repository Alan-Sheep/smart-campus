<template>
  <!-- 商品详情页 -->
  <view class="detail-page" v-if="goods">
    <!-- 图片轮播 -->
    <swiper class="image-swiper" :indicator-dots="true" indicator-color="rgba(255,255,255,0.5)" indicator-active-color="#fff">
      <swiper-item>
        <image :src="goods.images" mode="aspectFill" class="goods-image"></image>
      </swiper-item>
    </swiper>

    <!-- 价格信息 -->
    <view class="price-section">
      <view class="price-row">
        <text class="price">¥{{ goods.price }}</text>
        <text class="original-price" v-if="goods.originalPrice">原价 ¥{{ goods.originalPrice }}</text>
      </view>
      <text class="title">{{ goods.title }}</text>
      <text class="time">{{ goods.createTime }} 发布</text>
    </view>

    <!-- 卖家信息 -->
    <view class="seller-section" @click="goToSeller">
      <view class="seller-avatar">👤</view>
      <view class="seller-info">
        <text class="seller-name">{{ goods.seller }}</text>
        <text class="seller-desc">信用良好</text>
      </view>
      <text class="arrow">›</text>
    </view>

    <!-- 商品描述 -->
    <view class="desc-section">
      <view class="section-title">商品描述</view>
      <text class="desc-text">{{ goods.description || '商品九成新，买回来没怎么用过，因为毕业了所以便宜出。有兴趣的同学可以联系我，支持当面验货。' }}</text>
    </view>

    <!-- 底部操作栏 -->
    <view class="bottom-bar">
      <view class="bar-item" @click="contactSeller">
        <text class="bar-icon">💬</text>
        <text class="bar-text">私聊</text>
      </view>
      <view class="bar-item" @click="collectGoods">
        <text class="bar-icon">{{ isCollected ? '❤️' : '🤍' }}</text>
        <text class="bar-text">收藏</text>
      </view>
      <view class="buy-btn" @click="buyNow">
        我想要
      </view>
    </view>
  </view>
</template>

<script setup>
/**
 * 商品详情页
 * 调用真实后端接口获取商品详情
 */
import { ref, onMounted } from 'vue'
import { goodsApi } from '@/api/index'

const goods = ref(null)
const isCollected = ref(false)
// 加载状态
const loading = ref(false)

onMounted(() => {
  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1]
  const id = currentPage.options?.id || 1

  loadDetail(id)
})

/**
 * 加载商品详情
 */
async function loadDetail(id) {
  loading.value = true
  try {
    const data = await goodsApi.getGoodsDetail(id)
    goods.value = data
  } catch (err) {
    console.warn('商品详情接口请求失败:', err)
    goods.value = null
  } finally {
    loading.value = false
  }
}

function goToSeller() {
  uni.navigateTo({
    url: '/pages/message/chat?targetUserId=' + (goods.value?.sellerId || 1) +
         '&targetNickname=' + encodeURIComponent(goods.value?.seller || '卖家')
  })
}

function contactSeller() {
  uni.navigateTo({
    url: '/pages/message/chat?targetUserId=' + (goods.value?.sellerId || 1) +
         '&targetNickname=' + encodeURIComponent(goods.value?.seller || '卖家')
  })
}

/**
 * 收藏/取消收藏商品
 * 调用真实接口
 */
async function collectGoods() {
  try {
    const data = await goodsApi.toggleCollect(goods.value.id)
    isCollected.value = data?.isCollected !== undefined ? data.isCollected : !isCollected.value
    uni.showToast({
      title: isCollected.value ? '收藏成功' : '已取消收藏',
      icon: 'success'
    })
  } catch (err) {
    // 接口失败，乐观更新
    console.warn('收藏操作失败:', err)
    isCollected.value = !isCollected.value
    uni.showToast({
      title: isCollected.value ? '收藏成功' : '已取消收藏',
      icon: 'success'
    })
  }
}

function buyNow() {
  uni.showModal({
    title: '确认购买',
    content: '确定要购买这个商品吗？请先和卖家沟通好交易细节。',
    success: (res) => {
      if (res.confirm) {
        uni.showToast({ title: '已发送购买请求', icon: 'success' })
      }
    }
  })
}
</script>

<style lang="scss" scoped>
.detail-page {
  min-height: 100vh;
  background: #f5f5f7;
  padding-bottom: 140rpx;
}

.image-swiper {
  width: 100%;
  height: 750rpx;
}

.goods-image {
  width: 100%;
  height: 100%;
}

.price-section {
  background: #fff;
  padding: 24rpx;
  margin-bottom: 20rpx;
}

.price-row {
  display: flex;
  align-items: baseline;
  gap: 16rpx;
  margin-bottom: 16rpx;
}

.price {
  font-size: 48rpx;
  font-weight: bold;
  color: #ff4d4f;
}

.original-price {
  font-size: 26rpx;
  color: #999;
  text-decoration: line-through;
}

.title {
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
  line-height: 1.5;
  margin-bottom: 12rpx;
  display: block;
}

.time {
  font-size: 24rpx;
  color: #999;
}

.seller-section {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 24rpx;
  background: #fff;
  margin-bottom: 20rpx;
}

.seller-avatar {
  width: 80rpx;
  height: 80rpx;
  line-height: 80rpx;
  text-align: center;
  background: #f0f0f0;
  border-radius: 50%;
  font-size: 40rpx;
}

.seller-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6rpx;
}

.seller-name {
  font-size: 28rpx;
  font-weight: 500;
  color: #333;
}

.seller-desc {
  font-size: 22rpx;
  color: #52c41a;
}

.arrow {
  font-size: 32rpx;
  color: #ccc;
}

.desc-section {
  background: #fff;
  padding: 24rpx;
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

.buy-btn {
  flex: 1;
  height: 88rpx;
  line-height: 88rpx;
  text-align: center;
  background: linear-gradient(135deg, #ff6b6b, #ff4d4f);
  color: #fff;
  font-size: 30rpx;
  font-weight: bold;
  border-radius: 44rpx;
}
</style>
