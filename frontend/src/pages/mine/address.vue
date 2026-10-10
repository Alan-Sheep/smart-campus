<template>
  <view class="address-page">
    <!-- 地址列表 -->
    <view class="address-list" v-if="list.length > 0">
      <view
        class="address-item"
        v-for="item in list"
        :key="item.id"
      >
        <view class="address-content" @click="selectAddress(item)">
          <view class="address-header">
            <text class="name">{{ item.name }}</text>
            <text class="phone">{{ item.phone }}</text>
            <view class="tag-default" v-if="item.isDefault">默认</view>
          </view>
          <text class="address-detail">{{ item.province }}{{ item.city }}{{ item.district }}{{ item.detail }}</text>
        </view>
        <view class="address-actions">
          <view class="action-btn" @click="setDefault(item)" v-if="!item.isDefault">
            <text>设为默认</text>
          </view>
          <view class="action-btn" @click="editAddress(item)">
            <text>编辑</text>
          </view>
          <view class="action-btn delete" @click="deleteAddress(item)">
            <text>删除</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 空状态 -->
    <view class="empty-state" v-else-if="!loading">
      <text class="empty-icon">📍</text>
      <text class="empty-title">暂无收货地址</text>
      <text class="empty-desc">添加地址，方便快速下单</text>
      <button class="add-btn" @click="addAddress">添加地址</button>
    </view>

    <!-- 加载中 -->
    <view class="loading-state" v-if="loading">
      <text>加载中...</text>
    </view>

    <!-- 底部添加按钮 -->
    <view class="bottom-bar" v-if="list.length > 0">
      <button class="add-btn" @click="addAddress">
        <text class="plus">+</text>
        <text>新增收货地址</text>
      </button>
    </view>

    <!-- 地址表单弹窗 -->
    <view class="modal-mask" v-if="showForm" @click="closeForm">
      <view class="modal-content" @click.stop>
        <view class="modal-header">
          <text class="modal-title">{{ isEdit ? '编辑地址' : '新增地址' }}</text>
          <text class="modal-close" @click="closeForm">✕</text>
        </view>
        <view class="form-body">
          <!-- 收货人姓名 -->
          <view class="form-item">
            <text class="form-label">收货人</text>
            <input
              class="form-input"
              v-model="form.name"
              placeholder="请输入收货人姓名"
              maxlength="20"
            />
          </view>
          <!-- 手机号 -->
          <view class="form-item">
            <text class="form-label">手机号</text>
            <input
              class="form-input"
              v-model="form.phone"
              type="number"
              placeholder="请输入11位手机号"
              maxlength="11"
            />
          </view>
          <!-- 快捷填充：地图定位 / 粘贴识别 -->
          <view class="quick-actions">
            <view class="quick-btn" @click="locateByAmap">📍 地图定位</view>
            <view class="quick-btn" @click="pasteRecognize">📋 粘贴识别</view>
          </view>
          <!-- 省市区 -->
          <view class="form-item">
            <text class="form-label">所在地区</text>
            <input
              class="form-input"
              v-model="form.province"
              placeholder="请输入省市区，如：广东省深圳市南山区"
            />
          </view>
          <!-- 详细地址 -->
          <view class="form-item">
            <text class="form-label">详细地址</text>
            <textarea
              class="form-textarea"
              v-model="form.detail"
              placeholder="请输入详细地址（街道、门牌号等）"
              maxlength="200"
              :auto-height="true"
            />
          </view>
          <!-- 设为默认 -->
          <view class="form-item switch-item">
            <text class="form-label">设为默认地址</text>
            <switch
              :checked="form.isDefault"
              color="#6c4ee6"
              @change="onSwitchChange"
            />
          </view>
        </view>
        <view class="modal-footer">
          <button class="cancel-btn" @click="closeForm">取消</button>
          <button class="confirm-btn" @click="saveAddress">保存</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
