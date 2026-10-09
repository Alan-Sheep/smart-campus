<template>
  <el-container class="layout">
    <!-- ============ 侧边栏 ============ -->
    <el-aside :width="collapsed ? '64px' : '232px'" class="sidebar">
      <!-- Logo -->
      <div class="logo" :class="{ collapsed }">
        <div class="logo-mark">
          <el-icon :size="20"><Reading /></el-icon>
        </div>
        <transition name="fade">
          <span v-show="!collapsed" class="logo-text">智慧校园</span>
        </transition>
      </div>

      <!-- 菜单 -->
      <el-scrollbar class="menu-scroll">
        <el-menu
          :default-active="activeMenu"
          :collapse="collapsed"
          :collapse-transition="false"
          background-color="transparent"
          router
          unique-opened
        >
          <template v-for="item in menuRoutes" :key="item.path">
            <!-- 有子菜单 -->
            <el-sub-menu v-if="item.children && item.children.length" :index="item.path">
              <template #title>
                <el-icon><component :is="item.meta.icon" /></el-icon>
                <span>{{ item.meta.title }}</span>
              </template>
              <el-menu-item
                v-for="child in item.children"
                :key="child.path"
                :index="resolvePath(item.path, child.path)"
              >
                <span>{{ child.meta.title }}</span>
              </el-menu-item>
            </el-sub-menu>

            <!-- 单菜单 -->
            <el-menu-item v-else :index="item.path">
              <el-icon><component :is="item.meta.icon" /></el-icon>
              <template #title>{{ item.meta.title }}</template>
            </el-menu-item>
          </template>
        </el-menu>
      </el-scrollbar>
    </el-aside>

    <!-- ============ 主区域 ============ -->
    <el-container class="main-wrap">
      <!-- 顶栏 -->
      <el-header class="navbar" height="60px">
        <div class="navbar-left">
          <el-icon class="collapse-btn" @click="collapsed = !collapsed">
            <Fold v-if="!collapsed" />
            <Expand v-else />
          </el-icon>
          <!-- 面包屑 -->
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-for="(bc, i) in breadcrumbs" :key="i">
              {{ bc }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="navbar-right">
          <el-tooltip content="刷新页面" placement="bottom">
            <el-icon class="nav-action" @click="refreshPage"><Refresh /></el-icon>
          </el-tooltip>

          <!-- 用户下拉 -->
          <el-dropdown trigger="click" @command="onCommand">
            <div class="user-box">
              <el-avatar :size="32" :src="auth.avatar" class="user-avatar">
                {{ auth.nickname.charAt(0) }}
              </el-avatar>
              <span class="user-name">{{ auth.nickname }}</span>
              <el-icon class="user-arrow"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>个人中心
                </el-dropdown-item>
                <el-dropdown-item command="logout" divided>
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 内容区 -->
      <el-main class="content">
        <router-view v-slot="{ Component }">
          <transition name="fade-slide" mode="out-in">
            <component :is="Component" :key="route.fullPath" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import { useAuthStore } from '@/store/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const collapsed = ref(false)

// 从路由表生成菜单（过滤 hidden）
const menuRoutes = computed(() => {
  const root = router.options.routes.find((r) => r.path === '/')
  return (root?.children || []).filter((r) => !r.meta?.hidden)
})

/** 当前高亮菜单 */
const activeMenu = computed(() => route.path)

/** 面包屑：取匹配链上带 title 的层级 */
const breadcrumbs = computed(() =>
  route.matched
    .filter((r) => r.meta?.title && r.path !== '/')
    .map((r) => r.meta.title)
)

/** 拼接子路由完整路径 */
function resolvePath(parent, child) {
  if (child.startsWith('/')) return child
  return `${parent.replace(/\/$/, '')}/${child}`
}

/** 刷新当前页 */
function refreshPage() {
  window.location.reload()
}

/** 下拉菜单命令 */
async function onCommand(cmd) {
  if (cmd === 'profile') {
    router.push('/profile')
    return
  }
  if (cmd === 'logout') {
    try {
      await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
        type: 'warning',
        confirmButtonText: '退出',
        cancelButtonText: '取消'
      })
    } catch {
      return
    }
    await auth.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  }
}
</script>

<style lang="scss" scoped>
.layout {
  height: 100vh;
  overflow: hidden;
}

/* ============ 侧边栏 ============ */
.sidebar {
  background: linear-gradient(180deg, #241a4d 0%, #1b1439 100%);
  transition: width 0.25s ease;
  display: flex;
  flex-direction: column;
  overflow: hidden;

  /* 让 Element 菜单在深色底上更好看 */
  :deep(.el-menu) {
    border-right: none;
    padding: 8px 10px;
  }

  :deep(.el-menu-item),
  :deep(.el-sub-menu__title) {
    color: rgba(255, 255, 255, 0.72);
    border-radius: 10px;
    margin-bottom: 4px;
    height: 46px;
    line-height: 46px;

    &:hover {
      background: rgba(255, 255, 255, 0.08) !important;
      color: #fff;
    }
  }

  :deep(.el-menu-item.is-active) {
    background: var(--sc-gradient) !important;
    color: #fff !important;
    font-weight: 600;
    box-shadow: 0 6px 16px rgba(108, 78, 230, 0.4);
  }

  :deep(.el-sub-menu.is-active > .el-sub-menu__title) {
    color: #fff;
  }

  /* 子菜单缩进 */
  :deep(.el-menu--inline .el-menu-item) {
    padding-left: 46px !important;
    min-width: auto;
    font-size: 13px;
    height: 40px;
    line-height: 40px;
  }
}

/* Logo */
.logo {
  height: 60px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 18px;
  flex-shrink: 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.07);

  &.collapsed {
    justify-content: center;
    padding: 0;
  }
}

.logo-mark {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: var(--sc-gradient);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(108, 78, 230, 0.45);
}

.logo-text {
  color: #fff;
  font-size: 17px;
  font-weight: 600;
  letter-spacing: 1px;
  white-space: nowrap;
}

.menu-scroll {
  flex: 1;
  overflow-x: hidden;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* ============ 主区域 ============ */
.main-wrap {
  background: var(--sc-bg);
  overflow: hidden;
}

/* 顶栏 */
.navbar {
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  box-shadow: 0 1px 4px rgba(31, 36, 48, 0.05);
  z-index: 10;
}

.navbar-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.collapse-btn {
  font-size: 19px;
  cursor: pointer;
  color: var(--sc-text-regular);
  transition: color 0.2s;

  &:hover {
    color: var(--sc-primary);
  }
}

.navbar-right {
  display: flex;
  align-items: center;
  gap: 18px;
}

.nav-action {
  font-size: 18px;
  cursor: pointer;
  color: var(--sc-text-regular);
  transition: color 0.2s, transform 0.3s;

  &:hover {
    color: var(--sc-primary);
    transform: rotate(90deg);
  }
}

.user-box {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 5px 10px 5px 5px;
  border-radius: 20px;
  transition: background 0.2s;

  &:hover {
    background: var(--sc-primary-soft);
  }
}

.user-avatar {
  background: var(--sc-gradient);
  color: #fff;
  font-weight: 600;
}

.user-name {
  font-size: 14px;
  color: var(--sc-text);
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-arrow {
  font-size: 12px;
  color: var(--sc-text-secondary);
}

/* 内容区 */
.content {
  padding: 20px;
  overflow-y: auto;
  background: var(--sc-bg);
}
</style>
