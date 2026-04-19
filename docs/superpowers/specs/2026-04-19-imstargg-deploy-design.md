# imstargg-deploy 설계

**작성일:** 2026-04-19
**범위:** `imstargg-backend` 의 `core-api`, `core-worker`, `core-batch` 를 k3s(단일 노드, prod) 에 배포하기 위한 리소스·스크립트·문서 정의.

## 목표

- 로컬(맥북 arm64) 에서 Docker 이미지를 빌드하고, 레지스트리 없이 tar 전송으로 k3s 서버에 배포.
- 추후 amd64 전환 가능성을 고려한 multi-arch 대응 여지 확보.
- CI/CD 없이 수동 실행이 원활하도록 스크립트와 README 제공.
- 버전 추적이 가능한 이미지 태깅 체계 (semver + date + git SHA).

## 결정 사항 요약

| 항목 | 결정 |
|---|---|
| 배포 파일 위치 | `imstargg-v2/imstargg-deploy/` (모노레포 내) |
| 빌드 방식 | Dockerfile + `docker buildx` (모듈별 Multi-stage) |
| 이미지 레지스트리 | 사용하지 않음. `docker save` + `scp` + `k3s ctr images import` |
| 버전 태깅 | `v{semver}-{YYYY.MM.DD}-{git_short_sha}` + 이동 태그 `v{semver}` / `latest` (k8s 매니페스트는 full tag 사용) |
| Secret 주입 | `kubectl create secret generic --from-env-file` |
| 네임스페이스 | `imstargg-prod` (현재는 prod만) |
| core-api/worker | k8s `Deployment` (replicas=1, Actuator 헬스체크) |
| core-batch | k8s `CronJob` 3종 (rotation, rank-stats, result-stats) |
| 타임존 | `Asia/Seoul` (컨테이너 `TZ=Asia/Seoul`, CronJob `spec.timeZone` 명시) |

## 디렉토리 구조

```
imstargg-v2/
├── imstargg-backend/
│   └── core/
│       ├── core-api/Dockerfile
│       ├── core-worker/Dockerfile
│       └── core-batch/Dockerfile
└── imstargg-deploy/
    ├── README.md                       # 빌드·전송·배포·롤백 가이드
    ├── VERSIONS.md                     # 모듈별 현재 버전 기록 (수동 갱신)
    ├── scripts/
    │   ├── build-image.sh              # 모듈 + 버전 입력 → buildx 빌드 + 태깅
    │   ├── save-image.sh               # docker save | gzip
    │   └── load-image.sh               # 서버에서 k3s ctr images import
    └── k8s/
        └── prod/
            ├── namespace.yaml
            ├── core-api/
            │   ├── deployment.yaml
            │   ├── service.yaml
            │   └── secret.env.example
            ├── core-worker/
            │   ├── deployment.yaml
            │   └── secret.env.example
            └── core-batch/
                ├── cronjob-battle-event-rotation-sync.yaml
                ├── cronjob-brawler-battle-rank-stats.yaml
                ├── cronjob-brawler-battle-result-stats.yaml
                └── secret.env.example
```

- `secret.env.example` 만 git 에 커밋. 실제 `secret.env` 파일은 `.gitignore` 처리.
- `Dockerfile` 은 각 모듈 디렉토리에 두되, 빌드 컨텍스트는 `imstargg-backend/` 로 지정 (Gradle 전체 프로젝트 필요).

## Dockerfile 패턴 (3개 모듈 공통)

