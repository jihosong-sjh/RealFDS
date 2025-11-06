"""
pytest 설정 및 공통 픽스처
confluent_kafka 모듈을 모킹하여 로컬 테스트 가능하게 함
"""
import sys
from unittest.mock import MagicMock

# Kafka Producer Mock 클래스 정의
class MockProducer:
    def __init__(self, config):
        self.config = config

    def produce(self, topic, value, key=None, callback=None):
        """Mock produce 메서드"""
        pass

    def poll(self, timeout):
        """Mock poll 메서드"""
        return 0

    def flush(self, timeout=None):
        """Mock flush 메서드"""
        return 0

# confluent_kafka 모듈 모킹 (Windows 환경에서 빌드 실패 대응)
mock_confluent_kafka = MagicMock()
mock_confluent_kafka.Producer = MockProducer
mock_confluent_kafka.KafkaException = Exception
sys.modules['confluent_kafka'] = mock_confluent_kafka
