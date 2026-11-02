# brawlstars-client RestClient 전환 + 기반 업그레이드 + 클라이언트 버그 수정 설계

- 작성일: 2026-11-02 (같은 날 누락 검토 반영)
- 대상: `imstargg-backend` 전체(버전·로깅), `client/brawlstars-client`, `support/http-client`(신규), `core/core-worker`, `core/core-batch`, `core/core-api`(`ErrorLogger`), `core/core-enum`, `infrastructure/db-core`, `support/monitoring`, `Dockerfile`, `.github/workflows/pr-test.yml`, `imstargg-backend/AGENTS.md`·`imstargg-backend/docs`
- 상태: 설계 확정 (구현 전)
- 참고 패턴: noguesstoday-backend — `support/http-client`, HTTP Service Client(`@ImportHttpServices`), 클라이언트 예외 ADR(`docs/superdomain/adr/2026-10-12-no-client-exception-hierarchy.md`), 재시도 ADR(`2026-10-12-no-client-retry.md`), kotlin-logging 규칙(`noguesstoday-backend/AGENTS.md`), 같은 버전 업그레이드 커밋 `79ac52e1`

## 1. 배경 — 운영 로그 조사 결과 (2026-11-02)

VictoriaLogs·VictoriaMetrics·운영 DB 조회와 테스트 키 재현으로 확인한 사실이다.

### 1.1 'API 클라이언트 오류' 의 실체는 read timeout

- core-worker 의 `[배틀 통계 축적] API 클라이언트 오류` 는 대부분 `feign.RetryableException: Read timed out`(read-timeout 5s)이다. 전부 `/v1/players/{tag}/battlelog` 이고, 호출당 약 0.24% 다(배포 전 약 0.14%).
- 다른 회선·클라이언트로 같은 패턴 2,000건을 재현하면 p99 3.45s·최대 6.47s, 5초 초과 0.25% 로 운영과 같다. 업스트림 꼬리 지연에 5초가 빠듯한 것이다.
- 배포(`c08e0c63`, 5명 병렬 호출) 뒤로는 같은 틱에 함께 보낸 요청이 수 ms 안에 함께 타임아웃되는 묶음이 생겼다(3.25일간 2건 묶음 321초, 3건 이상 19초). 독립 사건이라면 기대값의 6배를 넘는다.
- `BrawlStarsApiException.from` 이 상태 -1(전송 실패)과 디코드 실패를 `BrawlStarsApiClientException` 으로 분류해, 타임아웃이 "클라이언트 오류" 로 찍힌다.

### 1.2 DTO 역직렬화 실패

- 브롤러 `16000088`(`/v1/brawlers` 에 없는 숨김 브롤러)의 `name` 이 `null` 로 와서 `BattleResultBrawlerResponse.name: String` 역직렬화가 실패하고, 그 플레이어의 배틀로그 25건 전체가 버려진다(`#LQGGG` 로 재현).
- `PlayerResponse.nameColor` 가 `null` 로 와서 플레이어 갱신이 실패한 적이 있다(2026-10-25, `#800JQY2VR8`). 같은 필드가 `ClubMemberResponse`·`PlayerRankingResponse` 에도 non-null 로 있다.

### 1.3 '플레이어 삭제됨' 은 대부분 살아 있는 계정

- 통계 축적은 battlelog 404 를 받으면 무조건 `PlayerDeactivator.deactivate()` 를 부른다 — player soft-delete + `stats_player` hard delete. 배포 후 하루 약 4,300건(호출의 약 0.6%)이다.
- 표본 120개 중 51% 가 2일 안에 `StatsPlayerSyncJob` 으로 다시 등록됐고, 그중 22개는 삭제 처리 이후의 전투에 참가했다. 120개 모두 battlelog 수집에 한 번도 성공하지 않았다(지속적인 404). 전투에서 찾은 10개 중 9개는 이름이 `Brawler` 이고, 앞서 `/players` 로 확인한 3개도 이름 `Brawler`·트로피 0 을 돌려줬다. API 가 battlelog 를 막아 둔 계정군으로 보인다(봇인지 가려진 실제 유저인지는 우리 쪽에서 가릴 수 없다).
- 그래서 재등록 → 뽑힘 → 404 → 삭제가 무한히 반복된다.
- 배포 직후 급증(하루 65~128건 → 약 4,300건)한 원인은 배포 전 선택 쿼리다. `findAllByIdGreaterThanEqual(randomId, PageRequest.of(0, 1))` 는 `ORDER BY` 가 없어 Seq Scan 으로 실행되고, 무작위 ID 의 약 76% 에서 같은 물리 행을 돌려줬다. 극소수 플레이어만 반복 수집하느라 404 계정군이 드러나지 않았고, 배포의 `findFirstByIdGreaterThanEqualOrderByIdAsc` 가 균등 추출로 바로잡으면서 실제 비율이 드러났다(적재량도 같은 이유로 하루 약 2.6만 → 74.8만 건, 28배).
- `PlayerStatus.DELETED` 는 끝 상태다 — `ACTIVE` 로 되돌리는 코드가 없고, core-api 는 `DELETED` 플레이어의 갱신 요청을 410 으로 막는다. 이미 잘못 삭제된 살아 있는 계정은 지금 영구히 갱신할 수 없다.

### 1.4 구조 문제

- 동시 호출 정책(동시 개수·재시도·레이트리밋)이 `StatsBattleAccumulationService` 의 전용 스레드 풀 등 호출부에 흩어져 있다.
- Spring Cloud OpenFeign 은 2022.0 부터 기능 완료(버그 수정만) 상태이고 공식 문서가 Spring HTTP Service Client 로의 이전을 권한다. imstargg 에서 Feign·Spring Cloud 를 쓰는 곳은 `brawlstars-client` 하나뿐이다.

## 2. 결정 사항 (사용자 합의)

