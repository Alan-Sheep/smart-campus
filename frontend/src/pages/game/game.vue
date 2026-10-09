<template>
  <!-- 游戏搭子页 -->
  <view class="game-page">
    <!-- 顶部Banner -->
    <view class="top-banner">
      <text class="banner-title">🎮 游戏搭子</text>
      <text class="banner-desc">找个搭子一起开黑！</text>
    </view>

    <!-- 游戏分类 -->
    <scroll-view scroll-x class="game-types">
      <view 
        class="type-item" 
        :class="{ active: currentType === item.id }"
        v-for="item in gameTypes" 
        :key="item.id"
        @click="selectType(item.id)"
      >
        {{ item.name }}
      </view>
    </scroll-view>

    <!-- 列表 -->
    <scroll-view scroll-y class="game-list">
      <view 
        class="game-card" 
        v-for="item in gameList" 
        :key="item.id"
        @click="goToDetail(item.id)"
      >
        <view class="card-header">
          <view class="game-tag">{{ item.gameName }}</view>
          <text class="level-tag">{{ item.level }}</text>
        </view>

        <text class="game-title">{{ item.title }}</text>
        <text class="game-desc">{{ item.content }}</text>

        <view class="game-meta">
          <text class="meta-item">🕹️ {{ item.gameType }}</text>
          <text class="meta-item">⭐ {{ item.level }}</text>
        </view>

        <view class="card-footer">
          <text class="publisher">发起人：{{ item.publisher }}</text>
          <view class="contact-btn" @click.stop="contactPlayer(item)">
            找TA开黑
          </view>
        </view>
      </view>

      <view style="height: 40rpx;"></view>
    </scroll-view>

    <!-- 发布按钮 -->
    <view class="publish-btn" @click="goPublish">
      <text class="publish-icon">+</text>
      <text>找搭子</text>
    </view>
  </view>
</template>

<script setup>
/**
 * 游戏搭子页
 * 找一起打游戏的小伙伴
 */
import { ref, onMounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { gameApi } from '@/api/index'
import { checkLogin } from '@/utils/request'
import { guardWrite, onSchoolChanged } from '@/utils/school-permission'

const currentType = ref('all')

// 游戏分类：从后端动态加载
const gameTypes = ref([
  { id: 'all', name: '全部' }
])

const gameList = ref([])
// 加载状态
const loading = ref(false)

onMounted(() => {
  loadGameTypes()
  loadGameList()
})

/**
 * 切换学校返回时重新加载
 * 修复：游戏分类和数据是按学校隔离的（gameTypes 按当前 School-Id 过滤），
 * 切换学校后不刷新会一直显示上一所学校的分类和数据。
 */
onShow(() => {
  onSchoolChanged()
  loadGameTypes()
  loadGameList()
})

// 从后端加载游戏分类
async function loadGameTypes() {
  try {
    const data = await gameApi.getGameTypes()
    const list = data.records || data.list || data || []
    gameTypes.value = [
      { id: 'all', name: '全部' },
      ...list.map(name => ({ id: name, name }))
    ]
  } catch (err) {
    console.warn('游戏分类加载失败，使用默认分类:', err)
    gameTypes.value = [
      { id: 'all', name: '全部' },
      { id: '王者荣耀', name: '王者荣耀' },
      { id: '英雄联盟', name: '英雄联盟' },
      { id: '原神', name: '原神' },
      { id: '和平精英', name: '和平精英' },
      { id: 'CSGO', name: 'CSGO' },
      { id: '永劫无间', name: '永劫无间' }
    ]
  }
}

/**
 * 加载游戏搭子列表
 */
async function loadGameList() {
  loading.value = true
  try {
    // 调用真实接口获取游戏搭子列表
    const data = await gameApi.getGameList({ gameType: currentType.value, pageNum: 1, pageSize: 10 })
    // 兼容数组或 { records: [] } / { list: [] } 格式
    gameList.value = data.records || data.list || data || []
  } catch (err) {
    console.warn('游戏搭子列表接口请求失败:', err)
    gameList.value = []
  } finally {
    loading.value = false
  }
}

function selectType(id) {
  currentType.value = id
  loadGameList()
}

function goToDetail(id) {
  uni.navigateTo({ url: '/pages/game/detail?id=' + id })
}

function contactPlayer(item) {
  if (!checkLogin()) return
  const targetUserId = item.userId
  if (targetUserId) {
    uni.navigateTo({
      url: '/pages/message/chat?targetUserId=' + targetUserId + '&targetNickname=' + encodeURIComponent(item.publisher || '游戏玩家')
    })
  } else {
    uni.showToast({ title: '无法获取发布人信息', icon: 'none' })
  }
}
function goPublish() {
  if (guardWrite()) return
  if (!checkLogin()) return
  uni.navigateTo({ url: '/pages/game/publish' })
}
</script>

<style lang="scss" scoped>
.game-page {
  min-height: 100vh;
  background: #f5f5f7;
  display: flex;
  flex-direction: column;
}

.top-banner {
  padding: 40rpx 24rpx;
  background: linear-gradient(135deg, #667eea, #764ba2);
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

.game-types {
  background: #fff;
  white-space: nowrap;
  padding: 20rpx 24rpx;
}

.type-item {
  display: inline-block;
  padding: 12rpx 28rpx;
  margin-right: 16rpx;
  background: #f5f5f7;
  border-radius: 30rpx;
  font-size: 26rpx;
  color: #666;

  &.active {
    background: #667eea;
    color: #fff;
  }
}

.game-list {
  flex: 1;
  padding: 20rpx 24rpx;
  box-sizing: border-box;
}

.game-card {
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

.game-tag {
  padding: 8rpx 20rpx;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  font-size: 24rpx;
  border-radius: 10rpx;
}

.level-tag {
  padding: 6rpx 16rpx;
  background: #fff7e6;
  color: #fa8c16;
  font-size: 22rpx;
  border-radius: 20rpx;
}

.game-title {
  font-size: 30rpx;
  font-weight: bold;
  color: #333;
  margin-bottom: 12rpx;
  display: block;
}

.game-desc {
  font-size: 26rpx;
  color: #666;
  line-height: 1.5;
  margin-bottom: 16rpx;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}

.game-meta {
  display: flex;
  gap: 24rpx;
  margin-bottom: 20rpx;
}

.meta-item {
  font-size: 24rpx;
  color: #999;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 20rpx;
  border-top: 1rpx solid #f0f0f0;
}

.publisher {
  font-size: 24rpx;
  color: #999;
}

.contact-btn {
  padding: 14rpx 36rpx;
  background: linear-gradient(135deg, #667eea, #764ba2);
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
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  border-radius: 50%;
  box-shadow: 0 8rpx 24rpx rgba(102, 126, 234, 0.4);
  font-size: 20rpx;
  z-index: 100;
}

.publish-icon {
  font-size: 40rpx;
  line-height: 1;
}
</style>
