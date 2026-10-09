<template>
  <div class="page-container">
    <!-- ============ 页头 ============ -->
    <div class="page-header">
      <div>
        <h2 class="page-title">学校管理</h2>
        <p class="page-desc">维护平台入驻学校信息，包含 LOGO、区域与启用状态</p>
      </div>
      <div class="header-actions">
        <el-button :icon="Refresh" :loading="loading" @click="loadList">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openAdd">新增学校</el-button>
      </div>
    </div>

    <!-- ============ 筛选区 ============ -->
    <div class="filter-bar">
      <el-form :model="query" inline @submit.prevent>
        <el-form-item label="学校名称">
          <el-input
            v-model="query.name"
            placeholder="请输入学校名称"
            clearable
            style="width: 200px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>

        <el-form-item label="状态">
          <el-select
            v-model="query.status"
            placeholder="全部状态"
            clearable
            style="width: 150px"
          >
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
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
        empty-text="暂无学校数据"
        style="width: 100%"
      >
        <el-table-column prop="id" label="ID" width="80" />

        <el-table-column label="LOGO" width="100" align="center">
          <template #default="{ row }">
            <el-image
              v-if="row.logo"
              :src="row.logo"
              :preview-src-list="[row.logo]"
              :initial-index="0"
              preview-teleported
              fit="cover"
              class="logo-img"
            >
              <template #error>
                <div class="logo-empty"><el-icon><Picture /></el-icon></div>
              </template>
            </el-image>
            <div v-else class="logo-empty"><el-icon><Picture /></el-icon></div>
          </template>
        </el-table-column>

        <el-table-column prop="name" label="学校名称" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="school-name">{{ row.name || '-' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="省份" width="120">
          <template #default="{ row }">{{ row.province || '-' }}</template>
        </el-table-column>

        <el-table-column label="城市" width="120">
          <template #default="{ row }">{{ row.city || '-' }}</template>
        </el-table-column>

        <el-table-column label="地址" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ row.address || '-' }}</template>
        </el-table-column>

        <el-table-column label="排序" width="90" align="center">
          <template #default="{ row }">
            <span class="sort-num">{{ row.sort ?? 0 }}</span>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="light" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">
            <span class="text-secondary">{{ formatTime(row.createTime) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="150" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link :icon="Edit" @click="openEdit(row)">编辑</el-button>
            <el-button type="danger" link :icon="Delete" @click="handleDelete(row)">删除</el-button>
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

    <!-- ============ 新增 / 编辑弹窗 ============ -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑学校' : '新增学校'"
      width="600px"
      top="7vh"
      @closed="onDialogClosed"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="90px"
      >
        <el-form-item label="学校名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入学校名称" clearable />
        </el-form-item>

        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="省份" prop="province">
              <el-input v-model="form.province" placeholder="如：江苏省" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="城市" prop="city" label-width="70px">
              <el-input v-model="form.city" placeholder="如：南京市" clearable />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="地址" prop="address">
          <el-input
            v-model="form.address"
            type="textarea"
            :rows="2"
            placeholder="请输入详细地址"
          />
        </el-form-item>

        <el-form-item label="LOGO 地址" prop="logo">
          <el-input v-model="form.logo" placeholder="请输入 LOGO 图片 URL" clearable />
        </el-form-item>

        <el-form-item label="预览">
          <el-image
            v-if="form.logo"
            :src="form.logo"
            :preview-src-list="[form.logo]"
            :initial-index="0"
            preview-teleported
            fit="cover"
            class="logo-preview"
          >
            <template #error>
              <div class="logo-empty"><el-icon><Picture /></el-icon></div>
            </template>
          </el-image>
          <div v-else class="logo-preview logo-empty">
            <el-icon><Picture /></el-icon>
            <span class="empty-tip">暂无 LOGO</span>
          </div>
        </el-form-item>

        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="排序" prop="sort">
              <el-input-number v-model="form.sort" :min="0" :max="9999" controls-position="right" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status" label-width="70px">
              <el-radio-group v-model="form.status">
                <el-radio :value="1">启用</el-radio>
                <el-radio :value="0">禁用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitForm">
          {{ isEdit ? '保存修改' : '确定新增' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import dayjs from 'dayjs'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, Search, Refresh, RefreshLeft, Picture } from '@element-plus/icons-vue'
import { schoolApi } from '@/api'

/* ---------- 列表状态 ---------- */
const loading = ref(false)
const list = ref([])
const total = ref(0)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  name: '',
  status: null
})

/* ---------- 弹窗状态 ---------- */
const dialogVisible = ref(false)
const submitLoading = ref(false)
const formRef = ref()
const isEdit = computed(() => form.id !== null && form.id !== undefined)

const defaultForm = () => ({
  id: null,
  name: '',
  logo: '',
  province: '',
  city: '',
  address: '',
  sort: 0,
  status: 1
})

const form = reactive(defaultForm())

const rules = {
  name: [
    { required: true, message: '请输入学校名称', trigger: 'blur' },
    { min: 2, max: 50, message: '学校名称长度为 2 ~ 50 个字符', trigger: 'blur' }
  ],
  sort: [{ type: 'number', message: '排序必须为数字', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

/* ---------- 工具方法 ---------- */
function formatTime(time) {
  return time ? dayjs(time).format('YYYY-MM-DD HH:mm') : '-'
}

function resetForm() {
  Object.assign(form, defaultForm())
}

/* ---------- 数据加载 ---------- */
async function loadList() {
  loading.value = true
  try {
    const res = await schoolApi.list({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      name: query.name || undefined,
      status: query.status ?? undefined
    })
    list.value = Array.isArray(res?.records) ? res.records : []
    total.value = Number(res?.total || 0)
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
  query.name = ''
  query.status = null
  query.pageNum = 1
  loadList()
}

function handleSizeChange() {
  query.pageNum = 1
  loadList()
}

/* ---------- 新增 / 编辑 ---------- */
function openAdd() {
  resetForm()
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

function openEdit(row) {
  resetForm()
  Object.assign(form, {
    id: row.id,
    name: row.name || '',
    logo: row.logo || '',
    province: row.province || '',
    city: row.city || '',
    address: row.address || '',
    sort: Number(row.sort ?? 0),
    status: row.status ?? 1
  })
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

function onDialogClosed() {
  resetForm()
  formRef.value?.clearValidate()
}

async function submitForm() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  const payload = {
    name: form.name.trim(),
    logo: form.logo ? form.logo.trim() : '',
    province: form.province ? form.province.trim() : '',
    city: form.city ? form.city.trim() : '',
    address: form.address ? form.address.trim() : '',
    sort: Number(form.sort || 0),
    status: form.status
  }

  submitLoading.value = true
  try {
    if (isEdit.value) {
      await schoolApi.update({ ...payload, id: form.id })
      ElMessage.success('修改成功')
    } else {
      await schoolApi.add(payload)
      ElMessage.success('新增成功')
      query.pageNum = 1
    }
    dialogVisible.value = false
    loadList()
  } catch {
    // 请求层已提示错误
  } finally {
    submitLoading.value = false
  }
}

/* ---------- 删除 ---------- */
async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定要删除学校「${row.name || row.id}」吗？该操作不可恢复。`,
      '删除确认',
      {
        type: 'warning',
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        confirmButtonClass: 'el-button--danger'
      }
    )
  } catch {
    return
  }

  try {
    await schoolApi.remove(row.id)
    ElMessage.success('删除成功')
    // 删掉当前页最后一条时回退一页
    if (list.value.length === 1 && query.pageNum > 1) query.pageNum -= 1
    loadList()
  } catch {
    // 请求层已提示错误
  }
}

onMounted(() => {
  loadList()
})
</script>

<style lang="scss" scoped>
.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.school-name {
  font-weight: 500;
  color: var(--sc-text);
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

/* LOGO 缩略图 58 x 58 */
.logo-img {
  width: 58px;
  height: 58px;
  border-radius: 8px;
  border: 1px solid var(--sc-border);
  background: #f7f8fc;
  cursor: pointer;
  display: block;
  margin: 0 auto;
}

.logo-empty {
  width: 58px;
  height: 58px;
  border-radius: 8px;
  border: 1px dashed var(--sc-border);
  background: #f7f8fc;
  color: #c3c9d8;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  margin: 0 auto;
  gap: 6px;
}

/* 表单内 LOGO 预览 */
.logo-preview {
  width: 96px;
  height: 96px;
  border-radius: 10px;
  border: 1px solid var(--sc-border);
  background: #f7f8fc;
  overflow: hidden;
  flex-direction: column;

  &.logo-empty {
    width: 96px;
    height: 96px;
    font-size: 22px;
  }
}

.empty-tip {
  font-size: 12px;
  color: var(--sc-text-secondary);
}
</style>
