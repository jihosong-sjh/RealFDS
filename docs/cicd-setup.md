# RealFDS CI/CD 파이프라인 설정 가이드

## 📋 개요

RealFDS 프로젝트에 GitHub Actions 기반 CI/CD 파이프라인이 구현되었습니다.

### 구현된 기능

✅ **Phase 1**: 자동 빌드 & 테스트 (CI)
✅ **Phase 2**: Docker 이미지 자동 빌드 & Push
✅ **Phase 3**: Kubernetes 매니페스트
✅ **Phase 4**: Kubernetes 자동 배포 (CD)

---

## 🚀 빠른 시작

### 1단계: GitHub Secrets 설정

#### KUBECONFIG 생성 및 등록

```bash
# 1. Kubernetes config를 base64로 인코딩
cat ~/.kube/config | base64 -w 0

# 2. 출력값을 복사
```

GitHub 저장소에서:
1. **Settings** → **Secrets and variables** → **Actions**
2. **New repository secret** 클릭
3. 이름: `KUBECONFIG`
4. 값: 위에서 복사한 base64 문자열 붙여넣기
5. **Add secret** 클릭

### 2단계: Docker 이미지 레지스트리 설정

프로젝트는 GitHub Container Registry (ghcr.io)를 사용합니다.

#### 이미지 이름 업데이트

다음 파일들에서 `your-username`을 실제 GitHub 사용자명으로 변경:

1. `.github/workflows/build-images.yml`:
   ```yaml
   env:
     IMAGE_PREFIX: your-actual-username/realfds  # 여기를 수정
   ```

2. `k8s/base/*/deployment.yaml` (모든 deployment 파일):
   ```yaml
   image: ghcr.io/your-actual-username/realfds-*:latest
   ```

3. `k8s/overlays/*/kustomization.yaml`:
   ```yaml
   images:
     - name: ghcr.io/your-actual-username/realfds-*
   ```

#### 일괄 변경 스크립트

```bash
# Linux/Mac
find . -type f \( -name "*.yml" -o -name "*.yaml" \) -exec sed -i 's/your-username/YOUR_GITHUB_USERNAME/g' {} +

# Windows (PowerShell)
Get-ChildItem -Recurse -Include *.yml,*.yaml | ForEach-Object {
    (Get-Content $_.FullName) -replace 'your-username', 'YOUR_GITHUB_USERNAME' | Set-Content $_.FullName
}
```

### 3단계: 첫 배포

#### 3-1. CI 테스트

```bash
# Feature 브랜치 생성
git checkout -b feature/ci-test

# 변경사항 커밋
git add .
git commit -m "ci: Setup CI/CD pipeline"

# Push (CI가 자동 실행됨)
git push origin feature/ci-test
```

GitHub Actions 탭에서 CI 워크플로우가 실행되는지 확인하세요.

#### 3-2. Docker 이미지 빌드 테스트

```bash
# develop 브랜치로 머지
git checkout develop
git merge feature/ci-test
git push origin develop
```

다음이 자동으로 실행됩니다:
- ✅ CI: 빌드 & 테스트
- ✅ Docker: 이미지 빌드 & Push (develop 태그)
- ✅ CD: dev 환경 배포

#### 3-3. 프로덕션 배포

```bash
# main 브랜치로 머지
git checkout main
git merge develop
git push origin main
```

자동 실행:
- ✅ CI: 빌드 & 테스트
- ✅ Docker: 이미지 빌드 & Push (latest 태그)
- ✅ CD: prod 환경 배포

---

## 🏗️ 파일 구조

```
RealFDS/
├── .github/
│   ├── workflows/
│   │   ├── ci.yml                    # CI 파이프라인
│   │   ├── build-images.yml          # Docker 빌드
│   │   └── deploy-k8s.yml            # K8s 배포
│   └── README.md                     # CI/CD 문서
├── k8s/
│   ├── base/                         # Base K8s 매니페스트
│   │   ├── namespace.yaml
│   │   ├── configmap.yaml
│   │   ├── kustomization.yaml
│   │   ├── kafka/
│   │   ├── postgresql/
│   │   ├── flink/
│   │   └── [6개 애플리케이션]/
│   ├── overlays/
│   │   ├── dev/                      # 개발 환경
│   │   │   └── kustomization.yaml
│   │   └── prod/                     # 프로덕션 환경
│   │       └── kustomization.yaml
│   └── README.md                     # K8s 배포 문서
└── docs/
    └── cicd-setup.md                 # 이 파일
```

---

## 📊 워크플로우 트리거

| 워크플로우 | 트리거 | 실행 내용 |
|-----------|--------|----------|
| **ci.yml** | PR 생성, Push (모든 브랜치) | 빌드 & 테스트 |
| **build-images.yml** | Push (main, develop), 태그 생성 | Docker 이미지 빌드 & Push |
| **deploy-k8s.yml** | Push (main, develop), 태그 생성 | K8s 배포 |

### 브랜치별 동작

