# RealFDS CI/CD 파이프라인

이 디렉토리는 RealFDS 프로젝트의 GitHub Actions 기반 CI/CD 파이프라인을 포함합니다.

## 📋 워크플로우 개요

### 1. CI - Build and Test (`ci.yml`)

**트리거**: PR 생성, Push (main, develop, feature/* 브랜치)

**목적**: 모든 서비스의 빌드와 테스트를 자동화하여 코드 품질을 보장합니다.

**상태**: ✅ 구현 완료 (Phase 1)

---

### 2. Build and Push Docker Images (`build-images.yml`)

**트리거**:
- Push to main/develop 브랜치
- Git 태그 생성 (`v*.*.*`)
- PR 생성 (빌드만, Push 안함)

**목적**: 모든 마이크로서비스의 Docker 이미지를 자동으로 빌드하고 GitHub Container Registry에 푸시합니다.

**상태**: ✅ 구현 완료 (Phase 2)

---

### 3. Deploy to Kubernetes (`deploy-k8s.yml`)

**트리거**:
- Push to main 브랜치 → prod 환경 배포
- Push to develop 브랜치 → dev 환경 배포
- Git 태그 생성 (`v*.*.*`) → prod 환경에 태그 버전 배포
- 수동 실행 (workflow_dispatch) → 환경 선택 가능

**목적**: Kubernetes 클러스터에 자동으로 배포하고 health check를 수행합니다.

**상태**: ✅ 구현 완료 (Phase 4)

#### 배포 프로세스

1. **환경 결정**
   - main 브랜치 또는 태그 → prod
   - develop 브랜치 → dev
   - 수동 실행 → 사용자 선택

2. **Kubernetes 설정**
   - kubectl 설치 및 구성
   - KUBECONFIG secret으로 클러스터 접근
   - 클러스터 연결 확인

3. **이미지 태그 업데이트**
   - 태그 생성 시: 버전 번호 사용 (v1.2.3 → 1.2.3)
   - main 브랜치: `latest`
   - develop 브랜치: `develop`

4. **배포 실행**
   - Kustomize로 매니페스트 적용
   - Rolling update로 무중단 배포
   - 최대 5분 대기 (timeout)

5. **배포 검증**
   - Pod 상태 확인
   - Service 엔드포인트 확인
   - Health check 실행 (Spring Actuator)

6. **결과 요약**
   - 배포 상태 출력
   - 실패 시 알림

#### 배포 환경별 차이

| 항목 | dev | prod |
|------|-----|------|
| Namespace | realfds-dev | realfds-prod |
| Replicas | 1 | 3 |
| Image Tag | develop | latest 또는 버전 |
| 로그 레벨 | DEBUG | INFO |
| 리소스 제한 | 최소 | 최대 |

---

#### 빌드되는 이미지 (6개 서비스)

1. **transaction-generator** - 가상 거래 생성기 (Python)
2. **fraud-detector** - Flink 기반 실시간 탐지 엔진 (Scala)
3. **alert-service** - 알림 저장 서비스 (Spring Boot)
4. **websocket-gateway** - WebSocket 게이트웨이 (Spring Boot)
5. **alert-dashboard-backend** - 알림 히스토리 API (Spring Boot)
6. **frontend-dashboard** - 실시간 대시보드 UI (React + Nginx)

#### 이미지 태그 전략

| 트리거 | 생성되는 태그 | 예시 |
|--------|--------------|------|
| main 브랜치 Push | `latest`, `main`, `sha-abc123` | `ghcr.io/your-username/realfds-alert-service:latest` |
| develop 브랜치 Push | `develop`, `sha-abc123` | `ghcr.io/your-username/realfds-alert-service:develop` |
| Git 태그 (`v1.2.3`) | `1.2.3`, `1.2`, `latest`, `sha-abc123` | `ghcr.io/your-username/realfds-alert-service:1.2.3` |
| PR 생성 | `pr-42` (빌드만, Push 안함) | - |

#### 최적화

- **빌드 캐시**: GitHub Actions 캐시를 사용하여 빌드 시간 단축
- **멀티플랫폼**: linux/amd64 (필요시 arm64 추가 가능)
- **병렬 빌드**: 6개 서비스 동시 빌드로 총 시간 단축

---

### 1. CI - Build and Test (`ci.yml`) 상세

#### 실행되는 작업

1. **Java/Gradle 서비스 빌드 & 테스트** (병렬 실행)
   - `alert-service` - 알림 저장 서비스
   - `websocket-gateway` - WebSocket 게이트웨이
   - `fraud-detector` - Flink 기반 실시간 탐지 엔진
   - `alert-dashboard-backend` - 알림 히스토리 API

   각 서비스별로:
   - JDK 17 설정
   - Gradle 캐싱
   - 빌드 실행 (`gradle build -x test`)
   - 단위 테스트 실행 (`gradle test`)
   - 테스트 리포트 업로드

2. **Python 서비스 빌드 & 테스트**
   - `transaction-generator` - 가상 거래 생성기

   실행 내용:
   - Python 3.11 설정
   - 의존성 설치 (`pip install -r requirements.txt`)
   - Flake8 린팅
   - 단위 테스트 (pytest, 존재하는 경우)

3. **React 프론트엔드 빌드 & 테스트**
   - `frontend-dashboard` - 실시간 대시보드 UI

   실행 내용:
   - Node.js 20 설정
   - 의존성 설치 (`npm ci`)
   - 코드 린팅 (설정된 경우)
   - 단위 테스트 (`npm test`)
   - 프로덕션 빌드 (`npm run build`)
   - 빌드 아티팩트 업로드

4. **CI Success Check**
   - 모든 빌드 작업이 성공했는지 확인
   - PR 머지 조건으로 사용 가능

## 📦 Docker 이미지 사용하기

### 이미지 Pull

```bash
# 특정 서비스 이미지 가져오기
docker pull ghcr.io/your-username/realfds-alert-service:latest
docker pull ghcr.io/your-username/realfds-websocket-gateway:latest
docker pull ghcr.io/your-username/realfds-frontend-dashboard:latest

# 모든 이미지 가져오기
docker pull ghcr.io/your-username/realfds-transaction-generator:latest
docker pull ghcr.io/your-username/realfds-fraud-detector:latest
docker pull ghcr.io/your-username/realfds-alert-service:latest
docker pull ghcr.io/your-username/realfds-websocket-gateway:latest
docker pull ghcr.io/your-username/realfds-alert-dashboard-backend:latest
docker pull ghcr.io/your-username/realfds-frontend-dashboard:latest
```

### 특정 버전 사용

```bash
# 프로덕션 (태그 버전)
docker pull ghcr.io/your-username/realfds-alert-service:1.2.3

# 개발 환경
docker pull ghcr.io/your-username/realfds-alert-service:develop

# 특정 커밋
docker pull ghcr.io/your-username/realfds-alert-service:sha-abc1234
```

### docker-compose.yml 업데이트

빌드된 이미지를 사용하도록 docker-compose.yml을 수정:

```yaml
services:
  alert-service:
    image: ghcr.io/your-username/realfds-alert-service:latest
    # build 섹션 제거 또는 주석 처리
```

---

## 🔧 로컬 테스트

CI 워크플로우를 로컬에서 시뮬레이션하려면:

### Java/Gradle 서비스
```bash
cd alert-service
./gradlew clean build test

cd ../websocket-gateway
./gradlew clean build test

cd ../fraud-detector
./gradlew clean build test

cd ../alert-dashboard/backend
./gradlew clean build test
```

### Python 서비스
```bash
cd transaction-generator
pip install -r requirements.txt
flake8 .
pytest  # 테스트가 있는 경우
```

### React 프론트엔드
```bash
cd frontend-dashboard
npm ci
npm run lint  # 설정된 경우
npm test
npm run build
```

## 📊 워크플로우 상태 확인

워크플로우 실행 상태는 다음에서 확인할 수 있습니다:
- GitHub 저장소 → Actions 탭
- PR의 Checks 섹션

## 🚨 트러블슈팅

### 빌드 실패 시

1. **Gradle 빌드 실패**
   - 로컬에서 `./gradlew clean build` 실행하여 재현
   - 의존성 버전 충돌 확인
   - `build/reports/tests/test/` 에서 상세 로그 확인

2. **테스트 실패**
   - Actions 탭에서 "Upload test reports" 아티팩트 다운로드
   - 실패한 테스트 케이스 확인 후 수정

3. **캐시 문제**
   - Actions 탭 → Settings → Caches에서 캐시 삭제
   - 워크플로우 재실행

## 🔐 필수 GitHub Secrets 설정

CD 파이프라인을 사용하려면 다음 Secrets를 설정해야 합니다:

### Repository Settings → Secrets and variables → Actions

1. **KUBECONFIG** (필수)
   ```bash
   # Kubernetes config를 base64로 인코딩
   cat ~/.kube/config | base64 -w 0
   ```
   - 위 명령의 출력값을 GitHub Secret으로 저장
   - CD 워크플로우에서 kubectl 접근에 사용

2. **GITHUB_TOKEN** (자동 생성됨)
   - Docker 이미지 빌드 시 GHCR 접근에 사용
   - 별도 설정 불필요

### Secrets 설정 방법

1. GitHub 저장소 → Settings 메뉴
2. Secrets and variables → Actions 클릭
3. "New repository secret" 클릭
4. Secret 이름과 값 입력 후 저장

---

## 🚀 전체 워크플로우 흐름

```
코드 작성
  ↓
feature 브랜치 push
  ↓
[CI] 빌드 & 테스트 (ci.yml)
  ↓
PR 생성
  ↓
[CI] 재검증
  ↓
develop 브랜치 머지
  ↓
[CI] 빌드 & 테스트
[Docker] 이미지 빌드 & Push (develop 태그)
[CD] dev 환경 자동 배포
  ↓
테스트 및 검증
  ↓
main 브랜치 머지
  ↓
[CI] 빌드 & 테스트
[Docker] 이미지 빌드 & Push (latest 태그)
[CD] prod 환경 자동 배포
  ↓
Git 태그 생성 (v1.2.3)
  ↓
[Docker] 이미지 빌드 & Push (버전 태그)
[CD] prod 환경에 버전 배포
```

---

## 🔄 다음 단계 (완료)

- [x] **Phase 1**: CI - 빌드 & 테스트 자동화
- [x] **Phase 2**: Docker 이미지 빌드 & Push
- [x] **Phase 3**: Kubernetes 매니페스트 작성
- [x] **Phase 4**: 자동 배포 (CD) 워크플로우
- [ ] **Phase 5**: E2E 테스트 통합 (향후 계획)

## 📝 참고사항

- **빌드 시간**: 평균 5-8분 (병렬 실행)
- **캐싱**: Gradle, npm, pip 캐싱 활성화로 빌드 시간 단축
- **병렬 처리**: 각 서비스는 독립적으로 병렬 실행되어 효율적입니다
- **실패 격리**: `fail-fast: false` 설정으로 한 서비스 실패 시에도 다른 서비스 빌드 계속 진행