1. Feign 과 Spring Cloud 를 걷어내고 RestClient 기반 HTTP Service Client 로 전환한다. WebClient 는 쓰지 않는다 — 동시 요청이 많아야 수십 개이고 호출부가 전부 블로킹이라 논블로킹의 이득이 없다.
2. 동시 여러 호출은 `BrawlStarsClient` 내부가 책임진다(배치 메서드). 호출부의 스레드 풀은 없앤다.
3. HTTP 구성은 noguesstoday 패턴을 따른다(§4.3·§4.4).
4. 클라이언트 예외는 noguesstoday ADR 을 따른다 — 공용 예외 계층 없이 RestClient 예외를 그대로 올린다(§4.5).
5. 통계 축적 중 battlelog 404 는 `stats_player` 를 지우지 않고 상태 enum 으로 표시해 뽑기에서 제외한다. player soft-delete 는 하지 않는다. `stats_player` hard delete 는 어느 경로에서도 하지 않는다(§4.7).
6. 브롤러 이름 `null` 은 DTO·worker 도메인·`battle_team_player` 까지 nullable 로 두되, 통계 JSON(stats 테이블 `teams`)에는 빈 문자열로 저장한다 — 구버전 core-batch 가 읽을 수 있어 배포 순서 제약과 롤백 제약이 생기지 않는다(§4.6).
7. 사용자 갱신 중 `/players` 는 200 인데 battlelog 만 404 면 전투 없음으로 보고 갱신을 완료한다(§4.8).
8. 버전은 noguesstoday 와 맞추고 kotlin-logging 을 도입한다.
9. 전부 PR 하나로 내되, 커밋을 단계별로 나눈다(§3).
10. 과거 battlelog 404 로 잘못 `DELETED` 가 된 player 는 VictoriaLogs 로그의 태그로 일괄 복구한다(§6.3).

## 3. 범위와 커밋 순서

PR 하나 안에서 아래 순서로 커밋한다. 커밋마다 `./gradlew build` 가 통과해야 한다 — 운영에서 문제가 나면 어느 단계 탓인지 커밋 단위로 되짚기 위해서다. 각 커밋은 자기 변경을 검증하는 테스트를 함께 넣는다(§5 의 커밋 열).

| # | 커밋 | 내용 | 절 |
|---|------|------|----|
| 1 | `chore(backend)` | 버전 업그레이드(Spring Cloud 는 2025.1.3 으로 임시 상향), `developTest` 태그 수정, Gradle·Kotlin·Boot deprecated 정리, AGENTS.md Java 25 | §4.1 |
| 2 | `refactor(backend)` | kotlin-logging 전환(`ErrorLogger` 포함), AGENTS.md 로깅 규칙 | §4.2 |
| 3 | `refactor(brawlstars-client)` | Feign·Spring Cloud 제거 → HTTP Service Client, `support:http-client` 신설(커넥션 풀 포함), 배치 메서드, 호출부 예외 변경, AGENTS.md·maintenance 문서 | §4.3~4.5, §4.9 |
| 4 | `fix(backend)` | DTO null 처리(클라이언트 DTO·worker 도메인·db-core 엔티티·통계 JSON 빈 문자열) | §4.6 |
| 5 | `fix(core-worker)` | battlelog 404 상태 관리, 갱신 경로, `PlayerRefreshService` 정렬, renewal-flow 문서 | §4.7·§4.8, §4.9 |

core-api 의 응답 스펙은 바뀌지 않는다(버전·로깅만 바뀜) — 프론트엔드 동기화 대상이 아니다.

## 4. 설계 상세

### 4.1 버전 업그레이드 (커밋 1)

| 항목 | 현재 | 변경 |
|------|------|------|
| Kotlin (`kotlinVersion`) | 2.2.21 | 2.3.21 |
| Java (`javaVersion`, toolchain) | 21 | 25 |
| Spring Boot (`springBootVersion`) | 4.0.2 | 4.1.1 |
| Gradle wrapper | 9.3.0 | 9.7.1 (`./gradlew wrapper --gradle-version 9.7.1`) |
| Spring Cloud BOM (`springCloudVersion`) | 2025.1.0 | 커밋 1 에서 2025.1.3, 커밋 3 에서 BOM 째 제거 (§4.4) |

- Spring Cloud 를 커밋 1 에서 올리는 이유: 2025.1.0 의 spring-cloud-commons 5.0.0 은 호환성 검사(`CompatibilityVerifierProperties` 기본값 `4.0.x`, 기본으로 켜짐)가 Boot 4.1.1 을 거부해 core-worker·core-batch 컨텍스트가 뜨지 않는다. 2025.1.3(commons 5.0.3)은 `4.0.x`·`4.1.x` 를 허용한다. Feign 제거를 먼저 하는 순서는 배치 메서드가 Java 24+(JEP 491)에 기대므로 택하지 않는다.
- `resilience4jVersion=2.4.0`·`kotlinLoggingVersion=8.0.4` 는 이 버전들을 처음 쓰는 커밋(2·3)에서 `gradle.properties` 에 더한다.
- `Dockerfile`: 빌더 `eclipse-temurin:21-jdk-jammy` → `25-jdk-jammy`, 런타임 `21-jre-jammy` → `25-jre-jammy`(noguesstoday 와 같은 이미지).
- `.github/workflows/pr-test.yml`: `java-version: '25'`.
- 루트 `build.gradle.kts` 의 `tasks.withType<Test> { excludeTags("develop") }` 가 `developTest` 에도 걸려 include/exclude 가 겹친다. noguesstoday 처럼 `.matching { it.name != "developTest" }` 로 제외한다. 수정 전후로 `developTest` 가 테스트를 수집하는지 실행해 확인한다.
- deprecated 정리(noguesstoday `79ac52e1` 과 같은 일):
  - `settings.gradle.kts` 의 `val xxx: String by settings` 5개 → `providers.gradleProperty("xxx").get()` (Gradle 9.6 에서 deprecated, Gradle 10 제거).
  - Kotlin 2.3 이 경고하는 불필요한 `!!` 제거(`CoreDataSourceConfig.kt:15`, `LockConfig.kt:16` 등 빌드 경고로 확인).
  - `support/monitoring/src/main/resources/monitoring.yml` 의 `management.otlp.tracing.export.enabled` 는 Boot 4.0 부터 동작하지 않는다 → `management.tracing.export.otlp.enabled` 로 바꾼다(4.1 회귀가 아니라 4.0 부터 무시되던 설정의 정리).
