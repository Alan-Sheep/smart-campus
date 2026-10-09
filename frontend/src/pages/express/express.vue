<template>
  <!-- 快递代取页 -->
  <view class="express-page">
    <!-- 顶部Tab切换 -->
    <view class="tab-bar">
      <view
        class="tab-item"
        :class="{ active: currentTab === 'hall' }"
        @click="switchTab('hall')"
      >
        接单大厅
      </view>
      <view
        class="tab-item"
        :class="{ active: currentTab === 'my' }"
        @click="switchTab('my')"
      >
        我的订单
      </view>
    </view>

    <!-- 「我的订单」子筛选：我发布的 / 我接单的 -->
    <view class="role-bar" v-if="currentTab === 'my'">
      <view
        class="role-item"
        :class="{ active: currentRole === 'publish' }"
        @click="switchRole('publish')"
      >我发布的</view>
      <view
        class="role-item"
        :class="{ active: currentRole === 'pick' }"
        @click="switchRole('pick')"
      >我接单的</view>
    </view>

    <!-- 状态筛?-->
    <view class="status-bar" v-if="currentTab === 'my'">
      <view
        class="status-item"
        :class="{ active: currentStatus === item.value }"
        v-for="item in statusOptions"
        :key="item.value"
        @click="switchStatus(item.value)"
      >{{ item.label }}</view>
    </view>

    <!-- 订单列表 -->
    <scroll-view scroll-y class="order-list">
      <view 
        class="order-card" 
        v-for="item in expressList" 
        :key="item.id"
        @click="goToDetail(item.id)"
      >
        <!-- 订单状态标?-->
        <view class="order-header">
          <text class="order-title">{{ item.expressCompany }}</text>
          <text class="status-tag" :class="'status-' + item.status">
            {{ statusText(item.status) }}
          </text>
        </view>

        <!-- 快递信?-->
        <view class="order-info">
          <view class="info-row" v-if="item.pickupCode">
            <text class="info-label">🔢 取件码：</text>
            <text class="info-value">{{ item.pickupCode }}</text>
          </view>
          <view class="info-row">
            <text class="info-label">📍 取件地址：</text>
            <text class="info-value">{{ item.pickupAddress }}</text>
          </view>
          <view class="info-row">
            <text class="info-label">🏠 送达地址：</text>
            <text class="info-value">{{ item.deliveryAddress }}</text>
          </view>
          <view class="info-row">
            <text class="info-label">📏 预估重量：</text>
            <text class="info-value">{{ item.weight }}kg</text>
          </view>
        </view>

        <!-- 底部信息 -->
        <view class="order-footer">
          <view class="reward">
            <text class="reward-label">报酬</text>
            <text class="reward-value">¥{{ item.reward || item.serviceFee || 0 }}</text>
          </view>
          <!-- 接单大厅：待接单状态显示接单按?-->
          <view class="footer-btn" v-if="currentTab === 'hall' && item.status === 0" @click.stop="acceptOrder(item)">
            立即接单
          </view>
          <!-- 我的订单：待接单显示取消按钮 -->
          <view class="footer-btn cancel-btn" v-if="currentTab === 'my' && item.status === 0" @click.stop="cancelOrder(item)">
            取消订单
          </view>
          <!-- 我的订单：进行中显示确认完成按钮 -->
          <view class="footer-btn finish-btn" v-if="currentTab === 'my' && item.status === 1" @click.stop="finishOrder(item)">
            确认完成
          </view>
        </view>

        <!-- 备注 -->
        <view class="remark" v-if="item.remark">
          <text class="remark-text">备注：{{ item.remark }}</text>
        </view>
      </view>

      <view style="height: 40rpx;"></view>
    </scroll-view>

    <!-- 发布按钮（悬浮） -->
    <view class="publish-btn" @click="goPublish">
      <text class="publish-icon">+</text>
      <text>发布需：</text>
    </view>

    <!-- 发布弹窗 -->
    <view class="publish-modal" v-if="showPublishModal" @click.stop="closePublishModal">
      <view class="modal-content" @click.stop>
        <view class="modal-header">
          <text class="modal-title">发布快递代：</text>
          <text class="modal-close" @click="closePublishModal">×</text>
        </view>
        <scroll-view scroll-y class="modal-body">
          <view class="form-item">
            <text class="form-label">快递公 <text class="required">*</text></text>
            <input class="form-input" v-model="publishForm.expressCompany" placeholder="如：顺丰、圆？" />
          </view>
          <view class="form-item">
            <text class="form-label">取件地址 <text class="required">*</text></text>
            <input class="form-input" v-model="publishForm.pickupAddress" placeholder="如：菜鸟驿站1号柜" />
          </view>
          <view class="form-item">
            <text class="form-label">取件：</text>
            <input class="form-input" v-model="publishForm.pickupCode" placeholder="选填" />
          </view>
          <view class="form-item">
            <text class="form-label">送达地址 <text class="required">*</text></text>
            <input class="form-input" v-model="publishForm.deliveryAddress" placeholder="如：1号楼302宿舍" />
          </view>
          <view class="form-item">
            <text class="form-label">收件 <text class="required">*</text></text>
            <input class="form-input" v-model="publishForm.receiverName" placeholder="" />
          </view>
          <view class="form-item">
            <text class="form-label">联系电话 <text class="required">*</text></text>
            <input class="form-input" v-model="publishForm.receiverPhone" placeholder="" />
          </view>
          <view class="form-item">
            <text class="form-label">预估重量</text>
            <input class="form-input" type="digit" v-model="publishForm.weight" placeholder="单位：kg" />
          </view>
          <view class="form-item">
            <text class="form-label">报酬 <text class="required">*</text></text>
            <input class="form-input" type="digit" v-model="publishForm.serviceFee" placeholder="单位：元" />
          </view>
          <view class="form-item">
            <text class="form-label">小费</text>
            <input class="form-input" type="digit" v-model="publishForm.tip" placeholder="单位：元（选填？" />
          </view>
          <view class="form-item">
            <text class="form-label">备注</text>
            <textarea class="form-textarea" v-model="publishForm.remark" placeholder="选填，有什么特殊要求可以写在这？" />
          </view>
        </scroll-view>
        <view class="modal-footer">
          <view class="modal-btn cancel" @click="closePublishModal">取消</view>
          <view class="modal-btn confirm" @click="submitPublish">确认发布</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
