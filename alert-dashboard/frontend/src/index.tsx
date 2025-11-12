import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';
import './index.css';

/**
 * 실시간 시스템 대시보드 애플리케이션 진입점
 *
 * React 18의 createRoot API를 사용하여 애플리케이션을 마운트합니다.
 * StrictMode를 사용하여 개발 중 잠재적인 문제를 감지합니다.
 *
 * @see App - 루트 컴포넌트
 */

const root = ReactDOM.createRoot(document.getElementById('root') as HTMLElement);

root.render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
