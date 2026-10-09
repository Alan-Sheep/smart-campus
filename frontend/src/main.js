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
