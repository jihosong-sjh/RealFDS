package io.realfds.alert.repository

import io.realfds.alert.domain.Alert
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Alert 엔티티 Repository
 *
 * Spring Data R2DBC의 ReactiveCrudRepository를 확장하여
 * 기본 CRUD 메서드 (save, findById, findAll, delete 등)를 상속받습니다.
 *
 * 이 인터페이스는 Alert 엔티티의 영속화를 담당하며,
 * 리액티브 스트림 (Mono, Flux)을 사용하여 비동기 방식으로 데이터베이스에 액세스합니다.
 *
 * 동적 쿼리 및 복잡한 검색 조건은 CustomAlertRepository에서 처리합니다.
 */
@Repository
interface AlertRepository : ReactiveCrudRepository<Alert, UUID>
