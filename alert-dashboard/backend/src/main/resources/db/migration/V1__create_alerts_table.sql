-- Alert History 테이블 생성
-- 사기 탐지 알림 이력을 영구적으로 저장하는 테이블

CREATE TABLE alerts (
    -- 고유 식별자
    alert_id UUID PRIMARY KEY,
    schema_version VARCHAR(10) NOT NULL,

    -- 거래 정보
    transaction_id UUID NOT NULL,
    user_id VARCHAR(50) NOT NULL,
    amount BIGINT NOT NULL CHECK (amount > 0),
    currency VARCHAR(3) NOT NULL,
    country_code VARCHAR(2) NOT NULL,

    -- 탐지 규칙 정보
    rule_name VARCHAR(100) NOT NULL,
    reason TEXT NOT NULL CHECK (LENGTH(reason) BETWEEN 10 AND 1000),
    severity VARCHAR(10) NOT NULL CHECK (severity IN ('HIGH', 'MEDIUM', 'LOW')),

    -- 타임스탬프
    alert_timestamp TIMESTAMP WITH TIME ZONE NOT NULL,

    -- 상태 관리
    status VARCHAR(20) NOT NULL DEFAULT 'UNREAD' CHECK (status IN ('UNREAD', 'IN_PROGRESS', 'COMPLETED')),
    assigned_to VARCHAR(50),
    action_note TEXT CHECK (LENGTH(action_note) <= 2000),
    processed_at TIMESTAMP WITH TIME ZONE,

    -- 생성 시각
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    -- 제약 조건: COMPLETED 상태일 때는 processed_at 필수
    CONSTRAINT check_processed_at CHECK (
        (status = 'COMPLETED' AND processed_at IS NOT NULL) OR
        (status != 'COMPLETED')
    )
);

-- 성능 최적화를 위한 인덱스 생성
-- 날짜 범위 검색 및 최신 순 정렬에 사용
CREATE INDEX idx_alert_timestamp ON alerts(alert_timestamp DESC);

-- 규칙명 필터링에 사용
CREATE INDEX idx_rule_name ON alerts(rule_name);

-- 사용자별 필터링에 사용
CREATE INDEX idx_user_id ON alerts(user_id);

-- 상태 필터링에 사용
CREATE INDEX idx_status ON alerts(status);

-- 테이블 및 컬럼 설명 추가 (한국어 주석)
COMMENT ON TABLE alerts IS '사기 탐지 알림 이력';
COMMENT ON COLUMN alerts.alert_id IS '알림 고유 식별자';
COMMENT ON COLUMN alerts.schema_version IS '스키마 버전';
COMMENT ON COLUMN alerts.transaction_id IS '원본 거래 ID';
COMMENT ON COLUMN alerts.user_id IS '사용자 ID';
COMMENT ON COLUMN alerts.amount IS '거래 금액 (KRW)';
COMMENT ON COLUMN alerts.currency IS '통화 코드';
COMMENT ON COLUMN alerts.country_code IS '국가 코드';
COMMENT ON COLUMN alerts.rule_name IS '탐지 규칙명';
COMMENT ON COLUMN alerts.reason IS '알림 발생 이유 (한국어)';
COMMENT ON COLUMN alerts.severity IS '심각도 (HIGH, MEDIUM, LOW)';
COMMENT ON COLUMN alerts.alert_timestamp IS '알림 발생 시각';
COMMENT ON COLUMN alerts.status IS '처리 상태 (UNREAD, IN_PROGRESS, COMPLETED)';
COMMENT ON COLUMN alerts.assigned_to IS '담당자 ID';
COMMENT ON COLUMN alerts.action_note IS '처리 내용 메모';
COMMENT ON COLUMN alerts.processed_at IS '처리 완료 시각';
COMMENT ON COLUMN alerts.created_at IS '데이터베이스 생성 시각';
