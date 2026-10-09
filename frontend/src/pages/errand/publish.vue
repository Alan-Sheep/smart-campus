<template>
  <!-- 发布校园跑腿页 -->
  <view class="publish-page">
    <!-- 表单 -->
    <view class="form-section">
      <view class="form-group">
        <text class="form-label">跑腿类型</text>
        <picker :range="errandTypeList" range-key="label" @change="onTypeChange">
          <view class="picker-input">
            <text :class="{ placeholder: !formData.errandType }">
              {{ formData.errandType ? currentTypeLabel : '请选择跑腿类型' }}
            </text>
            <text class="arrow">›</text>
          </view>
        </picker>
      </view>

      <view class="form-group">
        <text class="form-label">标题</text>
        <input 
          class="form-input" 
          placeholder="请输入跑腿标题" 
          v-model="formData.title"
        />
      </view>

      <view class="form-group">
        <text class="form-label">详细描述</text>
        <textarea 
          class="form-textarea" 
          placeholder="请详细描述跑腿需求" 
          v-model="formData.description"
          maxlength="200"
        ></textarea>
        <text class="char-count">{{ formData.description.length }}/200</text>
      </view>

      <view class="form-group">
        <text class="form-label">取件地址</text>
        <input 
          class="form-input" 
          placeholder="请输入取件地址" 
          v-model="formData.pickupAddress"
        />
      </view>

      <view class="form-group">
        <text class="form-label">送达地址</text>
        <input 
          class="form-input" 
          placeholder="请输入送达地址" 
          v-model="formData.deliveryAddress"
        />
      </view>

      <view class="form-group">
        <text class="form-label">报酬金额（元）</text>
        <input 
          class="form-input" 
          type="digit" 
          placeholder="请输入报酬金额" 
          v-model="formData.serviceFee"
        />
      </view>

      <view class="form-group">
        <text class="form-label">期望完成时间</text>
        <picker mode="date" @change="onDateChange">
          <view class="picker-input">
            <text :class="{ placeholder: !formData.estimatedTime }">
              {{ formData.estimatedTime || '请选择期望完成时间' }}
            </text>
            <text class="arrow">›</text>
          </view>
        </picker>
      </view>

      <view class="form-group">
        <text class="form-label">联系电话</text>
        <input 
          class="form-input" 
          placeholder="请输入手机号" 
          v-model="formData.receiverPhone"
        />
      </view>
    </view>

    <!-- 发布按钮 -->
    <view class="submit-bar">
      <view class="submit-btn" @click="submitPublish">
        发布
      </view>
    </view>
  </view>
</template>

<script setup>
/**
 * 发布校园跑腿页
 */
import { reactive, computed } from 'vue'
import { errandApi } from '@/api/index'
import { checkLogin } from '@/utils/request'
import { guardWrite } from '@/utils/school-permission'

const errandTypeList = [
  { value: '1', label: '代取快递' },
  { value: '2', label: '代买饭' },
  { value: '3', label: '代送物品' },
  { value: '4', label: '其他' }
]

const formData = reactive({
  errandType: '',
  title: '',
  description: '',
  pickupAddress: '',
  deliveryAddress: '',
  serviceFee: '',
  estimatedTime: '',
  receiverPhone: ''
})

const currentTypeLabel = computed(() => {
  const item = errandTypeList.find(t => t.value === formData.errandType)
  return item ? item.label : ''
})

/**
 * 选择跑腿类型
 */
function onTypeChange(e) {
  const idx = e.detail.value
  formData.errandType = errandTypeList[idx].value
}

/**
 * 选择日期
 */
function onDateChange(e) {
  formData.estimatedTime = e.detail.value
}

/**
 * 提交发布
 */
function submitPublish() {
  if (guardWrite()) return
  if (!checkLogin()) return
  if (!formData.errandType) {
    uni.showToast({ title: '请选择跑腿类型', icon: 'none' })
    return
  }
  if (!formData.title) {
    uni.showToast({ title: '请输入标题', icon: 'none' })
    return
  }
  if (!formData.description) {
    uni.showToast({ title: '请输入详细描述', icon: 'none' })
    return
  }
  if (!formData.pickupAddress) {
    uni.showToast({ title: '请输入取件地址', icon: 'none' })
    return
  }
  if (!formData.deliveryAddress) {
    uni.showToast({ title: '请输入送达地址', icon: 'none' })
    return
  }
  if (!formData.serviceFee) {
    uni.showToast({ title: '请输入报酬金额', icon: 'none' })
    return
  }
  if (!formData.estimatedTime) {
    uni.showToast({ title: '请选择期望完成时间', icon: 'none' })
    return
  }
  if (!formData.receiverPhone) {
    uni.showToast({ title: '请输入联系电话', icon: 'none' })
    return
  }

  const submitData = {
    errandType: formData.errandType,
    title: formData.title,
    description: formData.description,
    pickupAddress: formData.pickupAddress,
    deliveryAddress: formData.deliveryAddress,
    serviceFee: Number(formData.serviceFee),
    estimatedTime: formData.estimatedTime,
    receiverPhone: formData.receiverPhone
  }

  errandApi.publishErrand(submitData).then(() => {
    uni.showToast({ title: '发布成功', icon: 'success' })
    setTimeout(() => { uni.navigateBack() }, 1000)
  }).catch(() => {
    uni.showToast({ title: '发布失败，请重试', icon: 'none' })
  })
}
</script>

<style lang="scss" scoped>
.publish-page {
  min-height: 100vh;
  background: #f5f5f7;
  padding-bottom: 140rpx;
}

/* 表单 */
.form-section {
  background: #fff;
  margin-top: 20rpx;
  padding: 0 24rpx;
}

.form-group {
  padding: 24rpx 0;
  border-bottom: 1rpx solid #f5f5f7;

  &:last-child {
    border-bottom: none;
  }
}

.form-label {
  font-size: 28rpx;
  color: #333;
  font-weight: 500;
  margin-bottom: 16rpx;
  display: block;
}

.form-input {
  width: 100%;
  height: 80rpx;
  background: #f9f9f9;
  border-radius: 12rpx;
  padding: 0 20rpx;
  font-size: 28rpx;
  box-sizing: border-box;
}

.form-textarea {
  width: 100%;
  height: 200rpx;
  background: #f9f9f9;
  border-radius: 12rpx;
  padding: 20rpx;
  font-size: 28rpx;
  box-sizing: border-box;
}

.char-count {
  text-align: right;
  font-size: 22rpx;
  color: #999;
  margin-top: 8rpx;
  display: block;
}

/* 选择器输入框 */
.picker-input {
  width: 100%;
  height: 80rpx;
  background: #f9f9f9;
  border-radius: 12rpx;
  padding: 0 20rpx;
  font-size: 28rpx;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.picker-input text {
  font-size: 28rpx;
  color: #333;

  &.placeholder {
    color: #999;
  }
}

.arrow {
  color: #ccc;
  font-size: 36rpx;
}

/* 底部按钮 */
.submit-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 20rpx 24rpx;
  background: #fff;
  border-top: 1rpx solid #eee;
  z-index: 100;
}

.submit-btn {
  width: 100%;
  height: 96rpx;
  line-height: 96rpx;
  text-align: center;
  background: linear-gradient(135deg, #6c4ee6 0%, #8b5cf6 100%);
  color: #fff;
  font-size: 32rpx;
  font-weight: bold;
  border-radius: 48rpx;
}
</style>
