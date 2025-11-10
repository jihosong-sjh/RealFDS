import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';
import './styles/App.css';

/**
 * 진입점: React 애플리케이션 렌더링
 * - React.StrictMode로 개발 모드에서 추가 검사 수행
 */
ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
);
