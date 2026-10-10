import { createSSRApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'

// 应用入口文件
// uni-app 使用 createSSRApp 创建应用，兼容小程序和H5
export function createApp() {
  const app = createSSRApp(App)
  // 使用 Pinia 状态管理
  const pinia = createPinia()
  app.use(pinia)
  return {
    app
  }
}

// ===== APP_VERSION_CHECK：版本变更检测（提示用户刷新，破除手机缓存） =====
// 每次会话记录版本号；发现与上次不同 → 提示用户刷新页面加载新版
;(function versionCheck() {
  try {
    const VERSION = (typeof window !== 'undefined' && window.__APP_VERSION) || 'unknown'
    const KEY = 'app_version_seen'
    const seen = uni.getStorageSync(KEY)
    if (seen && seen !== VERSION) {
      uni.showModal({
        title: '发现新版本',
        content: '为了获得最佳体验，请刷新页面',
        confirmText: '立即刷新',
        showCancel: false,
        success: () => {
          // #ifdef H5
          location.reload(true)
          // #endif
        }
      })
    }
    uni.setStorageSync(KEY, VERSION)
  } catch (e) { /* 非H5环境静默 */ }
})()
// ===== APP_VERSION_CHECK END =====
