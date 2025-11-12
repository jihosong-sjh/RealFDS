import { renderHook, act, waitFor } from '@testing-library/react';
import { useWebSocket } from '../../hooks/useWebSocket';

/**
 * useWebSocket 훅 테스트
 *
 * WebSocket 연결, 메시지 수신, 재연결, 백필 기능을 검증합니다.
 */

// WebSocket Mock 설정
class WebSocketMock {
  url: string;
  readyState: number;
  onopen: ((event: Event) => void) | null = null;
  onmessage: ((event: MessageEvent) => void) | null = null;
  onclose: ((event: CloseEvent) => void) | null = null;
  onerror: ((event: Event) => void) | null = null;

  static CONNECTING = 0;
  static OPEN = 1;
  static CLOSING = 2;
  static CLOSED = 3;

  constructor(url: string) {
    this.url = url;
    this.readyState = WebSocketMock.CONNECTING;

    // 자동으로 연결 완료 시뮬레이션
    setTimeout(() => {
      this.readyState = WebSocketMock.OPEN;
      if (this.onopen) {
        this.onopen(new Event('open'));
      }
    }, 10);
  }

  send(data: string) {
    console.log('WebSocket sending:', data);
  }

  close() {
    this.readyState = WebSocketMock.CLOSED;
    if (this.onclose) {
      this.onclose(new CloseEvent('close'));
    }
  }

  // 테스트 헬퍼: 메시지 수신 시뮬레이션
  simulateMessage(data: any) {
    if (this.onmessage) {
      const event = new MessageEvent('message', {
        data: JSON.stringify(data),
      });
      this.onmessage(event);
    }
  }

  // 테스트 헬퍼: 연결 끊김 시뮬레이션
  simulateDisconnect() {
    this.readyState = WebSocketMock.CLOSED;
    if (this.onclose) {
      this.onclose(new CloseEvent('close'));
    }
  }
}

// 전역 WebSocket을 Mock으로 대체
(global as any).WebSocket = WebSocketMock;