/**
 * 地址管理页面
 * 基于 localStorage 实现收货地址的增删改查和设为默认
 * localStorage key: 'addressList'
 */
import { ref, reactive } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'

const STORAGE_KEY = 'addressList'

const list = ref([])
const loading = ref(false)
const showForm = ref(false)
const isEdit = ref(false)

// 表单数据
const form = reactive({
  id: null,
  name: '',
  phone: '',
  province: '',
  detail: '',
  isDefault: false
})

onLoad(() => {
  loadList()
})

onShow(() => {
  // 页面显示时刷新列表
  loadList()
})

/**
 * 从 localStorage 读取地址列表
 */
function loadList() {
  loading.value = true
  try {
    const data = uni.getStorageSync(STORAGE_KEY)
    if (data && Array.isArray(data)) {
      list.value = data
    } else {
      list.value = []
    }
  } catch (e) {
    console.error('读取地址列表失败', e)
    list.value = []
  } finally {
    loading.value = false
  }
}

/**
 * 保存地址列表到 localStorage
 */
function saveListToStorage() {
  try {
    uni.setStorageSync(STORAGE_KEY, list.value)
  } catch (e) {
    console.error('保存地址列表失败', e)
  }
}

/**
 * 生成唯一ID
 */
function genId() {
  return Date.now().toString(36) + Math.random().toString(36).slice(2, 8)
}

/**
 * 重置表单
 */
function resetForm() {
  form.id = null
  form.name = ''
  form.phone = ''
  form.province = ''
  form.detail = ''
  form.isDefault = false
}

/**
 * 打开新增表单
 */
function addAddress() {
  resetForm()
  isEdit.value = false
  showForm.value = true
}

/**
 * 打开编辑表单
 */
function editAddress(item) {
  resetForm()
  isEdit.value = true
  form.id = item.id
  form.name = item.name
  form.phone = item.phone
  // 合并省市区显示到 province 字段
  form.province = `${item.province || ''}${item.city || ''}${item.district || ''}`
  form.detail = item.detail || ''
  form.isDefault = !!item.isDefault
  showForm.value = true
}

/**
 * 关闭表单
 */
function closeForm() {
  showForm.value = false
}

/**
 * switch 开关变化
 */
function onSwitchChange(e) {
  form.isDefault = e.detail.value
}

/**
 * 表单校验
 * @returns {string|null} 错误信息，null 表示通过
 */
function validateForm() {
  if (!form.name.trim()) {
    return '请输入收货人姓名'
  }
  if (!form.phone.trim()) {
    return '请输入手机号'
  }
  if (!/^1[3-9]\d{9}$/.test(form.phone.trim())) {
    return '请输入正确的11位手机号'
  }
  if (!form.province.trim()) {
    return '请输入所在地区'
  }
  if (!form.detail.trim()) {
    return '请输入详细地址'
  }
  return null
}

/**
 * 保存地址（新增或编辑）
 */
/**
 * 高德定位填充（H5: 高德 JS API；小程序: uni.getLocation + 高德逆地理）
 * Key：Web b8e2305360db8434bbcc0c0010392560 / 小程序 ecb1ccd0482b597998f789403b7e26af
 */
