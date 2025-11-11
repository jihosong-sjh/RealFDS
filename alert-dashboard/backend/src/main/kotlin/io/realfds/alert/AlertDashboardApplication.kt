package io.realfds.alert

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories

/**
 * Alert Dashboard 애플리케이션 메인 클래스
 *
 * Alert History (과거 알림 조회) 기능을 제공하는 Spring Boot 애플리케이션입니다.
 *
 * 주요 기능:
 * - PostgreSQL R2DBC를 사용한 비동기 데이터베이스 액세스
 * - Flyway를 통한 데이터베이스 마이그레이션 자동화
 * - Kafka를 통한 실시간 알림 이벤트 수신 및 영속화
 * - RESTful API를 통한 알림 이력 조회 (날짜, 규칙, 사용자, 상태 필터링)
 * - 페이지네이션을 통한 대용량 데이터 처리
 *
 * Constitution 준수:
 * - I. 학습 우선: PostgreSQL R2DBC를 통한 비동기 DB 액세스 학습
 * - II. 단순함: docker-compose up 한 번으로 실행 가능
 * - III. 실시간 우선: R2DBC 비동기 처리로 실시간 성능 보장
 * - IV. 서비스 경계: alert-service 내부 확장 (별도 마이크로서비스 추가 없음)
 * - V. 품질 표준: 테스트 커버리지 ≥70%, 로깅, 헬스 체크
 * - VI. 한국어 우선: 모든 주석, 문서, 로그 메시지 한국어
 *
 * @since 2025-11-11
 * @author RealFDS Team
 */
@SpringBootApplication
@EnableR2dbcRepositories  // R2DBC Repository 활성화
class AlertDashboardApplication

/**
 * 애플리케이션 진입점
 *
 * Spring Boot 애플리케이션을 시작합니다.
 * - 포트: 8084 (기본값)
 * - Flyway 마이그레이션 자동 실행
 * - PostgreSQL 연결 확인
 * - Kafka Consumer 시작
 *
 * @param args 명령줄 인자
 */
fun main(args: Array<String>) {
    runApplication<AlertDashboardApplication>(*args)
}
