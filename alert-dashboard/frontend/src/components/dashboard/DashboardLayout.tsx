import React, { useState } from 'react';
import { useWebSocket } from '../../hooks/useWebSocket';
import { ConnectionStatus } from '../common/ConnectionStatus';
import { ServiceHealthCard } from './ServiceHealthCard';
import { ServiceDetailModal } from './ServiceDetailModal';
import { TpsChart } from './TpsChart';
import { TpsMetricsCard } from './TpsMetricsCard';
import { AlertRateChart } from './AlertRateChart';
import { AlertMetricsCard } from './AlertMetricsCard';
import { ServiceHealth } from '../../types/metrics';
import './DashboardLayout.css';

/**
 * DashboardLayout 컴포넌트
 *
 * 실시간 시스템 대시보드의 최상위 레이아웃 컴포넌트입니다.
 * useWebSocket 훅을 사용하여 실시간 메트릭 데이터를 수신하고,
 * 모든 하위 컴포넌트에 데이터를 전달합니다.
 *
 * 레이아웃 구조:
 * - 상단: ConnectionStatus 배너 (연결 상태 표시)
 * - 첫 번째 행: 5개 ServiceHealthCard (그리드 레이아웃)
 * - 두 번째 행: TpsChart + TpsMetricsCard
 * - 세 번째 행: AlertRateChart + AlertMetricsCard
 *
 * @see useWebSocket - WebSocket 연결 관리
 * @see ConnectionStatus - 연결 상태 배너
 * @see ServiceHealthCard - 서비스 상태 카드
 * @see TpsChart - TPS 차트
 * @see AlertRateChart - 알림 발생률 차트
 */
export const DashboardLayout: React.FC = () => {
  /**
   * WebSocket 연결 및 실시간 메트릭 데이터 수신
   */
  const { metricsData, connectionStatus, error, lastUpdate } = useWebSocket(
    'ws://localhost:8083/ws/metrics'
  );

  /**
   * 서비스 상태 카드 클릭 시 표시할 상세 정보 모달 상태
   */
  const [selectedService, setSelectedService] = useState<ServiceHealth | null>(null);

  /**
   * 서비스 상태 카드 클릭 핸들러
   * ServiceDetailModal 열기
   */
  const handleServiceCardClick = (serviceHealth: ServiceHealth) => {
    setSelectedService(serviceHealth);
  };

  /**
   * 서비스 상세 모달 닫기 핸들러
   */
  const handleModalClose = () => {
    setSelectedService(null);
  };

  /**
   * 메트릭 데이터 로딩 중 상태 표시
   */
  if (!metricsData) {
    return (
      <div className="dashboard-layout">
        <ConnectionStatus status={connectionStatus} error={error} />
        <div className="loading-container">
          <div className="loading-spinner"></div>
          <p className="loading-text">데이터 수집 중...</p>
          <p className="loading-subtext">실시간 메트릭을 불러오고 있습니다</p>
        </div>
      </div>
    );
  }

  /**
   * 메트릭 데이터가 있을 경우 대시보드 렌더링
   */
  return (
    <div className="dashboard-layout">
      {/* 연결 상태 배너 (연결 끊김/재연결 중일 때만 표시) */}
      <ConnectionStatus status={connectionStatus} error={error} />

      {/* 대시보드 헤더 */}
      <div className="dashboard-header">
        <h1 className="dashboard-title">실시간 시스템 대시보드</h1>
        <div className="dashboard-subtitle">
          <span className="last-update-label">마지막 업데이트:</span>
          <span className="last-update-time">
            {lastUpdate ? new Date(lastUpdate).toLocaleString('ko-KR') : 'N/A'}
          </span>
        </div>
      </div>

      {/* 첫 번째 행: 5개 서비스 상태 카드 (그리드 레이아웃) */}
      <section className="dashboard-section services-section">
        <h2 className="section-title">서비스 상태 모니터링</h2>
        <div className="services-grid">
          {metricsData.services.map((service) => (
            <ServiceHealthCard
              key={service.serviceName}
              serviceHealth={service}
              onCardClick={handleServiceCardClick}
            />
          ))}
        </div>
      </section>

      {/* 두 번째 행: TPS 차트 + TPS 메트릭 카드 */}
      <section className="dashboard-section tps-section">
        <h2 className="section-title">실시간 거래량 추이 (TPS)</h2>
        <div className="metrics-row">
          <div className="chart-container">
            <TpsChart data={metricsData.transactionMetrics} />
          </div>
          <div className="metrics-card-container">
            <TpsMetricsCard
              current={metricsData.transactionMetrics.tps}
              average={
                metricsData.transactionMetrics.history
                  ? metricsData.transactionMetrics.history.reduce((sum, dp) => sum + dp.tps, 0) /
                    metricsData.transactionMetrics.history.length
                  : 0
              }
              max={
                metricsData.transactionMetrics.history
                  ? Math.max(...metricsData.transactionMetrics.history.map((dp) => dp.tps))
                  : 0
              }
            />
          </div>
        </div>
      </section>

      {/* 세 번째 행: 알림 발생률 차트 + 알림 메트릭 카드 */}
      <section className="dashboard-section alerts-section">
        <h2 className="section-title">실시간 알림 발생률 추이</h2>
        <div className="metrics-row">
          <div className="chart-container">
            <AlertRateChart data={metricsData.alertMetrics} />
          </div>
          <div className="metrics-card-container">
            <AlertMetricsCard
              current={metricsData.alertMetrics.alertsPerMinute}
              average={
                metricsData.alertMetrics.history
                  ? metricsData.alertMetrics.history.reduce(
                      (sum, dp) => sum + dp.alertsPerMinute,
                      0
                    ) / metricsData.alertMetrics.history.length
                  : 0
              }
              max={
                metricsData.alertMetrics.history
                  ? Math.max(...metricsData.alertMetrics.history.map((dp) => dp.alertsPerMinute))
                  : 0
              }
            />
          </div>
        </div>
      </section>

      {/* 서비스 상세 정보 모달 */}
      {selectedService && (
        <ServiceDetailModal serviceHealth={selectedService} onClose={handleModalClose} />
      )}

      {/* 대시보드 푸터 */}
      <footer className="dashboard-footer">
        <p className="footer-text">
          RealFDS - Real-time Fraud Detection System | 실시간 사기 거래 탐지 시스템
        </p>
        <p className="footer-subtext">
          메트릭 업데이트 주기: 5초 | 데이터 보관 기간: 1시간
        </p>
      </footer>
    </div>
  );
};

export default DashboardLayout;
