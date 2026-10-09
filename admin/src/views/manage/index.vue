<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-title">管理员管理</h2>
        <p class="page-desc">管理后台账号，可新增、编辑、重置密码与删除</p>
      </div>
      <div>
        <el-button :icon="Refresh" :loading="loading" @click="loadList">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openAdd">新增管理员</el-button>
      </div>
    </div>

    <!-- 筛选 -->
    <div class="filter-bar">
      <el-form inline>
        <el-form-item label="用户名">
          <el-input v-model="query.username" placeholder="请输入用户名" clearable style="width: 180px" @keyup.enter="onSearch" />
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
        <el-table-column label="头像" width="80">
          <template #default="{ row }">
            <el-avatar :size="38" :src="row.avatar" class="adm-avatar">
              {{ (row.nickname || row.username || 'A').charAt(0) }}
            </el-avatar>
          </template>
        </el-table-column>
        <el-table-column prop="username" label="用户名" min-width="130" />
        <el-table-column prop="nickname" label="昵称" min-width="130">
          <template #default="{ row }">{{ row.nickname || '-' }}</template>
        </el-table-column>
        <el-table-column label="所属学校" min-width="130">
          <template #default="{ row }">{{ schoolName(row.schoolId) }}</template>
        </el-table-column>
        <el-table-column label="角色" width="110">
          <template #default="{ row }">
            <el-tag :type="Number(row.role) === 2 ? 'danger' : 'primary'" effect="light">
              {{ Number(row.role) === 2 ? '超级管理员' : '普通管理员' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="light">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="最后登录" width="160">
          <template #default="{ row }">{{ fmt(row.lastLoginTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="210" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="warning" @click="onResetPwd(row)">重置密码</el-button>
            <el-button
              link
              type="danger"
              :disabled="row.id === auth.adminInfo?.id"
              @click="onRemove(row)"
            >删除</el-button>
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

    <!-- 新增/编辑 -->
    <el-dialog v-model="formVisible" :title="isEdit ? '编辑管理员' : '新增管理员'" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="登录用户名" />
        </el-form-item>
        <el-form-item v-if="!isEdit" label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="至少6位" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="显示名称" />
        </el-form-item>
        <el-form-item label="所属学校">
          <el-select v-model="form.schoolId" placeholder="不限制（全局）" clearable style="width: 100%">
            <el-option v-for="s in schools" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="角色">
          <el-radio-group v-model="form.role">
            <el-radio :value="1">普通管理员</el-radio>
            <el-radio :value="2">超级管理员</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="onSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Plus, Search, RefreshLeft } from '@element-plus/icons-vue'
import { manageApi, schoolApi } from '@/api'
import { useAuthStore } from '@/store/auth'
import dayjs from 'dayjs'

const auth = useAuthStore()

const loading = ref(false)
const submitting = ref(false)
const list = ref([])
const total = ref(0)
const schools = ref([])

const formVisible = ref(false)
const isEdit = ref(false)
const formRef = ref()

const query = reactive({ pageNum: 1, pageSize: 10, username: '', schoolId: null })

const form = reactive({
  id: null,
  username: '',
  password: '',
  nickname: '',
  schoolId: null,
  role: 1,
  status: 1
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }]
}

const fmt = (t) => (t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '-')
const schoolName = (id) => {
  if (!id) return '全局'
  const s = schools.value.find((x) => x.id === id)
  return s ? s.name : `学校#${id}`
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
    const data = await manageApi.list({ ...query })
    list.value = data?.records || []
    total.value = data?.total || 0
  } catch {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const onSearch = () => {
  query.pageNum = 1
  loadList()
}

function onReset() {
  query.username = ''
  query.schoolId = null
  query.pageNum = 1
  loadList()
}

function resetForm() {
  form.id = null
  form.username = ''
  form.password = ''
  form.nickname = ''
  form.schoolId = null
  form.role = 1
  form.status = 1
}

function openAdd() {
  resetForm()
  isEdit.value = false
  formVisible.value = true
  formRef.value?.clearValidate()
}

function openEdit(row) {
  resetForm()
  Object.assign(form, {
    id: row.id,
    username: row.username,
    nickname: row.nickname,
    schoolId: row.schoolId,
    role: row.role ?? 1,
    status: row.status ?? 1
  })
  isEdit.value = true
  formVisible.value = true
  formRef.value?.clearValidate()
}

async function onSubmit() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    if (isEdit.value) {
      // 后端 update 不接受密码字段，密码走「重置密码」
      await manageApi.update({
        id: form.id,
        username: form.username,
        nickname: form.nickname,
        schoolId: form.schoolId,
        role: form.role,
        status: form.status
      })
      ElMessage.success('修改成功')
    } else {
      await manageApi.add({
        username: form.username,
        password: form.password,
        nickname: form.nickname,
        schoolId: form.schoolId,
        role: form.role,
        status: form.status
      })
      ElMessage.success('新增成功')
    }
    formVisible.value = false
    loadList()
  } catch {
    /* 已提示 */
  } finally {
    submitting.value = false
  }
}

/** 重置密码 */
async function onResetPwd(row) {
  let pwd = ''
  try {
    const { value } = await ElMessageBox.prompt(
      `为「${row.username}」设置新密码（至少6位）`,
      '重置密码',
      {
        confirmButtonText: '确定重置',
        cancelButtonText: '取消',
        inputType: 'password',
        inputPlaceholder: '请输入新密码',
        inputValidator: (v) => {
          if (!v || v.length < 6) return '密码长度不能少于6位'
          return true
        }
      }
    )
    pwd = value
  } catch {
    return
  }
  try {
    await manageApi.resetPassword(row.id, { password: pwd })
    ElMessage.success('密码已重置')
  } catch {
    /* 已提示 */
  }
}

async function onRemove(row) {
  try {
    await ElMessageBox.confirm(`确定要删除管理员「${row.username}」吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '确定删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  try {
    await manageApi.remove(row.id)
    ElMessage.success('已删除')
    // 删除当前页最后一条时回退一页
    if (list.value.length === 1 && query.pageNum > 1) {
      query.pageNum -= 1
    }
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
.adm-avatar {
  background: var(--sc-gradient);
  color: #fff;
  font-weight: 600;
}
</style>
