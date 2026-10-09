<template>
  <view class="my-list-page">
    <!-- 顶部 tab：发布的 / 接单的 -->
    <view class="top-tabs" v-if="showRoleTabs">
      <view
        class="tab-item"
        :class="{ active: currentRole === 'publish' }"
        @click="switchRole('publish')"
      >我发布的</view>
      <view
        class="tab-item"
        :class="{ active: currentRole === 'accept' }"
        @click="switchRole('accept')"
      >我接单的</view>
    </view>

    <!-- 状态筛选 tab -->
    <view class="status-tabs">
      <scroll-view scroll-x class="status-scroll">
        <view
          class="status-item"
          :class="{ active: currentStatus === item.value }"
          v-for="item in statusList"
          :key="item.value"
          @click="switchStatus(item.value)"
        >
          {{ item.label }}
        </view>
      </scroll-view>
    </view>

    <!-- 列表内容 -->
    <view class="list-content" v-if="list.length > 0">
      <view
        class="list-item"
        v-for="item in list"
        :key="item.id"
        @click="goDetail(item)"
      >
        <!-- 左侧图片/图标 -->
        <view class="item-thumb" v-if="getItemCover(item)">
          <image :src="getItemCover(item)" mode="aspectFill" class="thumb-img"></image>
        </view>
        <view class="item-icon" v-else>{{ getItemIcon() }}</view>

        <!-- 右侧内容 -->
        <view class="item-content">
          <view class="item-header">
            <text class="item-title">{{ getItemTitle(item) }}</text>
            <view class="item-status" :class="'status-' + getItemStatus(item)">
              {{ getStatusText(item) }}
            </view>
          </view>
          <text class="item-desc">{{ getItemDesc(item) }}</text>
          <view class="item-footer">
            <text class="item-time">{{ getItemTime(item) }}</text>
            <text class="item-extra" v-if="getItemExtra(item)">{{ getItemExtra(item) }}</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 空状态 -->
    <view class="empty-state" v-else-if="!loading">
      <text class="empty-icon">📭</text>
      <text class="empty-text">暂无数据</text>
    </view>

    <!-- 加载中 -->
    <view class="loading-state" v-if="loading">
      <text>加载中...</text>
    </view>
  </view>
</template>

<script setup>
/**
 * 通用"我的列表"页面
 * 通过 URL 参数 type 区分模块：
 *   - express    快递代取
 *   - errand     校园跑腿
 *   - substitute 代课服务
 *   - activity   校园活动
 *   - goods      二手闲置
 *   - game       游戏搭子
 *   - lostfound  失物招领
 *   - post       我的帖子
 */
