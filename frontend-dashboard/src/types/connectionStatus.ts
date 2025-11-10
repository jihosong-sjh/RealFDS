// ConnectionStatus 타입 정의: WebSocket 연결 상태 관리

/**
 * ConnectionStatus 타입: WebSocket 연결 상태
 * - connected: 연결됨
 * - disconnected: 연결 끊김
 * - connecting: 연결 시도 중
 */
export type ConnectionStatus = 'connected' | 'disconnected' | 'connecting';

/**
 * ConnectionState 인터페이스: 연결 상태 상세 정보
 */
export interface ConnectionState {
  status: ConnectionStatus;       // 현재 연결 상태
  lastConnectedAt?: string;       // 마지막 연결 시각 (ISO 8601, 선택적)
  reconnectAttempts: number;      // 재연결 시도 횟수
}