- Java 24+ 는 `synchronized` 구간에서 가상 스레드가 캐리어에 고정되지 않는다(JEP 491). §4.4 의 배치 메서드가 이에 기댄다.

### 4.2 kotlin-logging 전환 (커밋 2)

- `support:logging` 이 `api("io.github.oshai:kotlin-logging-jvm:${property("kotlinLoggingVersion")}")` 로 노출한다. 로그를 남기는 모듈은 `support:logging` 에 의존하고 버전을 따로 적지 않는다. 현재 의존하는 모듈은 core-api·worker·batch 뿐이라 core-admin, infrastructure 4개(db-core·redis-core·aws-sqs·aws-s3), brawlstars-client 에 의존을 추가한다(커밋 3 에서 만드는 `support:http-client` 는 만들 때 의존을 넣는다).
- 운영 코드에서 `LoggerFactory` 를 쓰는 47개 파일을 바꾼다. 규칙은 noguesstoday 와 같다.
  - 로거는 파일 최상위 `private val log = KotlinLogging.logger {}`(로거 이름은 그 파일의 클래스).
  - 메시지는 람다 안의 문자열 템플릿: `log.info { "date=$date 대상 ${targets.size}건" }`.
  - 예외는 `log.warn(ex) { "…" }`.
  - 이름을 직접 지정한 로거는 이름을 유지한다: `ImageStorageServiceExtensions.kt` 는 `KotlinLogging.logger("com.imstargg.core.admin.application.ImageStorageCleanup")`.
- core-api `ErrorLogger`(`org.slf4j.Logger` 를 받는다, LoggerFactory 를 쓰지 않아 47개 집계 밖): `of(log: KLogger)` 로 바꾸고 `{}` 템플릿 호출을 람다로 바꾼다. 호출부 `ApiExceptionHandler`·`AsyncExceptionHandler` 는 파일 최상위 KLogger 를 넘긴다. `ErrorLoggerTest` 는 logback `ListAppender` 를 붙인 logback 로거를 `toKLogger()`(kotlin-logging slf4j 확장)로 감싸 넘기도록 고친다 — 테스트에서 slf4j/logback 를 쓰는 것은 허용한다.
- slf4j import 를 막는 Konsist 테스트는 넣지 않는다(architecture-test 모듈이 따로 필요 — §7).

### 4.3 `support:http-client` (신규 모듈, 커밋 3)

noguesstoday-backend 의 `support/http-client` 를 가져온다. 패키지 `com.imstargg.support.http`.

| 파일 | 역할 |
|------|------|
| `HttpClientConfig.kt` | `ClientHttpRequestFactoryBuilderCustomizer<HttpComponentsClientHttpRequestFactoryBuilder>` 하나로 (1) HC5 자체 재시도(`DefaultHttpRequestRetryStrategy` — 429·503·일부 IOException 1회)를 끄고 (2) 커넥션 풀을 정한다(아래) |
| `http-client.yml` | `spring.http.clients.imperative.factory: http-components`, `connect-timeout: 3s`, `read-timeout: 10s`. HC5 의 read-timeout 은 소켓 무활동 시간이다 |
| `ApiLoggingInterceptor.kt` | `ClientHttpRequestInterceptor` + `HttpRequestValues.Processor`. `[BrawlStarsApi#메서드] GET /v1/... -> 404 (123ms)` 를 Api 인터페이스 FQCN 로거로 남긴다. **응답이 오면 상태와 관계없이 INFO**(RestClient 는 인터셉터 체인이 끝난 뒤 상태 예외를 만든다), **전송 예외(타임아웃·연결 실패)만 WARN**. 헤더·본문은 남기지 않는다. 그룹 설정에서 같은 인스턴스를 `requestInterceptor` 와 `httpRequestValuesProcessor` 양쪽에 등록한다(빠지면 첫 호출에서 `IllegalStateException`) |

- **커넥션 풀**: Feign 설정(전체 2000·라우트당 500)을 지우면 HC5 기본값(전체 25·라우트당 5, 임대 대기 기본 3분)이 된다. 배치 호출 5건만으로 라우트가 차고, SQS 갱신 요청이 레이트리밋 허가를 쥔 채 커넥션을 기다리며, 그 대기가 호출 로그 소요 시간에 섞인다. 그래서 `withConnectionManagerCustomizer { it.setMaxConnTotal(100).setMaxConnPerRoute(50) }` 로 늘린다 — 라우트당 50 은 core-worker 의 동시 호출 상한(배치 5 + SQS 리스너 2개 × 기본 동시 처리 10 + 새로고침 1 = 26)을 넉넉히 넘는다. 임대 대기 상한은 `withDefaultRequestConfigCustomizer` 로 `connectionRequestTimeout` 10s 를 둔다. Feign 이 켜던 TTL 900s·만료 커넥션 정리는 옮기지 않는다 — HC5 의 2초 무활동 후 stale 검사가 남고, Boot 가 정한 기본 ConnectionConfig(타임아웃)를 덮어쓰지 않기 위해서다. 이 커스터마이저는 앱 전체(worker·batch)의 자동 구성 빌더에 걸린다(core-batch `CoreApiRestClientConfig` 는 `ClientHttpRequestFactoryBuilder.detect()` 로 따로 만들어 영향이 없다).
- 의존: `spring-web`, `support:logging`, `spring-boot-restclient`(없으면 `spring.http.clients.*` 가 그룹에 조용히 적용되지 않는다), `spring-boot-http-client`, `org.apache.httpcomponents.client5:httpclient5`.
- 등록: `settings.gradle.kts` include, `Dockerfile` 의존성 캐싱 `COPY` 목록(`COPY support/http-client/build.gradle.kts ./support/http-client/`).
- core-worker·core-batch 의 `application.yml` `spring.config.import` 에 `http-client.yml` 을 `brawlstars-client.yml` 앞에 추가한다. 두 앱의 테스트 `application.yml` 은 main 을 가리므로 같은 import 를 테스트 쪽에도 넣는다(§5 배선 테스트가 운영과 같은 팩토리·타임아웃을 보도록).
- read-timeout 5s → 10s: 재현 최대치가 6.47s 라 지금의 0.2~0.3% 타임아웃은 거의 사라진다. 대신 느린 호출 한 건이 통계 축적 틱을 최대 10초 붙잡는다(배치 메서드는 모든 태그를 기다린다).

