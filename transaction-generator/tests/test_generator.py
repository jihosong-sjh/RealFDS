"""
generator.py 단위 테스트
Given-When-Then 구조를 사용한 거래 생성 로직 검증
"""
import pytest
from src.generator import generate_transaction
from src.models import Transaction


class TestGenerateTransaction:
    """generate_transaction() 함수 테스트"""
    
    def test_generate_transaction_fields(self):
        """
        Given: 거래 생성 함수가 호출될 때
        When: Transaction 객체가 반환되면
        Then: 모든 필수 필드가 존재해야 함
        """
        # When
        transaction = generate_transaction()
        
        # Then
        assert transaction is not None
        assert isinstance(transaction, Transaction)
        assert hasattr(transaction, 'schema_version')
        assert hasattr(transaction, 'transaction_id')
        assert hasattr(transaction, 'user_id')
        assert hasattr(transaction, 'amount')
        assert hasattr(transaction, 'currency')
        assert hasattr(transaction, 'country_code')
        assert hasattr(transaction, 'timestamp')
    
    def test_amount_in_range(self):
        """
        Given: 100개의 거래가 생성될 때
        When: 각 거래의 금액을 확인하면
        Then: 모든 금액이 1,000~1,500,000 범위 내에 있어야 함
        """
        # Given & When
        transactions = [generate_transaction() for _ in range(100)]
        
        # Then
        for tx in transactions:
            assert 1000 <= tx.amount <= 1500000, \
                f"거래 금액이 범위를 벗어남: {tx.amount}"
    
    def test_country_code_valid(self):
        """
        Given: 100개의 거래가 생성될 때
        When: 각 거래의 국가 코드를 확인하면
        Then: 모든 국가 코드가 KR, US, JP, CN 중 하나여야 함
        """
        # Given
        valid_countries = {"KR", "US", "JP", "CN"}
        
        # When
        transactions = [generate_transaction() for _ in range(100)]
        
        # Then
        for tx in transactions:
            assert tx.country_code in valid_countries, \
                f"유효하지 않은 국가 코드: {tx.country_code}"
    
    def test_user_id_valid(self):
        """
        Given: 100개의 거래가 생성될 때
        When: 각 거래의 사용자 ID를 확인하면
        Then: 모든 사용자 ID가 user-1 ~ user-10 범위여야 함
        """
        # Given
        valid_user_ids = {f"user-{i}" for i in range(1, 11)}
        
        # When
        transactions = [generate_transaction() for _ in range(100)]
        
        # Then
        for tx in transactions:
            assert tx.user_id in valid_user_ids, \
                f"유효하지 않은 사용자 ID: {tx.user_id}"
    
    def test_currency_is_krw(self):
        """
        Given: 거래가 생성될 때
        When: 통화 코드를 확인하면
        Then: 항상 "KRW"여야 함
        """
        # When
        transaction = generate_transaction()
        
        # Then
        assert transaction.currency == "KRW"
    
    def test_schema_version(self):
        """
        Given: 거래가 생성될 때
        When: 스키마 버전을 확인하면
        Then: "1.0"이어야 함
        """
        # When
        transaction = generate_transaction()
        
        # Then
        assert transaction.schema_version == "1.0"
    
    def test_country_code_distribution(self):
        """
        Given: 1000개의 거래가 생성될 때
        When: 국가 코드 분포를 확인하면
        Then: KR이 가장 많아야 함 (약 80%)
        """
        # Given & When
        transactions = [generate_transaction() for _ in range(1000)]
        country_counts = {}
        
        for tx in transactions:
            country_counts[tx.country_code] = country_counts.get(tx.country_code, 0) + 1
        
        # Then - KR이 가장 많아야 함
        assert country_counts.get("KR", 0) > 700, \
            f"KR 비율이 너무 낮음: {country_counts}"
