"""
pytest 설정 및 공통 픽스처
confluent_kafka 모듈을 모킹하여 로컬 테스트 가능하게 함
"""
import sys
import pytest
from unittest.mock import MagicMock

# confluent_kafka 모듈 모킹 (Windows 환경에서 빌드 실패 대응)
mock_confluent_kafka = MagicMock()

# KafkaException을 실제 Exception 클래스로 정의
class KafkaException(Exception):
    """Mock KafkaException"""
    pass

mock_confluent_kafka.KafkaException = KafkaException

# Producer를 함수로 정의하여 매번 새로운 MagicMock 반환
def create_mock_producer(config):
    """Mock Producer 생성 함수 - 테스트마다 새로운 mock 인스턴스 반환"""
    mock = MagicMock()
    mock.config = config
    mock.produce = MagicMock()
    mock.poll = MagicMock(return_value=0)
    mock.flush = MagicMock(return_value=0)
    return mock

mock_confluent_kafka.Producer = create_mock_producer
sys.modules['confluent_kafka'] = mock_confluent_kafka


@pytest.fixture
def mock_kafka_producer():
    """각 테스트에서 사용할 수 있는 mock producer 픽스처"""
    mock = MagicMock()
    mock.produce = MagicMock()
    mock.poll = MagicMock(return_value=0)
    mock.flush = MagicMock(return_value=0)
    return mock