### 4.4 `brawlstars-client` 재구성 (커밋 3)

**의존성**

- 제거: `spring-cloud-starter-openfeign`, `feign-hc5`, `feign-micrometer`, `spring-cloud-starter-circuitbreaker-resilience4j`, `jackson-datatype-jsr310`(Jackson 3 는 java.time 내장). 루트 `build.gradle.kts` 의 `spring-cloud-dependencies` BOM 과 `springCloudVersion` 도 제거한다. Spring Cloud 서킷브레이커 스타터를 남긴 채 HTTP Service 그룹을 쓰면 그룹 전체에 CircuitBreaker 가 자동으로 씌워지므로(noguesstoday 주석) 반드시 함께 뺀다.
- 추가: `api("org.springframework:spring-web")`(호출부가 RestClient 예외 타입을 잡는다), `support:http-client`, `support:logging`, `spring-boot-restclient`, `io.github.resilience4j:resilience4j-spring-boot4`·`resilience4j-ratelimiter`(`resilience4jVersion`).
- core-worker·core-batch 테스트의 `io.github.openfeign:feign-core` 의존을 제거한다.

**구성 요소**

| 파일 | 변경 |
|------|------|
| `BrawlStarsApi.kt` | Feign `@FeignClient`·`@GetMapping` → `@HttpExchange`·`@GetExchange` 인터페이스(internal). 엔드포인트 12개 유지. `@SpringQueryMap PagingParam` 은 늘 빈 값이라 쿼리를 보낸 적이 없으므로 파라미터째 없앤다 |
| `request/PagingParam.kt` | 삭제 |
| `BrawlStarsClientConfig.kt` | `@EnableFeignClients` → `@ImportHttpServices(group = "brawlstars", types = [BrawlStarsApi::class])` + `RestClientHttpServiceGroupConfigurer`(baseUrl = `app.client.brawlstars.url`, 인터셉터 등록). `@EnableConfigurationProperties(BrawlStarsClientProperties::class)` 를 달아 모듈이 자기 Properties 를 스스로 등록한다(앱의 `@ConfigurationPropertiesScan` 과 같은 빈 이름이라 중복되지 않는다. 클라이언트 모듈 테스트 앱에는 스캔이 없다). `BrawlStarsClient` 빈 생성은 유지 |
| `BrawlStarsApiConfig.kt` | 삭제(Feign `RequestInterceptor` 빈) |
| `BrawlStarsApiKeyInterceptor.kt` | Feign `RequestInterceptor` → `ClientHttpRequestInterceptor`. 키 여러 개를 `AtomicInteger` 로 순환해 `Authorization: Bearer <key>` 를 붙이는 동작은 같다 |
| `BrawlStarsRateLimitInterceptor.kt` | 신규. resilience4j `RateLimiterRegistry` 의 `brawlstars` 제한기에서 요청마다 허가를 받는다(`RateLimiter.waitForPermission`). 대기 초과는 `RequestNotPermitted` 그대로 |
| `BrawlStarsApiException.kt` | 삭제(§4.5) |
| `BrawlStarsClient.kt` | 기존 메서드 10개는 시그니처·반환 타입 유지, `execute` 래핑 제거. 배치 메서드 추가 |
| `BrawlStarsClientProperties.kt` | `batchConcurrency: Int = 5` 추가 |

**인터셉터 순서** (등록 순서 = 바깥 → 안, `InterceptingClientHttpRequest` 의 `reduce(andThen)`)

1. `BrawlStarsRateLimitInterceptor` — `@RateLimiter` 애노테이션을 쓰지 않는 이유: 배치 메서드가 Client 안에서 건별 메서드를 부르는 자기 호출이라 애노테이션(aspect)이 건마다 적용되지 않는다. 인터셉터는 HTTP 요청마다 걸린다. 제한기 설정(`resilience4j.ratelimiter.instances.brawlstars`, prod 초당 10회·대기 600s)은 그대로다.
2. `ApiLoggingInterceptor` — 레이트리밋 대기를 뺀 응답 시간을 잰다(커넥션 풀을 늘려 임대 대기도 사실상 없다).
3. `BrawlStarsApiKeyInterceptor` — 로깅보다 안쪽이라 키가 로그에 남지 않는다.

**배치 메서드**

```kotlin
fun getPlayersRecentBattles(playerTags: Collection<String>): Map<String, Result<ListResponse<BattleResponse>>>
```

- 중복 태그는 하나로 합치고, 결과 맵은 입력 순서를 유지한다.
- 태그마다 가상 스레드에서 `getPlayerRecentBattles(tag)` 를 실행한다(`Executors.newVirtualThreadPerTaskExecutor()` 를 호출마다 열고 닫는다). 클라이언트 전체에서 동시에 떠 있는 배치 요청은 `Semaphore(batchConcurrency)` 로 제한한다.
- 태그별 `Exception` 은 `Result.failure` 로 담아 나머지 태그를 막지 않는다. `Error` 는 그대로 전파한다. 예외는 RestClient 예외 그대로다(§4.5).
- 모든 태그가 끝날 때까지 기다린다. 전체 제한 시간은 두지 않는다(건별 read-timeout 과 레이트리밋 대기가 상한).

