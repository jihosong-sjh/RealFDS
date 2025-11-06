# Alert Service - Dockerfile
# Multi-stage build를 사용하여 빌드 이미지와 실행 이미지를 분리합니다.

# ========================================
# Stage 1: Build Stage (Gradle 빌드)
# ========================================
FROM gradle:8.5-jdk17 AS builder

# 작업 디렉토리 설정
WORKDIR /app

# Gradle 래퍼 및 빌드 스크립트 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# 소스 코드 복사
COPY src src

# 애플리케이션 빌드 (테스트 포함)
# --no-daemon: Gradle 데몬을 사용하지 않음 (컨테이너에서는 불필요)
# -x test: 테스트 제외 (빠른 빌드를 위해, 필요시 제거)
RUN ./gradlew build -x test --no-daemon

# ========================================
# Stage 2: Runtime Stage (실행 이미지)
# ========================================
FROM eclipse-temurin:17-jre-alpine

# 애플리케이션 메타데이터
LABEL maintainer="RealFDS Team"
LABEL description="Alert Service - 실시간 금융 거래 탐지 시스템의 알림 서비스"
LABEL version="1.0.0"

# 작업 디렉토리 설정
WORKDIR /app

# 비특권 사용자 생성 및 전환 (보안 강화)
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# 빌드 스테이지에서 생성된 JAR 파일 복사
# build/libs/alert-service-1.0.0.jar → app.jar
COPY --from=builder /app/build/libs/*.jar app.jar

# 애플리케이션 포트 노출
EXPOSE 8081

# 환경 변수 기본값 설정
ENV SERVER_PORT=8081
ENV KAFKA_BOOTSTRAP_SERVERS=kafka:9092
ENV KAFKA_CONSUMER_GROUP_ID=alert-service-group
ENV JAVA_OPTS="-Xms256m -Xmx512m"

# Health Check 설정
# 30초마다 /actuator/health 엔드포인트 확인
# 3번 연속 실패 시 unhealthy로 판단
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health || exit 1

# Spring Boot 애플리케이션 실행
# -Djava.security.egd=file:/dev/./urandom: 빠른 난수 생성 (시작 시간 단축)
# -Dfile.encoding=UTF-8: UTF-8 인코딩 설정
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -Dfile.encoding=UTF-8 -jar app.jar"]
