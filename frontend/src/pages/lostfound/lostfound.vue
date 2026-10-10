<template>
  <!-- 失物招领?-->
  <view class="lostfound-page">
    <!-- 顶部搜索?-->
    <view class="search-bar">
      <input
        class="search-input"
        placeholder="搜索物品名称"
        v-model="keyword"
        @confirm="searchItems"
      />
    </view>

    <!-- Tab切换 -->
    <view class="tab-bar">
      <view
        class="tab-item"
        :class="{ active: currentTab === 'all' }"
        @click="switchTab('all')"
      >
        全部
      </view>
      <view
        class="tab-item"
        :class="{ active: currentTab === 'lost' }"
        @click="switchTab('lost')"
      >
        🔍 寻物启事
      </view>
      <view
        class="tab-item"
        :class="{ active: currentTab === 'found' }"
        @click="switchTab('found')"
      >
        📦 失物招领
      </view>
    </view>

    <!-- 列表 -->
    <view class="item-list">
      <view
        class="item-card"
        v-for="item in itemList"
        :key="item.id"
        @click="openDetail(item)"
      >
        <image :src="item.images && item.images[0]" class="item-image" mode="aspectFill"></image>
        <view class="item-info">
          <view class="item-header">
            <text class="item-title">{{ item.itemName }}</text>
            <view class="item-type" :class="item.type === 'lost' ? 'type-lost' : 'type-found'">
              {{ item.type === 'lost' ? '寻物' : '招领' }}
            </view>
          </view>
          <text class="item-desc">{{ item.description }}</text>
          <view class="item-meta">
            <text class="meta-item">📍 {{ item.location }}</text>
            <text class="meta-item">🕐 {{ item.createTime }}</text>
          </view>
          <view class="item-footer">
            <view class="publisher-info">
              <image :src="item.avatar" class="publisher-avatar"></image>
              <text class="publisher-name">{{ item.nickname }}</text>
            </view>
            <view class="status-badge" :class="'status-' + item.status">
              {{ statusText(item.type, item.status) }}
            </view>
          </view>
        </view>
      </view>

      <!-- 空状?-->
      <view class="empty-tip" v-if="!loading && itemList.length === 0">
        <text>暂无数据</text>
      </view>
    </view>

    <!-- 发布按钮 -->
    <view class="publish-btn" @click="openPublishModal">
      <text class="publish-icon">+</text>
      <text>发布</text>
    </view>

    <!-- 发布弹窗 -->
    <view class="publish-modal" v-if="showPublishModal" @click.stop="closePublishModal">
      <view class="modal-content" @click.stop>
        <view class="modal-header">
          <text class="modal-title">发布失物招领</text>
          <text class="modal-close" @click="closePublishModal">×</text>
        </view>
        <scroll-view scroll-y class="modal-body">
          <!-- 类型选择 -->
          <view class="form-item">
            <text class="form-label">类型 <text class="required">*</text></text>
            <view class="type-selector">
              <view
                class="type-option"
                :class="{ active: publishForm.type === 'lost' }"
                @click="publishForm.type = 'lost'"
              >
                🔍 寻物启事
              </view>
              <view
                class="type-option"
                :class="{ active: publishForm.type === 'found' }"
                @click="publishForm.type = 'found'"
              >
                📦 失物招领
              </view>
            </view>
          </view>
          <!-- 物品名称 -->
          <view class="form-item">
            <text class="form-label">物品名称 <text class="required">*</text></text>
            <input class="form-input" v-model="publishForm.itemName" placeholder="请输入物品名？" />
          </view>
          <!-- 地点 -->
          <view class="form-item">
            <text class="form-label">{{ publishForm.type === 'lost' ? '丢失' : '拾取' }}地点 <text class="required">*</text></text>
            <input class="form-input" v-model="publishForm.location" placeholder="请输入地？" />
          </view>
          <!-- 详细描述 -->
          <view class="form-item">
            <text class="form-label">详细描述 <text class="required">*</text></text>
            <textarea
              class="form-textarea"
              v-model="publishForm.description"
              placeholder="请详细描述物品特征、相关信息等"
            />
          </view>
        </scroll-view>
        <view class="modal-footer">
          <view class="modal-btn cancel" @click="closePublishModal">取消</view>
          <view class="modal-btn confirm" @click="submitPublish">发布</view>
        </view>
      </view>
    </view>

    <!-- 详情弹窗 -->
    <view class="detail-modal" v-if="showDetailModal" @click.stop="closeDetailModal">
      <view class="modal-content detail-content" @click.stop>
        <view class="modal-header">
          <text class="modal-title">{{ detailItem.itemName }}</text>
          <text class="modal-close" @click="closeDetailModal">×</text>
        </view>
        <scroll-view scroll-y class="modal-body detail-body">
          <!-- 图片 -->
          <view class="detail-images" v-if="detailItem.images && detailItem.images.length">
            <image
              v-for="(img, idx) in detailItem.images"
              :key="idx"
              :src="img"
              class="detail-image"
              mode="aspectFill"
            ></image>
          </view>
          <!-- 类型+状态标?-->
          <view class="detail-tags">
            <view class="detail-type-tag" :class="detailItem.type === 'lost' ? 'type-lost' : 'type-found'">
              {{ detailItem.type === 'lost' ? '寻物启事' : '失物招领' }}
            </view>
            <view class="detail-status-tag" :class="'status-' + detailItem.status">
              {{ statusText(detailItem.type, detailItem.status) }}
            </view>
          </view>
          <!-- 信息列表 -->
          <view class="detail-info">
            <view class="detail-row">
              <text class="detail-label">📍 地点</text>
              <text class="detail-value">{{ detailItem.location }}</text>
            </view>
            <view class="detail-row">
              <text class="detail-label">🕐 发布时间</text>
              <text class="detail-value">{{ detailItem.createTime }}</text>
            </view>
            <view class="detail-row">
              <text class="detail-label">👤 发布：</text>
              <view class="detail-publisher">
                <image :src="detailItem.avatar" class="detail-avatar"></image>
                <text class="detail-nickname">{{ detailItem.nickname }}</text>
              </view>
            </view>
          </view>
          <!-- 详细描述 -->
          <view class="detail-desc">
            <text class="detail-desc-title">详细描述</text>
            <text class="detail-desc-content">{{ detailItem.description }}</text>
          </view>
        </scroll-view>
        <view class="modal-footer">
          <view class="modal-btn cancel" @click="closeDetailModal">关闭</view>
          <view class="modal-btn confirm" @click="contactPublisher(detailItem)">联系TA</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