```dockerfile
# ---- Build stage ----
FROM --platform=$BUILDPLATFORM eclipse-temurin:21-jdk AS builder
WORKDIR /build
COPY . .
ARG MODULE_GRADLE_PATH   # 예: :core:core-worker
RUN ./gradlew ${MODULE_GRADLE_PATH}:bootJar -x test --no-daemon

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre
ENV TZ=Asia/Seoul
WORKDIR /app
ARG MODULE_JAR_DIR       # 예: core/core-worker
COPY --from=builder /build/${MODULE_JAR_DIR}/build/libs/*.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

- `TZ=Asia/Seoul` 로 CronJob 의 `date -d 'yesterday'` 가 KST 기준으로 동작.
- `--platform=$BUILDPLATFORM` 으로 host 아키텍처에서 빌드 수행 (크로스 빌드 시 속도 확보).
- 현재는 `--platform linux/arm64` 로 빌드. amd64 전환 시 `linux/arm64,linux/amd64` 추가하거나 교체.

## 이미지 빌드 스크립트

`scripts/build-image.sh`:

```bash
#!/usr/bin/env bash
set -euo pipefail

MODULE="${1:?Usage: build-image.sh <core-api|core-worker|core-batch> <version>}"
VERSION="${2:?Usage: build-image.sh <module> <version e.g. v1.0.0>}"

SHA=$(git rev-parse --short HEAD)
DATE=$(date +%Y.%m.%d)
FULL_TAG="${VERSION}-${DATE}-${SHA}"
IMAGE="imstargg/${MODULE}"

cd "$(dirname "$0")/../../imstargg-backend"

docker buildx build \
  --platform linux/arm64 \
  -t "${IMAGE}:${FULL_TAG}" \
  -t "${IMAGE}:${VERSION}" \
  -t "${IMAGE}:latest" \
  --build-arg "MODULE_GRADLE_PATH=:core:${MODULE}" \
  --build-arg "MODULE_JAR_DIR=core/${MODULE}" \
  --load \
  -f "core/${MODULE}/Dockerfile" \
  .

echo "Built: ${IMAGE}:${FULL_TAG}"
```

## 이미지 전송 스크립트

`scripts/save-image.sh`:

```bash
#!/usr/bin/env bash
set -euo pipefail
IMAGE_TAG="${1:?Usage: save-image.sh <image:full-tag>}"
OUT="$(echo "${IMAGE_TAG}" | tr '/:' '_').tar.gz"
docker save "${IMAGE_TAG}" | gzip > "${OUT}"
echo "Saved: ${OUT}"
```

`scripts/load-image.sh` (서버에서 실행):

```bash
#!/usr/bin/env bash
set -euo pipefail
TAR_GZ="${1:?Usage: load-image.sh <file.tar.gz>}"
gunzip -c "${TAR_GZ}" | sudo k3s ctr images import -
```

## 버전 관리 정책

- 태그 형식: `v{MAJOR}.{MINOR}.{PATCH}-{YYYY.MM.DD}-{GIT_SHA}` (불변, k8s 매니페스트에 기록).
- 이동 태그: `v{MAJOR}.{MINOR}.{PATCH}`, `latest` — 편의용. **k8s Deployment/CronJob 의 `image:` 필드에는 사용 금지**.
- `imstargg-deploy/VERSIONS.md` 에 모듈별 현재 prod 버전을 수동 기록.
- 릴리스 시 `git tag {module}-v{semver}` 부여.
- `imagePullPolicy: IfNotPresent` (레지스트리 미사용 → 이미 import 된 이미지 사용).

## Kubernetes 매니페스트

### namespace.yaml

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: imstargg-prod
```

### core-api

`deployment.yaml` (핵심):

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: core-api
  namespace: imstargg-prod
spec:
  replicas: 1
  selector:
    matchLabels: { app: core-api }
  template:
    metadata:
      labels: { app: core-api }
    spec:
      containers:
        - name: core-api
          image: imstargg/core-api:v0.1.0-2026.04.19-xxxxxxx
          imagePullPolicy: IfNotPresent
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: prod
          envFrom:
            - secretRef: { name: core-api-secret }
          ports:
            - containerPort: 8080
          resources:
            requests: { cpu: 200m, memory: 512Mi }
            limits:   { cpu: 1000m, memory: 2Gi }
          livenessProbe:
            httpGet: { path: /actuator/health/liveness, port: 8080 }
            initialDelaySeconds: 60
            periodSeconds: 30
          readinessProbe:
            httpGet: { path: /actuator/health/readiness, port: 8080 }
            initialDelaySeconds: 30
            periodSeconds: 10
