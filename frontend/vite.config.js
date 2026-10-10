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
  // 构建优化：分包、压缩、提升加载速度
  build: {
    // 单文件超过 500kb 才警告（默认 4kb 太严格）
    chunkSizeWarningLimit: 500,
    // 生产构建关闭 sourcemap（减小体积、加快构建）
    sourcemap: false,
    // 压缩配置：esbuild 比 terser 快 10 倍以上
    minify: 'esbuild',
    // esbuild 压缩增强选项
    esbuild: {
      // 移除 console/debugger（生产环境减小体积）
      drop: ['debugger'],
      pure: ['console.log', 'console.debug']
    },
    // 手动分包：把不常变的大依赖单独拆包，利用浏览器长效缓存
    rollupOptions: {
      output: {
        manualChunks: {
          // Vue 全家桶（基本不变，强缓存收益大）
          'vendor-vue': ['vue']
        }
      }
    },
    // 资源内联阈值：小于 4kb 的图片转 base64，减少请求数
    assetsInlineLimit: 4096
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
