# 백엔드 도커 이미지 멀티플랫폼 CI 개선 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** core-api/core-worker/core-batch 도커 이미지를, Java 빌드를 이미지 내부에서 수행하는 단일 멀티스테이지 Dockerfile + buildx 로 `linux/arm64` 빌드하도록 CI 를 개선한다.

**Architecture:** `--platform=$BUILDPLATFORM` 으로 고정한 빌더 스테이지에서 Gradle bootJar 를 네이티브로 1회 빌드하고 Spring Boot 4 `jarmode=tools` 로 레이어를 추출한다. 런타임 스테이지는 추출 레이어를 COPY 만 하므로(RUN 없음) arm64 이미지를 만들 때도 QEMU 가 불필요하다. 하나의 파라미터화 Dockerfile(`ARG GRADLE_MODULE`)이 세 앱을 모두 빌드한다.

**Tech Stack:** Docker BuildKit / buildx, Spring Boot 4.0.2, Gradle 9.3.0 (wrapper), Java 21 (eclipse-temurin), GitHub Actions, AWS ECR.

**Spec:** `docs/superpowers/specs/2026-05-30-docker-multiplatform-ci-design.html`

**Prerequisites (실행 환경):**
- 로컬에 Docker(Desktop 또는 colima)가 실행 중이고 `docker buildx` 사용 가능.
- 로컬 머신은 Apple Silicon(arm64)이므로 `--platform linux/arm64` 가 네이티브로 `--load` 된다.
- 최초 빌드는 컨테이너 내부에서 Gradle 의존성을 내려받으므로 수 분 소요(네트워크 필요).

---

## File Structure

- **Create** `imstargg-backend/.dockerignore` — 호스트 빌드 산출물/IDE/VCS 를 빌드 컨텍스트에서 제외(특히 `**/.gradle/`, `**/build/`).
- **Create** `imstargg-backend/Dockerfile` — 단일 멀티스테이지 Dockerfile. `ARG GRADLE_MODULE` 로 빌드 대상 결정.
- **Modify** `.github/workflows/deploy.yaml` — `build-jar` 잡 제거, `build-and-push` 를 in-image 빌드로 전환, `cleanup` 단순화.
- **Delete** `imstargg-backend/core/{core-api,core-worker,core-batch,core-admin}/Dockerfile` — 단일 Dockerfile 로 대체.
- **Modify** `imstargg-backend/AGENTS.md` — 모듈 추가 시 Dockerfile COPY 목록 갱신 필요 메모 1줄.

---

## Task 1: 단일 Dockerfile + .dockerignore 추가 및 로컬 빌드 검증

**Files:**
- Create: `imstargg-backend/.dockerignore`
- Create: `imstargg-backend/Dockerfile`

- [ ] **Step 1: `.dockerignore` 작성**

Create `imstargg-backend/.dockerignore`:

```gitignore
**/build/
**/bin/
**/.gradle/
.gradle/
.idea/
*.iml
**/.apt_generated/
**/.kotlin/
.prompt/
```

- [ ] **Step 2: 단일 `Dockerfile` 작성**

Create `imstargg-backend/Dockerfile`:

```dockerfile
# syntax=docker/dockerfile:1

# ── Builder: 네이티브 플랫폼에서 실행 (Kotlin 컴파일 에뮬레이션 회피) ──
FROM --platform=$BUILDPLATFORM eclipse-temurin:21-jdk-jammy AS builder
ENV GRADLE_USER_HOME=/workspace/.gradle
WORKDIR /workspace
ARG GRADLE_MODULE

# 1) 빌드 스크립트만 먼저 복사 → 의존성 해석 레이어 캐싱
#    (모듈 추가 시 아래 목록 갱신 필요)
COPY gradlew settings.gradle.kts gradle.properties build.gradle.kts ./
COPY gradle ./gradle
COPY core/core-enum/build.gradle.kts            ./core/core-enum/
COPY core/core-api/build.gradle.kts             ./core/core-api/
COPY core/core-admin/build.gradle.kts           ./core/core-admin/
COPY core/core-batch/build.gradle.kts           ./core/core-batch/
COPY core/core-worker/build.gradle.kts          ./core/core-worker/
COPY infrastructure/db-core/build.gradle.kts    ./infrastructure/db-core/
COPY infrastructure/redis-core/build.gradle.kts ./infrastructure/redis-core/
COPY infrastructure/aws-sqs/build.gradle.kts    ./infrastructure/aws-sqs/
COPY infrastructure/aws-s3/build.gradle.kts     ./infrastructure/aws-s3/
COPY client/brawlstars-client/build.gradle.kts  ./client/brawlstars-client/
COPY support/logging/build.gradle.kts           ./support/logging/
COPY support/monitoring/build.gradle.kts        ./support/monitoring/
RUN chmod +x gradlew && ./gradlew ${GRADLE_MODULE}:dependencies --no-daemon

# 2) 소스 복사 후 bootJar + 레이어 추출 (jarmode=tools, Spring Boot 4)
#    GRADLE_MODULE(:core:core-api) → 파일경로(core/core-api)로 변환해 해당 모듈 build/libs 만 탐색
COPY . .
RUN ./gradlew ${GRADLE_MODULE}:bootJar -x test --no-daemon --build-cache --parallel \
 && MODULE_PATH="$(echo "${GRADLE_MODULE#:}" | tr ':' '/')" \
 && cp "$(find ${MODULE_PATH}/build/libs -name '*.jar' -not -name '*-plain.jar' | head -n1)" application.jar \
 && java -Djarmode=tools -jar application.jar extract --layers --destination extracted

# ── Runtime: 타깃 플랫폼(arm64). COPY 만 → QEMU 불필요 ──
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=builder --chown=1000:1000 /workspace/extracted/dependencies/ ./
COPY --from=builder --chown=1000:1000 /workspace/extracted/spring-boot-loader/ ./
COPY --from=builder --chown=1000:1000 /workspace/extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=1000:1000 /workspace/extracted/application/ ./
USER 1000:1000
ENTRYPOINT ["java", "-jar", "application.jar"]
```

