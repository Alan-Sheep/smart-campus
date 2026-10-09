<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-title">个人中心</h2>
        <p class="page-desc">查看账号信息并修改登录密码</p>
      </div>
    </div>

    <el-row :gutter="16">
      <!-- 左：账号概览 -->
      <el-col :xs="24" :md="9">
        <div class="sc-card profile-card">
          <el-avatar :size="78" :src="auth.avatar" class="big-avatar">
            {{ auth.nickname.charAt(0) }}
          </el-avatar>
          <h3 class="nick">{{ auth.nickname }}</h3>
          <el-tag :type="isSuper ? 'danger' : 'primary'" effect="light" class="role-tag">
            {{ isSuper ? '超级管理员' : '普通管理员' }}
          </el-tag>

          <div class="info-list">
            <div class="info-row">
              <span class="label">用户名</span>
              <span class="value">{{ info.username || '-' }}</span>
            </div>
            <div class="info-row">
              <span class="label">所属学校</span>
              <span class="value">{{ info.schoolId || '全局' }}</span>
            </div>
            <div class="info-row">
              <span class="label">账号状态</span>
              <el-tag :type="info.status === 1 ? 'success' : 'info'" size="small" effect="light">
                {{ info.status === 1 ? '正常' : '禁用' }}
              </el-tag>
            </div>
            <div class="info-row">
              <span class="label">最后登录</span>
              <span class="value">{{ fmt(info.lastLoginTime) }}</span>
            </div>
            <div class="info-row">
              <span class="label">创建时间</span>
              <span class="value">{{ fmt(info.createTime) }}</span>
            </div>
          </div>
        </div>
      </el-col>

      <!-- 右：修改密码 -->
      <el-col :xs="24" :md="15">
        <div class="sc-card">
          <div class="card-head">
            <h3>修改密码</h3>
            <span class="card-sub">为保障账号安全，建议定期更换密码</span>
          </div>

          <el-form
            ref="pwdRef"
            :model="pwdForm"
            :rules="pwdRules"
            label-width="100px"
            style="max-width: 460px; margin-top: 8px"
          >
            <el-form-item label="原密码" prop="oldPassword">
              <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入当前密码" />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="至少6位" />
            </el-form-item>
            <el-form-item label="确认新密码" prop="confirmPassword">
              <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="submitting" @click="onSubmit">保存修改</el-button>
              <el-button @click="onResetForm">重置</el-button>
            </el-form-item>
          </el-form>

          <el-alert
            type="info"
            :closable="false"
            show-icon
            title="提示：修改密码后当前登录仍然有效，下次登录请使用新密码。"
          />
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { authApi } from '@/api'
import { useAuthStore } from '@/store/auth'
import dayjs from 'dayjs'

const auth = useAuthStore()
const info = ref({})
const pwdRef = ref()
const submitting = ref(false)

/**
 * 是否超级管理员
 * 注意：admin.role 是 TINYINT（1=普通管理员 2=超级管理员），不是字符串，
 * 早期误写成 role === 'super' 会导致始终判为普通管理员。
 */
const isSuper = computed(() => Number(auth.role) === 2)

const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validateConfirm = (rule, value, callback) => {
  if (value !== pwdForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' }
  ]
}

const fmt = (t) => (t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '-')
async function loadInfo() {
  try {
    info.value = await authApi.info()
    // 同步到 store，保证顶栏头像/昵称最新
    if (info.value) {
      auth.adminInfo = info.value
      localStorage.setItem('admin_info', JSON.stringify(info.value))
    }
  } catch {
    info.value = auth.adminInfo || {}
  }
}

async function onSubmit() {
  try {
    await pwdRef.value.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    await authApi.updatePassword({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword
    })
    ElMessage.success('密码修改成功')
    onResetForm()
  } catch {
    /* 已提示 */
  } finally {
    submitting.value = false
  }
}

function onResetForm() {
  pwdForm.oldPassword = ''
  pwdForm.newPassword = ''
  pwdForm.confirmPassword = ''
  pwdRef.value?.clearValidate()
}

onMounted(loadInfo)
</script>

<style lang="scss" scoped>
.profile-card {
  text-align: center;
  padding-top: 30px;
}

.big-avatar {
  background: var(--sc-gradient);
  color: #fff;
  font-size: 30px;
  font-weight: 600;
  box-shadow: 0 8px 22px rgba(108, 78, 230, 0.3);
}

.nick {
  margin: 14px 0 8px;
  font-size: 18px;
  font-weight: 600;
}

.role-tag {
  margin-bottom: 20px;
}

.info-list {
  text-align: left;
  border-top: 1px solid var(--sc-border);
  padding-top: 8px;
}

.info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 11px 4px;
  border-bottom: 1px dashed #f0f2f7;

  &:last-child {
    border-bottom: none;
  }

  .label {
    color: var(--sc-text-secondary);
    font-size: 13px;
  }

  .value {
    color: var(--sc-text);
    font-size: 13px;
    font-weight: 500;
  }
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  flex-wrap: wrap;
  gap: 8px;

  h3 {
    font-size: 15px;
    font-weight: 600;
    margin: 0;
  }
}

.card-sub {
  font-size: 12px;
  color: var(--sc-text-secondary);
}
</style>
