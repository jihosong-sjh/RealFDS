import { useState, useEffect, useRef } from 'react';
import type { Alert } from '../types/alert';
import type { ConnectionStatus } from '../types/connectionStatus';

/**
 * useWebSocket Hook: WebSocket 연결 관리 및 실시간 알림 수신
 *
 * @param url - WebSocket 서버 URL (예: "ws://localhost:8082/ws/alerts")
 * @returns alerts (최근 100개 알림 목록), connectionStatus (연결 상태)
 */
export function useWebSocket(url: string) {
  // 상태 관리: 알림 목록 (최근 100개만 유지)
  const [alerts, setAlerts] = useState<Alert[]>([]);

  // 상태 관리: 연결 상태
  const [connectionStatus, setConnectionStatus] = useState<ConnectionStatus>('disconnected');

  // 재연결 시도 횟수 추적
  const reconnectAttempts = useRef<number>(0);

  // WebSocket 인스턴스 참조
  const wsRef = useRef<WebSocket | null>(null);

  // 재연결 타이머 참조
  const reconnectTimerRef = useRef<number | null>(null);

  useEffect(() => {
    // WebSocket 연결 수립
    const connectWebSocket = () => {
      try {
        setConnectionStatus('connecting');
        const ws = new WebSocket(url);
        wsRef.current = ws;

        // 연결 성공 이벤트
        ws.onopen = () => {
          console.log('[WebSocket] 연결 성공:', url);
          setConnectionStatus('connected');
          reconnectAttempts.current = 0; // 재연결 카운터 초기화
        };

        // 메시지 수신 이벤트
        ws.onmessage = (event) => {
          try {
            const alert: Alert = JSON.parse(event.data);
            console.log('[WebSocket] 알림 수신:', alert.alertId, alert.ruleName);

            // 알림 추가 (최신 알림이 맨 앞, 최근 100개만 유지)
            setAlerts((prev) => {
              const updated = [alert, ...prev];
              return updated.slice(0, 100); // 최근 100개만 유지
            });
          } catch (error) {
            console.error('[WebSocket] 메시지 파싱 실패:', error);
          }
        };

        // 연결 종료 이벤트
        ws.onclose = (event) => {
          console.warn('[WebSocket] 연결 종료:', event.code, event.reason);
          setConnectionStatus('disconnected');
          wsRef.current = null;

          // 5초 후 자동 재연결 시도
          reconnectAttempts.current += 1;
          console.log(`[WebSocket] 재연결 시도 예정 (${reconnectAttempts.current}회차, 5초 후)`);

          reconnectTimerRef.current = window.setTimeout(() => {
            if (reconnectAttempts.current <= 10) { // 최대 10회 재시도
              connectWebSocket();
            } else {
              console.error('[WebSocket] 최대 재연결 시도 횟수 초과 (10회)');
            }
          }, 5000);
        };

        // 에러 이벤트
        ws.onerror = (error) => {
          console.error('[WebSocket] 에러 발생:', error);
        };
      } catch (error) {
        console.error('[WebSocket] 연결 실패:', error);
        setConnectionStatus('disconnected');
      }
    };

    // 초기 연결 시작
    connectWebSocket();

    // 클린업 함수: 컴포넌트 언마운트 시 WebSocket 종료
    return () => {
      if (wsRef.current) {
        wsRef.current.close();
        wsRef.current = null;
      }
      if (reconnectTimerRef.current) {
        clearTimeout(reconnectTimerRef.current);
        reconnectTimerRef.current = null;
      }
    };
  }, [url]);

  return { alerts, connectionStatus };
}
