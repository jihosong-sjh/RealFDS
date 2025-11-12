package io.realfds.alert.dto

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * AlertSearchCriteria 검증 단위 테스트
 *
 * AlertSearchCriteria의 필드 검증 로직을 테스트합니다.
 * Given-When-Then 구조를 따릅니다.
 */
class AlertSearchCriteriaTest {

    @Test
    fun `유효한 검색 조건으로 AlertSearchCriteria를 생성할 수 있다`() {
        // Given: 유효한 검색 조건
        val startDate = Instant.now().minus(7, ChronoUnit.DAYS)
        val endDate = Instant.now()
        val page = 0
        val size = 50

        // When: AlertSearchCriteria 생성
        val criteria = AlertSearchCriteria(
            startDate = startDate,
            endDate = endDate,
            page = page,
            size = size
        )

        // Then: 모든 필드가 올바르게 설정됨
        assertThat(criteria.startDate).isEqualTo(startDate)
        assertThat(criteria.endDate).isEqualTo(endDate)
        assertThat(criteria.page).isEqualTo(page)
        assertThat(criteria.size).isEqualTo(size)
    }

    @Test
    fun `기본값으로 AlertSearchCriteria를 생성할 수 있다`() {
        // Given & When: 기본값으로 생성
        val criteria = AlertSearchCriteria()

        // Then: 기본값이 올바르게 설정됨
        assertThat(criteria.startDate).isNull()
        assertThat(criteria.endDate).isNull()
        assertThat(criteria.page).isEqualTo(0)
        assertThat(criteria.size).isEqualTo(50)
    }

    @Test
    fun `페이지 번호는 0 이상이어야 한다`() {
        // Given: 음수 페이지 번호
        val invalidPage = -1

        // When & Then: IllegalArgumentException 발생
        assertThatThrownBy {
            AlertSearchCriteria(page = invalidPage)
        }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("페이지 번호는 0 이상이어야 합니다")
    }

    @Test
    fun `페이지 크기는 1 이상이어야 한다`() {
        // Given: 0 이하의 페이지 크기
        val invalidSize = 0

        // When & Then: IllegalArgumentException 발생
        assertThatThrownBy {
            AlertSearchCriteria(size = invalidSize)
        }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("페이지 크기는 1~100 사이여야 합니다")
    }

    @Test
    fun `페이지 크기는 100 이하여야 한다`() {
        // Given: 100 초과하는 페이지 크기
        val invalidSize = 101

        // When & Then: IllegalArgumentException 발생
        assertThatThrownBy {
            AlertSearchCriteria(size = invalidSize)
        }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("페이지 크기는 1~100 사이여야 합니다")
    }

    @Test
    fun `페이지 크기 1은 유효하다`() {
        // Given & When: 페이지 크기 1
        val criteria = AlertSearchCriteria(size = 1)

        // Then: 성공적으로 생성됨
        assertThat(criteria.size).isEqualTo(1)
    }

    @Test
    fun `페이지 크기 100은 유효하다`() {
        // Given & When: 페이지 크기 100
        val criteria = AlertSearchCriteria(size = 100)

        // Then: 성공적으로 생성됨
        assertThat(criteria.size).isEqualTo(100)
    }

    @Test
    fun `시작 날짜는 종료 날짜보다 늦을 수 없다`() {
        // Given: 시작 날짜가 종료 날짜보다 늦음
        val startDate = Instant.now()
        val endDate = Instant.now().minus(7, ChronoUnit.DAYS)

        // When & Then: IllegalArgumentException 발생
        assertThatThrownBy {
            AlertSearchCriteria(startDate = startDate, endDate = endDate)
        }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("시작 날짜는 종료 날짜보다 늦을 수 없습니다")
    }

    @Test
    fun `시작 날짜와 종료 날짜가 같은 경우 유효하다`() {
        // Given: 시작 날짜와 종료 날짜가 동일
        val sameDate = Instant.now()

        // When: AlertSearchCriteria 생성
        val criteria = AlertSearchCriteria(startDate = sameDate, endDate = sameDate)

        // Then: 성공적으로 생성됨
        assertThat(criteria.startDate).isEqualTo(sameDate)
        assertThat(criteria.endDate).isEqualTo(sameDate)
    }

    @Test
    fun `시작 날짜는 미래일 수 없다`() {
        // Given: 미래의 시작 날짜
        val futureDate = Instant.now().plus(1, ChronoUnit.DAYS)

        // When & Then: IllegalArgumentException 발생
        assertThatThrownBy {
            AlertSearchCriteria(startDate = futureDate)
        }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("시작 날짜는 미래일 수 없습니다")
    }

    @Test
    fun `종료 날짜는 미래일 수 없다`() {
        // Given: 미래의 종료 날짜
        val futureDate = Instant.now().plus(1, ChronoUnit.DAYS)

        // When & Then: IllegalArgumentException 발생
        assertThatThrownBy {
            AlertSearchCriteria(endDate = futureDate)
        }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("종료 날짜는 미래일 수 없습니다")
    }

    @Test
    fun `시작 날짜만 지정할 수 있다`() {
        // Given: 시작 날짜만 지정
        val startDate = Instant.now().minus(7, ChronoUnit.DAYS)

        // When: AlertSearchCriteria 생성
        val criteria = AlertSearchCriteria(startDate = startDate, endDate = null)

        // Then: 성공적으로 생성됨
        assertThat(criteria.startDate).isEqualTo(startDate)
        assertThat(criteria.endDate).isNull()
    }

    @Test
    fun `종료 날짜만 지정할 수 있다`() {
        // Given: 종료 날짜만 지정
        val endDate = Instant.now()

        // When: AlertSearchCriteria 생성
        val criteria = AlertSearchCriteria(startDate = null, endDate = endDate)

        // Then: 성공적으로 생성됨
        assertThat(criteria.startDate).isNull()
        assertThat(criteria.endDate).isEqualTo(endDate)
    }

    @Test
    fun `날짜 범위를 지정하지 않을 수 있다`() {
        // Given & When: 날짜 범위 미지정
        val criteria = AlertSearchCriteria(startDate = null, endDate = null)

        // Then: 성공적으로 생성됨
        assertThat(criteria.startDate).isNull()
        assertThat(criteria.endDate).isNull()
    }

    @Test
    fun `유효한 날짜 범위 - 7일 전부터 오늘까지`() {
        // Given: 7일 전부터 오늘까지
        val startDate = Instant.now().minus(7, ChronoUnit.DAYS)
        val endDate = Instant.now()

        // When: AlertSearchCriteria 생성
        val criteria = AlertSearchCriteria(startDate = startDate, endDate = endDate)

        // Then: 성공적으로 생성됨
        assertThat(criteria.startDate).isEqualTo(startDate)
        assertThat(criteria.endDate).isEqualTo(endDate)
    }
}
