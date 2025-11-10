import { useWebSocket } from './hooks/useWebSocket';
import { Header } from './components/Header';
import { ConnectionStatus } from './components/ConnectionStatus';
import { AlertList } from './components/AlertList';
import './styles/App.css';

/**
 * App 컴포넌트: 루트 컴포넌트
 * - WebSocket 연결 관리
 * - Header, ConnectionStatus, AlertList 컴포넌트 배치
 */
function App() {
  // WebSocket URL 환경 변수 또는 기본값 사용
  const websocketUrl = import.meta.env.VITE_WEBSOCKET_URL || 'ws://localhost:8082/ws/alerts';

  // useWebSocket hook으로 WebSocket 연결 및 알림 수신
  const { alerts, connectionStatus } = useWebSocket(websocketUrl);

  return (
    <div className="app-container">
      {/* 헤더 */}
      <Header />

      {/* 연결 상태 표시 */}
      <ConnectionStatus status={connectionStatus} />

      {/* 알림 목록 */}
      <AlertList alerts={alerts} />
    </div>
  );
}

export default App;
