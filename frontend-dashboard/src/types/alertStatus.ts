// AlertStatus enum 정의: 알림 처리 상태

/**
 * AlertStatus: 알림 처리 상태 열거형
 * - UNREAD: 미확인 (회색)
 * - IN_PROGRESS: 확인중 (파란색)
 * - COMPLETED: 완료 (초록색)
 */
export enum AlertStatus {
  UNREAD = 'UNREAD',
  IN_PROGRESS = 'IN_PROGRESS',
  COMPLETED = 'COMPLETED',
}

/**
 * alertStatusDisplay: 상태별 한국어 표시 텍스트
 */
export const alertStatusDisplay: Record<AlertStatus, string> = {
  [AlertStatus.UNREAD]: '미확인',
  [AlertStatus.IN_PROGRESS]: '확인중',
  [AlertStatus.COMPLETED]: '완료',
};

/**
 * alertStatusColor: 상태별 색상 코드
 */
export const alertStatusColor: Record<AlertStatus, string> = {
  [AlertStatus.UNREAD]: '#6b7280', // 회색
  [AlertStatus.IN_PROGRESS]: '#3b82f6', // 파란색
  [AlertStatus.COMPLETED]: '#10b981', // 초록색
};
