<template>
  <div class="page-container">
    <!-- ============ 页头 ============ -->
    <div class="page-header">
      <div>
        <h2 class="page-title">用户管理</h2>
        <p class="page-desc">查看平台注册用户，支持资料编辑与启用 / 禁用</p>
      </div>
      <div class="header-actions">
        <el-button :icon="Refresh" :loading="loading" @click="loadList">刷新</el-button>
      </div>
    </div>

    <!-- ============ 筛选区 ============ -->
    <div class="filter-bar">
      <el-form :model="query" inline @submit.prevent>
        <el-form-item label="昵称">
          <el-input
            v-model="query.nickname"
            placeholder="请输入昵称"
            clearable
            style="width: 180px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>

        <el-form-item label="手机号">
          <el-input
            v-model="query.phone"
            placeholder="请输入手机号"
            clearable
            style="width: 180px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>

        <el-form-item label="学校">
          <el-select
            v-model="query.schoolId"
            placeholder="全部学校"
            clearable
            filterable
            style="width: 200px"
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
        row-key="id"
        empty-text="暂无用户数据"
        style="width: 100%"
      >
        <el-table-column prop="id" label="ID" width="80" />

        <el-table-column label="头像" width="90" align="center">
          <template #default="{ row }">
            <el-avatar :size="38" :src="row.avatar || undefined" class="user-avatar">
              {{ avatarText(row.nickname) }}
            </el-avatar>
          </template>
        </el-table-column>

        <el-table-column label="昵称" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="nickname">{{ row.nickname || '未设置' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="手机号" width="140">
          <template #default="{ row }">
            <span>{{ row.phone || '-' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="学校名称" min-width="170" show-overflow-tooltip>
          <template #default="{ row }">
            <span>{{ schoolName(row.schoolId) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="160" align="center">
          <template #default="{ row }">
            <div class="status-cell">
              <el-switch
                v-model="row.status"
                :active-value="1"
                :inactive-value="0"
                :loading="statusLoading === row.id"
                @change="(val) => onSwitchChange(row, val)"
              />
              <el-tag
                :type="row.status === 1 ? 'success' : 'info'"
                size="small"
                effect="light"
              >
                {{ row.status === 1 ? '正常' : '禁用' }}
              </el-tag>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="注册时间" width="170">
          <template #default="{ row }">
            <span class="text-secondary">{{ formatTime(row.createTime) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="210" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link :icon="View" @click="openDetail(row)">详情</el-button>
            <el-button type="primary" link :icon="Edit" @click="openEdit(row)">编辑</el-button>
            <el-button
              :type="row.status === 1 ? 'danger' : 'success'"
              link
              :icon="row.status === 1 ? CircleClose : CircleCheck"
              :loading="statusLoading === row.id"
              @click="onToggleClick(row)"
            >
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- ============ 分页 ============ -->
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

    <!-- ============ 详情弹窗 ============ -->
    <el-dialog v-model="detailVisible" title="用户详情" width="680px" top="8vh">
      <div v-loading="detailLoading" class="detail-body">
        <el-descriptions :column="2" border class="detail-desc">
          <el-descriptions-item label="用户 ID">{{ detail.id ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="detail.status === 1 ? 'success' : 'info'" size="small">
              {{ detail.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </el-descriptions-item>

          <el-descriptions-item label="昵称">{{ detail.nickname || '-' }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ detail.phone || '-' }}</el-descriptions-item>

          <el-descriptions-item label="所属学校">
            {{ schoolName(detail.schoolId) }}
          </el-descriptions-item>
          <el-descriptions-item label="是否删除">
            {{ detail.deleted === 1 ? '已删除' : '否' }}
          </el-descriptions-item>

          <el-descriptions-item label="OpenID" :span="2">
            <span class="break-all">{{ detail.openid || '-' }}</span>
          </el-descriptions-item>

          <el-descriptions-item label="头像" :span="2">
            <el-avatar :size="54" :src="detail.avatar || undefined">
              {{ avatarText(detail.nickname) }}
            </el-avatar>
          </el-descriptions-item>

          <el-descriptions-item label="注册时间">
            {{ formatTimeFull(detail.createTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="更新时间">
            {{ formatTimeFull(detail.updateTime) }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button
          type="primary"
          :icon="Edit"
          @click="openEdit(detail); detailVisible = false"
        >
          编辑资料
        </el-button>
      </template>
    </el-dialog>

    <!-- ============ 编辑弹窗 ============ -->
    <el-dialog v-model="editVisible" title="编辑用户" width="480px">
      <el-form
        ref="editFormRef"
        :model="editForm"
        :rules="editRules"
        label-width="80px"
      >
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="editForm.nickname" placeholder="请输入昵称" clearable />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="editForm.phone" placeholder="请输入手机号" clearable />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="editLoading" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import dayjs from 'dayjs'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Refresh,
  RefreshLeft,
  Search,
  View,
  Edit,
  CircleCheck,
  CircleClose
} from '@element-plus/icons-vue'
import { userApi, schoolApi } from '@/api'

/* ---------- 列表状态 ---------- */
const loading = ref(false)
const list = ref([])
const total = ref(0)
const schools = ref([])
const statusLoading = ref(null)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  nickname: '',
  phone: '',
  schoolId: null
})

/* ---------- 弹窗状态 ---------- */
const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref({})

const editVisible = ref(false)
const editLoading = ref(false)
const editFormRef = ref()
const editForm = reactive({ id: null, nickname: '', phone: '' })
const editRules = {
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { min: 1, max: 30, message: '昵称长度不能超过 30 个字符', trigger: 'blur' }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ]
}

/* ---------- 工具方法 ---------- */
const schoolMap = computed(() => {
  const map = {}
  schools.value.forEach((s) => {
    map[s.id] = s.name
  })
  return map
})

function schoolName(id) {
  if (id === null || id === undefined || id === '') return '-'
  return schoolMap.value[id] || `学校 #${id}`
}

function avatarText(nickname) {
  const name = (nickname || '').trim()
  return name ? name.charAt(0).toUpperCase() : '?'
}

function formatTime(time) {
  return time ? dayjs(time).format('YYYY-MM-DD HH:mm') : '-'
}

function formatTimeFull(time) {
  return time ? dayjs(time).format('YYYY-MM-DD HH:mm:ss') : '-'
}

/* ---------- 数据加载 ---------- */
async function loadSchools() {
  try {
    const res = await schoolApi.all()
    schools.value = Array.isArray(res) ? res : []
  } catch {
    schools.value = []
  }
}

async function loadList() {
  loading.value = true
  try {
    const res = await userApi.list({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      nickname: query.nickname || undefined,
      phone: query.phone || undefined,
      schoolId: query.schoolId ?? undefined
    })
    list.value = Array.isArray(res?.records) ? res.records : []
    total.value = Number(res?.total || 0)
  } catch {
    // 请求层已提示错误，这里仅兜底清空
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
  query.nickname = ''
  query.phone = ''
  query.schoolId = null
  query.pageNum = 1
  loadList()
}

function handleSizeChange() {
  query.pageNum = 1
  loadList()
}

/* ---------- 启用 / 禁用 ---------- */
async function changeStatus(row, next) {
  const actionText = next === 1 ? '启用' : '禁用'
  try {
    await ElMessageBox.confirm(
      `确定要${actionText}用户「${row.nickname || row.id}」吗？`,
      '操作确认',
      {
        type: 'warning',
        confirmButtonText: `确定${actionText}`,
        cancelButtonText: '取消'
      }
    )
  } catch {
    return false
  }

  statusLoading.value = row.id
  try {
    await userApi.updateStatus(row.id, next)
    row.status = next
    ElMessage.success(`${actionText}成功`)
    return true
  } catch {
    return false
  } finally {
    statusLoading.value = null
  }
}

/** 表格内开关切换（取消或失败时回滚显示） */
async function onSwitchChange(row, val) {
  const next = Number(val)
  const ok = await changeStatus(row, next)
  if (!ok) row.status = next === 1 ? 0 : 1
}

/** 操作列按钮切换 */
async function onToggleClick(row) {
  await changeStatus(row, row.status === 1 ? 0 : 1)
}

/* ---------- 详情 ---------- */
async function openDetail(row) {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = { ...row }
  try {
    const res = await userApi.detail(row.id)
    if (res) detail.value = res
  } catch {
    // 接口失败时保留列表行数据兜底展示
  } finally {
    detailLoading.value = false
  }
}

/* ---------- 编辑 ---------- */
function openEdit(row) {
  if (!row || row.id === undefined) return
  editForm.id = row.id
  editForm.nickname = row.nickname || ''
  editForm.phone = row.phone || ''
  editVisible.value = true
  nextTick(() => editFormRef.value?.clearValidate())
}

async function submitEdit() {
  if (!editFormRef.value) return
  try {
    await editFormRef.value.validate()
  } catch {
    return
  }

  editLoading.value = true
  try {
    await userApi.update({
      id: editForm.id,
      nickname: editForm.nickname.trim(),
      phone: editForm.phone.trim()
    })
    ElMessage.success('保存成功')
    editVisible.value = false
    loadList()
  } catch {
    // 请求层已提示错误
  } finally {
    editLoading.value = false
  }
}

onMounted(() => {
  loadSchools()
  loadList()
})
</script>

<style lang="scss" scoped>
.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-avatar {
  background: var(--sc-gradient);
  color: #fff;
  font-weight: 600;
  font-size: 15px;
}

.nickname {
  font-weight: 500;
  color: var(--sc-text);
}

/* 状态列：开关 + 标签 */
.status-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.detail-body {
  min-height: 120px;
}

.break-all {
  word-break: break-all;
}
</style>
