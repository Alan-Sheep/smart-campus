<template>
  <!-- 发布二手商品页 -->
  <view class="publish-page">
    <!-- 表单 -->
    <view class="form-section">
      <view class="form-group">
        <text class="form-label">商品标题</text>
        <input 
          class="form-input" 
          placeholder="请输入商品标题" 
          v-model="formData.title"
        />
      </view>

      <view class="form-group">
        <text class="form-label">商品分类</text>
        <picker :range="categoryLabels" @change="onCategoryChange">
          <view class="picker-input">
            <text :class="{ placeholder: !formData.categoryId }">
              {{ formData.categoryId ? currentCategoryLabel : '请选择商品分类' }}
            </text>
            <text class="arrow">›</text>
          </view>
        </picker>
      </view>

      <view class="form-group">
        <text class="form-label">商品描述</text>
        <textarea 
          class="form-textarea" 
          placeholder="请详细描述商品情况、成色、使用时长等" 
          v-model="formData.description"
          maxlength="500"
        ></textarea>
        <text class="char-count">{{ formData.description.length }}/500</text>
      </view>

      <view class="form-group">
        <text class="form-label">出售价格（元）</text>
        <input 
          class="form-input" 
          type="digit" 
          placeholder="请输入出售价格" 
          v-model="formData.price"
        />
      </view>

      <view class="form-group">
        <text class="form-label">商品图片</text>
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
        <text class="upload-tip">最多上传6张图片</text>
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
 * 发布二手商品页
 */
import { reactive, computed, onMounted, ref } from 'vue'
import { goodsApi, fileApi } from '@/api/index'
import { checkLogin } from '@/utils/request'
import { guardWrite } from '@/utils/school-permission'

// 提交中状态（防止重复提交）
const submitting = ref(false)

const formData = reactive({
  title: '',
  categoryId: '',
  description: '',
  price: '',
  images: []
})

const categoryList = reactive([])

const categoryLabels = computed(() => categoryList.map(c => c.name || c.label || ''))

const currentCategoryLabel = computed(() => {
  const item = categoryList.find(c => String(c.id) === String(formData.categoryId))
  return item ? (item.name || item.label || '') : ''
})

onMounted(() => {
  loadCategories()
})

/**
 * 加载商品分类
 */
function loadCategories() {
  goodsApi.getGoodsCategory().then(res => {
    const list = res.records || res.list || res || []
    categoryList.push(...list)
  }).catch(() => {
    // 接口失败时使用默认分类
    categoryList.push(
      { id: '1', name: '数码电子' },
      { id: '2', name: '图书教材' },
      { id: '3', name: '生活用品' },
      { id: '4', name: '服饰鞋包' },
      { id: '5', name: '运动户外' },
      { id: '6', name: '其他' }
    )
  })
}

/**
 * 选择分类
 */
function onCategoryChange(e) {
  const idx = e.detail.value
  if (categoryList[idx]) {
    formData.categoryId = categoryList[idx].id
  }
}

/**
 * 上传图片
 */
function uploadImage() {
  uni.chooseImage({
    count: 6 - formData.images.length,
    success: (res) => {
      formData.images.push(...res.tempFilePaths)
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
 * 判断是否已经是远程地址（避免重复上传）
 */
function isRemoteUrl(p) {
  return typeof p === 'string' && /^(https?:)?\/\//.test(p)
}

/**
 * 提交发布
 *
 * 修复：原先直接把 uni.chooseImage 返回的**本地临时路径**逗号拼接后提交，
 * 这些路径只在本机有效，其他用户看到的是破图，且应用重启后失效。
 * 现在先逐张上传到服务器，拿到真实 URL 再提交。
 */
async function submitPublish() {
  if (guardWrite()) return
  if (!checkLogin()) return
  if (!formData.title) {
    uni.showToast({ title: '请输入商品标题', icon: 'none' })
    return
  }
  if (!formData.categoryId) {
    uni.showToast({ title: '请选择商品分类', icon: 'none' })
    return
  }
  if (!formData.description) {
    uni.showToast({ title: '请输入商品描述', icon: 'none' })
    return
  }
  if (!formData.price) {
    uni.showToast({ title: '请输入出售价格', icon: 'none' })
    return
  }
  if (formData.images.length === 0) {
    uni.showToast({ title: '请至少上传一张商品图片', icon: 'none' })
    return
  }
  if (submitting.value) return
  submitting.value = true

  try {
    // 逐张上传本地图片，已是远程地址的跳过
    const uploaded = []
    for (const path of formData.images) {
      if (isRemoteUrl(path)) {
        uploaded.push(path)
        continue
      }
      const res = await fileApi.uploadImage(path)
      const url = res && (res.url || res.fileUrl)
      if (url) {
        uploaded.push(url)
      }
    }

    if (uploaded.length === 0) {
      uni.showToast({ title: '图片上传失败，请重试', icon: 'none' })
      return
    }

    const submitData = {
      title: formData.title,
      categoryId: formData.categoryId,
      description: formData.description,
      price: Number(formData.price),
      images: uploaded.join(',')
    }

    await goodsApi.publishGoods(submitData)
    uni.showToast({ title: '发布成功', icon: 'success' })
    setTimeout(() => { uni.navigateBack() }, 1000)
  } catch (err) {
    // request / uploadImage 已统一提示，这里兜底
    console.warn('发布商品失败:', err)
  } finally {
    submitting.value = false
  }
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

.upload-tip {
  font-size: 22rpx;
  color: #999;
  margin-top: 12rpx;
  display: block;
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
