import { useState, useEffect, useRef, useCallback } from 'react';
import { MetricsDataPoint, WebSocketMessage, WebSocketMessageType } from '../types/metrics';

/**
 * useWebSocket 커스텀 훅
 *
 * WebSocket 연결을 관리하고 메트릭 데이터를 실시간으로 수신합니다.
 * 연결 끊김 시 Exponential Backoff 전략으로 자동 재연결하며,
 * 재연결 시 누락된 데이터를 백필 요청합니다.
 *
 * @param url WebSocket 서버 URL (예: ws://localhost:8083/ws/metrics)
 * @returns WebSocket 상태 및 메트릭 데이터
 */

export type ConnectionStatus = 'connecting' | 'connected' | 'disconnected' | 'error';

export interface UseWebSocketReturn {
  /** 현재 메트릭 데이터 포인트 */
  metricsData: MetricsDataPoint | null;
  /** WebSocket 연결 상태 */
  connectionStatus: ConnectionStatus;
  /** 에러 메시지 (있을 경우) */
  error: string | null;
  /** 마지막 업데이트 시각 */
  lastUpdate: string | null;
}

export const useWebSocket = (url: string): UseWebSocketReturn => {
  const [metricsData, setMetricsData] = useState<MetricsDataPoint | null>(null);
  const [connectionStatus, setConnectionStatus] = useState<ConnectionStatus>('connecting');
  const [error, setError] = useState<string | null>(null);
  const [lastUpdate, setLastUpdate] = useState<string | null>(null);

  const wsRef = useRef<WebSocket | null>(null);
  const reconnectTimeoutRef = useRef<NodeJS.Timeout | null>(null);
  const reconnectAttemptsRef = useRef<number>(0);
  const lastReceivedTimestampRef = useRef<string | null>(null);

  /**
   * Exponential Backoff 재연결 지연 계산
   * 1s → 2s → 4s → 8s → 16s → 32s (최대)
   */
  const calculateReconnectDelay = useCallback((): number => {
    const baseDelay = 1000; // 1초
    const maxDelay = 32000; // 32초
    const delay = Math.min(baseDelay * Math.pow(2, reconnectAttemptsRef.current), maxDelay);
    return delay;
  }, []);

  /**
   * BACKFILL_REQUEST 메시지 전송
   * 재연결 시 마지막으로 수신한 메트릭 이후의 데이터를 요청합니다.
   */
  const sendBackfillRequest = useCallback((ws: WebSocket) => {
    if (lastReceivedTimestampRef.current) {
      const backfillRequest: WebSocketMessage = {
        type: 'BACKFILL_REQUEST' as WebSocketMessageType,
        timestamp: new Date().toISOString(),
        lastReceivedTimestamp: lastReceivedTimestampRef.current,
      };

      ws.send(JSON.stringify(backfillRequest));
      console.log('[useWebSocket] BACKFILL_REQUEST 전송:', lastReceivedTimestampRef.current);
    }
  }, []);

  /**
   * WebSocket 연결 수립
   */
  const connect = useCallback(() => {
    try {
      console.log('[useWebSocket] 연결 시도 중...', { url, attempts: reconnectAttemptsRef.current });
      setConnectionStatus('connecting');
      setError(null);

      const ws = new WebSocket(url);
      wsRef.current = ws;

      ws.onopen = () => {
        console.log('[useWebSocket] 연결 성공');
        setConnectionStatus('connected');
        reconnectAttemptsRef.current = 0; // 재연결 카운터 리셋

        // 재연결 시 백필 요청
        sendBackfillRequest(ws);
      };

      ws.onmessage = (event: MessageEvent) => {
        try {
          // Ping-pong heartbeat 메시지는 무시
          if (event.data === 'ping' || event.data === 'pong') {
            return;
          }

          const message: WebSocketMessage = JSON.parse(event.data);

          switch (message.type) {
            case 'METRICS_UPDATE':
              // 실시간 메트릭 업데이트
              setMetricsData(message.payload as MetricsDataPoint);
              setLastUpdate(message.timestamp);
              lastReceivedTimestampRef.current = message.timestamp;
              console.log('[useWebSocket] METRICS_UPDATE 수신:', message.timestamp);
              break;

            case 'BACKFILL_RESPONSE':
              // 누락된 데이터 백필
              const backfillData = message.payload as MetricsDataPoint[];
              if (backfillData && backfillData.length > 0) {
                // 가장 최신 데이터만 상태로 업데이트
                const latestData = backfillData[backfillData.length - 1];
                setMetricsData(latestData);
                setLastUpdate(message.timestamp);
                lastReceivedTimestampRef.current = latestData.timestamp;
                console.log('[useWebSocket] BACKFILL_RESPONSE 수신:', backfillData.length, '개 데이터');
              }
              break;

            case 'ERROR':
              // 서버 에러 메시지
              const errorPayload = message.payload as { errorCode: string; errorMessage: string };
              setError(errorPayload.errorMessage);
              console.error('[useWebSocket] 서버 에러:', errorPayload);
              break;

            default:
              console.warn('[useWebSocket] 알 수 없는 메시지 타입:', message.type);
          }
        } catch (err) {
          console.error('[useWebSocket] 메시지 파싱 실패:', err);
          setError('메시지 파싱 중 오류가 발생했습니다');
        }
      };

      ws.onclose = (event: CloseEvent) => {
        console.log('[useWebSocket] 연결 종료:', event.code, event.reason);
        setConnectionStatus('disconnected');
        wsRef.current = null;

        // Exponential Backoff 재연결
        const delay = calculateReconnectDelay();
        reconnectAttemptsRef.current += 1;

        console.log('[useWebSocket] 재연결 예정:', { attempts: reconnectAttemptsRef.current, delay });

        reconnectTimeoutRef.current = setTimeout(() => {
          connect();
        }, delay);
      };

      ws.onerror = (event: Event) => {
        console.error('[useWebSocket] WebSocket 오류:', event);
        setConnectionStatus('error');
        setError('WebSocket 연결 중 오류가 발생했습니다');
      };
    } catch (err) {
      console.error('[useWebSocket] 연결 실패:', err);
      setConnectionStatus('error');
      setError('WebSocket 연결을 초기화할 수 없습니다');
    }
  }, [url, calculateReconnectDelay, sendBackfillRequest]);

  /**
   * 컴포넌트 마운트 시 연결, 언마운트 시 연결 해제
   */
  useEffect(() => {
    connect();

    return () => {
      // 재연결 타이머 정리
      if (reconnectTimeoutRef.current) {
        clearTimeout(reconnectTimeoutRef.current);
        reconnectTimeoutRef.current = null;
      }

      // WebSocket 연결 종료
      if (wsRef.current) {
        console.log('[useWebSocket] 연결 종료 (cleanup)');
        wsRef.current.close();
        wsRef.current = null;
      }
    };
  }, [connect]);

  /**
   * 브라우저 탭 활성화 시 백필 요청
   * 사용자가 탭을 전환했다가 돌아왔을 때 누락된 데이터를 복구합니다.
   */
  useEffect(() => {
    const handleVisibilityChange = () => {
      if (document.visibilityState === 'visible' && wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
        console.log('[useWebSocket] 탭 활성화: 백필 요청');
        sendBackfillRequest(wsRef.current);
      }
    };

    document.addEventListener('visibilitychange', handleVisibilityChange);

    return () => {
      document.removeEventListener('visibilitychange', handleVisibilityChange);
    };
  }, [sendBackfillRequest]);

  return {
    metricsData,
    connectionStatus,
    error,
    lastUpdate,
  };
};