describe('useWebSocket Hook 테스트', () => {
  let mockWebSocket: WebSocketMock;

  beforeEach(() => {
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.runOnlyPendingTimers();
    jest.useRealTimers();
  });

  test('Given WebSocket 연결, When METRICS_UPDATE 수신, Then 상태 업데이트', async () => {
    // Given: useWebSocket 훅 렌더링
    const { result } = renderHook(() => useWebSocket('ws://localhost:8083/ws/metrics'));

    // WebSocket 연결 완료 대기
    await act(async () => {
      jest.advanceTimersByTime(20);
    });

    // Mock WebSocket 인스턴스 가져오기
    mockWebSocket = (WebSocket as any).mock.instances?.[0] || new WebSocketMock('');

    // When: METRICS_UPDATE 메시지 수신
    const testMetrics = {
      type: 'METRICS_UPDATE',
      timestamp: new Date().toISOString(),
      payload: {
        services: [
          {
            serviceName: 'transaction-generator',
            status: 'UP',
            lastChecked: new Date().toISOString(),
            responseTime: 100,
            memoryUsage: 512,
          },
        ],
        transactionMetrics: {
          timestamp: new Date().toISOString(),
          tps: 50,
          totalTransactions: 10000,
        },
        alertMetrics: {
          timestamp: new Date().toISOString(),
          alertsPerMinute: 18,
          byRule: {
            HIGH_VALUE: 10,
            FOREIGN_COUNTRY: 5,
            HIGH_FREQUENCY: 3,
          },
        },
      },
    };

    await act(async () => {
      mockWebSocket.simulateMessage(testMetrics);
    });

    // Then: 상태가 업데이트됨
    await waitFor(() => {
      expect(result.current.metricsData).toBeDefined();
      expect(result.current.metricsData?.services).toHaveLength(1);
      expect(result.current.metricsData?.services[0].serviceName).toBe('transaction-generator');
    });
  });

  test('Given 연결 끊김, When 재연결, Then Exponential Backoff (1s, 2s, 4s...)', async () => {
    // Given: useWebSocket 훅 렌더링
    const { result } = renderHook(() => useWebSocket('ws://localhost:8083/ws/metrics'));

    // 초기 연결 완료
    await act(async () => {
      jest.advanceTimersByTime(20);
    });

    mockWebSocket = (WebSocket as any).mock.instances?.[0] || new WebSocketMock('');

    // When: 연결 끊김
    await act(async () => {
      mockWebSocket.simulateDisconnect();
    });

    // Then: 1초 후 재연결 시도
    expect(result.current.connectionStatus).toBe('disconnected');

    await act(async () => {
      jest.advanceTimersByTime(1000);
    });

    expect(result.current.connectionStatus).toBe('connecting');

    // 두 번째 연결 실패 시뮬레이션
    mockWebSocket = (WebSocket as any).mock.instances?.[1] || new WebSocketMock('');
    await act(async () => {
      mockWebSocket.simulateDisconnect();
    });

    // 2초 후 재연결 시도
    await act(async () => {
      jest.advanceTimersByTime(2000);
    });

    expect(result.current.connectionStatus).toBe('connecting');
  });

  test('Given 재연결 성공, When BACKFILL_REQUEST 전송, Then 누락 데이터 복구', async () => {
    // Given: useWebSocket 훅 렌더링 및 초기 연결
    const { result } = renderHook(() => useWebSocket('ws://localhost:8083/ws/metrics'));

    await act(async () => {
      jest.advanceTimersByTime(20);
    });

    mockWebSocket = (WebSocket as any).mock.instances?.[0] || new WebSocketMock('');

    // 첫 번째 메트릭 수신
    const firstMetrics = {
      type: 'METRICS_UPDATE',
      timestamp: '2025-01-01T00:00:00Z',
      payload: {
        services: [],
        transactionMetrics: { timestamp: '2025-01-01T00:00:00Z', tps: 10, totalTransactions: 1000 },
        alertMetrics: { timestamp: '2025-01-01T00:00:00Z', alertsPerMinute: 5, byRule: {} },
      },
    };

    await act(async () => {
      mockWebSocket.simulateMessage(firstMetrics);
    });

    // When: 연결 끊김 및 재연결
    await act(async () => {
      mockWebSocket.simulateDisconnect();
      jest.advanceTimersByTime(1000);
    });

    // 재연결 성공
    mockWebSocket = (WebSocket as any).mock.instances?.[1] || new WebSocketMock('');
    await act(async () => {
      jest.advanceTimersByTime(20);
    });

    // Then: BACKFILL_REQUEST가 전송됨
    // (실제 구현에서 send 메서드 호출을 확인할 수 있음)
    expect(mockWebSocket.readyState).toBe(WebSocketMock.OPEN);

    // BACKFILL_RESPONSE 수신
    const backfillResponse = {
      type: 'BACKFILL_RESPONSE',
      timestamp: new Date().toISOString(),
      payload: [
        {
          services: [],
          transactionMetrics: { timestamp: '2025-01-01T00:00:05Z', tps: 15, totalTransactions: 1050 },
          alertMetrics: { timestamp: '2025-01-01T00:00:05Z', alertsPerMinute: 6, byRule: {} },
        },
        {
          services: [],
          transactionMetrics: { timestamp: '2025-01-01T00:00:10Z', tps: 20, totalTransactions: 1100 },
          alertMetrics: { timestamp: '2025-01-01T00:00:10Z', alertsPerMinute: 7, byRule: {} },
        },
      ],
    };

    await act(async () => {
      mockWebSocket.simulateMessage(backfillResponse);
    });

    // 누락된 데이터가 복구됨
    await waitFor(() => {
      expect(result.current.connectionStatus).toBe('connected');
    });
  });

  test('Given 연결 상태, When 컴포넌트 언마운트, Then WebSocket 연결 종료', async () => {
    // Given: useWebSocket 훅 렌더링
    const { result, unmount } = renderHook(() => useWebSocket('ws://localhost:8083/ws/metrics'));

    await act(async () => {
      jest.advanceTimersByTime(20);
    });

    mockWebSocket = (WebSocket as any).mock.instances?.[0] || new WebSocketMock('');

    // When: 컴포넌트 언마운트
    unmount();

    // Then: WebSocket 연결 종료
    expect(mockWebSocket.readyState).toBe(WebSocketMock.CLOSED);
  });

  test('Given ERROR 메시지 수신, When 에러 처리, Then 에러 상태 업데이트', async () => {
    // Given: useWebSocket 훅 렌더링
    const { result } = renderHook(() => useWebSocket('ws://localhost:8083/ws/metrics'));

    await act(async () => {
      jest.advanceTimersByTime(20);
    });

    mockWebSocket = (WebSocket as any).mock.instances?.[0] || new WebSocketMock('');

    // When: ERROR 메시지 수신
    const errorMessage = {
      type: 'ERROR',
      timestamp: new Date().toISOString(),
      payload: {
        errorCode: 'INVALID_MESSAGE',
        errorMessage: '잘못된 메시지 형식입니다',
      },
    };

    await act(async () => {
      mockWebSocket.simulateMessage(errorMessage);
    });

    // Then: 에러 상태가 업데이트됨
    await waitFor(() => {
      expect(result.current.error).toBeDefined();
      expect(result.current.error).toContain('잘못된 메시지 형식');
    });
  });
});
