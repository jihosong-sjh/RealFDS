import React, { useState } from 'react';

/**
 * 알림 이력 필터 컴포넌트
 *
 * 규칙명, 사용자 ID, 상태로 알림을 필터링할 수 있는 UI를 제공합니다.
 * User Story 3: 다중 조건 필터링 지원
 */

// 알림 상태 타입
export type AlertStatus = 'UNREAD' | 'IN_PROGRESS' | 'COMPLETED' | '';

// 필터 값 타입
export interface FilterValues {
  ruleName: string;
  userId: string;
  status: AlertStatus;
}

// 컴포넌트 Props 타입
interface AlertHistoryFiltersProps {
  onFilterChange: (filters: FilterValues) => void;
  onSearch: () => void;
}

/**
 * AlertHistoryFilters 컴포넌트
 *
 * @param onFilterChange - 필터 값이 변경될 때 호출되는 콜백
 * @param onSearch - 검색 버튼 클릭 시 호출되는 콜백
 */
export const AlertHistoryFilters: React.FC<AlertHistoryFiltersProps> = ({
  onFilterChange,
  onSearch,
}) => {
  // 필터 상태 관리
  const [filters, setFilters] = useState<FilterValues>({
    ruleName: '',
    userId: '',
    status: '',
  });

  /**
   * 필터 값 변경 핸들러
   */
  const handleFilterChange = (field: keyof FilterValues, value: string) => {
    const newFilters = { ...filters, [field]: value };
    setFilters(newFilters);
    onFilterChange(newFilters);
  };

  /**
   * 초기화 버튼 클릭 핸들러
   */
  const handleReset = () => {
    const resetFilters: FilterValues = {
      ruleName: '',
      userId: '',
      status: '',
    };
    setFilters(resetFilters);
    onFilterChange(resetFilters);
  };

  return (
    <div className="alert-history-filters">
      <h3>필터 조건</h3>

      <div className="filter-group">
        {/* 규칙명 드롭다운 */}
        <div className="filter-item">
          <label htmlFor="ruleName">탐지 규칙</label>
          <select
            id="ruleName"
            value={filters.ruleName}
            onChange={(e) => handleFilterChange('ruleName', e.target.value)}
            className="filter-select"
          >
            <option value="">전체</option>
            <option value="HIGH_AMOUNT">고액 거래 (HIGH_AMOUNT)</option>
            <option value="FOREIGN_COUNTRY">해외 거래 (FOREIGN_COUNTRY)</option>
            <option value="RAPID_TRANSACTION">연속 거래 (RAPID_TRANSACTION)</option>
          </select>
        </div>

        {/* 사용자 ID 입력 필드 */}
        <div className="filter-item">
          <label htmlFor="userId">사용자 ID</label>
          <input
            type="text"
            id="userId"
            value={filters.userId}
            onChange={(e) => handleFilterChange('userId', e.target.value)}
            placeholder="예: user-1"
            className="filter-input"
            pattern="[a-zA-Z0-9-]+"
            title="영문자, 숫자, 하이픈만 입력 가능"
          />
        </div>

        {/* 상태 드롭다운 */}
        <div className="filter-item">
          <label htmlFor="status">처리 상태</label>
          <select
            id="status"
            value={filters.status}
            onChange={(e) => handleFilterChange('status', e.target.value as AlertStatus)}
            className="filter-select"
          >
            <option value="">전체</option>
            <option value="UNREAD">미확인 (UNREAD)</option>
            <option value="IN_PROGRESS">처리 중 (IN_PROGRESS)</option>
            <option value="COMPLETED">완료 (COMPLETED)</option>
          </select>
        </div>
      </div>

      {/* 버튼 그룹 */}
      <div className="filter-actions">
        <button
          onClick={onSearch}
          className="btn-search"
          type="button"
        >
          검색
        </button>
        <button
          onClick={handleReset}
          className="btn-reset"
          type="button"
        >
          초기화
        </button>
      </div>

      <style>{`
        .alert-history-filters {
          background: #f5f5f5;
          padding: 20px;
          border-radius: 8px;
          margin-bottom: 20px;
        }

        .alert-history-filters h3 {
          margin: 0 0 16px 0;
          font-size: 18px;
          font-weight: 600;
          color: #333;
        }

        .filter-group {
          display: grid;
          grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
          gap: 16px;
          margin-bottom: 16px;
        }

        .filter-item {
          display: flex;
          flex-direction: column;
        }

        .filter-item label {
          margin-bottom: 8px;
          font-weight: 500;
          color: #555;
          font-size: 14px;
        }

        .filter-select,
        .filter-input {
          padding: 10px 12px;
          border: 1px solid #ddd;
          border-radius: 4px;
          font-size: 14px;
          transition: border-color 0.2s;
        }

        .filter-select:focus,
        .filter-input:focus {
          outline: none;
          border-color: #4CAF50;
          box-shadow: 0 0 0 2px rgba(76, 175, 80, 0.1);
        }

        .filter-input::placeholder {
          color: #999;
        }

        .filter-actions {
          display: flex;
          gap: 12px;
          justify-content: flex-end;
        }

        .btn-search,
        .btn-reset {
          padding: 10px 24px;
          border: none;
          border-radius: 4px;
          font-size: 14px;
          font-weight: 500;
          cursor: pointer;
          transition: all 0.2s;
        }

        .btn-search {
          background-color: #4CAF50;
          color: white;
        }

        .btn-search:hover {
          background-color: #45a049;
        }

        .btn-reset {
          background-color: #fff;
          color: #666;
          border: 1px solid #ddd;
        }

        .btn-reset:hover {
          background-color: #f5f5f5;
        }

        .btn-search:active,
        .btn-reset:active {
          transform: translateY(1px);
        }
      `}</style>
    </div>
  );
};

export default AlertHistoryFilters;
