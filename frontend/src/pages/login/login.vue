<template>
  <!-- 登录页 -->
  <view class="login-page">
    <!-- 返回按钮 -->
    <view class="back-btn" @click="goBack">
      <text class="back-icon">‹</text>
    </view>

    <!-- 顶部Logo区域 -->
    <view class="logo-section">
      <view class="logo-icon">🎓</view>
      <text class="app-name">智慧校园</text>
      <text class="app-slogan">让校园生活更便捷</text>
    </view>

    <!-- 登录表单 -->
    <view class="login-form">
      <!-- 登录方式切换 -->
      <view class="login-tabs">
        <view
          class="login-tab"
          :class="{ active: loginType === 'email' }"
          @click="switchLoginType('email')"
        >邮箱登录</view>
        <view
          class="login-tab"
          :class="{ active: loginType === 'phone' }"
          @click="switchLoginType('phone')"
        >手机号登录</view>
      </view>

      <!-- 邮箱登录 -->
      <template v-if="loginType === 'email'">
        <view class="form-group">
          <text class="label">邮箱</text>
          <input
            class="input"
            type="text"
            placeholder="请输入邮箱（如 12345@qq.com）"
            v-model="email"
          />
        </view>
      </template>

      <!-- 手机号登录 -->
      <template v-else>
        <view class="form-group">
          <text class="label">手机号</text>
          <input
            class="input"
            type="number"
            placeholder="请输入手机号"
            v-model="phone"
            maxlength="11"
          />
        </view>
      </template>

      <view class="form-group">
        <text class="label">验证码</text>
        <view class="code-input">
          <input
            class="input code"
            type="number"
            placeholder="请输入验证码"
            v-model="code"
            maxlength="6"
          />
          <view
            class="send-code-btn"
            :class="{ disabled: counting }"
            @click="sendCode"
          >
            {{ counting ? countDown + 's' : '获取验证码' }}
          </view>
        </view>
      </view>

      <!-- 登录按钮 -->
      <view class="login-btn" @click="handleLogin">
        登录 / 注册
      </view>

      <!-- 协议 -->
      <view class="agreement">
        <text class="text">登录即表示同意</text>
        <text class="link">《用户协议》</text>
        <text class="text">和</text>
        <text class="link">《隐私政策》</text>
      </view>
    </view>

    <!-- 其他登录方式 -->
    <view class="other-login">
      <view class="divider">
        <view class="line"></view>
        <text class="divider-text">其他登录方式</text>
        <view class="line"></view>
      </view>
      <view class="wx-login" @click="handleWxLogin">
        <text class="wx-icon">💬</text>
        <text>微信登录</text>
      </view>
    </view>
  </view>
</template>

<script setup>
/**
 * 登录页
 * 支持邮箱验证码登录 / 手机号验证码登录 / 微信登录
 * 邮箱为推荐方式（个人项目短信签名需企业资质，邮箱零门槛）
 */
import { ref } from 'vue'
import { useUserStore } from '@/store/user'
import { userApi } from '@/api/index'

const userStore = useUserStore()

// 登录方式：email=邮箱验证码（默认） phone=手机号验证码
const loginType = ref('email')
const email = ref('')
const phone = ref('')
const code = ref('')
const counting = ref(false)
const countDown = ref(60)
// 登录按钮加载状态
const loginLoading = ref(false)

let timer = null

/**
 * 切换登录方式
 */
function switchLoginType(type) {
  if (loginType.value === type) return
  loginType.value = type
  code.value = '' // 清空验证码，避免两种方式混用
}

/**
 * 校验当前登录方式的账号格式
 */
function validateAccount() {
  if (loginType.value === 'email') {
    if (!/^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/.test(email.value.trim())) {
      uni.showToast({ title: '请输入正确的邮箱', icon: 'none' })
      return false
    }
    return true
  }
  if (!/^1[3-9]\d{9}$/.test(phone.value)) {
    uni.showToast({ title: '请输入正确的手机号', icon: 'none' })
    return false
  }
  return true
}

/**
 * 发送验证码
 * 邮箱走 /sendEmailCode；手机号走旧 /sendCode
 */
async function sendCode() {
  if (counting.value) return
  if (!validateAccount()) return

  try {
    let tip = ''
    if (loginType.value === 'email') {
      const result = await userApi.sendEmailCode(email.value.trim())
      // data: { mock, code? } —— mock=true 开发模式验证码直接返回
      if (result && result.mock && result.code) {
        tip = '开发模式验证码：' + result.code
      } else {
        tip = '验证码已发送到你的邮箱，请注意查收'
      }
    } else {
      const result = await userApi.sendCode(phone.value)
      // 旧短信接口：开发模式下直接返回验证码字符串
      if (result && typeof result === 'string') {
        tip = '测试验证码：' + result
      } else {
        tip = '验证码已发送'
      }
    }

    uni.showModal({ title: '验证码已发送', content: tip, showCancel: false })

    // 开始倒计时
    counting.value = true
    countDown.value = 60
    timer = setInterval(() => {
      countDown.value--
      if (countDown.value <= 0) {
        clearInterval(timer)
        counting.value = false
      }
    }, 1000)
  } catch (err) {
    // 发送失败，request 已统一提示错误
    console.warn('发送验证码失败:', err)
  }
}

/**
 * 登录（邮箱/手机号二选一，按当前 tab 决定）
 * 调用真实登录接口
 */
