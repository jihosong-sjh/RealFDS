import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import { ServiceHealthCard } from '../../../components/dashboard/ServiceHealthCard';
import { ServiceHealth } from '../../../types/metrics';

/**
 * ServiceHealthCard 컴포넌트 테스트
 *
 * Given-When-Then 구조를 사용하여 서비스 상태 카드 UI 검증
 *
 * @see ../../../components/dashboard/ServiceHealthCard.tsx
 */
describe('ServiceHealthCard 컴포넌트 테스트', () => {
  // 테스트 데이터 준비
  const upServiceData: ServiceHealth = {
    serviceName: 'transaction-generator',
    status: 'UP',
    lastChecked: '2025-11-12T10:30:05Z',
    responseTime: 45,
    memoryUsage: 128,
    errorType: null,
    errorMessage: null,
  };

  const downServiceData: ServiceHealth = {
    serviceName: 'fraud-detector',
    status: 'DOWN',
    lastChecked: '2025-11-12T10:30:02Z',
    responseTime: null,
    memoryUsage: null,
    errorType: 'TIMEOUT',
    errorMessage: 'Health check 응답 시간 초과 (>3초)',
  };

  test('Given UP 상태 서비스, When 렌더링, Then 녹색 카드 표시', () => {
    // Given: UP 상태 서비스 데이터
    const mockOnCardClick = jest.fn();

    // When: 컴포넌트 렌더링
    const { container } = render(
      <ServiceHealthCard
        serviceHealth={upServiceData}
        onCardClick={mockOnCardClick}
      />
    );

    // Then: 서비스 이름 표시
    expect(screen.getByText('transaction-generator')).toBeInTheDocument();

    // Then: UP 상태 표시
    expect(screen.getByText('UP')).toBeInTheDocument();

    // Then: 녹색 배경 클래스 적용
    const card = container.querySelector('.service-health-card');
    expect(card).toHaveClass('status-up');
    expect(card).not.toHaveClass('status-down');

    // Then: 응답 시간 표시
    expect(screen.getByText(/45ms/i)).toBeInTheDocument();

    // Then: 마지막 확인 시간 표시
    expect(screen.getByText(/10:30:05/i)).toBeInTheDocument();
  });

  test('Given DOWN 상태 서비스, When 렌더링, Then 빨간색 카드 표시', () => {
    // Given: DOWN 상태 서비스 데이터
    const mockOnCardClick = jest.fn();

    // When: 컴포넌트 렌더링
    const { container } = render(
      <ServiceHealthCard
        serviceHealth={downServiceData}
        onCardClick={mockOnCardClick}
      />
    );

    // Then: 서비스 이름 표시
    expect(screen.getByText('fraud-detector')).toBeInTheDocument();

    // Then: DOWN 상태 표시
    expect(screen.getByText('DOWN')).toBeInTheDocument();

    // Then: 빨간색 배경 클래스 적용
    const card = container.querySelector('.service-health-card');
    expect(card).toHaveClass('status-down');
    expect(card).not.toHaveClass('status-up');

    // Then: 에러 타입 표시
    expect(screen.getByText(/TIMEOUT/i)).toBeInTheDocument();

    // Then: 응답 시간 미표시 (null)
    expect(screen.queryByText(/ms/i)).not.toBeInTheDocument();
  });

  test('Given 상태 카드 클릭, When 클릭 이벤트, Then 상세 정보 모달 열림', () => {
    // Given: 클릭 핸들러 모킹
    const mockOnCardClick = jest.fn();

    // When: 컴포넌트 렌더링
    const { container } = render(
      <ServiceHealthCard
        serviceHealth={upServiceData}
        onCardClick={mockOnCardClick}
      />
    );

    // When: 카드 클릭
    const card = container.querySelector('.service-health-card');
    fireEvent.click(card!);

    // Then: 클릭 핸들러 호출됨
    expect(mockOnCardClick).toHaveBeenCalledTimes(1);
    expect(mockOnCardClick).toHaveBeenCalledWith(upServiceData);
  });

  test('Given DOWN 상태 서비스, When 클릭, Then 에러 메시지가 모달에 전달됨', () => {
    // Given: DOWN 상태 서비스
    const mockOnCardClick = jest.fn();

    // When: 컴포넌트 렌더링 및 클릭
    const { container } = render(
      <ServiceHealthCard
        serviceHealth={downServiceData}
        onCardClick={mockOnCardClick}
      />
    );

    const card = container.querySelector('.service-health-card');
    fireEvent.click(card!);

    // Then: 에러 정보가 클릭 핸들러에 전달됨
    expect(mockOnCardClick).toHaveBeenCalledWith(
      expect.objectContaining({
        errorType: 'TIMEOUT',
        errorMessage: 'Health check 응답 시간 초과 (>3초)',
      })
    );
  });

  test('Given 5개 서비스 카드, When 각 카드 렌더링, Then 고유한 서비스명 표시', () => {
    // Given: 5개 서비스 데이터
    const services: ServiceHealth[] = [
      { ...upServiceData, serviceName: 'transaction-generator' },
      { ...upServiceData, serviceName: 'fraud-detector' },
      { ...upServiceData, serviceName: 'alert-service' },
      { ...upServiceData, serviceName: 'websocket-gateway' },
      { ...upServiceData, serviceName: 'frontend-dashboard' },
    ];

    const mockOnCardClick = jest.fn();

    // When: 5개 카드 렌더링
    const { container } = render(
      <>
        {services.map((service) => (
          <ServiceHealthCard
            key={service.serviceName}
            serviceHealth={service}
            onCardClick={mockOnCardClick}
          />
        ))}
      </>
    );

    // Then: 5개 카드 모두 렌더링됨
    const cards = container.querySelectorAll('.service-health-card');
    expect(cards).toHaveLength(5);

    // Then: 각 서비스명이 고유하게 표시됨
    expect(screen.getByText('transaction-generator')).toBeInTheDocument();
    expect(screen.getByText('fraud-detector')).toBeInTheDocument();
    expect(screen.getByText('alert-service')).toBeInTheDocument();
    expect(screen.getByText('websocket-gateway')).toBeInTheDocument();
    expect(screen.getByText('frontend-dashboard')).toBeInTheDocument();
  });

  test('Given UP 상태 서비스, When 메모리 사용량 표시, Then MB 단위로 표시', () => {
    // Given: 메모리 사용량 128MB
    const mockOnCardClick = jest.fn();

    // When: 컴포넌트 렌더링
    render(
      <ServiceHealthCard
        serviceHealth={upServiceData}
        onCardClick={mockOnCardClick}
      />
    );

    // Then: 메모리 사용량이 MB 단위로 표시됨
    expect(screen.getByText(/128\s*MB/i)).toBeInTheDocument();
  });

  test('Given DOWN 상태 서비스, When 메모리 사용량 null, Then 메모리 정보 미표시', () => {
    // Given: DOWN 상태 (메모리 사용량 null)
    const mockOnCardClick = jest.fn();

    // When: 컴포넌트 렌더링
    render(
      <ServiceHealthCard
        serviceHealth={downServiceData}
        onCardClick={mockOnCardClick}
      />
    );

    // Then: 메모리 정보가 표시되지 않음
    expect(screen.queryByText(/MB/i)).not.toBeInTheDocument();
  });

  test('Given lastChecked 시각, When 렌더링, Then 사람이 읽기 쉬운 형식으로 표시', () => {
    // Given: ISO-8601 타임스탬프
    const mockOnCardClick = jest.fn();

    // When: 컴포넌트 렌더링
    render(
      <ServiceHealthCard
        serviceHealth={upServiceData}
        onCardClick={mockOnCardClick}
      />
    );

    // Then: 시각이 로컬 시간 형식으로 표시됨 (예: 10:30:05)
    expect(screen.getByText(/10:30/i)).toBeInTheDocument();
  });
});
