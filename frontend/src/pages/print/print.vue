<template>
  <!-- 打印服务?-->
  <view class="print-page">
    <!-- 顶部说明 -->
    <view class="top-banner">
      <view class="banner-icon">🖨️</view>
      <view class="banner-info">
        <text class="banner-title">校园打印服务</text>
        <text class="banner-desc">支持多种格式，快速打印，送货上门</text>
      </view>
    </view>

    <!-- 打印类型选择 -->
    <view class="section">
      <view class="section-title">打印类型</view>
      <view class="type-grid">
        <view 
          class="type-item" 
          :class="{ active: formData.printType === item.id }"
          v-for="item in printTypes" 
          :key="item.id"
          @click="selectType(item.id)"
        >
          <text class="type-icon">{{ item.icon }}</text>
          <text class="type-name">{{ item.name }}</text>
          <text class="type-price">￥{{ item.price }}/页</text>
        </view>
      </view>
    </view>

    <!-- 打印参数 -->
    <view class="section">
      <view class="section-title">打印参数</view>
      
      <view class="form-row">
        <text class="form-label">纸张大小</text>
        <view class="form-options">
          <view 
            class="option-item" 
            :class="{ active: formData.paperSize === item }"
            v-for="item in paperSizes" 
            :key="item"
            @click="formData.paperSize = item"
          >
            {{ item }}
          </view>
        </view>
      </view>

      <view class="form-row">
        <text class="form-label">打印份数</text>
        <view class="counter">
          <view class="counter-btn" @click="changeCount(-1)">-</view>
          <text class="counter-num">{{ formData.copies }}</text>
          <view class="counter-btn" @click="changeCount(1)">+</view>
        </view>
      </view>

      <view class="form-row">
        <text class="form-label">是否双面</text>
        <switch :checked="formData.isDouble" color="#6c4ee6" @change="toggleDouble" />
      </view>

      <view class="form-row">
        <text class="form-label">是否彩印</text>
        <switch :checked="formData.isColor" color="#6c4ee6" @change="toggleColor" />
      </view>
    </view>

    <!-- 文件上传 -->
    <view class="section">
      <view class="section-title">上传文件</view>
      <view class="upload-area" @click="uploadFile">
        <view v-if="!formData.fileName" class="upload-placeholder">
          <text class="upload-icon">📄</text>
          <text class="upload-text">点击上传文件</text>
          <text class="upload-tip">支持 PDF、Word、Excel、PPT 等格：</text>
        </view>
        <view v-else class="file-info">
          <text class="file-icon">📎</text>
          <text class="file-name">{{ formData.fileName }}</text>
          <text class="file-del" @click.stop="removeFile">×</text>
        </view>
      </view>
    </view>

    <!-- 配送信?-->
    <view class="section">
      <view class="section-title">配送信息</view>
      <view class="form-row">
        <text class="form-label">收货地址</text>
        <input class="form-input" placeholder="请输入宿舍号/详细地址" v-model="formData.address" />
      </view>
      <view class="form-row">
        <text class="form-label">联系电话</text>
        <input class="form-input" type="number" placeholder="请输入联系电话"? v-model="formData.phone" />
      </view>
      <view class="form-row">
        <text class="form-label">备注说明</text>
        <input class="form-input" placeholder="如有特殊要求请备注"? v-model="formData.remark" />
      </view>
    </view>

    <!-- 价格计算 -->
    <view class="price-section">
      <view class="price-row">
        <text class="price-label">打印费用</text>
        <text class="price-value">¥{{ calculatedPrice.toFixed(2) }}</text>
      </view>
      <view class="price-row">
        <text class="price-label">配送费：</text>
        <text class="price-value">¥2.00</text>
      </view>
      <view class="price-row total">
        <text class="price-label">合计</text>
        <text class="price-value">¥{{ (calculatedPrice + 2).toFixed(2) }}</text>
      </view>
    </view>

    <!-- 底部提交按钮 -->
    <view class="submit-bar">
      <view class="total-info">
        <text class="total-label">合计：</text>
        <text class="total-price">¥{{ (calculatedPrice + 2).toFixed(2) }}</text>
      </view>
      <view class="submit-btn" @click="submitOrder">
        提交订单
      </view>
    </view>

    <view style="height: 140rpx;"></view>
  </view>
</template>

<script setup>
/**
 * 打印服务? * 用户可以选择打印类型、参数，上传文件，提交打印订? * 调用真实后端接口提交订单
 */
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { printApi } from '@/api/index'
import { fileApi } from '@/api/index'
import { checkLogin } from '@/utils/request'
import { guardWrite, onSchoolChanged } from '@/utils/school-permission'

