import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api/users': {
        target: 'http://localhost:8081',
        changeOrigin: true
      },
      '/api/restaurants': {
        target: 'http://localhost:8082',
        changeOrigin: true
      },
      '/api/v1/carts': {
        target: 'http://localhost:8083',
        changeOrigin: true
      },
      '/api/orders': {
        target: 'http://localhost:8084',
        changeOrigin: true
      },
      '/api/payments': {
        target: 'http://localhost:8085',
        changeOrigin: true
      }
    }
  }
})
