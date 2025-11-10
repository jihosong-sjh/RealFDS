# Feature Specification: 알림 통계 및 분석 (Alert Analytics)

**Feature Branch**: `005-alert-analytics`
**Created**: 2025-11-10
**Status**: Draft
**Prerequisites**: 003-alert-history (PostgreSQL 필요)

## 비전 및 목적

### 무엇을 만드는가
과거 알림 데이터를 집계하여 시간대별, 규칙별, 사용자별 통계를 제공하고, 사기 패턴 분석 및 리포팅을 지원하는 분석 시스템. 차트 시각화와 CSV 내보내기 기능 포함.

### 왜 만드는가
- **패턴 발견**: 시간대별, 요일별 사기 발생 패턴 파악
- **규칙 효과성**: 어떤 탐지 규칙이 가장 많이 트리거되는지 분석
- **리스크 관리**: 고위험 사용자 및 행동 패턴 식별
- **리포팅**: 주간/월간 통계 리포트를 CSV로 내보내기

### 성공 지표
- 집계 쿼리 응답 시간 **<1초** (10,000개 알림 기준)
- CSV 내보내기 속도 **<3초** (10,000개 알림 기준)
- 차트 시각화 명확성 **90% 이상** (사용자 테스트)
- 일/주/월 단위 통계 정확도 **100%**

## 사용자 및 페르소나

### 주요 사용자: 금융 보안 팀 리더
- **이름**: 박분석 (35세, 8년 경력)
- **목표**:
  - 주간/월간 사기 동향 리포트 작성
  - 탐지 규칙의 효과성 평가
  - 고위험 시간대 및 사용자 식별
  - 경영진에게 데이터 기반 인사이트 제공
- **문제점**:
  - 수동으로 데이터를 집계하는 번거로움
  - Excel로 차트 만드는 시간 소모
  - 실시간 데이터는 있지만 트렌드 분석 어려움

### 부차적 사용자: 컴플라이언스 담당자
- **목표**: 월간 사기 탐지 리포트를 규제 기관에 제출
- **니즈**: CSV 형식의 상세 통계 데이터

## User Scenarios & Testing

### User Story 1 - 시간대별 알림 추이 분석 (Priority: P1)

보안 팀 리더가 시간대별(24시간) 알림 발생 추이를 차트로 확인하여 고위험 시간대를 파악하는 기능

**Acceptance Scenarios**:

1. **Given** 분석 페이지에 접속했을 때, **When** "시간대별 추이" 탭을 클릭하면, **Then** 최근 24시간의 시간대별 알림 발생 수가 막대 그래프로 표시됨

2. **Given** 특정 날짜를 선택했을 때, **When** 날짜를 입력하고 "조회" 버튼을 클릭하면, **Then** 해당 날짜의 24시간 시간대별 알림이 표시됨

3. **Given** 시간대별 차트를 확인할 때, **When** 특정 시간대 막대를 클릭하면, **Then** 해당 시간대의 상세 알림 목록으로 이동함

4. **Given** 시간대별 데이터를 확인할 때, **When** 차트 아래 테이블을 보면, **Then** 시간대별 알림 수가 숫자로 정리되어 표시됨

---

### User Story 2 - 규칙별 알림 비율 분석 (Priority: P1)

보안 팀 리더가 탐지 규칙별 알림 발생 비율을 파이 차트로 확인하여 어떤 규칙이 가장 효과적인지 평가하는 기능

**Acceptance Scenarios**:

1. **Given** 분석 페이지에서 "규칙별 분석" 탭을 선택했을 때, **When** 기간(최근 7일)을 선택하고 조회하면, **Then** 규칙별(HIGH_VALUE, FOREIGN_COUNTRY, HIGH_FREQUENCY) 알림 비율이 파이 차트로 표시됨

2. **Given** 파이 차트를 확인할 때, **When** 특정 규칙 섹션을 클릭하면, **Then** 해당 규칙의 상세 통계(총 건수, 비율, 완료율)가 모달로 표시됨

3. **Given** 규칙별 데이터를 확인할 때, **When** 차트 옆의 테이블을 보면, **Then** 규칙명, 건수, 비율(%), 완료율이 표시됨

4. **Given** 기간을 변경했을 때, **When** 일/주/월 버튼을 클릭하면, **Then** 해당 기간의 규칙별 통계로 즉시 업데이트됨

---

### User Story 3 - CSV 내보내기 (Priority: P2)

보안 팀 리더가 분석 결과를 CSV 파일로 내보내어 리포트 작성 또는 추가 분석에 활용하는 기능

**Acceptance Scenarios**:

