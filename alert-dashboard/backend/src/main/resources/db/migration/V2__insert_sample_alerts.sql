-- 샘플 알림 데이터 삽입 (개발/테스트 환경 전용)
-- 다양한 규칙명, 심각도, 상태를 가진 샘플 데이터 3개 생성

INSERT INTO alerts (
    alert_id, schema_version, transaction_id, user_id, amount, currency, country_code,
    rule_name, reason, severity, alert_timestamp, status, created_at
) VALUES
    -- 샘플 1: 고액 거래 알림 (미확인 상태)
    (
        '550e8400-e29b-41d4-a716-446655440001', '1.0',
        '660e8400-e29b-41d4-a716-446655440001', 'user-5',
        1500000, 'KRW', 'KR',
        'HIGH_AMOUNT', '거래 금액이 설정된 임계값(1,000,000원)을 초과했습니다',
        'HIGH', '2025-11-11T12:00:00Z', 'UNREAD', '2025-11-11T12:00:01Z'
    ),
    -- 샘플 2: 해외 거래 알림 (진행 중 상태)
    (
        '550e8400-e29b-41d4-a716-446655440002', '1.0',
        '660e8400-e29b-41d4-a716-446655440002', 'user-3',
        850000, 'KRW', 'CN',
        'FOREIGN_COUNTRY', '해외(CN) 국가에서 거래가 발생했습니다',
        'MEDIUM', '2025-11-11T11:30:00Z', 'IN_PROGRESS', '2025-11-11T11:30:01Z'
    ),
    -- 샘플 3: 연속 거래 알림 (완료 상태)
    (
        '550e8400-e29b-41d4-a716-446655440003', '1.0',
        '660e8400-e29b-41d4-a716-446655440003', 'user-7',
        200000, 'KRW', 'KR',
        'RAPID_TRANSACTION', '1분 동안 5건 이상의 거래가 발생했습니다',
        'LOW', '2025-11-11T10:45:00Z', 'COMPLETED', '2025-11-11T10:45:01Z'
    );
