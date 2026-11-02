# brawlstars-client RestClient 전환 + 기반 업그레이드 + 클라이언트 버그 수정 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Feign·Spring Cloud 를 걷어내 brawlstars-client 를 RestClient HTTP Service Client 로 바꾸고, 동시 호출을 클라이언트 배치 메서드로 모으며, DTO null 역직렬화 실패와 battlelog 404 오삭제를 고친다. 그 기반으로 Java 25·Boot 4.1.1·Kotlin 2.3.21 업그레이드와 kotlin-logging 전환을 함께 한다.

**Architecture:** `support:http-client`(HC5 고정·재시도 끔·풀·타임아웃·호출 로깅) 위에 `@ImportHttpServices(group = "brawlstars")` 그룹을 두고, `BrawlStarsClient` 가 `BrawlStarsApi`(`@HttpExchange`) 프록시를 감싼다. 인터셉터는 바깥부터 레이트리밋 → 로깅 → 키 순환. 예외는 RestClient 예외를 그대로 올리고 호출부가 잡는다. 통계 축적의 battlelog 404 는 `stats_player.status` 로 표시해 뽑기에서 뺀다.

**Tech Stack:** Kotlin 2.3.21, Java 25, Spring Boot 4.1.1(Spring Framework 7), Apache HttpClient 5.6, resilience4j 2.4.0, kotlin-logging 8.0.4, JUnit 5·AssertJ·MockK/springmockk, Testcontainers Postgres.

**Spec:** `docs/superpowers/specs/2026-11-02-brawlstars-client-restclient-migration-design.md`

## Global Constraints

