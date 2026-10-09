<template>
  <view class="setting-page">
    <!-- 账号设置 -->
    <view class="section">
      <view class="section-title">账号设置</view>
      <view class="cell-group">
        <view class="cell" @click="goEditProfile">
          <text class="cell-label">个人资料</text>
          <text class="cell-arrow">›</text>
        </view>
        <view class="cell" @click="goChangePassword">
          <text class="cell-label">修改密码</text>
          <text class="cell-arrow">›</text>
        </view>
      </view>
    </view>

    <!-- 通用设置 -->
    <view class="section">
      <view class="section-title">通用设置</view>
      <view class="cell-group">
        <view class="cell" @click="clearCache">
          <text class="cell-label">清除缓存</text>
          <view class="cell-right">
            <text class="cell-value">{{ cacheSize }}</text>
            <text class="cell-arrow">›</text>
          </view>
        </view>
        <view class="cell">
          <text class="cell-label">消息通知</text>
          <switch :checked="notifyEnabled" color="#6c4ee6" @change="onNotifyChange" />
        </view>
      </view>
    </view>

    <!-- 关于 -->
    <view class="section">
      <view class="section-title">关于</view>
      <view class="cell-group">
        <view class="cell" @click="goAbout">
          <text class="cell-label">关于我们</text>
          <text class="cell-arrow">›</text>
        </view>
        <view class="cell">
          <text class="cell-label">版本号</text>
          <text class="cell-value">v1.0.0</text>
        </view>
      </view>
    </view>

    <!-- 退出登录 -->
    <view class="section" v-if="isLogin">
      <view class="logout-btn" @click="handleLogout">退出登录</view>
    </view>

    <!-- 修改密码弹窗 -->
    <view class="modal-mask" v-if="showPwdModal" @click="showPwdModal = false">
      <view class="modal-content" @click.stop>
        <view class="modal-header">
          <text class="modal-title">修改密码</text>
          <text class="modal-close" @click="showPwdModal = false">×</text>
        </view>
        <view class="modal-body">
          <view class="form-item">
            <text class="form-label">原密码</text>
            <input class="form-input" type="password" password v-model="pwdForm.oldPassword" placeholder="请输入原密码" />
          </view>
          <view class="form-item">
            <text class="form-label">新密码</text>
            <input class="form-input" type="password" password v-model="pwdForm.newPassword" placeholder="请输入新密码" />
          </view>
          <view class="form-item">
            <text class="form-label">确认新密码</text>
            <input class="form-input" type="password" password v-model="pwdForm.confirmPassword" placeholder="请再次输入新密码" />
          </view>
        </view>
        <view class="modal-footer">
          <view class="modal-btn cancel" @click="showPwdModal = false">取消</view>
          <view class="modal-btn confirm" @click="submitChangePwd">确认</view>
        </view>
      </view>
    </view>

    <view style="height: 60rpx;"></view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useUserStore } from '@/store/user'
import { userApi } from '@/api/index'

const userStore = useUserStore()
const isLogin = ref(userStore.isLogin)
const cacheSize = ref('0 KB')
const notifyEnabled = ref(true)
const showPwdModal = ref(false)
const pwdForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

onMounted(() => {
  calcCacheSize()
})

/**
 * 计算缓存大小
 */
function calcCacheSize() {
  try {
    const info = uni.getStorageInfoSync()
    const size = info.currentSize || 0
    if (size < 1024) {
      cacheSize.value = size + ' KB'
    } else {
      cacheSize.value = (size / 1024).toFixed(2) + ' MB'
    }
  } catch (e) {
    cacheSize.value = '0 KB'
  }
}

/**
 * 清除缓存
 */
