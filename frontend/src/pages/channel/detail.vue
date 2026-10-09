<template>
  <view class="detail-page">
    <!-- 帖子内容 -->
    <view class="post-detail" v-if="post">
      <view class="post-header">
        <image :src="post.avatar" class="avatar"></image>
        <view class="author-info">
          <text class="nickname">{{ post.nickname }}</text>
          <text class="post-time">{{ post.createTime }}</text>
        </view>
      </view>
      <text class="post-title">{{ post.title }}</text>
      <text class="post-content">{{ post.content }}</text>
      <view v-if="post.images && post.images.length" class="post-images">
        <image v-for="(img, idx) in post.images" :key="idx" :src="img" mode="aspectFill" class="post-image"></image>
      </view>
      <view class="post-stats">
        <text>❤️ {{ post.likeCount }} 点赞</text>
        <text>💬 {{ post.commentCount || commentList.length }} 评论</text>
        <text>👁 {{ post.viewCount || 0 }} 浏览</text>
      </view>

      <!-- 操作栏：点赞 / 收藏 -->
      <view class="post-actions">
        <view class="action-item" :class="{ active: post.isLiked }" @click="toggleLike">
          <text class="action-icon">{{ post.isLiked ? '❤️' : '🤍' }}</text>
          <text class="action-text">{{ post.isLiked ? '已点赞' : '点赞' }}</text>
        </view>
        <view class="action-item" :class="{ active: post.isCollected }" @click="toggleCollect">
          <text class="action-icon">{{ post.isCollected ? '⭐' : '☆' }}</text>
          <text class="action-text">{{ post.isCollected ? '已收藏' : '收藏' }}</text>
        </view>
      </view>
    </view>

    <!-- 评论列表 -->
    <view class="comment-section">
      <view class="section-title">评论 ({{ commentList.length }})</view>
      <view v-if="commentList.length === 0" class="empty-comment">
        <text>暂无评论，快来抢沙发～</text>
      </view>
      <view v-else class="comment-list">
        <view class="comment-item" v-for="item in commentList" :key="item.id">
          <image :src="item.avatar" class="comment-avatar"></image>
          <view class="comment-content">
            <view class="comment-header">
              <text class="comment-nickname">{{ item.nickname }}</text>
              <text class="comment-time">{{ item.createTime }}</text>
            </view>
            <text class="comment-text">{{ item.content }}</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 底部评论输入框 -->
    <view class="bottom-bar">
      <input class="comment-input" v-model="commentText" placeholder="说点什么..." />
      <view class="send-btn" @click="sendComment">发送</view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { channelApi } from '@/api/index'
import { checkLogin } from '@/utils/request'

const DEFAULT_AVATAR = 'https://thirdwx.qlogo.cn/mmopen/vi_32/POgEwh4mIHO4nibH0KlMECNjjGxQUq24ZEaGT4poC6icRiccVGKSyXwibcPq4BWmiaIGuG1icwxaQX6grC9VemZoJ8rg/132'

const post = ref(null)
const commentList = ref([])
const commentText = ref('')
const postId = ref(1)

onMounted(() => {
  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1]
  const id = currentPage.options?.id
  if (id) {
    postId.value = Number(id)
  }
  loadDetail()
  loadComments()
})

async function loadDetail() {
  try {
    const data = await channelApi.getPostDetail(postId.value)
    post.value = enrichPostItem(data)
  } catch (err) {
    console.warn('帖子详情加载失败:', err)
    post.value = {
      id: postId.value,
      title: '帖子详情',
      content: '加载失败，请重试',
      nickname: '校园用户',
      avatar: DEFAULT_AVATAR,
      likeCount: 0,
      images: [],
      createTime: ''
    }
  }
}

async function loadComments() {
  try {
    const data = await channelApi.getCommentList(postId.value, { pageNum: 1, pageSize: 20 })
    const list = data.records || data.list || []
    commentList.value = list.map(item => ({
      ...item,
      nickname: item.nickname || ('校园用户' + (item.userId ? String(item.userId).slice(-4) : '')),
      avatar: item.avatar || DEFAULT_AVATAR,
      content: item.content || ''
    }))
  } catch (err) {
    console.warn('评论加载失败:', err)
    commentList.value = []
  }
}

function enrichPostItem(item) {
  return {
    ...item,
    nickname: item.nickname || ('校园用户' + (item.userId ? String(item.userId).slice(-4) : '')),
    avatar: item.avatar || DEFAULT_AVATAR,
    likeCount: item.likeCount || 0,
    commentCount: item.commentCount || 0,
    isLiked: item.isLiked === true,
    isCollected: item.isCollected === true,
    images: typeof item.images === 'string'
      ? item.images ? item.images.split(',').filter(url => url.trim()) : []
      : (Array.isArray(item.images) ? item.images : []),
    createTime: item.createTime || ''
  }
}

/**
 * 点赞 / 取消点赞（乐观更新，失败回滚）
 */