**설정 (`brawlstars-client.yml`)**

- `spring.cloud.openfeign.*` 제거(풀 설정은 §4.3 으로 옮긴다).
- `app.client.brawlstars.batch-concurrency: 5` 추가(통계 축적 `player-count: 5` 와 같다).
- prod 프로필에 `logging.level.com.imstargg.client.brawlstars.BrawlStarsApi: WARN` — 초당 약 10회라 응답 로그(하루 약 86만 줄)는 남기지 않고 전송 실패(타임아웃·연결 실패, 소요 시간 포함)만 남긴다. HTTP 오류 상태(404·429·503 등)는 호출부 로그(§4.5)로 남는다.
- 클라이언트 테스트 리소스 `application.yml`: 제한기 이름 `brawlstars-api`(실제 이름과 다름)를 `brawlstars` 로 고치고 Feign 설정을 제거하고 `spring.config.import: [http-client.yml, brawlstars-client.yml]` 을 넣는다.
- `BrawlStarsClientRealTest`(`develop` 태그)의 미리 인코딩한 태그 상수(`%2388J8Q9RPV`·`%232U8CVLQQ9`)를 `#88J8Q9RPV`·`#2U8CVLQQ9` 로 바꾼다 — RestClient 는 URI 변수를 완전 인코딩해 `%` 가 `%25` 로 이중 인코딩된다(운영 코드는 `#` 태그를 넘긴다).

**재시도**: 하지 않는다(noguesstoday 재시도 ADR 과 같다). 통계 축적은 무작위 추출이라 잃은 한 건은 다른 플레이어로 메워지고, 사용자 갱신은 사용자가 다시 요청한다.

### 4.5 예외 정책과 호출부 (커밋 3)

공용 예외 계층(`BrawlStarsApiException`·`BrawlStarsApiClientException`·`BrawlStarsApiServerException`)을 없애고 RestClient 예외를 그대로 올린다.

| 지금 | 바뀐 뒤 |
|------|---------|
| `BrawlStarsApiClientException.NotFound` | `HttpClientErrorException.NotFound` |
| `BrawlStarsApiClientException.TooManyRequests` | `HttpClientErrorException.TooManyRequests` |
| `BrawlStarsApiServerException.ServiceUnavailable` | `HttpServerErrorException.ServiceUnavailable` |
| 그 밖의 `BrawlStarsApiClientException`·`ServerException` | `RestClientResponseException` (상태 코드를 로그에 남긴다) |
| 타임아웃·연결 실패 (지금은 `ClientException` 에 섞임) | `ResourceAccessException` — "API 통신 실패" 로 따로 로그 |
| 디코드 실패 (지금은 `ClientException` 에 섞임) | `RestClientException("Error while extracting response for type …")` |
| 레이트리밋 대기 초과 | resilience4j `RequestNotPermitted` |

호출부 변경:

- `StatsBattleAccumulationService` — 전용 스레드 풀(`executor`, `@PreDestroy shutdown`)을 없애고 배치 메서드를 부른다. 결과는 스케줄러 스레드에서 플레이어 순서대로 처리한다(집계 DB 쓰기는 순차 실행 — 동시 쓰기 경합이 준다).
  - 성공 → `BattleBuilder` + `statsBattleAggregator.aggregate` (플레이어별 try/catch, 실패 시 error 로그).
  - `NotFound` → §4.7.
  - `ServiceUnavailable` → 틱당 한 번만 점검 자동 등록. 한 틱의 요청은 배치로 함께 나가므로, 503 을 받은 뒤 남은 플레이어 호출을 건너뛰던 지금의 중단 동작은 없어진다(다음 틱부터 `isUnderMaintenance()` 가 호출을 막는다).
  - `RestClientResponseException` → warn `API 오류 status=...`, `ResourceAccessException` → warn `API 통신 실패`, 그 밖(디코드 실패 포함) → error `처리 실패`.
- `PlayerRenewalService` — §4.8.
- `PlayerRefreshService`·`ClubRenewalService` — catch 타입만 바꾼다. 동작은 같다(`PlayerDeactivator` 가 `stats_player` 를 지우지 않게 되는 §4.7 변경만 따라간다).
- core-batch `BrawlStarsApiTasklet` — `HttpServerErrorException.ServiceUnavailable` 을 잡아 점검으로 보고 정상 종료(동작 같음).

### 4.6 DTO null 처리 (커밋 4)

- `BattleResultBrawlerResponse.name`, `PlayerResponse.nameColor`, `ClubMemberResponse.nameColor`, `PlayerRankingResponse.nameColor` 를 `String?` 로 바꾼다. `nameColor` 는 저장 쪽(`player.name_color`, `club_member.name_color`, `RankingEntityPlayer.nameColor`·`player_name_color`)이 이미 nullable 이라 DTO 만 바꾸면 된다. 랭킹 응답은 한 항목만 null 이어도 한 페이지(최대 200건)가 통째로 실패하므로 함께 고친다.
- 브롤러 이름:
  - core-worker 도메인 `BattleTeamPlayerBrawler.name: String?`
  - `BattleTeamPlayerEntityBrawler.name: String?` (`@Column(nullable = true)`) + DDL `battle_team_player.brawler_name drop not null`(§6.1). 이 컬럼을 읽는 곳은 없다(core-api 는 `brawler` 테이블 이름을 쓴다).
  - 통계 JSON `StatsBattleEntityTeamPlayerBrawler.name` 은 non-null `String` 그대로 두고, `StatsBattleAggregateApplier.convertTeams` 에서 null 이면 `""` 로 저장한다. 이유를 주석으로 남긴다 — 이 data class 는 jackson-module-kotlin 생성자 주입으로만 읽혀 non-null 파라미터에 null 이 오면 구버전 core-batch 통계 Reader 들이 실패한다. 빈 문자열이면 배포 순서·롤백 제약이 없다.
