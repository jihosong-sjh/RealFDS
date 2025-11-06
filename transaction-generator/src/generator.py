"""
거래 생성 로직
무작위 금융 거래 데이터를 생성하는 함수들을 제공합니다.
"""
import random
from src.models import Transaction


def generate_transaction() -> Transaction:
    """
    무작위 Transaction 객체를 생성
    
    생성 규칙:
    - 금액: 정규 분포 (평균 300,000원, 표준편차 200,000원)
    - 범위: 1,000원 ~ 1,500,000원
    - 국가: 80% KR, 10% US, 5% JP, 5% CN
    - 사용자: user-1 ~ user-10 균등 분포
    - 통화: KRW (고정)
    
    Returns:
        Transaction: 생성된 거래 객체
    """
    # 사용자 ID 생성 (user-1 ~ user-10 중 무작위 선택)
    user_id = f"user-{random.randint(1, 10)}"
    
    # 거래 금액 생성 (정규 분포)
    # 평균: 300,000원, 표준편차: 200,000원
    mean_amount = 300_000
    std_dev = 200_000
    amount = int(random.gauss(mean_amount, std_dev))
    
    # 금액 범위 제한 (1,000 ~ 1,500,000)
    amount = max(1000, min(1_500_000, amount))
    
    # 국가 코드 생성 (가중 무작위 선택)
    # 80% KR, 10% US, 5% JP, 5% CN
    countries = ["KR", "US", "JP", "CN"]
    weights = [0.80, 0.10, 0.05, 0.05]
    country_code = random.choices(countries, weights=weights)[0]
    
    # Transaction 객체 생성 및 반환
    return Transaction(
        user_id=user_id,
        amount=amount,
        country_code=country_code
    )
