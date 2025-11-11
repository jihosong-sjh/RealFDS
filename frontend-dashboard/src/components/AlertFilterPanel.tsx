import { AlertStatus, alertStatusDisplay } from '../types/alertStatus';
import { Severity, severityDisplay } from '../types/severity';

/**
 * AlertFilterPanel 컴포넌트: 알림 필터링 패널
 *
 * 주요 기능:
 * - 상태별 필터 드롭다운 (전체/미확인/확인중/완료)
 * - 담당자별 필터 입력 (User Story 2)
 * - 심각도별 필터 드롭다운 (전체/낮음/보통/높음/긴급) - User Story 3
 * - 필터 적용 시 콜백 호출
 * - 필터 초기화 버튼
 *
 * @param props.selectedStatus - 선택된 상태 필터 (null이면 전체)
 * @param props.onStatusChange - 상태 필터 변경 핸들러
 * @param props.selectedAssignee - 선택된 담당자 필터 (null이면 전체) - User Story 2
 * @param props.onAssigneeChange - 담당자 필터 변경 핸들러 - User Story 2
 * @param props.selectedSeverity - 선택된 심각도 필터 (null이면 전체) - User Story 3
 * @param props.onSeverityChange - 심각도 필터 변경 핸들러 - User Story 3
 */
interface AlertFilterPanelProps {
  selectedStatus: AlertStatus | null;
  onStatusChange: (status: AlertStatus | null) => void;
  selectedAssignee: string | null;
  onAssigneeChange: (assignee: string | null) => void;
  selectedSeverity: Severity | null;
  onSeverityChange: (severity: Severity | null) => void;
}

export function AlertFilterPanel({ selectedStatus, onStatusChange, selectedAssignee, onAssigneeChange, selectedSeverity, onSeverityChange }: AlertFilterPanelProps) {
  // 상태 필터 옵션
  const statusOptions: Array<{ value: AlertStatus | null; label: string }> = [
    { value: null, label: '전체' },
    { value: AlertStatus.UNREAD, label: alertStatusDisplay[AlertStatus.UNREAD] },
    { value: AlertStatus.IN_PROGRESS, label: alertStatusDisplay[AlertStatus.IN_PROGRESS] },
    { value: AlertStatus.COMPLETED, label: alertStatusDisplay[AlertStatus.COMPLETED] },
  ];

  // User Story 3: 심각도 필터 옵션
  const severityOptions: Array<{ value: Severity | null; label: string }> = [
    { value: null, label: '전체' },
    { value: Severity.LOW, label: severityDisplay[Severity.LOW] },
    { value: Severity.MEDIUM, label: severityDisplay[Severity.MEDIUM] },
    { value: Severity.HIGH, label: severityDisplay[Severity.HIGH] },
    { value: Severity.CRITICAL, label: severityDisplay[Severity.CRITICAL] },
  ];

  // 필터 초기화
  const handleReset = () => {
    onStatusChange(null);
    onAssigneeChange(null);
    onSeverityChange(null);
  };

  return (
    <div className="filter-panel">
      <div className="filter-section">
        <label htmlFor="status-filter" className="filter-label">
          상태 필터:
        </label>
        <select
          id="status-filter"
          className="filter-select"
          value={selectedStatus || ''}
          onChange={(e) => {
            const value = e.target.value;
            onStatusChange(value === '' ? null : (value as AlertStatus));
          }}
        >
          {statusOptions.map((option) => (
            <option key={option.value || 'all'} value={option.value || ''}>
              {option.label}
            </option>
          ))}
        </select>
      </div>

      {/* User Story 2: 담당자 필터 */}
      <div className="filter-section">
        <label htmlFor="assignee-filter" className="filter-label">
          👤 담당자 필터:
        </label>
        <input
          id="assignee-filter"
          type="text"
          className="filter-input"
          placeholder="담당자 이름 입력"
          value={selectedAssignee || ''}
          onChange={(e) => {
            const value = e.target.value.trim();
            onAssigneeChange(value === '' ? null : value);
          }}
        />
      </div>

      {/* User Story 3: 심각도 필터 */}
      <div className="filter-section">
        <label htmlFor="severity-filter" className="filter-label">
          ⚠️ 심각도 필터:
        </label>
        <select
          id="severity-filter"
          className="filter-select"
          value={selectedSeverity || ''}
          onChange={(e) => {
            const value = e.target.value;
            onSeverityChange(value === '' ? null : (value as Severity));
          }}
        >
          {severityOptions.map((option) => (
            <option key={option.value || 'all'} value={option.value || ''}>
              {option.label}
            </option>
          ))}
        </select>
      </div>

      {/* 필터 초기화 버튼 */}
      {(selectedStatus || selectedAssignee || selectedSeverity) && (
        <button className="btn btn-secondary btn-sm" onClick={handleReset}>
          필터 초기화
        </button>
      )}

      {/* 필터 적용 상태 표시 */}
      {(selectedStatus || selectedAssignee || selectedSeverity) && (
        <div className="filter-status">
          <span className="filter-status-text">
            필터 적용:
            {selectedStatus && <strong> {alertStatusDisplay[selectedStatus]}</strong>}
            {selectedStatus && (selectedAssignee || selectedSeverity) && <span> | </span>}
            {selectedAssignee && <strong> 담당자: {selectedAssignee}</strong>}
            {selectedAssignee && selectedSeverity && <span> | </span>}
            {selectedSeverity && <strong> 심각도: {severityDisplay[selectedSeverity]}</strong>}
          </span>
        </div>
      )}
    </div>
  );
}