```

`service.yaml`:

```yaml
apiVersion: v1
kind: Service
metadata:
  name: core-api
  namespace: imstargg-prod
spec:
  type: ClusterIP
  selector: { app: core-api }
  ports:
    - port: 80
      targetPort: 8080
```

- Ingress 연결은 별도 과제(범위 외).

`secret.env.example`:

```env
AWS_ACCESS_KEY=
AWS_SECRET_KEY=
DATABASE_URL=
DATABASE_USERNAME=
DATABASE_PASSWORD=
```

### core-worker

`deployment.yaml` 은 core-api 와 동일 패턴. 차이:

- Service 없음 (외부 노출 불필요).
- Secret 키: `AWS_ACCESS_KEY`, `AWS_SECRET_KEY`, `BRAWLSTARS_API_KEYS`, `DATABASE_URL`, `DATABASE_USERNAME`.
- 현재 요청된 환경변수에 `DATABASE_PASSWORD` 가 없어 그대로 반영. 실제 DB 접속 시 필요하면 추후 추가.

### core-batch (CronJob 3종)

공통 사항:

- `concurrencyPolicy: Forbid` — 이전 실행 미완료 시 새 실행 skip.
- `backoffLimit: 0` — k8s 레벨 재시도 없음. 실패 알림은 앱의 `app.alert.type=DISCORD` (`DISCORD_WEBHOOK_URL`) 가 처리.
- `successfulJobsHistoryLimit: 3`, `failedJobsHistoryLimit: 5`.
- `timeZone: Asia/Seoul` (k8s 1.27+ / k3s 최신 버전 지원).
- `restartPolicy: Never`.
- 리소스: `requests` 200m/512Mi, `limits` 1000m/2Gi.
- Secret 키: `BRAWLSTARS_API_KEYS`, `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `DISCORD_WEBHOOK_URL`.
- `command: ["sh", "-c"]` + `args` 로 shell expansion 사용.

| CronJob 파일 | `schedule` | job.name | date 파라미터 |
|---|---|---|---|
| `cronjob-battle-event-rotation-sync.yaml` | `"0 * * * *"` | `battleEventRotationSyncJob` | 없음 |
| `cronjob-brawler-battle-rank-stats.yaml` | `"0 2 * * *"` | `brawlerBattleRankStatsJob` | `date=$(date -d 'yesterday' +%Y-%m-%d)` |
| `cronjob-brawler-battle-result-stats.yaml` | `"0 2 * * *"` | `brawlerBattleResultStatsJob` | `date=$(date -d 'yesterday' +%Y-%m-%d)` |

예시 (rank-stats):

```yaml
apiVersion: batch/v1
kind: CronJob
metadata:
  name: brawler-battle-rank-stats
  namespace: imstargg-prod
spec:
  schedule: "0 2 * * *"
  timeZone: Asia/Seoul
  concurrencyPolicy: Forbid
  successfulJobsHistoryLimit: 3
  failedJobsHistoryLimit: 5
  jobTemplate:
    spec:
      backoffLimit: 0
      template:
        spec:
          restartPolicy: Never
          containers:
            - name: core-batch
              image: imstargg/core-batch:v0.1.0-2026.04.19-xxxxxxx
              imagePullPolicy: IfNotPresent
              env:
                - name: SPRING_PROFILES_ACTIVE
                  value: prod
              envFrom:
                - secretRef: { name: core-batch-secret }
              command: ["sh", "-c"]
              args:
                - >
                  java -jar /app/app.jar
                  --job.name=brawlerBattleRankStatsJob
                  date=$(date -d 'yesterday' +%Y-%m-%d)
              resources:
                requests: { cpu: 200m, memory: 512Mi }
                limits:   { cpu: 1000m, memory: 2Gi }
```

rotation-sync 의 args 는 `date=...` 없이 `--job.name=battleEventRotationSyncJob` 만 포함.

