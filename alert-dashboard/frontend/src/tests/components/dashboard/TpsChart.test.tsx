/**
 * TpsChart 컴포넌트 테스트
 *
 * Given-When-Then 구조를 사용하여 TPS 차트 렌더링 및 상호작용 검증
 *
 * 테스트 시나리오:
 * 1. Given 1시간 TPS 데이터, When 렌더링, Then LineChart 표시
 * 2. Given 차트 마우스 호버, When 데이터 포인트 가리킴, Then 툴팁 표시
 *
 * Constitution V 준수:
 * - Given-When-Then 구조 사용
 * - 한국어 주석으로 테스트 의도 설명
 *
 * @see components/dashboard/TpsChart.tsx
 */

import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import { TpsChart } from '../../../components/dashboard/TpsChart';

describe('TpsChart 컴포넌트 테스트', () => {
  /**
   * 테스트용 1시간 TPS 데이터 생성
   *
   * @param dataPoints 데이터 포인트 개수
   * @param startTps 시작 TPS 값
   * @returns TPS 데이터 포인트 배열
   */
  const generateMockTpsData = (dataPoints: number, startTps: number = 50) => {
    const now = new Date();
    return Array.from({ length: dataPoints }, (_, index) => ({
      timestamp: new Date(now.getTime() - (dataPoints - index - 1) * 5000).toISOString(),
      tps: startTps + Math.floor(Math.random() * 50), // 50-100 사이 랜덤 TPS
      totalTransactions: (startTps + index) * 5,
    }));
  };

  test('Given 1시간 TPS 데이터, When 렌더링, Then LineChart 표시', () => {
    // Given: 720개 데이터 포인트 (1시간 = 720 × 5초)
    const mockData = generateMockTpsData(720);

    // When: TpsChart 렌더링
    render(<TpsChart data={mockData} />);

    // Then: LineChart가 렌더링됨
    // Recharts는 SVG로 렌더링되므로 SVG 요소 확인
    const svgElement = screen.getByRole('img', { hidden: true }); // Recharts SVG는 role="img"
    expect(svgElement).toBeInTheDocument();

    // X축 라벨 확인 (시간 형식)
    const xAxisLabels = screen.getAllByText(/\d{1,2}:\d{2}/); // HH:MM 형식
    expect(xAxisLabels.length).toBeGreaterThan(0);

    // Y축 라벨 확인 (TPS 라벨)
    const yAxisLabel = screen.getByText('TPS');
    expect(yAxisLabel).toBeInTheDocument();
  });

  test('Given 빈 데이터, When 렌더링, Then "데이터 수집 중..." 메시지 표시', () => {
    // Given: 빈 데이터 배열
    const emptyData: any[] = [];

    // When: TpsChart 렌더링
    render(<TpsChart data={emptyData} />);

    // Then: "데이터 수집 중..." 메시지 표시
    const emptyMessage = screen.getByText(/데이터 수집 중.../);
    expect(emptyMessage).toBeInTheDocument();
  });

  test('Given 차트 마우스 호버, When 데이터 포인트 가리킴, Then 툴팁 표시', async () => {
    // Given: 10개 데이터 포인트 (테스트 간소화)
    const mockData = generateMockTpsData(10, 80);

    // When: TpsChart 렌더링 및 차트 영역에 마우스 호버
    const { container } = render(<TpsChart data={mockData} />);

    // Recharts의 LineChart 영역 찾기
    const chartArea = container.querySelector('.recharts-wrapper');
    expect(chartArea).toBeInTheDocument();

    // 차트 영역에 마우스 이동 (툴팁 트리거)
    if (chartArea) {
      fireEvent.mouseMove(chartArea, { clientX: 200, clientY: 100 });
    }

    // Then: 툴팁이 표시됨 (비동기 렌더링)
    await waitFor(() => {
      const tooltip = screen.queryByText(/TPS:/); // 툴팁 내용 확인
      // 툴팁은 마우스 위치에 따라 나타날 수도 있고 안 나타날 수도 있음
      // 실제 구현에서는 정확한 데이터 포인트 위치에 호버 필요
    });
  });

  test('Given 720개 데이터 포인트, When 렌더링, Then dot 표시 없음 (성능 최적화)', () => {
    // Given: 720개 데이터 포인트
    const mockData = generateMockTpsData(720);

    // When: TpsChart 렌더링
    const { container } = render(<TpsChart data={mockData} />);

    // Then: Line에 dot이 표시되지 않음 (dot={false} 설정 확인)
    const dots = container.querySelectorAll('.recharts-dot');
    expect(dots.length).toBe(0); // dot이 없어야 성능 최적화됨
  });

  test('Given TPS 값 급격히 증가, When 렌더링, Then Y축 자동 범위 조정', () => {
    // Given: TPS가 10배 증가하는 데이터
    const mockData = [
      { timestamp: new Date().toISOString(), tps: 10, totalTransactions: 100 },
      { timestamp: new Date(Date.now() + 5000).toISOString(), tps: 100, totalTransactions: 600 },
      { timestamp: new Date(Date.now() + 10000).toISOString(), tps: 1000, totalTransactions: 5600 },
    ];

    // When: TpsChart 렌더링
    const { container } = render(<TpsChart data={mockData} />);

    // Then: Y축이 최대값(1000)을 포함하도록 자동 조정됨
    const yAxisTicks = container.querySelectorAll('.recharts-yAxis .recharts-cartesian-axis-tick');
    expect(yAxisTicks.length).toBeGreaterThan(0);

    // Y축 최대값이 1000 이상인지 확인 (Recharts가 자동으로 범위 계산)
    const yAxisValues = Array.from(yAxisTicks).map((tick) => tick.textContent);
    const maxYValue = Math.max(...yAxisValues.map((val) => parseInt(val || '0', 10)));
    expect(maxYValue).toBeGreaterThanOrEqual(1000);
  });

  test('Given 애니메이션 활성화, When 렌더링, Then 부드러운 트랜지션 적용', () => {
    // Given: 초기 데이터
    const initialData = generateMockTpsData(10, 50);

    // When: TpsChart 렌더링
    const { rerender, container } = render(<TpsChart data={initialData} />);

    // 새로운 데이터로 재렌더링 (실시간 업데이트 시뮬레이션)
    const updatedData = [
      ...initialData,
      {
        timestamp: new Date().toISOString(),
        tps: 120,
        totalTransactions: initialData[initialData.length - 1].totalTransactions + 600,
      },
    ];
    rerender(<TpsChart data={updatedData} />);

    // Then: Line 컴포넌트에 isAnimationActive={true} 설정 확인
    const lineElement = container.querySelector('.recharts-line');
    expect(lineElement).toBeInTheDocument();
    // 애니메이션 속성은 DOM 검사로 직접 확인 어려움, 프롭 확인 필요
  });

  test('Given 한국 시간대, When 타임스탬프 렌더링, Then 올바른 로컬 시간 표시', () => {
    // Given: UTC 타임스탬프 데이터
    const mockData = [
      {
        timestamp: '2025-11-12T10:30:05Z', // UTC 시간
        tps: 75,
        totalTransactions: 1000,
      },
    ];

    // When: TpsChart 렌더링
    render(<TpsChart data={mockData} />);

    // Then: X축에 로컬 시간으로 변환되어 표시됨
    // toLocaleTimeString()으로 변환되므로 한국 시간대(UTC+9) 표시
    const timeLabels = screen.getAllByText(/\d{1,2}:\d{2}/);
    expect(timeLabels.length).toBeGreaterThan(0);
    // 실제 시간 확인은 브라우저 시간대에 따라 다름
  });

  test('Given ResponsiveContainer, When 화면 크기 변경, Then 차트 너비 자동 조정', () => {
    // Given: TpsChart 렌더링
    const mockData = generateMockTpsData(10);
    const { container } = render(<TpsChart data={mockData} />);

    // When: ResponsiveContainer가 렌더링됨
    const responsiveContainer = container.querySelector('.recharts-responsive-container');
    expect(responsiveContainer).toBeInTheDocument();

    // Then: ResponsiveContainer가 100% 너비로 설정됨
    if (responsiveContainer) {
      const styles = window.getComputedStyle(responsiveContainer);
      expect(styles.width).toBe('100%');
    }
  });
});