// 打印类型
const printTypes = ref([
  { id: 'document', name: '文档打印', icon: '📄', price: 0.2 },
  { id: 'photo', name: '照片打印', icon: '🖼️', price: 2 },
  { id: 'poster', name: '海报打印', icon: '📋', price: 5 },
  { id: 'other', name: '其他', icon: '📦', price: 1 }
])

// 纸张大小
const paperSizes = ['A4', 'A3', 'B5']

// 表单数据
const formData = reactive({
  printType: 'document',
  paperSize: 'A4',
  copies: 1,
  isDouble: false,
  isColor: false,
  fileName: '',
  fileUrl: '',
  address: '',
  phone: '',
  remark: ''
})

// 提交中状?
const submitting = ref(false)

// 计算价格（简单计算，实际以后端为准）
const calculatedPrice = computed(() => {
  const type = printTypes.value.find(t => t.id === formData.printType)
  let basePrice = type ? type.price : 0.2
  // 彩印加价
  if (formData.isColor) basePrice *= 3
  // 双面不额外加价，但省?
let price = basePrice * formData.copies
  return price
})

onMounted(() => {
  // loadPriceConfig()
})
// ---- 切换学校后刷新 ----
// 用户切换学校返回本页时，重新拉取当前学校的数据
// hasLoadedOnce 避免与 onMounted 首屏加载重复请求
let hasLoadedOnce = false
onShow(() => {
  onSchoolChanged()
  if (hasLoadedOnce) { loadPriceConfig() }
  hasLoadedOnce = true
})

/**
 * 获取打印价格配置
 */
async function loadPriceConfig() {
  try {
    const config = await printApi.getPriceConfig()
    // 更新打印类型和价格
    if (config && config.types) {
      printTypes.value = config.types
    }
  } catch (err) {
    console.warn('获取打印价格配置失败:', err)
  }
}

/**
 * 选择打印类型
 */
function selectType(id) {
  formData.printType = id
}

/**
 * 修改份数
 */
function changeCount(delta) {
  const newCount = formData.copies + delta
  if (newCount >= 1 && newCount <= 100) {
    formData.copies = newCount
  }
}

/**
 * 切换双面
 */
function toggleDouble(e) {
  formData.isDouble = e.detail.value
}

/**
 * 切换彩印
 */
function toggleColor(e) {
  formData.isColor = e.detail.value
}

/**
 * 上传文件
 * 调用真实文件上传接口
 */
function uploadFile() {
  // 选择文件
  uni.chooseMessageFile({
    count: 1,
    type: 'file',
    success: async (res) => {
      const file = res.tempFiles[0]
      formData.fileName = file.name

      try {
        // 调用文件上传接口
const uploadResult = await fileApi.uploadFile(file.path)
        formData.fileUrl = uploadResult.url || uploadResult.fileUrl || file.path
        uni.showToast({ title: '上传成功', icon: 'success' })
      } catch (err) {
        // 上传失败
        console.warn('文件上传失败:', err)
        formData.fileUrl = file.path // 保留本地路径作为降级
      }
    },
    fail: () => {
      uni.showToast({ title: '已取消选择', icon: 'none' })
    }
  })
}

/**
 * 删除文件
 */
function removeFile() {
  formData.fileName = ''
  formData.fileUrl = ''
}

/**
 * 提交订单
 * 调用真实提交打印订单接口
 */
async function submitOrder() {
  if (guardWrite()) return
  if (!checkLogin()) return
  if (!formData.fileName) {
    uni.showToast({ title: '请先上传文件', icon: 'none' })
    return
  }
  if (!formData.address) {
    uni.showToast({ title: '请输入收货地址', icon: 'none' })
    return
  }
  if (!formData.phone) {
    uni.showToast({ title: '请输入联系电话', icon: 'none' })
    return
  }

  if (submitting.value) return
  submitting.value = true

  try {
    // 调用提交打印订单接口
    await printApi.submitPrint({
      ...formData,
      price: calculatedPrice.value + 2 // 打印?+ 配送费
    })
    uni.showToast({ title: '下单成功', icon: 'success' })
    setTimeout(() => { uni.navigateBack() }, 1000)
  } catch (err) {
    // 下单失败，request 已统一处理错误提示
    console.warn('提交打印订单失败:', err)
  } finally {
    submitting.value = false
  }
}
</script>

<style lang="scss" scoped>
.print-page {
  min-height: 100vh;
  background: #f5f5f7;
  padding-bottom: 140rpx;
}