function locateByAmap() {
  // #ifdef H5
  uni.showLoading({ title: '定位中...' })
  const ensureAmap = () => new Promise((resolve, reject) => {
    if (window.AMap) return resolve(window.AMap)
    const s = document.createElement('script')
    s.src = 'https://webapi.amap.com/maps?v=2.0&key=b8e2305360db8434bbcc0c0010392560&plugin=AMap.Geocoder'
    s.onload = () => resolve(window.AMap)
    s.onerror = reject
    document.head.appendChild(s)
  })
  ensureAmap().then(AMap => {
    AMap.plugin('AMap.Geolocation', () => {
      const geo = new AMap.Geolocation({ enableHighAccuracy: true, timeout: 10000 })
      geo.getCurrentPosition((status, result) => {
        uni.hideLoading()
        if (status === 'complete' && result.position) {
          const geocoder = new AMap.Geocoder()
          geocoder.getAddress([result.position.lng, result.position.lat], (st, res) => {
            if (st === 'complete' && res.regeocode) {
              const comp = res.regeocode.addressComponent
              form.province = (comp.province || '') + (comp.city || '') + (comp.district || '')
              form.detail = (comp.township || '') + (comp.street || '') + (comp.streetNumber || '') || res.regeocode.formattedAddress
              uni.showToast({ title: '已填入定位地址', icon: 'success' })
            } else {
              uni.showToast({ title: '定位成功但解析失败', icon: 'none' })
            }
          })
        } else {
          uni.showToast({ title: '定位失败，请手动输入', icon: 'none' })
        }
      })
    })
  }).catch(() => { uni.hideLoading(); uni.showToast({ title: '地图加载失败', icon: 'none' }) })
  // #endif
  // #ifdef MP-WEIXIN
  uni.getLocation({
    type: 'gcj02',
    success: (loc) => {
      uni.request({
        url: 'https://restapi.amap.com/v3/geocode/regeo?key=ecb1ccd0482b597998f789403b7e26af&location='
          + loc.longitude + ',' + loc.latitude,
        success: (res) => {
          if (res.data && res.data.regeocode) {
            const comp = res.data.regeocode.addressComponent
            form.province = (comp.province || '') + (comp.city || '') + (comp.district || '')
            form.detail = (comp.township || '') + (comp.street || '') + (comp.streetNumber || '')
            uni.showToast({ title: '已填入定位地址', icon: 'success' })
          } else { uni.showToast({ title: '解析失败', icon: 'none' }) }
        },
        fail: () => uni.showToast({ title: '定位失败', icon: 'none' })
      })
    },
    fail: () => uni.showToast({ title: '获取定位失败', icon: 'none' })
  })
  // #endif
}

/**
 * 粘贴识别：从淘宝/美团/京东复制的地址文本自动抽取 姓名/电话/省市区/详细地址
 */
function pasteRecognize() {
  // #ifdef H5
  navigator.clipboard.readText().then(doRecognize).catch(() => {
    uni.showToast({ title: '请授权剪贴板后重试', icon: 'none' })
  })
  // #endif
  // #ifdef MP-WEIXIN
  uni.getClipboardData({
    success: (res) => doRecognize(res.data),
    fail: () => uni.showToast({ title: '读取剪贴板失败', icon: 'none' })
  })
  // #endif
}

function doRecognize(text) {
  if (!text || text.length < 5) { uni.showToast({ title: '剪贴板没有地址内容', icon: 'none' }); return }
  const t = text.replace(/\r/g, ' ')
  const phone = (t.match(/1[3-9]\d{9}/) || [''])[0]
  let name = ''
  const nameM = t.match(/([\u4e00-\u9fa5]{2,4})\s*1[3-9]\d{9}/)
  if (nameM) name = nameM[1]
  const regionM = t.match(/([^,，\s]+?(?:省|自治区))([^,，\s]+?(?:市|自治州|地区|盟))?([^,，\s]*?(?:区|县|市))/)
  let detail = ''
  if (regionM) {
    const idx = t.indexOf(regionM[0])
    detail = t.substring(idx + regionM[0].length).replace(/1[3-9]\d{9}/, '').trim()
  } else {
    const parts = t.split(/[\s,，]+/).filter(p => /[\u4e00-\u9fa5]{4,}/.test(p))
    detail = parts.sort((a, b) => b.length - a.length)[0] || ''
  }
  if (regionM) form.province = (regionM[1] || '') + (regionM[2] || '') + (regionM[3] || '')
  if (phone) form.phone = phone
  if (detail) form.detail = detail.replace(/^[,，\s]+/, '')
  if (name) form.name = name
  uni.showToast({ title: '识别完成，请核对', icon: 'success' })
}

