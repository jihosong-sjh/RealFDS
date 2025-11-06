package com.realfds.alert.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Health Check Controller
 *
 * 서비스의 헬스 체크를 담당하는 컨트롤러입니다.
 * Spring Boot Actuator를 사용하지 않고 직접 구현하여 학습 목적으로 사용합니다.
 *
 * 엔드포인트:
 * - GET /actuator/health: 서비스 상태 확인 (항상 {"status": "UP"} 반환)
 */
@RestController
@RequestMapping("/actuator")
public class HealthController {

    /**
     * Health check 엔드포인트
     *
     * 서비스가 정상 실행 중임을 확인하는 간단한 헬스 체크 엔드포인트입니다.
     * Docker 헬스 체크나 로드 밸런서의 상태 확인에 사용할 수 있습니다.
     *
     * @return Mono<Map<String, String>> - {"status": "UP"} JSON 응답
     */
    @GetMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, String>> health() {
        // 서비스가 정상 실행 중임을 나타내는 "UP" 상태 반환
        return Mono.just(Map.of("status", "UP"));
    }
}
