import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { DashboardLayout } from './components/dashboard/DashboardLayout';

/**
 * App 컴포넌트
 *
 * 실시간 시스템 대시보드 애플리케이션의 루트 컴포넌트입니다.
 * React Router를 사용하여 /dashboard 경로로 DashboardLayout을 렌더링합니다.
 *
 * 라우팅 구조:
 * - / → /dashboard로 리다이렉트
 * - /dashboard → DashboardLayout 컴포넌트 렌더링
 *
 * @see DashboardLayout - 실시간 대시보드 레이아웃
 */
const App: React.FC = () => {
  return (
    <Router>
      <Routes>
        {/* 루트 경로: /dashboard로 리다이렉트 */}
        <Route path="/" element={<Navigate to="/dashboard" replace />} />

        {/* 대시보드 경로: DashboardLayout 렌더링 */}
        <Route path="/dashboard" element={<DashboardLayout />} />

        {/* 404 Not Found: /dashboard로 리다이렉트 */}
        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </Router>
  );
};

export default App;
