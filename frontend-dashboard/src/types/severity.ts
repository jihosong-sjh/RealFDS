// Severity enum 정의: 알림 심각도

/**
 * Severity: 알림 심각도 열거형
 * - LOW: 낮음 (파란색)
 * - MEDIUM: 보통 (노란색)
 * - HIGH: 높음 (주황색)
 * - CRITICAL: 긴급 (빨간색)
 */
export enum Severity {
  LOW = 'LOW',
  MEDIUM = 'MEDIUM',
  HIGH = 'HIGH',
  CRITICAL = 'CRITICAL',
}

/**
 * severityDisplay: 심각도별 한국어 표시 텍스트
 */
export const severityDisplay: Record<Severity, string> = {
  [Severity.LOW]: '낮음',
  [Severity.MEDIUM]: '보통',
  [Severity.HIGH]: '높음',
  [Severity.CRITICAL]: '긴급',
};

/**
 * severityColor: 심각도별 색상 코드
 */
export const severityColor: Record<Severity, string> = {
  [Severity.LOW]: '#3b82f6', // 파란색
  [Severity.MEDIUM]: '#eab308', // 노란색
  [Severity.HIGH]: '#f97316', // 주황색
  [Severity.CRITICAL]: '#ef4444', // 빨간색
};

/**
 * severityBackgroundColor: 심각도별 배경 색상 코드 (연한색)
 */
export const severityBackgroundColor: Record<Severity, string> = {
  [Severity.LOW]: '#dbeafe', // 연한 파란색
  [Severity.MEDIUM]: '#fef3c7', // 연한 노란색
  [Severity.HIGH]: '#fed7aa', // 연한 주황색
  [Severity.CRITICAL]: '#fee2e2', // 연한 빨간색
};

/**
 * severityOrder: 심각도별 정렬 순서 (높은 순서가 우선)
 */
export const severityOrder: Record<Severity, number> = {
  [Severity.CRITICAL]: 4,
  [Severity.HIGH]: 3,
  [Severity.MEDIUM]: 2,
  [Severity.LOW]: 1,
};
