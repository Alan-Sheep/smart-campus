<template>
  <div class="login-page">
    <!-- 左侧品牌区 -->
    <div class="brand-side">
      <div class="brand-inner">
        <div class="brand-logo">
          <el-icon :size="30"><Reading /></el-icon>
        </div>
        <h1 class="brand-title">智慧校园</h1>
        <p class="brand-sub">Smart Campus 管理后台</p>

        <ul class="brand-features">
          <li><el-icon><DataLine /></el-icon> 数据概览与趋势分析</li>
          <li><el-icon><DocumentChecked /></el-icon> 内容审核与违规处理</li>
          <li><el-icon><List /></el-icon> 全业务订单统一管理</li>
          <li><el-icon><User /></el-icon> 用户与学校运营管理</li>
        </ul>
      </div>
      <!-- 装饰光斑 -->
      <span class="blob blob-1"></span>
      <span class="blob blob-2"></span>
      <span class="blob blob-3"></span>
    </div>

    <!-- 右侧表单区 -->
    <div class="form-side">
      <div class="form-box">
        <h2 class="form-title">欢迎回来 👋</h2>
        <p class="form-desc">请登录您的管理员账号</p>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          size="large"
          @keyup.enter="onSubmit"
        >
          <el-form-item prop="username">
            <el-input
              v-model="form.username"
              placeholder="请输入用户名"
              :prefix-icon="User"
              clearable
            />
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              :prefix-icon="Lock"
              show-password
            />
          </el-form-item>

          <el-form-item>
            <el-button
              type="primary"
              class="submit-btn"
              :loading="loading"
              @click="onSubmit"
            >
              {{ loading ? '登录中...' : '登 录' }}
            </el-button>
          </el-form-item>
        </el-form>

        <div class="demo-tip">
          <el-icon><InfoFilled /></el-icon>
          <span>默认账号：<b>admin</b> ／ 密码：<b>admin123</b></span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useAuthStore } from '@/store/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const formRef = ref()
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ]
}

async function onSubmit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    await auth.login({ username: form.username.trim(), password: form.password })
    ElMessage.success('登录成功')
    // 支持登录后跳回原页面
    const redirect = route.query.redirect || '/dashboard'
    router.push(redirect)
  } catch (e) {
    // 请求层已提示错误
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.login-page {
  height: 100vh;
  display: flex;
  overflow: hidden;
}

/* ============ 左侧品牌区 ============ */
.brand-side {
  flex: 1.15;
  background: linear-gradient(135deg, #2b1d5e 0%, #4c31b8 55%, #6c4ee6 100%);
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;

  @media (max-width: 900px) {
    display: none;
  }
}

.brand-inner {
  position: relative;
  z-index: 2;
  color: #fff;
  padding: 0 60px;
  max-width: 520px;
}

.brand-logo {
  width: 64px;
  height: 64px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.16);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.25);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 26px;
}

.brand-title {
  font-size: 40px;
  font-weight: 700;
  margin: 0 0 10px;
  letter-spacing: 3px;
}

.brand-sub {
  font-size: 15px;
  color: rgba(255, 255, 255, 0.7);
  margin: 0 0 44px;
  letter-spacing: 1px;
}

.brand-features {
  list-style: none;
  padding: 0;
  margin: 0;

  li {
    display: flex;
    align-items: center;
    gap: 12px;
    font-size: 15px;
    color: rgba(255, 255, 255, 0.88);
    padding: 13px 0;
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);

    &:last-child {
      border-bottom: none;
    }

    .el-icon {
      color: #b9a6ff;
      font-size: 18px;
    }
  }
}

/* 装饰光斑 */
.blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(70px);
  opacity: 0.5;
}
.blob-1 {
  width: 340px;
  height: 340px;
  background: #8b5cf6;
  top: -90px;
  right: -70px;
}
.blob-2 {
  width: 260px;
  height: 260px;
  background: #4f8cff;
  bottom: -70px;
  left: -60px;
}
.blob-3 {
  width: 180px;
  height: 180px;
  background: #d946ef;
  bottom: 32%;
  right: 16%;
  opacity: 0.35;
}

/* ============ 右侧表单区 ============ */
.form-side {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  padding: 24px;
}

.form-box {
  width: 100%;
  max-width: 360px;
}

.form-title {
  font-size: 26px;
  font-weight: 700;
  margin: 0 0 8px;
  color: var(--sc-text);
}

.form-desc {
  font-size: 14px;
  color: var(--sc-text-secondary);
  margin: 0 0 32px;
}

.submit-btn {
  width: 100%;
  height: 46px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 3px;
  background: var(--sc-gradient);
  border: none;
  box-shadow: 0 8px 20px rgba(108, 78, 230, 0.3);
  transition: transform 0.2s, box-shadow 0.2s;

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 10px 24px rgba(108, 78, 230, 0.4);
  }
}

.demo-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 22px;
  padding: 11px 14px;
  border-radius: 10px;
  background: var(--sc-primary-soft);
  color: #6a5acd;
  font-size: 13px;

  b {
    color: var(--sc-primary);
  }
}
</style>
