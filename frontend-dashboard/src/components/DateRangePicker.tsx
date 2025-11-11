import { useState, useEffect } from 'react';
import '../styles/DateRangePicker.css';

/**
 * DateRangePicker 컴포넌트 Props
 */
export interface DateRangePickerProps {
  /** 날짜 범위 변경 시 호출되는 콜백 함수 */
  onChange: (startDate: string | null, endDate: string | null) => void;
  /** 초기 시작 날짜 (ISO 8601 형식: YYYY-MM-DD) */
  initialStartDate?: string | null;
  /** 초기 종료 날짜 (ISO 8601 형식: YYYY-MM-DD) */
  initialEndDate?: string | null;
}

/**
 * DateRangePicker 컴포넌트
 *
 * 날짜 범위 선택을 위한 컴포넌트입니다.
 * - 시작 날짜(startDate)와 종료 날짜(endDate)를 선택할 수 있습니다
 * - 유효성 검사: startDate ≤ endDate 조건을 자동으로 검증합니다
 * - 한국어 라벨 및 에러 메시지를 제공합니다
 * - onChange 콜백을 통해 부모 컴포넌트에 선택된 날짜 범위를 전달합니다
 *
 * @example
 * ```tsx
 * <DateRangePicker
 *   onChange={(start, end) => console.log(start, end)}
 *   initialStartDate="2025-11-01"
 *   initialEndDate="2025-11-11"
 * />
 * ```
 */
export function DateRangePicker({ onChange, initialStartDate, initialEndDate }: DateRangePickerProps) {
  // 상태 관리: 시작 날짜
  const [startDate, setStartDate] = useState<string>(initialStartDate || '');
  // 상태 관리: 종료 날짜
  const [endDate, setEndDate] = useState<string>(initialEndDate || '');
  // 상태 관리: 유효성 검사 에러 메시지
  const [error, setError] = useState<string>('');

  /**
   * 날짜 범위 유효성 검사
   * - startDate와 endDate가 모두 입력된 경우, startDate ≤ endDate 조건을 확인합니다
   * - 조건 위반 시 에러 메시지를 설정합니다
   *
   * @returns 유효한 경우 true, 유효하지 않은 경우 false
   */
  const validateDateRange = (start: string, end: string): boolean => {
    // 둘 다 입력되지 않은 경우 유효함
    if (!start && !end) {
      setError('');
      return true;
    }

    // 하나만 입력된 경우 유효함
    if (!start || !end) {
      setError('');
      return true;
    }

    // startDate ≤ endDate 검증
    const startDateObj = new Date(start);
    const endDateObj = new Date(end);

    if (startDateObj > endDateObj) {
      setError('시작 날짜는 종료 날짜보다 늦을 수 없습니다');
      return false;
    }

    setError('');
    return true;
  };

  /**
   * 시작 날짜 변경 핸들러
   */
  const handleStartDateChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const newStartDate = e.target.value;
    setStartDate(newStartDate);

    // 유효성 검사 후 부모 컴포넌트에 전달
    if (validateDateRange(newStartDate, endDate)) {
      onChange(newStartDate || null, endDate || null);
    }
  };

  /**
   * 종료 날짜 변경 핸들러
   */
  const handleEndDateChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const newEndDate = e.target.value;
    setEndDate(newEndDate);

    // 유효성 검사 후 부모 컴포넌트에 전달
    if (validateDateRange(startDate, newEndDate)) {
      onChange(startDate || null, newEndDate || null);
    }
  };

  /**
   * 초기값 변경 시 상태 업데이트
   */
  useEffect(() => {
    if (initialStartDate !== undefined) {
      setStartDate(initialStartDate || '');
    }
    if (initialEndDate !== undefined) {
      setEndDate(initialEndDate || '');
    }
  }, [initialStartDate, initialEndDate]);

  return (
    <div className="date-range-picker">
      <div className="date-range-picker__inputs">
        {/* 시작 날짜 입력 필드 */}
        <div className="date-range-picker__field">
          <label htmlFor="startDate" className="date-range-picker__label">
            시작 날짜
          </label>
          <input
            type="date"
            id="startDate"
            value={startDate}
            onChange={handleStartDateChange}
            className="date-range-picker__input"
            aria-label="시작 날짜 선택"
            max={new Date().toISOString().split('T')[0]} // 미래 날짜 선택 불가
          />
        </div>

        {/* 종료 날짜 입력 필드 */}
        <div className="date-range-picker__field">
          <label htmlFor="endDate" className="date-range-picker__label">
            종료 날짜
          </label>
          <input
            type="date"
            id="endDate"
            value={endDate}
            onChange={handleEndDateChange}
            className="date-range-picker__input"
            aria-label="종료 날짜 선택"
            max={new Date().toISOString().split('T')[0]} // 미래 날짜 선택 불가
          />
        </div>
      </div>

      {/* 에러 메시지 표시 */}
      {error && (
        <div className="date-range-picker__error" role="alert">
          {error}
        </div>
      )}
    </div>
  );
}
