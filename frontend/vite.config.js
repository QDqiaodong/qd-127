import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    host: '127.0.0.1',
    port: Number(process.env.FRONTEND_PORT || 8227),
    strictPort: true,
    proxy: {
      '/api': {
        target: `http://localhost:${process.env.BACKEND_PORT || 8327}`,
        changeOrigin: true
      }
    }
  },
  preview: {
    host: '127.0.0.1',
    port: Number(process.env.FRONTEND_PORT || 8227),
    strictPort: true
  }
})
