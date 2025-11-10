import type { Alert } from '../types/alert';
import { AlertItem } from './AlertItem';

/**
 * AlertList 컴포넌트: 알림 목록 표시
 *
 * @param props.alerts - 알림 목록 (최신 알림이 맨 앞)
 */
interface AlertListProps {
  alerts: Alert[];
}

export function AlertList({ alerts }: AlertListProps) {
  // 알림이 없을 때 메시지 표시
  if (alerts.length === 0) {
    return (
      <div className="alert-list-empty">
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

      <div className="alert-list-container">
        {alerts.map((alert) => (
          <AlertItem key={alert.alertId} alert={alert} />
        ))}
      </div>
    </div>
  );
}
