// T084: useWebSocket hook 단위 테스트
// Given-When-Then 구조 사용, 한국어 주석

import { renderHook, waitFor } from '@testing-library/react';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { useWebSocket } from '../../src/hooks/useWebSocket';
import type { Alert } from '../../src/types/alert';

describe('useWebSocket Hook 테스트', () => {
  const TEST_URL = 'ws://localhost:8082/ws/alerts';

  // 테스트용 Mock Alert 데이터
  const createMockAlert = (alertId: string, ruleName: string): Alert => ({
    schemaVersion: '1.0',
    alertId,
    originalTransaction: {
      schemaVersion: '1.0',
      transactionId: 'tx-123',
      userId: 'user-1',
      amount: 1250000,
      currency: 'KRW',
      countryCode: 'KR',
      timestamp: '2025-11-10T10:00:00.000Z',
    },
    ruleType: 'SIMPLE_RULE',
    ruleName,
    reason: '고액 거래 (100만원 초과): 1,250,000원',
    severity: 'HIGH',
    alertTimestamp: '2025-11-10T10:00:01.000Z',
  });

  beforeEach(() => {
    // 각 테스트 전에 Mock 리셋
    vi.clearAllMocks();
  });

  it('test_initial_state: 초기 상태 확인 (disconnected, 빈 alerts 배열)', async () => {
    // Given: useWebSocket hook을 사용하는 컴포넌트

    // When: hook이 초기화됨
    const { result } = renderHook(() => useWebSocket(TEST_URL));

    // Then: 초기 상태는 disconnected이고 alerts는 빈 배열
    expect(result.current.connectionStatus).toBe('connecting'); // 최초에는 connecting
    expect(result.current.alerts).toEqual([]);
  });

  it('test_connection_established: WebSocket 연결 시 상태 변경 확인', async () => {
    // Given: useWebSocket hook 렌더링
    const { result } = renderHook(() => useWebSocket(TEST_URL));

    // When: WebSocket onopen 이벤트 트리거
    const ws = (global.WebSocket as any).mock.results[0]?.value;
    if (ws && ws.onopen) {
      ws.readyState = WebSocket.OPEN;
      ws.onopen(new Event('open'));
    }

    // Then: 연결 상태가 'connected'로 변경
    await waitFor(() => {
      expect(result.current.connectionStatus).toBe('connected');
    });
  });

  it('test_message_received: WebSocket 메시지 수신 시 alerts 배열에 추가', async () => {
    // Given: WebSocket이 연결된 상태
    const { result } = renderHook(() => useWebSocket(TEST_URL));
    const ws = (global.WebSocket as any).mock.results[0]?.value;

    if (ws && ws.onopen) {
      ws.readyState = WebSocket.OPEN;
      ws.onopen(new Event('open'));
    }

    await waitFor(() => {
      expect(result.current.connectionStatus).toBe('connected');
    });

    // When: 알림 메시지 수신
    const mockAlert = createMockAlert('alert-1', 'HIGH_VALUE');
    if (ws && ws.onmessage) {
      ws.onmessage(new MessageEvent('message', {
        data: JSON.stringify(mockAlert)
      }));
    }

    // Then: alerts 배열에 알림이 추가됨
    await waitFor(() => {
      expect(result.current.alerts).toHaveLength(1);
      expect(result.current.alerts[0].alertId).toBe('alert-1');
    });
  });

  it('test_max_alerts_100: 알림이 100개를 초과하면 가장 오래된 알림 제거', async () => {
    // Given: WebSocket이 연결된 상태
    const { result } = renderHook(() => useWebSocket(TEST_URL));
    const ws = (global.WebSocket as any).mock.results[0]?.value;

    if (ws && ws.onopen) {
      ws.readyState = WebSocket.OPEN;
      ws.onopen(new Event('open'));
    }

    await waitFor(() => {
      expect(result.current.connectionStatus).toBe('connected');
    });

    // When: 101개의 알림을 수신
    if (ws && ws.onmessage) {
      for (let i = 1; i <= 101; i++) {
        const mockAlert = createMockAlert(`alert-${i}`, 'HIGH_VALUE');
        ws.onmessage(new MessageEvent('message', {
          data: JSON.stringify(mockAlert)
        }));
      }
    }

    // Then: alerts 배열은 최근 100개만 유지
    await waitFor(() => {
      expect(result.current.alerts).toHaveLength(100);
      // 가장 최신 알림은 alert-101
      expect(result.current.alerts[0].alertId).toBe('alert-101');
      // 가장 오래된 알림은 alert-2 (alert-1은 제거됨)
      expect(result.current.alerts[99].alertId).toBe('alert-2');
    });
  });

  it('test_connection_closed: WebSocket 연결 종료 시 상태 변경', async () => {
    // Given: WebSocket이 연결된 상태
    const { result } = renderHook(() => useWebSocket(TEST_URL));
    const ws = (global.WebSocket as any).mock.results[0]?.value;

    if (ws && ws.onopen) {
      ws.readyState = WebSocket.OPEN;
      ws.onopen(new Event('open'));
    }

    await waitFor(() => {
      expect(result.current.connectionStatus).toBe('connected');
    });

    // When: WebSocket onclose 이벤트 트리거
    if (ws && ws.onclose) {
      ws.readyState = WebSocket.CLOSED;
      ws.onclose(new CloseEvent('close', { code: 1000, reason: 'Normal closure' }));
    }

    // Then: 연결 상태가 'disconnected'로 변경
    await waitFor(() => {
      expect(result.current.connectionStatus).toBe('disconnected');
    });
  });

  it('test_invalid_json: 잘못된 JSON 메시지 수신 시 에러 처리', async () => {
    // Given: WebSocket이 연결된 상태
    const { result } = renderHook(() => useWebSocket(TEST_URL));
    const ws = (global.WebSocket as any).mock.results[0]?.value;

    if (ws && ws.onopen) {
      ws.readyState = WebSocket.OPEN;
      ws.onopen(new Event('open'));
    }

    await waitFor(() => {
      expect(result.current.connectionStatus).toBe('connected');
    });

    const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

    // When: 잘못된 JSON 메시지 수신
    if (ws && ws.onmessage) {
      ws.onmessage(new MessageEvent('message', {
        data: 'invalid json {{{'
      }));
    }

    // Then: alerts 배열은 변경되지 않고, 에러가 로깅됨
    await waitFor(() => {
      expect(result.current.alerts).toHaveLength(0);
      expect(consoleSpy).toHaveBeenCalledWith(
        expect.stringContaining('[WebSocket] 메시지 파싱 실패'),
        expect.any(Error)
      );
    });

    consoleSpy.mockRestore();
  });
});
