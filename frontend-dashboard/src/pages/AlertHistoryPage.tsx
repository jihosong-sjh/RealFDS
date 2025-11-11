import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import type { SortingState } from '@tanstack/react-table';
import { DateRangePicker } from '../components/DateRangePicker';
import { AlertHistoryFilters, type FilterValues } from '../components/AlertHistoryFilters';
import { AlertHistoryTable } from '../components/AlertHistoryTable';
import { Pagination } from '../components/Pagination';
import { alertHistoryService } from '../services/alertHistoryService';
import '../styles/AlertHistoryPage.css';

/**
 * AlertHistoryPage 컴포넌트
 *
 * 알림 이력 조회 페이지입니다.
 * - DateRangePicker를 통해 날짜 범위 선택
 * - AlertHistoryFilters를 통해 다중 필터링 (규칙명, 사용자ID, 상태)
 * - AlertHistoryTable로 알림 목록 표시 (정렬 기능 포함)
 * - Pagination으로 페이지네이션 지원
 * - React Query를 사용한 서버 상태 관리
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

  // 상태 관리: 페이지네이션
  const [currentPage, setCurrentPage] = useState<number>(0);
  const [pageSize] = useState<number>(50);

  // 상태 관리: 테이블 정렬
  const [sorting, setSorting] = useState<SortingState>([
    { id: 'alertTimestamp', desc: true },
  ]);

  // 상태 관리: 검색 트리거
  const [searchTrigger, setSearchTrigger] = useState<number>(0);

  /**
   * React Query를 사용한 알림 이력 검색
   * - 자동 로딩 상태 관리
   * - 자동 에러 처리
   * - 캐싱 및 재시도 지원
   */
  const {
    data: searchResult,
    isLoading,
    error,
    isFetching,
  } = useQuery({
    queryKey: [
      'alertHistory',
      startDate,
      endDate,
      filters.ruleName,
      filters.userId,
      filters.status,
      currentPage,
      pageSize,
      searchTrigger,
    ],
    queryFn: async () => {
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

      console.log('[AlertHistoryPage] 검색 완료:', result.totalElements, '개');
      return result;
    },
    enabled: true,
    staleTime: 1000 * 60 * 2, // 2분
  });

  /**
   * 날짜 범위 변경 핸들러
   */
  const handleDateRangeChange = (start: string | null, end: string | null) => {
    setStartDate(start);
    setEndDate(end);
    setCurrentPage(0); // 날짜 범위 변경 시 첫 페이지로 이동
  };

  /**
   * 필터 값 변경 핸들러
   */
  const handleFilterChange = (newFilters: FilterValues) => {
    setFilters(newFilters);
    setCurrentPage(0); // 필터 변경 시 첫 페이지로 이동
  };

  /**
   * 검색 실행 핸들러
   * - searchTrigger를 증가시켜 React Query 재실행
   */
  const handleSearch = () => {
    setCurrentPage(0); // 검색 시 첫 페이지로 이동
    setSearchTrigger((prev) => prev + 1);
  };

  /**
   * 페이지 변경 핸들러
   */
  const handlePageChange = (newPage: number) => {
    setCurrentPage(newPage);
  };

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
      {(isLoading || isFetching) && (
        <div className="alert-history-page__loading">
          <div className="alert-history-page__spinner"></div>
          <p>알림을 검색하고 있습니다...</p>
        </div>
      )}

      {/* 에러 메시지 */}
      {error && !isLoading && (
        <div className="alert-history-page__error" role="alert">
          <strong>오류 발생:</strong>{' '}
          {error instanceof Error ? error.message : '알림 이력 조회에 실패했습니다'}
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

          {/* 알림 테이블 (TanStack Table 사용) */}
          <section className="alert-history-page__table-container">
            <AlertHistoryTable
              data={searchResult.content}
              sorting={sorting}
              onSortingChange={setSorting}
            />
          </section>

          {/* 페이지네이션 */}
          {searchResult.totalPages > 1 && (
            <section className="alert-history-page__pagination">
              <Pagination
                currentPage={searchResult.currentPage}
                totalPages={searchResult.totalPages}
                hasPrevious={searchResult.hasPrevious}
                hasNext={searchResult.hasNext}
                onPageChange={handlePageChange}
              />
            </section>
          )}
        </>
      )}
    </div>
  );
}
