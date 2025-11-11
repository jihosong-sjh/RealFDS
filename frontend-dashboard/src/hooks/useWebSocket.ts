import { useState, useEffect, useRef } from 'react';
import type { Alert } from '../types/alert';
import type { ConnectionState as ConnectionStatusType } from '../types/connectionStatus';

/**
 * useWebSocket Hook: WebSocket 연결 관리 및 실시간 알림 수신
 *
 * @param url - WebSocket 서버 URL (예: "ws://localhost:8082/ws/alerts")
 * @param alertServiceUrl - alert-service REST API URL (예: "http://localhost:8081")
 * @returns alerts (최근 100개 알림 목록), connectionState (연결 상태 정보)
 */
export function useWebSocket(url: string, alertServiceUrl: string) {
  // 상태 관리: 알림 목록 (최근 100개만 유지)
  const [alerts, setAlerts] = useState<Alert[]>([]);

  // 상태 관리: 연결 상태 정보
  const [connectionState, setConnectionState] = useState<ConnectionStatusType>({
    status: 'disconnected',
    reconnectAttempts: 0,
  });

  // WebSocket 인스턴스 참조
  const wsRef = useRef<WebSocket | null>(null);

  // 재연결 타이머 참조
  const reconnectTimerRef = useRef<number | null>(null);

  // 초기 데이터 로드 여부 추적
  const initialLoadRef = useRef<boolean>(false);

  // 초기 알림 데이터 로드
  useEffect(() => {
    const loadInitialAlerts = async () => {
      if (initialLoadRef.current) {
        return; // 이미 로드됨
      }

      try {
        console.log('[useWebSocket] 초기 알림 데이터 로드 시작:', alertServiceUrl);
        const response = await fetch(`${alertServiceUrl}/api/alerts`);

        if (!response.ok) {
          throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }

        const data: Alert[] = await response.json();
        console.log(`[useWebSocket] 초기 알림 ${data.length}개 로드 완료`);

        setAlerts(data.slice(0, 100)); // 최근 100개만 유지
        initialLoadRef.current = true;
      } catch (error) {
        console.error('[useWebSocket] 초기 알림 로드 실패:', error);
        // 로드 실패 시에도 WebSocket 연결은 계속 시도
      }
    };

    loadInitialAlerts();
  }, [alertServiceUrl]);

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
            const message = JSON.parse(event.data);

            // 002-alert-management: 이벤트 타입별 처리
            if (message.type === 'ALERT_STATUS_CHANGED') {
              // User Story 2: 알림 상태 변경 이벤트 처리 (assignedTo, actionNote 포함)
              console.log('[WebSocket] 상태 변경 이벤트 수신:', message.alertId, message.status, message.assignedTo, message.actionNote);

              setAlerts((prev) =>
                prev.map((alert) =>
                  alert.alertId === message.alertId
                    ? {
                        ...alert,
                        status: message.status,
                        processedAt: message.processedAt || alert.processedAt,
                        assignedTo: message.assignedTo !== undefined ? message.assignedTo : alert.assignedTo,
                        actionNote: message.actionNote !== undefined ? message.actionNote : alert.actionNote,
                      }
                    : alert
                )
              );
            } else {
              // 기존 NEW_ALERT 이벤트 처리 (message가 Alert 객체인 경우)
              const alert: Alert = message as Alert;
              console.log('[WebSocket] 신규 알림 수신:', alert.alertId, alert.ruleName);

              // 알림 추가 (중복 체크 후 최신 알림이 맨 앞, 최근 100개만 유지)
              setAlerts((prev) => {
                // 중복 체크: 이미 존재하는 alertId인지 확인
                const exists = prev.some((existing) => existing.alertId === alert.alertId);
                if (exists) {
                  console.log('[WebSocket] 중복 알림 무시:', alert.alertId);
                  return prev; // 중복이면 추가하지 않음
                }

                const updated = [alert, ...prev];
                return updated.slice(0, 100); // 최근 100개만 유지
              });
            }
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
