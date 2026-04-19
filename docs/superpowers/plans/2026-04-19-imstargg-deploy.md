# imstargg-deploy Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `core-api`, `core-worker`, `core-batch` 를 k3s(prod) 에 배포하기 위한 Dockerfile, 빌드/전송 스크립트, k8s 매니페스트, README 를 작성하고 k8s probe 동작을 위해 `monitoring.yml` 의 health endpoint 노출 설정을 보강한다.

**Architecture:** 로컬 `docker buildx` 로 arm64 이미지를 빌드 → `docker save` + `scp` + `k3s ctr images import` 로 레지스트리 없이 배포. 매니페스트는 `imstargg-v2/imstargg-deploy/k8s/prod/` 에 저장되며 image tag 는 full tag (`v{semver}-{YYYY.MM.DD}-{sha}`) 로 하드코딩. core-batch 는 3개 CronJob 을 단일 multi-document yaml 로 관리.

**Tech Stack:** Docker (buildx), Kubernetes (k3s, containerd), kubectl, Spring Boot Actuator, bash.

**Design Reference:** `docs/superpowers/specs/2026-04-19-imstargg-deploy-design.md`

---

## File Structure

**Created files:**
- `imstargg-deploy/README.md` — 빌드·전송·배포·롤백 가이드
- `imstargg-deploy/.gitignore` — 로컬 빌드 산출물(tar.gz), secret.env 실파일 제외
- `imstargg-deploy/scripts/build-image.sh` — buildx 로 이미지 빌드 + 태깅
- `imstargg-deploy/scripts/save-image.sh` — tar.gz 로 이미지 저장
- `imstargg-deploy/scripts/load-image.sh` — k3s containerd 로 이미지 import (서버)
- `imstargg-deploy/k8s/prod/namespace.yaml`
- `imstargg-deploy/k8s/prod/core-api/deployment.yaml`
- `imstargg-deploy/k8s/prod/core-api/service.yaml`
- `imstargg-deploy/k8s/prod/core-api/secret.env.example`
- `imstargg-deploy/k8s/prod/core-worker/deployment.yaml`
- `imstargg-deploy/k8s/prod/core-worker/secret.env.example`
- `imstargg-deploy/k8s/prod/core-batch/cronjobs.yaml` — 3개 CronJob 통합
- `imstargg-deploy/k8s/prod/core-batch/secret.env.example`
- `imstargg-backend/core/core-api/Dockerfile`
- `imstargg-backend/core/core-worker/Dockerfile`
- `imstargg-backend/core/core-batch/Dockerfile`

**Modified files:**
- `imstargg-backend/support/monitoring/src/main/resources/monitoring.yml` — health endpoint 노출 및 probes 그룹 설정 추가

**Responsibility boundaries:**
- `Dockerfile` 은 모듈별로 분리(모듈별 jar 경로/Gradle task 고정) — 빌드 컨텍스트는 `imstargg-backend/` 전체.
- k8s 매니페스트는 모듈별 폴더로 분리하되, 파일 수는 최소화(core-batch 는 단일 `cronjobs.yaml`).
- 스크립트는 책임 단위 3종으로만 분리 (build / save / load).

---

### Task 1: monitoring.yml 에 health endpoint 및 probes 그룹 노출 설정

**Files:**
- Modify: `imstargg-backend/support/monitoring/src/main/resources/monitoring.yml`

core-api 의 liveness/readiness probe 가 `/actuator/health/liveness`, `/actuator/health/readiness` 를 호출할 수 있어야 한다. 현재는 `prometheus` 만 노출되어 404 가 발생하므로 health 를 추가하고 probes 그룹을 명시적으로 활성화한다.

- [ ] **Step 1: `monitoring.yml` 을 덮어써서 health endpoint + probes 설정 추가**

파일 전체를 다음 내용으로 교체:

```yaml
management:
  server:
    port: 9292
  endpoints:
    web:
      exposure:
        include: prometheus, health
  endpoint:
    health:
      probes:
        enabled: true
      group:
        liveness:
          include: livenessState
        readiness:
          include: readinessState, db
  otlp:
    metrics:
      export:
        enabled: false
    tracing:
      export:
        enabled: false
```

- [ ] **Step 2: YAML 문법 및 actuator 키 구조 정적 검증**

Run:
```bash
python3 -c "import yaml; yaml.safe_load_all(open('imstargg-backend/support/monitoring/src/main/resources/monitoring.yml'))"
grep -E 'include:.*health' imstargg-backend/support/monitoring/src/main/resources/monitoring.yml
grep -E 'probes:' imstargg-backend/support/monitoring/src/main/resources/monitoring.yml
```

