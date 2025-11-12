/**
 * 실시간 시스템 대시보드 메트릭 타입 정의
 *
 * 백엔드 Kotlin 데이터 모델과 1:1 대응되는 TypeScript 인터페이스입니다.
 * WebSocket을 통해 실시간으로 전송되는 메트릭 데이터의 타입을 정의합니다.
 *
 * @see D:\side-project\RealFDS\alert-dashboard\backend\src\main\kotlin\io\realfds\alert\model
 */

// ============================================================================
// 서비스 Health Check 관련 타입
// ============================================================================

/**
 * 서비스 상태 Enum
 * - UP: 정상 작동 중
 * - DOWN: 중단 또는 오류 상태
 */
export type ServiceStatus = 'UP' | 'DOWN';

/**
 * 오류 유형 Enum
 * - TIMEOUT: Health Check 응답 시간 초과 (>3초)
 * - HTTP_ERROR: HTTP 4xx/5xx 에러
 * - NETWORK_ERROR: 네트워크 연결 실패 (Connection Refused, DNS 실패)
 */
export type ErrorType = 'TIMEOUT' | 'HTTP_ERROR' | 'NETWORK_ERROR';

/**
 * 서비스 이름 타입
 * 5개 마이크로서비스 식별자
 */
export type ServiceName =
  | 'transaction-generator'
  | 'fraud-detector'
  | 'alert-service'
  | 'websocket-gateway'
  | 'frontend-dashboard';

/**
 * 마이크로서비스의 Health Check 상태
 *
 * 5개 마이크로서비스의 실시간 Health Check 결과를 나타냅니다.
 *
 * @property serviceName 서비스 식별자 (5개 중 하나)
 * @property status 현재 서비스 상태 (UP 또는 DOWN)
 * @property lastChecked 마지막 Health Check 수행 시각 (ISO-8601)
 * @property responseTime Health Check 응답 시간 (밀리초 단위, DOWN 시 null)
 * @property memoryUsage JVM 메모리 사용량 (메가바이트 단위, DOWN 시 null)
 * @property errorType DOWN 상태 시 오류 유형 (UP 시 null)
 * @property errorMessage DOWN 상태 시 오류 상세 메시지 (UP 시 null)
 */
export interface ServiceHealth {
  /** 서비스 식별자 */
  serviceName: ServiceName;

  /** 현재 서비스 상태 */
  status: ServiceStatus;

  /** 마지막 Health Check 수행 시각 (ISO-8601) */
  lastChecked: string;

  /** Health Check 응답 시간 (밀리초 단위, DOWN 시 null) */
  responseTime: number | null;

  /** JVM 메모리 사용량 (메가바이트 단위, DOWN 시 null) */
  memoryUsage: number | null;

  /** DOWN 상태 시 오류 유형 (UP 시 null) */
  errorType: ErrorType | null;

  /** DOWN 상태 시 오류 상세 메시지 (UP 시 null, 최대 256자) */
  errorMessage: string | null;
}

// ============================================================================
// 거래량 메트릭 관련 타입
// ============================================================================

/**
 * 거래량 메트릭 데이터 포인트 (시계열)
 *
 * 초당 거래 처리량(TPS)을 나타냅니다.
 *
 * @property timestamp 메트릭 측정 시각 (ISO-8601, 5초 간격)
 * @property tps 초당 거래 수 (Transactions Per Second, 0~10000)
 * @property totalTransactions 시스템 시작 이후 누적 거래 수
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
 *
 * 현재, 평균, 최대 TPS 및 1시간 히스토리를 포함합니다.
 *
 * @property current 현재 TPS
 * @property average 1시간 평균 TPS
 * @property max 1시간 최대 TPS
 * @property history 최근 1시간 TPS 시계열 데이터 (720개)
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

// ============================================================================
// 알림 발생률 메트릭 관련 타입
// ============================================================================

/**
 * 알림 규칙 이름 타입
 * - HIGH_VALUE: 고액 거래 탐지 규칙
 * - FOREIGN_COUNTRY: 해외 거래 탐지 규칙
 * - HIGH_FREQUENCY: 고빈도 거래 탐지 규칙
 */
export type AlertRuleName = 'HIGH_VALUE' | 'FOREIGN_COUNTRY' | 'HIGH_FREQUENCY';

/**
 * 규칙별 알림 수 (맵)
 */
export interface AlertsByRule {
  HIGH_VALUE: number;
  FOREIGN_COUNTRY: number;
  HIGH_FREQUENCY: number;
}

/**
 * 알림 발생률 메트릭 데이터 포인트 (시계열)
 *
 * 분당 알림 발생 수를 규칙별로 분류하여 나타냅니다.
 *
 * @property timestamp 메트릭 측정 시각 (ISO-8601, 5초 간격)
 * @property alertsPerMinute 분당 총 알림 발생 수 (0~1000)
 * @property byRule 규칙별 알림 발생 수
 */
export interface AlertMetrics {
  /** 메트릭 측정 시각 (ISO-8601) */
  timestamp: string;

  /** 분당 총 알림 발생 수 */
  alertsPerMinute: number;

  /** 규칙별 알림 발생 수 */
  byRule: AlertsByRule;
}

