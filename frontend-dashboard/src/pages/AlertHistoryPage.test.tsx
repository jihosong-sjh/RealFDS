import { describe, it, expect, beforeAll, afterEach, afterAll, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { AlertHistoryPage } from './AlertHistoryPage';
import type { Alert } from '../types/alert';
import type { PagedAlertResult } from '../services/alertHistoryService';

/**
 * AlertHistoryPage 통합 테스트
 *
 * 테스트 범위:
 * - 초기 렌더링 및 자동 검색 확인
 * - 날짜 범위 선택 및 검색 확인
 * - 검색 결과 표시 확인
 * - 페이지네이션 확인
 * - 에러 처리 확인
 */

// Mock fetch globally
const mockFetch = vi.fn();
global.fetch = mockFetch as any;

describe('AlertHistoryPage', () => {
  /**
   * 테스트용 샘플 알림 데이터 생성
   */
  const createMockAlert = (id: number): Alert => ({
    schemaVersion: '1.0',
    alertId: `alert-${id}`,
    originalTransaction: {
      schemaVersion: '1.0',
      transactionId: `txn-${id}`,
      userId: `user-${id}`,
      amount: 1000000 + id * 100000,
      currency: 'KRW',
      countryCode: 'KR',
      timestamp: new Date(2025, 10, id).toISOString(),
    },
    ruleType: 'SIMPLE_RULE',
    ruleName: 'HIGH_AMOUNT',
    reason: `고액 거래 탐지 (${id})`,
    severity: 'HIGH',
    alertTimestamp: new Date(2025, 10, id).toISOString(),
    status: 'UNREAD',
    processedAt: null,
    assignedTo: null,
    actionNote: null,
  });

  /**
   * 테스트용 페이지네이션 결과 생성
   */
  const createMockPagedResult = (page: number, size: number, totalElements: number): PagedAlertResult => {
    const totalPages = Math.ceil(totalElements / size);
    const startIndex = page * size;
    const endIndex = Math.min(startIndex + size, totalElements);
    const content = Array.from({ length: endIndex - startIndex }, (_, i) => createMockAlert(startIndex + i + 1));

    return {
      content,
      totalElements,
      totalPages,
      currentPage: page,
      pageSize: size,
      hasNext: page < totalPages - 1,
      hasPrevious: page > 0,
    };
  };

  /**
   * 각 테스트 후 mock 초기화
   */
  afterEach(() => {
    mockFetch.mockClear();
  });

  /**
   * 테스트: 초기 렌더링 시 자동 검색이 실행되는지 확인
   */
  it('초기 렌더링 시 자동 검색이 실행된다', async () => {
    // Given: API 응답 모킹 (총 100개 알림, 첫 페이지)
    const mockResult = createMockPagedResult(0, 50, 100);
    mockFetch.mockResolvedValueOnce({
      ok: true,
      json: async () => mockResult,
    });

    // When: AlertHistoryPage 렌더링
    render(<AlertHistoryPage />);

    // Then: fetch가 호출되어야 함
    await waitFor(() => {
      expect(mockFetch).toHaveBeenCalledTimes(1);
    });

    // Then: 검색 결과가 표시되어야 함
    await waitFor(() => {
      expect(screen.getByText(/총/)).toBeInTheDocument();
      expect(screen.getByText(/100/)).toBeInTheDocument();
    });
  });

  /**
   * 테스트: 날짜 범위 선택 후 검색 버튼 클릭 시 API 호출 확인
   */
  it('날짜 범위 선택 후 검색 버튼 클릭 시 API가 호출된다', async () => {
    // Given: 초기 검색 응답 모킹
    const initialResult = createMockPagedResult(0, 50, 100);
    mockFetch.mockResolvedValueOnce({
      ok: true,
      json: async () => initialResult,
    });

    render(<AlertHistoryPage />);

    // Wait for initial search to complete
    await waitFor(() => {
      expect(mockFetch).toHaveBeenCalledTimes(1);
    });

    // Given: 날짜 범위 검색 응답 모킹
    const searchResult = createMockPagedResult(0, 50, 30);
    mockFetch.mockResolvedValueOnce({
      ok: true,
      json: async () => searchResult,
    });

    // When: 날짜 범위 선택
    const startDateInput = screen.getByLabelText('시작 날짜 선택');
    const endDateInput = screen.getByLabelText('종료 날짜 선택');

    fireEvent.change(startDateInput, { target: { value: '2025-11-01' } });
    fireEvent.change(endDateInput, { target: { value: '2025-11-11' } });

    // When: 검색 버튼 클릭
    const searchButton = screen.getByText('검색');
    fireEvent.click(searchButton);

    // Then: fetch가 추가로 호출되어야 함
    await waitFor(() => {
      expect(mockFetch).toHaveBeenCalledTimes(2);
    });

    // Then: 새로운 검색 결과가 표시되어야 함
    await waitFor(() => {
      expect(screen.getByText(/30/)).toBeInTheDocument();
    });
  });

  /**
   * 테스트: 검색 결과가 테이블로 표시되는지 확인
   */
  it('검색 결과가 테이블로 표시된다', async () => {
    // Given: API 응답 모킹 (3개 알림)
    const mockResult = createMockPagedResult(0, 50, 3);
    mockFetch.mockResolvedValueOnce({
      ok: true,
      json: async () => mockResult,
    });

    // When: AlertHistoryPage 렌더링
    render(<AlertHistoryPage />);

    // Then: 테이블이 표시되어야 함
    await waitFor(() => {
      expect(screen.getByText('알림 ID')).toBeInTheDocument();
      expect(screen.getByText('사용자 ID')).toBeInTheDocument();
      expect(screen.getByText('금액')).toBeInTheDocument();
      expect(screen.getByText('규칙명')).toBeInTheDocument();
      expect(screen.getByText('심각도')).toBeInTheDocument();
      expect(screen.getByText('발생 시각')).toBeInTheDocument();
      expect(screen.getByText('상태')).toBeInTheDocument();
    });

    // Then: 알림 데이터가 표시되어야 함
    await waitFor(() => {
      expect(screen.getByText('user-1')).toBeInTheDocument();
      expect(screen.getByText('user-2')).toBeInTheDocument();
      expect(screen.getByText('user-3')).toBeInTheDocument();
    });
  });

  /**
   * 테스트: 빈 결과 시 메시지가 표시되는지 확인
   */
  it('빈 결과 시 메시지가 표시된다', async () => {
    // Given: API 응답 모킹 (0개 알림)
    const mockResult = createMockPagedResult(0, 50, 0);
    mockFetch.mockResolvedValueOnce({
      ok: true,
      json: async () => mockResult,
    });

    // When: AlertHistoryPage 렌더링
    render(<AlertHistoryPage />);

    // Then: 빈 결과 메시지가 표시되어야 함
    await waitFor(() => {
      expect(screen.getByText('검색 조건에 맞는 알림이 없습니다.')).toBeInTheDocument();
    });
  });

  /**
   * 테스트: API 오류 시 에러 메시지가 표시되는지 확인
   */
  it('API 오류 시 에러 메시지가 표시된다', async () => {
    // Given: API 오류 응답 모킹
    mockFetch.mockResolvedValueOnce({
      ok: false,
      status: 503,
      statusText: 'Service Unavailable',
      text: async () => 'Database connection failed',
    });

    // When: AlertHistoryPage 렌더링
    render(<AlertHistoryPage />);

    // Then: 에러 메시지가 표시되어야 함
    await waitFor(() => {
      expect(screen.getByRole('alert')).toBeInTheDocument();
      expect(screen.getByText(/알림 이력 조회 실패/)).toBeInTheDocument();
    });
  });

  /**
   * 테스트: 로딩 상태가 표시되는지 확인
   */
  it('로딩 중일 때 로딩 상태가 표시된다', async () => {
    // Given: 느린 API 응답 모킹 (500ms 지연)
    mockFetch.mockImplementationOnce(() =>
      new Promise((resolve) =>
        setTimeout(() => {
          resolve({
            ok: true,
            json: async () => createMockPagedResult(0, 50, 10),
          });
        }, 500)
      )
    );

    // When: AlertHistoryPage 렌더링
    render(<AlertHistoryPage />);

    // Then: 로딩 메시지가 표시되어야 함
    expect(screen.getByText('알림을 검색하고 있습니다...')).toBeInTheDocument();

    // Then: 로딩 완료 후 결과가 표시되어야 함
    await waitFor(
      () => {
        expect(screen.queryByText('알림을 검색하고 있습니다...')).not.toBeInTheDocument();
      },
      { timeout: 1000 }
    );
  });

  /**
   * 테스트: 페이지네이션 버튼이 표시되고 동작하는지 확인
   */
  it('페이지네이션 버튼이 표시되고 동작한다', async () => {
    // Given: 첫 페이지 응답 모킹 (총 100개 알림, 페이지 크기 50)
    const firstPageResult = createMockPagedResult(0, 50, 100);
    mockFetch.mockResolvedValueOnce({
      ok: true,
      json: async () => firstPageResult,
    });

    render(<AlertHistoryPage />);

    // Wait for initial search
    await waitFor(() => {
      expect(mockFetch).toHaveBeenCalledTimes(1);
    });

    // Then: 페이지네이션 버튼이 표시되어야 함
    await waitFor(() => {
      expect(screen.getByText('이전')).toBeInTheDocument();
      expect(screen.getByText('다음')).toBeInTheDocument();
      expect(screen.getByText('페이지 1 / 2')).toBeInTheDocument();
    });

    // Given: 두 번째 페이지 응답 모킹
    const secondPageResult = createMockPagedResult(1, 50, 100);
    mockFetch.mockResolvedValueOnce({
      ok: true,
      json: async () => secondPageResult,
    });

    // When: 다음 버튼 클릭
    const nextButton = screen.getByText('다음');
    fireEvent.click(nextButton);

    // Then: fetch가 추가로 호출되어야 함
    await waitFor(() => {
      expect(mockFetch).toHaveBeenCalledTimes(2);
    });

    // Then: 페이지 번호가 업데이트되어야 함
    await waitFor(() => {
      expect(screen.getByText('페이지 2 / 2')).toBeInTheDocument();
    });
  });
});
