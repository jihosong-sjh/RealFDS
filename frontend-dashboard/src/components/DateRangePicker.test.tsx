import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { DateRangePicker } from './DateRangePicker';

/**
 * DateRangePicker 컴포넌트 단위 테스트
 *
 * 테스트 범위:
 * - 초기 렌더링 확인
 * - 날짜 선택 시 onChange 콜백 호출 확인
 * - 유효성 검사 (startDate ≤ endDate) 확인
 * - 에러 메시지 표시 확인
 */
describe('DateRangePicker', () => {
  /**
   * 테스트: 초기 렌더링 시 라벨과 입력 필드가 올바르게 표시되는지 확인
   */
  it('초기 렌더링 시 라벨과 입력 필드가 표시된다', () => {
    // Given: DateRangePicker 컴포넌트 렌더링
    const mockOnChange = vi.fn();
    render(<DateRangePicker onChange={mockOnChange} />);

    // When: 화면에서 라벨 및 입력 필드 검색
    const startDateLabel = screen.getByText('시작 날짜');
    const endDateLabel = screen.getByText('종료 날짜');
    const startDateInput = screen.getByLabelText('시작 날짜 선택');
    const endDateInput = screen.getByLabelText('종료 날짜 선택');

    // Then: 모든 요소가 존재해야 함
    expect(startDateLabel).toBeInTheDocument();
    expect(endDateLabel).toBeInTheDocument();
    expect(startDateInput).toBeInTheDocument();
    expect(endDateInput).toBeInTheDocument();
  });

  /**
   * 테스트: 초기값이 올바르게 설정되는지 확인
   */
  it('초기값이 올바르게 설정된다', () => {
    // Given: 초기 날짜 범위가 설정된 DateRangePicker 렌더링
    const mockOnChange = vi.fn();
    const initialStartDate = '2025-11-01';
    const initialEndDate = '2025-11-11';

    render(
      <DateRangePicker
        onChange={mockOnChange}
        initialStartDate={initialStartDate}
        initialEndDate={initialEndDate}
      />
    );

    // When: 입력 필드 검색
    const startDateInput = screen.getByLabelText('시작 날짜 선택') as HTMLInputElement;
    const endDateInput = screen.getByLabelText('종료 날짜 선택') as HTMLInputElement;

    // Then: 입력 필드 값이 초기값과 일치해야 함
    expect(startDateInput.value).toBe(initialStartDate);
    expect(endDateInput.value).toBe(initialEndDate);
  });

  /**
   * 테스트: 시작 날짜 선택 시 onChange 콜백이 호출되는지 확인
   */
  it('시작 날짜 선택 시 onChange가 호출된다', () => {
    // Given: DateRangePicker 컴포넌트 렌더링
    const mockOnChange = vi.fn();
    render(<DateRangePicker onChange={mockOnChange} />);

    // When: 시작 날짜 입력 필드에 값 입력
    const startDateInput = screen.getByLabelText('시작 날짜 선택');
    fireEvent.change(startDateInput, { target: { value: '2025-11-01' } });

    // Then: onChange 콜백이 호출되어야 함
    expect(mockOnChange).toHaveBeenCalledWith('2025-11-01', null);
  });

  /**
   * 테스트: 종료 날짜 선택 시 onChange 콜백이 호출되는지 확인
   */
  it('종료 날짜 선택 시 onChange가 호출된다', () => {
    // Given: DateRangePicker 컴포넌트 렌더링
    const mockOnChange = vi.fn();
    render(<DateRangePicker onChange={mockOnChange} />);

    // When: 종료 날짜 입력 필드에 값 입력
    const endDateInput = screen.getByLabelText('종료 날짜 선택');
    fireEvent.change(endDateInput, { target: { value: '2025-11-11' } });

    // Then: onChange 콜백이 호출되어야 함
    expect(mockOnChange).toHaveBeenCalledWith(null, '2025-11-11');
  });

  /**
   * 테스트: 유효한 날짜 범위 입력 시 onChange가 호출되고 에러 메시지가 표시되지 않는지 확인
   */
  it('유효한 날짜 범위 입력 시 onChange가 호출되고 에러가 표시되지 않는다', () => {
    // Given: DateRangePicker 컴포넌트 렌더링
    const mockOnChange = vi.fn();
    render(<DateRangePicker onChange={mockOnChange} />);

    // When: 유효한 날짜 범위 입력 (startDate ≤ endDate)
    const startDateInput = screen.getByLabelText('시작 날짜 선택');
    const endDateInput = screen.getByLabelText('종료 날짜 선택');

    fireEvent.change(startDateInput, { target: { value: '2025-11-01' } });
    fireEvent.change(endDateInput, { target: { value: '2025-11-11' } });

    // Then: onChange가 호출되어야 하고, 에러 메시지가 표시되지 않아야 함
    expect(mockOnChange).toHaveBeenCalledTimes(2);
    expect(mockOnChange).toHaveBeenLastCalledWith('2025-11-01', '2025-11-11');

    const errorMessage = screen.queryByRole('alert');
    expect(errorMessage).not.toBeInTheDocument();
  });

  /**
   * 테스트: 잘못된 날짜 범위 입력 시 에러 메시지가 표시되는지 확인
   */
  it('잘못된 날짜 범위 입력 시 에러 메시지가 표시된다', () => {
    // Given: DateRangePicker 컴포넌트 렌더링
    const mockOnChange = vi.fn();
    render(<DateRangePicker onChange={mockOnChange} />);

    // When: 잘못된 날짜 범위 입력 (startDate > endDate)
    const startDateInput = screen.getByLabelText('시작 날짜 선택');
    const endDateInput = screen.getByLabelText('종료 날짜 선택');

    fireEvent.change(startDateInput, { target: { value: '2025-11-11' } });
    fireEvent.change(endDateInput, { target: { value: '2025-11-01' } });

    // Then: 에러 메시지가 표시되어야 함
    const errorMessage = screen.getByRole('alert');
    expect(errorMessage).toBeInTheDocument();
    expect(errorMessage).toHaveTextContent('시작 날짜는 종료 날짜보다 늦을 수 없습니다');
  });

  /**
   * 테스트: 잘못된 날짜 범위 입력 시 onChange가 호출되지 않는지 확인
   */
  it('잘못된 날짜 범위 입력 시 onChange가 호출되지 않는다', () => {
    // Given: 유효한 날짜 범위가 설정된 DateRangePicker 렌더링
    const mockOnChange = vi.fn();
    render(
      <DateRangePicker
        onChange={mockOnChange}
        initialStartDate="2025-11-01"
        initialEndDate="2025-11-11"
      />
    );

    // When: 잘못된 종료 날짜 입력 (startDate > endDate)
    const endDateInput = screen.getByLabelText('종료 날짜 선택');
    fireEvent.change(endDateInput, { target: { value: '2025-10-01' } });

    // Then: onChange가 추가로 호출되지 않아야 함
    // (초기 렌더링 시에는 호출되지 않으므로 총 호출 횟수는 0)
    expect(mockOnChange).toHaveBeenCalledTimes(0);
  });

  /**
   * 테스트: 빈 값 입력 시 onChange가 null과 함께 호출되는지 확인
   */
  it('빈 값 입력 시 onChange가 null과 함께 호출된다', () => {
    // Given: 초기값이 설정된 DateRangePicker 렌더링
    const mockOnChange = vi.fn();
    render(
      <DateRangePicker
        onChange={mockOnChange}
        initialStartDate="2025-11-01"
        initialEndDate="2025-11-11"
      />
    );

    // When: 시작 날짜를 빈 값으로 변경
    const startDateInput = screen.getByLabelText('시작 날짜 선택');
    fireEvent.change(startDateInput, { target: { value: '' } });

    // Then: onChange가 null과 함께 호출되어야 함
    expect(mockOnChange).toHaveBeenCalledWith(null, '2025-11-11');
  });
});