function saveAddress() {
  const error = validateForm()
  if (error) {
    uni.showToast({ title: error, icon: 'none' })
    return
  }

  // 处理省市区：简化为整体存入 province，city/district 置空
  const addressData = {
    name: form.name.trim(),
    phone: form.phone.trim(),
    province: form.province.trim(),
    city: '',
    district: '',
    detail: form.detail.trim(),
    isDefault: form.isDefault
  }

  if (isEdit.value) {
    // 编辑模式：更新已有地址
    const index = list.value.findIndex(item => item.id === form.id)
    if (index > -1) {
      addressData.id = form.id
      list.value[index] = addressData
    }
  } else {
    // 新增模式
    addressData.id = genId()
    list.value.push(addressData)
  }

  // 如果设为默认，取消其他默认
  if (form.isDefault) {
    list.value.forEach(item => {
      if (item.id !== addressData.id) {
        item.isDefault = false
      }
    })
  }

  // 如果是第一条地址，自动设为默认
  if (list.value.length === 1) {
    list.value[0].isDefault = true
  }

  saveListToStorage()
  showForm.value = false
  uni.showToast({ title: '保存成功', icon: 'success' })
}

/**
 * 删除地址
 */
function deleteAddress(item) {
  uni.showModal({
    title: '提示',
    content: '确定要删除这个地址吗？',
    success: (res) => {
      if (res.confirm) {
        const index = list.value.findIndex(a => a.id === item.id)
        if (index > -1) {
          const wasDefault = list.value[index].isDefault
          list.value.splice(index, 1)
          // 如果删除的是默认地址，将第一条设为默认
          if (wasDefault && list.value.length > 0) {
            list.value[0].isDefault = true
          }
          saveListToStorage()
          uni.showToast({ title: '删除成功', icon: 'success' })
        }
      }
    }
  })
}

/**
 * 设为默认地址
 */
function setDefault(item) {
  list.value.forEach(a => {
    a.isDefault = (a.id === item.id)
  })
  saveListToStorage()
  uni.showToast({ title: '已设为默认', icon: 'success' })
}

/**
 * 选择地址：返回上一页并传递选中的地址信息
 * 使用 eventChannel 传递数据
 */
function selectAddress(item) {
  // 获取页面栈
  const pages = getCurrentPages()
  const prevPage = pages[pages.length - 2]

  // 优先使用 eventChannel
  if (prevPage) {
    const eventChannel = prevPage.getOpenerEventChannel?.()
    if (eventChannel && typeof eventChannel.emit === 'function') {
      eventChannel.emit('selectAddress', { address: item })
    } else {
      // 回退：使用 globalData
      const app = getApp()
      if (app && app.globalData) {
        app.globalData.selectedAddress = item
      }
    }
  }

  uni.navigateBack({
    delta: 1
  })
}
</script>

<style lang="scss" scoped>
.address-page {
  min-height: 100vh;
  background: #f5f5f7;
  padding-bottom: 140rpx;
}

/* 地址列表 */
.address-list {
  padding: 20rpx 24rpx;
}

.address-item {
  background: #fff;
  border-radius: 16rpx;
  margin-bottom: 16rpx;
  overflow: hidden;
}

.address-content {
  padding: 24rpx;
}

.address-header {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-bottom: 12rpx;
}

.name {
  font-size: 30rpx;
  font-weight: bold;
  color: #333;
}

.phone {
  font-size: 26rpx;
  color: #666;
}

.tag-default {
  padding: 4rpx 12rpx;
  background: rgba(108, 78, 230, 0.1);
  color: #6c4ee6;
  font-size: 20rpx;
  border-radius: 6rpx;
}

.address-detail {
  font-size: 26rpx;
  color: #666;
  line-height: 1.5;
}