/**
 * 失物招领? * 后端实体字段：id, schoolId, userId, type, itemName, description, images(逗号分隔), location, status, auditStatus
 * 后端列表返回 Page 对象，列表在 records 字段
 */
import { ref, onMounted, onUnmounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { scrollToTop } from '@/utils/scroll-top'
import { lostFoundApi } from '@/api/index'
import { checkLogin } from '@/utils/request'
import { guardWrite, onSchoolChanged } from '@/utils/school-permission'

// 默认头像
const DEFAULT_AVATAR = 'https://thirdwx.qlogo.cn/mmopen/vi_32/POgEwh4mIHO4nibH0KlMECNjjGxQUq24ZEaGT4poC6icRiccVGKSyXwibcPq4BWmiaIGuG1icwxaQX6grC9VemZoJ8rg/132'

const keyword = ref('')
const currentTab = ref('all') // all 全部, lost 寻物启事, found 失物招领
const itemList = ref([])
const loading = ref(false)

// 发布弹窗
const showPublishModal = ref(false)
// type 使用 'lost'/'found' 仅用于界面切换；提交时会转换为后?TINYINT?:寻物 1:招领?
const publishForm = ref({
  type: 'lost',
  itemName: '',
  location: '',
  description: ''
})

// 详情弹窗
const showDetailModal = ref(false)
const detailItem = ref({})

onMounted(() => {
  loadList()
})
// ---- 切换学校后刷新 ----
// 用户切换学校返回本页时，重新拉取当前学校的数据
// hasLoadedOnce 避免与 onMounted 首屏加载重复请求
let hasLoadedOnce = false
onShow(() => {
  scrollToTop()
  onSchoolChanged()
  if (hasLoadedOnce) { loadList() }
  hasLoadedOnce = true
})

/**
 * 加载失物招领列表
 */
async function loadList() {
  loading.value = true
  try {
    const params = { pageNum: 1, pageSize: 10 }
    if (currentTab.value !== 'all') {
      params.type = currentTab.value
    }
    if (keyword.value) {
      params.keyword = keyword.value
    }

    const data = await lostFoundApi.getLostFoundList(params)
    const records = data.records || data.list || data || []
    itemList.value = records.map(item => enrichItem(item))
  } catch (err) {
    console.warn('失物招领列表接口请求失败:', err)
    itemList.value = []
  } finally {
    loading.value = false
  }
}

/**
 * 补充/转换列表项字段
 * - images 字符串转数组
 */
function enrichItem(item) {
  const enriched = { ...item }
  // images 字段：字符串分割成数组
  if (enriched.images && typeof enriched.images === 'string') {
    enriched.images = enriched.images.split(',').filter(url => url.trim())
  } else if (!enriched.images) {
    enriched.images = []
  }
  // 补充默认昵称
  if (!enriched.nickname) {
    enriched.nickname = '校园用户'
  }
  // 补充默认头像
  if (!enriched.avatar) {
    enriched.avatar = DEFAULT_AVATAR
  }
  // 确保 status 有值（默认 0）
  if (enriched.status === undefined || enriched.status === null) {
    enriched.status = 0
  }
  return enriched
}

/**
 * 状态文本
 * 根据 type 和 status 返回不同的显示文案
 */
function statusText(type, status) {
  if (type === 'lost') {
    return status === 1 ? '已找到' : '寻找中'
  } else {
    return status === 1 ? '已认领' : '待认领'
  }
}

/**
 * 切换Tab
 */
function switchTab(tab) {
  currentTab.value = tab
  loadList()
}

/**
 * 搜索
 */
function searchItems() {
  loadList()
}

/**
 * 打开发布弹窗
 */
function openPublishModal() {
  if (!checkLogin()) return
  // 重置表单
  publishForm.value = {
    type: 'lost',
    itemName: '',
    location: '',
    description: ''
  }
  showPublishModal.value = true
}

/**
 * 关闭发布弹窗
 */
function closePublishModal() {
  showPublishModal.value = false
}

/**
 * 提交发布
 */
async function submitPublish() {
  if (guardWrite()) return
  const form = publishForm.value
  // 必填项校验
  if (!form.itemName.trim()) {
    uni.showToast({ title: '请填写物品名称', icon: 'none' })
    return
  }
  if (!form.location.trim()) {
    uni.showToast({ title: '请填写地点', icon: 'none' })
    return
  }
  if (!form.description.trim()) {
    uni.showToast({ title: '请填写详细描述', icon: 'none' })
    return
  }

  try {
    const submitData = {
      // 后端 lost_found.type ?TINYINT?:寻物 1:招领），必须转成数字
      type: form.type === 'found' ? 1 : 0,
      itemName: form.itemName.trim(),
      location: form.location.trim(),
      description: form.description.trim(),
      images: ''
    }
    await lostFoundApi.publishLostFound(submitData)
    uni.showToast({ title: '发布成功', icon: 'success' })
    showPublishModal.value = false
    // 刷新列表
    loadList()
  } catch (err) {
    console.warn('发布失败:', err)
  }
}

/**
 * 打开详情弹窗
 */
function openDetail(item) {
  detailItem.value = item
  showDetailModal.value = true
}

/**
 * 关闭详情弹窗
 */
function closeDetailModal() {
  showDetailModal.value = false
}

/**
 * 联系发布? */
function contactPublisher(item) {
  if (!checkLogin()) return
  // 自己发布的物品不需要联系
  const userInfo = uni.getStorageSync('userInfo')
  if (userInfo && userInfo.id && userInfo.id === item.userId) {
    uni.showToast({ title: '这是你发布的', icon: 'none' })
    return
  }
  const hasPhone = item.contact && !String(item.contact).includes('*')
  const opts = hasPhone ? ['发站内私信', '拨打电话'] : ['发站内私信']
  uni.showActionSheet({
    itemList: opts,
    success: (r) => {
      if (r.tapIndex === 0) {
        // 站内私信：跳聊天页并预填消息
        const nickname = encodeURIComponent(item.publisher || '失物发布者')
        const prefill = encodeURIComponent('你好，关于「' + (item.title || item.itemName || '') + '」')
        uni.navigateTo({
          url: '/pages/message/chat?targetUserId=' + item.userId
            + '&targetNickname=' + nickname
            + '&prefill=' + prefill
        })
      } else if (r.tapIndex === 1 && hasPhone) {
        uni.makePhoneCall({ phoneNumber: item.contact })
      }
    }
  })
}
</script></script>

<style lang="scss" scoped>
.lostfound-page {
  min-height: 100vh;
  background: #f5f5f7;
  padding-bottom: 40rpx;
}

.search-bar {
  padding: 20rpx 24rpx;
  background: #fff;
}

.search-input {
  width: 100%;
  height: 72rpx;
  background: #f5f5f7;
  border-radius: 36rpx;
  padding: 0 30rpx;
  font-size: 28rpx;
  box-sizing: border-box;
}

/* Tab切换 */
.tab-bar {
  display: flex;
  background: #fff;
  border-bottom: 1rpx solid #eee;
}

.tab-item {
  flex: 1;
  text-align: center;
  padding: 28rpx 0;
  font-size: 28rpx;
  color: #666;
  position: relative;

  &.active {
    color: #6c4ee6;
    font-weight: bold;

    &::after {
      content: '';
      position: absolute;
      bottom: 0;
      left: 50%;
      transform: translateX(-50%);
      width: 50rpx;
      height: 6rpx;
      background: #6c4ee6;
      border-radius: 3rpx;
    }
  }
}

/* 列表 */
.item-list {
  padding: 20rpx 24rpx;
}

.item-card {
  display: flex;
  background: #fff;
  border-radius: 20rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
}

.item-image {
  width: 180rpx;
  height: 180rpx;
  border-radius: 12rpx;
  flex-shrink: 0;
  background: #f0f0f0;
}

.item-info {
  flex: 1;
  margin-left: 20rpx;
  display: flex;
  flex-direction: column;
}

.item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12rpx;
}

