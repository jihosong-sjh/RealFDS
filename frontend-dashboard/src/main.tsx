import React from 'react';
import ReactDOM from 'react-dom/client';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import App from './App';
import './styles/App.css';

/**
 * React Query Client 생성
 * - 서버 상태 관리를 위한 QueryClient 인스턴스
 * - 기본 설정: 5분 캐시, 3회 재시도
 */
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 60 * 5, // 5분
      gcTime: 1000 * 60 * 10, // 10분 (이전 cacheTime)
      retry: 3,
      refetchOnWindowFocus: false,
    },
  },
});

/**
 * 진입점: React 애플리케이션 렌더링
 * - React.StrictMode로 개발 모드에서 추가 검사 수행
 * - QueryClientProvider로 React Query 제공
 */
ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <QueryClientProvider client={queryClient}>
      <App />
    </QueryClientProvider>
  </React.StrictMode>,
);
