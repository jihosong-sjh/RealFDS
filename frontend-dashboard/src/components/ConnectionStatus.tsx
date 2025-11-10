import type { ConnectionStatus as StatusType } from '../types/connectionStatus';

/**
 * ConnectionStatus 컴포넌트: WebSocket 연결 상태 표시
 *
 * @param props.status - 연결 상태 ("connected" | "disconnected" | "connecting")
 */
interface ConnectionStatusProps {
  status: StatusType;
}

export function ConnectionStatus({ status }: ConnectionStatusProps) {
  // 연결 상태에 따른 스타일 및 텍스트 설정
  const getStatusConfig = () => {
    switch (status) {
      case 'connected':
        return {
          className: 'status-connected',
          text: '✓ 실시간 연결됨',
          color: '#10b981', // 녹색
        };
      case 'disconnected':
        return {
          className: 'status-disconnected',
          text: '✗ 연결 끊김',
          color: '#ef4444', // 빨간색
        };
      case 'connecting':
        return {
          className: 'status-connecting',
          text: '⟳ 연결 중...',
          color: '#f59e0b', // 노란색
        };
      default:
        return {
          className: 'status-unknown',
          text: '알 수 없음',
          color: '#6b7280', // 회색
        };
    }
  };

  const config = getStatusConfig();

  return (
    <div className={`connection-status ${config.className}`} style={{ color: config.color }}>
      <span className="status-indicator" style={{ backgroundColor: config.color }}></span>
      <span className="status-text">{config.text}</span>
    </div>
  );
}