.item-title {
  font-size: 30rpx;
  font-weight: bold;
  color: #333;
}

.item-type {
  padding: 6rpx 16rpx;
  border-radius: 20rpx;
  font-size: 22rpx;

  &.type-lost {
    background: #fff1f0;
    color: #ff4d4f;
  }
  &.type-found {
    background: #f6ffed;
    color: #52c41a;
  }
}

.item-desc {
  font-size: 26rpx;
  color: #666;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
  margin-bottom: 12rpx;
}

.item-meta {
  display: flex;
  gap: 20rpx;
  margin-bottom: 12rpx;
}

.meta-item {
  font-size: 24rpx;
  color: #999;
}

.item-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: auto;
}

.publisher-info {
  display: flex;
  align-items: center;
  gap: 10rpx;
}

.publisher-avatar {
  width: 36rpx;
  height: 36rpx;
  border-radius: 50%;
  background: #eee;
}

.publisher-name {
  font-size: 24rpx;
  color: #999;
}

.status-badge {
  padding: 6rpx 16rpx;
  font-size: 22rpx;
  border-radius: 20rpx;

  &.status-0 {
    background: #fff7e6;
    color: #fa8c16;
  }
  &.status-1 {
    background: #f6ffed;
    color: #52c41a;
  }
}

/* 空状?*/
.empty-tip {
  text-align: center;
  padding: 100rpx 0;
  color: #999;
  font-size: 28rpx;
}

