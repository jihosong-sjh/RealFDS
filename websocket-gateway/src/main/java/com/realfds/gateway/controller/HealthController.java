package com.realfds.gateway.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 헬스 체크 컨트롤러
 *
 * 서비스 상태를 확인하기 위한 헬스 체크 엔드포인트를 제공합니다.
 *
 * 엔드포인트:
 * - GET /actuator/health: 서비스 상태 반환
 */
@RestController
public class HealthController {

    /**
     * 헬스 체크 엔드포인트
     *
     * @return {"status": "UP"} 형식의 응답
     */
    @GetMapping("/actuator/health")
    public Mono<Map<String, String>> health() {
        return Mono.just(Map.of("status", "UP"));
    }
}
