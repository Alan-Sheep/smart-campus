<template>
  <div class="page-container">
    <!-- ============ 页面头部 ============ -->
    <div class="page-header">
      <div>
        <h2 class="page-title">轮播图管理</h2>
        <p class="page-desc">管理小程序首页轮播图，支持跳转配置与排序调整</p>
      </div>
      <div class="header-actions">
        <el-button type="primary" :icon="Plus" @click="openDialog()">新增轮播图</el-button>
      </div>
    </div>

    <!-- ============ 筛选区 ============ -->
    <div class="filter-bar">
      <el-form :model="query" inline @submit.prevent>
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
        <el-table-column prop="sort" label="排序" width="80" align="center">
          <template #default="{ row }">
            <span class="sort-num">{{ row.sort ?? 0 }}</span>
          </template>
        </el-table-column>

        <el-table-column label="图片" width="150">
          <template #default="{ row }">
            <el-image
              v-if="row.image"
              :src="row.image"
              :preview-src-list="[row.image]"
              :initial-index="0"
              preview-teleported
              hide-on-click-modal
              fit="cover"
              class="banner-thumb"
            >
              <template #error>
                <div class="thumb-error">加载失败</div>
              </template>
            </el-image>
            <span v-else class="text-secondary">-</span>
          </template>
        </el-table-column>

        <el-table-column prop="title" label="标题" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="title-text">{{ row.title || '-' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="跳转类型" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="linkTypeTag(row.linkType)" effect="light" size="small">
              {{ linkTypeText(row.linkType) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="跳转地址" min-width="180">
          <template #default="{ row }">
            <el-tooltip
              v-if="row.linkUrl"
              :content="row.linkUrl"
              placement="top"
              :show-after="200"
            >
              <span class="text-ellipsis link-url">{{ row.linkUrl }}</span>
            </el-tooltip>
            <span v-else class="text-secondary">-</span>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag
              :type="row.status === 1 ? 'success' : 'info'"
              effect="light"
              size="small"
              class="status-tag"
              :title="row.status === 1 ? '点击禁用' : '点击启用'"
              @click="toggleStatus(row)"
            >
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="创建时间" width="165">
          <template #default="{ row }">
            <span class="text-secondary">{{ formatTime(row.createTime) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="250" fixed="right" align="center">
          <template #default="{ row, $index }">
            <el-button type="primary" link :icon="Edit" @click="openDialog(row)">编辑</el-button>
            <el-button
              type="primary"
              link
              :icon="Top"
              :disabled="$index === 0"
              @click="move($index, -1)"
            >
              上移
            </el-button>
            <el-button
              type="primary"
              link
              :icon="Bottom"
              :disabled="$index === list.length - 1"
              @click="move($index, 1)"
            >
              下移
            </el-button>
            <el-button type="danger" link :icon="Delete" @click="handleDelete(row)">
              删除
            </el-button>
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

    <!-- ============ 新增 / 编辑弹窗 ============ -->
    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '编辑轮播图' : '新增轮播图'"
      width="640px"
      top="8vh"
      destroy-on-close
      @closed="handleDialogClosed"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入轮播图标题" maxlength="50" show-word-limit clearable />
        </el-form-item>

        <el-form-item label="图片URL" prop="image">
          <el-input
            v-model="form.image"
            placeholder="请输入图片地址，如 /upload/banner/xxx.png"
            clearable
          />
          <div class="preview-box">
            <el-image v-if="form.image" :src="form.image" fit="cover" class="preview-img">
              <template #error>
                <div class="preview-error">图片加载失败</div>
              </template>
            </el-image>
            <div v-else class="preview-empty">图片预览区</div>
          </div>
        </el-form-item>

        <el-form-item label="所属学校" prop="schoolId">
          <el-select v-model="form.schoolId" placeholder="请选择学校" style="width: 100%" clearable>
            <el-option v-for="s in schools" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="跳转类型" prop="linkType">
          <el-select v-model="form.linkType" placeholder="请选择跳转类型" style="width: 100%">
            <el-option
              v-for="t in LINK_TYPES"
              :key="t.value"
              :label="t.label"
              :value="t.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item v-if="form.linkType !== 0" label="跳转地址" prop="linkUrl">
          <el-input
            v-model="form.linkUrl"
            :placeholder="form.linkType === 2 ? '请输入外部链接，如 https://xxx.com' : '请输入内部页面路径，如 /pages/goods/index'"
            clearable
          />
        </el-form-item>

        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="form.sort" :min="0" :max="9999" :step="1" controls-position="right" />
          <span class="form-tip">数值越小越靠前</span>
        </el-form-item>

        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
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
import { Plus, Search, RefreshLeft, Edit, Delete, Top, Bottom } from '@element-plus/icons-vue'
import { bannerApi, schoolApi } from '@/api'

/* ---------- 常量 ---------- */
const LINK_TYPES = [
  { value: 0, label: '不跳转', tag: 'info' },
  { value: 1, label: '内部页面', tag: 'primary' },
  { value: 2, label: '外部链接', tag: 'warning' }
]

/* ---------- 列表状态 ---------- */
const loading = ref(false)
const submitting = ref(false)
const list = ref([])
const total = ref(0)
const schools = ref([])

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  schoolId: null
})

/* ---------- 弹窗状态 ---------- */
const dialogVisible = ref(false)
const formRef = ref(null)
const form = reactive({
  id: null,
  title: '',
  image: '',
  schoolId: null,
  linkType: 0,
  linkUrl: '',
  sort: 0,
  status: 1
})

const rules = {
  title: [
    { required: true, message: '请输入轮播图标题', trigger: 'blur' },
    { min: 1, max: 50, message: '标题长度需在 50 个字符以内', trigger: 'blur' }
  ],
  image: [{ required: true, message: '请输入图片URL', trigger: 'blur' }],
  schoolId: [{ required: true, message: '请选择所属学校', trigger: 'change' }],
  linkType: [{ required: true, message: '请选择跳转类型', trigger: 'change' }],
  linkUrl: [
    {
      validator(rule, value, callback) {
        if (form.linkType !== 0 && !value) {
          callback(new Error('请填写跳转地址'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ]
}

/* ---------- 工具函数 ---------- */
function formatTime(value) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-'
}

function linkTypeText(type) {
  const hit = LINK_TYPES.find((t) => t.value === Number(type))
  return hit ? hit.label : '未知'
}

function linkTypeTag(type) {
  const hit = LINK_TYPES.find((t) => t.value === Number(type))
  return hit ? hit.tag : 'info'
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
    const data = await bannerApi.list({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
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
  query.schoolId = null
  query.pageNum = 1
  query.pageSize = 10
  loadList()
}

function handleSizeChange() {
  query.pageNum = 1
  loadList()
}

/* ---------- 新增 / 编辑 ---------- */
function openDialog(row) {
  if (row) {
    Object.assign(form, {
      id: row.id,
      title: row.title || '',
      image: row.image || '',
      schoolId: row.schoolId ?? null,
      linkType: row.linkType ?? 0,
      linkUrl: row.linkUrl || '',
      sort: row.sort ?? 0,
      status: row.status ?? 1
    })
  } else {
    Object.assign(form, {
      id: null,
      title: '',
      image: '',
      schoolId: query.schoolId ?? null,
      linkType: 0,
      linkUrl: '',
      sort: nextSort(),
      status: 1
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
    image: form.image,
    schoolId: form.schoolId,
    linkType: form.linkType,
    linkUrl: form.linkType === 0 ? '' : form.linkUrl,
    sort: form.sort ?? 0,
    status: form.status
  }

  try {
    if (form.id) {
      await bannerApi.update(payload)
      ElMessage.success('修改成功')
    } else {
      await bannerApi.add(payload)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadList()
  } finally {
    submitting.value = false
  }
}

/* ---------- 状态切换 ---------- */
async function toggleStatus(row) {
  const next = row.status === 1 ? 0 : 1
  const action = next === 1 ? '启用' : '禁用'
  try {
    await ElMessageBox.confirm(`确定要${action}轮播图「${row.title || row.id}」吗？`, '提示', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  await bannerApi.updateStatus(row.id, next)
  ElMessage.success(`${action}成功`)
  loadList()
}

/* ---------- 排序上移 / 下移 ---------- */
async function move(index, direction) {
  const target = index + direction
  if (target < 0 || target >= list.value.length) return

  const rows = [...list.value]
  const current = rows[index]
  const adjacent = rows[target]

  // 交换两行的 sort 值
  const currentSort = Number(current.sort) || 0
  const adjacentSort = Number(adjacent.sort) || 0
  current.sort = adjacentSort
  adjacent.sort = currentSort

  // 交换数组位置，得到新的展示顺序
  rows[index] = adjacent
  rows[target] = current
  list.value = rows

  const ids = rows.map((r) => r.id)
  try {
    loading.value = true
    // 提交交换后的完整顺序，由后端按数组下标重排 sort
    await bannerApi.sort(ids)
    ElMessage.success('排序已更新')
  } finally {
    loading.value = false
    loadList()
  }
}

/* ---------- 删除 ---------- */
async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定要删除轮播图「${row.title || row.id}」吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '确定删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  await bannerApi.remove(row.id)
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

.title-text {
  font-weight: 500;
  color: var(--sc-text);
}

.banner-thumb {
  width: 120px;
  height: 60px;
  border-radius: 8px;
  border: 1px solid var(--sc-border);
  background: #f7f8fc;
  display: block;
}

.thumb-error,
.preview-error {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: var(--sc-text-secondary);
  background: #f7f8fc;
}

.link-url {
  display: inline-block;
  max-width: 100%;
  vertical-align: middle;
  color: var(--sc-text-regular);
}

.status-tag {
  cursor: pointer;
  user-select: none;
}

/* ---------- 弹窗 ---------- */
.preview-box {
  margin-top: 10px;
  width: 100%;
}

.preview-img {
  width: 260px;
  height: 130px;
  border-radius: 10px;
  border: 1px solid var(--sc-border);
  background: #f7f8fc;
  display: block;
}

.preview-empty {
  width: 260px;
  height: 130px;
  border-radius: 10px;
  border: 1px dashed #dcdfe8;
  background: #fafbfe;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: var(--sc-text-secondary);
}

.form-tip {
  margin-left: 10px;
  font-size: 12px;
  color: var(--sc-text-secondary);
}

:deep(.el-form-item__content) {
  flex-wrap: wrap;
}
</style>
