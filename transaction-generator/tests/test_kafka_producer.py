"""
kafka_producer.py 단위 테스트
Given-When-Then 구조를 사용한 Kafka Producer 로직 검증
"""
import pytest
import json
from unittest.mock import Mock, MagicMock, call
from src.kafka_producer import TransactionProducer
from src.models import Transaction


class TestTransactionProducer:
    """TransactionProducer 클래스 테스트"""
    
    @pytest.fixture
    def mock_producer(self, monkeypatch):
        """Mock Kafka Producer 픽스처"""
        mock = MagicMock()
        
        # confluent_kafka.Producer를 모킹
        def mock_producer_init(config):
            return mock
        
        monkeypatch.setattr('confluent_kafka.Producer', mock_producer_init)
        return mock
    
    def test_init_with_valid_config(self, mock_producer):
        """
        Given: 유효한 Kafka 설정이 제공될 때
        When: TransactionProducer를 초기화하면
        Then: Producer가 정상적으로 생성되어야 함
        """
        # Given
        bootstrap_servers = "localhost:9092"
        
        # When
        producer = TransactionProducer(bootstrap_servers)
        
        # Then
        assert producer is not None
        assert producer.topic == "virtual-transactions"
    
    def test_send_transaction_success(self, mock_producer, monkeypatch):
        """
        Given: TransactionProducer가 초기화되고 Transaction이 생성되었을 때
        When: send_transaction()을 호출하면
        Then: Kafka로 메시지가 정상적으로 발행되어야 함
        """
        # Given
        mock_kafka = MagicMock()
        monkeypatch.setattr('confluent_kafka.Producer', lambda config: mock_kafka)
        
        producer = TransactionProducer("localhost:9092")
        transaction = Transaction(
            user_id="user-1",
            amount=100000,
            country_code="KR"
        )
        
        # When
        producer.send_transaction(transaction)
        
        # Then
        assert mock_kafka.produce.called
        # produce 메서드가 올바른 인자로 호출되었는지 확인
        call_args = mock_kafka.produce.call_args
        assert call_args[1]['topic'] == "virtual-transactions"
        
        # value가 JSON으로 인코딩되었는지 확인
        value = call_args[1]['value']
        assert isinstance(value, bytes)
        
        # JSON 파싱 가능한지 확인
        parsed = json.loads(value.decode('utf-8'))
        assert parsed['userId'] == "user-1"
        assert parsed['amount'] == 100000
        assert parsed['countryCode'] == "KR"
    
    def test_send_transaction_serialization(self, mock_producer, monkeypatch):
        """
        Given: Transaction 객체가 주어졌을 때
        When: send_transaction()을 호출하면
        Then: 올바른 JSON 형식으로 직렬화되어야 함
        """
        # Given
        mock_kafka = MagicMock()
        monkeypatch.setattr('confluent_kafka.Producer', lambda config: mock_kafka)
        
        producer = TransactionProducer("localhost:9092")
        transaction = Transaction(
            user_id="user-5",
            amount=250000,
            country_code="US",
            transaction_id="test-tx-123"
        )
        
        # When
        producer.send_transaction(transaction)
        
        # Then
        call_args = mock_kafka.produce.call_args
        value = call_args[1]['value']
        parsed = json.loads(value.decode('utf-8'))
        
        # 필수 필드 검증
        assert 'schemaVersion' in parsed
        assert 'transactionId' in parsed
        assert 'userId' in parsed
        assert 'amount' in parsed
        assert 'currency' in parsed
        assert 'countryCode' in parsed
        assert 'timestamp' in parsed
        
        assert parsed['schemaVersion'] == "1.0"
        assert parsed['transactionId'] == "test-tx-123"
        assert parsed['userId'] == "user-5"
        assert parsed['amount'] == 250000
        assert parsed['currency'] == "KRW"
        assert parsed['countryCode'] == "US"
    
    def test_send_transaction_kafka_error(self, monkeypatch):
        """
        Given: Kafka 연결이 실패할 때
        When: send_transaction()을 호출하면
        Then: 예외가 발생하거나 로그에 에러가 기록되어야 함
        """
        # Given
        from confluent_kafka import KafkaException
        
        mock_kafka = MagicMock()
        mock_kafka.produce.side_effect = KafkaException("Connection failed")
        monkeypatch.setattr('confluent_kafka.Producer', lambda config: mock_kafka)
        
        producer = TransactionProducer("invalid-broker:9092")
        transaction = Transaction(
            user_id="user-1",
            amount=100000,
            country_code="KR"
        )
        
        # When & Then
        with pytest.raises(KafkaException):
            producer.send_transaction(transaction)
    
    def test_poll_called(self, monkeypatch):
        """
        Given: TransactionProducer가 초기화되었을 때
        When: send_transaction()을 호출하면
        Then: poll(0)이 호출되어 delivery callback이 트리거되어야 함
        """
        # Given
        mock_kafka = MagicMock()
        monkeypatch.setattr('confluent_kafka.Producer', lambda config: mock_kafka)
        
        producer = TransactionProducer("localhost:9092")
        transaction = Transaction(
            user_id="user-1",
            amount=100000,
            country_code="KR"
        )
        
        # When
        producer.send_transaction(transaction)
        
        # Then
        assert mock_kafka.poll.called
    
    def test_flush_called(self, monkeypatch):
        """
        Given: TransactionProducer가 초기화되었을 때
        When: flush()를 호출하면
        Then: 모든 대기 중인 메시지가 전송되어야 함
        """
        # Given
        mock_kafka = MagicMock()
        monkeypatch.setattr('confluent_kafka.Producer', lambda config: mock_kafka)
        
        producer = TransactionProducer("localhost:9092")
        
        # When
        producer.flush()
        
        # Then
        assert mock_kafka.flush.called