## Secret 주입 플로우

서버에서 최초 1회 (또는 시크릿 변경 시):

```bash
kubectl apply -f k8s/prod/namespace.yaml

kubectl -n imstargg-prod create secret generic core-api-secret \
  --from-env-file=/path/to/core-api.env --dry-run=client -o yaml | kubectl apply -f -
kubectl -n imstargg-prod create secret generic core-worker-secret \
  --from-env-file=/path/to/core-worker.env --dry-run=client -o yaml | kubectl apply -f -
kubectl -n imstargg-prod create secret generic core-batch-secret \
  --from-env-file=/path/to/core-batch.env --dry-run=client -o yaml | kubectl apply -f -
```

- `--dry-run=client -o yaml | kubectl apply -f -` 패턴으로 upsert.
- `SPRING_PROFILES_ACTIVE=prod` 는 Secret 이 아닌 Deployment `env` 에 평문으로 둔다 (환경 식별자 성격).

## README 구성 (`imstargg-deploy/README.md`)

주요 섹션:

1. **사전 요구사항** — Docker (buildx 활성), kubectl, k3s 서버 SSH 접근.
2. **이미지 빌드 (로컬)**
   - `./scripts/build-image.sh core-worker v1.0.0`
3. **이미지 전송**
   - `./scripts/save-image.sh imstargg/core-worker:v1.0.0-2026.04.19-xxxxxxx`
   - `scp <tar.gz> user@server:/tmp/`
   - 서버: `./scripts/load-image.sh /tmp/<tar.gz>`
4. **최초 배포 준비**
   - namespace 생성, 3개 모듈의 secret 생성.
5. **배포**
   - 해당 `deployment.yaml` / `cronjob-*.yaml` 의 `image:` 필드를 새 full tag 로 업데이트.
   - `kubectl apply -f k8s/prod/core-worker/deployment.yaml`
   - `VERSIONS.md` 업데이트, git commit, `git tag core-worker-v1.0.0`.
6. **롤백**
   - `image:` 를 이전 full tag 로 되돌려 `kubectl apply`, 또는 `kubectl rollout undo deployment/core-worker -n imstargg-prod`.
7. **CronJob 수동 실행 (검증용)**
   - `kubectl create job --from=cronjob/brawler-battle-rank-stats manual-run-$(date +%s) -n imstargg-prod`
8. **로그 확인**
   - `kubectl logs -f deployment/core-worker -n imstargg-prod`
   - `kubectl logs -f -l app=core-api -n imstargg-prod`
9. **버전 관리 규칙** — full tag 를 매니페스트에 기록, `latest` 는 매니페스트 사용 금지, `VERSIONS.md` 수동 갱신.

## 범위 외 (추후 과제)

- core-api Ingress / TLS 설정.
- core-admin 배포 (본 설계 범위 아님).
- core-batch 나머지 Job 배포 (사용자가 직접 설정).
- amd64 전환 시 buildx platform 설정 변경 (현재는 arm64 고정).
- 멀티 노드 확장 시 tar 전송 자동화 또는 registry 도입.
- 시크릿 암호화 관리 (SOPS / sealed-secrets / ExternalSecrets) — 현재는 서버 로컬 `.env` 파일 의존.
- Prometheus/Grafana 연동, 로그 수집 파이프라인.

## 검증 기준

- `./gradlew :core:core-worker:bootJar` 가 로컬 build 에서 성공.
- `build-image.sh core-worker v0.1.0` 수행 시 arm64 이미지 생성 및 `docker images` 에서 3개 태그 확인.
- save/load 스크립트로 k3s 서버 containerd 에 이미지 import 성공 (`sudo k3s ctr images ls | grep core-worker`).
- `kubectl apply` 후 Pod `Running` 및 readiness 통과.
- core-batch CronJob 수동 트리거 (`kubectl create job --from=cronjob/...`) 시 Spring Batch 가 지정된 `job.name` 으로 실행되고 정상 종료 (ExitCode 0).