1. **Given** 시간대별 추이 차트를 확인한 상태에서, **When** "CSV 내보내기" 버튼을 클릭하면, **Then** 3초 이내에 `hourly-alerts-2025-11-10.csv` 파일이 다운로드됨

2. **Given** 규칙별 분석 데이터를 확인한 상태에서, **When** "CSV 내보내기" 버튼을 클릭하면, **Then** `by-rule-alerts-2025-11-01-to-2025-11-07.csv` 파일이 다운로드됨

3. **Given** CSV 파일을 열었을 때, **When** 파일 내용을 확인하면, **Then** UTF-8 인코딩으로 한글이 정상 표시되고, 헤더 행이 포함되어 있음

4. **Given** 대량 데이터(10,000개 알림)를 내보낼 때, **When** CSV 내보내기를 실행하면, **Then** 3초 이내에 파일이 생성되고 다운로드됨

---

## Requirements

### Functional Requirements

#### 시간대별 분석
- **FR-001**: 24시간 시간대별 알림 발생 수를 집계할 수 있어야 함
- **FR-002**: 특정 날짜의 시간대별 알림을 조회할 수 있어야 함
- **FR-003**: 시간대별 데이터를 막대 그래프로 시각화해야 함
- **FR-004**: 시간대별 데이터를 테이블로도 표시해야 함

#### 규칙별 분석
- **FR-005**: 규칙별 알림 발생 건수 및 비율을 집계할 수 있어야 함
- **FR-006**: 일/주/월 단위로 기간을 선택하여 조회할 수 있어야 함
- **FR-007**: 규칙별 데이터를 파이 차트로 시각화해야 함
- **FR-008**: 규칙별 완료율(COMPLETED 상태 비율)도 함께 표시해야 함

#### CSV 내보내기
- **FR-009**: 시간대별 통계를 CSV로 내보낼 수 있어야 함
- **FR-010**: 규칙별 통계를 CSV로 내보낼 수 있어야 함
- **FR-011**: CSV 파일은 UTF-8 BOM 인코딩을 사용하여 Excel 호환성 보장
- **FR-012**: CSV 파일명은 날짜 및 분석 유형이 포함되어야 함

#### 성능
- **FR-013**: 10,000개 알림에서 집계 쿼리 응답 시간 <1초
- **FR-014**: CSV 생성 시간 <3초 (10,000개 기준)

### Key Entities

- **HourlyAlertStats**:
  - `hour`: number (0-23)
  - `count`: number
  - `date`: Date

- **RuleAlertStats**:
  - `ruleName`: string
  - `count`: number
  - `percentage`: number
  - `completedCount`: number
  - `completionRate`: number

## Success Criteria

- **SC-001**: 집계 쿼리 응답 시간 평균 500ms, p95 1초
- **SC-002**: CSV 내보내기 10,000개 알림 기준 3초 이내
- **SC-003**: 차트 시각화 정확도 100% (데이터와 차트 일치)
- **SC-004**: CSV 파일 Excel 호환성 100%

## 시스템 경계

### In Scope
✅ 시간대별(24시간) 알림 추이
✅ 규칙별 알림 비율 분석
✅ 일/주/월 단위 기간 선택
✅ 막대 그래프, 파이 차트 시각화
✅ CSV 내보내기 (UTF-8 BOM)
✅ PostgreSQL 집계 쿼리

### Out of Scope
❌ 사용자별 통계 (향후 개선)
❌ 요일별 통계
❌ 금액 분포 분석
❌ 국가별 통계
❌ PDF 리포트 생성
❌ 이메일 자동 발송
❌ 커스텀 차트 설정

## 제약사항 및 가정

### Technical Constraints
- PostgreSQL 집계 쿼리 사용
- Chart.js 라이브러리
- 최대 데이터 범위: 1년

### Assumptions
- 분석 페이지 동시 사용자 3명 이하
- CSV 파일 크기 <10MB

## Dependencies

### Prerequisite Features
- **003-alert-history**: PostgreSQL 알림 데이터

## API Design

```
GET /api/analytics/hourly?date={YYYY-MM-DD}
  Response: { hour: number, count: number }[]

GET /api/analytics/by-rule?startDate={}&endDate={}
  Response: {
    ruleName: string,
    count: number,
    percentage: number,
    completedCount: number,
    completionRate: number
  }[]

GET /api/analytics/export/hourly?date={}&format=csv
  Response: CSV file

GET /api/analytics/export/by-rule?startDate={}&endDate={}&format=csv
  Response: CSV file
```

---

**Note**: 이 spec은 골격 버전입니다. `/speckit.specify` 명령으로 검증 및 개선 가능합니다.
