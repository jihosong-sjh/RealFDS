# RealFDS Kubernetes 배포

이 디렉토리는 RealFDS 프로젝트의 Kubernetes 배포 매니페스트를 포함합니다.

## 📁 디렉토리 구조

```
k8s/
├── base/                          # Base Kustomize 매니페스트
│   ├── namespace.yaml            # Namespace 정의
│   ├── configmap.yaml            # ConfigMap & Secret
│   ├── kustomization.yaml        # Base Kustomization
│   ├── kafka/                    # Kafka & Zookeeper
│   │   ├── zookeeper.yaml
│   │   └── kafka.yaml
│   ├── postgresql/               # PostgreSQL 데이터베이스
│   │   └── postgresql.yaml
│   ├── flink/                    # Flink 클러스터
│   │   ├── flink-configuration-configmap.yaml
│   │   ├── jobmanager.yaml
│   │   └── taskmanager.yaml
│   ├── transaction-generator/    # 거래 생성기
│   │   └── deployment.yaml
│   ├── fraud-detector/           # 실시간 탐지 엔진
│   │   └── job.yaml
│   ├── alert-service/            # 알림 저장 서비스
│   │   └── deployment.yaml
│   ├── websocket-gateway/        # WebSocket 게이트웨이
│   │   └── deployment.yaml
│   ├── alert-dashboard/          # 알림 히스토리 API
│   │   └── deployment.yaml
│   └── frontend-dashboard/       # React 대시보드
│       └── deployment.yaml
└── overlays/                      # 환경별 오버레이
    ├── dev/                       # 개발 환경
    │   └── kustomization.yaml
    └── prod/                      # 프로덕션 환경
        └── kustomization.yaml
```

## 🚀 빠른 시작

### 사전 요구사항

1. **Kubernetes 클러스터** (v1.24+)
   - Minikube, Kind, GKE, EKS, AKS 등

2. **kubectl** (v1.24+)
   ```bash
   kubectl version --client
   ```

3. **Kustomize** (v4.5+)
   ```bash
   kustomize version
   ```

### 개발 환경 배포

```bash
# Kustomize로 매니페스트 빌드 및 적용
kubectl apply -k k8s/overlays/dev/

# 배포 확인
kubectl get pods -n realfds-dev
kubectl get services -n realfds-dev
```

### 프로덕션 환경 배포

```bash
# 매니페스트 미리보기
kubectl kustomize k8s/overlays/prod/

# 배포
kubectl apply -k k8s/overlays/prod/

# 배포 확인
kubectl get pods -n realfds-prod
kubectl get services -n realfds-prod
```

## ⚙️ 환경별 설정

### 개발 환경 (dev)

- **Namespace**: `realfds-dev`
- **Replicas**: 각 서비스 1개
- **Image Tag**: `develop`
- **로그 레벨**: DEBUG
- **리소스**: 최소 리소스 할당

### 프로덕션 환경 (prod)

- **Namespace**: `realfds-prod`
- **Replicas**: 각 서비스 3개 (고가용성)
- **Image Tag**: `latest`
- **로그 레벨**: INFO
- **리소스**: 최대 리소스 할당

## 🔧 환경별 커스터마이징

### 이미지 태그 변경

특정 버전으로 배포하려면 `kustomization.yaml`의 `images` 섹션 수정:

```yaml
images:
  - name: ghcr.io/your-username/realfds-alert-service
    newTag: v1.2.3  # 원하는 태그로 변경
```

### 리소스 제한 변경

`kustomization.yaml`의 `patches` 섹션에서 리소스 제한 수정:

```yaml
patches:
  - patch: |-
      - op: replace
        path: /spec/template/spec/containers/0/resources/limits/memory
        value: 4Gi  # 메모리 제한 변경
```

### ConfigMap 값 수정

`kustomization.yaml`의 `configMapGenerator` 섹션에서 설정 변경:

```yaml
configMapGenerator:
  - name: realfds-config
    behavior: merge
    literals:
      - TRANSACTION_RATE=5  # 초당 거래 생성 수
      - LOG_LEVEL=DEBUG
```

