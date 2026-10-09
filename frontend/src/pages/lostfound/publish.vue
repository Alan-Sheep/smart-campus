<template>
  <!-- 发布失物招领页 -->
  <view class="publish-page">
    <!-- 类型切换 -->
    <view class="type-switch">
      <view 
        class="type-btn" 
        :class="{ active: formData.type === 'lost' }"
        @click="formData.type = 'lost'"
      >
        <text class="type-icon">😢</text>
        <text>我丢了东西</text>
      </view>
      <view 
        class="type-btn" 
        :class="{ active: formData.type === 'found' }"
        @click="formData.type = 'found'"
      >
        <text class="type-icon">😊</text>
        <text>我捡到东西</text>
      </view>
    </view>

    <!-- 表单 -->
    <view class="form-section">
      <view class="form-group">
        <text class="form-label">物品名称</text>
        <input 
          class="form-input" 
          :placeholder="formData.type === 'lost' ? '例如：丢失的钱包' : '例如：捡到的钥匙'"
          v-model="formData.title"
        />
      </view>

      <view class="form-group">
        <text class="form-label">{{ formData.type === 'lost' ? '丢失地点' : '捡到地点' }}</text>
        <input 
          class="form-input" 
          placeholder="请输入地点" 
          v-model="formData.location"
        />
      </view>

      <view class="form-group">
        <text class="form-label">详细描述</text>
        <textarea 
          class="form-textarea" 
          placeholder="请详细描述物品的特征，方便识别" 
          v-model="formData.description"
          maxlength="500"
        ></textarea>
        <text class="char-count">{{ formData.description.length }}/500</text>
      </view>

      <view class="form-group">
        <text class="form-label">物品图片</text>
        <view class="image-upload">
          <view 
            class="upload-item" 
            v-for="(img, idx) in formData.images" 
            :key="idx"
          >
            <image :src="img" mode="aspectFill" class="uploaded-image"></image>
            <view class="delete-btn" @click="removeImage(idx)">×</view>
          </view>
          <view 
            class="upload-btn" 
            v-if="formData.images.length < 6"
            @click="uploadImage"
          >
            <text class="plus">+</text>
            <text class="upload-text">添加图片</text>
          </view>
        </view>
      </view>

      <view class="form-group">
        <text class="form-label">联系方式</text>
        <input 
          class="form-input" 
          type="number" 
          placeholder="请输入手机号/微信号" 
          v-model="formData.contact"
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
 * 发布失物招领页
 */
import { reactive, onMounted } from 'vue'
import { lostFoundApi } from '@/api/index'
import { checkLogin } from '@/utils/request'
import { guardWrite } from '@/utils/school-permission'

const formData = reactive({
  type: 'lost', // lost 丢失, found 拾到
  title: '',
  location: '',
  description: '',
  images: [],
  contact: ''
})

onMounted(() => {
  // 获取页面参数
  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1]
  const type = currentPage.options?.type
  if (type) {
    formData.type = type
  }
})

/**
 * 上传图片
 */
function uploadImage() {
  uni.chooseImage({
    count: 6 - formData.images.length,
    success: (res) => {
      formData.images.push(...res.tempFilePaths)
      // 实际上传到服务器
      // res.tempFilePaths.forEach(path => {
      //   uploadApi.uploadImage(path).then(res => {
      //     formData.images.push(res.data.url)
      //   })
      // })
    }
  })
}

/**
 * 删除图片
 */
function removeImage(index) {
  formData.images.splice(index, 1)
}

/**
 * 提交发布
 */
function submitPublish() {
  if (guardWrite()) return
  if (!checkLogin()) return
  if (!formData.title) {
    uni.showToast({ title: '请输入物品名称', icon: 'none' })
    return
  }
  if (!formData.location) {
    uni.showToast({ 
      title: '请输入' + (formData.type === 'lost' ? '丢失' : '捡到') + '地点', 
      icon: 'none' 
    })
    return
  }
  if (!formData.description) {
    uni.showToast({ title: '请输入详细描述', icon: 'none' })
    return
  }
  if (!formData.contact) {
    uni.showToast({ title: '请输入联系方式', icon: 'none' })
    return
  }

  // 真实接口
  // lostFoundApi.publishLostFound(formData).then(res => {
  //   uni.showToast({ title: '发布成功', icon: 'success' })
  //   setTimeout(() => { uni.navigateBack() }, 1000)
  // })

  uni.showToast({ title: '发布成功', icon: 'success' })
  setTimeout(() => { uni.navigateBack() }, 1000)
}
</script>

<style lang="scss" scoped>
.publish-page {
  min-height: 100vh;
  background: #f5f5f7;
  padding-bottom: 140rpx;
}

/* 类型切换 */
.type-switch {
  display: flex;
  gap: 20rpx;
  padding: 30rpx 24rpx;
  background: #fff;
}

.type-btn {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 30rpx;
  background: #f9f9f9;
  border-radius: 16rpx;
  border: 2rpx solid transparent;
  font-size: 28rpx;
  color: #666;
  gap: 12rpx;

  &.active {
    background: #fff7e6;
    border-color: #fa8c16;
    color: #fa8c16;
    font-weight: bold;
  }
}

.type-icon {
  font-size: 48rpx;
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

/* 图片上传 */
.image-upload {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}

.upload-item {
  position: relative;
  width: 180rpx;
  height: 180rpx;
}

.uploaded-image {
  width: 100%;
  height: 100%;
  border-radius: 12rpx;
}

.delete-btn {
  position: absolute;
  top: -12rpx;
  right: -12rpx;
  width: 40rpx;
  height: 40rpx;
  line-height: 36rpx;
  text-align: center;
  background: #ff4d4f;
  color: #fff;
  border-radius: 50%;
  font-size: 28rpx;
}

.upload-btn {
  width: 180rpx;
  height: 180rpx;
  border: 2rpx dashed #ccc;
  border-radius: 12rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  color: #999;
}

.plus {
  font-size: 48rpx;
  line-height: 1;
}

.upload-text {
  font-size: 22rpx;
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
  background: #333;
  color: #fff;
  font-size: 32rpx;
  font-weight: bold;
  border-radius: 48rpx;
}
</style>