Expected:
- Python YAML 파싱이 오류 없이 완료 (종료 코드 0).
- `include: prometheus, health` 라인 출력.
- `probes:` 라인 출력.

실제 endpoint 동작 검증(`curl :9292/actuator/health/liveness`)은 이 plan 의 범위 외로, 배포 직후 Deployment Readiness Checklist 에서 수행한다. 로컬 bootRun 검증은 docker-compose 인프라(PostgreSQL/Valkey/localstack) 의존성이 있어 plan 내 자동 단계로 신뢰성이 낮다.

- [ ] **Step 3: Commit**

```bash
git add imstargg-backend/support/monitoring/src/main/resources/monitoring.yml
git commit -m "feat(monitoring): k8s probe 를 위한 health endpoint 노출 설정 추가"
```

---

### Task 2: imstargg-deploy 루트와 .gitignore 생성

**Files:**
- Create: `imstargg-deploy/.gitignore`

배포 tar.gz 산출물과 실제 `secret.env` 파일이 git 에 올라가지 않도록 처음부터 차단.

- [ ] **Step 1: 디렉토리 생성**

```bash
mkdir -p imstargg-deploy/scripts
mkdir -p imstargg-deploy/k8s/prod/core-api
mkdir -p imstargg-deploy/k8s/prod/core-worker
mkdir -p imstargg-deploy/k8s/prod/core-batch
```

- [ ] **Step 2: `.gitignore` 생성**

`imstargg-deploy/.gitignore`:

```gitignore
# Image save 산출물
*.tar
*.tar.gz

# 실제 secret 파일 (example 만 커밋)
**/secret.env
!**/secret.env.example
```

- [ ] **Step 3: Commit**

```bash
git add imstargg-deploy/.gitignore
git commit -m "chore(deploy): imstargg-deploy 루트 초기화 및 .gitignore 추가"
```

---

### Task 3: 공용 Dockerfile 작성 (core-worker)

**Files:**
- Create: `imstargg-backend/core/core-worker/Dockerfile`

build-arg 로 모듈 경로를 받는 multi-stage Dockerfile. core-worker 용으로 먼저 생성해 빌드 검증.

- [ ] **Step 1: Dockerfile 작성**

`imstargg-backend/core/core-worker/Dockerfile`:

```dockerfile
# ---- Build stage ----
FROM --platform=$BUILDPLATFORM eclipse-temurin:21-jdk AS builder
WORKDIR /build
COPY . .
ARG MODULE_GRADLE_PATH=:core:core-worker
RUN ./gradlew ${MODULE_GRADLE_PATH}:bootJar -x test --no-daemon

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre
ENV TZ=Asia/Seoul
WORKDIR /app
ARG MODULE_JAR_DIR=core/core-worker
COPY --from=builder /build/${MODULE_JAR_DIR}/build/libs/*.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

- [ ] **Step 2: 로컬에서 arm64 이미지 빌드 (검증)**

Run:
```bash
docker buildx build \
  --platform linux/arm64 \
  --build-arg MODULE_GRADLE_PATH=:core:core-worker \
  --build-arg MODULE_JAR_DIR=core/core-worker \
  -t imstargg/core-worker:dev \
  --load \
  -f imstargg-backend/core/core-worker/Dockerfile \
  imstargg-backend
```

Expected: `Successfully tagged imstargg/core-worker:dev`. `docker images imstargg/core-worker` 에 dev 태그 존재.

- [ ] **Step 3: 이미지 내용 검증 (JAR + TZ + Java)**

Run:
```bash
docker run --rm --entrypoint sh imstargg/core-worker:dev -c \
  'ls /app/app.jar && cat /etc/timezone 2>/dev/null || readlink /etc/localtime; java -version'
