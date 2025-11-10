// Alert 타입 정의: 실시간 FDS 알림 데이터 구조

/**
 * Transaction 인터페이스: 원본 거래 정보
 */
export interface Transaction {
  schemaVersion: string;     // 스키마 버전 (예: "1.0")
  transactionId: string;      // 거래 고유 식별자 (UUID)
  userId: string;             // 사용자 ID (예: "user-3")
  amount: number;             // 거래 금액 (정수)
  currency: string;           // 통화 코드 (예: "KRW")
  countryCode: string;        // 국가 코드 (예: "KR", "US")
  timestamp: string;          // 거래 발생 시각 (ISO 8601)
}

/**
 * Alert 인터페이스: 탐지된 의심스러운 거래 알림
 */
export interface Alert {
  schemaVersion: string;          // 스키마 버전 (예: "1.0")
  alertId: string;                // 알림 고유 식별자 (UUID)
  originalTransaction: Transaction; // 원본 거래 정보
  ruleType: string;               // 규칙 유형 ("SIMPLE_RULE" 또는 "STATEFUL_RULE")
  ruleName: string;               // 탐지 규칙 이름 ("HIGH_VALUE", "FOREIGN_COUNTRY", "HIGH_FREQUENCY")
  reason: string;                 // 한국어 설명 (예: "고액 거래 (100만원 초과)")
  severity: 'HIGH' | 'MEDIUM' | 'LOW'; // 심각도
  alertTimestamp: string;         // 알림 생성 시각 (ISO 8601)

  // 002-alert-management: 상태 관리 필드 추가
  status: 'UNREAD' | 'IN_PROGRESS' | 'COMPLETED'; // 처리 상태
  processedAt: string | null;     // 처리 완료 시각 (ISO 8601, status=COMPLETED 시만)
}
