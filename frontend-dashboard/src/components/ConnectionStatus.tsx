import type { ConnectionState } from '../types/connectionStatus';
import { formatTimestamp } from '../utils/formatter';

/**
 * ConnectionStatus 컴포넌트: WebSocket 연결 상태 표시
 * - 재연결 시도 횟수 표시 (reconnectAttempts)
 * - 마지막 연결 시각 표시 (lastConnectedAt)
 *
 * @param props.connectionState - 연결 상태 정보
 */
interface ConnectionStatusProps {
  connectionState: ConnectionState;
}

export function ConnectionStatus({ connectionState }: ConnectionStatusProps) {
  const { status, lastConnectedAt, reconnectAttempts } = connectionState;
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
      <div className="status-main">
        <span className="status-indicator" style={{ backgroundColor: config.color }}></span>
        <span className="status-text">{config.text}</span>
      </div>

      {/* 재연결 시도 횟수 표시 (연결 끊김 상태일 때만) */}
      {status === 'disconnected' && reconnectAttempts > 0 && (
        <div className="status-details">
          <span className="status-detail-text">재연결 시도: {reconnectAttempts}회</span>
        </div>
      )}

      {/* 마지막 연결 시각 표시 (연결됨 상태일 때만) */}
      {status === 'connected' && lastConnectedAt && (
        <div className="status-details">
          <span className="status-detail-text">
            마지막 연결: {formatTimestamp(lastConnectedAt)}
          </span>
        </div>
      )}
    </div>
  );
}
