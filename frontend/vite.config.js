import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'

// vite 配置文件
// uni-app 项目的 vite 配置，主要是引入 uni 插件
export default defineConfig({
  plugins: [uni()],
  // 配置路径别名，方便引用文件
  resolve: {
    alias: {
      '@': '/src'
    }
  },
  // 开发服务器配置
  server: {
    host: '0.0.0.0', // 监听所有地址（IPv4 + IPv6），方便 Nginx 代理
    port: 5173,
    open: true,
    // 代理配置：开发时把 /api 请求转发到后端 SpringBoot
    // 解决 H5 模式下的跨域问题
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        // 透传自定义请求头（学校ID、Token）
        configure: (proxy) => {
          proxy.on('proxyReq', (proxyReq, req) => {
            if (req.headers['school-id']) {
              proxyReq.setHeader('School-Id', req.headers['school-id'])
            }
            if (req.headers['authorization']) {
              proxyReq.setHeader('Authorization', req.headers['authorization'])
            }
          })
        }
      },
      // Knife4j 接口文档也代理
      '/doc.html': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/v3': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/webjars': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/swagger-resources': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