- 모든 경로·명령은 저장소 루트의 `imstargg-backend/` 기준이다(문서는 루트 기준으로 따로 적는다). 명령은 `cd imstargg-backend` 후 실행한다.
- 버전: Kotlin `2.3.21`, Java `25`, Spring Boot `4.1.1`, Gradle wrapper `9.7.1`, `resilience4jVersion=2.4.0`, `kotlinLoggingVersion=8.0.4`. Spring Cloud 는 Task 1 에서 `2025.1.3` 으로 올렸다가 Task 4 에서 BOM 째 제거한다. `springMockkVersion=5.0.1` 은 내리지 않는다.
- HTTP: `spring.http.clients.imperative.factory: http-components`, `connect-timeout: 3s`, `read-timeout: 10s`, 풀 전체 `100`·라우트당 `50`, 커넥션 임대 대기 `10s`, HC5 자체 재시도 끔. 클라이언트는 재시도하지 않는다.
- 레이트리밋: resilience4j 제한기 이름 `brawlstars`, 설정값(기본 1/s·대기 600s, prod 10/s)은 바꾸지 않는다. `@RateLimiter` 애노테이션은 쓰지 않는다.
- 배치 동시 실행 상한 `app.client.brawlstars.batch-concurrency: 5`.
- 클라이언트 예외 계층을 만들지 않는다 — `HttpClientErrorException`·`HttpServerErrorException`(상태별 하위 타입)·`RestClientResponseException`·`ResourceAccessException`·`RestClientException`·`RequestNotPermitted` 를 그대로 쓴다.
- 로깅: 운영 코드는 파일 최상위 `private val log = KotlinLogging.logger {}`, 메시지는 람다 문자열 템플릿, 예외는 `log.warn(ex) { "…" }`. `org.slf4j` 는 운영 코드에서 import 하지 않는다. 한글이 바로 붙는 자리는 `${n}건` 처럼 중괄호를 쓴다.
- 테스트: `org.junit.jupiter.api.Test`, AssertJ, 백틱 한글 메서드 이름, `// given` `// when` `// then` 주석. `CoreWorkerContextTest`·`CoreBatchContextTest` 를 상속하는 클래스에는 `@Tag("context")` 를 다시 달지 않는다. 테스트는 Redis 에 직접 기대지 않는다(CI 는 Testcontainers Postgres 만 있다). 로컬 실행에 Docker 가 필요하다.
- 공용 헬퍼로 묶기보다 테스트별 로컬 헬퍼 중복을 둔다(예: `createPlayerResponse`).
- 모든 Task 의 마지막 검증은 `./gradlew build`(ktlint 포함, 한 줄 140자) 통과다. 커밋 메시지는 Conventional Commits, 본문 끝에 `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- 스펙 §3 의 5개 커밋 단계는 이 계획에서 8개 커밋으로 나뉜다: 커밋 1=Task 1, 2=Task 2, 3=Task 3·4·5, 4=Task 6, 5=Task 7·8. 각 커밋은 단독으로 빌드가 통과한다.

## Review Focus

1. **DEBUG 레벨 `ErrorType`(예: `NOT_FOUND`, `PLAYER_DELETED`)** — kotlin-logging 전환 뒤에도 운영(INFO)에서 사라지지 않고 INFO 로 남아야 한다. → Task 2 `DEBUG 레벨 ErrorType 은 INFO 로 기록한다`.
2. **배치 결과에 레이트리밋 대기 초과(`RequestNotPermitted`)가 섞인 틱** — 그 플레이어만 error 로그를 남기고 나머지 플레이어는 집계돼야 한다. → Task 5 `레이트리밋 허가를 못 받은 플레이어가 있어도 나머지 플레이어는 집계한다`.
3. **뽑을 `ACTIVE` 플레이어가 하나도 없는 틱**(모두 `BATTLE_LOG_UNAVAILABLE`) — API 를 부르지 않고 조용히 끝나야 한다. → Task 7 `뽑을 ACTIVE 플레이어가 없으면 API 를 부르지 않는다`.
4. **동기화 배치가 표시된 태그를 새 전투에서 다시 만남** — 행을 새로 넣거나 상태를 `ACTIVE` 로 되돌리지 않아야 한다. → Task 7 `표시된 태그를 다시 만나도 새로 넣지 않고 상태를 유지한다`.
5. **DB 에 아직 없는 플레이어의 첫 갱신에서 battlelog 만 404** — 플레이어 행이 만들어지고 갱신이 `COMPLETED` 로 끝나야 한다. → Task 8 `DB 에 없는 플레이어도 battlelog 만 404 면 플레이어를 만들고 갱신을 완료한다`.

---

### Task 0: 작업 브랜치 준비

- [ ] **Step 1:** `superpowers:using-git-worktrees` 로 `origin/develop` 에서 `refactor/brawlstars-client-restclient` 브랜치의 워크트리를 만든다(`git fetch origin` 먼저).
- [ ] **Step 2:** 설계·계획 문서 커밋을 가져온다: `git cherry-pick af60043f..claude/vm-log-client-issue-fa1309` (`df3d124f` 부터 이 계획의 마지막 docs 커밋까지). 로컬 develop 에만 있는 `af60043f`(mcp 추가) 자체는 싣지 않는다.
- [ ] **Step 3:** 기준선 확인: `./gradlew build` → `BUILD SUCCESSFUL`.
- [ ] **Step 4:** 기준선 기록: `./gradlew :client:brawlstars-client:developTest` 를 돌려 수집된 테스트 수(`build/test-results/developTest/*.xml` 의 `tests`, 없으면 0)를 적어 둔다 — Task 1 의 수정 효과를 비교한다.

---

### Task 1: 버전 업그레이드 (스펙 커밋 1, §4.1)

**Files:**
- Modify: `gradle.properties:11,12,15,17`, `settings.gradle.kts:19-23`, `build.gradle.kts:50-54`(BOM 순서)·`:86-90`(Test 태스크), `gradle/wrapper/*`(wrapper 태스크가 바꾼다), `Dockerfile:4,38`
- Modify: `infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/config/CoreDataSourceConfig.kt:15`, `.../config/LockConfig.kt:16`
- Modify: `support/monitoring/src/main/resources/monitoring.yml:17-23`, `core/core-api/src/test/resources/application.yml:30-37`
- Modify: `../.github/workflows/pr-test.yml:44,48`, `AGENTS.md:41`

**Interfaces:** 없음(동작 변경 없음).

- [ ] **Step 1: 버전 값을 바꾼다** — `gradle.properties`: `kotlinVersion=2.3.21`, `javaVersion=25`, `springBootVersion=4.1.1`, `springCloudVersion=2025.1.3`.
- [ ] **Step 2: Spring Cloud BOM 을 awspring BOM 뒤로 옮긴다** — `build.gradle.kts` 의 `dependencyManagement.imports` 에서 `mavenBom("org.springframework.cloud:spring-cloud-dependencies:…")` 줄을 `mavenBom("io.awspring.cloud:spring-cloud-aws-dependencies:…")` 줄 다음으로 옮긴다(awspring BOM 이 commons 5.0.0 을 중첩 import 해 뒤의 BOM 이 이긴다).
- [ ] **Step 3: commons 버전을 확인한다**

Run: `./gradlew :core:core-worker:dependencyInsight --dependency spring-cloud-commons --configuration runtimeClasspath` 와 `:core:core-batch:` 같은 명령
Expected: 두 모듈 모두 `org.springframework.cloud:spring-cloud-commons:5.0.3`. 5.0.0 이 나오면 Step 2 가 적용되지 않은 것이다.

- [ ] **Step 4: settings 위임을 바꾼다** — `settings.gradle.kts:19-23` 의 `val xxx: String by settings` 5개를 `val xxx = providers.gradleProperty("xxx").get()` 로 바꾼다(`kotlinVersion`, `springBootVersion`, `springDependencyManagementVersion`, `ktlintVersion`, `kspVersion`).
- [ ] **Step 5: wrapper 를 올린다** — `./gradlew wrapper --gradle-version 9.7.1` 를 두 번 실행한다(두 번째가 9.7.1 기준으로 jar·스크립트를 다시 만든다). `gradle-wrapper.properties` 의 URL 이 `gradle-9.7.1-bin.zip` 인지 확인한다.
- [ ] **Step 6: developTest 태그 겹침을 고친다** — `build.gradle.kts:86-90` 의 `tasks.withType<Test> { useJUnitPlatform { excludeTags("develop") } }` 를 아래로 바꾼다(noguesstoday-backend `build.gradle.kts:80-90` 과 같음).

```kotlin
    // develop 태그는 CI 에서 돌지 않는 수동 테스트라 기본적으로 모든 Test 태스크에서 제외한다.
    // 단, developTest 는 그 태그를 골라 실행하는 태스크라 여기서 빼야 한다 — 함께 걸리면
    // useJUnitPlatform 옵션이 누적되어 "tag 'develop' is both included and excluded" 로 0건이 수집된다.
    tasks
        .withType<Test>()
        .matching { it.name != "developTest" }
        .configureEach {
            useJUnitPlatform {
                excludeTags("develop")
            }
        }
```

- [ ] **Step 7: 이미지·CI·문서** — `Dockerfile:4` `eclipse-temurin:25-jdk-jammy`, `:38` `eclipse-temurin:25-jre-jammy`. `pr-test.yml:44` `Set up JDK 25`, `:48` `java-version: '25'`. `AGENTS.md:41` 의 `Java 21` → `Java 25`.
- [ ] **Step 8: 죽은 otlp 키를 바꾼다** — `monitoring.yml` 과 `core-api` 테스트 `application.yml` 의 `management.otlp.tracing.export.enabled: false` 를 `management.tracing.export.otlp.enabled: false` 로 옮긴다(`management.otlp.metrics.export.enabled` 는 그대로 둔다).
- [ ] **Step 9: 불필요한 `!!` 를 지운다** — `CoreDataSourceConfig.kt:15`, `LockConfig.kt:16` 의 `.build()!!` → `.build()`. 그다음 `./gradlew build --rerun-tasks 2>&1 | grep '^w:'` 로 Kotlin 2.3 경고를 모아, 불필요한 `!!`·`?.` 경고만 같은 방식으로 지운다.
- [ ] **Step 10: 전체 빌드**

Run: `./gradlew build`
Expected: `BUILD SUCCESSFUL` (core-worker·core-batch 컨텍스트 테스트 포함 — commons 가 5.0.0 이면 `CompatibilityNotMetException` 으로 실패한다).

- [ ] **Step 11: developTest 수집 확인**

Run: `./gradlew :client:brawlstars-client:developTest`
Expected: 수집된 테스트 수 > 0 이고 "both included and excluded" 경고가 없다(키가 없어 실패해도 수집 확인으로 충분).

- [ ] **Step 12: 커밋**

```bash
git add -A
git commit -m "chore(backend): Java 25·Spring Boot 4.1.1·Kotlin 2.3.21·Gradle 9.7.1 로 올린다"
```

---

### Task 2: kotlin-logging 전환 (스펙 커밋 2, §4.2)

**Files:**
- Modify: `gradle.properties`(External dependency versions 에 `kotlinLoggingVersion=8.0.4`), `support/logging/build.gradle.kts`
- Modify(`implementation(project(":support:logging"))` 추가): `core/core-admin/build.gradle.kts`, `infrastructure/{db-core,redis-core,aws-sqs,aws-s3}/build.gradle.kts`, `client/brawlstars-client/build.gradle.kts`
- Modify(패키지 `com.example.template.core.error` → `com.imstargg.core.error`, KLogger): `core/core-api/src/main/kotlin/com/imstargg/core/error/ErrorLogger.kt`, `core/core-api/src/test/kotlin/com/imstargg/core/error/ErrorLoggerTest.kt`
- Modify(LoggerFactory 47개):
  - core-api(`core/core-api/src/main/kotlin/com/imstargg/core/`): `api/error/ApiExceptionHandler.kt`, `application/renewal/RenewalService.kt`, `config/AsyncExceptionHandler.kt`, `config/RedisFallbackCacheErrorHandler.kt`, `infrastructure/sqs/SqsRenewalMessageProducer.kt`
  - core-admin: `core/core-admin/src/main/kotlin/com/imstargg/core/admin/application/ImageStorageServiceExtensions.kt`
  - core-batch(`core/core-batch/src/main/kotlin/com/imstargg/core/batch/`): `job/HealthCheckJobConfig.kt`, `job/PartitionManageJobConfig.kt`, `job/brawlstars/BattleEventSyncItemWriter.kt`, `job/brawlstars/BrawlStarsApiTasklet.kt`, `job/brawlstars/task/{BattleEventModeSyncTask,BattleEventRotationSyncTask,BrawlStarsApiSpecTrackTask,BrawlerRankingCollectTask,BrawlerSyncTask,ClubRankingCollectTask,PlayerRankingCollectTask}.kt`, `job/operation/FeedbackDailyDigestJobConfig.kt`, `job/renewal/task/PlayerRankingRenewalTask.kt`, `job/statistics/{battlerank/BrawlerBattleRankStatsTask,battleresult/BrawlerBattleResultStatsTask,enemybattleresult/BrawlerEnemyBattleResultStatsTask,ownership/BrawlerItemOwnershipStatsTask,pairbattlerank/BrawlerPairBattleRankStatsTask,pairbattleresult/BrawlerPairBattleResultStatsTask,playersync/StatsPlayerSyncFromRankingTask}.kt`, `support/alert/{DiscordAlertManager,NoOpsAlertManager}.kt`, `support/coreapi/CoreApiClient.kt`, `support/parameter/{DateJobParameter,YearMonthJobParameter}.kt`
  - core-worker(`core/core-worker/src/main/kotlin/com/imstargg/core/worker/`): `application/club/{ClubDeactivator,ClubRenewalService}.kt`, `application/maintenance/BrawlStarsApiMaintenanceAutoRegistrar.kt`, `application/player/{PlayerDeactivator,PlayerRefreshService,PlayerRenewalService,PlayerUpdater}.kt`, `application/stats/StatsBattleAccumulationService.kt`, `config/AsyncExceptionHandler.kt`, `consumer/{ClubRenewalMessageConsumer,PlayerRenewalMessageConsumer}.kt`
  - infrastructure/client: `infrastructure/aws-s3/.../LocalAwsS3Initializer.kt`, `infrastructure/aws-sqs/.../LocalAwsSqsInitializer.kt`, `infrastructure/db-core/.../lock/PostgresAdvisoryLockTemplate.kt`, `infrastructure/redis-core/.../statistics/player/PlayerBrawlerStatsCacheStore.kt`, `client/brawlstars-client/.../BrawlStarsApiKeyInterceptor.kt`
- Modify: `AGENTS.md`(로깅 규칙·의존 관계)

**Interfaces:**
- Produces: `support:logging` 이 `api("io.github.oshai:kotlin-logging-jvm:${property("kotlinLoggingVersion")}")` 를 노출한다. `com.imstargg.core.error.ErrorLogger.of(log: io.github.oshai.kotlinlogging.KLogger): ErrorLogger`.

- [ ] **Step 1: ErrorLoggerTest 를 KLogger 기준으로 바꾸고 테스트를 더한다** — 패키지를 `com.imstargg.core.error` 로 바꾸고, 기존 3개는 지역 헬퍼 방식(`val (log, appender) = loggerWithAppender()`)을 그대로 두고 `ErrorLogger.of(log.toKLogger())`(import `io.github.oshai.kotlinlogging.slf4j.toKLogger`)로 바꾼다. 같은 방식으로 추가:

```kotlin
@Test
fun `DEBUG 레벨 ErrorType 은 INFO 로 기록한다`() {
    // given
    val (log, appender) = loggerWithAppender()
    val sut = ErrorLogger.of(log.toKLogger())
    // when
    sut.log(CoreException(ErrorType.NOT_FOUND))
    sut.log(IllegalArgumentException("bad"), ErrorType.INVALID_REQUEST)
    // then
    assertThat(appender.list).extracting<Level> { it.level }.containsExactly(Level.INFO, Level.INFO)
    assertThat(appender.list).allSatisfy { assertThat(it.throwableProxy).isNull() }
}

@Test
fun `ErrorType 과 함께 남긴 예외는 타입·메시지·원인을 남긴다`() {
    // given
    val (log, appender) = loggerWithAppender()
    val sut = ErrorLogger.of(log.toKLogger())
    // when
    sut.log(IllegalStateException("maintenance"), ErrorType.BRAWLSTARS_API_UNDER_MAINTENANCE)
    // then
    assertThat(appender.list.single().formattedMessage)
        .isEqualTo("[IllegalStateException] [BRAWLSTARS_API_UNDER_MAINTENANCE]: ${ErrorType.BRAWLSTARS_API_UNDER_MAINTENANCE.message} >>> maintenance")
}
```

- [ ] **Step 2: 실패 확인** — `./gradlew :core:core-api:test --tests 'com.imstargg.core.error.ErrorLoggerTest'` → 컴파일 실패(`ErrorLogger.of` 가 slf4j `Logger` 를 받음).
- [ ] **Step 3: 의존을 넣는다** — `kotlinLoggingVersion=8.0.4` 추가(`gradle.properties` 는 파일 끝에 개행이 없다 — shell `>>` 로 덧붙이지 말고 Edit 로 `springCloudAwsVersion=4.0.0` 다음 새 줄에 넣는다). `support/logging/build.gradle.kts` 첫 줄에 `api("io.github.oshai:kotlin-logging-jvm:${property("kotlinLoggingVersion")}")`(주석: 로그를 남기는 모듈은 이 모듈에 의존해 KotlinLogging 을 쓴다). Files 의 6개 모듈에 `implementation(project(":support:logging"))`.
- [ ] **Step 4: ErrorLogger 를 KLogger 로 바꾼다** — 패키지 `com.imstargg.core.error`, `class ErrorLogger private constructor(private val log: KLogger)`, `fun of(log: KLogger)`. `{}` 템플릿 상수 2개를 지우고 메시지를 람다로 만든다. 레벨 분기는 지금과 같다: `ERROR → log.error(ex) {…}`, `WARN → log.warn(ex) {…}`, 그 밖 → `log.info {…}`(예외 없음). 메시지 문자열은 기존 slf4j 출력과 같게(`"[${ex.javaClass.simpleName}]: ${ex.message}"`, `"[${ex.javaClass.simpleName}] [$errorType]: ${errorType.message} >>> ${ex.message}"`). 호출부 `ApiExceptionHandler`·`AsyncExceptionHandler` 는 import 경로를 고치고 파일 최상위 `log` 를 `ErrorLogger.of(log)` 로 넘긴다.
- [ ] **Step 5: 통과 확인** — Step 2 명령 → PASS.
- [ ] **Step 6: 47개 파일을 바꾼다** — 규칙:
  - `import org.slf4j.LoggerFactory` → `import io.github.oshai.kotlinlogging.KotlinLogging`(ktlint 알파벳 순서).
  - 선언은 import 다음, 클래스 KDoc·애노테이션 앞의 `private val log = KotlinLogging.logger {}`. 클래스 프로퍼티 `log` 는 지우고, companion 의 `log` 줄만 지운다(companion 에 그것뿐이면 블록째 지운다).
  - `log.info("a={} b={}", x, y.z)` → `log.info { "a=$x b=${y.z}" }`. 마지막 인자가 예외면 `log.warn(ex) { "…" }`. 예외를 넘기지 않던 호출은 넘기지 않는다. `runCatching{}.onFailure { log.warn("…{}", k, it) }` → `.onFailure { log.warn(it) { "…$k" } }`.
  - `ImageStorageServiceExtensions.kt` 는 `KotlinLogging.logger("com.imstargg.core.admin.application.ImageStorageCleanup")` 로 이름을 유지한다.
  - `ApiExceptionHandler` 가 상속한 `logger.debug("handleExceptionInternal", ex)`(commons-logging)는 그대로 둔다.
- [ ] **Step 7: slf4j 가 남지 않았는지 확인**

Run: `grep -rl "org.slf4j" --include='*.kt' . | grep /src/main/`
Expected: 출력 없음.

- [ ] **Step 8: AGENTS.md** — support 트리의 `logging/` 설명을 `로깅 — kotlin-logging(api 로 노출)·Micrometer Tracing·Logback` 으로, 의존 관계에 `db-core`·`redis-core`·`aws-sqs`·`aws-s3`·`brawlstars-client`·`core-admin` → `support:logging` 을 반영한다. 코드 규칙에 bullet 하나: "로깅은 kotlin-logging — 파일 최상위 `private val log = KotlinLogging.logger {}`, 람다 메시지(`log.info { "date=$date 대상 ${targets.size}건" }`), 예외는 `log.warn(ex) { "…" }`, 운영 코드는 slf4j `LoggerFactory`·`Logger` 를 import 하지 않는다(테스트는 logback `ListAppender` 용으로 쓸 수 있다), 로거를 넘겨받는 곳은 `KLogger`(`ErrorLogger.of`), 한글이 바로 붙으면 `${n}건`, 이름을 지정한 로거는 이름을 유지한다".
- [ ] **Step 9: 전체 빌드** — `./gradlew build` → `BUILD SUCCESSFUL`(`CoreAdminContextTest` 포함 — support:logging 의 otel bridge 가 새로 들어간 core-admin 컨텍스트가 뜨는지 확인).
- [ ] **Step 10: 커밋** — `git add -A && git commit -m "refactor(backend): 로깅을 kotlin-logging 으로 바꾼다"`

---

### Task 3: `support:http-client` 모듈 (스펙 커밋 3-1, §4.3)

**Files:**
- Create: `support/http-client/build.gradle.kts`, `support/http-client/src/main/kotlin/com/imstargg/support/http/HttpClientConfig.kt`, `.../ApiLoggingInterceptor.kt`, `support/http-client/src/main/resources/http-client.yml`
- Test: `support/http-client/src/test/kotlin/com/imstargg/support/http/ApiLoggingInterceptorTest.kt`, `.../HttpClientConfigTest.kt`
- Modify: `settings.gradle.kts`(`"client:brawlstars-client",` 다음 줄에 `"support:http-client",`), `Dockerfile`(`COPY client/brawlstars-client/…` 다음 줄에 `COPY support/http-client/build.gradle.kts       ./support/http-client/`)

**Interfaces:**
- Produces: `class ApiLoggingInterceptor : ClientHttpRequestInterceptor, HttpRequestValues.Processor` (인자 없는 생성자). `@Configuration class HttpClientConfig` 의 `@Bean fun httpComponentsCustomizer(): ClientHttpRequestFactoryBuilderCustomizer<HttpComponentsClientHttpRequestFactoryBuilder>`, `companion object { const val MAX_CONN_TOTAL = 100; const val MAX_CONN_PER_ROUTE = 50; val CONNECTION_REQUEST_TIMEOUT: Timeout = Timeout.ofSeconds(10) }`. 클래스패스 리소스 `http-client.yml`.

- [ ] **Step 1: 모듈 골격** — `build.gradle.kts`: `implementation("org.springframework:spring-web")`, `implementation(project(":support:logging"))`, `implementation("org.springframework.boot:spring-boot-restclient")`(주석: 없으면 `spring.http.clients.*` 가 그룹에 조용히 적용되지 않는다), `implementation("org.springframework.boot:spring-boot-http-client")`, `implementation("org.apache.httpcomponents.client5:httpclient5")`. settings·Dockerfile 등록.
- [ ] **Step 2: 실패하는 테스트를 쓴다** — `ApiLoggingInterceptorTest`(noguesstoday-backend `support/http-client/src/test/.../ApiLoggingInterceptorTest.kt` 를 옮기되 `org.junit.jupiter.api.Test` 사용). 하네스: `SampleApi`(`@GetExchange("/items") fun getItems(@RequestParam id: Int): String`), `SampleApi` 로거에 logback `ListAppender`, `RestClient.builder().baseUrl("http://localhost").requestInterceptor(sut)` + `MockRestServiceServer.bindTo(builder)`, 프록시는 `HttpServiceProxyFactory.builderFor(RestClientAdapter.create(builder.build())).httpRequestValuesProcessor(sut)`.
  - `프록시로 부른 호출은 호출한 Api 인터페이스의 로거로 Api#메서드 를 붙여 남긴다` — 메시지 1개, `startsWith("[SampleApi#getItems] GET /items?id=1 -> 200")`.
  - `프로세서로 등록하지 않으면 호출이 실패한다` — `IllegalStateException`, 메시지에 `httpRequestValuesProcessor`.
  - `오류 상태 응답도 INFO 로 남긴다` — `withStatus(NOT_FOUND)` → `HttpClientErrorException.NotFound` 가 던져지고 로그 1개는 `Level.INFO`, `-> 404` 포함.
  - `전송 예외는 WARN 으로 남기고 예외를 그대로 올린다` — `withException(SocketTimeoutException())` → `ResourceAccessException`(root cause `SocketTimeoutException`), 로그는 `Level.WARN`, `-> SocketTimeoutException` 포함.
  - `헤더는 남기지 않는다` — `Authorization: Bearer secret` 을 단 호출의 로그에 `secret` 이 없다.

  `HttpClientConfigTest` — `private fun factory() = HttpClientConfig().httpComponentsCustomizer().customize(ClientHttpRequestFactoryBuilder.httpComponents()).build(HttpClientSettings.defaults()) as HttpComponentsClientHttpRequestFactory`:
  - `커넥션 임대 대기 상한은 10초다` — `(factory().httpClient as Configurable).config.connectionRequestTimeout` == `Timeout.ofSeconds(10)`.
  - `503 응답에 HC5 가 재시도하지 않아 요청은 한 번만 간다` — `com.sun.net.httpserver.HttpServer` 가 503 을 주며 횟수를 센다 → `HttpServerErrorException.ServiceUnavailable`, 횟수 1.
  - `한 라우트에 5개를 넘는 동시 요청이 커넥션 대기 없이 서버에 닿는다` — 서버 `setExecutor(Executors.newCachedThreadPool())`, 핸들러는 진행 중 수를 올리고 latch 대기, 요청 6건을 가상 스레드로 보내 `Awaitility.await().atMost(5s)` 안에 진행 중 6 을 확인한 뒤 latch 해제.
  - `자동 구성 빌더에 커스터마이저가 적용된다` — `ApplicationContextRunner().withConfiguration(AutoConfigurations.of(HttpClientAutoConfiguration::class.java, ImperativeHttpClientAutoConfiguration::class.java)).withUserConfiguration(HttpClientConfig::class.java).withPropertyValues("spring.http.clients.imperative.factory=http-components")` 의 `ClientHttpRequestFactoryBuilder` 빈으로 만든 팩토리의 임대 대기가 10s.
- [ ] **Step 3: 실패 확인** — `./gradlew :support:http-client:test` → 컴파일 실패.
- [ ] **Step 4: 구현** — `ApiLoggingInterceptor` 는 noguesstoday 파일을 패키지만 바꿔 옮긴다(로거 `KotlinLogging.logger(method.declaringClass.name)`, 응답이면 상태와 무관하게 INFO, `catch (ex: Exception)` 에서 WARN 후 재던짐; KDoc 은 "`BrawlStarsApiKeyInterceptor` 를 이 인터셉터보다 뒤에 등록한다" 로 고친다). `HttpClientConfig` 는 **반환된 빌더 체인을 돌려준다**(`with*` 는 새 불변 빌더): `builder.withHttpClientCustomizer { it.disableAutomaticRetries() }.withConnectionManagerCustomizer { it.setMaxConnTotal(MAX_CONN_TOTAL).setMaxConnPerRoute(MAX_CONN_PER_ROUTE) }.withDefaultRequestConfigCustomizer { it.setConnectionRequestTimeout(CONNECTION_REQUEST_TIMEOUT) }`. KDoc: 재시도를 끄는 이유, 풀 근거(동시 호출 상한 26), TTL·`setValidateAfterInactivity` 를 쓰지 않는 이유(Boot 의 ConnectionConfig 를 덮음). `http-client.yml`: `spring.http.clients.imperative.factory: http-components`, `connect-timeout: 3s`, `read-timeout: 10s` + 주석(앱이 import, 그룹별 오버라이드는 `spring.http.serviceclient.<group>.*`).
- [ ] **Step 5: 통과 확인** — `./gradlew :support:http-client:test` → PASS.
- [ ] **Step 6: 전체 빌드** — `./gradlew build` → `BUILD SUCCESSFUL`.
- [ ] **Step 7: 커밋** — `git add -A && git commit -m "feat(support): HTTP 클라이언트 공통 설정 모듈(support:http-client)을 추가한다"`

---

### Task 4: brawlstars-client 를 HTTP Service Client 로 전환 (스펙 커밋 3-2, §4.4·§4.5)

**Files:**
- Modify: `gradle.properties`(`springCloudVersion` 줄 삭제, `resilience4jVersion=2.4.0` 추가), `build.gradle.kts`(`spring-cloud-dependencies` BOM 줄 삭제)
- Modify: `client/brawlstars-client/build.gradle.kts`(전체 교체), `.../BrawlStarsApi.kt`, `.../BrawlStarsClient.kt`, `.../BrawlStarsClientConfig.kt`, `.../BrawlStarsApiKeyInterceptor.kt`, `client/brawlstars-client/src/main/resources/brawlstars-client.yml`, `client/brawlstars-client/src/test/resources/application.yml`, `client/brawlstars-client/src/test/kotlin/com/imstargg/BrawlStarsClientRealTest.kt:8-18`, `client/brawlstars-client/README.md`
- Create: `client/brawlstars-client/src/main/kotlin/com/imstargg/client/brawlstars/BrawlStarsRateLimitInterceptor.kt`
- Delete: `.../BrawlStarsApiConfig.kt`, `.../BrawlStarsApiException.kt`, `.../request/PagingParam.kt`
- Test: `client/brawlstars-client/src/test/kotlin/com/imstargg/client/brawlstars/{BrawlStarsClientTest,BrawlStarsApiKeyInterceptorTest,BrawlStarsRateLimitInterceptorTest}.kt`, `client/brawlstars-client/src/test/resources/brawlstars/{battlelog.json,player.json}`
- Modify(core-worker): `build.gradle.kts`(`testImplementation("io.github.openfeign:feign-core")` → `testImplementation("io.github.resilience4j:resilience4j-ratelimiter:${property("resilience4jVersion")}")`), `src/main/resources/application.yml`(import 목록에서 `brawlstars-client.yml` 앞에 `- http-client.yml`), `src/test/resources/application.yml`(`spring.config.import: [http-client.yml]` 만), `application/stats/StatsBattleAccumulationService.kt`, `application/player/{PlayerRenewalService,PlayerRefreshService}.kt`, `application/club/ClubRenewalService.kt`
- Test(core-worker): `application/stats/StatsBattleAccumulationServiceTest.kt`(예외 헬퍼), `application/player/PlayerRenewalServiceTest.kt`, Create `application/player/PlayerRefreshServiceTest.kt`, `application/club/ClubRenewalServiceTest.kt`, `src/test/kotlin/com/imstargg/BrawlStarsClientHttpWiringContextTest.kt`
- Modify(core-batch): `build.gradle.kts`(feign-core 테스트 의존 삭제), `src/main/resources/application.yml`(import 에 `- http-client.yml` 을 `brawlstars-client.yml` 앞에), `src/test/resources/application.yml`(`spring.config.import: [http-client.yml]` 만), `job/brawlstars/BrawlStarsApiTasklet.kt`, Test `job/brawlstars/BrawlStarsApiTaskletTest.kt`
- Modify: `AGENTS.md`(brawlstars-client 설명·support 트리·의존·클라이언트 규칙)

**Interfaces:**
- Consumes: Task 3 의 `ApiLoggingInterceptor`, `http-client.yml`.
- Produces:
  - `@HttpExchange internal interface BrawlStarsApi` — 메서드 이름 그대로, `@GetExchange("<기존 경로>")`, paging 인자 없음: `getLogOfRecentBattlesForAPlayer(@PathVariable playerTag: String): ListResponse<BattleResponse>`, `getPlayerInformation(playerTag): PlayerResponse`, `getListClubMembers(clubTag): ListResponse<ClubMemberResponse>`, `getClubInformation(clubTag): ClubResponse`, `getClubRankingsForACountryOrGlobalRankings(countryCode): ListResponse<ClubRankingResponse>`, `getBrawlerRankingsForACountryOrGlobalRankings(countryCode, brawlerId: Long): ListResponse<PlayerRankingResponse>`, `getPlayerRankingsForACountryOrGlobalRankings(countryCode): ListResponse<PlayerRankingResponse>`, `getListOfAvailableBrawlers(): ListResponse<BrawlerResponse>`, `getInformationAboutABrawler(brawlerId: Long): BrawlerResponse`, `getEventRotation(): List<ScheduledEventResponse>`, `getGameModes(): ListResponse<EventTypeResponse>`, `getApiSpec(): String`.
  - `class BrawlStarsClient internal constructor(private val brawlStarsApi: BrawlStarsApi)` — 공개 메서드 10개의 시그니처는 지금과 같다(`getPlayerRecentBattles`, `getPlayerInformation`, `getBrawlers`, `getGameModes`, `getEventRotation`, `getPlayerRanking`, `getBrawlerRanking(country, brawlerId: Long)`, `getClub`, `getClubRanking`, `getApiSpec`).
  - `internal class BrawlStarsApiKeyInterceptor(private val keys: List<String>) : ClientHttpRequestInterceptor`, `internal class BrawlStarsRateLimitInterceptor(private val rateLimiter: RateLimiter) : ClientHttpRequestInterceptor`.
  - `@Configuration @EnableConfigurationProperties(BrawlStarsClientProperties::class) @ImportHttpServices(group = BrawlStarsClientConfig.GROUP, types = [BrawlStarsApi::class]) internal class BrawlStarsClientConfig` — `companion object { const val GROUP = "brawlstars"; const val RATE_LIMITER_NAME = "brawlstars" }`, `@Bean fun brawlStarsGroupConfigurer(properties: BrawlStarsClientProperties, rateLimiterRegistry: RateLimiterRegistry): RestClientHttpServiceGroupConfigurer`, `@Bean fun brawlStarsClient(brawlStarsApi: BrawlStarsApi): BrawlStarsClient`.

- [ ] **Step 1: 클라이언트 단위 테스트를 쓴다** — `BrawlStarsClientTest` 하네스: `val builder = RestClient.builder().baseUrl("http://localhost")`, `val server = MockRestServiceServer.bindTo(builder).build()`, `val sut = BrawlStarsClient(HttpServiceProxyFactory.builderFor(RestClientAdapter.create(builder.build())).build().createClient(BrawlStarsApi::class.java))`. 응답 본문은 `withSuccess(ClassPathResource("brawlstars/battlelog.json"), MediaType.APPLICATION_JSON)` 처럼 리소스 파일로 준다(`battlelog.json` 에는 DTO 에 없는 필드 `"unknownField": 1` 을 하나 넣는다).
  - `태그의 # 을 %23 으로 인코딩해 battlelog 를 GET 한다` — `requestTo("http://localhost/v1/players/%2388J8Q9RPV/battlelog")`, `method(GET)`.
  - `battleTime 을 읽고 모르는 필드는 무시한다` — `"20261102T010203.000Z"` → `OffsetDateTime.of(2026, 11, 2, 1, 2, 3, 0, ZoneOffset.UTC)`.
  - `플레이어 응답의 3vs3Victories·soloVictories·duoVictories·isQualifiedFromChampionshipChallenge 를 이름대로 읽는다` — `player.json` 값과 각 프로퍼티 비교.
  - `API 명세는 application octet-stream 본문을 문자열로 받는다` — `requestTo("http://localhost/v1")`, `withSuccess("swagger: \"2.0\"", MediaType.APPLICATION_OCTET_STREAM)` → `"swagger: \"2.0\""`.
  - `브롤러 랭킹은 국가와 브롤러 ID 경로로 쿼리 없이 GET 한다` — `requestTo("http://localhost/v1/rankings/kr/brawlers/16000000")`.
  - `404 는 HttpClientErrorException NotFound 로 올린다`, `503 은 HttpServerErrorException ServiceUnavailable 로 올린다` — `catchThrowableOfType`.
  - `해석 실패는 RestClientException 이고 RestClientResponseException 이 아니다` — 본문 `"{"`.

  `BrawlStarsApiKeyInterceptorTest`(`MockClientHttpRequest`·`MockClientHttpResponse`): `키를 순서대로 돌려 Authorization Bearer 헤더로 붙인다`(키 `[k1, k2]` 로 3번 → `Bearer k1`, `Bearer k2`, `Bearer k1`), `키가 없으면 생성에 실패한다`(`IllegalArgumentException`).
  `BrawlStarsRateLimitInterceptorTest`(`RateLimiter.of("t", RateLimiterConfig.custom().limitForPeriod(2).limitRefreshPeriod(Duration.ofHours(1)).timeoutDuration(Duration.ZERO).build())`): `요청마다 허가를 하나씩 쓴다`(`metrics.availablePermissions` 2 → 1), `허가가 없으면 RequestNotPermitted 를 던지고 요청을 보내지 않는다`(세 번째 호출, execution 호출 횟수 2).
- [ ] **Step 2: 실패 확인** — `./gradlew :client:brawlstars-client:test` → 컴파일 실패.
- [ ] **Step 3: 빌드 설정** — `gradle.properties` 에서 `springCloudVersion` 삭제·`resilience4jVersion=2.4.0` 추가(Edit 로 새 줄에 — 파일 끝 개행 없음), 루트 BOM 줄 삭제. `client/brawlstars-client/build.gradle.kts` 를 `api("org.springframework:spring-web")`, `implementation(project(":support:http-client"))`, `implementation(project(":support:logging"))`, `implementation("org.springframework.boot:spring-boot-restclient")`, `implementation("io.github.resilience4j:resilience4j-spring-boot4:${property("resilience4jVersion")}")`, `implementation("io.github.resilience4j:resilience4j-ratelimiter:${property("resilience4jVersion")}")` 로 교체. worker·batch 의 feign-core 테스트 의존을 Files 대로 바꾼다.
- [ ] **Step 4: 클라이언트 구현**
  - `BrawlStarsApi`: Feign·`@RateLimiter`·PagingParam import 를 지우고 Interfaces 대로 바꾼다.
  - `BrawlStarsClient`: `execute` 와 FeignException import 를 지우고 각 메서드는 위임만 한다.
  - `BrawlStarsApiKeyInterceptor`: `intercept` 에서 `request.headers.set(HttpHeaders.AUTHORIZATION, "Bearer $key")`, 키 인덱스는 `Math.floorMod(keyIndex.getAndIncrement(), keys.size)`(int 넘침 시 음수 방지), `init` 의 `require(keys.isNotEmpty())` 유지.
  - `BrawlStarsRateLimitInterceptor`: `RateLimiter.waitForPermission(rateLimiter)` 후 `execution.execute(request, body)`.
  - `BrawlStarsClientConfig`: `groups.filterByName(GROUP).forEachGroup { _, clientBuilder, proxyFactoryBuilder -> }` 안에서 `ApiLoggingInterceptor()` 인스턴스 하나를 만들고 `clientBuilder.baseUrl(properties.url).requestInterceptor(BrawlStarsRateLimitInterceptor(rateLimiterRegistry.rateLimiter(RATE_LIMITER_NAME))).requestInterceptor(apiLogging).requestInterceptor(BrawlStarsApiKeyInterceptor(properties.apiKeys))`, `proxyFactoryBuilder.httpRequestValuesProcessor(apiLogging)`. **`clientBuilder.requestFactory(…)` 는 부르지 않는다**(자동 구성 팩토리·풀·타임아웃이 덮인다).
  - 삭제 3개 파일.
- [ ] **Step 5: 설정 파일**
  - `brawlstars-client.yml`: `spring.cloud.openfeign.*` 블록 삭제. prod 문서에 `logging.level.com.imstargg.client.brawlstars.BrawlStarsApi: WARN`.
  - 클라이언트 테스트 `application.yml`: Feign 블록 삭제. 첫 문서는 `spring.config.import: [http-client.yml, brawlstars-client.yml]`, 제한기 테스트 값(`resilience4j.ratelimiter.instances.brawlstars`: `limit-for-period: 10`, `limit-refresh-period: 1s`, `timeout-duration: 60s`)은 import 보다 우선하도록 `---` 뒤 두 번째 문서에 둔다(이름 `brawlstars-api` → `brawlstars`).
  - worker·batch main `application.yml` import 에 `http-client.yml`, 테스트 `application.yml` 에는 `http-client.yml` 만 import(주석: `brawlstars-client.yml` 을 가져오면 테스트 `app.client.brawlstars.*` 값이 덮인다).
  - `BrawlStarsClientRealTest:17-18` 상수를 `"#88J8Q9RPV"`, `"#2U8CVLQQ9"` 로. 같은 파일 8-12행의 `@TestPropertySource(properties = ["app.client.brawlstars.api-keys="])` 를 지운다 — 인라인 테스트 프로퍼티가 yml 보다 우선해 키를 빈 값으로 덮는다. 키는 import 한 `brawlstars-client.yml` 의 `${BRAWLSTARS_API_KEYS:}` 로 받는다(없으면 `[""]` 로 기동하고 호출이 403 으로 실패 — 수집 확인에는 충분).
- [ ] **Step 6: 클라이언트 테스트 통과** — `./gradlew :client:brawlstars-client:test` → PASS.
- [ ] **Step 7: 호출부 테스트를 먼저 바꾸고 더한다** — 예외 헬퍼(테스트 파일마다 private 함수로 둔다): `HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found", HttpHeaders(), ByteArray(0), null)`, `HttpServerErrorException.create(HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable", HttpHeaders(), ByteArray(0), null)`, `ResourceAccessException("I/O error on GET request: Read timed out", SocketTimeoutException("Read timed out"))`.
  - `StatsBattleAccumulationServiceTest`: feign import 와 `serviceUnavailable()` 을 위 헬퍼로 교체(테스트 이름·검증 유지).
  - `PlayerRenewalServiceTest` 에 추가: `플레이어 조회가 404 면 renewal 을 NOT_FOUND 로 끝내고 플레이어를 DELETED 로 바꾼다`(미리 `aPlayerEntity().withBrawlStarsTag(tag)` 저장), `503 이면 점검을 등록하고 renewal 을 FAILED 로 끝낸다`(`brawlStarsApiMaintenanceRepository.count() == 1`).
  - Create `PlayerRefreshServiceTest : CoreWorkerContextTest()`: `플레이어 조회가 404 면 플레이어를 DELETED 로 바꾼다`(ACTIVE player 1명 저장 → 항상 그 player 가 뽑힌다), `503 이면 점검을 등록한다`.
  - Create `ClubRenewalServiceTest : CoreWorkerContextTest()`: `클럽 조회가 404 면 renewal 을 NOT_FOUND 로 끝내고 클럽을 DELETED 로 바꾼다`(`aClubEntity()`, `aRenewalEntity().withType(RenewalType.CLUB)` + `toPending()`), `503 이면 점검을 등록하고 renewal 을 FAILED 로 끝낸다`.
  - `BrawlStarsApiTaskletTest`: 헬퍼 교체, 추가 `503 이 아닌 HTTP 오류는 전파한다`(500 → 예외가 밖으로 나온다).
- [ ] **Step 8: 실패 확인** — `./gradlew --continue :core:core-worker:test :core:core-batch:test` → 두 모듈 모두 컴파일 실패(호출부가 삭제된 예외 타입을 참조).
- [ ] **Step 9: 호출부 catch 를 바꾼다**

| 파일 | 지금 | 바꾼 뒤 |
|------|------|---------|
| `StatsBattleAccumulationService` | `BrawlStarsApiClientException.NotFound` / `BrawlStarsApiServerException.ServiceUnavailable` / `BrawlStarsApiClientException` / `BrawlStarsApiServerException` / `Exception` | `HttpClientErrorException.NotFound`(동작 유지 — Task 7 에서 바꾼다) / `HttpServerErrorException.ServiceUnavailable`(동작 유지) / `RestClientResponseException` → `log.warn(ex) { "[배틀 통계 축적] API 오류 status=${ex.statusCode.value()} tag=$tag" }` / `ResourceAccessException` → `log.warn(ex) { "[배틀 통계 축적] API 통신 실패 tag=$tag" }` / `Exception` 유지. 전용 스레드 풀은 Task 5 에서 없앤다 |
| `PlayerRenewalService` | NotFound / ServiceUnavailable / Exception | `HttpClientErrorException.NotFound` / `HttpServerErrorException.ServiceUnavailable` / `Exception` 유지 |
| `PlayerRefreshService` | NotFound / ServiceUnavailable(`break`) / ClientException / ServerException / Exception | `HttpClientErrorException.NotFound` / `HttpServerErrorException.ServiceUnavailable`(`break` 유지) / `RestClientResponseException` → warn `[플레이어 갱신] API 오류 status=… tag=…` / `ResourceAccessException` → warn `[플레이어 갱신] API 통신 실패 tag=…` / `Exception` 유지 |
| `ClubRenewalService` | NotFound / ServiceUnavailable | `HttpClientErrorException.NotFound` / `HttpServerErrorException.ServiceUnavailable` |
| `BrawlStarsApiTasklet` | `BrawlStarsApiServerException.ServiceUnavailable` | `HttpServerErrorException.ServiceUnavailable` |

`when`·`catch` 순서는 하위 타입(NotFound·ServiceUnavailable)을 `RestClientResponseException` 보다 먼저 둔다.

- [ ] **Step 10: 호출부 테스트 통과** — Step 8 명령 → PASS.
- [ ] **Step 11: HTTP 배선 컨텍스트 테스트를 쓴다** — `BrawlStarsClientHttpWiringContextTest`: `CoreWorkerContextTest` 를 상속하지 않고 그 클래스 애노테이션(`@Tag("context") @SpringBootTest @Import(TestJpaConfig::class) @TestConstructor(autowireMode = ALL)`)과 consumer 2개의 `@MockkBean(relaxed = true)` 를 복제한다(`BrawlStarsClient` mock 만 뺀다). 생성자 `(brawlStarsClient: BrawlStarsClient, rateLimiterRegistry: RateLimiterRegistry)`. companion 에 `com.sun.net.httpserver.HttpServer` 하나(`setExecutor(Executors.newCachedThreadPool())`, `@AfterAll` 정지)를 두고 `exchange.requestURI.rawPath` 로 분기한다: `/v1/players/%23SLOW/battlelog` 는 3초 잠든다, `/v1/players/%23UNAVAILABLE/battlelog` 는 503(호출 수를 센다), 그 밖의 battlelog 는 `Content-Type: application/json` 헤더와 함께 `{"items":[],"paging":null}` 를 주고 `Authorization` 헤더와 rawPath 를 기록한다(`HttpServer` 는 Content-Type 을 붙이지 않아, 빠지면 RestClient 가 본문을 읽지 못한다). `@DynamicPropertySource`: `app.client.brawlstars.url=http://127.0.0.1:<port>`, `app.client.brawlstars.api-keys=wiring-key`, `spring.http.clients.read-timeout=500ms`, `resilience4j.ratelimiter.instances.brawlstars.limit-for-period=1000`, `.limit-refresh-period=1h`, `.timeout-duration=0s`.
  - `응답 대기 초과는 HC5 read-timeout 으로 ResourceAccessException 이 된다` — `assertTimeoutPreemptively(Duration.ofSeconds(5))` 안에서 `getPlayerRecentBattles("#SLOW")` → `ResourceAccessException`, `hasRootCauseInstanceOf(SocketTimeoutException::class.java)`.
  - `요청에 Bearer 키를 붙이고 태그를 %23 으로 보낸다` — 기록된 헤더 `Bearer wiring-key`, rawPath `/v1/players/%23A/battlelog`(로깅 processor 가 빠졌다면 이 호출이 `IllegalStateException` 으로 실패한다).
  - `HTTP 요청마다 brawlstars 제한기 허가를 하나씩 쓴다` — 호출 전후 `rateLimiterRegistry.rateLimiter("brawlstars").metrics.availablePermissions` 차이 == 호출 수(2).
  - `503 은 HTTP 클라이언트 층에서 재시도하지 않는다` — `HttpServerErrorException.ServiceUnavailable`, 서버 호출 수 1.
- [ ] **Step 12: 배선 테스트 통과** — `./gradlew :core:core-worker:test --tests 'com.imstargg.BrawlStarsClientHttpWiringContextTest'` → PASS.
- [ ] **Step 13: 의존 확인·기록** — `./gradlew :core:core-worker:dependencyInsight --dependency spring-retry --configuration runtimeClasspath`(db-core 도) → `2.0.12`. `./gradlew :core:core-batch:dependencyInsight --dependency aspectjweaver --configuration runtimeClasspath` 결과를 PR 설명에 적는다 — aspectjweaver 가 없으면 core-batch 는 지금까지 `@RateLimiter` 가 동작하지 않았고, 전환 뒤 처음으로 초당 10회 제한을 받는다(배포 후 ranking-collect 소요 시간 확인 항목).
- [ ] **Step 14: Feign·Spring Cloud 잔재 확인**

Run: `grep -rnE "feign|openfeign|org\.springframework\.cloud|spring-cloud-starter|BrawlStarsApi(Client|Server)?Exception|PagingParam" --include='*.kt' --include='*.kts' --include='*.yml' .` 와 Step 15 뒤의 `grep -n -i feign AGENTS.md`
Expected: 앞 명령은 `infrastructure/db-core` 의 `io.github.openfeign.querydsl` 만, 뒤 명령은 출력 없음.

- [ ] **Step 15: 문서** — `AGENTS.md:57` 트리 주석 `(OpenFeign)` → `(HTTP Service Client·RestClient)`, `:68` 의존 관계 → `` `brawlstars-client` → `support:http-client`, `support:logging` (HTTP Service Client·RestClient 기반) ``, support 트리에 `http-client/ # HTTP 클라이언트 공통 설정 — HC5·타임아웃·재시도 끔·커넥션 풀·호출 로깅`, 코드 규칙에 "외부 API 클라이언트는 RestClient 예외를 그대로 올리고(공용 예외 계층 없음) 재시도하지 않는다. HTTP 설정은 `support:http-client`, 그룹별 값은 `spring.http.serviceclient.<group>.*`". `client/brawlstars-client/README.md` 에 HTTP 설정 위치와 레이트리밋 인터셉터(제한기 `brawlstars`) 한 줄씩.
- [ ] **Step 16: 전체 빌드** — `./gradlew build` → `BUILD SUCCESSFUL`.
- [ ] **Step 17: 커밋** — `git add -A && git commit -m "refactor(brawlstars-client): Feign·Spring Cloud 를 걷어내고 HTTP Service Client(RestClient)로 바꾼다"`

---

### Task 5: 배치 메서드로 동시 호출 옮기기 (스펙 커밋 3-3, §4.4·§4.5)

**Files:**
- Modify: `client/brawlstars-client/src/main/kotlin/com/imstargg/client/brawlstars/{BrawlStarsClient,BrawlStarsClientConfig,BrawlStarsClientProperties}.kt`, `.../resources/brawlstars-client.yml`(`app.client.brawlstars.batch-concurrency: 5`), `client/brawlstars-client/README.md`
- Modify: `core/core-worker/src/main/kotlin/com/imstargg/core/worker/application/stats/StatsBattleAccumulationService.kt`
- Modify: `docs/brawlstars-api-maintenance.md:27,32`
- Test: Create `client/brawlstars-client/src/test/kotlin/com/imstargg/client/brawlstars/BrawlStarsClientBatchTest.kt`; Modify `client/brawlstars-client/src/test/kotlin/com/imstargg/client/brawlstars/BrawlStarsClientTest.kt`(하네스 생성자에 `batchConcurrency = 5`), `core/core-worker/.../stats/StatsBattleAccumulationServiceTest.kt`, `core/core-worker/src/test/kotlin/com/imstargg/BrawlStarsClientHttpWiringContextTest.kt`

**Interfaces:**
- Consumes: Task 4 의 `BrawlStarsApi`, `BrawlStarsClientConfig`.
- Produces: `class BrawlStarsClient internal constructor(private val brawlStarsApi: BrawlStarsApi, batchConcurrency: Int)`, `fun getPlayersRecentBattles(playerTags: Collection<String>): Map<String, Result<ListResponse<BattleResponse>>>`. `data class BrawlStarsClientProperties(val apiKeys: List<String>, val url: String, val batchConcurrency: Int = 5)`. `BrawlStarsClientConfig.brawlStarsClient(brawlStarsApi, properties)`.

- [ ] **Step 1: 배치 단위 테스트를 쓴다** — `BrawlStarsClientBatchTest`: `private val api = mockk<BrawlStarsApi>()`, `private val sut = BrawlStarsClient(api, batchConcurrency = 2)`.
  - `일부 태그가 실패해도 나머지 태그의 결과를 담아 돌려준다` — `#B` 만 NotFound → `result.getValue("#B").exceptionOrNull()` 이 `HttpClientErrorException.NotFound`, `#A`·`#C` 는 성공.
  - `동시에 보내는 요청 수가 batchConcurrency 를 넘지 않는다` — 태그 6개, stub 이 진행 중 수를 올리고 최대값을 기록한 뒤 100ms 잠든다 → 최대 진행 중 == 2.
  - `두 배치 호출이 겹쳐도 클라이언트 전체 동시 요청 수는 batchConcurrency 를 넘지 않는다` — 두 스레드에서 동시에 호출 → 최대 진행 중 ≤ 2.
  - `중복 태그는 한 번만 호출하고 결과는 입력 순서를 따른다` — `listOf("#B", "#A", "#B")` → `keys` `containsExactly("#B", "#A")`, `verify(exactly = 1) { api.getLogOfRecentBattlesForAPlayer("#B") }`.
  - `Error 는 Result 에 담지 않고 그대로 던진다` — stub 이 `StackOverflowError` → `catchThrowableOfType(StackOverflowError::class.java)`.
  - `빈 목록이면 호출 없이 빈 맵을 돌려준다`.
- [ ] **Step 2: 실패 확인** — `./gradlew :client:brawlstars-client:test --tests '*BrawlStarsClientBatchTest'` → 컴파일 실패.
- [ ] **Step 3: 배치 메서드를 구현한다** — `Semaphore(batchConcurrency)` 는 클라이언트 필드(전체 공유). 요청마다 허가를 잡고 놓는다. `Error` 만 다시 던지고 `Exception` 은 `Result.failure` 로 담는다(`runCatching` 은 `Error` 까지 삼키므로 쓰지 않는다).

```kotlin
fun getPlayersRecentBattles(playerTags: Collection<String>): Map<String, Result<ListResponse<BattleResponse>>> {
    val tags = playerTags.distinct()
    if (tags.isEmpty()) return emptyMap()
    return Executors.newVirtualThreadPerTaskExecutor().use { executor ->
        tags.associateWith { tag -> executor.submit(Callable { withBatchPermit { getPlayerRecentBattles(tag) } }) }
            .mapValues { (_, future) ->
                try {
                    Result.success(future.get())
                } catch (ex: ExecutionException) {
                    val cause = ex.cause ?: ex
                    if (cause is Error) throw cause
                    Result.failure(cause)
                }
            }
    }
}
```

  `withBatchPermit` 은 `private inline fun <T> withBatchPermit(block: () -> T): T` — `batchPermits.acquire()` 후 `try { block() } finally { batchPermits.release() }`.
  `BrawlStarsClientProperties` 에 `batchConcurrency: Int = 5`, `BrawlStarsClientConfig.brawlStarsClient(brawlStarsApi, properties)` 가 `BrawlStarsClient(brawlStarsApi, properties.batchConcurrency)`. Task 4 의 `BrawlStarsClientTest` 하네스 생성자도 `batchConcurrency = 5` 를 넘긴다. `brawlstars-client.yml` 에 `batch-concurrency: 5`.
- [ ] **Step 4: 통과 확인** — `./gradlew :client:brawlstars-client:test` → PASS.
- [ ] **Step 5: 통계 축적 테스트를 배치 기준으로 바꾼다** — `StatsBattleAccumulationServiceTest`:
  - 기존 stub `getPlayerRecentBattles` 를 `every { brawlStarsClient.getPlayersRecentBattles(capture(tags)) } answers { firstArg<Collection<String>>().associateWith { Result.success(ListResponse<BattleResponse>(emptyList(), null)) } }` 로 바꾸고, 태그 검증은 `tags.captured` 로 한다.
  - `한 틱의 플레이어를 동시에 수집한다` 는 삭제(동시성은 `BrawlStarsClientBatchTest` 가 본다).
  - 점검 테스트는 모든 태그를 `Result.failure(serviceUnavailable())` 로 → `brawlStarsApiMaintenanceRepository.count() == 1`.
  - `@MockkBean(relaxUnitFun = true) private lateinit var statsBattleAggregator: StatsBattleAggregator` 를 두고 추가:
    - `타임아웃이 난 플레이어는 경고만 남기고 나머지 플레이어를 계속 집계한다` — 첫 태그만 `Result.failure(ResourceAccessException(...))` → `verify(exactly = <뽑힌 수 - 1>) { statsBattleAggregator.aggregate(any()) }`.
    - `레이트리밋 허가를 못 받은 플레이어가 있어도 나머지 플레이어는 집계한다` — 첫 태그만 `Result.failure(RequestNotPermitted.createRequestNotPermitted(RateLimiter.ofDefaults("brawlstars")))` → 같은 검증(Review Focus 2).
    - `한 틱에 점검 응답이 섞여도 나머지 플레이어는 집계한다` — 첫 태그만 `Result.failure(serviceUnavailable())`, 나머지는 성공 → `brawlStarsApiMaintenanceRepository.count() == 1`, `verify(exactly = <뽑힌 수 - 1>) { statsBattleAggregator.aggregate(any()) }`.
- [ ] **Step 6: 실패 확인** — `./gradlew :core:core-worker:test --tests '*StatsBattleAccumulationServiceTest'` → FAIL(서비스가 아직 단건 메서드를 부른다).
- [ ] **Step 7: 서비스를 배치 호출로 바꾼다** — `executor` 필드·KDoc·`@PreDestroy shutdown()`·companion(`SHUTDOWN_TIMEOUT_SECONDS`)·`CompletableFuture`/`Executors`/`TimeUnit`/`AtomicBoolean` import 를 지운다. `accumulate()` 는 `brawlStarsClient.getPlayersRecentBattles(statsPlayers.map { it.brawlStarsTag })` 결과를 스케줄러 스레드에서 플레이어 순서대로 처리한다: 성공 → `BattleBuilder` + `statsBattleAggregator.aggregate`(플레이어별 `try/catch(Exception)` → `log.error(ex) { "[배틀 통계 축적] 처리 실패 tag=$tag" }`), 실패 → Task 4 의 분기와 같다. 로컬 플래그는 점검 중복 등록만 막는다 — 503 다음 순서의 플레이어도 성공이면 집계하고 다른 실패는 각 분기대로 처리한다(지금의 `if (maintenanceDetected.get()) return` 조기 종료는 옮기지 않는다).
- [ ] **Step 8: 통과 확인** — Step 6 명령 → PASS.
- [ ] **Step 9: 배선 테스트에 배치 경로를 더한다** — `HTTP 요청마다 brawlstars 제한기 허가를 하나씩 쓴다` 를 단건 1회 + `getPlayersRecentBattles(listOf("#A", "#B"))` 로 바꾸고 허가 차이 == 3.
- [ ] **Step 10: 문서** — `docs/brawlstars-api-maintenance.md:32` "스케줄러는 503 감지 시 나머지 플레이어 처리 중단 (break)" → "통계 축적은 한 틱의 요청을 배치로 함께 보내므로 503 을 받으면 틱당 한 번 점검을 등록하고 다음 틱부터 호출을 막는다. 처리 중단(break)은 플레이어 새로고침에만 해당한다". 27행 `StatsBattleAccumulationScheduler.accumulate()` → `StatsBattleAccumulationService.accumulate()`, 표에 `| PlayerRefreshService.refresh() | O | O |` 행을 추가한다. `README.md` 에 배치 메서드 한 줄(`getPlayersRecentBattles`, 동시 실행 상한 `batch-concurrency`).
- [ ] **Step 11: 전체 빌드** — `./gradlew build` → `BUILD SUCCESSFUL`.
- [ ] **Step 12: 커밋** — `git add -A && git commit -m "refactor(brawlstars-client): 여러 플레이어 battlelog 동시 조회를 클라이언트 배치 메서드로 옮긴다"`

---

### Task 6: DTO null 처리 (스펙 커밋 4, §4.6)

**Files:**
- Modify: `client/brawlstars-client/src/main/kotlin/com/imstargg/client/brawlstars/response/BattleResultBrawlerResponse.kt:5`(`name: String?`), `PlayerResponse.kt:8`, `ClubMemberResponse.kt:6`, `PlayerRankingResponse.kt:6`(`nameColor: String?`)
- Modify: `core/core-worker/src/main/kotlin/com/imstargg/core/worker/domain/Battle.kt:35`(`BattleTeamPlayerBrawler.name: String?`), `core/core-worker/.../application/stats/StatsBattleAggregateApplier.kt:126`
- Modify: `infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/brawlstars/battle/BattleTeamPlayerEntityBrawler.kt:10-11`(`nullable = true`, `val name: String?`), `infrastructure/db-core/src/main/resources/schema.sql:200`(`brawler_name varchar(65) null`)
- Create: `infrastructure/db-core/src/main/resources/db/migration/V261102a__battle_team_player_brawler_name_nullable.sql`
- Test: Create `client/brawlstars-client/src/test/resources/brawlstars/{battlelog-hidden-brawler,player-name-color-null,club-name-color-null,ranking-name-color-null}.json`; Modify `BrawlStarsClientTest.kt`, `core/core-worker/src/test/kotlin/com/imstargg/core/worker/domain/BattleBuilderTest.kt`, `.../application/stats/StatsBattleAggregateApplierTest.kt`, `.../application/player/PlayerUpdaterUpdateBattlesTest.kt`

**Interfaces:**
- Produces: `BattleResultBrawlerResponse.name: String?`, `BattleTeamPlayerBrawler.name: String?`, `BattleTeamPlayerEntityBrawler.name: String?`. `StatsBattleEntityTeamPlayerBrawler.name: String` 은 바꾸지 않는다.

- [ ] **Step 1: 디코딩 픽스처와 테스트를 쓴다** — `battlelog-hidden-brawler.json` 은 아래 익명화 픽스처(`#LQGGG` 재현 전투에서 태그·이름만 바꿈). 나머지 세 파일은 각 DTO 의 non-null 필드를 모두 채운 최소 JSON 이고 `nameColor` 만 `null` 이다(`club-name-color-null.json` 은 `/v1/clubs/{tag}` 응답, 멤버 한 명의 `nameColor` 가 null; `ranking-name-color-null.json` 은 `/v1/rankings/kr/players` 의 `items` 한 건).

```json
{"items":[{"battleTime":"20241224T123233.000Z",
  "event":{"id":15000144,"mode":"brawlBall","modeId":5,"map":"Sunny Soccer"},
  "battle":{"mode":"brawlBall","type":"ranked","result":"victory","duration":46,"trophyChange":9,
    "starPlayer":{"tag":"#P3","name":"p3","brawler":{"id":16000088,"name":null,"power":11,"trophies":8}},
    "teams":[[{"tag":"#P1","name":"p1","brawler":{"id":16000088,"name":null,"power":11,"trophies":8}},
              {"tag":"#P2","name":"p2","brawler":{"id":16000086,"name":"SHADE","power":8,"trophies":8}},
              {"tag":"#P3","name":"p3","brawler":{"id":16000088,"name":null,"power":11,"trophies":8}}],
             [{"tag":"#P4","name":"p4","brawler":{"id":16000016,"name":"PAM","power":10,"trophies":16}},
              {"tag":"#P5","name":"p5","brawler":{"id":16000030,"name":"EMZ","power":10,"trophies":0}},
              {"tag":"#P6","name":"p6","brawler":{"id":16000045,"name":"STU","power":10,"trophies":0}}]]}}],
 "paging":{"cursors":{}}}
```

  `BrawlStarsClientTest` 추가: `브롤러 이름이 null 인 battlelog 를 디코딩한다`(`teams[0][0].brawler.name` null, `starPlayer.brawler.name` null, `teams[0][1].brawler.name == "SHADE"`), `nameColor 가 null 인 플레이어 응답을 디코딩한다`, `nameColor 가 null 인 클럽 멤버를 디코딩한다`, `nameColor 가 null 인 랭킹 항목을 디코딩한다`.
- [ ] **Step 2: 도메인·저장 테스트를 쓴다**
  - `BattleBuilderTest`: `브롤러_이름이_null이어도_Battle을_생성한다` — `BattleResultBrawlerResponse(id = 16000088L, name = null, power = 11, trophies = 500, trophyChange = null)` 로 만든 응답 → `actual.teams[0][0].brawler.name` null.
  - `StatsBattleAggregateApplierTest`: `브롤러 이름이 null 이면 통계 JSON 에 빈 문자열로 저장한다` — `createTeam3v3()`(트로피 1500, 고유 ID) 에서 `#A1` 의 브롤러 이름만 null 로 한 전투 → `statsResultBattleRepository.findAll().single().teams.flatten().first { it.brawlStarsTag == "#A1" }.brawler.name == ""`.
  - `PlayerUpdaterUpdateBattlesTest`: `브롤러_이름이_null이면_battle_team_player에_null로_저장한다` — 저장된 해당 선수의 `brawler.name` null.
- [ ] **Step 3: 실패 확인** — `./gradlew --continue :client:brawlstars-client:test :core:core-worker:test --tests '*BattleBuilderTest' --tests '*StatsBattleAggregateApplierTest' --tests '*PlayerUpdaterUpdateBattlesTest'` → FAIL(역직렬화 실패·컴파일 실패).
- [ ] **Step 4: 구현** — Files 의 타입 변경. `StatsBattleAggregateApplier.convertTeams` 는 `name = player.brawler.name ?: ""` 와 주석: "통계 JSON 은 jackson-module-kotlin 생성자 주입으로 읽혀 non-null 파라미터에 null 이 오면 구버전 core-batch Reader 가 실패한다 — 빈 문자열이면 배포 순서·롤백 제약이 없다".
- [ ] **Step 5: DDL·스키마** — `schema.sql:200` 을 `null` 로. 마이그레이션은 `V261020__add_deleted_at_to_brawlstars_resources.sql` 머리 주석 형식(무엇을/대상/배경/배포 순서 'DDL 선적용 → 앱 배포'·구버전 호환·메타데이터 변경/멱등)에 "적용 시 `lock_timeout` 3s, 실패하면 02:00~09:00 KST 를 피해 재시도"를 더하고 본문은 `set lock_timeout = '3s';` + `alter table battle_team_player alter column brawler_name drop not null;`(반복 실행해도 오류가 없는 멱등임을 주석에 적는다).
- [ ] **Step 6: 통과 확인** — Step 3 명령 → PASS.
- [ ] **Step 7: 전체 빌드** — `./gradlew build` → `BUILD SUCCESSFUL`.
- [ ] **Step 8: 커밋** — `git add -A && git commit -m "fix(backend): 브롤러 이름·nameColor 가 null 인 응답을 받아들인다"`

---

### Task 7: battlelog 404 를 상태로 표시 (스펙 커밋 5-1, §4.7)

**Files:**
- Create: `core/core-enum/src/main/kotlin/com/imstargg/core/enums/StatsPlayerStatus.kt`, `core/core-worker/src/main/kotlin/com/imstargg/core/worker/application/stats/StatsPlayerStatusUpdater.kt`, `infrastructure/db-core/src/main/resources/db/migration/V261102b__add_status_to_stats_player.sql`
- Modify: `infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/battle/StatsPlayerEntity.kt`, `.../statistics/battle/repository/StatsPlayerJpaRepository.kt:7,13`, `infrastructure/db-core/src/main/resources/schema.sql:351`, `infrastructure/db-core/src/testFixtures/kotlin/com/imstargg/infrastructure/db/core/test/fixture/StatsPlayerEntityFixture.kt`
- Modify: `core/core-worker/.../application/player/PlayerDeactivator.kt`, `core/core-worker/.../application/stats/StatsBattleAccumulationService.kt`
- Test: Modify `core/core-worker/.../stats/StatsBattleAccumulationServiceTest.kt`, `core/core-worker/.../player/PlayerRenewalServiceTest.kt`; Create `core/core-batch/src/test/kotlin/com/imstargg/core/batch/job/statistics/playersync/StatsPlayerSyncJobItemWriterTest.kt`

**Interfaces:**
- Produces: `enum class StatsPlayerStatus { ACTIVE, BATTLE_LOG_UNAVAILABLE }`. `StatsPlayerEntity.status: StatsPlayerStatus`(`@Enumerated(EnumType.STRING) @Column(name = "status", length = 25, nullable = false)`, 기본 `ACTIVE`, `protected set`), `fun markBattleLogUnavailable()`. `StatsPlayerJpaRepository.findFirstByIdGreaterThanEqualAndStatusOrderByIdAsc(id: Long, status: StatsPlayerStatus): StatsPlayerEntity?`(기존 `findFirstByIdGreaterThanEqualOrderByIdAsc`·`deleteByBrawlStarsTag` 삭제). `@Component class StatsPlayerStatusUpdater` 의 `@Transactional fun markBattleLogUnavailable(statsPlayerId: Long)`. `StatsPlayerEntityFixture.withStatus(value: StatsPlayerStatus)`.

- [ ] **Step 1: 테스트를 쓴다**
  - `StatsBattleAccumulationServiceTest`:
    - `battlelog 404 면 stats_player 를 BATTLE_LOG_UNAVAILABLE 로 표시하고 행과 player 는 유지한다` — 200개 태그 모두에 `aPlayerEntity().withBrawlStarsTag("#P$it")` 를 저장하고, 모든 결과를 `Result.failure(notFound())` → `tags.captured` 의 모든 태그에 대해 `statsPlayerRepository.findById(idByTag.getValue(tag)).get().status == BATTLE_LOG_UNAVAILABLE` 이고 `playerRepository.findByBrawlStarsTag(tag)!!.status == PlayerStatus.ACTIVE`, `statsPlayerRepository.count() == 200`.
    - `ACTIVE 인 stats_player 만 뽑는다` — 10의 배수 태그(`#P10`, `#P20` … `#P200`, 최대 ID 행 포함)만 ACTIVE 로 남기고 나머지는 `markBattleLogUnavailable()` 후 `saveAll` → 어떤 무작위 ID 로도 ACTIVE 행이 잡히므로, 캡처한 태그가 모두 그 ACTIVE 집합에 속한다.
    - `점검 응답 뒤 순서의 404 도 표시한다` — 캡처한 첫 태그는 `Result.failure(serviceUnavailable())`, 둘째 태그는 `Result.failure(notFound())`, 나머지 성공 → 둘째 태그의 status 가 `BATTLE_LOG_UNAVAILABLE`.
    - `뽑을 ACTIVE 플레이어가 없으면 API 를 부르지 않는다` — 200행 모두 `BATTLE_LOG_UNAVAILABLE` → `verify(exactly = 0) { brawlStarsClient.getPlayersRecentBattles(any()) }`(Review Focus 3).
  - `PlayerRenewalServiceTest` 의 `플레이어 조회가 404 면 …` 에 `aStatsPlayerEntity().withBrawlStarsTag(tag)` 를 미리 저장하고 `statsPlayerRepository.findAllByBrawlStarsTagIn(listOf(tag))` 크기 1 을 더 단언한다.
  - Create `StatsPlayerSyncJobItemWriterTest : CoreBatchContextTest()` — `StatsPlayerSyncJobItemWriter(statsPlayerJpaRepository)` 를 직접 만들고 `write(Chunk(listOf(listOf(teamPlayer("#A"), teamPlayer("#B")))))`(`teamPlayer(tag) = StatsBattleEntityTeamPlayer(brawlStarsTag = tag, name = "p", brawler = StatsBattleEntityTeamPlayerBrawler(16000000, "SHELLY", 11, 1000))`, `#A` 는 미리 `withStatus(BATTLE_LOG_UNAVAILABLE)` 로 저장): `표시된 태그를 다시 만나도 새로 넣지 않고 상태를 유지한다` — `count() == 2`, `#A` 는 `BATTLE_LOG_UNAVAILABLE`, `#B` 는 `ACTIVE`(Review Focus 4). `@AfterEach cleanUp.all()`.
- [ ] **Step 2: 실패 확인** — `./gradlew --continue :core:core-worker:test --tests '*StatsBattleAccumulationServiceTest' --tests '*PlayerRenewalServiceTest' :core:core-batch:test --tests '*StatsPlayerSyncJobItemWriterTest'` → 컴파일 실패.
- [ ] **Step 3: 구현**
  - enum 은 `PlayerStatus.kt` 와 같은 형태. 엔티티는 `PlayerEntity.status`/`toDeleted()` 와 같은 패턴. 픽스처는 `PlayerEntityFixture` 처럼 `private var status = ACTIVE`, `withStatus`, `build()` 에서 `BATTLE_LOG_UNAVAILABLE` 이면 `markBattleLogUnavailable()`.
  - `StatsPlayerStatusUpdater.markBattleLogUnavailable(statsPlayerId)` 는 `findById(statsPlayerId).orElse(null)?.markBattleLogUnavailable()`(변경 감지로 `updated_at` 갱신).
  - `PlayerDeactivator`: `StatsPlayerJpaRepository` 의존과 `deleteByBrawlStarsTag` 호출을 지운다(player soft-delete 와 로그만 남음).
  - `StatsBattleAccumulationService`: `PlayerDeactivator` 의존을 `StatsPlayerStatusUpdater` 로 바꾸고, `NotFound` 분기는 `statsPlayerStatusUpdater.markBattleLogUnavailable(statsPlayer.id!!)` + `log.info { "[배틀 통계 축적] battlelog 없음(404) — 수집 제외 tag=$tag" }`. 뽑기는 `findFirstByIdGreaterThanEqualAndStatusOrderByIdAsc(Random.nextLong(1, maxId + 1), StatsPlayerStatus.ACTIVE)`. 뽑은 플레이어가 없으면 배치 호출 전에 반환한다.
- [ ] **Step 4: DDL·스키마** — `schema.sql` 의 `stats_player` 블록 `brawlstars_tag` 줄 다음에 `status varchar(25) not null default 'ACTIVE',`. `V261102b__add_status_to_stats_player.sql` 은 Task 6 과 같은 머리 주석 형식(배경: battlelog 404 계정을 지우지 않고 표시, 상수 기본값이라 메타데이터 변경, 구버전 앱은 컬럼을 무시) + `set lock_timeout = '3s';` + `alter table stats_player add column if not exists status varchar(25) not null default 'ACTIVE';`.
- [ ] **Step 5: 통과 확인** — Step 2 명령 → PASS.
- [ ] **Step 6: 전체 빌드** — `./gradlew build` → `BUILD SUCCESSFUL`.
- [ ] **Step 7: 커밋** — `git add -A && git commit -m "fix(core-worker): battlelog 404 계정을 지우지 않고 BATTLE_LOG_UNAVAILABLE 로 표시해 수집에서 뺀다"`

---

### Task 8: 갱신 경로·새로고침 정렬 (스펙 커밋 5-2, §4.7·§4.8)

**Files:**
- Modify: `core/core-worker/src/main/kotlin/com/imstargg/core/worker/application/player/PlayerRenewalService.kt`, `.../player/PlayerRefreshService.kt`
- Modify: `infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/brawlstars/player/repository/PlayerJpaRepository.kt:19-23`
- Modify: `docs/renewal-flow.md:24-25`
- Test: Modify `core/core-worker/.../player/PlayerRenewalServiceTest.kt`

**Interfaces:**
- Produces: `PlayerJpaRepository.findAllByStatusAndIdGreaterThanEqualOrderByIdAsc(status: PlayerStatus, id: Long, pageable: Pageable): List<PlayerEntity>`(기존 `findAllByStatusAndIdGreaterThanEqual` 의 이름 변경).

- [ ] **Step 1: 테스트를 쓴다** — `PlayerRenewalServiceTest`(given 에 `justRun { statsBattleAggregator.asyncAggregate(any()) }`):
  - `battlelog 만 404 면 전투 0건으로 갱신을 완료한다` — player 를 미리 저장, `getPlayerInformation` 은 성공, `getPlayerRecentBattles` 는 `throws notFound()` → renewal `COMPLETED`, player `status == ACTIVE`, 그 player 의 저장된 전투 0건.
  - `DB 에 없는 플레이어도 battlelog 만 404 면 플레이어를 만들고 갱신을 완료한다` — player 행 없이 같은 stub → renewal `COMPLETED`, `playerRepository.findByBrawlStarsTag(tag)` 가 not null 이고 `ACTIVE`(Review Focus 5).
- [ ] **Step 2: 실패 확인** — `./gradlew :core:core-worker:test --tests '*PlayerRenewalServiceTest'` → FAIL(지금은 `NOT_FOUND`).
- [ ] **Step 3: 구현** — `PlayerRenewalService` 의 battlelog 호출만 따로 감싼다: `HttpClientErrorException.NotFound` 면 `log.info { "[플레이어 갱신] battlelog 없음(404) — 전투 0건으로 완료 tag=$brawlStarsTag" }` 후 `emptyList()`. `/players` 404 는 바깥 catch 가 그대로 받는다. `PlayerJpaRepository` 메서드 이름을 바꾸고 `PlayerRefreshService.findRandomPlayers` 호출을 맞춘다(같은 `PageRequest.of(0, properties.playerCount)`).
- [ ] **Step 4: 통과 확인** — Step 2 명령 → PASS.
- [ ] **Step 5: 문서** — `docs/renewal-flow.md:25` 의 `EXECUTING → NOT_FOUND` 조건 "API 404 응답" → "`/players` 404(클럽은 `/clubs` 404)", 24행 다음에 `| EXECUTING → COMPLETED | core-worker | battlelog 404 — 전투 0건으로 완료 |` 행 추가.
- [ ] **Step 6: 전체 빌드** — `./gradlew build` → `BUILD SUCCESSFUL`.
- [ ] **Step 7: 커밋** — `git add -A && git commit -m "fix(core-worker): 갱신 중 battlelog 만 404 면 전투 0건으로 완료한다"`

---

### Task 9: 마무리

- [ ] **Step 1: 버전 bump** — `release` 스킬의 bump 단계로 core-api·core-batch·core-worker SNAPSHOT 버전을 올리고 그 커밋을 만든다(core-admin 은 대상 외).
- [ ] **Step 2: 최종 검증** — `./gradlew build` → `BUILD SUCCESSFUL`. 현재 IP 가 등록된 테스트 키가 있으면 `BRAWLSTARS_API_KEYS=<키> ./gradlew :client:brawlstars-client:developTest` 로 실제 응답 디코딩을 확인한다(키는 명령 인자·로그·커밋에 남기지 않는다). 키가 없으면 건너뛰었다고 PR 설명에 적는다.
- [ ] **Step 3: 범위 확인** — `git log --oneline origin/develop..HEAD` 가 docs 3개 + Task 1~8 커밋 8개 + bump 1개인지 확인한다. push·PR 은 사용자가 요청할 때 한다.

---

## 배포 후 운영 절차 (스펙 §6, 코드 작업 아님)

1. 앱 배포 전 운영 DB 에 `V261102a`·`V261102b` 를 수동 적용한다(`lock_timeout` 3s, 실패 시 02:00~09:00 KST 를 피해 재시도).
2. core-worker·core-batch·core-api(·core-admin) 를 Java 25 이미지로 배포한다. 순서 제약 없음.
3. 오삭제 복구(1회): VictoriaLogs 에서 `"[배틀 통계 축적] 플레이어 삭제됨"` 태그를 보관 기간 전체로 뽑고(더 오래된 것은 복구 대상 밖임을 기록), 그중 `player.status = 'DELETED'` 수를 센 뒤 `update player set status = 'ACTIVE', updated_at = now() where brawlstars_tag in (...) and status = 'DELETED';`.
4. 24시간 확인: `"Read timed out"` ≈ 0, `"[배틀 통계 축적] 플레이어 삭제됨"` 0, `"[배틀 통계 축적] battlelog 없음(404)"` 감소(갱신 경로 로그는 사용자 재요청으로 계속 나므로 제외), `"Error while extracting response"`·`"[배틀 통계 축적] 처리 실패"` 0, `RequestNotPermitted` 0, 통계 적재량(id 차분) 하루 약 74.8만 건 유지, ranking-collect 소요 시간(Task 4 Step 13 의 aspectj 결과와 비교), 이벤트 통계 화면의 숨김 브롤러 `-` 행.
5. 롤백: 앱 이미지만 되돌린다. core-worker 를 되돌리면 표시된 행이 다시 삭제되는 루프가 돌아온다(데이터 손실 아님).
