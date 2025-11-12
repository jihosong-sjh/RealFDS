# Alert Dashboard Backend Dockerfile
# Multi-stage build for optimal image size

# Stage 1: Build
FROM gradle:8.5-jdk17-alpine AS build

# 작업 디렉토리 설정
WORKDIR /app

# Gradle 캐시 최적화를 위해 의존성 파일만 먼저 복사
COPY build.gradle.kts settings.gradle.kts gradle.properties* ./
COPY gradle ./gradle

# 의존성 다운로드 (캐싱)
RUN gradle dependencies --no-daemon || true

# 소스 코드 복사
COPY src ./src

# 애플리케이션 빌드 (테스트 제외)
RUN gradle bootJar --no-daemon -x test

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine

# 비root 사용자 생성 (보안)
RUN addgroup -S spring && adduser -S spring -G spring

# 작업 디렉토리 설정
WORKDIR /app

# 빌드 결과물 복사
COPY --from=build /app/build/libs/alert-dashboard.jar ./app.jar

# 로그 디렉토리 생성 및 권한 설정
RUN mkdir -p logs && chown spring:spring app.jar logs

# 사용자 전환
USER spring:spring

# 헬스 체크
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8084/actuator/health || exit 1

# 포트 노출
EXPOSE 8084

# 애플리케이션 실행
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", \
    "app.jar"]