- [ ] **Step 3: buildx 사용 가능 확인**

Run: `docker buildx version`
Expected: 버전 문자열 출력(예: `github.com/docker/buildx v0.x.x`). 오류면 Docker 가 실행 중인지 확인.

- [ ] **Step 4: core-api 로컬 arm64 빌드 (1차)**

Run:
```bash
docker buildx build \
  --platform linux/arm64 \
  --build-arg GRADLE_MODULE=:core:core-api \
  -f imstargg-backend/Dockerfile \
  -t imstargg/core-api:local \
  --load \
  imstargg-backend
```
Expected: 마지막에 `naming to docker.io/imstargg/core-api:local` 등 SUCCESS. (Gradle 의존성 다운로드로 수 분 소요 가능.)

- [ ] **Step 5: 이미지 구조 확인 (레이어 추출 결과)**

Run: `docker run --rm --entrypoint ls imstargg/core-api:local /app`
Expected: 출력 목록에 `application.jar` 포함. (그 외 `org`, `lib`/`BOOT-INF` 등 레이어 산출물도 보일 수 있음.)

- [ ] **Step 6: 런타임 JRE 동작 확인**

Run: `docker run --rm --entrypoint java imstargg/core-api:local -version`
Expected: `openjdk version "21..."` 출력.

- [ ] **Step 7: core-worker / core-batch 빌드 검증**

Run:
```bash
docker buildx build --platform linux/arm64 --build-arg GRADLE_MODULE=:core:core-worker \
  -f imstargg-backend/Dockerfile -t imstargg/core-worker:local --load imstargg-backend
docker buildx build --platform linux/arm64 --build-arg GRADLE_MODULE=:core:core-batch \
  -f imstargg-backend/Dockerfile -t imstargg/core-batch:local --load imstargg-backend
```
Expected: 두 빌드 모두 SUCCESS. (2차 빌드부터는 의존성 레이어 캐시 히트로 더 빠름.)

- [ ] **Step 8: (선택) core-api 기동 스모크 — DB 없이 부팅 시작만 확인**

Run:
```bash
docker run -d --name imstargg-smoke imstargg/core-api:local
docker logs imstargg-smoke   # Spring 배너가 보일 때까지 몇 번 재실행
docker rm -f imstargg-smoke
```
Expected: 로그에 Spring Boot 배너와 `Starting CoreApiApplication` 류 메시지. (DB 미연결로 이후 실패는 정상 — 이미지 조립/엔트리포인트가 올바름을 의미.)

- [ ] **Step 9: 커밋**

```bash
git add imstargg-backend/Dockerfile imstargg-backend/.dockerignore
git commit -m "feat(backend): in-image 빌드 단일 멀티스테이지 Dockerfile 추가

- core-api/worker/batch 공용 Dockerfile (ARG GRADLE_MODULE)
- BUILDPLATFORM 빌더로 네이티브 빌드, 런타임 COPY-only 로 arm64 QEMU 불요
- Spring Boot 4 jarmode=tools 레이어 추출

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 2: deploy.yaml 전환 + 기존 Dockerfile 4개 삭제

**Files:**
- Modify: `.github/workflows/deploy.yaml` (전체 교체)
- Delete: `imstargg-backend/core/core-api/Dockerfile`
- Delete: `imstargg-backend/core/core-worker/Dockerfile`
- Delete: `imstargg-backend/core/core-batch/Dockerfile`
- Delete: `imstargg-backend/core/core-admin/Dockerfile`

- [ ] **Step 1: `.github/workflows/deploy.yaml` 전체를 아래로 교체**

```yaml
name: Deploy