- 숨김 브롤러가 섞인 전투도 버리지 않고 저장·집계한다. 그 결과 `brawler` 테이블에 없는 ID 의 통계 행이 생겨 core-api 가 `brawler = null` 로 내보내고 프론트엔드는 이미지 없는 `-` 행으로 그린다. 프론트엔드가 이미 null 브롤러(brawler-sync 전 신규 브롤러)를 처리하므로 의도된 동작으로 받아들인다.

### 4.7 battlelog 404 상태 관리 (통계 축적, 커밋 5)

- core-enum 에 `StatsPlayerStatus { ACTIVE, BATTLE_LOG_UNAVAILABLE }` 를 추가한다.
- `stats_player.status varchar(25) not null default 'ACTIVE'` — 상수 기본값이라 Postgres 에서 메타데이터 변경으로 끝난다(행 약 555만 개, §6.1).
- `StatsPlayerEntity`: `@Enumerated(EnumType.STRING) var status: StatsPlayerStatus = ACTIVE` (`protected set`) + `fun markBattleLogUnavailable()` — `PlayerEntity.status`/`toDeleted()` 와 같은 방식.
- 신규 `StatsPlayerStatusUpdater`(core-worker, `@Transactional`): 뽑은 `StatsPlayerEntity` 의 id 로 `findById` 해 `markBattleLogUnavailable()` 을 부른다(변경 감지로 `updated_at` 갱신). 축적 서비스는 태그→엔티티 맵으로 결과와 id 를 잇는다.
- 뽑기: `findFirstByIdGreaterThanEqualOrderByIdAsc` → `findFirstByIdGreaterThanEqualAndStatusOrderByIdAsc(id, ACTIVE)`. PK 인덱스 스캔에 status 필터가 붙는다(status 인덱스 불필요). 최대 ID 조회는 그대로.
- 통계 축적 중 `NotFound` → `StatsPlayerStatusUpdater.markBattleLogUnavailable(id)` + info 로그 `[배틀 통계 축적] battlelog 없음(404) — 수집 제외 tag=...`. `PlayerDeactivator` 는 부르지 않는다.
- 행이 남아 있으므로 `StatsPlayerSyncJobItemWriter`·`StatsPlayerSyncFromRankingTask`(기존 태그 제외 후 insert)가 다시 넣지 않는다 — 재등록·재삭제 루프가 끊긴다.
- `stats_player` 의 다른 소비처 `BrawlerItemOwnershipStatsReader` 는 status 로 거르지 않는다. 소유 통계는 `/players` 데이터 기반이고, 이 계정군(트로피 0)은 `BrawlerTrophyRange`(500 이상) 구간에 들지 않아 결과가 달라지지 않는다.
- 표시된 행은 재확인하지 않는다.
- `PlayerDeactivator` 에서 `statsPlayerRepository.deleteByBrawlStarsTag` 를 없애 player soft-delete 만 남긴다. 미사용이 되는 `StatsPlayerJpaRepository.deleteByBrawlStarsTag` 는 삭제한다. 갱신·새로고침 중 `/players` 404(진짜 삭제된 계정)의 player soft-delete 는 그대로 둔다 — core-api 가 이 상태로 410 을 돌려주는 사용자 기능이다.
- `PlayerRefreshService` 의 무작위 추출(`PlayerJpaRepository.findAllByStatusAndIdGreaterThanEqual(status, id, pageable)`, 정렬 없음)에도 §1.3 과 같은 Seq Scan 쏠림 패턴이 있다 → `findAllByStatusAndIdGreaterThanEqualOrderByIdAsc` 로 바꾼다.

### 4.8 사용자 갱신 경로 (커밋 5)

- `PlayerRenewalService`: `/players` 가 404 면 지금처럼 renewal `notFound` + `PlayerDeactivator.deactivate`.
- `/players` 는 200 인데 battlelog 가 `NotFound` 면 전투 목록을 빈 목록으로 보고 갱신을 완료한다(info 로그). 프로필(`Brawler`·트로피 0 이어도)은 갱신된다. 지금은 이 경우 계정이 삭제로 처리된다.

### 4.9 문서 갱신

| 문서 | 고칠 내용 | 커밋 |
|------|-----------|------|
| `imstargg-backend/AGENTS.md` | Java 21 → 25 | 1 |
| `imstargg-backend/AGENTS.md` | 로깅 규칙(kotlin-logging, 파일 최상위 로거, 람다 메시지, `support:logging` 의존) 추가, core-admin 의 `support:logging` 의존 반영 | 2 |
| `imstargg-backend/AGENTS.md` | brawlstars-client 설명 OpenFeign → HTTP Service Client(RestClient), support 트리에 `http-client`, 모듈 의존 관계, 클라이언트 규칙(RestClient 예외 그대로·재시도 없음·HTTP 설정은 `support:http-client`) | 3 |
| `imstargg-backend/docs/brawlstars-api-maintenance.md` | "스케줄러는 503 감지 시 나머지 플레이어 처리 중단(break)" → 통계 축적은 틱당 1회 등록만 하고 다음 틱부터 차단(§4.5), 중단은 새로고침에만 해당 | 3 |
| `client/brawlstars-client/README.md` | HTTP 설정 위치(`support:http-client`)·배치 메서드·레이트리밋 인터셉터 한 줄씩 | 3 |
| `imstargg-backend/docs/renewal-flow.md` | `EXECUTING → NOT_FOUND` 조건을 '`/players` 404' 로, battlelog 404 → `COMPLETED`(전투 0건) 추가 | 5 |

## 5. 테스트

