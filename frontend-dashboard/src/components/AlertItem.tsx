import type { Alert } from '../types/alert';

/**
 * AlertItem 컴포넌트: 개별 알림 카드 표시
 *
 * @param props.alert - 알림 데이터
 */
interface AlertItemProps {
  alert: Alert;
}

export function AlertItem({ alert }: AlertItemProps) {
  // 심각도에 따른 색상 및 아이콘 설정
  const getSeverityConfig = (severity: string) => {
    switch (severity) {
      case 'HIGH':
        return {
          className: 'severity-high',
          color: '#ef4444', // 빨간색
          icon: '⚠️',
          label: '높음',
        };
      case 'MEDIUM':
        return {
          className: 'severity-medium',
          color: '#f59e0b', // 노란색
          icon: '⚡',
          label: '중간',
        };
      case 'LOW':
        return {
          className: 'severity-low',
          color: '#3b82f6', // 파란색
          icon: 'ℹ️',
          label: '낮음',
        };
      default:
        return {
          className: 'severity-unknown',
          color: '#6b7280', // 회색
          icon: '?',
          label: '알 수 없음',
        };
    }
  };

  const severityConfig = getSeverityConfig(alert.severity);

  // 시간 포맷팅 (ISO 8601 → 한국 시간)
  const formatTimestamp = (timestamp: string) => {
    try {
      const date = new Date(timestamp);
      return date.toLocaleString('ko-KR', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
      });
    } catch {
      return timestamp;
    }
  };

  // 금액 포맷팅 (1,000,000 → 1,000,000원)
  const formatAmount = (amount: number) => {
    return amount.toLocaleString('ko-KR') + '원';
  };

  return (
    <div className={`alert-item ${severityConfig.className}`}>
      {/* 심각도 표시 */}
      <div className="alert-header">
        <span className="severity-badge" style={{ backgroundColor: severityConfig.color }}>
          <span className="severity-icon">{severityConfig.icon}</span>
          <span className="severity-label">{severityConfig.label}</span>
        </span>
        <span className="alert-timestamp">{formatTimestamp(alert.alertTimestamp)}</span>
      </div>

      {/* 알림 내용 */}
      <div className="alert-body">
        <h3 className="alert-reason">{alert.reason}</h3>

        <div className="alert-details">
          <div className="detail-row">
            <span className="detail-label">거래 ID:</span>
            <span className="detail-value">{alert.originalTransaction.transactionId}</span>
          </div>
          <div className="detail-row">
            <span className="detail-label">사용자 ID:</span>
            <span className="detail-value">{alert.originalTransaction.userId}</span>
          </div>
          <div className="detail-row">
            <span className="detail-label">거래 금액:</span>
            <span className="detail-value">{formatAmount(alert.originalTransaction.amount)}</span>
          </div>
          <div className="detail-row">
            <span className="detail-label">국가:</span>
            <span className="detail-value">{alert.originalTransaction.countryCode}</span>
          </div>
          <div className="detail-row">
            <span className="detail-label">탐지 규칙:</span>
            <span className="detail-value">{alert.ruleName}</span>
          </div>
        </div>
      </div>
    </div>
  );
}
