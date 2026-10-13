<template>
  <!-- 校园频道页 - 类似校园论坛/朋友圈 -->
  <view class="channel-page">
    <!-- 自定义导航栏 -->
    <view class="nav-bar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-left" @click="publishPost">
        <text class="publish-btn">✏️ 发布</text>
      </view>
      <view class="nav-title">校园频道</view>
      <view class="nav-right">
        <!-- 右侧预留位置，保持标题居中 -->
      </view>
    </view>

    <scroll-view 
      scroll-y 
      class="scroll-content"
      :style="{ paddingTop: navHeight + 'px' }"
      refresher-enabled
      :refresher-triggered="isRefreshing"
      @refresherrefresh="onRefresh"
    >
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

      <!-- 帖子列表 -->
      <view class="post-list">
        <view class="post-item" v-for="item in postList" :key="item.id" @click="goToDetail(item.id)">
          <!-- 作者信息 -->
          <view class="post-header">
            <image :src="item.avatar" class="avatar" lazy-load></image>
            <view class="author-info">
              <text class="nickname">{{ item.nickname }}</text>
              <text class="post-time">{{ item.createTime }}</text>
            </view>
          </view>

          <!-- 帖子内容 -->
          <view class="post-content">
            <text class="post-title">{{ item.title }}</text>
            <text class="post-text">{{ item.content }}</text>
          </view>

          <!-- 帖子图片 -->
          <view v-if="item.images && item.images.length" class="post-images">
            <image 
              v-for="(img, idx) in item.images" 
              :key="idx" 
              :src="img" 
              mode="aspectFill"
              class="post-image"
              :class="'image-count-' + item.images.length"
              @click.stop="previewImage(item.images, idx)"
            ></image>
          </view>

          <!-- 底部操作栏 -->
          <view class="post-footer">
            <view class="footer-item" @click.stop="toggleLike(item)">
              <text class="footer-icon">{{ item.isLiked ? '❤️' : '🤍' }}</text>
              <text class="footer-text">{{ item.likeCount }}</text>
            </view>
            <view class="footer-item" @click.stop="goToDetail(item.id)">
              <text class="footer-icon">💬</text>
              <text class="footer-text">{{ item.commentCount }}</text>
            </view>
            <view class="footer-item" @click.stop="sharePost(item)">
              <text class="footer-icon">📤</text>
              <text class="footer-text">分享</text>
            </view>
          </view>
        </view>
      </view>

      <view style="height: 40rpx;"></view>
    </scroll-view>

    <!-- 发布弹窗 -->
    <view class="publish-modal" v-if="showPublishModal" @click.stop="closePublishModal">
      <view class="modal-content" @click.stop>
        <view class="modal-header">
          <text class="modal-title">发布帖子</text>
          <text class="modal-close" @click="closePublishModal">×</text>
        </view>
        <scroll-view scroll-y class="modal-body">
          <view class="form-item">
            <text class="form-label">分类 <text class="required">*</text></text>
            <view class="category-picker">
              <view
                v-for="c in categoryOptions"
                :key="c"
                class="category-option"
                :class="{ active: publishForm.category === c }"
                @click="publishForm.category = c"
              >{{ c }}</view>
            </view>
          </view>
          <view class="form-item">
            <text class="form-label">标题 <text class="required">*</text></text>
            <input class="form-input" v-model="publishForm.title" placeholder="请输入帖子标题" maxlength="50" />
          </view>
          <view class="form-item">
            <text class="form-label">内容 <text class="required">*</text></text>
            <textarea class="form-textarea" v-model="publishForm.content" placeholder="分享你的校园生活..." maxlength="500" />
          </view>
        </scroll-view>
        <view class="modal-footer">
          <view class="modal-btn cancel" @click="closePublishModal">取消</view>
          <view class="modal-btn confirm" @click="submitPublish">发布</view>
        </view>
      </view>
    </view>
      <!-- AI 助手悬浮球 -->
    <view class="ai-fab" @click="goAiChat">
      <text class="ai-fab-icon">🤖</text>
    </view>
  </view>
</template>

<script setup>
/**
 * 校园频道页
 * 类似校园论坛，展示学生发布的帖子
 */
