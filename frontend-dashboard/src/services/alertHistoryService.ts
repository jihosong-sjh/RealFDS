import type { Alert } from '../types/alert';

/**
 * AlertStatus: 알림 상태 타입
 */
export type AlertStatus = 'UNREAD' | 'IN_PROGRESS' | 'COMPLETED';

/**
 * AlertSearchCriteria: 알림 이력 검색 조건
 */
export interface AlertSearchCriteria {
  /** 시작 날짜 (YYYY-MM-DD 형식) */
  startDate?: string | null;
  /** 종료 날짜 (YYYY-MM-DD 형식) */
  endDate?: string | null;
  /** 탐지 규칙명 (HIGH_AMOUNT, FOREIGN_COUNTRY, RAPID_TRANSACTION) */
  ruleName?: string | null;
  /** 사용자 ID (예: user-1) */
  userId?: string | null;
  /** 알림 상태 (UNREAD, IN_PROGRESS, COMPLETED) */
  status?: AlertStatus | null;
  /** 페이지 번호 (0부터 시작) */
  page?: number;
  /** 페이지 크기 (1~100) */
  size?: number;
}

/**
 * PagedAlertResult: 페이지네이션된 알림 검색 결과
 */
export interface PagedAlertResult {
  /** 현재 페이지의 알림 목록 */
  content: Alert[];
  /** 전체 알림 개수 */
  totalElements: number;
  /** 전체 페이지 수 */
  totalPages: number;
  /** 현재 페이지 번호 (0부터 시작) */
  currentPage: number;
  /** 페이지 크기 */
  pageSize: number;
  /** 다음 페이지 존재 여부 */
  hasNext: boolean;
  /** 이전 페이지 존재 여부 */
  hasPrevious: boolean;
}

/**
 * Alert History Service
 *
 * 알림 이력 조회 API와 통신하는 서비스입니다.
 * - 날짜 범위 검색 지원
 * - 페이지네이션 지원
 * - ISO 8601 형식 날짜 변환
 */
export class AlertHistoryService {
  private readonly baseUrl: string;

  /**
   * AlertHistoryService 생성자
   *
   * @param baseUrl alert-dashboard API의 기본 URL (예: "http://localhost:8083")
   */
  constructor(baseUrl: string) {
    this.baseUrl = baseUrl;
  }

  /**
   * 알림 이력 검색
   *
   * 검색 조건에 따라 과거 알림을 조회합니다.
   * - startDate, endDate가 제공되지 않으면 백엔드의 기본값(최근 7일) 사용
   * - 날짜는 YYYY-MM-DD 형식에서 ISO 8601 형식(YYYY-MM-DDTHH:mm:ssZ)으로 변환됩니다
   *
   * @param criteria 검색 조건 (startDate, endDate, page, size)
   * @returns PagedAlertResult (알림 목록 + 페이지네이션 메타데이터)
   * @throws Error API 호출 실패 시
   *
   * @example
   * ```ts
   * const service = new AlertHistoryService('http://localhost:8083');
   * const result = await service.searchAlerts({
   *   startDate: '2025-11-01',
   *   endDate: '2025-11-11',
   *   page: 0,
   *   size: 50
   * });
   * console.log(result.content); // 알림 목록
   * console.log(result.totalElements); // 전체 알림 개수
   * ```
   */
  async searchAlerts(criteria: AlertSearchCriteria = {}): Promise<PagedAlertResult> {
    // 쿼리 파라미터 생성
    const params = new URLSearchParams();

    // 날짜 범위 파라미터 추가 (YYYY-MM-DD → ISO 8601 변환)
    if (criteria.startDate) {
      const startDateISO = this.convertToISO8601(criteria.startDate, true);
      params.append('startDate', startDateISO);
    }

    if (criteria.endDate) {
      const endDateISO = this.convertToISO8601(criteria.endDate, false);
      params.append('endDate', endDateISO);
    }

    // 필터 파라미터 추가 (User Story 3: 다중 필터링)
    if (criteria.ruleName && criteria.ruleName.trim() !== '') {
      params.append('ruleName', criteria.ruleName);
    }

    if (criteria.userId && criteria.userId.trim() !== '') {
      params.append('userId', criteria.userId);
    }

    if (criteria.status && criteria.status.trim() !== '') {
      params.append('status', criteria.status);
    }

    // 페이지네이션 파라미터 추가
    if (criteria.page !== undefined) {
      params.append('page', criteria.page.toString());
    }

    if (criteria.size !== undefined) {
      params.append('size', criteria.size.toString());
    }

    // API 요청 URL 생성
    const url = `${this.baseUrl}/api/alerts/history?${params.toString()}`;

    console.log('[AlertHistoryService] 알림 이력 검색 요청:', url);

    try {
      const response = await fetch(url, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
        },
      });

      if (!response.ok) {
        const errorBody = await response.text();
        throw new Error(
          `알림 이력 조회 실패: HTTP ${response.status} ${response.statusText} - ${errorBody}`
        );
      }

      const data: PagedAlertResult = await response.json();
      console.log('[AlertHistoryService] 알림 이력 조회 성공:', data.totalElements, '개');

      return data;
    } catch (error) {
      console.error('[AlertHistoryService] 알림 이력 조회 실패:', error);
      throw error;
    }
  }

  /**
   * YYYY-MM-DD 형식을 ISO 8601 형식으로 변환
   *
   * - startDate인 경우: 해당 날짜의 00:00:00 (자정)
   * - endDate인 경우: 해당 날짜의 23:59:59 (자정 직전)
   *
   * @param dateString YYYY-MM-DD 형식의 날짜 문자열
   * @param isStartDate 시작 날짜 여부 (true: 00:00:00, false: 23:59:59)
   * @returns ISO 8601 형식의 날짜 문자열 (YYYY-MM-DDTHH:mm:ssZ)
   *
   * @example
   * ```ts
   * convertToISO8601('2025-11-01', true)  // "2025-11-01T00:00:00Z"
   * convertToISO8601('2025-11-11', false) // "2025-11-11T23:59:59Z"
   * ```
   */
  private convertToISO8601(dateString: string, isStartDate: boolean): string {
    // 날짜 객체 생성 (로컬 시간대)
    const date = new Date(dateString);

    if (isStartDate) {
      // 시작 날짜: 00:00:00
      date.setHours(0, 0, 0, 0);
    } else {
      // 종료 날짜: 23:59:59
      date.setHours(23, 59, 59, 999);
    }

    // ISO 8601 형식으로 변환 (UTC 시간대)
    return date.toISOString();
  }
}

/**
 * AlertHistoryService 싱글톤 인스턴스
 *
 * 환경 변수에서 alert-dashboard URL을 가져옵니다.
 * 기본값: http://localhost:8083
 */
export const alertHistoryService = new AlertHistoryService(
  import.meta.env.VITE_ALERT_DASHBOARD_URL || 'http://localhost:8083'
);
