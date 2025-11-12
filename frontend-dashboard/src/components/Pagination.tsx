import '../styles/Pagination.css';

/**
 * Pagination Props 정의
 */
interface PaginationProps {
  /** 현재 페이지 (0-based) */
  currentPage: number;
  /** 전체 페이지 수 */
  totalPages: number;
  /** 이전 페이지 존재 여부 */
  hasPrevious: boolean;
  /** 다음 페이지 존재 여부 */
  hasNext: boolean;
  /** 페이지 변경 핸들러 */
  onPageChange: (page: number) => void;
}

/**
 * Pagination 컴포넌트
 *
 * 알림 목록 페이지네이션 UI를 제공합니다.
 * - 현재 페이지 및 전체 페이지 표시
 * - 이전/다음 버튼
 * - 페이지 번호 클릭 이벤트
 * - 한국어 라벨
 * - TypeScript 타입 안정성
 */
export function Pagination({
  currentPage,
  totalPages,
  hasPrevious,
  hasNext,
  onPageChange,
}: PaginationProps) {
  /**
   * 표시할 페이지 번호 목록 생성
   * 최대 7개의 페이지 번호를 표시 (현재 페이지를 중심으로)
   */
  const getPageNumbers = (): (number | string)[] => {
    const pages: (number | string)[] = [];
    const maxPagesToShow = 7;

    if (totalPages <= maxPagesToShow) {
      // 전체 페이지가 7개 이하인 경우, 모든 페이지 표시
      for (let i = 0; i < totalPages; i++) {
        pages.push(i);
      }
    } else {
      // 전체 페이지가 7개 초과인 경우
      const leftOffset = Math.max(0, currentPage - 2);
      const rightOffset = Math.min(totalPages - 1, currentPage + 2);

      // 첫 페이지 항상 표시
      if (leftOffset > 0) {
        pages.push(0);
        if (leftOffset > 1) {
          pages.push('...');
        }
      }

      // 중간 페이지 표시
      for (let i = leftOffset; i <= rightOffset; i++) {
        pages.push(i);
      }

      // 마지막 페이지 항상 표시
      if (rightOffset < totalPages - 1) {
        if (rightOffset < totalPages - 2) {
          pages.push('...');
        }
        pages.push(totalPages - 1);
      }
    }

    return pages;
  };

  const pageNumbers = getPageNumbers();

  /**
   * 페이지 번호 클릭 핸들러
   */
  const handlePageClick = (page: number | string) => {
    if (typeof page === 'number' && page !== currentPage) {
      onPageChange(page);
    }
  };

  return (
    <nav className="pagination" aria-label="페이지네이션">
      {/* 이전 버튼 */}
      <button
        className="pagination__button pagination__button--prev"
        onClick={() => onPageChange(currentPage - 1)}
        disabled={!hasPrevious}
        aria-label="이전 페이지"
      >
        <span aria-hidden="true">←</span>
        <span className="pagination__button-text">이전</span>
      </button>

      {/* 페이지 번호 */}
      <div className="pagination__pages" role="list">
        {pageNumbers.map((page, index) => {
          if (page === '...') {
            return (
              <span key={`ellipsis-${index}`} className="pagination__ellipsis">
                ...
              </span>
            );
          }

          const pageNum = page as number;
          const isActive = pageNum === currentPage;

          return (
            <button
              key={pageNum}
              className={`pagination__page-button ${
                isActive ? 'pagination__page-button--active' : ''
              }`}
              onClick={() => handlePageClick(pageNum)}
              disabled={isActive}
              aria-label={`페이지 ${pageNum + 1}`}
              aria-current={isActive ? 'page' : undefined}
            >
              {pageNum + 1}
            </button>
          );
        })}
      </div>

      {/* 다음 버튼 */}
      <button
        className="pagination__button pagination__button--next"
        onClick={() => onPageChange(currentPage + 1)}
        disabled={!hasNext}
        aria-label="다음 페이지"
      >
        <span className="pagination__button-text">다음</span>
        <span aria-hidden="true">→</span>
      </button>

      {/* 페이지 정보 */}
      <div className="pagination__info" aria-live="polite" aria-atomic="true">
        페이지 {currentPage + 1} / {totalPages}
      </div>
    </nav>
  );
}
