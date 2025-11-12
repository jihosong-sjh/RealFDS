import React from 'react';
import { ServiceHealth } from '../../types/metrics';
import './ServiceHealthCard.css';

/**
 * 서비스 상태 카드 컴포넌트
 *
 * 서비스 이름, 상태(UP/DOWN), 마지막 확인 시간 표시
 * - UP: 녹색 카드
 * - DOWN: 빨간색 카드
 * - 클릭 시 상세 정보 모달 열기 (onCardClick prop)
 *
 * @param serviceHealth - 서비스 Health Check 상태
 * @param onCardClick - 카드 클릭 시 호출되는 콜백 함수
 */
interface ServiceHealthCardProps {
  serviceHealth: ServiceHealth;
  onCardClick: (serviceHealth: ServiceHealth) => void;
}

export const ServiceHealthCard: React.FC<ServiceHealthCardProps> = ({
  serviceHealth,
  onCardClick,
}) => {
  const { serviceName, status, lastChecked, responseTime, memoryUsage, errorType } = serviceHealth;

  /**
   * ISO-8601 타임스탬프를 로컬 시간 문자열로 변환
   *
   * @param isoTimestamp - ISO-8601 형식 타임스탬프
   * @returns 로컬 시간 문자열 (예: "10:30:05")
   */
  const formatTimestamp = (isoTimestamp: string): string => {
    try {
      const date = new Date(isoTimestamp);
      return date.toLocaleTimeString('ko-KR', {
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
      });
    } catch (e) {
      return 'N/A';
    }
  };

  /**
   * 서비스 이름을 표시용 형식으로 변환
   *
   * @param name - 서비스 이름 (예: "transaction-generator")
   * @returns 표시용 이름 (예: "Transaction Generator")
   */
  const formatServiceName = (name: string): string => {
    return name
      .split('-')
      .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
      .join(' ');
  };

  /**
   * 카드 클릭 핸들러
   *
   * 상세 정보 모달을 열기 위해 부모 컴포넌트에 서비스 상태 전달
   */
  const handleClick = () => {
    onCardClick(serviceHealth);
  };

  return (
    <div
      className={`service-health-card status-${status.toLowerCase()}`}
      onClick={handleClick}
      role="button"
      tabIndex={0}
      onKeyPress={(e) => {
        if (e.key === 'Enter') handleClick();
      }}
      aria-label={`${serviceName} 서비스 상태: ${status}`}
    >
      {/* 서비스 이름 헤더 */}
      <div className="card-header">
        <h3 className="service-name">{formatServiceName(serviceName)}</h3>
        <span className={`status-badge status-${status.toLowerCase()}`}>
          {status}
        </span>
      </div>

      {/* 상태 정보 */}
      <div className="card-body">
        {status === 'UP' ? (
          // UP 상태: 응답 시간 및 메모리 사용량 표시
          <>
            <div className="metric-row">
              <span className="metric-label">응답 시간:</span>
              <span className="metric-value">{responseTime}ms</span>
            </div>
            {memoryUsage !== null && (
              <div className="metric-row">
                <span className="metric-label">메모리 사용량:</span>
                <span className="metric-value">{memoryUsage} MB</span>
              </div>
            )}
          </>
        ) : (
          // DOWN 상태: 에러 타입 표시
          <div className="error-info">
            <span className="error-type">{errorType}</span>
          </div>
        )}

        {/* 마지막 확인 시간 */}
        <div className="last-checked">
          <span className="last-checked-label">마지막 확인:</span>
          <span className="last-checked-value">{formatTimestamp(lastChecked)}</span>
        </div>
      </div>

      {/* 클릭 가능 아이콘 */}
      <div className="card-footer">
        <span className="more-info-icon">ℹ️ 상세 정보</span>
      </div>
    </div>
  );
};
