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
  test: {
    // Vitest 설정: 단위 테스트 및 컴포넌트 테스트
    globals: true, // describe, it, expect 등을 전역으로 사용
    environment: 'jsdom', // React 컴포넌트 테스트를 위한 브라우저 환경 시뮬레이션
    setupFiles: './tests/setup.ts', // 테스트 초기화 파일
  },
})
