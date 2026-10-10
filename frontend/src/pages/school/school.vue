<template>
  <!-- 学校选择页 -->
  <view class="school-page">
    <!-- 搜索框 -->
    <view class="search-bar">
      <input 
        class="search-input" 
        placeholder="搜索学校名称" 
        v-model="keyword"
        @input="handleSearch"
      />
    </view>

    <!-- 当前学校 -->
    <view class="current-school" v-if="currentSchool">
      <text class="label">当前学校</text>
      <view class="school-card current">
        <text class="school-icon">🏫</text>
        <text class="school-name">{{ currentSchool.name }}</text>
        <text class="current-tag">当前</text>
      </view>
    </view>

    <!-- 学校列表 -->
    <view class="school-list">
      <view 
        class="school-card" 
        v-for="item in filteredSchools" 
        :key="item.id"
        @click="selectSchool(item)"
      >
        <text class="school-icon">🏫</text>
        <text class="school-name">{{ item.name }}</text>
        <text v-if="currentSchool.id === item.id" class="check-icon">✓</text>
      </view>
    </view>

    <view class="empty-tip" v-if="!filteredSchools.length">
      <text>没有找到相关学校</text>
    </view>
  </view>
</template>

<script setup>
/**
 * 学校选择页
 * 用户可以在这里切换学校
 */
import { ref, computed, onMounted } from 'vue'
import { useSchoolStore } from '@/store/school'
import { schoolApi, userApi } from '@/api/index'
import { checkLogin } from '@/utils/request'

const schoolStore = useSchoolStore()
const currentSchool = computed(() => schoolStore.currentSchool)

const keyword = ref('')
const schoolList = ref([])
// 加载状态
const loading = ref(false)

// 过滤后的学校列表
const filteredSchools = computed(() => {
  if (!keyword.value) return schoolList.value
  return schoolList.value.filter(item =>
    item.name.includes(keyword.value)
  )
})

onMounted(() => {
  loadSchoolList()
})

/**
 * 加载学校列表
 */
async function loadSchoolList() {
  loading.value = true
  try {
    const data = await schoolApi.getSchoolList()
    const list = Array.isArray(data) ? data : (data.list || [])
    schoolList.value = list
    schoolStore.setSchoolList(list)
  } catch (err) {
    console.warn('学校列表接口请求失败:', err)
    schoolList.value = []
  } finally {
    loading.value = false
  }
}

/**
 * 搜索学校
 */
function handleSearch() {
  // 实时过滤，不需要额外处理
  // 如果数据量大，可以调用后端搜索接口
  // schoolApi.searchSchool(keyword.value).then(res => { ... })
}

/**
 * 选择学校
 * 调用真实接口切换学校
 *
 * 若已登录且用户的 school_id 尚未绑定（新注册用户），
 * 同时调用 updateUserInfo 把学校归属写回用户档案。
 */
async function selectSchool(school) {
  if (school.id === currentSchool.value.id) {
    uni.showToast({ title: '当前已是该学校', icon: 'none' })
    return
  }

  uni.showModal({
    title: '切换学校',
    content: '确定要切换到「' + school.name + '」吗？',
    success: async (res) => {
      if (res.confirm) {
        // 判断进入本页的来源：登录引导（无上一页）还是普通切换（有上一页）
        const pages = getCurrentPages()
        const fromLoginGuide = pages.length <= 1

        try {
          // 调用切换学校接口
          await schoolApi.switchSchool(school.id)
          // 切换成功后更新本地状态
          schoolStore.switchSchool(school)
          // 已登录用户：把学校归属绑定到账号（首次选择学校的用户会走到这里）
          await bindSchoolToUser(school.id)
          goAfterSelect(fromLoginGuide)
        } catch (err) {
          // 接口失败，降级为只切换本地状态
          console.warn('切换学校接口失败，仅切换本地:', err)
          schoolStore.switchSchool(school)
          goAfterSelect(fromLoginGuide)
        }
      }
    }
  })
}

/**
 * 选完学校后的跳转
 * 登录引导进入（页面栈只有本页）→ 不能 navigateBack（会没地方去），
 * 直接 switchTab 回主页——修复「登录后选完学校停在选校页」的问题
 * 普通切换 → 返回上一页
 */
function goAfterSelect(fromLoginGuide) {
  setTimeout(() => {
    if (fromLoginGuide) {
      uni.switchTab({ url: '/pages/index/index' })
    } else {
      uni.navigateBack()
    }
  }, 500)
}

/**
 * 把所选学校绑定到当前登录用户
 * 新用户首次登录没有学校归属（school_id 为 NULL），选择学校后回填
 */
async function bindSchoolToUser(schoolId) {
  try {
    if (!checkLogin(false)) return
    const info = await userApi.getUserInfo()
    // 只有未绑定的才回写，避免切学校时反复覆盖用户已绑定的学校
    if (info && !info.schoolId) {
      await userApi.updateUserInfo({ schoolId })
    }
  } catch (err) {
    console.warn('绑定学校到用户失败:', err)
  }
}
</script>

<style lang="scss" scoped>
.school-page {
  min-height: 100vh;
  background: #f5f5f7;
  padding: 20rpx 24rpx;
  box-sizing: border-box;
}

/* 搜索框 */
.search-bar {
  margin-bottom: 30rpx;
}

.search-input {
  width: 100%;
  height: 72rpx;
  background: #fff;
  border-radius: 36rpx;
  padding: 0 30rpx;
  font-size: 28rpx;
  box-sizing: border-box;
}

/* 当前学校 */
.current-school {
  margin-bottom: 30rpx;
}

.label {
  font-size: 26rpx;
  color: #999;
  margin-bottom: 16rpx;
  display: block;
}

/* 学校列表 */
.school-list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.school-card {
  display: flex;
  align-items: center;
  padding: 28rpx 24rpx;
  background: #fff;
  border-radius: 16rpx;
  gap: 20rpx;

  &.current {
    background: linear-gradient(135deg, #6c4ee620, #8b75f020);
    border: 2rpx solid #6c4ee6;
  }
}

.school-icon {
  font-size: 40rpx;
}

.school-name {
  flex: 1;
  font-size: 30rpx;
  color: #333;
}

.current-tag {
  padding: 6rpx 16rpx;
  background: #6c4ee6;
  color: #fff;
  font-size: 22rpx;
  border-radius: 20rpx;
}

.check-icon {
  font-size: 32rpx;
  color: #6c4ee6;
}

.empty-tip {
  text-align: center;
  padding: 100rpx 0;
  color: #999;
  font-size: 28rpx;
}
</style>
