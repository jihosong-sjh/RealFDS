/**
 * 실시간 시스템 대시보드 메트릭 타입 정의
 *
 * TypeScript 인터페이스로 서버와 클라이언트 간 메시지 형식 정의
 * WebSocket 메시지, 서비스 상태, 메트릭 데이터 포함
 *
 * @see alert-dashboard/backend - 백엔드 Java 엔티티와 대응
 */

/**
 * 서비스 Health Check 상태
 */
export interface ServiceHealth {
  /** 서비스 식별자 */
  serviceName: 'transaction-generator' | 'fraud-detector' | 'alert-service' | 'websocket-gateway' | 'alert-dashboard';

  /** 현재 서비스 상태 */
  status: 'UP' | 'DOWN';

  /** 마지막 Health Check 수행 시각 (ISO-8601) */
  lastChecked: string;

  /** Health Check 응답 시간 (밀리초 단위, DOWN 시 null) */
  responseTime: number | null;

  /** JVM 메모리 사용량 (메가바이트 단위, DOWN 시 null) */
  memoryUsage: number | null;

  /** DOWN 상태 시 오류 유형 (UP 시 null) */
  errorType: 'TIMEOUT' | 'HTTP_ERROR' | 'NETWORK_ERROR' | null;

  /** DOWN 상태 시 오류 상세 메시지 (UP 시 null) */
  errorMessage: string | null;
}

/**
 * 거래량 메트릭 데이터 포인트
 */
export interface TransactionMetrics {
  /** 메트릭 측정 시각 (ISO-8601) */
  timestamp: string;

  /** 초당 거래 수 (TPS) */
  tps: number;

  /** 누적 거래 수 */
  totalTransactions: number;
}

/**
 * 거래량 메트릭 집계 데이터
 */
export interface TpsAggregated {
  /** 현재 TPS */
  current: number;

  /** 1시간 평균 TPS */
  average: number;

  /** 1시간 최대 TPS */
  max: number;

  /** 최근 1시간 TPS 시계열 데이터 (720개) */
  history: TransactionMetrics[];
}

/**
 * 알림 발생률 메트릭 데이터 포인트
 */
export interface AlertMetrics {
  /** 메트릭 측정 시각 (ISO-8601) */
  timestamp: string;

  /** 분당 총 알림 발생 수 */
  alertsPerMinute: number;

  /** 규칙별 알림 발생 수 */
  byRule: {
    HIGH_VALUE: number;
    FOREIGN_COUNTRY: number;
    HIGH_FREQUENCY: number;
  };
}

/**
 * 알림 발생률 메트릭 집계 데이터
 */
export interface AlertsAggregated {
  /** 현재 분당 알림 수 */
  current: number;

  /** 1시간 평균 알림 발생률 */
  average: number;

  /** 1시간 최대 알림 발생률 */
  max: number;

  /** 현재 규칙별 알림 수 */
  byRule: {
    HIGH_VALUE: number;
    FOREIGN_COUNTRY: number;
    HIGH_FREQUENCY: number;
  };

  /** 최근 1시간 알림 발생률 시계열 데이터 (720개) */
  history: AlertMetrics[];

  /** 경고 상태 여부 (current > average * 2) */
  isWarning?: boolean;
}

/**
 * 메트릭 업데이트 메시지 (Server → Client)
 */
export interface MetricsUpdateMessage {
  type: 'METRICS_UPDATE';
  timestamp: string;
  data: {
    services: ServiceHealth[];
    tps: TpsAggregated;
    alerts: AlertsAggregated;
  };
}

/**
 * 백필 요청 메시지 (Client → Server)
 */
export interface BackfillRequestMessage {
  type: 'BACKFILL_REQUEST';
  lastReceivedTimestamp: string;
}

/**
 * 백필 응답 메시지 (Server → Client)
 */
export interface BackfillResponseMessage {
  type: 'BACKFILL_RESPONSE';
  data: Array<{
    timestamp: string;
    tps: number;
    totalTransactions: number;
    alertsPerMinute: number;
    byRule: {
      HIGH_VALUE: number;
      FOREIGN_COUNTRY: number;
      HIGH_FREQUENCY: number;
    };
  }>;
}

/**
 * 에러 메시지 (Server → Client)
 */
export interface ErrorMessage {
  type: 'ERROR';
  message: string;
  timestamp: string;
}

/**
 * WebSocket 메시지 Union Type
 */
export type WebSocketMessage =
  | MetricsUpdateMessage
  | BackfillRequestMessage
  | BackfillResponseMessage
  | ErrorMessage;

/**
 * TPS 차트 데이터 형식 (Recharts Line Chart)
 */
export interface TpsChartData {
  /** X축: 시각 (ISO-8601) */
  timestamp: string;

  /** Y축: TPS */
  tps: number;
}

/**
 * 알림 발생률 차트 데이터 형식 (Recharts Stacked Area Chart)
 */
export interface AlertRateChartData {
  /** X축: 시각 (ISO-8601) */
  timestamp: string;

  /** Y축 (스택): HIGH_VALUE 규칙 알림 수 */
  HIGH_VALUE: number;

  /** Y축 (스택): FOREIGN_COUNTRY 규칙 알림 수 */
  FOREIGN_COUNTRY: number;

  /** Y축 (스택): HIGH_FREQUENCY 규칙 알림 수 */
  HIGH_FREQUENCY: number;
}

/**
 * WebSocket 연결 상태
 */
export type ConnectionStatus = 'connecting' | 'connected' | 'disconnected' | 'error';
