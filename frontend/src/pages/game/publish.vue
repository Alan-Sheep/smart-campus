<template>
  <!-- 发布游戏搭子页 -->
  <view class="publish-page">
    <!-- 表单 -->
    <view class="form-section">
      <view class="form-group">
        <text class="form-label">游戏名称</text>
        <picker :range="gameTypeLabels" @change="onGameTypeChange">
          <view class="picker-input">
            <text :class="{ placeholder: !formData.gameName }">
              {{ formData.gameName || '请选择游戏名称' }}
            </text>
            <text class="arrow">›</text>
          </view>
        </picker>
        <view class="custom-input-wrap">
          <text class="custom-tip">没找到？手动输入：</text>
          <input 
            class="form-input custom-input" 
            placeholder="手动输入游戏名称" 
            v-model="customGameName"
            @blur="onCustomGameInput"
          />
        </view>
      </view>

      <view class="form-group">
        <text class="form-label">游戏段位/等级</text>
        <input 
          class="form-input" 
          placeholder="请输入游戏段位或等级" 
          v-model="formData.gameLevel"
        />
      </view>

      <view class="form-group">
        <text class="form-label">招募描述</text>
        <textarea 
          class="form-textarea" 
          placeholder="请描述你的招募需求，如时间段、人数要求等" 
          v-model="formData.description"
          maxlength="200"
        ></textarea>
        <text class="char-count">{{ formData.description.length }}/200</text>
      </view>

      <view class="form-group">
        <text class="form-label">游戏模式/类型</text>
        <picker :range="modeList" range-key="label" @change="onModeChange">
          <view class="picker-input">
            <text :class="{ placeholder: !formData.playMode }">
              {{ formData.playMode ? currentModeLabel : '请选择游戏模式' }}
            </text>
            <text class="arrow">›</text>
          </view>
        </picker>
      </view>

      <view class="form-group">
        <text class="form-label">联系方式</text>
        <input 
          class="form-input" 
          placeholder="请输入手机号/微信号/游戏ID" 
          v-model="formData.contactWay"
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
 * 发布游戏搭子页
 */
import { reactive, computed, onMounted, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { gameApi } from '@/api/index'
import { checkLogin } from '@/utils/request'
import { guardWrite } from '@/utils/school-permission'

const modeList = [
  { value: 'rank', label: '排位' },
  { value: 'match', label: '匹配' },
  { value: 'casual', label: '娱乐' },
  { value: 'other', label: '其他' }
]

const formData = reactive({
  gameName: '',
  gameLevel: '',
  description: '',
  playMode: '',
  contactWay: ''
})

const customGameName = ref('')

const gameTypeList = reactive([])

const gameTypeLabels = computed(() => gameTypeList.map(g => g.name || g.label || g.gameName || ''))

const currentModeLabel = computed(() => {
  const item = modeList.find(m => m.value === formData.playMode)
  return item ? item.label : ''
})

onMounted(() => {
  loadGameTypes()
})

// 切换学校后重新加载本校的游戏分类
onShow(() => {
  loadGameTypes()
})

/**
 * 加载游戏类型
 *
 * 注意：后端 /api/game/gameTypes 返回的是**字符串数组**（游戏名去重），
 * 而模板与选择逻辑原本按对象属性读取（g.name/label/gameName），
 * 导致选择器显示空白、formData.gameName 被置空而无法提交。
 * 这里统一归一化成 { id, name } 结构。
 */
function loadGameTypes() {
  gameApi.getGameTypes().then(res => {
    const raw = res && res.records ? res.records : (res && res.list ? res.list : (res || []))
    const list = (Array.isArray(raw) ? raw : []).map((item, index) => {
      // 兼容后端返回字符串或对象两种形态
      if (typeof item === 'string') {
        return { id: String(index + 1), name: item }
      }
      return {
        id: String(item.id || index + 1),
        name: item.name || item.label || item.gameName || ''
      }
    }).filter(g => g.name)
    if (list.length > 0) {
      gameTypeList.push(...list)
    } else {
      pushDefaultGameTypes()
    }
  }).catch(() => {
    // 接口失败时使用默认游戏类型
    pushDefaultGameTypes()
  })
}

/**
 * 默认游戏类型（接口不可用时兜底）
 */
function pushDefaultGameTypes() {
  gameTypeList.push(
    { id: '1', name: '王者荣耀' },
    { id: '2', name: '和平精英' },
    { id: '3', name: '英雄联盟' },
    { id: '4', name: '原神' },
    { id: '5', name: 'CSGO' },
    { id: '6', name: '永劫无间' },
    { id: '7', name: '其他' }
  )
}

/**
 * 选择游戏类型
 */
function onGameTypeChange(e) {
  const idx = e.detail.value
  if (gameTypeList[idx]) {
    const item = gameTypeList[idx]
    formData.gameName = item.name || item.label || item.gameName || ''
    customGameName.value = ''
  }
}

/**
 * 手动输入游戏名称
 */
function onCustomGameInput() {
  if (customGameName.value.trim()) {
    formData.gameName = customGameName.value.trim()
  }
}

/**
 * 选择游戏模式
 */
function onModeChange(e) {
  const idx = e.detail.value
  formData.playMode = modeList[idx].value
}

/**
 * 提交发布
 */
function submitPublish() {
  if (guardWrite()) return
  if (!checkLogin()) return
  if (!formData.gameName) {
    uni.showToast({ title: '请选择或输入游戏名称', icon: 'none' })
    return
  }
  if (!formData.gameLevel) {
    uni.showToast({ title: '请输入游戏段位/等级', icon: 'none' })
    return
  }
  if (!formData.description) {
    uni.showToast({ title: '请输入招募描述', icon: 'none' })
    return
  }
  if (!formData.playMode) {
    uni.showToast({ title: '请选择游戏模式', icon: 'none' })
    return
  }
  if (!formData.contactWay) {
    uni.showToast({ title: '请输入联系方式', icon: 'none' })
    return
  }

  const submitData = {
    gameName: formData.gameName,
    gameLevel: formData.gameLevel,
    description: formData.description,
    playMode: formData.playMode,
    contactWay: formData.contactWay
  }

  gameApi.publishGame(submitData).then(() => {
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

/* 自定义输入 */
.custom-input-wrap {
  margin-top: 16rpx;
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.custom-tip {
  font-size: 24rpx;
  color: #999;
  white-space: nowrap;
}

.custom-input {
  flex: 1;
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