/* 发布按钮 */
.publish-btn {
  position: fixed;
  right: 40rpx;
  bottom: 120rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 110rpx;
  height: 110rpx;
  background: linear-gradient(135deg, #6c4ee6, #8b5cf6);
  color: #fff;
  border-radius: 50%;
  box-shadow: 0 8rpx 24rpx rgba(108, 78, 230, 0.4);
  font-size: 20rpx;
  z-index: 100;
}

.publish-icon {
  font-size: 40rpx;
  line-height: 1;
}

/* ===== 弹窗通用样式 ===== */
.publish-modal,
.detail-modal {
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
  max-height: 80vh;
  background: #fff;
  border-radius: 20rpx;
  display: flex;
  flex-direction: column;
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
  padding: 0 10rpx;
}

.modal-body {
  flex: 1;
  padding: 20rpx 30rpx;
  max-height: 60vh;
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

/* ===== 发布弹窗表单 ===== */
.form-item {
  margin-bottom: 28rpx;
}

.form-label {
  display: block;
  font-size: 26rpx;
  color: #333;
  margin-bottom: 12rpx;

  .required {
    color: #ff4d4f;
  }
}

.form-input {
  width: 100%;
  height: 72rpx;
  padding: 0 20rpx;
  border: 1rpx solid #e8e8e8;
  border-radius: 12rpx;
  font-size: 26rpx;
  color: #333;
  background: #fafafa;
  box-sizing: border-box;
}

.form-textarea {
  width: 100%;
  height: 180rpx;
  padding: 16rpx 20rpx;
  border: 1rpx solid #e8e8e8;
  border-radius: 12rpx;
  font-size: 26rpx;
  color: #333;
  background: #fafafa;
  box-sizing: border-box;
}

/* 类型选择?*/
.type-selector {
  display: flex;
  gap: 20rpx;
}

.type-option {
  flex: 1;
  text-align: center;
  padding: 20rpx 0;
  border: 2rpx solid #e8e8e8;
  border-radius: 12rpx;
  font-size: 26rpx;
  color: #666;
  background: #fafafa;
  transition: all 0.2s;

  &.active {
    border-color: #6c4ee6;
    background: #f3f0ff;
    color: #6c4ee6;
    font-weight: bold;
  }
}

/* ===== 详情弹窗 ===== */
.detail-content {
  width: 88%;
}

.detail-body {
  padding: 24rpx 30rpx;
}

.detail-images {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  margin-bottom: 24rpx;
}

.detail-image {
  width: 200rpx;
  height: 200rpx;
  border-radius: 12rpx;
  background: #f0f0f0;
}

.detail-tags {
  display: flex;
  gap: 16rpx;
  margin-bottom: 24rpx;
}

.detail-type-tag {
  padding: 8rpx 20rpx;
  border-radius: 24rpx;
  font-size: 24rpx;

  &.type-lost {
    background: #fff1f0;
    color: #ff4d4f;
  }
  &.type-found {
    background: #f6ffed;
    color: #52c41a;
  }
}

.detail-status-tag {
  padding: 8rpx 20rpx;
  border-radius: 24rpx;
  font-size: 24rpx;

  &.status-0 {
    background: #fff7e6;
    color: #fa8c16;
  }
  &.status-1 {
    background: #f6ffed;
    color: #52c41a;
  }
}

.detail-info {
  background: #f9f9f9;
  border-radius: 12rpx;
  padding: 20rpx;
  margin-bottom: 24rpx;
}

.detail-row {
  display: flex;
  align-items: center;
  margin-bottom: 16rpx;
  font-size: 26rpx;

  &:last-child {
    margin-bottom: 0;
  }
}

.detail-label {
  color: #999;
  flex-shrink: 0;
  width: 160rpx;
}

.detail-value {
  color: #333;
  flex: 1;
}

.detail-publisher {
  display: flex;
  align-items: center;
  gap: 12rpx;
  flex: 1;
}

.detail-avatar {
  width: 44rpx;
  height: 44rpx;
  border-radius: 50%;
  background: #eee;
}

.detail-nickname {
  color: #333;
  font-size: 26rpx;
}

.detail-desc {
  background: #f9f9f9;
  border-radius: 12rpx;
  padding: 20rpx;
}

.detail-desc-title {
  display: block;
  font-size: 26rpx;
  font-weight: bold;
  color: #333;
  margin-bottom: 12rpx;
}

.detail-desc-content {
  font-size: 26rpx;
  color: #666;
  line-height: 1.6;
}
</style>