on:
  workflow_dispatch:
    inputs:
      app:
        description: '배포할 애플리케이션'
        required: true
        type: choice
        options:
          - core-api
          - core-batch
          - core-worker

env:
  AWS_REGION: ap-northeast-2

jobs:
  build-and-push:
    runs-on: ubuntu-latest
    permissions:
      id-token: write
      contents: read
    steps:
      - uses: actions/checkout@v4

      - uses: docker/setup-buildx-action@v3

      - uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: ${{ secrets.AWS_ROLE_ARN }}
          aws-region: ${{ env.AWS_REGION }}

      - id: login-ecr
        uses: aws-actions/amazon-ecr-login@v2

      - name: Set image tag
        run: |
          VERSION=$(./imstargg-deploy/scripts/get-app-version.sh "${{ inputs.app }}")
          echo "IMAGE_TAG=${VERSION}-${GITHUB_SHA::7}" >> "$GITHUB_ENV"

      - uses: docker/build-push-action@v6
        with:
          context: imstargg-backend
          file: imstargg-backend/Dockerfile
          build-args: |
            GRADLE_MODULE=:core:${{ inputs.app }}
          push: true
          platforms: linux/arm64
          provenance: false
          tags: ${{ steps.login-ecr.outputs.registry }}/imstargg/${{ inputs.app }}:${{ env.IMAGE_TAG }}
          cache-from: type=gha,scope=${{ inputs.app }}
          cache-to: type=gha,mode=max,scope=${{ inputs.app }}

  cleanup:
    needs: build-and-push
    if: always()
    runs-on: ubuntu-latest
    permissions:
      actions: write
    steps:
      - uses: actions/github-script@v7
        with:
          script: |
            const artifacts = await github.rest.actions.listArtifactsForRepo({
              owner: context.repo.owner,
              repo: context.repo.repo,
              per_page: 100
            });
            for (const artifact of artifacts.data.artifacts) {
              if (artifact.name.endsWith('.dockerbuild')) {
                await github.rest.actions.deleteArtifact({
                  owner: context.repo.owner,
                  repo: context.repo.repo,
                  artifact_id: artifact.id
                });
              }
            }

  update-image-tags:
    needs: build-and-push
    runs-on: ubuntu-latest
    permissions:
      id-token: write
      contents: write
    steps:
      - uses: actions/checkout@v4

      - uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: ${{ secrets.AWS_ROLE_ARN }}
          aws-region: ${{ env.AWS_REGION }}

      - id: login-ecr
        uses: aws-actions/amazon-ecr-login@v2

      - name: Update image tag
        env:
          ECR_REGISTRY: ${{ steps.login-ecr.outputs.registry }}
        run: |
          VERSION=$(./imstargg-deploy/scripts/get-app-version.sh "${{ inputs.app }}")
          IMAGE_TAG="${VERSION}-${GITHUB_SHA::7}"
          echo "IMAGE_TAG=${IMAGE_TAG}" >> "$GITHUB_ENV"
          cd imstargg-deploy/apps/prod

          if [ "${{ inputs.app }}" == "core-batch" ]; then
            yq e ".cronjob.image.repository = \"${ECR_REGISTRY}/imstargg/core-batch\"" -i core-batch/values.yaml
            yq e ".cronjob.image.tag = \"${IMAGE_TAG}\"" -i core-batch/values.yaml
          else
            yq e ".deployment.image.repository = \"${ECR_REGISTRY}/imstargg/${{ inputs.app }}\"" -i ${{ inputs.app }}/values.yaml
            yq e ".deployment.image.tag = \"${IMAGE_TAG}\"" -i ${{ inputs.app }}/values.yaml
          fi

      - name: Commit and push
        run: |
          git config user.name "github-actions[bot]"
          git config user.email "github-actions[bot]@users.noreply.github.com"
          git add imstargg-deploy/
          git diff --staged --quiet && exit 0
          git commit -m "chore: update ${{ inputs.app }} image tag to ${IMAGE_TAG}"

          for i in 1 2 3 4 5; do
            git push && exit 0
            echo "Push failed, rebasing and retrying ($i/5)..."
            git pull --rebase
          done
          echo "Failed to push after 5 attempts"
          exit 1
```

- [ ] **Step 2: 기존 Dockerfile 4개 삭제**

Run:
```bash
git rm imstargg-backend/core/core-api/Dockerfile \
       imstargg-backend/core/core-worker/Dockerfile \
       imstargg-backend/core/core-batch/Dockerfile \
       imstargg-backend/core/core-admin/Dockerfile