## 📊 배포 확인

### Pod 상태 확인

```bash
# 모든 Pod 확인
kubectl get pods -n realfds-dev

# 특정 서비스 로그 확인
kubectl logs -n realfds-dev -l app=alert-service --tail=100 -f

# Pod 상세 정보
kubectl describe pod -n realfds-dev <pod-name>
```

### 서비스 접근

```bash
# 서비스 목록 확인
kubectl get services -n realfds-dev

# 프론트엔드 접근 (LoadBalancer)
kubectl get service frontend-dashboard -n realfds-dev

# Port Forward로 로컬 접근
kubectl port-forward -n realfds-dev service/frontend-dashboard 8083:8083
```

### Flink UI 접근

```bash
# Flink JobManager UI
kubectl port-forward -n realfds-dev service/flink-jobmanager 8081:8081
# 브라우저: http://localhost:8081
```

## 🗄️ 데이터 지속성

### PersistentVolume 확인

```bash
# PVC 확인
kubectl get pvc -n realfds-dev

# PV 상태 확인
kubectl get pv
```

### 데이터 백업

```bash
# PostgreSQL 데이터 백업
kubectl exec -n realfds-dev postgresql-0 -- pg_dump -U postgres alertdb > backup.sql

# Flink 체크포인트 백업
kubectl cp realfds-dev/flink-jobmanager-xxx:/flink-checkpoints ./backups/
```

## 🔄 업데이트 및 롤백

### Rolling Update

```bash
# 새 이미지로 업데이트
kubectl set image deployment/alert-service -n realfds-dev alert-service=ghcr.io/your-username/realfds-alert-service:v1.2.3

# 업데이트 상태 확인
kubectl rollout status deployment/alert-service -n realfds-dev
```

### 롤백

```bash
# 이전 버전으로 롤백
kubectl rollout undo deployment/alert-service -n realfds-dev

# 특정 리비전으로 롤백
kubectl rollout undo deployment/alert-service -n realfds-dev --to-revision=2
```

## 🧹 리소스 정리

### 개발 환경 삭제

```bash
kubectl delete -k k8s/overlays/dev/
```

### 프로덕션 환경 삭제

```bash
kubectl delete -k k8s/overlays/prod/
```

### 네임스페이스만 삭제

```bash
kubectl delete namespace realfds-dev
kubectl delete namespace realfds-prod
```

## 🐛 트러블슈팅

### Pod가 시작되지 않는 경우

```bash
# Pod 이벤트 확인
kubectl describe pod -n realfds-dev <pod-name>

# 로그 확인
kubectl logs -n realfds-dev <pod-name>

# 이전 컨테이너 로그 확인 (CrashLoopBackOff)
kubectl logs -n realfds-dev <pod-name> --previous
```

### 서비스 연결 문제

```bash
# 서비스 엔드포인트 확인
kubectl get endpoints -n realfds-dev

# DNS 테스트
kubectl run -n realfds-dev test-pod --image=busybox --rm -it -- nslookup kafka
```

### 리소스 부족

```bash
# 노드 리소스 확인
kubectl top nodes

# Pod 리소스 사용량 확인
kubectl top pods -n realfds-dev

# 리소스 제한 낮추기 (overlays에서 조정)
```

## 📝 참고사항

### Kafka 토픽 자동 생성

Kafka는 `KAFKA_AUTO_CREATE_TOPICS_ENABLE=true`로 설정되어 있어 필요한 토픽이 자동 생성됩니다:
- `virtual-transactions`
- `transaction-alerts`

### PostgreSQL 초기화

PostgreSQL은 최초 실행 시 자동으로 `alertdb` 데이터베이스를 생성합니다. 테이블은 `alert-service`가 자동으로 생성합니다 (Spring Data R2DBC).

### Flink Job 제출

`fraud-detector` Job은 Kubernetes Job으로 실행되며, Flink JobManager에 자동으로 제출됩니다.

## 🔗 관련 문서

- [GitHub Actions CI/CD](../.github/README.md)
- [프로젝트 아키텍처](../docs/architecture.md)
- [개발 가이드](../docs/development.md)