/**
 * 快递代取页
 * 展示可接的快递代取订单，也可以发布自己的需? */
import { ref, onMounted, onUnmounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { expressApi } from '@/api/index'
import { checkLogin } from '@/utils/request'
import { guardWrite, onSchoolChanged } from '@/utils/school-permission'

const currentTab = ref('hall') // hall 接单大厅, my 我的订单
// 「我的订单」内的子筛选：我发布的 / 我接单的（修复：此前不区分，全部订单混在一起）
const currentRole = ref('publish')
// 「我的订单」内的状态筛选（数字与后端约定一致，'all' 表示全部?
const currentStatus = ref('all')
const expressList = ref([])
// 加载状?
const loading = ref(false)
// 发布弹窗
const showPublishModal = ref(false)
// 发布表单
const publishForm = ref({
  expressCompany: '',
  pickupAddress: '',
  pickupCode: '',
  deliveryAddress: '',
  receiverName: '',
  receiverPhone: '',
  weight: '',
  serviceFee: '',
  tip: '',
  remark: ''
})

// 「我的订单」状态选项（数字与后端 status 语义一致）
const statusOptions = [
  { label: '全部', value: 'all' },
  { label: '待接单', value: 1 },
  { label: '已接单', value: 2 },
  { label: '已完成', value: 3 },
  { label: '已取消', value: 4 },
  { label: '已退款', value: 5 }
]

onMounted(() => {
  loadExpressList()
})
// ---- 切换学校后刷新 ----
// 用户切换学校返回本页时，重新拉取当前学校的数据
// hasLoadedOnce 避免与 onMounted 首屏加载重复请求
let hasLoadedOnce = false
onShow(() => {
  onSchoolChanged()
  if (hasLoadedOnce) { loadExpressList() }
  hasLoadedOnce = true
})

/**
 * 加载订单列表
 *
 * 修复：「我的订单」原先不区分「我发布?/ 我接单的」，也没把状态传给后端，
 * 导致所有订单混在一起显示。现在传入 type（publish/pick）与 status 做服务端筛选。
 */
async function loadExpressList() {
  loading.value = true
  try {
    let data
    if (currentTab.value === 'hall') {
      // 大厅列表：调用大厅接口，无需登录
      data = await expressApi.getHallList({ pageNum: 1, pageSize: 10 })
    } else {
      // 我的订单：需要登录
      if (!checkLogin(false)) {
        loading.value = false
        return
      }
      data = await expressApi.getExpressList({
        type: currentRole.value,                                        // publish=我发布的 / pick=我接单的
        status: currentStatus.value !== 'all' ? currentStatus.value : undefined,
        pageNum: 1,
        pageSize: 10
      })
    }
    // MyBatis-Plus Page 对象的列表字段是 records
    expressList.value = data.records || data.list || data || []
  } catch (err) {
    console.warn('快递订单列表接口请求失败:', err)
    expressList.value = []
  } finally {
    loading.value = false
  }
}

/**
 * 切换Tab
 */
function switchTab(tab) {
  if (currentTab.value === tab) return
  currentTab.value = tab
  // 切到「我的订单」时重置子筛选
  if (tab === 'my') {
    currentRole.value = 'publish'
    currentStatus.value = 'all'
  }
  loadExpressList()
}

/**
 * 切换「我发布的 / 我接单的」
 */
function switchRole(role) {
  if (currentRole.value === role) return
  currentRole.value = role
  loadExpressList()
}

/**
 * 切换状态筛? */
function switchStatus(v) {
  if (currentStatus.value === v) return
  currentStatus.value = v
  loadExpressList()
}

/**
 * 状态文? */
function statusText(status) {
  const map = {
    0: '待接单',
    1: '进行中',
    2: '已完成',
    3: '已取消'
  }
  return map[status] || '未知'
}

/**
 * 跳转到详? */
function goToDetail(id) {
  uni.navigateTo({ url: '/pages/express/detail?id=' + id })
}

/**
 * 接单
 * 调用真实接单接口
 */
async function acceptOrder(item) {
  if (guardWrite()) return
  if (!checkLogin()) return
  uni.showModal({
    title: '确认接单',
    content: '确定要接这个快递代取订单吗？报酬 ¥' + (item.reward || item.serviceFee || 0),
    success: async (res) => {
      if (res.confirm) {
        try {
          // 调用接单接口
          await expressApi.acceptOrder(item.id)
          uni.showToast({ title: '接单成功', icon: 'success' })
          // 刷新列表
          loadExpressList()
        } catch (err) {
          // 接单失败，request 已统一处理错误提示
          console.warn('接单失败:', err)
        }
      }
    }
  })
}

/**
 * 取消订单
 */
async function cancelOrder(item) {
  if (guardWrite()) return
  uni.showModal({
    title: '确认取消',
    content: '确定要取消这个快递代取订单吗？',
    success: async (res) => {
      if (res.confirm) {
        try {
          await expressApi.cancelOrder(item.id)
          uni.showToast({ title: '已取消', icon: 'success' })
          loadExpressList()
        } catch (err) {
          console.warn('取消订单失败:', err)
        }
      }
    }
  })
}

/**
 * 确认完成订单
 */
async function finishOrder(item) {
  uni.showModal({
    title: '确认完成',
    content: '确认快递已经送达，订单完成？',
    success: async (res) => {
      if (res.confirm) {
        try {
          await expressApi.finishOrder(item.id)
          uni.showToast({ title: '订单已完成', icon: 'success' })
          loadExpressList()
        } catch (err) {
          console.warn('完成订单失败:', err)
        }
      }
    }
  })
}

/**
 * 打开发布弹窗
 */
function goPublish() {
  if (guardWrite()) return
  if (!checkLogin()) return
  // 重置表单
  publishForm.value = {
    expressCompany: '',
    pickupAddress: '',
    pickupCode: '',
    deliveryAddress: '',
    receiverName: '',
    receiverPhone: '',
    weight: '',
    serviceFee: '',
    tip: '',
    remark: ''
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
  if (!form.expressCompany.trim()) {
    uni.showToast({ title: '请填写快递公司', icon: 'none' })
    return
  }
  if (!form.pickupAddress.trim()) {
    uni.showToast({ title: '请填写取件地址', icon: 'none' })
    return
  }
  if (!form.deliveryAddress.trim()) {
    uni.showToast({ title: '请填写送达地址', icon: 'none' })
    return
  }
  if (!form.receiverName.trim()) {
    uni.showToast({ title: '请填写收件人', icon: 'none' })
    return
  }
  if (!form.receiverPhone.trim()) {
    uni.showToast({ title: '请填写联系电话', icon: 'none' })
    return
  }
  if (!form.serviceFee || Number(form.serviceFee) <= 0) {
    uni.showToast({ title: '请填写服务费', icon: 'none' })
    return
  }

  try {
    // 组装提交数据，数值类型转?
const submitData = {
      expressCompany: form.expressCompany.trim(),
      pickupAddress: form.pickupAddress.trim(),
      pickupCode: form.pickupCode.trim(),
      deliveryAddress: form.deliveryAddress.trim(),
      receiverName: form.receiverName.trim(),
      receiverPhone: form.receiverPhone.trim(),
      weight: form.weight ? Number(form.weight) : 0,
      serviceFee: Number(form.serviceFee),
      tip: form.tip ? Number(form.tip) : 0,
      remark: form.remark.trim()
    }
    await expressApi.publishExpress(submitData)
    uni.showToast({ title: '发布成功', icon: 'success' })
    showPublishModal.value = false
    // 切换到我的订?tab 并刷?    currentTab.value = 'my'
    loadExpressList()
  } catch (err) {
    console.warn('发布失败:', err)
  }
}
</script>

<style lang="scss" scoped>
.express-page {
  min-height: 100vh;
  background: #f5f5f7;
  display: flex;
  flex-direction: column;
}

/* Tab切换 */
.tab-bar {
  display: flex;
  background: #fff;
  position: sticky;
  top: 0;
  z-index: 10;
}

/* 「我发布?/ 我接单的」子筛?*/
.role-bar {
  display: flex;
  gap: 16rpx;
  padding: 20rpx 24rpx 0;
  background: #fff;
}

.role-item {
  padding: 12rpx 30rpx;
  border-radius: 26rpx;
  font-size: 26rpx;
  color: #666;
  background: #f5f5f7;
  transition: all 0.2s;

  &.active {
    color: #fff;
    background: linear-gradient(135deg, #6c4ee6 0%, #8b5cf6 100%);
    font-weight: 600;
  }
}

/* 状态筛选条 */
.status-bar {
  display: flex;
  gap: 12rpx;
  padding: 16rpx 24rpx 20rpx;
  background: #fff;
  overflow-x: auto;
  white-space: nowrap;

  &::-webkit-scrollbar {
    display: none;
  }
}

.status-item {
  padding: 10rpx 26rpx;
  border-radius: 22rpx;
  font-size: 24rpx;
  color: #666;
  background: #f5f5f7;
  flex-shrink: 0;
  transition: all 0.2s;

  &.active {
    color: #6c4ee6;
    background: #f1edfd;
    font-weight: 600;
    border: 1rpx solid #d9cfff;
  }
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

/* 订单列表 */
.order-list {
  flex: 1;
  padding: 20rpx 24rpx;
  box-sizing: border-box;
}

.order-card {
  background: #fff;
  border-radius: 20rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;
}

.order-title {
  font-size: 30rpx;
  font-weight: bold;
  color: #333;
}

.status-tag {
  padding: 6rpx 16rpx;
  font-size: 22rpx;
  border-radius: 20rpx;

  &.status-0 {
    background: #fff1f0;
    color: #ff4d4f;
  }
  &.status-1 {
    background: #e6f7ff;
    color: #1890ff;
  }
  &.status-2 {
    background: #fff7e6;
    color: #fa8c16;
  }
  &.status-3 {
    background: #f6ffed;
    color: #52c41a;
  }
}

/* 订单信息 */
.order-info {
  margin-bottom: 20rpx;
}

.info-row {
  display: flex;
  margin-bottom: 12rpx;
  font-size: 26rpx;

  &:last-child {
    margin-bottom: 0;
  }
}

.info-label {
  color: #999;
  flex-shrink: 0;
}

.info-value {
  color: #333;
  flex: 1;
}

/* 底部 */
.order-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 20rpx;
  border-top: 1rpx solid #f0f0f0;
}

.reward {
  display: flex;
  align-items: baseline;
  gap: 8rpx;
}

.reward-label {
  font-size: 24rpx;
  color: #999;
}

.reward-value {
  font-size: 36rpx;
  font-weight: bold;
  color: #ff4d4f;
}

.footer-btn {
  padding: 14rpx 36rpx;
  background: linear-gradient(135deg, #6c4ee6, #8b75f0);
  color: #fff;
  font-size: 26rpx;
  border-radius: 30rpx;

  &.cancel-btn {
    background: linear-gradient(135deg, #ff7875, #ffa39e);
  }

  &.finish-btn {
    background: linear-gradient(135deg, #52c41a, #95de64);
  }
}

/* 备注 */
.remark {
  margin-top: 16rpx;
  padding: 16rpx;
  background: #f9f9f9;
  border-radius: 12rpx;
}

.remark-text {
  font-size: 24rpx;
  color: #666;
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
  background: linear-gradient(135deg, #6c4ee6, #8b75f0);
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

/* 发布弹窗 */
.publish-modal {
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

.form-item {
  margin-bottom: 24rpx;
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
  height: 160rpx;
  padding: 16rpx 20rpx;
  border: 1rpx solid #e8e8e8;
  border-radius: 12rpx;
  font-size: 26rpx;
  color: #333;
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
