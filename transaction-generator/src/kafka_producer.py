"""
Kafka Producer 모듈
Transaction 객체를 Kafka 토픽으로 발행하는 Producer 클래스를 제공합니다.
"""
import json
from typing import Optional
from confluent_kafka import Producer, KafkaException
from loguru import logger
from src.models import Transaction


class TransactionProducer:
    """
    Transaction 객체를 Kafka로 발행하는 Producer 클래스
    
    Attributes:
        producer: confluent_kafka Producer 인스턴스
        topic: 거래 데이터를 발행할 Kafka 토픽명
    """
    
    def __init__(self, bootstrap_servers: str, topic: str = "virtual-transactions"):
        """
        TransactionProducer 초기화
        
        Args:
            bootstrap_servers: Kafka 브로커 주소 (예: "localhost:9092")
            topic: 발행할 토픽명 (기본값: "virtual-transactions")
        """
        # Kafka Producer 설정
        config = {
            'bootstrap.servers': bootstrap_servers,
            'client.id': 'transaction-generator',
            # 메시지 전송 신뢰성 설정
            'acks': 'all',  # 모든 레플리카가 메시지를 수신할 때까지 대기
            'retries': 3,  # 전송 실패 시 재시도 횟수
            # 성능 최적화
            'linger.ms': 10,  # 메시지 배치를 위한 대기 시간 (밀리초)
            'batch.size': 16384,  # 배치 크기 (바이트)
        }
        
        try:
            self.producer = Producer(config)
            self.topic = topic
            logger.info(f"Kafka Producer 초기화 완료: {bootstrap_servers}, 토픽: {topic}")
        except KafkaException as e:
            logger.error(f"Kafka Producer 초기화 실패: {e}")
            raise
    
    def _delivery_report(self, err: Optional[Exception], msg):
        """
        메시지 전송 결과 콜백
        
        Args:
            err: 에러 발생 시 예외 객체, 성공 시 None
            msg: 전송된 메시지 객체
        """
        if err is not None:
            logger.error(f"메시지 전송 실패: {err}")
        else:
            logger.debug(
                f"메시지 전송 성공: 토픽={msg.topic()}, "
                f"파티션={msg.partition()}, 오프셋={msg.offset()}"
            )
    
    def send_transaction(self, transaction: Transaction) -> None:
        """
        Transaction 객체를 JSON으로 직렬화하여 Kafka로 발행
        
        Args:
            transaction: 발행할 Transaction 객체
        
        Raises:
            KafkaException: Kafka 연결 실패 또는 메시지 발행 실패 시
        """
        try:
            # Transaction 객체를 딕셔너리로 변환
            transaction_dict = transaction.to_dict()
            
            # JSON 문자열로 직렬화
            message_value = json.dumps(transaction_dict).encode('utf-8')
            
            # Kafka로 메시지 발행 (비동기)
            self.producer.produce(
                topic=self.topic,
                value=message_value,
                key=transaction.user_id.encode('utf-8'),  # userId를 key로 사용
                callback=self._delivery_report
            )
            
            # 이전 produce() 호출의 delivery callback 트리거
            self.producer.poll(0)
            
            logger.info(
                f"거래 발행: ID={transaction.transaction_id}, "
                f"사용자={transaction.user_id}, 금액={transaction.amount}원"
            )
            
        except KafkaException as e:
            logger.error(f"Kafka 메시지 발행 실패: {e}")
            raise
        except Exception as e:
            logger.error(f"예상치 못한 에러 발생: {e}")
            raise
    
    def flush(self, timeout: float = 10.0) -> None:
        """
        대기 중인 모든 메시지를 Kafka로 전송
        
        Args:
            timeout: 최대 대기 시간 (초)
        """
        remaining = self.producer.flush(timeout)
        if remaining > 0:
            logger.warning(f"{remaining}개의 메시지가 전송되지 않았습니다.")
        else:
            logger.info("모든 메시지 전송 완료")
    
    def close(self) -> None:
        """Producer 리소스 정리"""
        self.flush()
        logger.info("Kafka Producer 종료")