import { ref, onMounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { channelApi } from '@/api/index'
import { checkLogin } from '@/utils/request'

// 默认头像 URL
const DEFAULT_AVATAR = 'https://thirdwx.qlogo.cn/mmopen/vi_32/POgEwh4mIHO4nibH0KlMECNjjGxQUq24ZEaGT4poC6icRiccVGKSyXwibcPq4BWmiaIGuG1icwxaQX6grC9VemZoJ8rg/132'

const statusBarHeight = ref(20)
const navHeight = ref(88)
const isRefreshing = ref(false)
// 加载状态
const loading = ref(false)

// 当前选中的分类
const currentTab = ref('推荐')
// 是否已完成首次加载（用于 onShow 避免与 onMounted 重复请求）
let hasLoadedOnce = false
// 分类列表（与后端 post.category 取值一致：推荐/吐槽/交友/分享）
const tabList = ref([
  { id: '推荐', name: '推荐' },
  { id: '吐槽', name: '吐槽' },
  { id: '交友', name: '交友' },
  { id: '分享', name: '分享' }
])

// 帖子列表
const postList = ref([])

// 发布弹窗
const showPublishModal = ref(false)
// 发布表单（category 必须有值，否则后端会归到「推荐」）
const publishForm = ref({
  category: '推荐',
  title: '',
  content: ''
})
// 可选分类（不含「推荐」——推荐是聚合视图，不是真实分类）
const categoryOptions = ['吐槽', '交友', '分享']

onMounted(() => {
  const sysInfo = uni.getSystemInfoSync()
  statusBarHeight.value = sysInfo.statusBarHeight || 20
  navHeight.value = statusBarHeight.value + 44

  loadPostList()
})

/**
 * 从帖子详情返回时刷新列表
 *
 * 修复：此前在详情页发表评论后返回频道页，列表里的评论数不会更新
 *（因为页面是 keep-alive 的，onMounted 只执行一次）。
 * 改为 onShow 时重新拉取，保证评论数/点赞数及时同步。
 */
onShow(() => {
  // 首屏由 onMounted 负责，避免重复请求；后续每次返回都刷新
  if (hasLoadedOnce) {
    loadPostList()
  }
  hasLoadedOnce = true
})

/**
 * 加载帖子列表
 * category 传后端约定的中文分类名；「推荐」等价全部
 */
async function loadPostList() {
  loading.value = true
  try {
    const data = await channelApi.getPostList({
      pageNum: 1,
      pageSize: 10,
      keyword: '',
      category: currentTab.value
    })
    const list = data.records || data.list || data || []
    postList.value = list.map(item => enrichPostItem(item))
  } catch (err) {
    console.warn('帖子列表接口请求失败:', err)
    postList.value = []
  } finally {
    loading.value = false
  }
}

/**
 * 补充帖子的前端字段（后端未返回的字段）
 * @param {Object} item - 后端返回的帖子数据
 * @returns {Object} 补充后的帖子数据
 */
function enrichPostItem(item) {
  return {
    ...item,
    // 昵称：默认 '校园用户' + userId 后四位
    nickname: item.nickname || ('校园用户' + (item.userId ? String(item.userId).slice(-4) : '')),
    // 头像：默认头像
    avatar: item.avatar || DEFAULT_AVATAR,
    // 点赞数：后端没返回，默认 0
    likeCount: item.likeCount !== undefined ? item.likeCount : 0,
    // 评论数：后端没返回，默认 0
    commentCount: item.commentCount !== undefined ? item.commentCount : 0,
    // 是否已点赞：后端没返回，默认 false
    isLiked: item.isLiked !== undefined ? item.isLiked : false,
    // 图片：如果是逗号分隔的字符串，分割成数组；否则是空数组
    images: typeof item.images === 'string'
      ? item.images ? item.images.split(',').filter(url => url.trim()) : []
      : (Array.isArray(item.images) ? item.images : []),
    // 创建时间：保留原字段
    createTime: item.createTime || ''
  }
}

/**
 * 切换分类
 * 现在会把分类传给后端做服务端筛选
 */
function switchTab(id) {
  if (currentTab.value === id) return
  currentTab.value = id
  loadPostList()
}

/**
 * 跳转到帖子详情（评论入口）
 */
function goToDetail(id) {
  uni.navigateTo({
    url: '/pages/channel/detail?id=' + id
  })
}

/**
 * 点赞/取消点赞
 * 调用真实接口，接口返回 Boolean（true=已点赞, false=已取消）
 * 保持乐观更新逻辑
 */
async function toggleLike(item) {
  if (!checkLogin()) return
  // 先乐观更新 UI
  const oldIsLiked = item.isLiked
  const oldLikeCount = item.likeCount
  item.isLiked = !item.isLiked
  item.likeCount += item.isLiked ? 1 : -1

  try {
    // 调用点赞接口，返回 Boolean
    const result = await channelApi.toggleLike(item.id)
    // 根据返回值更新状态
    // result 为 true 表示已点赞，false 表示已取消点赞
    if (result === true || result === false) {
      item.isLiked = result
      // 如果结果与乐观更新不一致，调整计数
      if (result !== oldIsLiked) {
        item.likeCount = oldLikeCount + (result ? 1 : -1)
      } else {
        item.likeCount = oldLikeCount
      }
    }
  } catch (err) {
    // 接口失败，回滚状态
    console.warn('点赞失败:', err)
    item.isLiked = oldIsLiked
    item.likeCount = oldLikeCount
  }
}

/**
 * 分享帖子
 */
function sharePost(item) {
  // #ifdef MP-WEIXIN
  uni.showShareMenu({ withShareTicket: true })
  uni.showToast({ title: '点击右上角分享', icon: 'none' })
  // #endif
  // #ifndef MP-WEIXIN
  uni.setClipboardData({
    data: item.title + ' - 校园频道',
    success: () => {
      uni.showToast({ title: '已复制标题，去分享吧', icon: 'none' })
    }
  })
  // #endif
}

/**
 * 预览图片
 */
function previewImage(images, current) {
  uni.previewImage({
    urls: images,
    current: images[current]
  })
}

/**
 * 发布帖子 - 检查登录并打开弹窗
 */
function publishPost() {
  if (!checkLogin()) return
  // 重置表单：默认使用当前所在分类，方便用户在该分类下快速发帖
  publishForm.value = {
    category: currentTab.value === '推荐' ? '吐槽' : currentTab.value,
    title: '',
    content: ''
  }
  showPublishModal.value = true
}

/**
 * 关闭发布弹窗
 */
function closePublishModal() {
  showPublishModal.value = false
}

/**
 * 提交发布
 */
async function submitPublish() {
  const form = publishForm.value
  // 校验标题
  if (!form.title.trim()) {
    uni.showToast({ title: '请输入标题', icon: 'none' })
    return
  }
  // 校验内容
  if (!form.content.trim()) {
    uni.showToast({ title: '请输入内容', icon: 'none' })
    return
  }
  // 校验分类（兜底，正常不会为空）
  if (!form.category) {
    uni.showToast({ title: '请选择分类', icon: 'none' })
    return
  }

  try {
    // 调用发布接口（带上分类）
    await channelApi.publishPost({
      title: form.title.trim(),
      content: form.content.trim(),
      category: form.category,
      images: ''
    })
    uni.showToast({ title: '发布成功', icon: 'success' })
    showPublishModal.value = false
    // 若当前在「推荐」则停留；否则切到所发分类，让用户立刻看到自己的帖子
    if (currentTab.value !== '推荐' && currentTab.value !== form.category) {
      currentTab.value = form.category
    }
    // 刷新列表
    loadPostList()
  } catch (err) {
    console.warn('发布失败:', err)
  }
}

/**
 * 下拉刷新
 */
async function onRefresh() {
  isRefreshing.value = true
  try {
    await loadPostList()
  } finally {
    isRefreshing.value = false
  }
}
function goAiChat() {
  uni.navigateTo({ url: '/pages/ai/chat' })
}
</script>

<style lang="scss" scoped>
.channel-page {
  min-height: 100vh;
  background: #f5f5f7;
}

.nav-bar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  background: #fff;
  border-bottom: 1rpx solid #eee;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24rpx;
  height: 88rpx;
  box-sizing: content-box;
}

