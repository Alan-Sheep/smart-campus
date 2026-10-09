<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-title">失物招领审核</h2>
        <p class="page-desc">审核用户发布的寻物 / 招领信息，通过后才会在用户端展示</p>
      </div>
      <el-button :icon="Refresh" :loading="loading" @click="loadList">刷新</el-button>
    </div>

    <!-- 筛选 -->
    <div class="filter-bar">
      <el-form inline>
        <el-form-item label="审核状态">
          <el-select v-model="query.auditStatus" placeholder="全部" clearable style="width: 140px" @change="onSearch">
            <el-option label="待审核" :value="0" />
            <el-option label="已通过" :value="1" />
            <el-option label="已驳回" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="query.type" placeholder="全部类型" clearable style="width: 140px" @change="onSearch">
            <el-option label="寻物" :value="0" />
            <el-option label="招领" :value="1" />
          </el-select>
        </el-form-item>
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
      <el-table :data="displayList" v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="物品图" width="90">
          <template #default="{ row }">
            <el-image
              v-if="firstImage(row.images)"
              :src="firstImage(row.images)"
              :preview-src-list="imageList(row.images)"
              fit="cover"
              class="thumb"
              preview-teleported
            />
            <span v-else class="text-secondary">无图</span>
          </template>
        </el-table-column>
        <el-table-column prop="itemName" label="物品名称" min-width="160" show-overflow-tooltip />
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag :type="row.type === 1 ? 'success' : 'warning'" effect="light">
              {{ row.type === 1 ? '招领' : '寻物' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="location" label="地点" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ row.location || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'warning'" effect="light">
              {{ row.status === 1 ? '已解决' : '未解决' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="审核状态" width="110">
          <template #default="{ row }">
            <el-tag :type="auditTagType(row.auditStatus)" effect="light">
              {{ auditText(row.auditStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发布时间" width="160">
          <template #default="{ row }">{{ fmt(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button
              v-if="row.auditStatus !== 1"
              link
              type="success"
              @click="onAudit(row, 1)"
            >通过</el-button>
            <el-button
              v-if="row.auditStatus !== 2"
              link
              type="warning"
              @click="onReject(row)"
            >驳回</el-button>
            <el-button link type="danger" @click="onRemove(row)">下架</el-button>
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

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="失物招领详情" width="620px">
      <el-descriptions v-if="detail" :column="2" border class="detail-desc">
        <el-descriptions-item label="物品名称" :span="2">{{ detail.itemName }}</el-descriptions-item>
        <el-descriptions-item label="类型">
          <el-tag :type="detail.type === 1 ? 'success' : 'warning'" effect="light">
            {{ detail.type === 1 ? '招领' : '寻物' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="发布用户">{{ detail.userId }}</el-descriptions-item>
        <el-descriptions-item label="地点">{{ detail.location || '（未填写）' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="detail.status === 1 ? 'success' : 'warning'" effect="light">
            {{ detail.status === 1 ? '已解决' : '未解决' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="审核状态" :span="2">
          <el-tag :type="auditTagType(detail.auditStatus)">{{ auditText(detail.auditStatus) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">
          <span style="white-space: pre-wrap">{{ detail.description || '（无）' }}</span>
        </el-descriptions-item>
        <el-descriptions-item v-if="detail.rejectReason" label="驳回原因" :span="2">
          <span class="reject-text">{{ detail.rejectReason }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="物品图片" :span="2">
          <div class="img-grid">
            <el-image
              v-for="(img, i) in imageList(detail.images)"
              :key="i"
              :src="img"
              :preview-src-list="imageList(detail.images)"
              :initial-index="i"
              fit="cover"
              class="img-item"
              preview-teleported
            />
            <span v-if="!imageList(detail.images).length" class="text-secondary">（无图片）</span>
          </div>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <template v-if="detail && detail.auditStatus !== 1">
          <el-button type="success" @click="onAudit(detail, 1); detailVisible = false">通过审核</el-button>
        </template>
        <template v-if="detail && detail.auditStatus !== 2">
          <el-button type="warning" @click="onReject(detail); detailVisible = false">驳回</el-button>
        </template>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search, RefreshLeft } from '@element-plus/icons-vue'
import { auditApi, schoolApi } from '@/api'
import dayjs from 'dayjs'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const schools = ref([])
const detail = ref(null)
const detailVisible = ref(false)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  auditStatus: null,
  type: null,
  schoolId: null
})

/**
 * 后端 lostfoundList 已支持 type 服务端筛选（0:寻物 1:招领），
 * 因此这里直接使用服务端返回的分页数据，total 与筛选结果保持准确。
 */
const displayList = computed(() => list.value)

const fmt = (t) => (t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '-')

/** 后端 auditStatus：0待审核 1已通过 2已驳回 */
const auditText = (s) => ({ 0: '待审核', 1: '已通过', 2: '已驳回' }[s] ?? '未知')
const auditTagType = (s) => ({ 0: 'warning', 1: 'success', 2: 'danger' }[s] ?? 'info')

function imageList(images) {
  if (!images) return []
  return String(images)
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean)
}
const firstImage = (images) => imageList(images)[0] || ''

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
    const data = await auditApi.lostfoundList({ ...query })
    list.value = data?.records || []
    total.value = data?.total || 0
  } catch {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function onSearch() {
  query.pageNum = 1
  loadList()
}

function onReset() {
  query.auditStatus = null
  query.type = null
  query.schoolId = null
  query.pageNum = 1
  loadList()
}

async function openDetail(row) {
  try {
    detail.value = await auditApi.lostfoundDetail(row.id)
    detailVisible.value = true
  } catch {
    // 详情失败时退化为使用列表行数据
    detail.value = row
    detailVisible.value = true
  }
}

/** 审核通过 */
async function onAudit(row, status) {
  try {
    await ElMessageBox.confirm(`确定要通过「${row.itemName}」的审核吗？`, '审核确认', {
      type: 'success',
      confirmButtonText: '通过',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  try {
    await auditApi.lostfoundAudit(row.id, { auditStatus: status })
    ElMessage.success('已通过审核')
    loadList()
  } catch {
    /* 已提示 */
  }
}

/** 驳回（必须填写原因） */
async function onReject(row) {
  let reason = ''
  try {
    const { value } = await ElMessageBox.prompt('请输入驳回原因，将展示给发布者', '驳回审核', {
      confirmButtonText: '确定驳回',
      cancelButtonText: '取消',
      inputPlaceholder: '例如：物品信息不完整 / 图片与描述不符',
      inputValidator: (v) => (v && v.trim() ? true : '请填写驳回原因'),
      inputType: 'textarea'
    })
    reason = value
  } catch {
    return
  }
  try {
    await auditApi.lostfoundAudit(row.id, { auditStatus: 2, rejectReason: reason })
    ElMessage.success('已驳回')
    loadList()
  } catch {
    /* 已提示 */
  }
}

/** 下架/移除（后端该接口不接收请求体） */
async function onRemove(row) {
  try {
    await ElMessageBox.confirm(
      `确定要下架「${row.itemName}」吗？下架后用户端将不再展示。`,
      '下架信息',
      { type: 'warning', confirmButtonText: '确定下架', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  try {
    await auditApi.lostfoundRemove(row.id)
    ElMessage.success('已下架')
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
.reject-text {
  color: var(--sc-danger);
}

.img-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.img-item {
  width: 92px;
  height: 92px;
  border-radius: 8px;
  border: 1px solid var(--sc-border);
}
</style>
