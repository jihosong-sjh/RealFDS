import { useState } from 'react';
import type { Alert } from '../types/alert';
import { AlertItem } from './AlertItem';
import { Severity, severityOrder } from '../types/severity';

/**
 * AlertList 컴포넌트: 알림 목록 표시
 * - 스크롤 영역 추가 (최대 높이 600px, overflow-y: auto)
 * - 로딩 인디케이터 추가 (알림 없을 때)
 * - 심각도별 정렬 기능 추가 (User Story 3)
 *
 * @param props.alerts - 알림 목록 (최신 알림이 맨 앞)
 */
interface AlertListProps {
  alerts: Alert[];
}

export function AlertList({ alerts }: AlertListProps) {
  // User Story 3: 심각도별 정렬 상태 관리
  const [sortBySeverity, setSortBySeverity] = useState(false);

  // User Story 3: 심각도별 정렬 로직 (CRITICAL → HIGH → MEDIUM → LOW 순서)
  const sortedAlerts = sortBySeverity
    ? [...alerts].sort((a, b) => {
        const severityA = a.severity as Severity;
        const severityB = b.severity as Severity;
        return severityOrder[severityB] - severityOrder[severityA];
      })
    : alerts;

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
        {/* User Story 3: 심각도별 정렬 버튼 */}
        <button
          className={`btn btn-sm ${sortBySeverity ? 'btn-primary' : 'btn-secondary'}`}
          onClick={() => setSortBySeverity(!sortBySeverity)}
          title="심각도순 정렬"
        >
          ⚠️ 심각도순
        </button>
      </div>

      {/* 스크롤 가능한 알림 목록 컨테이너 (최대 높이 600px) */}
      <div className="alert-list-container alert-list-scrollable">
        {sortedAlerts.map((alert) => (
          <AlertItem key={alert.alertId} alert={alert} />
        ))}
      </div>
    </div>
  );
}
