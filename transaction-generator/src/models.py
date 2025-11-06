"""
Transaction 데이터 모델
거래 엔터티의 구조와 직렬화 로직을 정의합니다.
"""
from datetime import datetime, timezone
from typing import Dict
import uuid


class Transaction:
    """
    금융 거래를 나타내는 데이터 모델 클래스
    
    Attributes:
        schema_version: 스키마 버전 (고정값: "1.0")
        transaction_id: 거래 고유 식별자 (UUID v4)
        user_id: 거래 발생 사용자 ID ("user-1" ~ "user-10")
        amount: 거래 금액 (정수, KRW 기준)
        currency: 통화 코드 (고정값: "KRW")
        country_code: 거래 발생 국가 코드 (ISO 3166-1 alpha-2)
        timestamp: 거래 발생 시각 (ISO 8601 형식)
    """
    
    def __init__(
        self,
        user_id: str,
        amount: int,
        country_code: str,
        transaction_id: str = None,
        timestamp: str = None
    ):
        """
        Transaction 인스턴스 초기화
        
        Args:
            user_id: 사용자 ID
            amount: 거래 금액
            country_code: 국가 코드
            transaction_id: 거래 ID (미제공시 자동 생성)
            timestamp: 거래 시각 (미제공시 현재 시각)
        """
        self.schema_version = "1.0"
        self.transaction_id = transaction_id or str(uuid.uuid4())
        self.user_id = user_id
        self.amount = amount
        self.currency = "KRW"
        self.country_code = country_code
        self.timestamp = timestamp or datetime.now(timezone.utc).isoformat().replace('+00:00', 'Z')
    
    def to_dict(self) -> Dict:
        """
        Transaction 객체를 JSON 직렬화 가능한 딕셔너리로 변환
        
        Returns:
            Dict: 모든 필드를 포함하는 딕셔너리
        """
        return {
            "schemaVersion": self.schema_version,
            "transactionId": self.transaction_id,
            "userId": self.user_id,
            "amount": self.amount,
            "currency": self.currency,
            "countryCode": self.country_code,
            "timestamp": self.timestamp
        }
    
    def __repr__(self) -> str:
        """객체의 문자열 표현"""
        return f"Transaction(id={self.transaction_id}, user={self.user_id}, amount={self.amount})"
