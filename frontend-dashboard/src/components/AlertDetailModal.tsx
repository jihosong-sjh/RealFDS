import { useState } from 'react';
import type { Alert } from '../types/alert';
import { AlertStatus, alertStatusDisplay, alertStatusColor } from '../types/alertStatus';
import { formatTimestamp, formatAmount } from '../utils/formatter';
import { useAlertManagement } from '../hooks/useAlertManagement';

/**
 * AlertDetailModal 컴포넌트: 알림 상세 정보 및 상태 관리 모달
 *
 * 주요 기능:
 * - 원본 거래 정보 전체 표시
 * - 탐지 규칙 및 사유 표시
 * - 심각도 및 현재 상태 표시
 * - 상태 변경 버튼 (확인중으로 변경, 완료 처리)
 * - 모달 로딩 시간 <200ms 목표
 *
 * @param props.alert - 알림 데이터
 * @param props.isOpen - 모달 열림 상태
 * @param props.onClose - 모달 닫기 핸들러
 */
interface AlertDetailModalProps {
  alert: Alert | null;
  isOpen: boolean;
  onClose: () => void;
}

export function AlertDetailModal({ alert, isOpen, onClose }: AlertDetailModalProps) {
  const { changeAlertStatus, assignAlert, recordAction, isLoading, error } = useAlertManagement('http://localhost:8081');
  const [localError, setLocalError] = useState<string | null>(null);
  const [assignedTo, setAssignedTo] = useState<string>('');
  const [actionNote, setActionNote] = useState<string>('');
  const [completeAfterAction, setCompleteAfterAction] = useState<boolean>(false);

  // 모달이 닫혀있거나 알림이 없으면 렌더링하지 않음
  if (!isOpen || !alert) {
    return null;
  }

  // 상태 변경 핸들러
  const handleStatusChange = async (newStatus: AlertStatus) => {
    setLocalError(null);
    const success = await changeAlertStatus(alert.alertId, newStatus);
    if (success) {
      console.log('[AlertDetailModal] 상태 변경 성공:', newStatus);
      // WebSocket을 통해 실시간 업데이트되므로 모달은 닫지 않음
    } else {
      setLocalError('상태 변경에 실패했습니다. 다시 시도해주세요.');
    }
  };

  // 담당자 할당 핸들러 (User Story 2)
  const handleAssign = async () => {
    if (!assignedTo.trim()) {
      setLocalError('담당자 이름을 입력해주세요.');
      return;
    }
    setLocalError(null);
    const success = await assignAlert(alert.alertId, assignedTo.trim());
    if (success) {
      console.log('[AlertDetailModal] 담당자 할당 성공:', assignedTo);
      setAssignedTo(''); // 입력 필드 초기화
    } else {
      setLocalError('담당자 할당에 실패했습니다. 다시 시도해주세요.');
    }
  };

  // 조치 내용 기록 핸들러 (User Story 2)
  const handleRecordAction = async () => {
    if (!actionNote.trim()) {
      setLocalError('조치 내용을 입력해주세요.');
      return;
    }
    setLocalError(null);
    const success = await recordAction(alert.alertId, actionNote.trim(), completeAfterAction);
    if (success) {
      console.log('[AlertDetailModal] 조치 기록 성공:', actionNote.length, completeAfterAction);
      setActionNote(''); // 입력 필드 초기화
      setCompleteAfterAction(false);
    } else {
      setLocalError('조치 기록에 실패했습니다. 다시 시도해주세요.');
    }
  };

  // 심각도 색상 가져오기
  const getSeverityColor = (severity: string) => {
    switch (severity) {
      case 'HIGH':
        return '#ef4444'; // 빨간색
      case 'MEDIUM':
        return '#f59e0b'; // 노란색
      case 'LOW':
        return '#3b82f6'; // 파란색
      default:
        return '#6b7280'; // 회색
    }
  };

  const severityColor = getSeverityColor(alert.severity);
  const statusText = alertStatusDisplay[alert.status as keyof typeof alertStatusDisplay] || '알 수 없음';
  const statusColor = alertStatusColor[alert.status as keyof typeof alertStatusColor] || '#6b7280';

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()}>
        {/* 모달 헤더 */}
        <div className="modal-header">
          <h2 className="modal-title">알림 상세 정보</h2>
          <button className="modal-close-btn" onClick={onClose} aria-label="닫기">
            ✕
          </button>
        </div>

        {/* 모달 바디 */}
        <div className="modal-body">
          {/* 상태 및 심각도 표시 */}
          <div className="modal-badges">
            <span className="status-badge" style={{ backgroundColor: statusColor }}>
              {statusText}
            </span>
            <span className="severity-badge" style={{ backgroundColor: severityColor }}>
              {alert.severity}
            </span>
          </div>

          {/* 알림 정보 */}
          <div className="modal-section">
            <h3 className="section-title">알림 정보</h3>
            <div className="detail-grid">
              <div className="detail-item">
                <span className="detail-label">알림 ID:</span>
                <span className="detail-value">{alert.alertId}</span>
              </div>
              <div className="detail-item">
                <span className="detail-label">탐지 규칙:</span>
                <span className="detail-value">{alert.ruleName}</span>
              </div>
              <div className="detail-item">
                <span className="detail-label">사유:</span>
                <span className="detail-value">{alert.reason}</span>
              </div>
              <div className="detail-item">
                <span className="detail-label">알림 생성 시각:</span>
                <span className="detail-value">{formatTimestamp(alert.alertTimestamp)}</span>
              </div>
              {alert.processedAt && (
                <div className="detail-item">
                  <span className="detail-label">처리 완료 시각:</span>
                  <span className="detail-value">{formatTimestamp(alert.processedAt)}</span>
                </div>
              )}
            </div>
          </div>

          {/* 거래 정보 */}
          <div className="modal-section">
            <h3 className="section-title">거래 정보</h3>
            <div className="detail-grid">
              <div className="detail-item">
                <span className="detail-label">거래 ID:</span>
                <span className="detail-value">{alert.originalTransaction.transactionId}</span>
              </div>
              <div className="detail-item">
                <span className="detail-label">사용자 ID:</span>
                <span className="detail-value">{alert.originalTransaction.userId}</span>
              </div>
              <div className="detail-item">
                <span className="detail-label">거래 금액:</span>
                <span className="detail-value">{formatAmount(alert.originalTransaction.amount)}</span>
              </div>
              <div className="detail-item">
                <span className="detail-label">통화:</span>
                <span className="detail-value">{alert.originalTransaction.currency}</span>
              </div>
              <div className="detail-item">
                <span className="detail-label">국가:</span>
                <span className="detail-value">{alert.originalTransaction.countryCode}</span>
              </div>
              <div className="detail-item">
                <span className="detail-label">거래 시각:</span>
                <span className="detail-value">{formatTimestamp(alert.originalTransaction.timestamp)}</span>
              </div>
            </div>
          </div>

          {/* User Story 2: 담당자 할당 섹션 */}
          <div className="modal-section">
            <h3 className="section-title">👤 담당자 할당</h3>
            <div className="detail-grid">
              <div className="detail-item">
                <span className="detail-label">현재 담당자:</span>
                <span className="detail-value">{alert.assignedTo || '미할당'}</span>
              </div>
            </div>
            <div className="form-group">
              <input
                type="text"
                className="form-input"
                placeholder="담당자 이름 입력 (최대 100자)"
                value={assignedTo}
                onChange={(e) => setAssignedTo(e.target.value)}
                maxLength={100}
                disabled={isLoading}
              />
              <button
                className="btn btn-secondary"
                onClick={handleAssign}
                disabled={isLoading || !assignedTo.trim()}
              >
                {isLoading ? '할당 중...' : '할당하기'}
              </button>
            </div>
          </div>

          {/* User Story 2: 조치 내용 기록 섹션 */}
          <div className="modal-section">
            <h3 className="section-title">📝 조치 내용 기록</h3>
            {alert.actionNote && (
              <div className="detail-grid">
                <div className="detail-item">
                  <span className="detail-label">기록된 조치 내용:</span>
                  <span className="detail-value">{alert.actionNote}</span>
                </div>
              </div>
            )}
            <div className="form-group">
              <textarea
                className="form-textarea"
                placeholder="조치 내용 입력 (최대 2000자)\n예: 고객에게 확인 전화. 정상 거래로 확인됨."
                value={actionNote}
                onChange={(e) => setActionNote(e.target.value)}
                maxLength={2000}
                rows={4}
                disabled={isLoading}
              />
              <div className="char-count">
                {actionNote.length} / 2000자
              </div>
              <div className="checkbox-group">
                <label>
                  <input
                    type="checkbox"
                    checked={completeAfterAction}
                    onChange={(e) => setCompleteAfterAction(e.target.checked)}
                    disabled={isLoading}
                  />
                  <span>조치 기록 후 완료 처리</span>
                </label>
              </div>
              <button
                className="btn btn-secondary"
                onClick={handleRecordAction}
                disabled={isLoading || !actionNote.trim()}
              >
                {isLoading ? '기록 중...' : '조치 기록하기'}
              </button>
            </div>
            {!alert.actionNote && (
              <p className="form-hint">
                💡 조치 내용 입력은 선택 사항이지만, 완료 처리 시 입력을 권장합니다.
              </p>
            )}
          </div>

          {/* 에러 메시지 표시 */}
          {(error || localError) && (
            <div className="error-message">
              {error || localError}
            </div>
          )}

          {/* 상태 변경 버튼 */}
          <div className="modal-actions">
            {alert.status === AlertStatus.UNREAD && (
              <button
                className="btn btn-primary"
                onClick={() => handleStatusChange(AlertStatus.IN_PROGRESS)}
                disabled={isLoading}
              >
                {isLoading ? '처리중...' : '확인중으로 변경'}
              </button>
            )}
            {(alert.status === AlertStatus.UNREAD || alert.status === AlertStatus.IN_PROGRESS) && (
              <button
                className="btn btn-success"
                onClick={() => handleStatusChange(AlertStatus.COMPLETED)}
                disabled={isLoading}
              >
                {isLoading ? '처리중...' : '완료 처리'}
              </button>
            )}
            {alert.status === AlertStatus.COMPLETED && (
              <button
                className="btn btn-secondary"
                onClick={() => handleStatusChange(AlertStatus.IN_PROGRESS)}
                disabled={isLoading}
              >
                {isLoading ? '처리중...' : '재조사'}
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
