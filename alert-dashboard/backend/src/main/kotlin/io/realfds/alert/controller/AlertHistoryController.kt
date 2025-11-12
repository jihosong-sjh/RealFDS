package io.realfds.alert.controller

import io.realfds.alert.dto.AlertSearchCriteria
import io.realfds.alert.dto.PagedAlertResult
import io.realfds.alert.service.AlertHistoryService
import io.realfds.alert.service.DatabaseConnectionException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import java.time.Instant

/**
 * Alert History REST API Controller
 *
 * 알림 이력 조회 엔드포인트를 제공합니다.
 * 검색 조건(날짜 범위, 페이지네이션)을 받아 알림 목록을 반환합니다.
 */
@RestController
@RequestMapping("/api/alerts")
@CrossOrigin(origins = ["http://localhost:3000"]) // 개발 환경에서 CORS 허용
class AlertHistoryController(
    private val alertHistoryService: AlertHistoryService
) {
    private val logger = LoggerFactory.getLogger(AlertHistoryController::class.java)

    /**
     * GET /api/alerts/history - 알림 이력 조회 엔드포인트
     *
     * 쿼리 파라미터를 통해 검색 조건을 받아 알림 목록을 반환합니다.
     *
     * 요청 예시:
     * - GET /api/alerts/history?page=0&size=50
     * - GET /api/alerts/history?startDate=2025-11-01T00:00:00Z&endDate=2025-11-11T23:59:59Z&page=0&size=20
     *
     * @param startDate 검색 시작 날짜 (ISO 8601 형식, optional)
     * @param endDate 검색 종료 날짜 (ISO 8601 형식, optional)
     * @param page 페이지 번호 (0부터 시작, optional, 기본값: 0)
     * @param size 페이지 크기 (1~100, optional, 기본값: 50)
     * @return 페이지네이션된 알림 검색 결과 (200 OK) 또는 에러 응답 (400/503/500)
     */
    @GetMapping("/history")
    fun searchAlerts(
        @RequestParam(required = false) startDate: Instant?,
        @RequestParam(required = false) endDate: Instant?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int
    ): Mono<ResponseEntity<Any>> {
        logger.info("알림 이력 조회 요청: startDate={}, endDate={}, page={}, size={}", startDate, endDate, page, size)

        return try {
            // AlertSearchCriteria 생성 (검증 로직 실행)
            val criteria = AlertSearchCriteria(
                startDate = startDate,
                endDate = endDate,
                page = page,
                size = size
            )

            // 알림 검색 서비스 호출
            alertHistoryService.searchAlerts(criteria)
                .map { result ->
                    logger.info("알림 이력 조회 응답: totalElements={}, currentPage={}", result.totalElements, result.currentPage)
                    ResponseEntity.ok<Any>(result)
                }
                .onErrorResume { error ->
                    handleError(error)
                }
        } catch (e: IllegalArgumentException) {
            // AlertSearchCriteria 검증 실패 (페이지 번호, 크기, 날짜 범위 등)
            logger.warn("잘못된 검색 조건: {}", e.message)
            Mono.just(
                ResponseEntity
                    .badRequest()
                    .body<Any>(ErrorResponse(e.message ?: "잘못된 요청입니다"))
            )
        }
    }

    /**
     * 에러를 처리하고 적절한 HTTP 응답을 반환합니다.
     *
     * - DatabaseConnectionException: 503 Service Unavailable
     * - IllegalArgumentException: 400 Bad Request
     * - 기타 예외: 500 Internal Server Error
     */
    private fun handleError(error: Throwable): Mono<ResponseEntity<Any>> {
        return when (error) {
            is DatabaseConnectionException -> {
                logger.error("데이터베이스 연결 오류", error)
                Mono.just(
                    ResponseEntity
                        .status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body<Any>(ErrorResponse("서비스를 일시적으로 사용할 수 없습니다"))
                )
            }
            is IllegalArgumentException -> {
                logger.warn("잘못된 요청: {}", error.message)
                Mono.just(
                    ResponseEntity
                        .badRequest()
                        .body<Any>(ErrorResponse(error.message ?: "잘못된 요청입니다"))
                )
            }
            else -> {
                logger.error("알 수 없는 오류 발생", error)
                Mono.just(
                    ResponseEntity
                        .internalServerError()
                        .body<Any>(ErrorResponse("알 수 없는 오류가 발생했습니다"))
                )
            }
        }
    }
}

/**
 * 에러 응답 DTO
 *
 * API 에러 발생 시 클라이언트에게 반환하는 응답 형식입니다.
 *
 * @property message 오류 메시지 (한국어)
 */
data class ErrorResponse(
    val message: String
)
