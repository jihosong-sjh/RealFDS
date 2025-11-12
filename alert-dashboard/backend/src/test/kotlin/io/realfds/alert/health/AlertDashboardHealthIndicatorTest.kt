package io.realfds.alert.health

import io.r2dbc.spi.Connection
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.Result
import io.r2dbc.spi.Statement
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.boot.actuate.health.Status
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

/**
 * AlertDashboardHealthIndicator 단위 테스트
 *
 * 헬스 체크 로직이 올바르게 동작하는지 검증합니다.
 */
class AlertDashboardHealthIndicatorTest {

    /**
     * Given: R2DBC 연결이 정상적으로 동작하는 경우
     * When: health() 메서드를 호출하면
     * Then: UP 상태를 반환해야 합니다
     */
    @Test
    fun `should return UP when database connection is healthy`() {
        // Given: Mock ConnectionFactory, Connection, Statement, Result
        val mockConnectionFactory = mock<ConnectionFactory>()
        val mockConnection = mock<Connection>()
        val mockStatement = mock<Statement>()
        val mockResult = mock<Result>()

        // ConnectionFactory가 Connection을 반환하도록 설정
        whenever(mockConnectionFactory.create()).thenReturn(Mono.just(mockConnection))

        // Connection이 Statement를 반환하도록 설정
        whenever(mockConnection.createStatement("SELECT 1")).thenReturn(mockStatement)

        // Statement가 Result를 반환하도록 설정
        whenever(mockStatement.execute()).thenReturn(Mono.just(mockResult))

        // Result가 성공 값을 반환하도록 설정
        whenever(mockResult.map<String>(org.mockito.kotlin.any())).thenReturn(Mono.just("OK"))

        // Connection close 설정
        whenever(mockConnection.close()).thenReturn(Mono.empty())

        // Health Indicator 생성
        val healthIndicator = AlertDashboardHealthIndicator(mockConnectionFactory)

        // When: health() 메서드 호출
        val healthMono = healthIndicator.health()

        // Then: UP 상태 반환 확인
        StepVerifier.create(healthMono)
            .expectNextMatches { health ->
                health.status == Status.UP &&
                health.details["database"] != null
            }
            .verifyComplete()
    }

    /**
     * Given: R2DBC 연결이 실패하는 경우
     * When: health() 메서드를 호출하면
     * Then: DOWN 상태를 반환해야 합니다
     */
    @Test
    fun `should return DOWN when database connection fails`() {
        // Given: ConnectionFactory가 예외를 발생시키도록 설정
        val mockConnectionFactory = mock<ConnectionFactory>()
        whenever(mockConnectionFactory.create())
            .thenReturn(Mono.error(RuntimeException("Connection failed")))

        // Health Indicator 생성
        val healthIndicator = AlertDashboardHealthIndicator(mockConnectionFactory)

        // When: health() 메서드 호출
        val healthMono = healthIndicator.health()

        // Then: DOWN 상태 반환 확인
        StepVerifier.create(healthMono)
            .expectNextMatches { health ->
                health.status == Status.DOWN &&
                health.details["database"] == "연결 실패"
            }
            .verifyComplete()
    }
}
