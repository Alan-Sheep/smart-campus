<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-title">意见反馈</h2>
        <p class="page-desc">查看用户提交的意见与问题反馈，支持标记处理状态与官方回复</p>
      </div>
      <el-button :icon="Refresh" :loading="loading" @click="loadList">刷新</el-button>
    </div>

    <!-- 筛选 -->
    <div class="filter-bar">
      <el-form inline>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 140px" @change="onSearch">
            <el-option label="待处理" :value="0" />
            <el-option label="处理中" :value="1" />
            <el-option label="已完成" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="query.type" placeholder="全部类型" clearable style="width: 150px" @change="onSearch">
            <el-option label="功能建议" :value="0" />
            <el-option label="BUG反馈" :value="1" />
            <el-option label="投诉举报" :value="2" />
            <el-option label="其他" :value="3" />
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
      <el-table :data="list" v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="类型" width="110">
          <template #default="{ row }">
            <el-tag :type="typeTagType(row.type)" effect="light">{{ typeText(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
        <el-table-column label="内容摘要" min-width="220">
          <template #default="{ row }">
            <span class="text-secondary">{{ summary(row.content) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="联系方式" width="150">
          <template #default="{ row }">{{ row.contact || '-' }}</template>
        </el-table-column>
        <el-table-column prop="userId" label="用户ID" width="90" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" effect="light">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="160">
          <template #default="{ row }">{{ fmt(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-dropdown trigger="click" @command="(cmd) => onHandle(row, cmd)">
              <el-button link type="warning" :loading="handlingId === row.id">处理</el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item :command="0" :disabled="row.status === 0">标记待处理</el-dropdown-item>
                  <el-dropdown-item :command="1" :disabled="row.status === 1">标记处理中</el-dropdown-item>
                  <el-dropdown-item :command="2" :disabled="row.status === 2">标记已完成</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <el-button link type="success" @click="openReply(row)">回复</el-button>
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
    <el-dialog v-model="detailVisible" title="反馈详情" width="640px">
      <el-descriptions v-if="detail" :column="2" border class="detail-desc">
        <el-descriptions-item label="标题" :span="2">{{ detail.title }}</el-descriptions-item>
        <el-descriptions-item label="类型">
          <el-tag :type="typeTagType(detail.type)" effect="light">{{ typeText(detail.type) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="用户ID">{{ detail.userId }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusTagType(detail.status)" effect="light">{{ statusText(detail.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="联系方式">{{ detail.contact || '-' }}</el-descriptions-item>
        <el-descriptions-item label="提交时间" :span="2">{{ fmt(detail.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="内容" :span="2">
          <span style="white-space: pre-wrap">{{ detail.content || '（无）' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="图片" :span="2">
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
        <el-descriptions-item v-if="detail.reply" label="回复内容" :span="2">
          <span class="reply-text" style="white-space: pre-wrap">{{ detail.reply }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="回复时间">{{ fmt(detail.replyTime) }}</el-descriptions-item>
        <el-descriptions-item label="回复管理员ID">{{ detail.replyAdminId ?? '-' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button v-if="detail" type="success" @click="openReply(detail); detailVisible = false">回复</el-button>
      </template>
    </el-dialog>

    <!-- 回复弹窗 -->
    <el-dialog v-model="replyVisible" title="回复反馈" width="600px">
      <div v-if="replyRow" class="reply-origin">
        <div class="reply-origin-title">
          <span class="reply-origin-label">反馈标题：</span>{{ replyRow.title }}
        </div>
        <div class="reply-origin-label">反馈内容：</div>
        <div class="reply-origin-content">{{ replyRow.content || '（无）' }}</div>
      </div>
      <el-input
        v-model="replyText"
        type="textarea"
        :rows="5"
        maxlength="500"
        show-word-limit
        placeholder="请输入回复内容，将展示给提交反馈的用户"
      />
      <template #footer>
        <el-button @click="replyVisible = false">取消</el-button>
        <el-button type="primary" :loading="replyLoading" @click="onReplySubmit">提交回复</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search, RefreshLeft } from '@element-plus/icons-vue'
import { feedbackApi, schoolApi } from '@/api'
import dayjs from 'dayjs'

const loading = ref(false)
const handlingId = ref(null)
const list = ref([])
const total = ref(0)
const schools = ref([])
const detail = ref(null)
const detailVisible = ref(false)

const replyVisible = ref(false)
const replyLoading = ref(false)
const replyRow = ref(null)
const replyText = ref('')

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  status: null,
  type: null,
  schoolId: null
})

const fmt = (t) => (t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '-')

/** 后端 type：0功能建议 1BUG反馈 2投诉举报 3其他 */
const typeText = (t) => ({ 0: '功能建议', 1: 'BUG反馈', 2: '投诉举报', 3: '其他' }[t] ?? '未知')
const typeTagType = (t) => ({ 0: 'primary', 1: 'danger', 2: 'warning', 3: 'info' }[t] ?? 'info')

/** 后端 status：0待处理 1处理中 2已完成 */
const statusText = (s) => ({ 0: '待处理', 1: '处理中', 2: '已完成' }[s] ?? '未知')
const statusTagType = (s) => ({ 0: 'warning', 1: 'primary', 2: 'success' }[s] ?? 'info')

/** 内容摘要：截断到 50 个字符 */
function summary(content) {
  const text = String(content || '').replace(/\s+/g, ' ').trim()
  if (!text) return '-'
  return text.length > 50 ? text.slice(0, 50) + '…' : text
}

function imageList(images) {
  if (!images) return []
  return String(images)
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean)
}

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
    const data = await feedbackApi.list({ ...query })
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
  query.status = null
  query.type = null
  query.schoolId = null
  query.pageNum = 1
  loadList()
}

async function openDetail(row) {
  try {
    detail.value = await feedbackApi.detail(row.id)
    detailVisible.value = true
  } catch {
    // 详情失败时退化为使用列表行数据
    detail.value = row
    detailVisible.value = true
  }
}

/** 标记处理状态（query 参数 status） */
async function onHandle(row, status) {
  const label = statusText(status)
  try {
    await ElMessageBox.confirm(`确定将「${row.title}」标记为${label}吗？`, '处理确认', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  handlingId.value = row.id
  try {
    await feedbackApi.handle(row.id, status)
    ElMessage.success(`已标记为${label}`)
    loadList()
  } catch {
    /* 已提示 */
  } finally {
    handlingId.value = null
  }
}

function openReply(row) {
  replyRow.value = row
  replyText.value = ''
  replyVisible.value = true
}

/** 提交回复（后端 body 参数名为 reply） */
async function onReplySubmit() {
  const content = replyText.value.trim()
  if (!content) {
    ElMessage.warning('请输入回复内容')
    return
  }
  replyLoading.value = true
  try {
    await feedbackApi.reply(replyRow.value.id, { reply: content })
    ElMessage.success('回复成功')
    replyVisible.value = false
    loadList()
  } catch {
    /* 已提示 */
  } finally {
    replyLoading.value = false
  }
}

onMounted(() => {
  loadSchools()
  loadList()
})
</script>

<style lang="scss" scoped>
.reply-text {
  color: var(--sc-success);
}

.reply-origin {
  padding: 12px 14px;
  margin-bottom: 14px;
  border-radius: var(--sc-radius-sm);
  background: var(--sc-bg);
  border: 1px solid var(--sc-border);
}

.reply-origin-title {
  font-weight: 600;
  margin-bottom: 8px;
  color: var(--sc-text);
}

.reply-origin-label {
  font-size: 13px;
  color: var(--sc-text-secondary);
}

.reply-origin-content {
  margin-top: 4px;
  max-height: 120px;
  overflow-y: auto;
  white-space: pre-wrap;
  color: var(--sc-text-regular);
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
