import { useState } from 'react';
import type { AlertStatus } from '../types/alertStatus';

/**
 * useAlertManagement Hook: 알림 상태 관리 API 호출
 *
 * 주요 기능:
 * - changeAlertStatus: 알림 상태 변경 (UNREAD → IN_PROGRESS → COMPLETED)
 * - 에러 처리 및 로딩 상태 관리
 *
 * @param baseUrl - alert-service API 기본 URL (예: "http://localhost:8081")
 */
export function useAlertManagement(baseUrl: string) {
  // 로딩 상태 관리
  const [isLoading, setIsLoading] = useState(false);
  // 에러 상태 관리
  const [error, setError] = useState<string | null>(null);

  /**
   * changeAlertStatus: 알림 상태 변경 API 호출
   *
   * @param alertId - 알림 ID (UUID)
   * @param newStatus - 새로운 상태 (IN_PROGRESS 또는 COMPLETED)
   * @returns 상태 변경 성공 여부
   */
  const changeAlertStatus = async (
    alertId: string,
    newStatus: AlertStatus
  ): Promise<boolean> => {
    setIsLoading(true);
    setError(null);

    try {
      const response = await fetch(`${baseUrl}/api/alerts/${alertId}/status`, {
        method: 'PATCH',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ status: newStatus }),
      });

      if (!response.ok) {
        // HTTP 에러 처리
        if (response.status === 404) {
          throw new Error('알림을 찾을 수 없습니다.');
        } else if (response.status === 400) {
          throw new Error('유효하지 않은 상태 값입니다.');
        } else {
          throw new Error(`상태 변경 실패: ${response.status}`);
        }
      }

      const data = await response.json();
      console.log('[useAlertManagement] 상태 변경 성공:', alertId, newStatus);
      return true;
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : '알 수 없는 오류가 발생했습니다.';
      console.error('[useAlertManagement] 상태 변경 실패:', errorMessage);
      setError(errorMessage);
      return false;
    } finally {
      setIsLoading(false);
    }
  };

  return {
    changeAlertStatus,
    isLoading,
    error,
  };
}
