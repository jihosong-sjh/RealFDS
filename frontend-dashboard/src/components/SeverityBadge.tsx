import React from 'react';
import { Severity, severityDisplay, severityColor } from '../types/severity';

/**
 * SeverityBadge 컴포넌트: 심각도 뱃지 표시
 *
 * Props:
 * - severity: 알림의 심각도 (LOW, MEDIUM, HIGH, CRITICAL)
 *
 * 기능:
 * - 심각도에 따른 색상 코딩 (낮음: 파란색, 보통: 노란색, 높음: 주황색, 긴급: 빨간색)
 * - 한국어 심각도 텍스트 표시
 */
interface SeverityBadgeProps {
  severity: Severity;
}

const SeverityBadge: React.FC<SeverityBadgeProps> = ({ severity }) => {
  const color = severityColor[severity];
  const displayText = severityDisplay[severity];

  return (
    <span
      style={{
        display: 'inline-block',
        padding: '2px 8px',
        borderRadius: '4px',
        fontSize: '12px',
        fontWeight: 'bold',
        color: '#ffffff',
        backgroundColor: color,
      }}
    >
      {displayText}
    </span>
  );
};

export default SeverityBadge;
