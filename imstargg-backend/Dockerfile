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
