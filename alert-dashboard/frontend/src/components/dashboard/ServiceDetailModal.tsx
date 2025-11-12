import React from 'react';
import { ServiceHealth } from '../../types/metrics';
import './ServiceDetailModal.css';

/**
 * 서비스 상세 정보 모달 컴포넌트
 *
 * 서비스 Health Check 상세 정보 표시
 * - UP 상태: 메모리 사용량, 평균 응답 시간 표시
 * - DOWN 상태: errorType, errorMessage 표시
 *
 * @param serviceHealth - 서비스 Health Check 상태 (null이면 모달 닫힘)
 * @param onClose - 모달 닫기 콜백 함수
 */
interface ServiceDetailModalProps {
  serviceHealth: ServiceHealth | null;
  onClose: () => void;
}

export const ServiceDetailModal: React.FC<ServiceDetailModalProps> = ({
  serviceHealth,
  onClose,
}) => {
  // serviceHealth가 null이면 모달 숨김
  if (!serviceHealth) {
    return null;
  }

  const { serviceName, status, lastChecked, responseTime, memoryUsage, errorType, errorMessage } =
    serviceHealth;

  /**
   * ISO-8601 타임스탬프를 로컬 날짜/시간 문자열로 변환
   *
   * @param isoTimestamp - ISO-8601 형식 타임스탬프
   * @returns 로컬 날짜/시간 문자열 (예: "2025-11-12 10:30:05")
   */
  const formatFullTimestamp = (isoTimestamp: string): string => {
    try {
      const date = new Date(isoTimestamp);
      return date.toLocaleString('ko-KR', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
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
   * 모달 배경 클릭 핸들러
   *
   * 모달 외부 클릭 시 모달 닫기
   */
  const handleBackdropClick = (e: React.MouseEvent<HTMLDivElement>) => {
    if (e.target === e.currentTarget) {
      onClose();
    }
  };

  return (
    <div
      className="modal-backdrop"
      onClick={handleBackdropClick}
      role="dialog"
      aria-modal="true"
      aria-labelledby="modal-title"
    >
      <div className="modal-content">
        {/* 모달 헤더 */}
        <div className="modal-header">
          <h2 id="modal-title" className="modal-title">
            {formatServiceName(serviceName)}
          </h2>
          <button
            className="close-button"
            onClick={onClose}
            aria-label="모달 닫기"
          >
            ✕
          </button>
        </div>

        {/* 모달 본문 */}
        <div className="modal-body">
          {/* 상태 배지 */}
          <div className={`status-section status-${status.toLowerCase()}`}>
            <span className="status-label">현재 상태:</span>
            <span className={`status-value status-${status.toLowerCase()}`}>
              {status}
            </span>
          </div>

          {/* 마지막 확인 시간 */}
          <div className="info-row">
            <span className="info-label">마지막 확인:</span>
            <span className="info-value">{formatFullTimestamp(lastChecked)}</span>
          </div>

          {status === 'UP' ? (
            // UP 상태: 성능 지표 표시
            <>
              <div className="info-section">
                <h3 className="section-title">성능 지표</h3>

                <div className="info-row">
                  <span className="info-label">응답 시간:</span>
                  <span className="info-value">{responseTime}ms</span>
                </div>

                {memoryUsage !== null && (
                  <>
                    <div className="info-row">
                      <span className="info-label">메모리 사용량:</span>
                      <span className="info-value">{memoryUsage} MB</span>
                    </div>

                    {/* 메모리 사용량 시각화 (진행 바) */}
                    <div className="progress-bar-container">
                      <div
                        className="progress-bar"
                        style={{
                          width: `${Math.min((memoryUsage / 2048) * 100, 100)}%`,
                        }}
                        aria-label={`메모리 사용률 ${((memoryUsage / 2048) * 100).toFixed(1)}%`}
                      />
                    </div>
                    <div className="progress-label">
                      메모리 사용률: {((memoryUsage / 2048) * 100).toFixed(1)}% (최대 2GB 기준)
                    </div>
                  </>
                )}
              </div>

              {/* 평균 응답 시간 정보 */}
              {responseTime !== null && (
                <div className="info-section">
                  <h3 className="section-title">응답 시간 분석</h3>
                  <div className="response-time-analysis">
                    {responseTime < 100 && (
                      <span className="analysis-badge excellent">
                        ✅ 매우 빠름 (&lt;100ms)
                      </span>
                    )}
                    {responseTime >= 100 && responseTime < 500 && (
                      <span className="analysis-badge good">
                        ✓ 정상 (100-500ms)
                      </span>
                    )}
                    {responseTime >= 500 && responseTime < 1000 && (
                      <span className="analysis-badge warning">
                        ⚠ 느림 (500-1000ms)
                      </span>
                    )}
                    {responseTime >= 1000 && (
                      <span className="analysis-badge slow">
                        🔴 매우 느림 (&gt;1000ms)
                      </span>
                    )}
                  </div>
                </div>
              )}
            </>
          ) : (
            // DOWN 상태: 에러 정보 표시
            <div className="error-section">
              <h3 className="section-title">오류 정보</h3>

              <div className="info-row error-row">
                <span className="info-label">오류 유형:</span>
                <span className="error-type-value">{errorType}</span>
              </div>

              <div className="error-message-box">
                <span className="error-message-label">오류 메시지:</span>
                <p className="error-message-text">{errorMessage}</p>
              </div>

              {/* 에러 유형별 가이드 */}
              <div className="error-guide">
                {errorType === 'TIMEOUT' && (
                  <p className="guide-text">
                    💡 <strong>타임아웃:</strong> 서비스 응답 시간이 3초를 초과했습니다.
                    서비스 과부하 또는 네트워크 지연을 확인하세요.
                  </p>
                )}
                {errorType === 'HTTP_ERROR' && (
                  <p className="guide-text">
                    💡 <strong>HTTP 에러:</strong> 서비스가 HTTP 에러를 반환했습니다.
                    서비스 로그를 확인하여 내부 오류를 진단하세요.
                  </p>
                )}
                {errorType === 'NETWORK_ERROR' && (
                  <p className="guide-text">
                    💡 <strong>네트워크 에러:</strong> 서비스에 연결할 수 없습니다.
                    서비스가 실행 중인지, 네트워크 설정이 올바른지 확인하세요.
                  </p>
                )}
              </div>
            </div>
          )}
        </div>

        {/* 모달 푸터 */}
        <div className="modal-footer">
          <button
            className="close-button-primary"
            onClick={onClose}
          >
            닫기
          </button>
        </div>
      </div>
    </div>
  );
};