docker rmi imstargg/core-worker:dev
```

Expected:
- `/app/app.jar` 파일 존재 (ls 출력).
- TZ 가 `Asia/Seoul` 로 설정 (`/etc/timezone` 또는 `/etc/localtime` symlink 가 Seoul 을 가리킴).
- `openjdk version "21..."` 출력.

실제 Spring Boot 기동은 DB/SQS 인프라가 필요하므로 plan 범위 외. 배포 후 k8s Pod 로그에서 `Started CoreWorkerApplication` 확인.

- [ ] **Step 4: Commit**

```bash
git add imstargg-backend/core/core-worker/Dockerfile
git commit -m "build(core-worker): multi-stage Dockerfile (TZ=Asia/Seoul, arm64)"
```

---

### Task 4: core-api Dockerfile 작성

**Files:**
- Create: `imstargg-backend/core/core-api/Dockerfile`

Task 3 과 동일한 패턴. build-arg 기본값만 차이.

- [ ] **Step 1: Dockerfile 작성**

`imstargg-backend/core/core-api/Dockerfile`:

```dockerfile
# ---- Build stage ----
FROM --platform=$BUILDPLATFORM eclipse-temurin:21-jdk AS builder
WORKDIR /build
COPY . .
ARG MODULE_GRADLE_PATH=:core:core-api
RUN ./gradlew ${MODULE_GRADLE_PATH}:bootJar -x test --no-daemon

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre
ENV TZ=Asia/Seoul
WORKDIR /app
ARG MODULE_JAR_DIR=core/core-api
COPY --from=builder /build/${MODULE_JAR_DIR}/build/libs/*.jar /app/app.jar
EXPOSE 8080 9292
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

- [ ] **Step 2: 빌드 검증**

Run:
```bash
docker buildx build \
  --platform linux/arm64 \
  -t imstargg/core-api:dev \
  --load \
  -f imstargg-backend/core/core-api/Dockerfile \
  imstargg-backend
docker rmi imstargg/core-api:dev
```

Expected: 빌드 성공.

- [ ] **Step 3: Commit**

```bash
git add imstargg-backend/core/core-api/Dockerfile
git commit -m "build(core-api): multi-stage Dockerfile (management port 9292 노출)"
```

---

### Task 5: core-batch Dockerfile 작성

**Files:**
- Create: `imstargg-backend/core/core-batch/Dockerfile`

- [ ] **Step 1: Dockerfile 작성**

`imstargg-backend/core/core-batch/Dockerfile`:

```dockerfile
# ---- Build stage ----
FROM --platform=$BUILDPLATFORM eclipse-temurin:21-jdk AS builder
WORKDIR /build
COPY . .
ARG MODULE_GRADLE_PATH=:core:core-batch
RUN ./gradlew ${MODULE_GRADLE_PATH}:bootJar -x test --no-daemon

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre
ENV TZ=Asia/Seoul
WORKDIR /app
ARG MODULE_JAR_DIR=core/core-batch
COPY --from=builder /build/${MODULE_JAR_DIR}/build/libs/*.jar /app/app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

core-batch 는 외부 포트 노출 불필요(CronJob, web-application-type=none).

- [ ] **Step 2: 빌드 검증**

Run:
```bash
docker buildx build \
  --platform linux/arm64 \
  -t imstargg/core-batch:dev \
  --load \
  -f imstargg-backend/core/core-batch/Dockerfile \
  imstargg-backend
docker rmi imstargg/core-batch:dev
```

Expected: 빌드 성공.

- [ ] **Step 3: Commit**

```bash
git add imstargg-backend/core/core-batch/Dockerfile
git commit -m "build(core-batch): multi-stage Dockerfile"
```

---

### Task 6: build-image.sh 스크립트 작성

**Files:**
- Create: `imstargg-deploy/scripts/build-image.sh`

- [ ] **Step 1: 스크립트 작성**

`imstargg-deploy/scripts/build-image.sh`:

```bash
#!/usr/bin/env bash
set -euo pipefail

usage() {
  echo "Usage: $0 <core-api|core-worker|core-batch> <version e.g. v1.0.0>"
  exit 1
}

MODULE="${1:-}"
VERSION="${2:-}"

if [[ -z "$MODULE" || -z "$VERSION" ]]; then usage; fi
if [[ ! "$MODULE" =~ ^core-(api|worker|batch)$ ]]; then
  echo "ERROR: invalid module: $MODULE" >&2
  usage
fi
if [[ ! "$VERSION" =~ ^v[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "ERROR: version must match vMAJOR.MINOR.PATCH: $VERSION" >&2
  usage
fi

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
BACKEND_DIR="${REPO_ROOT}/imstargg-backend"

SHA=$(git -C "${REPO_ROOT}" rev-parse --short HEAD)
DATE=$(date +%Y.%m.%d)
FULL_TAG="${VERSION}-${DATE}-${SHA}"
IMAGE="imstargg/${MODULE}"

echo "Building ${IMAGE}:${FULL_TAG}"

docker buildx build \
  --platform linux/arm64 \
  -t "${IMAGE}:${FULL_TAG}" \
  -t "${IMAGE}:${VERSION}" \
  -t "${IMAGE}:latest" \
  --build-arg "MODULE_GRADLE_PATH=:core:${MODULE}" \
  --build-arg "MODULE_JAR_DIR=core/${MODULE}" \
  --load \
  -f "${BACKEND_DIR}/core/${MODULE}/Dockerfile" \
  "${BACKEND_DIR}"

echo ""
echo "Built tags:"
echo "  ${IMAGE}:${FULL_TAG}  (k8s 매니페스트에 사용)"
echo "  ${IMAGE}:${VERSION}   (이동 태그)"
echo "  ${IMAGE}:latest       (이동 태그)"
```

- [ ] **Step 2: 실행 권한 부여**

Run:
```bash
chmod +x imstargg-deploy/scripts/build-image.sh
```

- [ ] **Step 3: 스크립트 실제 실행 검증**

Run:
```bash
./imstargg-deploy/scripts/build-image.sh core-worker v0.1.0
docker images imstargg/core-worker
```

Expected: `imstargg/core-worker` 에 `v0.1.0-<DATE>-<SHA>`, `v0.1.0`, `latest` 세 태그 모두 생성.

- [ ] **Step 4: 인자 검증 로직 확인**

Run:
```bash
./imstargg-deploy/scripts/build-image.sh invalid-module v1.0.0 || echo "rejected"
./imstargg-deploy/scripts/build-image.sh core-worker 1.0.0 || echo "rejected"
```

Expected: 두 명령 모두 에러 출력 후 `rejected` 표시.

- [ ] **Step 5: Commit**

```bash
git add imstargg-deploy/scripts/build-image.sh
git commit -m "feat(deploy): build-image.sh 스크립트 (buildx + 버전 태깅)"
```

---

### Task 7: save-image.sh 스크립트 작성

**Files:**
- Create: `imstargg-deploy/scripts/save-image.sh`

- [ ] **Step 1: 스크립트 작성**

`imstargg-deploy/scripts/save-image.sh`:

```bash
#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 1 ]]; then
  echo "Usage: $0 <image:full-tag>"
  exit 1
fi

IMAGE_TAG="$1"
OUT="$(echo "${IMAGE_TAG}" | tr '/:' '__').tar.gz"

echo "Saving ${IMAGE_TAG} -> ${OUT}"
docker save "${IMAGE_TAG}" | gzip > "${OUT}"
ls -lh "${OUT}"
```

- [ ] **Step 2: 실행 권한 + 동작 검증**

Run:
```bash
chmod +x imstargg-deploy/scripts/save-image.sh
# Task 6 에서 만든 이미지로 검증
FULL_TAG=$(docker images imstargg/core-worker --format '{{.Tag}}' | grep -E '^v0\.1\.0-' | head -1)
./imstargg-deploy/scripts/save-image.sh "imstargg/core-worker:${FULL_TAG}"
ls imstargg_core-worker_*.tar.gz
rm imstargg_core-worker_*.tar.gz
```

Expected: tar.gz 파일 생성 후 확인. 파일 크기 > 100MB 수준이면 정상.

- [ ] **Step 3: Commit**

```bash
git add imstargg-deploy/scripts/save-image.sh
git commit -m "feat(deploy): save-image.sh 스크립트 (docker save + gzip)"
```

---

### Task 8: load-image.sh 스크립트 작성 (서버용)

**Files:**
- Create: `imstargg-deploy/scripts/load-image.sh`

- [ ] **Step 1: 스크립트 작성**

`imstargg-deploy/scripts/load-image.sh`:

```bash
#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 1 ]]; then
  echo "Usage: $0 <file.tar.gz>   # run on k3s node"
  exit 1
fi

TAR_GZ="$1"

if [[ ! -f "${TAR_GZ}" ]]; then
  echo "ERROR: file not found: ${TAR_GZ}" >&2
  exit 1
fi

if ! command -v k3s >/dev/null 2>&1; then
  echo "ERROR: k3s not found in PATH. Run this script on the k3s node." >&2
  exit 1
fi

echo "Importing ${TAR_GZ} to k3s containerd..."
gunzip -c "${TAR_GZ}" | sudo k3s ctr images import -
echo "Done. Verify with: sudo k3s ctr images ls | grep imstargg"
```

- [ ] **Step 2: 실행 권한 + 구문 검증 (로컬 검증 불가, syntax 만)**

Run:
```bash
chmod +x imstargg-deploy/scripts/load-image.sh
bash -n imstargg-deploy/scripts/load-image.sh && echo "syntax OK"
./imstargg-deploy/scripts/load-image.sh 2>&1 | head -1
```

Expected: `syntax OK` 출력. 인자 없이 실행 시 Usage 출력.

- [ ] **Step 3: Commit**

```bash
git add imstargg-deploy/scripts/load-image.sh
git commit -m "feat(deploy): load-image.sh 스크립트 (k3s ctr images import)"
```

---

### Task 9: namespace.yaml 생성

**Files:**
- Create: `imstargg-deploy/k8s/prod/namespace.yaml`

- [ ] **Step 1: 매니페스트 작성**

`imstargg-deploy/k8s/prod/namespace.yaml`:

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: imstargg-prod
```

- [ ] **Step 2: 유효성 검증 (kubectl client-side)**

Run:
```bash
kubectl apply --dry-run=client -f imstargg-deploy/k8s/prod/namespace.yaml
```

Expected: `namespace/imstargg-prod created (dry run)` 출력.

- [ ] **Step 3: Commit**

```bash
git add imstargg-deploy/k8s/prod/namespace.yaml
git commit -m "feat(deploy): imstargg-prod namespace 매니페스트"
```

---

### Task 10: core-api 매니페스트 작성 (deployment + service + secret.env.example)

**Files:**
- Create: `imstargg-deploy/k8s/prod/core-api/deployment.yaml`
- Create: `imstargg-deploy/k8s/prod/core-api/service.yaml`
- Create: `imstargg-deploy/k8s/prod/core-api/secret.env.example`

- [ ] **Step 1: deployment.yaml 작성**

`imstargg-deploy/k8s/prod/core-api/deployment.yaml`:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: core-api
  namespace: imstargg-prod
spec:
  replicas: 1
  selector:
    matchLabels:
      app: core-api
  template:
    metadata:
      labels:
        app: core-api
    spec:
      containers:
        - name: core-api
          image: imstargg/core-api:v0.1.0-2026.04.19-REPLACE_SHA
          imagePullPolicy: IfNotPresent
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: prod
          envFrom:
            - secretRef:
                name: core-api-secret
          ports:
            - name: http
              containerPort: 8080
            - name: management
              containerPort: 9292
          resources:
            requests:
              cpu: 200m
              memory: 512Mi
            limits:
              cpu: 1000m
              memory: 2Gi
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 9292
            initialDelaySeconds: 60
            periodSeconds: 30
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 9292
            initialDelaySeconds: 30
            periodSeconds: 10
```

`REPLACE_SHA` 는 최초 배포 시 실제 git short SHA 로 치환. 이 placeholder 는 최초 실제 배포 커밋 직전에 교체되며, 이후 모든 bump 는 이 필드를 직접 편집.

- [ ] **Step 2: service.yaml 작성**

`imstargg-deploy/k8s/prod/core-api/service.yaml`:

```yaml
apiVersion: v1
kind: Service
metadata:
  name: core-api
  namespace: imstargg-prod
spec:
  type: ClusterIP
  selector:
    app: core-api
  ports:
    - name: http
      port: 80
      targetPort: 8080
```

- [ ] **Step 3: secret.env.example 작성**

`imstargg-deploy/k8s/prod/core-api/secret.env.example`:

```env
AWS_ACCESS_KEY=
AWS_SECRET_KEY=
DATABASE_URL=
DATABASE_USERNAME=
DATABASE_PASSWORD=
```

- [ ] **Step 4: 유효성 검증**

Run:
```bash
kubectl apply --dry-run=client -f imstargg-deploy/k8s/prod/core-api/deployment.yaml
kubectl apply --dry-run=client -f imstargg-deploy/k8s/prod/core-api/service.yaml
```

Expected: 두 명령 모두 `(dry run)` 성공 출력. 실패 시 yaml 문법 확인.

- [ ] **Step 5: Commit**

```bash
git add imstargg-deploy/k8s/prod/core-api/
git commit -m "feat(deploy): core-api k8s 매니페스트 (deployment/service/secret example)"
```

---

### Task 11: core-worker 매니페스트 작성

**Files:**
- Create: `imstargg-deploy/k8s/prod/core-worker/deployment.yaml`
- Create: `imstargg-deploy/k8s/prod/core-worker/secret.env.example`

- [ ] **Step 1: deployment.yaml 작성 (probe 없음)**

`imstargg-deploy/k8s/prod/core-worker/deployment.yaml`:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: core-worker
  namespace: imstargg-prod
spec:
  replicas: 1
  selector:
    matchLabels:
      app: core-worker
  template:
    metadata:
      labels:
        app: core-worker
    spec:
      containers:
        - name: core-worker
          image: imstargg/core-worker:v0.1.0-2026.04.19-REPLACE_SHA
          imagePullPolicy: IfNotPresent
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: prod
          envFrom:
            - secretRef:
                name: core-worker-secret
          resources:
            requests:
              cpu: 200m
              memory: 512Mi
            limits:
              cpu: 1000m
              memory: 2Gi
```

core-worker 는 `spring-boot-starter` (non-web) 사용으로 HTTP 서버 미기동. probe/ports 미설정. Deployment 의 기본 `restartPolicy: Always` 가 프로세스 crash 복구.

- [ ] **Step 2: secret.env.example 작성**

`imstargg-deploy/k8s/prod/core-worker/secret.env.example`:

```env
AWS_ACCESS_KEY=
AWS_SECRET_KEY=
BRAWLSTARS_API_KEYS=
DATABASE_URL=
DATABASE_USERNAME=
```

(spec 에 명시된 대로 `DATABASE_PASSWORD` 는 포함하지 않음.)

- [ ] **Step 3: 유효성 검증**

Run:
```bash
kubectl apply --dry-run=client -f imstargg-deploy/k8s/prod/core-worker/deployment.yaml
```

Expected: `(dry run)` 성공.

- [ ] **Step 4: Commit**

```bash
git add imstargg-deploy/k8s/prod/core-worker/
git commit -m "feat(deploy): core-worker k8s 매니페스트 (non-web, probe 없음)"
```

---

### Task 12: core-batch cronjobs.yaml 작성 (3 CronJob 통합)

**Files:**
- Create: `imstargg-deploy/k8s/prod/core-batch/cronjobs.yaml`
- Create: `imstargg-deploy/k8s/prod/core-batch/secret.env.example`

- [ ] **Step 1: cronjobs.yaml 작성 (multi-document)**

`imstargg-deploy/k8s/prod/core-batch/cronjobs.yaml`:

```yaml
apiVersion: batch/v1
kind: CronJob
metadata:
  name: battle-event-rotation-sync
  namespace: imstargg-prod
spec:
  schedule: "0 * * * *"
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
              image: imstargg/core-batch:v0.1.0-2026.04.19-REPLACE_SHA
              imagePullPolicy: IfNotPresent
              env:
                - name: SPRING_PROFILES_ACTIVE
                  value: prod
              envFrom:
                - secretRef:
                    name: core-batch-secret
              command: ["sh", "-c"]
              args:
                - >
                  java -jar /app/app.jar
                  --job.name=battleEventRotationSyncJob
              resources:
                requests:
                  cpu: 200m
                  memory: 512Mi
                limits:
                  cpu: 1000m
                  memory: 2Gi
---
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
              image: imstargg/core-batch:v0.1.0-2026.04.19-REPLACE_SHA
              imagePullPolicy: IfNotPresent
              env:
                - name: SPRING_PROFILES_ACTIVE
                  value: prod
              envFrom:
                - secretRef:
                    name: core-batch-secret
              command: ["sh", "-c"]
              args:
                - >
                  java -jar /app/app.jar
                  --job.name=brawlerBattleRankStatsJob
                  date=$(date -d 'yesterday' +%Y-%m-%d)
              resources:
                requests:
                  cpu: 200m
                  memory: 512Mi
                limits:
                  cpu: 1000m
                  memory: 2Gi
---
apiVersion: batch/v1
kind: CronJob
metadata:
  name: brawler-battle-result-stats
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
              image: imstargg/core-batch:v0.1.0-2026.04.19-REPLACE_SHA
              imagePullPolicy: IfNotPresent
              env:
                - name: SPRING_PROFILES_ACTIVE
                  value: prod
              envFrom:
                - secretRef:
                    name: core-batch-secret
              command: ["sh", "-c"]
              args:
                - >
                  java -jar /app/app.jar
                  --job.name=brawlerBattleResultStatsJob
                  date=$(date -d 'yesterday' +%Y-%m-%d)
              resources:
                requests:
                  cpu: 200m
                  memory: 512Mi
                limits:
                  cpu: 1000m
                  memory: 2Gi
```

- [ ] **Step 2: secret.env.example 작성**

`imstargg-deploy/k8s/prod/core-batch/secret.env.example`:

```env
BRAWLSTARS_API_KEYS=
DATABASE_URL=
DATABASE_USERNAME=
DATABASE_PASSWORD=
DISCORD_WEBHOOK_URL=
```

- [ ] **Step 3: 유효성 검증**

Run:
```bash
kubectl apply --dry-run=client -f imstargg-deploy/k8s/prod/core-batch/cronjobs.yaml
```

Expected: 3개 CronJob 모두 `(dry run)` 성공 출력.

- [ ] **Step 4: image tag 가 3곳 모두 동일한지 확인**

Run:
```bash
grep -c 'imstargg/core-batch:' imstargg-deploy/k8s/prod/core-batch/cronjobs.yaml
grep 'imstargg/core-batch:' imstargg-deploy/k8s/prod/core-batch/cronjobs.yaml | sort -u | wc -l
```

Expected: 첫 번째는 `3`, 두 번째는 `1` (동일 태그 3회 등장).

- [ ] **Step 5: Commit**

```bash
git add imstargg-deploy/k8s/prod/core-batch/
git commit -m "feat(deploy): core-batch 3개 CronJob 을 단일 cronjobs.yaml 로 작성"
```

---

### Task 13: README.md 작성

**Files:**
- Create: `imstargg-deploy/README.md`

- [ ] **Step 1: README 작성**

`imstargg-deploy/README.md`:

````markdown
# imstargg-deploy

`core-api`, `core-worker`, `core-batch` 를 k3s (prod, 단일 노드) 에 배포하기 위한 스크립트·매니페스트 모음.

## 사전 요구사항

- Docker Desktop (buildx 활성) — 로컬 빌드
- kubectl — 서버 kubeconfig 로 prod 클러스터 접근
- k3s 노드 SSH 접근 권한
- git

## 디렉토리 구조

```
imstargg-deploy/
├── scripts/
│   ├── build-image.sh     # 로컬: buildx + 태깅
│   ├── save-image.sh      # 로컬: docker save + gzip
│   └── load-image.sh      # 서버: k3s ctr images import
└── k8s/prod/
    ├── namespace.yaml
    ├── core-api/{deployment,service,secret.env.example}.yaml
    ├── core-worker/{deployment,secret.env.example}.yaml
    └── core-batch/{cronjobs,secret.env.example}.yaml
```

## 버전 관리 정책

- 태그 형식: `v{MAJOR}.{MINOR}.{PATCH}-{YYYY.MM.DD}-{GIT_SHA}`
- k8s 매니페스트 `image:` 필드에 full tag 를 직접 하드코딩.
- 버전 변경 = yaml 편집 + git commit. `git log k8s/prod/<module>/...` 이 버전 이력.
- `latest`, `v{semver}` 이동 태그는 로컬 편의용으로만 존재. 매니페스트에 사용 금지.

## 1. 이미지 빌드 (로컬)

```bash
./scripts/build-image.sh core-worker v1.0.0
# 태그 3종 생성:
#   imstargg/core-worker:v1.0.0-2026.04.19-a3f9b2c
#   imstargg/core-worker:v1.0.0
#   imstargg/core-worker:latest
```

## 2. 이미지 전송

```bash
FULL_TAG=v1.0.0-2026.04.19-a3f9b2c
./scripts/save-image.sh imstargg/core-worker:${FULL_TAG}
# 산출물: imstargg_core-worker_v1.0.0-2026.04.19-a3f9b2c.tar.gz

scp imstargg_core-worker_${FULL_TAG}.tar.gz user@server:/tmp/
```

## 3. 서버에서 이미지 import

```bash
ssh user@server
cd /path/to/imstargg-deploy
./scripts/load-image.sh /tmp/imstargg_core-worker_v1.0.0-2026.04.19-a3f9b2c.tar.gz
sudo k3s ctr images ls | grep imstargg/core-worker
```

## 4. 최초 배포 (namespace + secret)

```bash
kubectl apply -f k8s/prod/namespace.yaml

# secret.env.example 을 복사해 실제 값 채움 (서버 로컬에 보관, git 커밋 금지)
cp k8s/prod/core-api/secret.env.example /path/to/core-api.env
# ... 값 편집

kubectl -n imstargg-prod create secret generic core-api-secret \
  --from-env-file=/path/to/core-api.env \
  --dry-run=client -o yaml | kubectl apply -f -

kubectl -n imstargg-prod create secret generic core-worker-secret \
  --from-env-file=/path/to/core-worker.env \
  --dry-run=client -o yaml | kubectl apply -f -

kubectl -n imstargg-prod create secret generic core-batch-secret \
  --from-env-file=/path/to/core-batch.env \
  --dry-run=client -o yaml | kubectl apply -f -
```

## 5. 배포 (버전 bump)

### core-api / core-worker

```bash
NEW_TAG=v1.0.0-2026.04.19-a3f9b2c
sed -i '' "s|imstargg/core-worker:[^[:space:]]*|imstargg/core-worker:${NEW_TAG}|g" \
  k8s/prod/core-worker/deployment.yaml
git diff k8s/prod/core-worker/deployment.yaml   # 변경 확인
git commit -am "deploy(core-worker): bump to ${NEW_TAG}"
kubectl apply -f k8s/prod/core-worker/deployment.yaml
kubectl -n imstargg-prod rollout status deployment/core-worker
```

### core-batch (3 CronJob 동시 bump)

```bash
NEW_TAG=v1.0.0-2026.04.19-a3f9b2c
sed -i '' "s|imstargg/core-batch:[^[:space:]]*|imstargg/core-batch:${NEW_TAG}|g" \
  k8s/prod/core-batch/cronjobs.yaml
git diff k8s/prod/core-batch/cronjobs.yaml   # 3곳 변경 확인 필수
git commit -am "deploy(core-batch): bump to ${NEW_TAG}"
kubectl apply -f k8s/prod/core-batch/cronjobs.yaml
```

## 6. 롤백

```bash
# git 기반 롤백 (권장)
git log k8s/prod/core-worker/deployment.yaml
git checkout <prev-commit> -- k8s/prod/core-worker/deployment.yaml
git commit -m "deploy(core-worker): rollback to <prev-full-tag>"
kubectl apply -f k8s/prod/core-worker/deployment.yaml

# 긴급 시
kubectl -n imstargg-prod rollout undo deployment/core-worker
# 이후 git 상태 동기화 필요
```

## 7. CronJob 수동 실행 (검증용)

```bash
kubectl -n imstargg-prod create job \
  --from=cronjob/brawler-battle-rank-stats \
  manual-$(date +%s)
kubectl -n imstargg-prod logs -f job/manual-<timestamp>
```

## 8. 로그 확인

```bash
kubectl -n imstargg-prod logs -f deployment/core-worker
kubectl -n imstargg-prod logs -f deployment/core-api
kubectl -n imstargg-prod get pods -l job-name
```

## 향후 과제

- amd64 전환 시 `build-image.sh` 의 `--platform linux/arm64` 를 `linux/arm64,linux/amd64` 로 변경.
- dev 네임스페이스 추가 시 `k8s/dev/` 디렉토리 생성 및 kustomize 도입 고려.
- core-admin 및 나머지 core-batch Job 의 배포는 별도 과제.
````

- [ ] **Step 2: Markdown 문법 검증 (git diff 확인)**

Run:
```bash
cat imstargg-deploy/README.md | head -30
wc -l imstargg-deploy/README.md
```

Expected: 150~200줄 수준, 위 섹션이 순서대로 출력.

- [ ] **Step 3: Commit**

```bash
git add imstargg-deploy/README.md
git commit -m "docs(deploy): imstargg-deploy README 작성"
```

---

### Task 14: 통합 검증 — 전체 매니페스트 dry-run apply

**Files:** 없음 (검증만)

- [ ] **Step 1: 전체 prod 매니페스트 dry-run**

Run:
```bash
kubectl apply --dry-run=client -R -f imstargg-deploy/k8s/prod/
```

Expected 출력 (순서 무관):
- `namespace/imstargg-prod created (dry run)`
- `deployment.apps/core-api created (dry run)`
- `service/core-api created (dry run)`
- `deployment.apps/core-worker created (dry run)`
- `cronjob.batch/battle-event-rotation-sync created (dry run)`
- `cronjob.batch/brawler-battle-rank-stats created (dry run)`
- `cronjob.batch/brawler-battle-result-stats created (dry run)`

- [ ] **Step 2: secret.env 실파일이 git 에 없는지 확인**

Run:
```bash
git ls-files imstargg-deploy/ | grep -v '\.example$' | grep 'secret\.env' && echo "LEAK" || echo "clean"
```

Expected: `clean` 출력.

- [ ] **Step 3: core-batch image tag 일관성 최종 확인**

Run:
```bash
grep 'imstargg/core-batch:' imstargg-deploy/k8s/prod/core-batch/cronjobs.yaml | awk '{print $2}' | sort -u
```

Expected: 1줄만 출력 (3개 CronJob 이 모두 동일 태그).

- [ ] **Step 4: scripts 실행 권한 확인**

Run:
```bash
ls -l imstargg-deploy/scripts/*.sh
```

Expected: 3개 파일 모두 `-rwxr-xr-x` (실행 권한).

- [ ] **Step 5: 최종 commit (검증 자체는 커밋 없음, 문제 발견 시 이전 Task 로 돌아가 수정)**

문제 없으면 다음 단계(실제 서버 배포)로 진행. 문제가 있으면 해당 Task 로 돌아가 수정 후 재검증.

---

## Deployment Readiness Checklist (plan 범위 외, 실제 배포 시)

아래는 plan 의 구현 범위가 아니다. 실제 prod 배포 시 사용자/운영자가 수행.

- [ ] k3s 노드에서 `sudo k3s ctr images import` 성공
- [ ] namespace 및 3개 secret 생성
- [ ] 매니페스트의 `REPLACE_SHA` placeholder 를 최초 빌드의 실제 SHA 로 교체 + commit
- [ ] `kubectl apply -R -f k8s/prod/` 로 배포
- [ ] core-api Pod `curl -f http://<pod-ip>:9292/actuator/health/liveness` 로 200 확인
- [ ] CronJob 수동 트리거 후 Spring Batch job 정상 종료 (ExitCode 0) 확인
