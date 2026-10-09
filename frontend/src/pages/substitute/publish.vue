<template>
  <!-- 发布代课服务页 -->
  <view class="publish-page">
    <!-- 表单 -->
    <view class="form-section">
      <view class="form-group">
        <text class="form-label">课程名称</text>
        <input 
          class="form-input" 
          placeholder="请输入课程名称" 
          v-model="formData.courseName"
        />
      </view>

      <view class="form-group">
        <text class="form-label">上课日期</text>
        <picker mode="date" @change="onDateChange">
          <view class="picker-input">
            <text :class="{ placeholder: !formData.courseDate }">
              {{ formData.courseDate || '请选择上课日期' }}
            </text>
            <text class="arrow">›</text>
          </view>
        </picker>
      </view>

      <view class="form-group">
        <text class="form-label">上课时间</text>
        <input 
          class="form-input" 
          placeholder="例如：周二第3-4节" 
          v-model="formData.courseTime"
        />
      </view>

      <view class="form-group">
        <text class="form-label">上课地点</text>
        <input 
          class="form-input" 
          placeholder="请输入上课地点/教室" 
          v-model="formData.classroom"
        />
      </view>

      <view class="form-group">
        <text class="form-label">代课老师</text>
        <input 
          class="form-input" 
          placeholder="请输入代课老师姓名" 
          v-model="formData.teacher"
        />
      </view>

      <view class="form-group">
        <text class="form-label">报酬金额（元）</text>
        <input 
          class="form-input" 
          type="digit" 
          placeholder="请输入报酬金额" 
          v-model="formData.reward"
        />
      </view>

      <view class="form-group">
        <text class="form-label">备注说明</text>
        <textarea 
          class="form-textarea" 
          placeholder="请输入其他备注说明" 
          v-model="formData.requirement"
          maxlength="200"
        ></textarea>
        <text class="char-count">{{ formData.requirement.length }}/200</text>
      </view>

      <view class="form-group">
        <text class="form-label">联系方式</text>
        <input 
          class="form-input" 
          placeholder="请输入手机号/微信号" 
          v-model="formData.contactWechat"
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
 * 发布代课服务页
 */
import { reactive } from 'vue'
import { substituteApi } from '@/api/index'
import { checkLogin } from '@/utils/request'
import { guardWrite } from '@/utils/school-permission'

const formData = reactive({
  courseName: '',
  courseDate: '',
  courseTime: '',
  classroom: '',
  teacher: '',
  reward: '',
  requirement: '',
  contactWechat: ''
})

/**
 * 选择日期
 */
function onDateChange(e) {
  formData.courseDate = e.detail.value
}

/**
 * 提交发布
 */
function submitPublish() {
  if (guardWrite()) return
  if (!checkLogin()) return
  if (!formData.courseName) {
    uni.showToast({ title: '请输入课程名称', icon: 'none' })
    return
  }
  if (!formData.courseDate) {
    uni.showToast({ title: '请选择上课日期', icon: 'none' })
    return
  }
  if (!formData.courseTime) {
    uni.showToast({ title: '请输入上课时间', icon: 'none' })
    return
  }
  if (!formData.classroom) {
    uni.showToast({ title: '请输入上课地点', icon: 'none' })
    return
  }
  if (!formData.teacher) {
    uni.showToast({ title: '请输入代课老师', icon: 'none' })
    return
  }
  if (!formData.reward) {
    uni.showToast({ title: '请输入报酬金额', icon: 'none' })
    return
  }
  if (!formData.requirement) {
    uni.showToast({ title: '请输入备注说明', icon: 'none' })
    return
  }
  if (!formData.contactWechat) {
    uni.showToast({ title: '请输入联系方式', icon: 'none' })
    return
  }

  const submitData = {
    courseName: formData.courseName,
    courseDate: formData.courseDate,
    courseTime: formData.courseTime,
    classroom: formData.classroom,
    teacher: formData.teacher,
    reward: Number(formData.reward),
    requirement: formData.requirement,
    contactWechat: formData.contactWechat
  }

  substituteApi.publishSubstitute(submitData).then(() => {
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