async function toggleLike() {
  if (!checkLogin()) return
  if (!post.value) return
  const oldLiked = post.value.isLiked
  const oldCount = post.value.likeCount
  post.value.isLiked = !oldLiked
  post.value.likeCount = oldCount + (post.value.isLiked ? 1 : -1)

  try {
    const liked = await channelApi.toggleLike(postId.value)
    // 以后端返回为准，避免前后端不一致
    if (typeof liked === 'boolean') {
      post.value.isLiked = liked
      post.value.likeCount = oldCount + (liked ? 1 : 0) - (oldLiked ? 1 : 0)
    }
  } catch (err) {
    post.value.isLiked = oldLiked
    post.value.likeCount = oldCount
    console.warn('点赞失败:', err)
  }
}

/**
 * 收藏 / 取消收藏
 */
async function toggleCollect() {
  if (!checkLogin()) return
  if (!post.value) return
  const oldCollected = post.value.isCollected
  post.value.isCollected = !oldCollected // 乐观更新

  try {
    const collected = await channelApi.toggleCollect(postId.value)
    if (typeof collected === 'boolean') {
      post.value.isCollected = collected
    }
    uni.showToast({
      title: post.value.isCollected ? '已收藏' : '已取消收藏',
      icon: 'none'
    })
  } catch (err) {
    post.value.isCollected = oldCollected // 失败回滚
    console.warn('收藏失败:', err)
  }
}

function sendComment() {
  if (!checkLogin()) return
  if (!commentText.value.trim()) {
    uni.showToast({ title: '请输入评论内容', icon: 'none' })
    return
  }
  channelApi.addComment({ postId: postId.value, content: commentText.value.trim() })
    .then(() => {
      uni.showToast({ title: '评论成功', icon: 'success' })
      commentText.value = ''
      // 同时刷新评论列表与帖子详情，保证评论数立即更新
      loadComments()
      loadDetail()
    })
    .catch(() => {
      uni.showToast({ title: '评论失败', icon: 'none' })
    })
}
</script>

<style lang="scss" scoped>
.detail-page {
  min-height: 100vh;
  background: #f5f5f7;
  padding-bottom: 120rpx;
}

.post-detail {
  background: #fff;
  padding: 30rpx;
  margin-bottom: 20rpx;
}

.post-header {
  display: flex;
  align-items: center;
  margin-bottom: 20rpx;
}

.avatar {
  width: 80rpx;
  height: 80rpx;
  border-radius: 50%;
  margin-right: 20rpx;
}

.author-info {
  flex: 1;
}

.nickname {
  font-size: 28rpx;
  font-weight: 500;
  color: #333;
  display: block;
}

.post-time {
  font-size: 24rpx;
  color: #999;
}

.post-title {
  font-size: 34rpx;
  font-weight: bold;
  color: #333;
  display: block;
  margin-bottom: 16rpx;
}

.post-content {
  font-size: 28rpx;
  color: #333;
  line-height: 1.6;
  display: block;
  margin-bottom: 20rpx;
}

.post-images {
  display: flex;
  flex-wrap: wrap;
  gap: 10rpx;
  margin-bottom: 20rpx;
}

.post-image {
  width: 200rpx;
  height: 200rpx;
  border-radius: 12rpx;
}

.post-stats {
  display: flex;
  gap: 30rpx;
  font-size: 24rpx;
  color: #999;
  padding-top: 20rpx;
  border-top: 1rpx solid #f0f0f0;
}

/* 点赞 / 收藏 操作栏 */
.post-actions {
  display: flex;
  gap: 24rpx;
  margin-top: 24rpx;
}

.action-item {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10rpx;
  padding: 20rpx 0;
  border-radius: 14rpx;
  background: #f7f7fa;
  border: 1rpx solid #eee;
  transition: all 0.2s;

  .action-icon {
    font-size: 32rpx;
  }

  .action-text {
    font-size: 26rpx;
    color: #666;
  }

  &.active {
    background: #f3efff;
    border-color: #d9cfff;

    .action-text {
      color: #6c4ee6;
      font-weight: 600;
    }
  }
}

.comment-section {
  background: #fff;
  padding: 30rpx;
}

.section-title {
  font-size: 30rpx;
  font-weight: bold;
  color: #333;
  margin-bottom: 20rpx;
}

.empty-comment {
  text-align: center;
  padding: 60rpx 0;
  color: #999;
  font-size: 26rpx;
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 30rpx;
}

.comment-item {
  display: flex;
  gap: 20rpx;
}

.comment-avatar {
  width: 64rpx;
  height: 64rpx;
  border-radius: 50%;
  flex-shrink: 0;
}

.comment-content {
  flex: 1;
}

.comment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8rpx;
}

.comment-nickname {
  font-size: 26rpx;
  color: #666;
  font-weight: 500;
}

.comment-time {
  font-size: 22rpx;
  color: #ccc;
}

.comment-text {
  font-size: 28rpx;
  color: #333;
  line-height: 1.5;
}

.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: #fff;
  padding: 20rpx 30rpx;
  display: flex;
  gap: 20rpx;
  align-items: center;
  border-top: 1rpx solid #eee;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
}

.comment-input {
  flex: 1;
  height: 72rpx;
  background: #f5f5f7;
  border-radius: 36rpx;
  padding: 0 30rpx;
  font-size: 28rpx;
}

.send-btn {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  padding: 16rpx 32rpx;
  border-radius: 36rpx;
  font-size: 28rpx;
}
</style>
