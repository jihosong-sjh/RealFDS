"""
Transaction Generator 메인 진입점
주기적으로 거래 데이터를 생성하여 Kafka로 발행합니다.
"""
import os
import sys
import time
import signal
from typing import NoReturn
from dotenv import load_dotenv
from loguru import logger

from src.generator import generate_transaction
from src.kafka_producer import TransactionProducer


# 로그 설정
logger.remove()  # 기본 핸들러 제거
logger.add(
    sys.stdout,
    format="<green>{time:YYYY-MM-DD HH:mm:ss}</green> | <level>{level: <8}</level> | <level>{message}</level>",
    level="INFO"
)


class TransactionGeneratorService:
    """거래 생성 서비스 클래스"""
    
    def __init__(self):
        """환경 변수 로드 및 초기화"""
        load_dotenv()
        
        # 환경 변수 읽기
        self.bootstrap_servers = os.getenv('KAFKA_BOOTSTRAP_SERVERS', 'kafka:9092')
        self.interval_ms = int(os.getenv('TRANSACTION_GENERATION_INTERVAL_MS', '1000'))
        self.topic = os.getenv('TRANSACTION_TOPIC', 'virtual-transactions')
        
        # Kafka Producer 초기화
        try:
            self.producer = TransactionProducer(
                bootstrap_servers=self.bootstrap_servers,
                topic=self.topic
            )
            logger.info(
                f"Transaction Generator 시작: "
                f"브로커={self.bootstrap_servers}, "
                f"토픽={self.topic}, "
                f"주기={self.interval_ms}ms"
            )
        except Exception as e:
            logger.error(f"초기화 실패: {e}")
            raise
        
        # Graceful shutdown을 위한 플래그
        self.running = True
        signal.signal(signal.SIGINT, self._signal_handler)
        signal.signal(signal.SIGTERM, self._signal_handler)
    
    def _signal_handler(self, signum, frame):
        """시그널 핸들러 (Graceful shutdown)"""
        logger.info(f"종료 시그널 수신: {signum}")
        self.running = False
    
    def run(self) -> NoReturn:
        """
        메인 실행 루프
        설정된 주기마다 거래를 생성하고 Kafka로 발행합니다.
        """
        transaction_count = 0
        
        try:
            while self.running:
                try:
                    # 거래 생성
                    transaction = generate_transaction()
                    
                    # Kafka로 발행
                    self.producer.send_transaction(transaction)
                    transaction_count += 1
                    
                    # 주기적으로 통계 출력 (10개마다)
                    if transaction_count % 10 == 0:
                        logger.info(f"총 {transaction_count}개 거래 발행 완료")
                    
                    # 지정된 주기만큼 대기 (밀리초 → 초 변환)
                    time.sleep(self.interval_ms / 1000.0)
                    
                except KeyboardInterrupt:
                    # Ctrl+C 처리
                    logger.info("사용자 중단 요청")
                    break
                except Exception as e:
                    logger.error(f"거래 생성/발행 중 에러 발생: {e}")
                    # 에러 발생 시 5초 대기 후 재시도
                    time.sleep(5)
        
        finally:
            # 종료 시 정리 작업
            logger.info(f"서비스 종료 중... (총 {transaction_count}개 거래 발행)")
            self.producer.close()
            logger.info("서비스 정상 종료")


def main():
    """메인 함수"""
    try:
        service = TransactionGeneratorService()
        service.run()
    except Exception as e:
        logger.error(f"서비스 시작 실패: {e}")
        sys.exit(1)


if __name__ == "__main__":
    main()
