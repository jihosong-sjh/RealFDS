# Transaction Generator Dockerfile
# Python 기반 거래 데이터 생성 서비스

# 베이스 이미지: Python 3.11 slim (경량화)
FROM python:3.11-slim

# 작업 디렉토리 설정
WORKDIR /app

# 의존성 파일 복사
COPY requirements.txt .

# Python 의존성 설치
# --no-cache-dir: 캐시 파일 저장하지 않음 (이미지 크기 감소)
RUN pip install --no-cache-dir -r requirements.txt

# 소스 코드 복사
COPY src/ ./src/

# 환경 변수 설정 (기본값, docker-compose에서 오버라이드 가능)
ENV KAFKA_BOOTSTRAP_SERVERS=kafka:9092
ENV TRANSACTION_GENERATION_INTERVAL_MS=1000
ENV TRANSACTION_TOPIC=virtual-transactions
# Python 모듈 검색 경로 설정
ENV PYTHONPATH=/app

# 헬스 체크 (선택적)
# HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
#   CMD python -c "import sys; sys.exit(0)"

# 실행 명령
CMD ["python", "src/main.py"]
