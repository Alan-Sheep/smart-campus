import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import { fileURLToPath, URL } from 'node:url'
import fs from 'node:fs'
import os from 'node:os'
import path from 'node:path'

/**
 * 确保 esbuild 使用一个可写的临时目录。
 *
 * 背景：在部分 Windows 环境下，默认的系统临时目录
 * （如 C:\Users\<user>\AppData\Local\Temp）不允许当前进程删除文件，
 * 导致 vite build 在收尾阶段报
 *   [vite:esbuild-transpile] remove ...: Access is denied
 * 构建因此失败。这里显式把临时目录指向项目内的可写目录。
 */
const localTmp = path.resolve(process.cwd(), '.tmp-esbuild')
try {
  fs.mkdirSync(localTmp, { recursive: true })
  process.env.TEMP = localTmp
  process.env.TMP = localTmp
  // esbuild 直接读取 TMPDIR/TEMP，保持三者一致
  process.env.TMPDIR = localTmp
} catch {
  // 创建失败则退回系统默认值，不影响开发
}

// 管理后台 Vite 配置
export default defineConfig({
  plugins: [
    vue(),
    // Element Plus 按需自动引入（含组件与 API）
    AutoImport({
      imports: ['vue', 'vue-router', 'pinia'],
      resolvers: [ElementPlusResolver()],
      dts: false
    }),
    Components({
      resolvers: [ElementPlusResolver()],
      dts: false
    })
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    host: '0.0.0.0',
    port: 8081,
    open: false,
    // 开发环境把 /api 代理到后端，避免跨域
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  build: {
    outDir: 'dist',
    chunkSizeWarningLimit: 1500,
    rollupOptions: {
      output: {
        // 拆包，避免单个 chunk 过大影响加载
        manualChunks: {
          vue: ['vue', 'vue-router', 'pinia'],
          element: ['element-plus'],
          echarts: ['echarts']
        }
      }
    }
  }
})