| 대상 | 커밋 | 방식 | 검증 |
|------|------|------|------|
| 빌드·기존 테스트 | 1·2 | `./gradlew build` | 업그레이드·로깅 전환 후 전부 통과, `developTest` 수집 확인. `ErrorLoggerTest` 는 `toKLogger()` 로 고쳐 통과 |
| `BrawlStarsClient` | 3 | `MockRestServiceServer` + 수동 `HttpServiceProxyFactory` 프록시 | 태그 `#` → `%23` 인코딩, 모르는 필드 무시, `PlayerResponse` 의 `@field:JsonProperty("3vs3Victories")` 등 이름 매핑, `getApiSpec` 이 `application/octet-stream` 본문을 String 으로 받음, 404 → `HttpClientErrorException.NotFound`, 503 → `ServiceUnavailable` |
| 배치 메서드 | 3 | 단위 | 부분 실패 시 나머지 결과 유지, 동시 실행 수가 `batchConcurrency` 를 넘지 않음, 중복 태그 합침. (기존 `StatsBattleAccumulationServiceTest` 의 `한 틱의 플레이어를 동시에 수집한다` 는 동시성이 클라이언트로 옮겨가므로 지우고 이 테스트로 대신한다) |
| 인터셉터 | 3 | 단위 | 키 순환 순서, 로그에 키 없음, 응답 INFO·전송 예외 WARN |
| HTTP 배선 | 3 | core-worker 의 새 컨텍스트 테스트 베이스 — `CoreWorkerContextTest` 는 `BrawlStarsClient` 를 `@MockkBean` 으로 바꾸므로 상속하지 않는다. client 를 mock 하지 않는 별도 `@SpringBootTest` 에 `@Tag("context")` 를 직접 단다. `app.client.brawlstars.url` 은 `@DynamicPropertySource` 로 테스트용 HTTP 서버/응답 없는 소켓에 맞춘다 | 실제 `BrawlStarsClient` 공개 메서드로: (a) 응답 없는 소켓 → read-timeout 안에 `ResourceAccessException`(HC5 팩토리·타임아웃이 그룹에 닿음), (b) 스텁 서버가 `Authorization: Bearer <key>` 를 받음(키 인터셉터 등록), (c) 호출 수만큼 `RateLimiterRegistry.rateLimiter("brawlstars")` 허가가 줄어듦 — 배치 경로 포함(레이트리밋 인터셉터 등록), (d) processor 미등록이면 `ApiLoggingInterceptor` 가 첫 호출에서 실패하므로 같은 테스트가 함께 확인 |
| DTO null | 4 | 클라이언트 디코딩 + worker 단위 | 브롤러 이름 `null`(픽스처)·`nameColor` `null`(플레이어·클럽 멤버·랭킹) 디코딩, 통계 JSON 에 `""` 로 저장, `battle_team_player` 에 null 저장 |
| `StatsBattleAccumulationService` | 3·5 | 기존 테스트 수정(배치 메서드 stub 으로 교체) | 404 → 상태 `BATTLE_LOG_UNAVAILABLE`, 행 유지, player 미변경; `ACTIVE` 만 뽑힘(`StatsPlayerEntityFixture` 에 status 추가); 503 은 틱당 한 번 등록; 타임아웃은 warn 후 다음 플레이어 계속 |
| `PlayerRenewalService` | 5 | 기존 테스트 파일에 케이스 추가(지금은 집계 제출 실패 1개뿐) | battlelog 404 → 갱신 완료(전투 0건); `/players` 404 → notFound + 비활성화 |
| `PlayerRefreshService`·`ClubRenewalService` | 3 | 새 테스트(지금 테스트 파일 없음) | 예외 타입 변경 후 `/players`·`/clubs` 404 → 비활성화, 503 → 점검 등록 |
| `BrawlStarsApiTasklet` | 3 | 기존 테스트 수정 | `HttpServerErrorException.ServiceUnavailable` → 정상 종료 |

- 검증 명령: 커밋마다 `./gradlew build`(ktlint·단위·컨텍스트 테스트). CI(`pr-test.yml`)는 Testcontainers Postgres 만 있다 — 테스트가 Redis 에 직접 기대지 않게 한다.
- `BrawlStarsClientRealTest`(`develop` 태그)는 실제 키로 수동 실행해 전환 후 응답 디코딩을 확인한다(§4.4 의 태그 상수·Properties 등록 수정 후).

## 6. 배포와 검증

### 6.1 DDL 수동 적용

이 프로젝트는 Flyway 를 쓰지 않는다. 앱 배포 전에 적용하고, `schema.sql` 도 같이 고친다. 파일은 `V261020__add_deleted_at_to_brawlstars_resources.sql` 형식을 따른다 — 머리 주석에 배경·배포 순서('DDL 선적용 → 앱 배포')·멱등 방식. 두 DDL 모두 기존 앱과 호환된다(상태 기본값 `ACTIVE`, nullable 완화). 메타데이터 변경이지만 잠깐 ACCESS EXCLUSIVE 락을 잡으므로 `set lock_timeout = '3s'` 로 적용하고, 실패하면 통계 배치 시간대(02:00~09:00 KST)를 피해 다시 시도한다.

- `infrastructure/db-core/src/main/resources/db/migration/V261102a__add_status_to_stats_player.sql`: `alter table stats_player add column if not exists status varchar(25) not null default 'ACTIVE';`
- `infrastructure/db-core/src/main/resources/db/migration/V261102b__battle_team_player_brawler_name_nullable.sql`: `alter table battle_team_player alter column brawler_name drop not null;`

### 6.2 앱 배포

- core-worker·core-batch·core-api 를 Java 25 이미지로 배포한다(버전·로깅 변경이 세 앱 모두에 걸린다). 통계 JSON 이 호환되므로(§4.6) 앱 사이 배포 순서 제약은 없다. core-admin 도 같은 Dockerfile 로 빌드되므로 따로 배포한다면 함께 올린다.
- PR 단계에서 `/release` 로 세 앱의 SNAPSHOT 버전을 올린다.

### 6.3 기존 오삭제 player 복구 (배포 후 1회)