function clearCache() {
  uni.showModal({
    title: '提示',
    content: '确定要清除本地缓存吗？',
    success: (res) => {
      if (res.confirm) {
        try {
          // 只清数据缓存，保留用户登录态
          const token = uni.getStorageSync('token')
          const userInfo = uni.getStorageSync('userInfo')
          uni.clearStorageSync()
          if (token) uni.setStorageSync('token', token)
          if (userInfo) uni.setStorageSync('userInfo', userInfo)
          cacheSize.value = '0 KB'
          uni.showToast({ title: '清除成功', icon: 'success' })
        } catch (e) {
          uni.showToast({ title: '清除失败', icon: 'none' })
        }
      }
    }
  })
}

/**
 * 消息通知开关
 */
function onNotifyChange(e) {
  notifyEnabled.value = e.detail.value
  uni.showToast({
    title: e.detail.value ? '已开启通知' : '已关闭通知',
    icon: 'none'
  })
}

function goEditProfile() {
  uni.navigateTo({ url: '/pages/mine/profile' })
}

function goChangePassword() {
  pwdForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
  showPwdModal.value = true
}

async function submitChangePwd() {
  const { oldPassword, newPassword, confirmPassword } = pwdForm.value
  if (!oldPassword) {
    uni.showToast({ title: '请输入原密码', icon: 'none' })
    return
  }
  if (!newPassword || newPassword.length < 6) {
    uni.showToast({ title: '新密码至少6位', icon: 'none' })
    return
  }
  if (newPassword !== confirmPassword) {
    uni.showToast({ title: '两次密码不一致', icon: 'none' })
    return
  }
  try {
    await userApi.changePassword({ oldPassword, newPassword })
    uni.showToast({ title: '修改成功', icon: 'success' })
    showPwdModal.value = false
  } catch (err) {
    console.warn('修改密码失败:', err)
  }
}

function goAbout() {
  uni.navigateTo({ url: '/pages/mine/about' })
}

/**
 * 退出登录
 */
function handleLogout() {
  uni.showModal({
    title: '提示',
    content: '确定要退出登录吗？',
    success: async (res) => {
      if (res.confirm) {
        try {
          await userApi.logout()
        } catch (e) {}
        userStore.logout()
        uni.showToast({ title: '已退出登录', icon: 'success' })
        setTimeout(() => {
          uni.navigateBack()
        }, 1500)
      }
    }
  })
}
</script>

<style lang="scss" scoped>
.setting-page {
  min-height: 100vh;
  background: #f5f5f7;
  padding-top: 20rpx;
}

.section {
  margin-bottom: 20rpx;
}

.section-title {
  padding: 16rpx 32rpx;
  font-size: 24rpx;
  color: #999;
}

.cell-group {
  background: #fff;
  margin: 0 24rpx;
  border-radius: 16rpx;
  overflow: hidden;
}

.cell {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28rpx 24rpx;
  border-bottom: 1rpx solid #f5f5f7;

  &:last-child {
    border-bottom: none;
  }
}

.cell-label {
  font-size: 28rpx;
  color: #333;
}

.cell-right {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.cell-value {
  font-size: 26rpx;
  color: #999;
}

.cell-arrow {
  font-size: 32rpx;
  color: #ccc;
}

.logout-btn {
  margin: 40rpx 24rpx 0;
  background: #fff;
  border-radius: 16rpx;
  text-align: center;
  padding: 28rpx;
  color: #ff4d4f;
  font-size: 30rpx;
}

/* 修改密码弹窗 */
.modal-mask {
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
  background: #fff;
  border-radius: 20rpx;
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
}

.modal-body {
  padding: 20rpx 30rpx;
}

.form-item {
  margin-bottom: 24rpx;
}

.form-label {
  display: block;
  font-size: 26rpx;
  color: #333;
  margin-bottom: 12rpx;
}

.form-input {
  width: 100%;
  height: 72rpx;
  padding: 0 20rpx;
  border: 1rpx solid #e8e8e8;
  border-radius: 12rpx;
  font-size: 26rpx;
  background: #fafafa;
  box-sizing: border-box;
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
</style>
