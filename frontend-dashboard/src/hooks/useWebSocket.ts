import { useState, useEffect, useRef } from 'react';
import type { Alert } from '../types/alert';
import type { ConnectionState, ConnectionStatus } from '../types/connectionStatus';

/**
 * useWebSocket Hook: WebSocket 연결 관리 및 실시간 알림 수신
 *
 * @param url - WebSocket 서버 URL (예: "ws://localhost:8082/ws/alerts")
 * @returns alerts (최근 100개 알림 목록), connectionState (연결 상태 정보)
 */
export function useWebSocket(url: string) {
  // 상태 관리: 알림 목록 (최근 100개만 유지)
  const [alerts, setAlerts] = useState<Alert[]>([]);

  // 상태 관리: 연결 상태 정보
  const [connectionState, setConnectionState] = useState<ConnectionState>({
    status: 'disconnected',
    reconnectAttempts: 0,
  });

  // WebSocket 인스턴스 참조
  const wsRef = useRef<WebSocket | null>(null);

  // 재연결 타이머 참조
  const reconnectTimerRef = useRef<number | null>(null);

  useEffect(() => {
    // WebSocket 연결 수립
    const connectWebSocket = () => {
      try {
        setConnectionState((prev) => ({ ...prev, status: 'connecting' }));
        const ws = new WebSocket(url);
        wsRef.current = ws;

        // 연결 성공 이벤트
        ws.onopen = () => {
          console.log('[WebSocket] 연결 성공:', url);
          setConnectionState({
            status: 'connected',
            lastConnectedAt: new Date().toISOString(),
            reconnectAttempts: 0,
          });
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
          wsRef.current = null;

          // 5초 후 자동 재연결 시도
          setConnectionState((prev) => {
            const newAttempts = prev.reconnectAttempts + 1;
            console.log(`[WebSocket] 재연결 시도 예정 (${newAttempts}회차, 5초 후)`);

            reconnectTimerRef.current = window.setTimeout(() => {
              if (newAttempts <= 10) { // 최대 10회 재시도
                connectWebSocket();
              } else {
                console.error('[WebSocket] 최대 재연결 시도 횟수 초과 (10회)');
              }
            }, 5000);

            return {
              ...prev,
              status: 'disconnected',
              reconnectAttempts: newAttempts,
            };
          });
        };

        // 에러 이벤트
        ws.onerror = (error) => {
          console.error('[WebSocket] 에러 발생:', error);
        };
      } catch (error) {
        console.error('[WebSocket] 연결 실패:', error);
        setConnectionState((prev) => ({ ...prev, status: 'disconnected' }));
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

  return { alerts, connectionState };
}
