import React from 'react';
import { ConnectionStatus as Status } from '../../hooks/useWebSocket';

/**
 * ConnectionStatus 배너 컴포넌트
 *
 * WebSocket 연결 상태를 사용자에게 시각적으로 표시합니다.
 * - 연결 끊김: 빨간색 경고 배너 ("연결 끊김")
 * - 재연결 중: 노란색 정보 배너 ("재연결 중...")
 * - 연결됨: 배너 숨김 (정상 상태)
 * - 에러: 빨간색 에러 배너 (에러 메시지 표시)
 *
 * @param props.status WebSocket 연결 상태
 * @param props.error 에러 메시지 (있을 경우)
 */

interface ConnectionStatusProps {
  /** WebSocket 연결 상태 */
  status: Status;
  /** 에러 메시지 (있을 경우) */
  error: string | null;
}

export const ConnectionStatus: React.FC<ConnectionStatusProps> = ({ status, error }) => {
  /**
   * 연결 상태에 따른 배너 스타일 결정
   */
  const getBannerStyle = (): React.CSSProperties => {
    const baseStyle: React.CSSProperties = {
      position: 'fixed',
      top: 0,
      left: 0,
      right: 0,
      padding: '12px 20px',
      textAlign: 'center',
      fontWeight: 600,
      fontSize: '14px',
      zIndex: 9999,
      boxShadow: '0 2px 8px rgba(0, 0, 0, 0.15)',
      transition: 'all 0.3s ease-in-out',
    };

    switch (status) {
      case 'disconnected':
        return {
          ...baseStyle,
          backgroundColor: '#f44336', // 빨간색
          color: '#ffffff',
        };

      case 'connecting':
        return {
          ...baseStyle,
          backgroundColor: '#ff9800', // 주황색
          color: '#ffffff',
        };

      case 'error':
        return {
          ...baseStyle,
          backgroundColor: '#d32f2f', // 진한 빨간색
          color: '#ffffff',
        };

      default:
        return { display: 'none' }; // 연결됨 (숨김)
    }
  };

  /**
   * 연결 상태에 따른 배너 메시지 결정
   */
  const getMessage = (): string => {
    switch (status) {
      case 'disconnected':
        return '⚠️ 연결 끊김 - 실시간 업데이트가 중단되었습니다';

      case 'connecting':
        return '🔄 재연결 중... 잠시만 기다려주세요';

      case 'error':
        return error ? `❌ 오류: ${error}` : '❌ 연결 오류가 발생했습니다';

      default:
        return ''; // 연결됨 (메시지 없음)
    }
  };

  /**
   * 연결됨 상태일 경우 배너를 표시하지 않음
   */
  if (status === 'connected') {
    return null;
  }

  return (
    <div style={getBannerStyle()} role="alert" aria-live="polite">
      {getMessage()}
    </div>
  );
};

export default ConnectionStatus;
