# Fraud Detector Dockerfile
# Apache Flink 1.18.1 + Scala 2.12 기반 실시간 거래 탐지 엔진

# ===========================================
# Stage 1: Build (Gradle로 Fat JAR 빌드)
# ===========================================
FROM gradle:8.5-jdk11 AS builder

WORKDIR /app

# Gradle 캐시 최적화: 의존성 먼저 다운로드
COPY build.gradle settings.gradle ./
COPY gradle/ gradle/
RUN gradle dependencies --no-daemon || true

# 소스 코드 복사 및 빌드
COPY src/ src/
RUN gradle shadowJar --no-daemon

# ===========================================
# Stage 2: Runtime (Flink 공식 이미지 사용)
# ===========================================
FROM flink:1.18.1-scala_2.12-java11

# 메타데이터
LABEL maintainer="RealFDS Team"
LABEL description="Real-time Fraud Detection Engine using Apache Flink"
LABEL version="1.0.0"

# 작업 디렉토리
WORKDIR /opt/flink

# Stage 1에서 빌드된 JAR 파일 복사
COPY --from=builder /app/build/libs/fraud-detector-1.0.0.jar /opt/flink/usrlib/fraud-detector.jar

# 환경 변수 설정
ENV KAFKA_BOOTSTRAP_SERVERS=kafka:9092
ENV HIGH_VALUE_THRESHOLD=1000000
ENV FLINK_CHECKPOINT_INTERVAL=60000
ENV LOG_LEVEL=INFO

# Flink Job Manager 포트 노출
EXPOSE 8081
EXPOSE 6123

# Flink Task Manager 포트 노출
EXPOSE 6121
EXPOSE 6122

# 헬스체크 (JobManager에 Job이 제출되었는지 확인)
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://${JOB_MANAGER_RPC_ADDRESS:-jobmanager}:8081/jobs || exit 1

# Flink Job 실행
# 외부 JobManager에 Job 제출하는 스크립트
# JobManager가 준비될 때까지 대기 후 Job 제출
CMD bash -c ' \
    echo "Waiting for JobManager to be ready..."; \
    while ! curl -sf http://${JOB_MANAGER_RPC_ADDRESS:-jobmanager}:8081/overview > /dev/null; do \
        echo "JobManager not ready, retrying in 5 seconds..."; \
        sleep 5; \
    done; \
    echo "JobManager is ready. Submitting job..."; \
    /opt/flink/bin/flink run \
        --jobmanager ${JOB_MANAGER_RPC_ADDRESS:-jobmanager}:8081 \
        --detached \
        /opt/flink/usrlib/fraud-detector.jar; \
    echo "Job submitted. Keeping container alive..."; \
    tail -f /dev/null \
'
