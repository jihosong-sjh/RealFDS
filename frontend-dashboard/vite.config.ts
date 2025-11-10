import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Vite 설정: React 프로젝트를 위한 빌드 및 개발 서버 설정
// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 8083, // 개발 서버 포트
    host: true, // Docker 컨테이너 내에서 외부 접근 허용
  },
  preview: {
    port: 8083, // 프리뷰 서버 포트 (빌드 후 미리보기)
    host: true,
  },
})
