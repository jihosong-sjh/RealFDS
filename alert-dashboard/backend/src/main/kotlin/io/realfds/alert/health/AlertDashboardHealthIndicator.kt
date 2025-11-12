package io.realfds.alert.health

import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.slf4j.LoggerFactory
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.ReactiveHealthIndicator
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.time.Duration

/**
 * Alert Dashboard 헬스 체크 인디케이터
 *
 * PostgreSQL 데이터베이스 연결 상태 및 R2DBC 커넥션 풀 상태를 확인합니다.
 * Spring Boot Actuator의 /actuator/health 엔드포인트에서 사용됩니다.
 *
 * @property connectionFactory R2DBC 커넥션 팩토리
 */
@Component
class AlertDashboardHealthIndicator(
    private val connectionFactory: ConnectionFactory
) : ReactiveHealthIndicator {

    private val logger = LoggerFactory.getLogger(AlertDashboardHealthIndicator::class.java)

    companion object {
        private const val VALIDATION_QUERY = "SELECT 1"
        private val TIMEOUT = Duration.ofSeconds(3)
    }

    /**
     * 헬스 체크를 수행합니다.
     *
     * PostgreSQL 데이터베이스에 간단한 쿼리(SELECT 1)를 실행하여 연결 상태를 확인합니다.
     * 쿼리가 성공하면 UP, 실패하거나 타임아웃되면 DOWN을 반환합니다.
     *
     * @return Health 객체 (UP/DOWN 상태 및 상세 정보 포함)
     */
    override fun health(): Mono<Health> {
        return checkDatabaseConnection()
            .map { Health.up().withDetail("database", it).build() }
            .onErrorResume { error ->
                logger.error("데이터베이스 헬스 체크 실패", error)
                Mono.just(
                    Health.down()
                        .withDetail("database", "연결 실패")
                        .withDetail("error", error.message ?: "알 수 없는 오류")
                        .build()
                )
            }
            .timeout(TIMEOUT)
            .onErrorResume { timeoutError ->
                logger.error("데이터베이스 헬스 체크 타임아웃", timeoutError)
                Mono.just(
                    Health.down()
                        .withDetail("database", "타임아웃")
                        .withDetail("timeout", "${TIMEOUT.seconds}초")
                        .build()
                )
            }
    }

    /**
     * 데이터베이스 연결 상태를 확인합니다.
     *
     * R2DBC 커넥션을 획득하고 검증 쿼리를 실행하여 데이터베이스 상태를 확인합니다.
     *
     * @return 데이터베이스 상태 정보 (맵 형태)
     */
    private fun checkDatabaseConnection(): Mono<Map<String, Any>> {
        return Mono.usingWhen(
            Mono.from(connectionFactory.create()),
            { connection ->
                // 검증 쿼리 실행
                Mono.from(connection.createStatement(VALIDATION_QUERY).execute())
                    .flatMap { result ->
                        Mono.from(result.map { _, _ -> "OK" })
                    }
                    .map {
                        mapOf(
                            "status" to "UP",
                            "validationQuery" to VALIDATION_QUERY,
                            "connectionFactory" to connectionFactory.javaClass.simpleName
                        )
                    }
            },
            { connection -> Mono.from(connection.close()) }
        )
    }
}