| 브랜치/액션 | CI | Docker | Deploy | 환경 | 이미지 태그 |
|------------|-------|--------|--------|------|------------|
| feature/* push | ✅ | ❌ | ❌ | - | - |
| PR 생성 | ✅ | ✅ (빌드만) | ❌ | - | - |
| develop 머지 | ✅ | ✅ | ✅ | dev | develop |
| main 머지 | ✅ | ✅ | ✅ | prod | latest |
| 태그 (v1.2.3) | ❌ | ✅ | ✅ | prod | 1.2.3 |

---

## 🔧 로컬 테스트

### Kubernetes 매니페스트 검증

```bash
# Dev 환경 매니페스트 미리보기
kubectl kustomize k8s/overlays/dev/

# Prod 환경 매니페스트 미리보기
kubectl kustomize k8s/overlays/prod/

# 구문 검증 (dry-run)
kubectl apply -k k8s/overlays/dev/ --dry-run=client
```

### Docker 이미지 로컬 빌드

```bash
# 개별 서비스 빌드
docker build -t realfds-alert-service:local ./alert-service

# 전체 빌드 (docker-compose 사용)
docker-compose build
```

---

## 🔍 모니터링 & 디버깅

### GitHub Actions 확인

1. GitHub 저장소 → **Actions** 탭
2. 최근 워크플로우 실행 확인
3. 실패 시 로그 확인

### Kubernetes 배포 상태 확인

```bash
# Dev 환경
kubectl get pods -n realfds-dev
kubectl get services -n realfds-dev

# Prod 환경
kubectl get pods -n realfds-prod
kubectl get services -n realfds-prod

# 특정 Pod 로그 확인
kubectl logs -n realfds-dev -l app=alert-service --tail=100 -f

# Pod 이벤트 확인
kubectl describe pod -n realfds-dev <pod-name>
```

### 배포 롤백

```bash
# 자동 롤백 (이전 버전으로)
kubectl rollout undo deployment/alert-service -n realfds-prod

# 특정 리비전으로 롤백
kubectl rollout history deployment/alert-service -n realfds-prod
kubectl rollout undo deployment/alert-service -n realfds-prod --to-revision=2
```

---

## 🎯 릴리스 프로세스

### 정식 릴리스 (Semantic Versioning)

1. **버전 결정**
   - Major: 주요 변경 (Breaking changes)
   - Minor: 기능 추가 (Backwards compatible)
   - Patch: 버그 수정

2. **태그 생성 및 Push**
   ```bash
   # 태그 생성
   git tag -a v1.2.3 -m "Release v1.2.3: Add new features"

   # 태그 Push
   git push origin v1.2.3
   ```

3. **자동 실행**
   - Docker 이미지: `ghcr.io/username/realfds-*:1.2.3`
   - K8s 배포: prod 환경에 자동 배포

4. **GitHub Release 생성** (선택사항)
   ```bash
   # GitHub CLI 사용
   gh release create v1.2.3 --title "v1.2.3" --notes "Release notes..."
   ```

---

## ⚠️ 주의사항

### 1. KUBECONFIG Secret

- **절대 공개하지 마세요!** 클러스터에 대한 전체 액세스 권한이 포함됩니다.
- 정기적으로 갱신하세요 (3-6개월마다).
- 최소 권한 원칙을 따르세요.

### 2. Docker 이미지 권한

- 이미지를 Public으로 설정하거나, 팀원에게 GHCR 접근 권한 부여 필요
- GitHub 저장소 → Settings → Packages

### 3. 리소스 제한

- K8s 매니페스트의 리소스 제한을 클러스터 용량에 맞게 조정
- `k8s/base/*/deployment.yaml`에서 `resources` 섹션 수정

### 4. 프로덕션 배포

- main 브랜치에 직접 Push 금지
- 항상 PR을 통해 코드 리뷰 후 머지
- develop → main 머지 전에 충분한 테스트

---

## 📚 추가 문서

- [CI/CD 파이프라인 상세](.github/README.md)
- [Kubernetes 배포 가이드](k8s/README.md)
- [프로젝트 아키텍처](architecture.md)
- [개발 가이드](development.md)

---

## 🆘 문제 해결

### CI 빌드 실패

**문제**: Gradle 빌드 실패
```bash
# 로컬에서 재현
cd alert-service
./gradlew clean build test
```

**해결**: 의존성 버전 충돌 확인, 테스트 수정

### Docker 빌드 실패

**문제**: 이미지 Push 권한 없음
```bash
# GITHUB_TOKEN 권한 확인
# Settings → Actions → General → Workflow permissions
# "Read and write permissions" 선택
```

### K8s 배포 실패

**문제**: KUBECONFIG 오류
```bash
# Secret 값 재확인
echo "$KUBECONFIG" | base64 -d > test-config
kubectl --kubeconfig=test-config cluster-info
```

**해결**: KUBECONFIG Secret 재생성 및 업데이트

---

## ✅ 체크리스트

배포 전 확인사항:

- [ ] GitHub Secrets 설정 완료 (KUBECONFIG)
- [ ] Docker 이미지 이름 변경 (your-username → 실제 사용자명)
- [ ] K8s 클러스터 접근 가능 확인
- [ ] 로컬에서 매니페스트 검증 완료
- [ ] CI 워크플로우 성공 확인
- [ ] Docker 이미지 빌드 성공 확인
- [ ] dev 환경 배포 테스트 완료
- [ ] Health check 통과 확인

---

**구축 완료 날짜**: 2025-11-12
**버전**: 1.0.0
**유지보수**: CI/CD 파이프라인 업데이트 시 이 문서도 함께 업데이트하세요.