/* 顶部Banner */
.top-banner {
  display: flex;
  align-items: center;
  gap: 24rpx;
  padding: 30rpx 24rpx;
  background: linear-gradient(135deg, #45b7d1, #4ecdc4);
  margin: 20rpx 24rpx;
  border-radius: 20rpx;
  color: #fff;
}

.banner-icon {
  font-size: 60rpx;
}

.banner-info {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.banner-title {
  font-size: 32rpx;
  font-weight: bold;
}

.banner-desc {
  font-size: 24rpx;
  opacity: 0.9;
}

/* 通用区块 */
.section {
  background: #fff;
  margin: 20rpx 24rpx;
  border-radius: 20rpx;
  padding: 24rpx;
}

.section-title {
  font-size: 30rpx;
  font-weight: bold;
  color: #333;
  margin-bottom: 24rpx;
}

/* 类型网格 */
.type-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20rpx;
}

.type-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20rpx 10rpx;
  border: 2rpx solid #eee;
  border-radius: 16rpx;
  gap: 8rpx;

  &.active {
    border-color: #6c4ee6;
    background: #f7f5ff;
  }
}

.type-icon {
  font-size: 36rpx;
}

.type-name {
  font-size: 24rpx;
  color: #333;
}

.type-price {
  font-size: 20rpx;
  color: #ff4d4f;
}

/* 表单?*/
.form-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24rpx 0;
  border-bottom: 1rpx solid #f5f5f7;

  &:last-child {
    border-bottom: none;
  }
}

.form-label {
  font-size: 28rpx;
  color: #333;
}

.form-input {
  flex: 1;
  text-align: right;
  font-size: 28rpx;
  color: #333;
}

/* 选项 */
.form-options {
  display: flex;
  gap: 16rpx;
}

.option-item {
  padding: 10rpx 24rpx;
  border: 2rpx solid #eee;
  border-radius: 30rpx;
  font-size: 26rpx;
  color: #666;

  &.active {
    border-color: #6c4ee6;
    color: #6c4ee6;
    background: #f7f5ff;
  }
}

/* 计数?*/
.counter {
  display: flex;
  align-items: center;
  gap: 24rpx;
}

.counter-btn {
  width: 56rpx;
  height: 56rpx;
  line-height: 52rpx;
  text-align: center;
  border: 2rpx solid #ddd;
  border-radius: 50%;
  font-size: 32rpx;
  color: #666;
}

.counter-num {
  font-size: 30rpx;
  font-weight: bold;
  min-width: 60rpx;
  text-align: center;
}

/* 文件上传 */
.upload-area {
  border: 2rpx dashed #ddd;
  border-radius: 16rpx;
  padding: 40rpx;
  text-align: center;
}

.upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12rpx;
}

.upload-icon {
  font-size: 48rpx;
}

.upload-text {
  font-size: 28rpx;
  color: #666;
}

.upload-tip {
  font-size: 22rpx;
  color: #999;
}

.file-info {
  display: flex;
  align-items: center;
  gap: 12rpx;
  justify-content: center;
}

.file-icon {
  font-size: 32rpx;
}

.file-name {
  font-size: 28rpx;
  color: #333;
  max-width: 400rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-del {
  width: 40rpx;
  height: 40rpx;
  line-height: 36rpx;
  text-align: center;
  background: #ff4d4f;
  color: #fff;
  border-radius: 50%;
  font-size: 28rpx;
}

/* 价格区域 */
.price-section {
  background: #fff;
  margin: 20rpx 24rpx;
  border-radius: 20rpx;
  padding: 24rpx;
}

.price-row {
  display: flex;
  justify-content: space-between;
  padding: 12rpx 0;
  font-size: 26rpx;

  &.total {
    padding-top: 20rpx;
    margin-top: 12rpx;
    border-top: 1rpx solid #f0f0f0;
    font-size: 30rpx;
    font-weight: bold;

    .price-value {
      color: #ff4d4f;
      font-size: 36rpx;
    }
  }
}

.price-label {
  color: #666;
}

.price-value {
  color: #333;
}

/* 底部提交?*/
.submit-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  padding: 20rpx 24rpx;
  background: #fff;
  border-top: 1rpx solid #eee;
  z-index: 100;
}

.total-info {
  flex: 1;
  display: flex;
  align-items: baseline;
}

.total-label {
  font-size: 26rpx;
  color: #666;
}

.total-price {
  font-size: 40rpx;
  font-weight: bold;
  color: #ff4d4f;
}

.submit-btn {
  padding: 24rpx 60rpx;
  background: linear-gradient(135deg, #6c4ee6, #8b75f0);
  color: #fff;
  font-size: 30rpx;
  font-weight: bold;
  border-radius: 48rpx;
}
</style>