async function handleLogin() {
  if (!validateAccount()) return

  // 验证验证码
  if (!code.value || code.value.length !== 6) {
    uni.showToast({ title: '请输入6位验证码', icon: 'none' })
    return
  }

  if (loginLoading.value) return
  loginLoading.value = true

  try {
    // 按登录方式组装参数（后端按 email/phone 字段区分）
    const params = loginType.value === 'email'
      ? { email: email.value.trim(), code: code.value }
      : { phone: phone.value, code: code.value }
    // 调用登录接口，返回 { token, userInfo }
    const data = await userApi.login(params)
    // 保存登录状态
    userStore.setLogin(data)
    uni.showToast({ title: '登录成功', icon: 'success' })
    setTimeout(() => {
      // 新用户（尚未选择学校）引导先选学校，保证发布的内容有学校归属
      if (!data.userInfo || !data.userInfo.schoolId) {
        uni.navigateTo({ url: '/pages/school/school' })
        return
      }
      // 如果有来源页面，返回来源页，否则返回首页
      const pages = getCurrentPages()
      if (pages.length > 1) {
        uni.navigateBack()
      } else {
        uni.switchTab({ url: '/pages/index/index' })
      }
    }, 1000)
  } catch (err) {
    // 登录失败，request 已统一处理错误提示
    console.warn('登录失败:', err)
  } finally {
    loginLoading.value = false
  }
}

/**
 * 返回上一页
 */
function goBack() {
  const pages = getCurrentPages()
  if (pages.length > 1) {
    uni.navigateBack()
  } else {
    // 如果是第一页，就返回首页
    uni.switchTab({ url: '/pages/index/index' })
  }
}

/**
 * 微信登录
 */
async function handleWxLogin() {
  // #ifdef MP-WEIXIN
  // 微信小程序环境下，调用微信登录
  uni.login({
    provider: 'weixin',
    success: async (loginRes) => {
      // 获取到 code 后发送给后端
      const wxCode = loginRes.code
      try {
        const data = await userApi.wxLogin({ wxCode })
        userStore.setLogin(data)
        uni.showToast({ title: '登录成功', icon: 'success' })
        setTimeout(() => {
          const pages = getCurrentPages()
          if (pages.length > 1) {
            uni.navigateBack()
          } else {
            uni.switchTab({ url: '/pages/index/index' })
          }
        }, 1000)
      } catch (err) {
        console.warn('微信登录失败:', err)
      }
    }
  })
  // #endif

  // #ifdef H5
  uni.showToast({ title: '请在微信小程序中使用微信登录', icon: 'none' })
  // #endif
}
</script>

<style lang="scss" scoped>
.login-page {
  min-height: 100vh;
  background: #fff;
  padding: 0 48rpx;
  box-sizing: border-box;
  position: relative;
}

/* 返回按钮 */
.back-btn {
  position: absolute;
  top: 80rpx;
  left: 30rpx;
  width: 64rpx;
  height: 64rpx;
  line-height: 56rpx;
  text-align: center;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.05);
  z-index: 10;
}

.back-icon {
  font-size: 48rpx;
  color: #333;
  font-weight: bold;
}

/* Logo区域 */
.logo-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 120rpx;
  padding-bottom: 80rpx;
}

.logo-icon {
  font-size: 120rpx;
  margin-bottom: 24rpx;
}

.app-name {
  font-size: 48rpx;
  font-weight: bold;
  color: #333;
  margin-bottom: 12rpx;
}

.app-slogan {
  font-size: 26rpx;
  color: #999;
}

/* 登录表单 */
.login-form {
  margin-bottom: 80rpx;
}

.form-group {
  margin-bottom: 36rpx;
}

.label {
  font-size: 28rpx;
  color: #333;
  font-weight: 500;
  margin-bottom: 16rpx;
  display: block;
}

.input {
  width: 100%;
  height: 88rpx;
  background: #f5f5f7;
  border-radius: 16rpx;
  padding: 0 24rpx;
  font-size: 30rpx;
  box-sizing: border-box;
}

.code-input {
  display: flex;
  gap: 20rpx;
  align-items: center;
}

.input.code {
  flex: 1;
}

.send-code-btn {
  flex-shrink: 0;
  height: 88rpx;
  padding: 0 28rpx;
  background: #6c4ee6;
  color: #fff;
  font-size: 26rpx;
  border-radius: 16rpx;
  display: flex;
  align-items: center;
  justify-content: center;

  &.disabled {
    background: #ccc;
  }
}

/* 登录按钮 */
.login-btn {
  width: 100%;
  height: 96rpx;
  line-height: 96rpx;
  text-align: center;
  background: linear-gradient(135deg, #6c4ee6, #8b75f0);
  color: #fff;
  font-size: 32rpx;
  font-weight: bold;
  border-radius: 48rpx;
  margin-top: 20rpx;
}

/* 协议 */
.agreement {
  text-align: center;
  margin-top: 24rpx;
  font-size: 24rpx;
  color: #999;
}

.link {
  color: #6c4ee6;
}

/* 其他登录方式 */
.other-login {
  margin-top: 60rpx;
}

.divider {
  display: flex;
  align-items: center;
  gap: 20rpx;
  margin-bottom: 40rpx;
}

.line {
  flex: 1;
  height: 1rpx;
  background: #eee;
}

.divider-text {
  font-size: 24rpx;
  color: #999;
}

.wx-login {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  height: 88rpx;
  border: 2rpx solid #e8e8e8;
  border-radius: 44rpx;
  font-size: 28rpx;
  color: #333;
}

.wx-icon {
  font-size: 36rpx;
}
</style>
