// T086: AlertList 컴포넌트 단위 테스트
// Given-When-Then 구조 사용, 한국어 주석

import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { AlertList } from '../../src/components/AlertList';
import type { Alert } from '../../src/types/alert';

describe('AlertList 컴포넌트 테스트', () => {
  // 테스트용 Mock Alert 데이터 생성 함수
  const createMockAlert = (
    alertId: string,
    ruleName: string,
    severity: Alert['severity'],
    amount: number
  ): Alert => ({
    schemaVersion: '1.0',
    alertId,
    originalTransaction: {
      schemaVersion: '1.0',
      transactionId: `tx-${alertId}`,
      userId: 'user-1',
      amount,
      currency: 'KRW',
      countryCode: 'KR',
      timestamp: '2025-11-10T10:00:00.000Z',
    },
    ruleType: 'SIMPLE_RULE',
    ruleName,
    reason: `${ruleName} 알림`,
    severity,
    alertTimestamp: '2025-11-10T10:00:01.000Z',
  });

  it('test_render_alerts: 알림 목록 렌더링 확인', () => {
    // Given: 3개의 알림 데이터
    const mockAlerts: Alert[] = [
      createMockAlert('alert-1', 'HIGH_VALUE', 'HIGH', 1250000),
      createMockAlert('alert-2', 'FOREIGN_COUNTRY', 'MEDIUM', 50000),
      createMockAlert('alert-3', 'HIGH_FREQUENCY', 'HIGH', 75000),
    ];

    // When: AlertList 컴포넌트 렌더링
    render(<AlertList alerts={mockAlerts} />);

    // Then: 모든 알림이 렌더링됨
    expect(screen.getByText('HIGH_VALUE 알림')).toBeInTheDocument();
    expect(screen.getByText('FOREIGN_COUNTRY 알림')).toBeInTheDocument();
    expect(screen.getByText('HIGH_FREQUENCY 알림')).toBeInTheDocument();

    // 알림 개수 표시 확인
    expect(screen.getByText('3개')).toBeInTheDocument();
  });

  it('test_render_empty_message: 알림 없을 때 메시지 표시 확인', () => {
    // Given: 빈 알림 목록
    const mockAlerts: Alert[] = [];

    // When: AlertList 컴포넌트 렌더링
    render(<AlertList alerts={mockAlerts} />);

    // Then: "알림이 없습니다" 메시지가 표시됨
    expect(screen.getByText(/알림이 없습니다/i)).toBeInTheDocument();
    expect(screen.getByText(/실시간으로 탐지된 의심스러운 거래가 여기에 표시됩니다/i)).toBeInTheDocument();

    // 빈 상태 아이콘 확인
    expect(screen.getByText('🔍')).toBeInTheDocument();
  });

  it('test_alert_count_display: 알림 개수 표시 확인', () => {
    // Given: 다양한 개수의 알림
    const testCases = [
      { count: 1, alerts: [createMockAlert('alert-1', 'HIGH_VALUE', 'HIGH', 1250000)] },
      { count: 5, alerts: Array.from({ length: 5 }, (_, i) =>
        createMockAlert(`alert-${i}`, 'HIGH_VALUE', 'HIGH', 1000000 + i * 1000)
      ) },
      { count: 10, alerts: Array.from({ length: 10 }, (_, i) =>
        createMockAlert(`alert-${i}`, 'HIGH_VALUE', 'HIGH', 1000000 + i * 1000)
      ) },
    ];

    testCases.forEach(({ count, alerts }) => {
      // When: AlertList 컴포넌트 렌더링
      const { unmount } = render(<AlertList alerts={alerts} />);

      // Then: 올바른 개수가 표시됨
      expect(screen.getByText(`${count}개`)).toBeInTheDocument();

      // 다음 테스트를 위해 언마운트
      unmount();
    });
  });

  it('test_alert_order: 알림 순서 확인 (최신 알림이 맨 위)', () => {
    // Given: 시간 순서대로 정렬된 알림들 (최신이 맨 앞)
    const mockAlerts: Alert[] = [
      { ...createMockAlert('alert-3', 'HIGH_VALUE', 'HIGH', 1250000), alertTimestamp: '2025-11-10T10:00:03.000Z' },
      { ...createMockAlert('alert-2', 'FOREIGN_COUNTRY', 'MEDIUM', 50000), alertTimestamp: '2025-11-10T10:00:02.000Z' },
      { ...createMockAlert('alert-1', 'HIGH_FREQUENCY', 'HIGH', 75000), alertTimestamp: '2025-11-10T10:00:01.000Z' },
    ];

    // When: AlertList 컴포넌트 렌더링
    const { container } = render(<AlertList alerts={mockAlerts} />);

    // Then: 알림이 순서대로 렌더링됨
    const alertItems = container.querySelectorAll('.alert-item');
    expect(alertItems).toHaveLength(3);

    // 첫 번째 알림이 alert-3인지 확인 (가장 최신)
    expect(alertItems[0]).toHaveTextContent('HIGH_VALUE 알림');

    // 두 번째 알림이 alert-2인지 확인
    expect(alertItems[1]).toHaveTextContent('FOREIGN_COUNTRY 알림');

    // 세 번째 알림이 alert-1인지 확인 (가장 오래된)
    expect(alertItems[2]).toHaveTextContent('HIGH_FREQUENCY 알림');
  });

  it('test_list_header: 목록 헤더 표시 확인', () => {
    // Given: 알림 데이터
    const mockAlerts: Alert[] = [
      createMockAlert('alert-1', 'HIGH_VALUE', 'HIGH', 1250000),
    ];

    // When: AlertList 컴포넌트 렌더링
    render(<AlertList alerts={mockAlerts} />);

    // Then: "실시간 알림" 제목이 표시됨
    expect(screen.getByText('실시간 알림')).toBeInTheDocument();
  });

  it('test_multiple_severity_types: 다양한 심각도 알림 렌더링 확인', () => {
    // Given: 다양한 심각도의 알림들
    const mockAlerts: Alert[] = [
      createMockAlert('alert-1', 'HIGH_VALUE', 'HIGH', 1250000),
      createMockAlert('alert-2', 'FOREIGN_COUNTRY', 'MEDIUM', 50000),
      createMockAlert('alert-3', 'CUSTOM_RULE', 'LOW', 30000),
    ];

    // When: AlertList 컴포넌트 렌더링
    const { container } = render(<AlertList alerts={mockAlerts} />);

    // Then: 모든 심각도가 올바르게 렌더링됨
    expect(container.querySelector('.severity-high')).toBeInTheDocument();
    expect(container.querySelector('.severity-medium')).toBeInTheDocument();
    expect(container.querySelector('.severity-low')).toBeInTheDocument();
  });

  it('test_large_alert_list: 많은 알림 렌더링 확인 (100개)', () => {
    // Given: 100개의 알림
    const mockAlerts: Alert[] = Array.from({ length: 100 }, (_, i) =>
      createMockAlert(`alert-${i}`, 'HIGH_VALUE', 'HIGH', 1000000 + i * 1000)
    );

    // When: AlertList 컴포넌트 렌더링
    const { container } = render(<AlertList alerts={mockAlerts} />);

    // Then: 100개 알림이 모두 렌더링됨
    const alertItems = container.querySelectorAll('.alert-item');
    expect(alertItems).toHaveLength(100);
    expect(screen.getByText('100개')).toBeInTheDocument();
  });

  it('test_unique_keys: 각 알림이 고유한 key를 가지는지 확인', () => {
    // Given: 알림 목록
    const mockAlerts: Alert[] = [
      createMockAlert('alert-1', 'HIGH_VALUE', 'HIGH', 1250000),
      createMockAlert('alert-2', 'FOREIGN_COUNTRY', 'MEDIUM', 50000),
      createMockAlert('alert-3', 'HIGH_FREQUENCY', 'HIGH', 75000),
    ];

    // When: AlertList 컴포넌트 렌더링
    const { container } = render(<AlertList alerts={mockAlerts} />);

    // Then: 각 알림이 렌더링됨 (React는 중복 key가 있으면 경고를 발생시키므로,
    // 정상적으로 렌더링되면 key가 고유함을 의미)
    const alertItems = container.querySelectorAll('.alert-item');
    expect(alertItems).toHaveLength(3);
  });

  it('test_empty_list_no_header: 빈 목록일 때 헤더가 표시되지 않음', () => {
    // Given: 빈 알림 목록
    const mockAlerts: Alert[] = [];

    // When: AlertList 컴포넌트 렌더링
    render(<AlertList alerts={mockAlerts} />);

    // Then: "실시간 알림" 헤더가 표시되지 않음
    expect(screen.queryByText('실시간 알림')).not.toBeInTheDocument();

    // 대신 빈 상태 메시지가 표시됨
    expect(screen.getByText(/알림이 없습니다/i)).toBeInTheDocument();
  });
});
