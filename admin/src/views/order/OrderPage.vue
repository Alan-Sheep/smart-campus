<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-title">{{ config.title }}</h2>
        <p class="page-desc">{{ config.desc }}</p>
      </div>
      <el-button :icon="Refresh" :loading="loading" @click="loadList">刷新</el-button>
    </div>

    <!-- 状态快捷筛选 -->
    <div class="status-tabs">
      <div
        v-for="tab in statusTabs"
        :key="tab.value"
        class="status-tab"
        :class="{ active: query.status === tab.value }"
        @click="onStatusChange(tab.value)"
      >
        {{ tab.label }}
      </div>
    </div>

    <!-- 筛选 -->
    <div class="filter-bar">
      <el-form inline>
        <el-form-item label="学校">
          <el-select v-model="query.schoolId" placeholder="全部学校" clearable style="width: 180px" @change="onSearch">
            <el-option v-for="s in schools" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="onSearch">查询</el-button>
          <el-button :icon="RefreshLeft" @click="onReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 表格 -->
    <div class="table-card">
      <el-table :data="list" v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="orderNo" label="订单号" width="190" show-overflow-tooltip />
        <el-table-column
          v-for="c in config.columns"
          :key="c.prop"
          :prop="c.prop"
          :label="c.label"
          :width="c.width"
          :min-width="c.minWidth"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            <span v-if="c.money" class="money">¥{{ Number(row[c.prop] || 0).toFixed(2) }}</span>
            <span v-else>{{ row[c.prop] ?? '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" effect="light">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="下单时间" width="160">
          <template #default="{ row }">{{ fmt(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button v-if="row.status === 1" link type="success" @click="onAccept(row)">接单</el-button>
            <el-button v-if="row.status === 2" link type="success" @click="onComplete(row)">完成</el-button>
            <el-button v-if="row.status === 1 || row.status === 2" link type="warning" @click="onCancel(row)">取消</el-button>
            <el-button v-if="row.status === 4" link type="danger" @click="onRefund(row)">退款</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="loadList"
          @current-change="loadList"
        />
      </div>
    </div>

    <!-- 详情 -->
    <el-dialog v-model="detailVisible" :title="config.title + ' - 订单详情'" width="680px">
      <el-descriptions v-if="detail" :column="2" border class="detail-desc">
        <el-descriptions-item label="订单号" :span="2">{{ detail.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="订单ID">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusTagType(detail.status)">{{ statusText(detail.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item
          v-for="c in config.detailFields"
          :key="c.prop"
          :label="c.label"
          :span="c.span || 1"
        >
          <span v-if="c.money">¥{{ Number(detail[c.prop] || 0).toFixed(2) }}</span>
          <span v-else-if="c.pre" style="white-space: pre-wrap">{{ detail[c.prop] || '（无）' }}</span>
          <span v-else>{{ detail[c.prop] ?? '-' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="下单时间">{{ fmt(detail.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="接单时间">{{ fmt(detail.acceptTime) }}</el-descriptions-item>
        <el-descriptions-item label="完成时间">{{ fmt(detail.completeTime) }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.cancelReason" label="取消原因" :span="2">
          <span class="danger-text">{{ detail.cancelReason }}</span>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button v-if="detail && detail.status === 1" type="success" @click="onAccept(detail); detailVisible = false">接单</el-button>
        <el-button v-if="detail && detail.status === 2" type="success" @click="onComplete(detail); detailVisible = false">完成订单</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * 订单管理通用页面
 * 快递 / 打印 / 跑腿 / 代课 四种订单结构相似，统一由此组件按 config 渲染，
 * 避免四份重复代码，也保证交互与状态语义完全一致。
 *
 * 状态（与后端 AdminOrderController 一致）：
 * 1=待接单 2=已接单 3=已完成 4=已取消 5=已退款
 */
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search, RefreshLeft } from '@element-plus/icons-vue'
import { orderApi, schoolApi } from '@/api'
import dayjs from 'dayjs'

const props = defineProps({
  module: { type: String, required: true } // express | print | errand | substitute
})

// 每个模块的列与标题配置
const CONFIGS = {
  express: {
    title: '快递代取订单',
    desc: '管理快递代取订单，可代接单、完成、取消与退款',
    columns: [
      { prop: 'expressCompany', label: '快递公司', width: 110 },
      { prop: 'pickupCode', label: '取件码', width: 100 },
      { prop: 'pickupAddress', label: '取件地址', minWidth: 150 },
      { prop: 'deliveryAddress', label: '送达地址', minWidth: 150 },
      { prop: 'receiverName', label: '收件人', width: 100 },
      { prop: 'receiverPhone', label: '联系电话', width: 125 },
      { prop: 'totalAmount', label: '金额', width: 90, money: true }
    ],
    detailFields: [
      { prop: 'expressCompany', label: '快递公司' },
      { prop: 'pickupCode', label: '取件码' },
      { prop: 'pickupAddress', label: '取件地址', span: 2 },
      { prop: 'deliveryAddress', label: '送达地址', span: 2 },
      { prop: 'receiverName', label: '收件人' },
      { prop: 'receiverPhone', label: '联系电话' },
      { prop: 'serviceFee', label: '服务费', money: true },
      { prop: 'tip', label: '小费', money: true },
      { prop: 'totalAmount', label: '总金额', money: true },
      { prop: 'remark', label: '备注', span: 2, pre: true }
    ]
  },
  print: {
    title: '打印服务订单',
    desc: '管理打印订单，可代接单、完成、取消与退款',
    columns: [
      { prop: 'fileUrls', label: '文件', minWidth: 160 },
      { prop: 'paperSize', label: '纸张', width: 80 },
      { prop: 'copies', label: '份数', width: 70 },
      { prop: 'deliveryAddress', label: '收货地址', minWidth: 150 },
      { prop: 'receiverName', label: '收件人', width: 100 },
      { prop: 'receiverPhone', label: '联系电话', width: 125 },
      { prop: 'totalAmount', label: '金额', width: 90, money: true }
    ],
    detailFields: [
      { prop: 'fileUrls', label: '文件地址', span: 2 },
      { prop: 'printType', label: '打印类型' },
      { prop: 'paperSize', label: '纸张规格' },
      { prop: 'copies', label: '份数' },
      { prop: 'isDoubleSide', label: '是否双面' },
      { prop: 'totalPages', label: '总页数' },
      { prop: 'deliveryAddress', label: '收货地址', span: 2 },
      { prop: 'receiverName', label: '收件人' },
      { prop: 'receiverPhone', label: '联系电话' },
      { prop: 'totalAmount', label: '总金额', money: true },
      { prop: 'remark', label: '备注', span: 2, pre: true }
    ]
  },
  errand: {
    title: '校园跑腿订单',
    desc: '管理跑腿订单，可代接单、完成、取消与退款',
    columns: [
      { prop: 'title', label: '标题', minWidth: 150 },
      { prop: 'pickupAddress', label: '取货地址', minWidth: 130 },
      { prop: 'deliveryAddress', label: '送达地址', minWidth: 130 },
      { prop: 'receiverName', label: '收货人', width: 100 },
      { prop: 'receiverPhone', label: '联系电话', width: 125 },
      { prop: 'totalAmount', label: '金额', width: 90, money: true }
    ],
    detailFields: [
      { prop: 'title', label: '标题', span: 2 },
      { prop: 'description', label: '描述', span: 2, pre: true },
      { prop: 'pickupAddress', label: '取货地址', span: 2 },
      { prop: 'deliveryAddress', label: '送达地址', span: 2 },
      { prop: 'receiverName', label: '收货人' },
      { prop: 'receiverPhone', label: '联系电话' },
      { prop: 'estimatedTime', label: '期望时间' },
      { prop: 'serviceFee', label: '跑腿费', money: true },
      { prop: 'goodsFee', label: '商品费', money: true },
      { prop: 'tip', label: '小费', money: true },
      { prop: 'totalAmount', label: '总金额', money: true }
    ]
  },
  substitute: {
    title: '代课服务订单',
    desc: '管理代课订单，可代接单、完成、取消与退款',
    columns: [
      { prop: 'courseName', label: '课程', minWidth: 140 },
      { prop: 'courseDate', label: '上课日期', width: 120 },
      { prop: 'courseTime', label: '上课时间', width: 130 },
      { prop: 'classroom', label: '教室', width: 110 },
      { prop: 'teacher', label: '老师', width: 100 },
      { prop: 'reward', label: '报酬', width: 90, money: true }
    ],
    detailFields: [
      { prop: 'courseName', label: '课程名称', span: 2 },
      { prop: 'courseDate', label: '上课日期' },
      { prop: 'courseTime', label: '上课时间' },
      { prop: 'classroom', label: '教室' },
      { prop: 'teacher', label: '老师' },
      { prop: 'major', label: '专业' },
      { prop: 'reward', label: '报酬', money: true },
      { prop: 'requirement', label: '要求', span: 2, pre: true },
      { prop: 'contactWechat', label: '微信' },
      { prop: 'contactQq', label: 'QQ' }
    ]
  }
}

const config = CONFIGS[props.module] || CONFIGS.express
const api = orderApi[props.module]

const loading = ref(false)
const list = ref([])
const total = ref(0)
const schools = ref([])
const detail = ref(null)
const detailVisible = ref(false)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  status: null,
  schoolId: null
})

const statusTabs = [
  { label: '全部', value: null },
  { label: '待接单', value: 1 },
  { label: '已接单', value: 2 },
  { label: '已完成', value: 3 },
  { label: '已取消', value: 4 },
  { label: '已退款', value: 5 }
]

const fmt = (t) => (t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '-')

const statusText = (s) =>
  ({ 1: '待接单', 2: '已接单', 3: '已完成', 4: '已取消', 5: '已退款' }[s] ?? '未知')
const statusTagType = (s) =>
  ({ 1: 'warning', 2: 'primary', 3: 'success', 4: 'info', 5: 'danger' }[s] ?? 'info')

async function loadSchools() {
  try {
    schools.value = await schoolApi.all()
  } catch {
    schools.value = []
  }
}

async function loadList() {
  loading.value = true
  try {
    const data = await api.list({ ...query })
    list.value = data?.records || []
    total.value = data?.total || 0
  } catch {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function onStatusChange(v) {
  query.status = v
  query.pageNum = 1
  loadList()
}

function onSearch() {
  query.pageNum = 1
  loadList()
}

function onReset() {
  query.status = null
  query.schoolId = null
  query.pageNum = 1
  loadList()
}

async function openDetail(row) {
  try {
    detail.value = await api.detail(row.id)
  } catch {
    detail.value = row
  }
  detailVisible.value = true
}

async function onAccept(row) {
  try {
    await ElMessageBox.confirm(`确定要接单（订单号 ${row.orderNo}）吗？`, '接单确认', {
      type: 'info'
    })
  } catch {
    return
  }
  try {
    await api.accept(row.id)
    ElMessage.success('已接单')
    loadList()
  } catch {
    /* 已提示 */
  }
}

async function onComplete(row) {
  try {
    await ElMessageBox.confirm(`确定要将订单 ${row.orderNo} 标记为已完成吗？`, '完成确认', {
      type: 'success'
    })
  } catch {
    return
  }
  try {
    await api.complete(row.id)
    ElMessage.success('已完成')
    loadList()
  } catch {
    /* 已提示 */
  }
}

async function onCancel(row) {
  let reason = ''
  try {
    const { value } = await ElMessageBox.prompt('请输入取消原因', '取消订单', {
      confirmButtonText: '确定取消',
      cancelButtonText: '返回',
      inputPlaceholder: '例如：用户申请取消',
      inputValidator: (v) => (v && v.trim() ? true : '请填写取消原因'),
      inputType: 'textarea'
    })
    reason = value
  } catch {
    return
  }
  try {
    await api.cancel(row.id, reason)
    ElMessage.success('已取消')
    loadList()
  } catch {
    /* 已提示 */
  }
}

async function onRefund(row) {
  try {
    await ElMessageBox.confirm(
      `确定要为订单 ${row.orderNo} 退款吗？该操作不可撤销。`,
      '退款确认',
      { type: 'warning', confirmButtonText: '确定退款' }
    )
  } catch {
    return
  }
  try {
    await api.refund(row.id)
    ElMessage.success('已退款')
    loadList()
  } catch {
    /* 已提示 */
  }
}

onMounted(() => {
  loadSchools()
  loadList()
})
</script>

<style lang="scss" scoped>
/* 状态快捷筛选条 */
.status-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.status-tab {
  padding: 8px 18px;
  border-radius: 20px;
  background: #fff;
  color: var(--sc-text-regular);
  font-size: 13px;
  cursor: pointer;
  border: 1px solid var(--sc-border);
  transition: all 0.2s ease;
  user-select: none;

  &:hover {
    color: var(--sc-primary);
    border-color: var(--sc-primary-light);
  }

  &.active {
    background: var(--sc-gradient);
    color: #fff;
    border-color: transparent;
    font-weight: 600;
    box-shadow: 0 4px 12px rgba(108, 78, 230, 0.28);
  }
}

.money {
  color: #d03050;
  font-weight: 600;
}

.danger-text {
  color: var(--sc-danger);
}
</style>
