import { useState, useEffect } from 'react';
import { DateRangePicker } from '../components/DateRangePicker';
import { AlertHistoryFilters, type FilterValues } from '../components/AlertHistoryFilters';
import { alertHistoryService, type PagedAlertResult } from '../services/alertHistoryService';
import type { Alert } from '../types/alert';
import '../styles/AlertHistoryPage.css';

/**
 * AlertHistoryPage 컴포넌트
 *
 * 알림 이력 조회 페이지입니다.
 * - DateRangePicker를 통해 날짜 범위 선택
 * - AlertHistoryFilters를 통해 다중 필터링 (규칙명, 사용자ID, 상태)
 * - 검색 버튼 클릭 시 API 호출
 * - 알림 목록을 테이블로 표시
 * - 페이지네이션 지원
 * - 로딩 상태 및 에러 상태 표시
 */
export function AlertHistoryPage() {
  // 상태 관리: 검색 조건 (날짜 범위)
  const [startDate, setStartDate] = useState<string | null>(null);
  const [endDate, setEndDate] = useState<string | null>(null);

  // 상태 관리: 검색 조건 (다중 필터)
  const [filters, setFilters] = useState<FilterValues>({
    ruleName: '',
    userId: '',
    status: '',
  });

  const [currentPage, setCurrentPage] = useState<number>(0);
  const [pageSize] = useState<number>(50);

  // 상태 관리: 검색 결과
  const [searchResult, setSearchResult] = useState<PagedAlertResult | null>(null);

  // 상태 관리: 로딩 상태
  const [isLoading, setIsLoading] = useState<boolean>(false);

  // 상태 관리: 에러 메시지
  const [error, setError] = useState<string | null>(null);

  /**
   * 날짜 범위 변경 핸들러
   */
  const handleDateRangeChange = (start: string | null, end: string | null) => {
    setStartDate(start);
    setEndDate(end);
  };

  /**
   * 필터 값 변경 핸들러
   */
  const handleFilterChange = (newFilters: FilterValues) => {
    setFilters(newFilters);
  };

  /**
   * 검색 실행 핸들러 (모든 검색 조건 포함)
   */
  const handleSearch = async () => {
    setIsLoading(true);
    setError(null);

    try {
      console.log('[AlertHistoryPage] 검색 시작:', {
        startDate,
        endDate,
        ruleName: filters.ruleName || null,
        userId: filters.userId || null,
        status: filters.status || null,
        page: currentPage,
        size: pageSize,
      });

      const result = await alertHistoryService.searchAlerts({
        startDate,
        endDate,
        ruleName: filters.ruleName || null,
        userId: filters.userId || null,
        status: filters.status || null,
        page: currentPage,
        size: pageSize,
      });

      setSearchResult(result);
      console.log('[AlertHistoryPage] 검색 완료:', result.totalElements, '개');
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : '알림 이력 조회에 실패했습니다';
      setError(errorMessage);
      console.error('[AlertHistoryPage] 검색 실패:', err);
    } finally {
      setIsLoading(false);
    }
  };

  /**
   * 페이지 변경 핸들러
   */
  const handlePageChange = (newPage: number) => {
    setCurrentPage(newPage);
  };

  /**
   * currentPage 변경 시 자동으로 검색 실행
   */
  useEffect(() => {
    if (searchResult !== null) {
      handleSearch();
    }
  }, [currentPage]);

  /**
   * 컴포넌트 마운트 시 초기 검색 실행 (최근 7일 기본값)
   */
  useEffect(() => {
    handleSearch();
  }, []);

  return (
    <div className="alert-history-page">
      {/* 헤더 */}
      <header className="alert-history-page__header">
        <h1 className="alert-history-page__title">알림 이력 조회</h1>
        <p className="alert-history-page__subtitle">
          과거에 발생한 알림을 검색하고 패턴을 분석하세요
        </p>
      </header>

      {/* 검색 조건: 날짜 범위 */}
      <section className="alert-history-page__search">
        <DateRangePicker
          onChange={handleDateRangeChange}
          initialStartDate={startDate}
          initialEndDate={endDate}
        />
      </section>

      {/* 검색 조건: 다중 필터 (User Story 3) */}
      <section className="alert-history-page__filters">
        <AlertHistoryFilters
          onFilterChange={handleFilterChange}
          onSearch={handleSearch}
        />
      </section>

      {/* 로딩 상태 */}
      {isLoading && (
        <div className="alert-history-page__loading">
          <div className="alert-history-page__spinner"></div>
          <p>알림을 검색하고 있습니다...</p>
        </div>
      )}

      {/* 에러 메시지 */}
      {error && !isLoading && (
        <div className="alert-history-page__error" role="alert">
          <strong>오류 발생:</strong> {error}
        </div>
      )}

      {/* 검색 결과 */}
      {searchResult && !isLoading && !error && (
        <>
          {/* 결과 요약 */}
          <section className="alert-history-page__summary">
            <p>
              총 <strong>{searchResult.totalElements}</strong>개의 알림을 찾았습니다
              (페이지 {searchResult.currentPage + 1} / {searchResult.totalPages})
            </p>
          </section>

          {/* 알림 테이블 */}
          {searchResult.content.length > 0 ? (
            <section className="alert-history-page__table-container">
              <table className="alert-history-page__table">
                <thead>
                  <tr>
                    <th>알림 ID</th>
                    <th>사용자 ID</th>
                    <th>금액</th>
                    <th>규칙명</th>
                    <th>심각도</th>
                    <th>발생 시각</th>
                    <th>상태</th>
                  </tr>
                </thead>
                <tbody>
                  {searchResult.content.map((alert: Alert) => (
                    <tr key={alert.alertId}>
                      <td className="alert-history-page__table-cell--id">
                        {alert.alertId.substring(0, 8)}...
                      </td>
                      <td>{alert.originalTransaction?.userId || 'N/A'}</td>
                      <td className="alert-history-page__table-cell--amount">
                        {alert.originalTransaction?.amount.toLocaleString()} {alert.originalTransaction?.currency}
                      </td>
                      <td>{alert.ruleName}</td>
                      <td>
                        <span className={`alert-history-page__severity alert-history-page__severity--${alert.severity.toLowerCase()}`}>
                          {alert.severity}
                        </span>
                      </td>
                      <td className="alert-history-page__table-cell--timestamp">
                        {new Date(alert.alertTimestamp).toLocaleString('ko-KR')}
                      </td>
                      <td>
                        <span className={`alert-history-page__status alert-history-page__status--${alert.status.toLowerCase()}`}>
                          {alert.status}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </section>
          ) : (
            <div className="alert-history-page__empty">
              <p>검색 조건에 맞는 알림이 없습니다.</p>
            </div>
          )}

          {/* 페이지네이션 */}
          {searchResult.totalPages > 1 && (
            <section className="alert-history-page__pagination">
              <button
                className="alert-history-page__pagination-button"
                onClick={() => handlePageChange(currentPage - 1)}
                disabled={!searchResult.hasPrevious}
              >
                이전
              </button>

              <span className="alert-history-page__pagination-info">
                페이지 {currentPage + 1} / {searchResult.totalPages}
              </span>

              <button
                className="alert-history-page__pagination-button"
                onClick={() => handlePageChange(currentPage + 1)}
                disabled={!searchResult.hasNext}
              >
                다음
              </button>
            </section>
          )}
        </>
      )}
    </div>
  );
}
