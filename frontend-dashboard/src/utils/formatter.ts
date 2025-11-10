/**
 * 시간 포맷팅 유틸리티
 * ISO 8601 형식의 타임스탬프를 "YYYY-MM-DD HH:MM:SS" 형식으로 변환
 *
 * @param timestamp ISO 8601 형식의 타임스탬프 (예: "2025-11-06T10:30:45.123Z")
 * @returns 포맷된 시간 문자열 (예: "2025-11-06 10:30:45")
 */
export function formatTimestamp(timestamp: string): string {
  try {
    const date = new Date(timestamp);

    // 날짜가 유효한지 확인
    if (isNaN(date.getTime())) {
      return timestamp; // 유효하지 않은 경우 원본 반환
    }

    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    const seconds = String(date.getSeconds()).padStart(2, '0');

    return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
  } catch (error) {
    console.error('타임스탬프 포맷팅 오류:', error);
    return timestamp; // 에러 발생 시 원본 반환
  }
}

/**
 * 금액 포맷팅 유틸리티
 * 숫자를 천 단위 쉼표가 있는 원화 형식으로 변환
 *
 * @param amount 금액 숫자 (예: 1250000)
 * @returns 포맷된 금액 문자열 (예: "1,250,000원")
 */
export function formatAmount(amount: number): string {
  try {
    // 숫자가 유효한지 확인
    if (typeof amount !== 'number' || isNaN(amount)) {
      return '0원';
    }

    // toLocaleString을 사용하여 천 단위 쉼표 추가
    const formattedAmount = amount.toLocaleString('ko-KR');

    return `${formattedAmount}원`;
  } catch (error) {
    console.error('금액 포맷팅 오류:', error);
    return `${amount}원`; // 에러 발생 시 쉼표 없이 반환
  }
}
