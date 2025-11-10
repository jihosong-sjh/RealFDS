import type { Alert } from '../types/alert';
import { AlertItem } from './AlertItem';

/**
 * AlertList 컴포넌트: 알림 목록 표시
 * - 스크롤 영역 추가 (최대 높이 600px, overflow-y: auto)
 * - 로딩 인디케이터 추가 (알림 없을 때)
 *
 * @param props.alerts - 알림 목록 (최신 알림이 맨 앞)
 */
interface AlertListProps {
  alerts: Alert[];
}

export function AlertList({ alerts }: AlertListProps) {
  // 알림이 없을 때 로딩 인디케이터 표시
  if (alerts.length === 0) {
    return (
      <div className="alert-list-empty">
        <div className="loading-indicator">
          <div className="loading-spinner"></div>
          <p className="loading-text">알림 대기 중...</p>
        </div>
        <p className="empty-message">🔍 알림이 없습니다</p>
        <p className="empty-description">실시간으로 탐지된 의심스러운 거래가 여기에 표시됩니다.</p>
      </div>
    );
  }

  return (
    <div className="alert-list">
      <div className="alert-list-header">
        <h2 className="list-title">실시간 알림</h2>
        <span className="alert-count">{alerts.length}개</span>
      </div>

      {/* 스크롤 가능한 알림 목록 컨테이너 (최대 높이 600px) */}
      <div className="alert-list-container alert-list-scrollable">
        {alerts.map((alert) => (
          <AlertItem key={alert.alertId} alert={alert} />
        ))}
      </div>
    </div>
  );
}
