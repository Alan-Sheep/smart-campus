import { createRouter, createWebHistory } from 'vue-router'

/**
 * 路由表
 * meta.title  页面标题（面包屑/浏览器标题）
 * meta.icon   菜单图标
 * meta.hidden 是否在侧边菜单隐藏
 */
const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', hidden: true }
  },
  {
    path: '/',
    component: () => import('@/layout/index.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '数据概览', icon: 'DataLine' }
      },
      {
        path: 'user',
        name: 'UserManage',
        component: () => import('@/views/user/index.vue'),
        meta: { title: '用户管理', icon: 'User' }
      },
      {
        path: 'school',
        name: 'SchoolManage',
        component: () => import('@/views/school/index.vue'),
        meta: { title: '学校管理', icon: 'School' }
      },
      {
        path: 'banner',
        name: 'BannerManage',
        component: () => import('@/views/banner/index.vue'),
        meta: { title: '轮播图管理', icon: 'Picture' }
      },
      {
        path: 'notice',
        name: 'NoticeManage',
        component: () => import('@/views/notice/index.vue'),
        meta: { title: '公告管理', icon: 'Bell' }
      },
      {
        path: 'audit',
        name: 'Audit',
        redirect: '/audit/goods',
        meta: { title: '内容审核', icon: 'DocumentChecked' },
        children: [
          {
            path: 'goods',
            name: 'AuditGoods',
            component: () => import('@/views/audit/goods.vue'),
            meta: { title: '商品审核' }
          },
          {
            path: 'post',
            name: 'AuditPost',
            component: () => import('@/views/audit/post.vue'),
            meta: { title: '帖子审核' }
          },
          {
            path: 'activity',
            name: 'AuditActivity',
            component: () => import('@/views/audit/activity.vue'),
            meta: { title: '活动审核' }
          },
          {
            path: 'lostfound',
            name: 'AuditLostFound',
            component: () => import('@/views/audit/lostfound.vue'),
            meta: { title: '失物招领审核' }
          }
        ]
      },
      {
        path: 'order',
        name: 'Order',
        redirect: '/order/express',
        meta: { title: '订单管理', icon: 'List' },
        children: [
          {
            path: 'express',
            name: 'OrderExpress',
            component: () => import('@/views/order/express.vue'),
            meta: { title: '快递代取' }
          },
          {
            path: 'errand',
            name: 'OrderErrand',
            component: () => import('@/views/order/errand.vue'),
            meta: { title: '校园跑腿' }
          },
          {
            path: 'print',
            name: 'OrderPrint',
            component: () => import('@/views/order/print.vue'),
            meta: { title: '打印服务' }
          },
          {
            path: 'substitute',
            name: 'OrderSubstitute',
            component: () => import('@/views/order/substitute.vue'),
            meta: { title: '代课服务' }
          }
        ]
      },
      {
        path: 'feedback',
        name: 'Feedback',
        component: () => import('@/views/feedback/index.vue'),
        meta: { title: '意见反馈', icon: 'ChatDotRound' }
      },
      {
        path: 'manage',
        name: 'Manage',
        component: () => import('@/views/manage/index.vue'),
        meta: { title: '管理员管理', icon: 'Avatar' }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/profile/index.vue'),
        meta: { title: '个人中心', icon: 'Setting', hidden: true }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/dashboard',
    meta: { hidden: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

// 全局前置守卫：未登录跳转登录页
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('admin_token')
  document.title = to.meta?.title
    ? `${to.meta.title} · 智慧校园管理后台`
    : '智慧校园管理后台'

  if (to.path === '/login') {
    // 已登录访问登录页 → 直接进首页
    if (token) return next('/dashboard')
    return next()
  }

  if (!token) {
    return next({ path: '/login', query: { redirect: to.fullPath } })
  }
  next()
})

export default router
