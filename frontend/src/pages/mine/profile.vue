<template>
  <view class="profile-page">
    <!-- 头像区域 -->
    <view class="avatar-section" @click="changeAvatar">
      <image :src="formData.avatar || defaultAvatar" class="avatar"></image>
      <text class="change-text">点击更换头像</text>
    </view>

    <!-- 表单区域 -->
    <view class="form-section">
      <view class="form-item">
        <text class="form-label">昵称</text>
        <input
          class="form-input"
          v-model="formData.nickname"
          placeholder="请输入昵称"
          maxlength="20"
        />
      </view>
      <view class="form-item">
        <text class="form-label">手机号</text>
        <input
          class="form-input"
          v-model="formData.phone"
          placeholder="请输入手机号"
          type="number"
          maxlength="11"
        />
      </view>
      <view class="form-item">
        <text class="form-label">性别</text>
        <view class="gender-group">
          <view
            class="gender-item"
            :class="{ active: formData.gender === 1 }"
            @click="formData.gender = 1"
          >男</view>
          <view
            class="gender-item"
            :class="{ active: formData.gender === 2 }"
            @click="formData.gender = 2"
          >女</view>
          <view
            class="gender-item"
            :class="{ active: formData.gender === 0 }"
            @click="formData.gender = 0"
          >保密</view>
        </view>
      </view>
      <view class="form-item">
        <text class="form-label">个性签名</text>
        <textarea
          class="form-textarea"
          v-model="formData.bio"
          placeholder="介绍一下自己吧~"
          maxlength="100"
        ></textarea>
      </view>
    </view>

    <!-- 保存按钮 -->
    <view class="save-section">
      <button class="save-btn" :disabled="saving" @click="handleSave">
        {{ saving ? '保存中...' : '保存' }}
      </button>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useUserStore } from '@/store/user'
import { userApi } from '@/api/index'

const userStore = useUserStore()
const defaultAvatar = 'https://thirdwx.qlogo.cn/mmopen/vi_32/POgEwh4mIHO4nibH0KlMECNjjGxQUq24ZEaGT4poC6icRiccVGKSyXwibcPq4BWmiaIGuG1icwxaQX6grC9VemZoJ8rg/132'

const formData = ref({
  nickname: '',
  phone: '',
  avatar: '',
  gender: 0,
  bio: ''
})
const saving = ref(false)

onMounted(() => {
  // 从 store 加载用户信息
  const userInfo = userStore.userInfo
  if (userInfo) {
    formData.value = {
      nickname: userInfo.nickname || '',
      phone: userInfo.phone || '',
      avatar: userInfo.avatar || defaultAvatar,
      gender: userInfo.gender || 0,
      bio: userInfo.bio || ''
    }
  }
})

/**
 * 更换头像
 */
function changeAvatar() {
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: (res) => {
      formData.value.avatar = res.tempFilePaths[0]
    }
  })
}

/**
 * 保存
 */
async function handleSave() {
  if (!formData.value.nickname.trim()) {
    uni.showToast({ title: '请输入昵称', icon: 'none' })
    return
  }

  saving.value = true
  try {
    // 这里简化处理：只更新昵称、手机、头像、签名
    const updateData = {
      nickname: formData.value.nickname,
      phone: formData.value.phone,
      avatar: formData.value.avatar
    }
    const result = await userApi.updateUserInfo(updateData)
    // 更新 store 里的用户信息
    userStore.setUserInfo(result)
    uni.showToast({ title: '保存成功', icon: 'success' })
    setTimeout(() => {
      uni.navigateBack()
    }, 1000)
  } catch (err) {
    uni.showToast({ title: err.msg || '保存失败', icon: 'none' })
  } finally {
    saving.value = false
  }
}
</script>

<style lang="scss" scoped>
.profile-page {
  min-height: 100vh;
  background: #f5f5f7;
  padding-bottom: 40rpx;
}

/* 头像区域 */
.avatar-section {
  background: #fff;
  padding: 60rpx 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16rpx;
  margin-bottom: 20rpx;
}

.avatar {
  width: 160rpx;
  height: 160rpx;
  border-radius: 50%;
  border: 4rpx solid #fff;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.1);
}

.change-text {
  font-size: 24rpx;
  color: #6c4ee6;
}

/* 表单区域 */
.form-section {
  background: #fff;
  margin-bottom: 20rpx;
}

.form-item {
  padding: 24rpx;
  border-bottom: 1rpx solid #f5f5f7;

  &:last-child {
    border-bottom: none;
  }
}

.form-label {
  display: block;
  font-size: 26rpx;
  color: #999;
  margin-bottom: 12rpx;
}

.form-input {
  width: 100%;
  height: 60rpx;
  font-size: 30rpx;
  color: #333;
}

.form-textarea {
  width: 100%;
  height: 120rpx;
  font-size: 30rpx;
  color: #333;
  line-height: 1.5;
}

.gender-group {
  display: flex;
  gap: 20rpx;
}

.gender-item {
  flex: 1;
  height: 72rpx;
  line-height: 72rpx;
  text-align: center;
  background: #f5f5f7;
  border-radius: 12rpx;
  font-size: 28rpx;
  color: #666;

  &.active {
    background: rgba(108, 78, 230, 0.1);
    color: #6c4ee6;
    font-weight: bold;
  }
}

/* 保存按钮 */
.save-section {
  padding: 40rpx 24rpx;
}

.save-btn {
  width: 100%;
  height: 88rpx;
  line-height: 88rpx;
  background: linear-gradient(135deg, #6c4ee6, #8b5cf6);
  color: #fff;
  font-size: 30rpx;
  font-weight: bold;
  border-radius: 44rpx;
}
</style>