```
Expected: 4개 파일 `rm` 처리.

- [ ] **Step 3: deploy.yaml 잔존 참조 없음 확인**

Run: `grep -rn "core/core-.*/Dockerfile\|app.jar\|build-jar\|upload-artifact\|download-artifact" .github/workflows/deploy.yaml`
Expected: 출력 없음(빈 결과). 옛 잡/artifact/개별 Dockerfile 참조가 모두 사라졌음을 의미.

- [ ] **Step 4: deploy.yaml YAML 문법 검증**

Run: `yq e '.' .github/workflows/deploy.yaml > /dev/null && echo OK`
Expected: `OK`. (`yq` 미설치 시: `python3 -c "import yaml,sys; yaml.safe_load(open('.github/workflows/deploy.yaml')); print('OK')"`)

- [ ] **Step 5: 커밋**

```bash
git add .github/workflows/deploy.yaml
git commit -m "ci(deploy): in-image 빌드로 전환하고 개별 Dockerfile 제거

- build-jar 잡/artifact 전달 제거, build-and-push 가 단일 Dockerfile 로 직접 빌드
- platforms: linux/arm64, provenance: false (ECR manifest 호환)
- core-api/worker/batch/admin 개별 Dockerfile 삭제

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 3: 백엔드 AGENTS.md 에 유지보수 메모 추가

**Files:**
- Modify: `imstargg-backend/AGENTS.md`

- [ ] **Step 1: `### Module Dependencies` 섹션 끝에 메모 1줄 추가**

`imstargg-backend/AGENTS.md` 의 `### Module Dependencies` 블록(불릿 목록) 맨 아래에 다음 줄을 추가:

```markdown
- **Docker 빌드**: 루트 `Dockerfile`(단일, `ARG GRADLE_MODULE`)이 세 앱을 빌드. 모듈을 추가/삭제하면 Dockerfile 의 의존성 prefetch `COPY build.gradle.kts` 목록도 함께 갱신해야 캐싱이 유지됨.
```

- [ ] **Step 2: 커밋**

```bash
git add imstargg-backend/AGENTS.md
git commit -m "docs(backend): Dockerfile 모듈 목록 유지보수 메모 추가

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 4: (수동/선택) CI 엔드투엔드 검증

> ⚠️ 이 단계는 실제 ECR 푸시 + gitops values.yaml 커밋(외부 영향)을 일으킨다. 사용자 승인 후에만 실행한다. 자동 실행하지 말 것.

- [ ] **Step 1: core-api 배포 워크플로 수동 트리거**

브랜치를 원격에 푸시한 뒤:
```bash
gh workflow run deploy.yaml -f app=core-api --ref dev
gh run watch
```
Expected: `build-and-push` → `update-image-tags` 성공.

- [ ] **Step 2: ECR manifest 아키텍처 확인**

Run:
```bash
aws ecr batch-get-image --repository-name imstargg/core-api \
  --image-ids imageTag=<위에서 푸시된 IMAGE_TAG> \
  --query 'images[].imageManifest' --output text | head
```
Expected: manifest 의 platform 이 `linux/arm64`. (unknown/unknown 항목이 없어야 함 — `provenance: false` 효과.)

- [ ] **Step 3: 2차 빌드 캐시 확인 (선택)**

같은 앱을 소스 변경 없이 다시 트리거 → 로그에서 의존성 prefetch 레이어가 `CACHED` 로 표시되는지 확인.

---

## Self-Review (작성자 체크)

- **Spec coverage:**
  - Java 빌드 in-image → Task 1 Dockerfile(builder 스테이지) + Task 2(build-jar 잡 제거). ✅
  - 레이어드 JAR(jarmode=tools) → Task 1 Step 2. ✅
  - arm64 빌드 / amd64 후속 → Task 2(`platforms: linux/arm64`), 스펙 §7. ✅
  - bootBuildImage vs Dockerfile, 단일 vs 개별 → 스펙 §3.1/§3.2에서 결론, Task 1·2에서 구현. ✅
  - prefetch 캐싱 + GRADLE_USER_HOME 실디렉터리 → Task 1 Dockerfile. ✅
  - `.dockerignore` → Task 1 Step 1. ✅
  - 기존 4개 Dockerfile 삭제 → Task 2 Step 2. ✅
  - jar 탐색 경로 한정(버그 수정) → Task 1 Step 2 `MODULE_PATH` 변환. ✅
- **Placeholder scan:** 모든 step 에 실제 내용/명령 포함. 플레이스홀더 없음. ✅
- **Type/이름 일관성:** `GRADLE_MODULE`(빌드 인자), `:core:${app}`(gradle 경로), `application.jar`(추출/엔트리포인트), `extracted/`(추출 대상)이 Dockerfile·deploy.yaml 전반에서 일치. ✅
```
