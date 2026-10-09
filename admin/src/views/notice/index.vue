<template>
  <div class="page-container">
    <!-- ============ 页面头部 ============ -->
    <div class="page-header">
      <div>
        <h2 class="page-title">公告管理</h2>
        <p class="page-desc">发布与管理校园公告，支持置顶与多学校归属</p>
      </div>
      <div class="header-actions">
        <el-button type="primary" :icon="Plus" @click="openDialog()">新增公告</el-button>
      </div>
    </div>

    <!-- ============ 筛选区 ============ -->
    <div class="filter-bar">
      <el-form :model="query" inline @submit.prevent>
        <el-form-item label="公告标题">
          <el-input
            v-model="query.title"
            placeholder="请输入公告标题"
            clearable
            style="width: 220px"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />
        </el-form-item>
        <el-form-item label="学校">
          <el-select
            v-model="query.schoolId"
            placeholder="全部学校"
            clearable
            style="width: 200px"
            @change="handleSearch"
          >
            <el-option
              v-for="s in schools"
              :key="s.id"
              :label="s.name"
              :value="s.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" :loading="loading" @click="handleSearch">
            查询
          </el-button>
          <el-button :icon="RefreshLeft" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- ============ 表格 ============ -->
    <div class="table-card">
      <el-table
        v-loading="loading"
        :data="list"
        stripe
        row-key="id"
        style="width: 100%"
      >
        <el-table-column prop="id" label="ID" width="80" align="center" />

        <el-table-column label="标题" min-width="230">
          <template #default="{ row }">
            <div class="title-cell">
              <el-tag v-if="row.isTop === 1" type="danger" size="small" effect="dark">置顶</el-tag>
              <span class="title-text">{{ row.title || '-' }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="内容" min-width="240">
          <template #default="{ row }">
            <el-tooltip
              v-if="row.content"
              :content="row.content"
              placement="top"
              :show-after="200"
            >
              <span class="content-ellipsis">{{ truncate(row.content, 40) }}</span>
            </el-tooltip>
            <span v-else class="text-secondary">-</span>
          </template>
        </el-table-column>

        <el-table-column label="学校" min-width="140">
          <template #default="{ row }">
            <span>{{ schoolName(row.schoolId) }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="sort" label="排序" width="80" align="center">
          <template #default="{ row }">
            <span class="sort-num">{{ row.sort ?? 0 }}</span>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="light" size="small">
              {{ row.status === 1 ? '已发布' : '已下架' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="创建时间" width="165">
          <template #default="{ row }">
            <span class="text-secondary">{{ formatTime(row.createTime) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="290" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link :icon="View" @click="openDetail(row)">查看</el-button>
            <el-button type="primary" link :icon="Edit" @click="openDialog(row)">编辑</el-button>
            <el-button
              :type="row.isTop === 1 ? 'warning' : 'primary'"
              link
              :icon="row.isTop === 1 ? Bottom : Top"
              @click="toggleTop(row)"
            >
              {{ row.isTop === 1 ? '取消置顶' : '置顶' }}
            </el-button>
            <el-button type="danger" link :icon="Delete" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          background
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="loadList"
        />
      </div>
    </div>

    <!-- ============ 查看详情弹窗 ============ -->
    <el-dialog v-model="detailVisible" title="公告详情" width="720px" top="8vh">
      <div v-loading="detailLoading" class="detail-body">
        <h3 class="detail-title">
          <el-tag v-if="detail.isTop === 1" type="danger" size="small" effect="dark">置顶</el-tag>
          <span>{{ detail.title || '-' }}</span>
        </h3>

        <el-descriptions :column="2" border class="detail-desc">
          <el-descriptions-item label="公告ID">{{ detail.id ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="所属学校">{{ schoolName(detail.schoolId) }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="detail.status === 1 ? 'success' : 'info'" size="small" effect="light">
              {{ detail.status === 1 ? '已发布' : '已下架' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="排序">{{ detail.sort ?? 0 }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatTime(detail.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ formatTime(detail.updateTime) }}</el-descriptions-item>
        </el-descriptions>

        <div class="detail-content-label">公告内容</div>
        <div class="detail-content">{{ detail.content || '（暂无内容）' }}</div>
      </div>

      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button type="primary" :icon="Edit" @click="editFromDetail">编辑</el-button>
      </template>
    </el-dialog>

    <!-- ============ 新增 / 编辑弹窗 ============ -->
    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '编辑公告' : '新增公告'"
      width="720px"
      top="7vh"
      destroy-on-close
      @closed="handleDialogClosed"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
        <el-form-item label="公告标题" prop="title">
          <el-input
            v-model="form.title"
            placeholder="请输入公告标题"
            maxlength="100"
            show-word-limit
            clearable
          />
        </el-form-item>

        <el-form-item label="公告内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="6"
            placeholder="请输入公告内容，支持换行"
            maxlength="2000"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="所属学校" prop="schoolId">
          <el-select v-model="form.schoolId" placeholder="请选择学校" style="width: 100%" clearable>
            <el-option v-for="s in schools" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="排序" prop="sort">
          <el-input-number
            v-model="form.sort"
            :min="0"
            :max="9999"
            :step="1"
            controls-position="right"
          />
          <span class="form-tip">数值越小越靠前</span>
        </el-form-item>

        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">发布</el-radio>
            <el-radio :value="0">下架</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="是否置顶" prop="isTop">
          <el-switch
            v-model="form.isTop"
            :active-value="1"
            :inactive-value="0"
            active-text="置顶"
            inactive-text="普通"
            inline-prompt
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import dayjs from 'dayjs'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, RefreshLeft, Edit, Delete, View, Top, Bottom } from '@element-plus/icons-vue'
import { noticeApi, schoolApi } from '@/api'

/* ---------- 列表状态 ---------- */
const loading = ref(false)
const submitting = ref(false)
const list = ref([])
const total = ref(0)
const schools = ref([])

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  title: '',
  schoolId: null
})

/* ---------- 弹窗状态 ---------- */
const dialogVisible = ref(false)
const formRef = ref(null)
const form = reactive({
  id: null,
  title: '',
  content: '',
  schoolId: null,
  sort: 0,
  status: 1,
  isTop: 0
})

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref({})

const rules = {
  title: [
    { required: true, message: '请输入公告标题', trigger: 'blur' },
    { max: 100, message: '标题长度需在 100 个字符以内', trigger: 'blur' }
  ],
  content: [{ required: true, message: '请输入公告内容', trigger: 'blur' }],
  schoolId: [{ required: true, message: '请选择所属学校', trigger: 'change' }]
}

/* ---------- 工具函数 ---------- */
function formatTime(value) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-'
}

function truncate(text, len) {
  if (!text) return ''
  const plain = String(text).replace(/\s+/g, ' ').trim()
  return plain.length > len ? `${plain.slice(0, len)}...` : plain
}

function schoolName(id) {
  if (id === null || id === undefined) return '全部学校'
  const hit = schools.value.find((s) => String(s.id) === String(id))
  return hit ? hit.name : `学校#${id}`
}

/* ---------- 数据加载 ---------- */
async function loadSchools() {
  try {
    schools.value = (await schoolApi.all()) || []
  } catch {
    schools.value = []
  }
}

async function loadList() {
  loading.value = true
  try {
    const data = await noticeApi.list({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      title: query.title || undefined,
      schoolId: query.schoolId || undefined
    })
    list.value = data?.records || []
    total.value = data?.total || 0
  } catch {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.pageNum = 1
  loadList()
}

function handleReset() {
  query.title = ''
  query.schoolId = null
  query.pageNum = 1
  query.pageSize = 10
  loadList()
}

function handleSizeChange() {
  query.pageNum = 1
  loadList()
}

/* ---------- 查看详情 ---------- */
function openDetail(row) {
  detail.value = { ...row }
  detailVisible.value = true
}

function editFromDetail() {
  const row = detail.value
  detailVisible.value = false
  openDialog(row)
}

/* ---------- 新增 / 编辑 ---------- */
function openDialog(row) {
  if (row && row.id) {
    Object.assign(form, {
      id: row.id,
      title: row.title || '',
      content: row.content || '',
      schoolId: row.schoolId ?? null,
      sort: row.sort ?? 0,
      status: row.status ?? 1,
      isTop: row.isTop ?? 0
    })
  } else {
    Object.assign(form, {
      id: null,
      title: '',
      content: '',
      schoolId: query.schoolId ?? null,
      sort: nextSort(),
      status: 1,
      isTop: 0
    })
  }
  dialogVisible.value = true
}

function nextSort() {
  if (!list.value.length) return 0
  return Math.max(...list.value.map((i) => Number(i.sort) || 0)) + 1
}

function handleDialogClosed() {
  formRef.value?.clearValidate()
}

async function handleSubmit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitting.value = true
  const payload = {
    id: form.id ?? undefined,
    title: form.title,
    content: form.content,
    schoolId: form.schoolId,
    sort: form.sort ?? 0,
    status: form.status,
    isTop: form.isTop
  }

  try {
    if (form.id) {
      await noticeApi.update(payload)
      ElMessage.success('修改成功')
    } else {
      await noticeApi.add(payload)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadList()
  } finally {
    submitting.value = false
  }
}

/* ---------- 置顶切换 ---------- */
async function toggleTop(row) {
  const next = row.isTop === 1 ? 0 : 1
  const action = next === 1 ? '置顶' : '取消置顶'
  try {
    await ElMessageBox.confirm(`确定要${action}公告「${row.title || row.id}」吗？`, '提示', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  await noticeApi.top(row.id, next)
  ElMessage.success(`${action}成功`)
  loadList()
}

/* ---------- 删除 ---------- */
async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定要删除公告「${row.title || row.id}」吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '确定删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  await noticeApi.remove(row.id)
  ElMessage.success('删除成功')
  if (list.value.length === 1 && query.pageNum > 1) {
    query.pageNum -= 1
  }
  loadList()
}

onMounted(() => {
  loadSchools()
  loadList()
})
</script>

<style lang="scss" scoped>
.header-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.title-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.title-text {
  font-weight: 500;
  color: var(--sc-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.content-ellipsis {
  display: inline-block;
  max-width: 100%;
  vertical-align: middle;
  color: var(--sc-text-regular);
  cursor: default;
}

.sort-num {
  display: inline-block;
  min-width: 26px;
  padding: 1px 8px;
  border-radius: 6px;
  background: var(--sc-primary-soft);
  color: var(--sc-primary);
  font-weight: 600;
  font-size: 13px;
}

/* ---------- 详情弹窗 ---------- */
.detail-body {
  min-height: 120px;
}

.detail-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 17px;
  font-weight: 600;
  color: var(--sc-text);
  margin: 0 0 16px;
  line-height: 1.5;
}

.detail-content-label {
  margin: 18px 0 8px;
  font-size: 13px;
  font-weight: 600;
  color: var(--sc-text-secondary);
}

.detail-content {
  padding: 16px 18px;
  border-radius: var(--sc-radius-sm);
  background: #fafbfe;
  border: 1px solid var(--sc-border);
  color: var(--sc-text-regular);
  font-size: 14px;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 46vh;
  overflow-y: auto;
}

.form-tip {
  margin-left: 10px;
  font-size: 12px;
  color: var(--sc-text-secondary);
}
</style>
