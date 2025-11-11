import type { Alert } from '../types/alert';
import { formatTimestamp, formatAmount } from '../utils/formatter';
import { alertStatusDisplay, alertStatusColor } from '../types/alertStatus';
import { Severity, severityBackgroundColor } from '../types/severity';
import SeverityBadge from './SeverityBadge';

/**
 * AlertItem 컴포넌트: 개별 알림 카드 표시
 * - 포맷팅 유틸리티 사용 (formatTimestamp, formatAmount)
 * - 상태 뱃지 추가 (UNREAD: 회색, IN_PROGRESS: 파란색, COMPLETED: 초록색) [002-alert-management]
 * - 심각도 뱃지 추가 (SeverityBadge 컴포넌트 사용) [002-alert-management User Story 3]
 * - 애니메이션 효과 추가 (새 알림 등장 시 fade-in)
 *
 * @param props.alert - 알림 데이터
 * @param props.onClick - 알림 클릭 이벤트 핸들러 (선택 사항)
 */
interface AlertItemProps {
  alert: Alert;
  onClick?: () => void;
}

export function AlertItem({ alert, onClick }: AlertItemProps) {
  // 002-alert-management: 상태 정보 가져오기
  const statusText = alertStatusDisplay[alert.status as keyof typeof alertStatusDisplay] || '알 수 없음';
  const statusColor = alertStatusColor[alert.status as keyof typeof alertStatusColor] || '#6b7280';

  // 002-alert-management User Story 3: 심각도를 Severity enum으로 변환
  const severity = alert.severity as Severity;

  // 002-alert-management User Story 3: 심각도별 배경색 설정
  const backgroundColor = severityBackgroundColor[severity];

  return (
    <div
      className="alert-item alert-item-fade-in"
      onClick={onClick}
      style={{
        cursor: onClick ? 'pointer' : 'default',
        backgroundColor, // 심각도별 배경색 적용
      }}
    >
      {/* 상태 및 심각도 표시 */}
      <div className="alert-header">
        {/* 002-alert-management: 상태 뱃지 추가 */}
        <span className="status-badge" style={{ backgroundColor: statusColor }}>
          <span className="status-label">{statusText}</span>
        </span>
        {/* 002-alert-management User Story 3: SeverityBadge 컴포넌트 추가 */}
        <SeverityBadge severity={severity} />
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
          {/* 002-alert-management User Story 2: 담당자 표시 */}
          <div className="detail-row">
            <span className="detail-label">👤 담당자:</span>
            <span className="detail-value assignee">
              {alert.assignedTo || '미할당'}
            </span>
          </div>
        </div>
      </div>
    </div>
  );
}