import { ref, computed, onMounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import {
  expressApi,
  errandApi,
  substituteApi,
  activityApi,
  goodsApi,
  gameApi,
  lostFoundApi,
  channelApi,
  printApi
} from '@/api/index'
import { checkLogin } from '@/utils/request'

const list = ref([])
const loading = ref(false)
const currentRole = ref('publish') // publish=我发布的 / accept=我接单的
const currentStatus = ref('all')
const moduleType = ref('')

// 每个模块的状态选项
// 注意：value 必须是后端能接受的数字（与库中 status 语义一致），
// 早期用 'pending'/'progress' 这类字符串直接发给后端，会触发
// "参数格式不正确：status" 报错。
const statusMap = {
  express: [
    { label: '全部', value: 'all' },
    { label: '待接单', value: 1 },
    { label: '已接单', value: 2 },
    { label: '已完成', value: 3 },
    { label: '已取消', value: 4 },
    { label: '已退款', value: 5 }
  ],
  print: [
    { label: '全部', value: 'all' },
    { label: '待接单', value: 1 },
    { label: '已接单', value: 2 },
    { label: '已完成', value: 3 },
    { label: '已取消', value: 4 },
    { label: '已退款', value: 5 }
  ],
  errand: [
    { label: '全部', value: 'all' },
    { label: '待接单', value: 1 },
    { label: '进行中', value: 2 },
    { label: '已完成', value: 3 },
    { label: '已取消', value: 0 }
  ],
  substitute: [
    { label: '全部', value: 'all' },
    { label: '待接单', value: 1 },
    { label: '已接单', value: 2 },
    { label: '已完成', value: 3 },
    { label: '已取消', value: 0 }
  ],
  activity: [
    { label: '全部', value: 'all' },
    { label: '报名中', value: 1 },
    { label: '进行中', value: 2 },
    { label: '已结束', value: 0 }
  ],
  goods: [
    { label: '全部', value: 'all' },
    { label: '在售中', value: 0 },
    { label: '已售出', value: 1 },
    { label: '已下架', value: 2 }
  ],
  game: [
    { label: '全部', value: 'all' },
    { label: '招募中', value: 0 },
    { label: '已组队', value: 1 }
  ],
  lostfound: [
    { label: '全部', value: 'all' },
    { label: '寻找中', value: '0' },
    { label: '已找到', value: '1' }
  ],
  post: [
    { label: '全部', value: 'all' }
  ]
}

const statusList = computed(() => statusMap[moduleType.value] || [])

// 是否显示角色 tab（只有有接单功能的模块才显示）
const showRoleTabs = computed(() => {
  return ['express', 'errand', 'substitute'].includes(moduleType.value)
})

onLoad((options) => {
  moduleType.value = options.type || 'express'
  setPageTitle()
  loadList()
})

function setPageTitle() {
  const titleMap = {
    express: '我的快递',
    print: '我的打印',
    errand: '我的跑腿',
    substitute: '我的代课',
    activity: '我的活动',
    goods: '我的闲置',
    game: '游戏搭子',
    lostfound: '失物招领',
    post: '我的帖子'
  }
  uni.setNavigationBarTitle({ title: titleMap[moduleType.value] || '我的' })
}

/**
 * 切换角色（发布/接单）
 */
function switchRole(role) {
  currentRole.value = role
  currentStatus.value = 'all'
  loadList()
}

/**
 * 切换状态
 */
function switchStatus(status) {
  currentStatus.value = status
  loadList()
}

/**
 * 加载列表
 *
 * 关键修复：
 *  1. 「我发布的 / 我接单的」此前没有真正生效——各模块后端约定的 type 值并不统一：
 *     快递用 publish/pick，跑腿与代课用 publish/accept。
 *     之前直接把前端角色 'accept' 透传给快递，后端识别不了，于是永远返回全部订单。
 *  2. 各模块的 status 之前传的是 'pending' 这类字符串，后端要数字，会报
 *     “参数格式不正确：status”。现在按状态选项传数字。
 */
async function loadList() {
  if (!checkLogin()) return

  loading.value = true
  try {
    let data = []
    const type = moduleType.value
    const role = currentRole.value
    const status = currentStatus.value !== 'all' ? currentStatus.value : undefined

    if (type === 'express') {
      // 快递后端：publish=我发布的, pick=我接单的
      data = await expressApi.getExpressList({
        type: role === 'accept' ? 'pick' : 'publish',
        status,
        pageNum: 1,
        pageSize: 20
      })
    } else if (type === 'print') {
      data = await printApi.getMyOrders({
        status,
        pageNum: 1,
        pageSize: 20
      })
    } else if (type === 'errand') {
      // 跑腿后端：publish / accept
      data = await errandApi.getErrandList({
        type: role,
        status,
        pageNum: 1,
        pageSize: 20
      })
    } else if (type === 'substitute') {
      // 代课后端：publish / accept
      data = await substituteApi.getSubstituteList({
        type: role,
        status,
        pageNum: 1,
        pageSize: 20
      })
    } else if (type === 'activity') {
      data = await activityApi.getMyActivities({ pageNum: 1, pageSize: 20 })
    } else if (type === 'goods') {
      data = await goodsApi.getMyGoods({
        status,
        pageNum: 1,
        pageSize: 20
      })
    } else if (type === 'game') {
      data = await gameApi.getMyPosts({ pageNum: 1, pageSize: 20 })
    } else if (type === 'lostfound') {
      data = await lostFoundApi.getMyPosts({ pageNum: 1, pageSize: 20 })
    } else if (type === 'post') {
      data = await channelApi.getMyPosts({ pageNum: 1, pageSize: 20 })
    }

    list.value = data.records || data.list || data || []
  } catch (err) {
    console.warn('加载我的列表失败:', err)
    list.value = []
  } finally {
    loading.value = false
  }
}

// ============== 列表项展示辅助函数 ==============

function getItemIcon() {
  const icons = {
    express: '📦', print: '🖨️', errand: '🏃', substitute: '📚',
    activity: '🎉', goods: '🛒', game: '🎮',
    lostfound: '🔍', post: '📝'
  }
  return icons[moduleType.value] || '📋'
}

function getItemCover(item) {
  if (moduleType.value === 'goods') return item.cover || (item.images ? item.images.split(',')[0] : '')
  if (moduleType.value === 'activity') return item.cover || (item.images ? item.images.split(',')[0] : '')
  if (moduleType.value === 'lostfound') return item.images ? item.images.split(',')[0] : ''
  if (moduleType.value === 'post') return item.images ? item.images.split(',')[0] : ''
  return ''
}

function getItemTitle(item) {
  if (moduleType.value === 'post') return item.title || item.content?.substring(0, 30)
  if (moduleType.value === 'game') return item.title || item.description?.substring(0, 30)
  return item.title || item.name || '未命名'
}

function getItemDesc(item) {
  if (moduleType.value === 'express') {
    return `${item.pickupAddress || ''} → ${item.deliveryAddress || ''}`
  }
  if (moduleType.value === 'print') {
    return `${item.fileName || '打印文件'} · ${item.pageCount || 0}页`
  }
  if (moduleType.value === 'errand') {
    return item.description || item.title
  }
  if (moduleType.value === 'substitute') {
    return `${item.courseName || ''} ${item.courseTime || ''}`
  }
  if (moduleType.value === 'activity') {
    return `📍 ${item.location || ''}  📅 ${item.startTime || item.activityDate || ''}`
  }
  if (moduleType.value === 'goods') {
    return `¥${item.price || 0}`
  }
  if (moduleType.value === 'game') {
    return `${item.gameName || ''} · ${item.gameLevel || '不限段位'}`
  }
  if (moduleType.value === 'lostfound') {
    return `${item.type === 1 || item.type === 'found' ? '失物招领' : '寻物启事'} · ${item.location || ''}`
  }
  if (moduleType.value === 'post') {
    return (item.content || '').substring(0, 50)
  }
  return item.desc || ''
}

function getItemTime(item) {
  return item.createTime || ''
}

function getItemExtra(item) {
  if (moduleType.value === 'goods') return item.price ? `¥${item.price}` : ''
  if (moduleType.value === 'errand') return item.reward ? `¥${item.reward}` : ''
  if (moduleType.value === 'print') return item.totalPrice ? `¥${item.totalPrice}` : ''
  return ''
}

function getItemStatus(item) {
  return item.status
}

function getStatusText(item) {
  const status = item.status
  const type = moduleType.value

  // 重要：不同模块的 api 层对 status 的处理并不一致——
  //   express / errand / substitute 的 api 文件里有 adapt 映射，
  //   会把后端原始状态统一转换成「0=待接单 1=进行中 2=已完成 3=已取消」；
  //   print / goods / activity 等则直接透传后端原始值。
  // 因此展示映射必须按各自实际拿到的值来写，否则会显示「未知」。
  switch (type) {
    case 'express':
    case 'errand':
    case 'substitute':
      // 已由 api 层 adapt 转换
      return { 0: '待接单', 1: '进行中', 2: '已完成', 3: '已取消' }[status] || '未知'
    case 'print':
      // 后端原始值：1待接单 2已接单 3已完成 4已取消 5已退款
      return { 1: '待接单', 2: '已接单', 3: '已完成', 4: '已取消', 5: '已退款' }[status] || '未知'
    case 'activity':
      return { 0: '已结束', 1: '报名中', 2: '进行中' }[status] || '未知'
    case 'goods':
      return { 0: '在售中', 1: '已售出', 2: '已下架' }[status] || '未知'
    case 'game':
      return { 0: '已关闭', 1: '招募中' }[status] || '未知'
    case 'lostfound':
      return { 0: '未解决', 1: '已解决' }[status] || '未知'
    default:
      return status
  }
}

/**
 * 跳详情
 */
function goDetail(item) {
  const type = moduleType.value
  const id = item.id

  const detailPageMap = {
    express: '/pages/express/detail?id=',
    print: '/pages/print/detail?id=',
    errand: '/pages/errand/detail?id=',
    substitute: '/pages/substitute/detail?id=',
    activity: '/pages/activity/detail?id=',
    goods: '/pages/goods/detail?id=',
    game: '/pages/game/detail?id=',
    lostfound: '/pages/lostfound/detail?id=',
    post: '/pages/channel/detail?id='
  }

  const url = detailPageMap[type]
  if (url) {
    uni.navigateTo({ url: url + id })
  }
}
</script>

<style lang="scss" scoped>
.my-list-page {
  min-height: 100vh;
  background: #f5f5f7;
}

/* 顶部角色 tab */
.top-tabs {
  display: flex;
  background: #fff;
  border-bottom: 1rpx solid #f0f0f0;
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

/* 状态筛选 tab */
.status-tabs {
  background: #fff;
  padding: 16rpx 0;
  border-bottom: 1rpx solid #f0f0f0;
}

.status-scroll {
  white-space: nowrap;
  padding: 0 24rpx;
}

.status-item {
  display: inline-block;
  padding: 10rpx 28rpx;
  margin-right: 16rpx;
  background: #f5f5f7;
  border-radius: 30rpx;
  font-size: 26rpx;
  color: #666;

  &.active {
    background: rgba(108, 78, 230, 0.1);
    color: #6c4ee6;
    font-weight: bold;
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

.item-icon {
  flex-shrink: 0;
  width: 160rpx;
  height: 160rpx;
  border-radius: 12rpx;
  background: #f5f5f7;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 60rpx;
}

.item-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 10rpx;
  min-width: 0;
}

.item-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12rpx;
}

.item-title {
  flex: 1;
  font-size: 30rpx;
  font-weight: bold;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-status {
  flex-shrink: 0;
  font-size: 22rpx;
  padding: 4rpx 16rpx;
  border-radius: 20rpx;

  &.status-0, &.status-pending {
    background: #fff7e6;
    color: #fa8c16;
  }
  &.status-1, &.status-progress {
    background: #e6f7ff;
    color: #1890ff;
  }
  &.status-2, &.status-done {
    background: #f6ffed;
    color: #52c41a;
  }
  &.status-3, &.status-cancel {
    background: #f5f5f5;
    color: #999;
  }
}

.item-desc {
  font-size: 24rpx;
  color: #666;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.item-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: auto;
}

.item-time {
  font-size: 22rpx;
  color: #999;
}

.item-extra {
  font-size: 28rpx;
  color: #ff4d4f;
  font-weight: bold;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 120rpx 0;
  gap: 20rpx;
}

.empty-icon {
  font-size: 100rpx;
}

.empty-text {
  font-size: 28rpx;
  color: #999;
}

/* 加载中 */
.loading-state {
  text-align: center;
  padding: 60rpx 0;
  font-size: 26rpx;
  color: #999;
}
</style>
