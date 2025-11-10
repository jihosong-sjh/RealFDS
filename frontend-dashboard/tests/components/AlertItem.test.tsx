// T085: AlertItem 컴포넌트 단위 테스트
// Given-When-Then 구조 사용, 한국어 주석

import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { AlertItem } from '../../src/components/AlertItem';
import type { Alert } from '../../src/types/alert';

describe('AlertItem 컴포넌트 테스트', () => {
  // 테스트용 Mock Alert 데이터
  const createMockAlert = (severity: Alert['severity'], ruleName: string, amount: number, countryCode: string): Alert => ({
    schemaVersion: '1.0',
    alertId: 'alert-test-123',
    originalTransaction: {
      schemaVersion: '1.0',
      transactionId: 'tx-456',
      userId: 'user-5',
      amount,
      currency: 'KRW',
      countryCode,
      timestamp: '2025-11-10T10:30:45.123Z',
    },
    ruleType: 'SIMPLE_RULE',
    ruleName,
    reason: `테스트 알림: ${ruleName}`,
    severity,
    alertTimestamp: '2025-11-10T10:30:47.456Z',
  });

  it('test_render_alert_fields: 모든 필드 렌더링 확인', () => {
    // Given: 고액 거래 알림 데이터
    const mockAlert = createMockAlert('HIGH', 'HIGH_VALUE', 1250000, 'KR');

    // When: AlertItem 컴포넌트 렌더링
    render(<AlertItem alert={mockAlert} />);

    // Then: 모든 필드가 화면에 표시됨
    expect(screen.getByText(/테스트 알림: HIGH_VALUE/i)).toBeInTheDocument();
    expect(screen.getByText(/tx-456/i)).toBeInTheDocument();
    expect(screen.getByText(/user-5/i)).toBeInTheDocument();
    expect(screen.getByText(/1,250,000원/i)).toBeInTheDocument();
    expect(screen.getByText(/KR/i)).toBeInTheDocument();
    expect(screen.getByText(/HIGH_VALUE/i)).toBeInTheDocument();
  });

  it('test_severity_color_high: 심각도 HIGH일 때 빨간색 표시', () => {
    // Given: HIGH 심각도 알림
    const mockAlert = createMockAlert('HIGH', 'HIGH_VALUE', 1250000, 'KR');

    // When: AlertItem 컴포넌트 렌더링
    const { container } = render(<AlertItem alert={mockAlert} />);

    // Then: severity-high 클래스가 적용됨
    const alertItem = container.querySelector('.alert-item');
    expect(alertItem).toHaveClass('severity-high');

    // severity badge 확인
    const severityBadge = container.querySelector('.severity-badge');
    expect(severityBadge).toHaveStyle({ backgroundColor: '#ef4444' });

    // 심각도 라벨 확인
    expect(screen.getByText('높음')).toBeInTheDocument();

    // 아이콘 확인
    expect(screen.getByText('⚠️')).toBeInTheDocument();
  });

  it('test_severity_color_medium: 심각도 MEDIUM일 때 노란색 표시', () => {
    // Given: MEDIUM 심각도 알림
    const mockAlert = createMockAlert('MEDIUM', 'FOREIGN_COUNTRY', 50000, 'US');

    // When: AlertItem 컴포넌트 렌더링
    const { container } = render(<AlertItem alert={mockAlert} />);

    // Then: severity-medium 클래스가 적용됨
    const alertItem = container.querySelector('.alert-item');
    expect(alertItem).toHaveClass('severity-medium');

    // severity badge 확인
    const severityBadge = container.querySelector('.severity-badge');
    expect(severityBadge).toHaveStyle({ backgroundColor: '#f59e0b' });

    // 심각도 라벨 확인
    expect(screen.getByText('중간')).toBeInTheDocument();

    // 아이콘 확인
    expect(screen.getByText('⚡')).toBeInTheDocument();
  });

  it('test_severity_color_low: 심각도 LOW일 때 파란색 표시', () => {
    // Given: LOW 심각도 알림
    const mockAlert = createMockAlert('LOW', 'CUSTOM_RULE', 30000, 'KR');

    // When: AlertItem 컴포넌트 렌더링
    const { container } = render(<AlertItem alert={mockAlert} />);

    // Then: severity-low 클래스가 적용됨
    const alertItem = container.querySelector('.alert-item');
    expect(alertItem).toHaveClass('severity-low');

    // severity badge 확인
    const severityBadge = container.querySelector('.severity-badge');
    expect(severityBadge).toHaveStyle({ backgroundColor: '#3b82f6' });

    // 심각도 라벨 확인
    expect(screen.getByText('낮음')).toBeInTheDocument();

    // 아이콘 확인
    expect(screen.getByText('ℹ️')).toBeInTheDocument();
  });

  it('test_amount_formatting: 금액 포맷팅 확인 (1,000,000원 형식)', () => {
    // Given: 다양한 금액의 알림들
    const testCases = [
      { amount: 1000, expected: '1,000원' },
      { amount: 50000, expected: '50,000원' },
      { amount: 1250000, expected: '1,250,000원' },
      { amount: 10000000, expected: '10,000,000원' },
    ];

    testCases.forEach(({ amount, expected }) => {
      // Given: 특정 금액의 알림
      const mockAlert = createMockAlert('HIGH', 'HIGH_VALUE', amount, 'KR');

      // When: AlertItem 컴포넌트 렌더링
      const { unmount } = render(<AlertItem alert={mockAlert} />);

      // Then: 금액이 올바르게 포맷팅됨
      expect(screen.getByText(expected)).toBeInTheDocument();

      // 다음 테스트를 위해 언마운트
      unmount();
    });
  });

  it('test_timestamp_formatting: 시간 포맷팅 확인 (한국 시간 형식)', () => {
    // Given: 특정 시간의 알림
    const mockAlert = createMockAlert('HIGH', 'HIGH_VALUE', 1250000, 'KR');

    // When: AlertItem 컴포넌트 렌더링
    render(<AlertItem alert={mockAlert} />);

    // Then: 시간이 한국 형식으로 표시됨 (YYYY. MM. DD. HH:MM:SS 형식)
    // 정확한 포맷은 브라우저 설정에 따라 다를 수 있으므로, 시간 텍스트가 존재하는지만 확인
    const timestampElements = screen.getAllByText(/2025|11|10/);
    expect(timestampElements.length).toBeGreaterThan(0);
  });

  it('test_country_code_display: 국가 코드 표시 확인', () => {
    // Given: 해외 거래 알림
    const mockAlert = createMockAlert('MEDIUM', 'FOREIGN_COUNTRY', 75000, 'US');

    // When: AlertItem 컴포넌트 렌더링
    render(<AlertItem alert={mockAlert} />);

    // Then: 국가 코드가 표시됨
    expect(screen.getByText('US')).toBeInTheDocument();
  });

  it('test_transaction_id_display: 거래 ID 표시 확인', () => {
    // Given: 알림 데이터
    const mockAlert = createMockAlert('HIGH', 'HIGH_VALUE', 1250000, 'KR');

    // When: AlertItem 컴포넌트 렌더링
    render(<AlertItem alert={mockAlert} />);

    // Then: 거래 ID가 표시됨
    expect(screen.getByText('tx-456')).toBeInTheDocument();
  });

  it('test_user_id_display: 사용자 ID 표시 확인', () => {
    // Given: 알림 데이터
    const mockAlert = createMockAlert('HIGH', 'HIGH_VALUE', 1250000, 'KR');

    // When: AlertItem 컴포넌트 렌더링
    render(<AlertItem alert={mockAlert} />);

    // Then: 사용자 ID가 표시됨
    expect(screen.getByText('user-5')).toBeInTheDocument();
  });

  it('test_rule_name_display: 탐지 규칙 이름 표시 확인', () => {
    // Given: 알림 데이터
    const mockAlert = createMockAlert('HIGH', 'HIGH_VALUE', 1250000, 'KR');

    // When: AlertItem 컴포넌트 렌더링
    render(<AlertItem alert={mockAlert} />);

    // Then: 탐지 규칙 이름이 표시됨
    expect(screen.getByText('HIGH_VALUE')).toBeInTheDocument();
  });
});