/**
 * 알림 발생률 메트릭 집계 데이터
 *
 * 현재, 평균, 최대 알림 발생률 및 1시간 히스토리를 포함합니다.
 *
 * @property current 현재 분당 알림 수
 * @property average 1시간 평균 알림 발생률
 * @property max 1시간 최대 알림 발생률
 * @property byRule 현재 규칙별 알림 수
 * @property history 최근 1시간 알림 발생률 시계열 데이터 (720개)
 * @property isWarning 경고 상태 여부 (current > average * 2)
 */
export interface AlertsAggregated {
  /** 현재 분당 알림 수 */
  current: number;

  /** 1시간 평균 알림 발생률 */
  average: number;

  /** 1시간 최대 알림 발생률 */
  max: number;

  /** 현재 규칙별 알림 수 */
  byRule: AlertsByRule;

  /** 최근 1시간 알림 발생률 시계열 데이터 (720개) */
  history: AlertMetrics[];

  /** 경고 상태 여부 (current > average * 2) */
  isWarning?: boolean;
}

// ============================================================================
// WebSocket 메시지 타입
// ============================================================================

/**
 * WebSocket 메시지 타입 Enum
 * - METRICS_UPDATE: 서버 → 클라이언트 (5초마다 브로드캐스트)
 * - BACKFILL_REQUEST: 클라이언트 → 서버 (재연결 시 누락 데이터 요청)
 * - BACKFILL_RESPONSE: 서버 → 클라이언트 (누락 데이터 전송)
 * - ERROR: 서버 → 클라이언트 (에러 알림)
 */
export type WebSocketMessageType =
  | 'METRICS_UPDATE'
  | 'BACKFILL_REQUEST'
  | 'BACKFILL_RESPONSE'
  | 'ERROR';

/**
 * 메트릭 업데이트 메시지 (Server → Client)
 *
 * 5초마다 서버가 모든 연결된 클라이언트에 브로드캐스트하는 메시지입니다.
 *
 * @property type 메시지 타입 (항상 'METRICS_UPDATE')
 * @property timestamp 메시지 생성 시각 (ISO-8601)
 * @property data 메트릭 데이터 (서비스 상태, TPS, 알림률)
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
 *
 * WebSocket 재연결 시 클라이언트가 누락된 메트릭 데이터를 요청하는 메시지입니다.
 *
 * @property type 메시지 타입 (항상 'BACKFILL_REQUEST')
 * @property lastReceivedTimestamp 클라이언트가 마지막으로 수신한 메트릭의 타임스탬프 (ISO-8601)
 */
export interface BackfillRequestMessage {
  type: 'BACKFILL_REQUEST';
  lastReceivedTimestamp: string;
}

/**
 * 백필 응답 메시지 (Server → Client)
 *
 * 서버가 클라이언트의 백필 요청에 대한 응답으로 누락된 메트릭 데이터를 전송하는 메시지입니다.
 *
 * @property type 메시지 타입 (항상 'BACKFILL_RESPONSE')
 * @property data 누락된 메트릭 데이터 포인트 배열 (시간 순서 정렬)
 */
export interface BackfillResponseMessage {
  type: 'BACKFILL_RESPONSE';
  data: Array<{
    timestamp: string;
    tps: number;
    totalTransactions: number;
    alertsPerMinute: number;
    byRule: AlertsByRule;
  }>;
}

/**
 * 에러 메시지 (Server → Client)
 *
 * 서버가 에러 발생 시 클라이언트에 알리는 메시지입니다.
 *
 * @property type 메시지 타입 (항상 'ERROR')
 * @property message 에러 메시지
 * @property code 에러 코드 (선택적)
 */
export interface ErrorMessage {
  type: 'ERROR';
  message: string;
  code?: string;
}

/**
 * WebSocket 메시지 Union Type
 *
 * 모든 WebSocket 메시지 타입의 합집합입니다.
 */
export type WebSocketMessage =
  | MetricsUpdateMessage
  | BackfillRequestMessage
  | BackfillResponseMessage
  | ErrorMessage;

// ============================================================================
// Recharts 차트 데이터 형식
// ============================================================================

/**
 * TPS 차트 데이터 형식 (Line Chart)
 *
 * Recharts LineChart에서 사용되는 데이터 형식입니다.
 *
 * @property timestamp X축: 시각 (ISO-8601)
 * @property tps Y축: TPS
 */
export interface TpsChartData {
  /** X축: 시각 (ISO-8601) */
  timestamp: string;

  /** Y축: TPS */
  tps: number;
}

/**
 * 알림 발생률 차트 데이터 형식 (Stacked Area Chart)
 *
 * Recharts AreaChart (스택 형태)에서 사용되는 데이터 형식입니다.
 *
 * @property timestamp X축: 시각 (ISO-8601)
 * @property HIGH_VALUE Y축 (스택): HIGH_VALUE 규칙 알림 수
 * @property FOREIGN_COUNTRY Y축 (스택): FOREIGN_COUNTRY 규칙 알림 수
 * @property HIGH_FREQUENCY Y축 (스택): HIGH_FREQUENCY 규칙 알림 수
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

// ============================================================================
// WebSocket 연결 상태 타입
// ============================================================================

/**
 * WebSocket 연결 상태
 * - disconnected: 연결 끊김
 * - connecting: 연결 시도 중
 * - connected: 연결 완료
 */
export type WebSocketConnectionStatus = 'disconnected' | 'connecting' | 'connected';
