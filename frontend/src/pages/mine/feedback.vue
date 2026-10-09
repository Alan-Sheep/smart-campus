<template>
  <view class="feedback-page">
    <!-- 反馈类型 -->
    <view class="form-section">
      <view class="section-title">反馈类型</view>
      <view class="type-list">
        <view
          class="type-item"
          :class="{ active: formData.type === item.value }"
          v-for="item in typeList"
          :key="item.value"
          @click="selectType(item.value)"
        >
          {{ item.label }}
        </view>
      </view>
    </view>

    <!-- 反馈标题 -->
    <view class="form-section">
      <view class="section-title">反馈标题</view>
      <input
        class="form-input"
        v-model="formData.title"
        placeholder="请简要描述问题"
        maxlength="50"
      />
    </view>

    <!-- 反馈内容 -->
    <view class="form-section">
      <view class="section-title">
        <text>详细描述</text>
        <text class="count">{{ formData.content.length }}/500</text>
      </view>
      <textarea
        class="form-textarea"
        v-model="formData.content"
        placeholder="请详细描述您遇到的问题或建议，我们会认真对待每一条反馈~"
        maxlength="500"
      ></textarea>
    </view>

    <!-- 联系方式 -->
    <view class="form-section">
      <view class="section-title">联系方式（选填）</view>
      <input
        class="form-input"
        v-model="formData.contact"
        placeholder="手机号/微信号，方便我们联系您"
      />
    </view>

    <!-- 提交按钮 -->
    <view class="submit-section">
      <button class="submit-btn" :disabled="submitting" @click="handleSubmit">
        {{ submitting ? '提交中...' : '提交反馈' }}
      </button>
      <text class="tip">感谢您的反馈，我们会在1-3个工作日内处理</text>
    </view>

    <!-- 我的反馈记录 -->
    <view class="history-section" v-if="myFeedback.length > 0">
      <view class="section-title">我的反馈</view>
      <view class="feedback-list">
        <view class="feedback-item" v-for="item in myFeedback" :key="item.id">
          <view class="fb-header">
            <text class="fb-title">{{ item.title }}</text>
            <view class="fb-status" :class="'status-' + item.status">
              {{ statusText(item.status) }}
            </view>
          </view>
          <text class="fb-content">{{ item.content }}</text>
          <text class="fb-time">{{ item.createTime }}</text>
          <!-- 只要有回复内容就展示（后端回复后会把状态置为 2=已完成） -->
          <view class="fb-reply" v-if="item.reply">
            <text class="reply-label">官方回复：</text>
            <text class="reply-text">{{ item.reply }}</text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { feedbackApi } from '@/api/index'
import { checkLogin } from '@/utils/request'

// 反馈类型：value 必须是后端 TINYINT 可接受的数字
// 库中语义：0:功能建议 1:BUG反馈 2:投诉举报 3:其他
const typeList = [
  { label: '功能建议', value: 0 },
  { label: 'Bug反馈', value: 1 },
  { label: '内容投诉', value: 2 },
  { label: '其他', value: 3 }
]

const formData = ref({
  type: 0,
  title: '',
  content: '',
  contact: ''
})

const submitting = ref(false)
const myFeedback = ref([])

/**
 * 反馈状态文案
 * 库中语义：0:待处理 1:处理中 2:已完成
 */
function statusText(status) {
  return { 0: '待处理', 1: '处理中', 2: '已回复' }[status] ?? '已回复'
}

onMounted(() => {
  loadMyFeedback()
})

function selectType(type) {
  formData.value.type = type
}

/**
 * 提交反馈
 */
async function handleSubmit() {
  if (!checkLogin()) return

  if (!formData.value.title.trim()) {
    uni.showToast({ title: '请输入反馈标题', icon: 'none' })
    return
  }
  if (!formData.value.content.trim()) {
    uni.showToast({ title: '请输入详细描述', icon: 'none' })
    return
  }

  submitting.value = true
  try {
    await feedbackApi.submitFeedback(formData.value)
    uni.showToast({ title: '提交成功', icon: 'success' })
    // 清空表单
    formData.value.title = ''
    formData.value.content = ''
    formData.value.contact = ''
    // 刷新列表
    loadMyFeedback()
  } catch (err) {
    uni.showToast({ title: err.message || '提交失败', icon: 'none' })
  } finally {
    submitting.value = false
  }
}

/**
 * 加载我的反馈
 */
async function loadMyFeedback() {
  if (!checkLogin()) return
  try {
    const data = await feedbackApi.getMyFeedback({ pageNum: 1, pageSize: 10 })
    myFeedback.value = data.records || data.list || []
  } catch (err) {
    // 失败不提示
  }
}
</script>

<style lang="scss" scoped>
.feedback-page {
  min-height: 100vh;
  background: #f5f5f7;
  padding: 20rpx 0;
}

.form-section {
  background: #fff;
  margin-bottom: 20rpx;
  padding: 24rpx;
}

.section-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 26rpx;
  color: #666;
  margin-bottom: 20rpx;

  .count {
    font-size: 24rpx;
    color: #999;
  }
}

.type-list {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}

.type-item {
  padding: 12rpx 28rpx;
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

.form-input {
  width: 100%;
  height: 80rpx;
  font-size: 28rpx;
  color: #333;
}

.form-textarea {
  width: 100%;
  height: 200rpx;
  font-size: 28rpx;
  color: #333;
  line-height: 1.6;
}

.submit-section {
  padding: 40rpx 24rpx;
  text-align: center;
}

.submit-btn {
  width: 100%;
  height: 88rpx;
  line-height: 88rpx;
  background: linear-gradient(135deg, #6c4ee6, #8b5cf6);
  color: #fff;
  font-size: 30rpx;
  border-radius: 44rpx;
}

.tip {
  display: block;
  margin-top: 20rpx;
  font-size: 24rpx;
  color: #999;
}

.history-section {
  padding: 0 24rpx;
}

.feedback-list {
  margin-top: 16rpx;
}

.feedback-item {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 16rpx;
}

.fb-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12rpx;
}

.fb-title {
  font-size: 28rpx;
  font-weight: bold;
  color: #333;
}

.fb-status {
  font-size: 22rpx;
  padding: 4rpx 16rpx;
  border-radius: 20rpx;

  &.status-0 {
    background: #fff7e6;
    color: #fa8c16;
  }
  &.status-1 {
    background: #e6f4ff;
    color: #1677ff;
  }
  &.status-2 {
    background: #f6ffed;
    color: #52c41a;
  }
}

.fb-content {
  font-size: 26rpx;
  color: #666;
  line-height: 1.5;
  margin-bottom: 12rpx;
}

.fb-time {
  font-size: 22rpx;
  color: #999;
}

.fb-reply {
  margin-top: 16rpx;
  padding: 16rpx;
  background: #f9f9ff;
  border-radius: 12rpx;
  border-left: 6rpx solid #6c4ee6;
}

.reply-label {
  font-size: 24rpx;
  color: #6c4ee6;
  font-weight: bold;
}

.reply-text {
  font-size: 24rpx;
  color: #666;
  line-height: 1.5;
}
</style>