배포 후에 한다 — 새 코드는 battlelog 404 로 player 를 지우지 않으므로 복구한 행이 다시 지워지지 않는다.

1. VictoriaLogs 에서 `"[배틀 통계 축적] 플레이어 삭제됨"` 로그의 태그를 보관 기간 전체에 걸쳐 뽑는다(보관 기간보다 오래된 오삭제는 로그가 없어 복구 대상에서 빠진다 — 그 사실을 기록한다).
2. 그 태그 중 현재 `player.status = 'DELETED'` 인 행 수를 먼저 센다(규모 확인).
3. `update player set status = 'ACTIVE', updated_at = now() where brawlstars_tag in (...) and status = 'DELETED';` 를 수동 적용한다.
4. 진짜 삭제된 계정이 섞여도 괜찮다 — 그 계정은 다음 갱신 때 `/players` 404 로 다시 `DELETED` 가 된다.

### 6.4 배포 후 확인 (VictoriaLogs, 배포 후 24시간)

- `"Read timed out"` 이 거의 0 (배포 전 core-worker 하루 약 1,700건)
- `"[배틀 통계 축적] 플레이어 삭제됨"` 0 (배포 전 하루 약 4,300건). `"[플레이어 갱신] 플레이어 삭제됨"` 은 진짜 삭제 계정(`/players` 404)이라 남는다.
- `"battlelog 없음(404)"` 이 날마다 줄어듦(한 번 표시되면 다시 뽑히지 않음)
- 디코드 실패: `"Error while extracting response"` 와 `"[배틀 통계 축적] 처리 실패"` 가 0. (Feign 의 `DecodeException` 은 배포 전 기준값으로만 쓴다 — 전환 뒤에는 클래스가 없어 늘 0 이다)
- `RequestNotPermitted` 0
- 통계 적재량(id 차분)이 배포 전 하루 약 74.8만 건 수준 유지
- 이벤트 통계 화면에 숨김 브롤러 `-` 행이 보이는지 한 번 확인(§4.6 의도된 동작)

### 6.5 롤백

- DDL 이 기존 앱과 호환되므로 앱만 이전 이미지로 되돌리면 된다. 통계 JSON 은 빈 문자열이라 구버전 core-batch 가 읽을 수 있고, `battle_team_player.brawler_name` 은 구버전 앱이 읽지 않는다.
- core-worker 를 되돌리면 구버전이 `BATTLE_LOG_UNAVAILABLE` 행을 다시 뽑아 404 를 받고 지우므로 표시가 사라지고 재등록·재삭제 루프가 되살아난다(데이터 손실은 아니다). 구버전은 battlelog 404 계정을 다시 soft-delete 한다.

## 7. 범위 제외 (향후 과제)

- ranking-collect 가 타임아웃 한 건에 스텝 전체가 실패해 하루 종일 재시작을 반복하는 문제 — 건별 skip 이나 배치 메서드 적용은 따로 다룬다.
- 레이트리밋 대기 600s 동안 SQS 리스너 스레드가 묶이는 문제(2026-07-07 core-worker 설계 P1).
- slf4j import 금지 Konsist 검사(architecture-test 모듈).
- core-worker 앱 지표 수집(웹 서버가 없어 Prometheus 수집 대상이 아니다).
- core-batch `CoreApiRestClientConfig` 를 `support:http-client` 기본값으로 옮기기.
- Jackson 2(`com.fasterxml.jackson.module:jackson-module-kotlin`) 의존 정리.
- `spring-retry` 버전이 awspring BOM 의 중첩 import(spring-cloud-commons-dependencies, 2.0.12)로 관리되는 숨은 결합 — 버전 고정 또는 Spring Framework 7 `@Retryable` 이전.
- `ApiLoggingInterceptor` 가 non-2xx 응답을 WARN 으로 올릴지(404 가 하루 수천 건 WARN 이 된다).
- `BATTLE_LOG_UNAVAILABLE` 행 재확인.

PR 준비 시 처리(설계 밖): 작업 브랜치는 `origin/develop` 기준의 규칙명(예: `refactor/brawlstars-client-restclient`)으로 만들고, 아직 push 하지 않은 develop 커밋 `af60043f`(mcp 추가)를 이 PR 에 실을지 먼저 develop 에 따로 push 할지 정한다.

## 8. 위험과 대응

| 위험 | 대응 |
|------|------|
| Boot 4.1·Java 25·Kotlin 2.3 동시 업그레이드로 빌드·런타임 문제 | 커밋 1 에서 Spring Cloud 를 2025.1.3 으로 올려 호환성 검사를 통과시키고, 커밋 1 단독으로 전체 빌드·테스트 통과 확인. 운영 문제는 커밋 단위로 되짚는다 |
| Spring Cloud BOM 제거 뒤 의존 해석 변화 | 커밋 3 에서 `./gradlew :core:core-worker:dependencyInsight --dependency spring-retry`(db-core 도)로 2.0.12 가 유지되는지, spring-cloud-aws 해석이 그대로인지 확인하고 SQS 컨텍스트 테스트로 검증 |
| `spring.http.clients.*`·풀 설정이 그룹에 조용히 적용되지 않음 | `support:http-client` 가 `spring-boot-restclient` 를 의존하고, §5 HTTP 배선 테스트로 고정 |
| 커넥션 풀 고갈로 갱신 지연 | 라우트당 50·전체 100 + 임대 대기 10s 상한(§4.3) |
| Jackson 3 컨버터의 모르는 필드 처리 | 모르는 필드가 섞인 픽스처 디코딩 테스트 |
| 배치 메서드가 레이트리밋을 우회 | 제한기를 인터셉터에 두고 배치 경로 포함 배선 테스트 |
| read-timeout 10s 로 틱이 길어짐 | fixed-rate 따라잡기로 처리량은 유지될 것으로 보고, 배포 후 적재량으로 확인 |
| 통계 JSON null 로 구버전 core-batch 실패 | 통계 JSON 에는 빈 문자열 저장(§4.6) |
