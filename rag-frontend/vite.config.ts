import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
    },
  },
  server: {
    port: 5173,
    proxy: {
      // 开发时代理到后端，避免跨域
      '/api': {
        target: 'http://localhost:8084',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '/api'),
        // 延长代理超时，LLM 调用可能较慢
        timeout: 180000,
        // SSE 流式传输需要的配置
        configure: (proxy) => {
          proxy.on('proxyRes', (proxyRes) => {
            // 对 SSE 响应禁用缓冲
            if (proxyRes.headers['content-type']?.includes('text/event-stream')) {
              proxyRes.headers['Cache-Control'] = 'no-cache'
              proxyRes.headers['Connection'] = 'keep-alive'
            }
          })
        },
      },
    },
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (!id.includes('node_modules')) return
          if (
            id.includes('vue') ||
            id.includes('pinia') ||
            id.includes('vue-router') ||
            id.includes('element-plus') ||
            id.includes('@element-plus/icons-vue')
          ) return 'vendor-core'
          if (id.includes('axios') || id.includes('marked')) return 'vendor-utils'
          return 'vendor'
        },
      },
    },
  },
})