.nav-left {
  width: 160rpx;
  display: flex;
  align-items: center;
  justify-content: flex-start;
}

.nav-title {
  flex: 1;
  text-align: center;
  font-size: 34rpx;
  font-weight: bold;
  color: #333;
}

.nav-right {
  width: 160rpx;
  display: flex;
  align-items: center;
  justify-content: flex-end;
}

.publish-btn {
  font-size: 24rpx;
  color: #6c4ee6;
  background: rgba(108, 78, 230, 0.1);
  padding: 8rpx 16rpx;
  border-radius: 28rpx;
  white-space: nowrap;
}

.scroll-content {
  height: 100vh;
  box-sizing: border-box;
}

/* 分类标签 */
.tab-list {
  display: flex;
  background: #fff;
  padding: 20rpx 24rpx;
  gap: 30rpx;
  overflow-x: auto;
  white-space: nowrap;
  position: sticky;
  top: 0;
  z-index: 10;
}

.tab-item {
  font-size: 28rpx;
  color: #666;
  padding: 8rpx 0;
  position: relative;
  flex-shrink: 0;

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

/* 帖子列表 */
.post-list {
  padding: 20rpx 24rpx;
}

.post-item {
  background: #fff;
  border-radius: 20rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
}

.post-header {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-bottom: 20rpx;
}

.avatar {
  width: 72rpx;
  height: 72rpx;
  border-radius: 50%;
}

.author-info {
  display: flex;
  flex-direction: column;
  gap: 4rpx;
}

.nickname {
  font-size: 28rpx;
  font-weight: 500;
  color: #333;
}

.post-time {
  font-size: 22rpx;
  color: #999;
}

.post-content {
  margin-bottom: 20rpx;
}

.post-title {
  font-size: 30rpx;
  font-weight: 500;
  color: #333;
  margin-bottom: 12rpx;
  display: block;
}

.post-text {
  font-size: 28rpx;
  color: #666;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
  overflow: hidden;
}

/* 图片 */
.post-images {
  display: flex;
  flex-wrap: wrap;
  gap: 8rpx;
  margin-bottom: 20rpx;
}

.post-image {
  width: 200rpx;
  height: 200rpx;
  border-radius: 12rpx;

  &.image-count-1 {
    width: 100%;
    height: 400rpx;
  }
}

/* 底部操作 */
.post-footer {
  display: flex;
  justify-content: space-around;
  padding-top: 20rpx;
  border-top: 1rpx solid #f0f0f0;
}

.footer-item {
  display: flex;
  align-items: center;
  gap: 8rpx;
}

.footer-icon {
  font-size: 32rpx;
}

.footer-text {
  font-size: 24rpx;
  color: #666;
}

/* 发布弹窗 */
.publish-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal-content {
  width: 85%;
  max-height: 80vh;
  background: #fff;
  border-radius: 20rpx;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 30rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.modal-title {
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
}

.modal-close {
  font-size: 48rpx;
  color: #999;
  line-height: 1;
  padding: 0 10rpx;
}

.modal-body {
  flex: 1;
  padding: 20rpx 30rpx;
  max-height: 60vh;
}

.form-item {
  margin-bottom: 24rpx;
}

.form-label {
  display: block;
  font-size: 26rpx;
  color: #333;
  margin-bottom: 12rpx;

  .required {
    color: #ff4d4f;
  }
}

.form-input {
  width: 100%;
  height: 72rpx;
  padding: 0 20rpx;
  border: 1rpx solid #e8e8e8;
  border-radius: 12rpx;
  font-size: 26rpx;
  color: #333;
  background: #fafafa;
  box-sizing: border-box;
}

.form-textarea {
  width: 100%;
  height: 240rpx;
  padding: 16rpx 20rpx;
  border: 1rpx solid #e8e8e8;
  border-radius: 12rpx;
  font-size: 26rpx;
  color: #333;
  background: #fafafa;
  box-sizing: border-box;
}

/* 发布时的分类选择 */
.category-picker {
  display: flex;
  gap: 16rpx;
}

.category-option {
  flex: 1;
  text-align: center;
  padding: 16rpx 0;
  border-radius: 12rpx;
  font-size: 26rpx;
  color: #666;
  background: #fafafa;
  border: 1rpx solid #e8e8e8;
  transition: all 0.2s;

  &.active {
    color: #fff;
    background: linear-gradient(135deg, #6c4ee6 0%, #8b5cf6 100%);
    border-color: transparent;
    font-weight: 600;
  }
}

.modal-footer {
  display: flex;
  border-top: 1rpx solid #f0f0f0;
}

.modal-btn {
  flex: 1;
  text-align: center;
  padding: 28rpx 0;
  font-size: 28rpx;

  &.cancel {
    color: #666;
    border-right: 1rpx solid #f0f0f0;
  }

  &.confirm {
    color: #6c4ee6;
    font-weight: bold;
  }
}

/* AI 助手悬浮球 */
.ai-fab {
  position: fixed;
  right: 30rpx;
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
}
.ai-fab .ai-fab-icon {
  font-size: 44rpx;
}
</style>
