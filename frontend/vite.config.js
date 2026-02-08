import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  define: {
    global: 'globalThis',
  },
  // Use environment variable for base path (empty for Railway)
  // In vite.config.js, use process.env, not import.meta.env
  base: process.env.VITE_BASE_PATH || '/',
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: process.env.VITE_API_BASE_URL || 'http://localhost:8080',
        changeOrigin: true,
      },
      '/ws': {
        target: process.env.VITE_WS_BASE_URL || 'ws://localhost:8080',
        ws: true,
        changeOrigin: true,
      },
    },
  },
})