.address-actions {
  display: flex;
  border-top: 1rpx solid #f5f5f7;
}

.action-btn {
  flex: 1;
  text-align: center;
  padding: 20rpx 0;
  font-size: 24rpx;
  color: #666;
  border-right: 1rpx solid #f5f5f7;

  &:last-child {
    border-right: none;
  }

  &.delete {
    color: #ff4d4f;
  }
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 160rpx 60rpx;
  gap: 20rpx;
}

.empty-icon {
  font-size: 120rpx;
}

.empty-title {
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
  margin-top: 20rpx;
}

.empty-desc {
  font-size: 26rpx;
  color: #999;
}

.add-btn {
  margin-top: 40rpx;
  width: 300rpx;
  height: 80rpx;
  line-height: 80rpx;
  background: #6c4ee6;
  color: #fff;
  font-size: 28rpx;
  border-radius: 40rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8rpx;

  &::after {
    border: none;
  }
}

.plus {
  font-size: 32rpx;
  font-weight: bold;
}

/* 底部添加按钮 */
.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 20rpx 24rpx;
  background: #fff;
  box-shadow: 0 -2rpx 10rpx rgba(0, 0, 0, 0.05);
}

.bottom-bar .add-btn {
  width: 100%;
  height: 88rpx;
  line-height: 88rpx;
  margin: 0;
}

/* 加载中 */
.loading-state {
  text-align: center;
  padding: 60rpx 0;
  font-size: 26rpx;
  color: #999;
}

/* 表单弹窗 */
.modal-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 999;
  display: flex;
  align-items: flex-end;
}

.modal-content {
  width: 100%;
  background: #fff;
  border-radius: 24rpx 24rpx 0 0;
  max-height: 85vh;
  overflow-y: auto;
  animation: slideUp 0.3s ease;
}

@keyframes slideUp {
  from {
    transform: translateY(100%);
  }
  to {
    transform: translateY(0);
  }
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 30rpx 32rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.modal-title {
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
}

.modal-close {
  font-size: 36rpx;
  color: #999;
  padding: 0 10rpx;
}

.form-body {
  padding: 20rpx 32rpx;
}

/* 快捷填充按钮 */
.quick-actions {
  display: flex;
  gap: 16rpx;
  margin-bottom: 4rpx;
}

.quick-btn {
  flex: 1;
  text-align: center;
  padding: 18rpx 0;
  border-radius: 12rpx;
  font-size: 26rpx;
  color: #6c4ee6;
  background: #f3efff;
  border: 1rpx solid #d9cfff;
}

.form-item {
  padding: 20rpx 0;
  border-bottom: 1rpx solid #f5f5f7;
}

.form-label {
  display: block;
  font-size: 26rpx;
  color: #666;
  margin-bottom: 16rpx;
}

.form-input {
  width: 100%;
  height: 72rpx;
  font-size: 28rpx;
  color: #333;
  background: #f9f9f9;
  border-radius: 8rpx;
  padding: 0 20rpx;
  box-sizing: border-box;
}

.form-textarea {
  width: 100%;
  min-height: 120rpx;
  font-size: 28rpx;
  color: #333;
  background: #f9f9f9;
  border-radius: 8rpx;
  padding: 16rpx 20rpx;
  box-sizing: border-box;
  line-height: 1.5;
}

.switch-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: none;

  .form-label {
    margin-bottom: 0;
  }
}

.modal-footer {
  display: flex;
  gap: 20rpx;
  padding: 20rpx 32rpx 40rpx;
}

.cancel-btn,
.confirm-btn {
  flex: 1;
  height: 84rpx;
  line-height: 84rpx;
  font-size: 30rpx;
  border-radius: 42rpx;
  margin: 0;

  &::after {
    border: none;
  }
}

.cancel-btn {
  background: #f5f5f7;
  color: #666;
}

.confirm-btn {
  background: #6c4ee6;
  color: #fff;
}
</style>
