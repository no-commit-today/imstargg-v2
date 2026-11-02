# brawlstars-client RestClient 전환 + 기반 업그레이드 + 클라이언트 버그 수정 설계

- 작성일: 2026-11-02
- 대상: `imstargg-backend` 전체(버전·로깅), `client/brawlstars-client`, `support/http-client`(신규), `core/core-worker`, `core/core-batch`, `core/core-enum`, `infrastructure/db-core`, `Dockerfile`, `.github/workflows/pr-test.yml`
- 상태: 설계 확정 (구현 전)
- 참고 패턴: noguesstoday-backend — `support/http-client`, HTTP Service Client(`@ImportHttpServices`), 클라이언트 예외 ADR(`docs/superdomain/adr/2026-10-12-no-client-exception-hierarchy.md`), kotlin-logging 규칙(`noguesstoday-backend/AGENTS.md`)

## 1. 배경 — 운영 로그 조사 결과 (2026-11-02)

VictoriaLogs·VictoriaMetrics·운영 DB 조회와 테스트 키 재현으로 확인한 사실이다.

### 1.1 'API 클라이언트 오류' 의 실체는 read timeout

- core-worker 의 `[배틀 통계 축적] API 클라이언트 오류` 는 대부분 `feign.RetryableException: Read timed out`(read-timeout 5s)이다. 전부 `/v1/players/{tag}/battlelog` 이고, 호출당 약 0.24% 다(배포 전 약 0.14%).
- 다른 회선·클라이언트로 같은 패턴 2,000건을 재현하면 p99 3.45s·최대 6.47s, 5초 초과 0.25% 로 운영과 같다. 업스트림 꼬리 지연에 5초가 빠듯한 것이다.
- 배포(`c08e0c63`, 5명 병렬 호출) 뒤로는 같은 틱에 함께 보낸 요청이 수 ms 안에 함께 타임아웃되는 묶음이 생겼다(3.25일간 2건 묶음 321초, 3건 이상 19초). 독립 사건이라면 기대값의 6배를 넘는다.
- `BrawlStarsApiException.from` 이 상태 -1(전송 실패)과 디코드 실패를 `BrawlStarsApiClientException` 으로 분류해, 타임아웃이 "클라이언트 오류" 로 찍힌다.

### 1.2 DTO 역직렬화 실패

- 브롤러 `16000088`(`/v1/brawlers` 에 없는 숨김 브롤러)의 `name` 이 `null` 로 와서 `BattleResultBrawlerResponse.name: String` 역직렬화가 실패하고, 그 플레이어의 배틀로그 25건 전체가 버려진다(`#LQGGG` 로 재현).
- `PlayerResponse.nameColor` 가 `null` 로 와서 플레이어 갱신이 실패한 적이 있다(2026-10-25, `#800JQY2VR8`).

### 1.3 '플레이어 삭제됨' 은 대부분 살아 있는 계정

- 통계 축적은 battlelog 404 를 받으면 무조건 `PlayerDeactivator.deactivate()` 를 부른다 — player soft-delete + `stats_player` hard delete. 배포 후 하루 약 4,300건(호출의 약 0.6%)이다.
- 표본 120개 중 51% 가 2일 안에 `StatsPlayerSyncJob` 으로 다시 등록됐고, 그중 22개는 삭제 처리 이후의 전투에 참가했다. 120개 모두 battlelog 수집에 한 번도 성공하지 않았다(지속적인 404). 전투에서 찾은 10개 중 9개는 이름이 `Brawler` 이고, 앞서 `/players` 로 확인한 3개도 이름 `Brawler`·트로피 0 을 돌려줬다. API 가 battlelog 를 막아 둔 계정군으로 보인다(봇인지 가려진 실제 유저인지는 우리 쪽에서 가릴 수 없다).
- 그래서 재등록 → 뽑힘 → 404 → 삭제가 무한히 반복된다.
- 배포 직후 급증(하루 65~128건 → 약 4,300건)한 원인은 배포 전 선택 쿼리다. `findAllByIdGreaterThanEqual(randomId, PageRequest.of(0, 1))` 는 `ORDER BY` 가 없어 Seq Scan 으로 실행되고, 무작위 ID 의 약 76% 에서 같은 물리 행을 돌려줬다. 극소수 플레이어만 반복 수집하느라 404 계정군이 드러나지 않았고, 배포의 `findFirstByIdGreaterThanEqualOrderByIdAsc` 가 균등 추출로 바로잡으면서 실제 비율이 드러났다(적재량도 같은 이유로 하루 약 2.6만 → 74.8만 건, 28배).

### 1.4 구조 문제

- 동시 호출 정책(동시 개수·재시도·레이트리밋)이 `StatsBattleAccumulationService` 의 전용 스레드 풀 등 호출부에 흩어져 있다.
- Spring Cloud OpenFeign 은 2022.0 부터 기능 완료(버그 수정만) 상태이고 공식 문서가 Spring HTTP Service Client 로의 이전을 권한다. imstargg 에서 Feign·Spring Cloud 를 쓰는 곳은 `brawlstars-client` 하나뿐이다.

## 2. 결정 사항 (사용자 합의)

1. Feign 과 Spring Cloud 를 걷어내고 RestClient 기반 HTTP Service Client 로 전환한다. WebClient 는 쓰지 않는다 — 동시 요청이 많아야 수십 개이고 호출부가 전부 블로킹이라 논블로킹의 이득이 없다.
2. 동시 여러 호출은 `BrawlStarsClient` 내부가 책임진다(배치 메서드). 호출부의 스레드 풀은 없앤다.
3. HTTP 구성은 noguesstoday 패턴을 따른다(§4.3·§4.4).
4. 클라이언트 예외는 noguesstoday ADR 을 따른다 — 공용 예외 계층 없이 RestClient 예외를 그대로 올린다(§4.5).
5. 통계 축적 중 battlelog 404 는 `stats_player` 를 지우지 않고 상태 enum 으로 표시해 뽑기에서 제외한다. player soft-delete 는 하지 않는다. `stats_player` hard delete 는 어느 경로에서도 하지 않는다(§4.7).
6. 브롤러 이름 `null` 은 저장 계층까지 nullable 로 둔다(§4.6).
7. 사용자 갱신 중 `/players` 는 200 인데 battlelog 만 404 면 전투 없음으로 보고 갱신을 완료한다(§4.8).
8. 버전은 noguesstoday 와 맞추고 kotlin-logging 을 도입한다.
9. 전부 PR 하나로 내되, 커밋을 단계별로 나눈다(§3).

## 3. 범위와 커밋 순서

PR 하나 안에서 아래 순서로 커밋한다. 커밋마다 `./gradlew build` 가 통과해야 한다 — 운영에서 문제가 나면 어느 단계 탓인지 커밋 단위로 되짚기 위해서다.

| # | 커밋 | 내용 | 절 |
|---|------|------|----|
| 1 | `chore(backend)` | 버전 업그레이드 + `developTest` 태그 수정 | §4.1 |
| 2 | `refactor(backend)` | kotlin-logging 전환 | §4.2 |
| 3 | `refactor(brawlstars-client)` | Feign·Spring Cloud 제거 → HTTP Service Client, `support:http-client` 신설, 배치 메서드, 호출부 예외 변경 | §4.3~4.5 |
| 4 | `fix(backend)` | DTO null 처리(클라이언트 DTO·worker 도메인·db-core 엔티티) | §4.6 |
| 5 | `fix(core-worker)` | battlelog 404 상태 관리 + 갱신 경로 | §4.7·§4.8 |

core-api 의 응답 스펙은 바뀌지 않는다(버전·로깅만 바뀜) — 프론트엔드 동기화 대상이 아니다.

## 4. 설계 상세

### 4.1 버전 업그레이드

| 항목 | 현재 | 변경 |
|------|------|------|
| Kotlin (`kotlinVersion`) | 2.2.21 | 2.3.21 |
| Java (`javaVersion`, toolchain) | 21 | 25 |
| Spring Boot (`springBootVersion`) | 4.0.2 | 4.1.1 |
| Gradle wrapper | 9.3.0 | 9.7.1 |
| Spring Cloud BOM (`springCloudVersion`) | 2025.1.0 | 제거 (§4.4) |
| resilience4j | Spring Cloud 스타터 경유 | `resilience4jVersion=2.4.0` 직접 의존 |
| kotlin-logging | 없음 | `kotlinLoggingVersion=8.0.4` |

- `Dockerfile`: 빌더 `eclipse-temurin:21-jdk-jammy` → `25-jdk-jammy`, 런타임 `21-jre-jammy` → `25-jre-jammy`(noguesstoday 와 같은 이미지). §4.3 의 새 모듈 `support/http-client/build.gradle.kts` 를 의존성 캐싱 `COPY` 목록에 추가한다.
- `.github/workflows/pr-test.yml`: `java-version: '25'`.
- 루트 `build.gradle.kts` 의 `tasks.withType<Test> { excludeTags("develop") }` 가 `developTest` 에도 걸려 include/exclude 가 겹친다. noguesstoday 처럼 `.matching { it.name != "developTest" }` 로 제외한다. 수정 전후로 `developTest` 가 테스트를 수집하는지 실행해 확인한다.
- Java 24+ 는 `synchronized` 구간에서 가상 스레드가 캐리어에 고정되지 않는다(JEP 491). §4.4 의 배치 메서드가 이에 기댄다.

### 4.2 kotlin-logging 전환

- `support:logging` 이 `api("io.github.oshai:kotlin-logging-jvm:${property("kotlinLoggingVersion")}")` 로 노출한다. 로그를 남기는 모듈은 `support:logging` 에 의존하고 버전을 따로 적지 않는다. 현재 의존하는 모듈은 core-api·worker·batch 뿐이라 core-admin, infrastructure 4개(db-core·redis-core·aws-sqs·aws-s3), brawlstars-client, support:http-client 에 의존을 추가한다.
- 운영 코드에서 `LoggerFactory` 를 쓰는 47개 파일을 바꾼다(테스트 1개는 logback `ListAppender` 용이면 그대로 둔다). 규칙은 noguesstoday 와 같다.
  - 로거는 파일 최상위 `private val log = KotlinLogging.logger {}`(로거 이름은 그 파일의 클래스).
  - 메시지는 람다 안의 문자열 템플릿: `log.info { "date=$date 대상 ${targets.size}건" }`.
  - 예외는 `log.warn(ex) { "…" }`.
- slf4j import 를 막는 Konsist 테스트는 넣지 않는다(architecture-test 모듈이 따로 필요 — §7).

### 4.3 `support:http-client` (신규 모듈)

noguesstoday-backend 의 `support/http-client` 를 가져온다. 패키지 `com.imstargg.support.http`.

| 파일 | 역할 |
|------|------|
| `HttpClientConfig.kt` | `ClientHttpRequestFactoryBuilderCustomizer<HttpComponentsClientHttpRequestFactoryBuilder>` 로 HC5 자체 재시도(`DefaultHttpRequestRetryStrategy` — 429·503·일부 IOException 1회)를 끈다. 클라이언트는 재시도하지 않는다는 결정과 맞춘다 |
| `http-client.yml` | `spring.http.clients.imperative.factory: http-components`, `connect-timeout: 3s`, `read-timeout: 10s`. HC5 의 read-timeout 은 소켓 무활동 시간이다 |
| `ApiLoggingInterceptor.kt` | `ClientHttpRequestInterceptor` + `HttpRequestValues.Processor`. `[BrawlStarsApi#메서드] GET /v1/... -> 200 (123ms)` 를 Api 인터페이스 FQCN 로거로 남긴다(성공 INFO, 실패 WARN). 헤더·본문은 남기지 않는다. 그룹 설정에서 같은 인스턴스를 `requestInterceptor` 와 `httpRequestValuesProcessor` 양쪽에 등록한다 |

- 의존: `spring-web`, `support:logging`, `spring-boot-restclient`(없으면 `spring.http.clients.*` 가 그룹에 조용히 적용되지 않는다), `spring-boot-http-client`, `org.apache.httpcomponents.client5:httpclient5`.
- core-worker·core-batch 의 `application.yml` `spring.config.import` 에 `http-client.yml` 을 `brawlstars-client.yml` 앞에 추가한다.
- read-timeout 5s → 10s: 재현 최대치가 6.47s 라 지금의 0.2~0.3% 타임아웃은 거의 사라진다. 대신 느린 호출 한 건이 통계 축적 틱을 최대 10초 붙잡는다(배치 메서드는 모든 태그를 기다린다).
- core-batch 의 `CoreApiRestClientConfig` 는 이번에 옮기지 않는다(§7).

### 4.4 `brawlstars-client` 재구성

**의존성**

- 제거: `spring-cloud-starter-openfeign`, `feign-hc5`, `feign-micrometer`, `spring-cloud-starter-circuitbreaker-resilience4j`, `jackson-datatype-jsr310`(Jackson 3 는 java.time 내장). 루트 `build.gradle.kts` 의 `spring-cloud-dependencies` BOM 도 제거한다. Spring Cloud 서킷브레이커 스타터를 남긴 채 HTTP Service 그룹을 쓰면 그룹 전체에 CircuitBreaker 가 자동으로 씌워지므로(noguesstoday 주석) 반드시 함께 뺀다.
- 추가: `api("org.springframework:spring-web")`(호출부가 RestClient 예외 타입을 잡는다), `support:http-client`, `support:logging`, `spring-boot-restclient`, `io.github.resilience4j:resilience4j-spring-boot4`·`resilience4j-ratelimiter`(`resilience4jVersion`).
- core-worker·core-batch 테스트의 `io.github.openfeign:feign-core` 의존을 제거한다.

**구성 요소**

| 파일 | 변경 |
|------|------|
| `BrawlStarsApi.kt` | Feign `@FeignClient`·`@GetMapping` → `@HttpExchange`·`@GetExchange` 인터페이스(internal). 엔드포인트 12개 유지. `@SpringQueryMap PagingParam` 은 늘 빈 값이라 쿼리를 보낸 적이 없으므로 파라미터째 없앤다 |
| `request/PagingParam.kt` | 삭제 |
| `BrawlStarsClientConfig.kt` | `@EnableFeignClients` → `@ImportHttpServices(group = "brawlstars", types = [BrawlStarsApi::class])` + `RestClientHttpServiceGroupConfigurer`(baseUrl = `app.client.brawlstars.url`, 인터셉터 등록). `BrawlStarsClient` 빈 생성은 유지 |
| `BrawlStarsApiConfig.kt` | 삭제(Feign `RequestInterceptor` 빈) |
| `BrawlStarsApiKeyInterceptor.kt` | Feign `RequestInterceptor` → `ClientHttpRequestInterceptor`. 키 여러 개를 `AtomicInteger` 로 순환해 `Authorization: Bearer <key>` 를 붙이는 동작은 같다 |
| `BrawlStarsRateLimitInterceptor.kt` | 신규. resilience4j `RateLimiterRegistry` 의 `brawlstars` 제한기에서 요청마다 허가를 받는다(`RateLimiter.waitForPermission`). 대기 초과는 `RequestNotPermitted` 그대로 |
| `BrawlStarsApiException.kt` | 삭제(§4.5) |
| `BrawlStarsClient.kt` | 기존 메서드 10개는 시그니처·반환 타입 유지, `execute` 래핑 제거. 배치 메서드 추가 |
| `BrawlStarsClientProperties.kt` | `batchConcurrency: Int = 5` 추가 |

**인터셉터 순서** (바깥 → 안)

1. `BrawlStarsRateLimitInterceptor` — `@RateLimiter` 애노테이션을 쓰지 않는 이유: 배치 메서드가 Client 안에서 건별 메서드를 부르는 자기 호출이라 애노테이션(aspect)이 건마다 적용되지 않는다. 인터셉터는 HTTP 요청마다 걸린다. 제한기 설정(`resilience4j.ratelimiter.instances.brawlstars`, prod 초당 10회·대기 600s)은 그대로다.
2. `ApiLoggingInterceptor` — 레이트리밋 대기를 뺀 응답 시간을 잰다.
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

- `spring.cloud.openfeign.*` 제거.
- `app.client.brawlstars.batch-concurrency: 5` 추가(통계 축적 `player-count: 5` 와 같다).
- prod 프로필에 `logging.level.com.imstargg.client.brawlstars.BrawlStarsApi: WARN` — 초당 약 10회라 성공 로그(하루 약 86만 줄)는 남기지 않고 실패만 남긴다. 실패 로그의 소요 시간으로 타임아웃을 분석할 수 있다.
- 테스트 리소스 `application.yml` 의 제한기 이름 `brawlstars-api`(실제 이름과 다름)를 `brawlstars` 로 고치고 Feign 설정을 제거한다.

**재시도**: 하지 않는다(noguesstoday `2026-10-12-no-client-retry` ADR 과 같다). 통계 축적은 무작위 추출이라 잃은 한 건은 다른 플레이어로 메워지고, 사용자 갱신은 사용자가 다시 요청한다.

### 4.5 예외 정책과 호출부

공용 예외 계층(`BrawlStarsApiException`·`BrawlStarsApiClientException`·`BrawlStarsApiServerException`)을 없애고 RestClient 예외를 그대로 올린다.

| 지금 | 바뀐 뒤 |
|------|---------|
| `BrawlStarsApiClientException.NotFound` | `HttpClientErrorException.NotFound` |
| `BrawlStarsApiClientException.TooManyRequests` | `HttpClientErrorException.TooManyRequests` |
| `BrawlStarsApiServerException.ServiceUnavailable` | `HttpServerErrorException.ServiceUnavailable` |
| 그 밖의 `BrawlStarsApiClientException`·`ServerException` | `RestClientResponseException` (상태 코드를 로그에 남긴다) |
| 타임아웃·연결 실패 (지금은 `ClientException` 에 섞임) | `ResourceAccessException` — "API 통신 실패" 로 따로 로그 |
| 디코드 실패 (지금은 `ClientException` 에 섞임) | `RestClientException` |
| 레이트리밋 대기 초과 | resilience4j `RequestNotPermitted` |

호출부 변경:

- `StatsBattleAccumulationService` — 전용 스레드 풀(`executor`, `@PreDestroy shutdown`)을 없애고 배치 메서드를 부른다. 결과는 스케줄러 스레드에서 플레이어 순서대로 처리한다(집계 DB 쓰기는 순차 실행 — 동시 쓰기 경합이 준다).
  - 성공 → `BattleBuilder` + `statsBattleAggregator.aggregate` (플레이어별 try/catch, 실패 시 error 로그).
  - `NotFound` → §4.7.
  - `ServiceUnavailable` → 틱당 한 번만 점검 자동 등록(지금과 같음).
  - `RestClientResponseException` → warn `API 오류 status=...`, `ResourceAccessException` → warn `API 통신 실패`, 그 밖 → error `처리 실패`.
- `PlayerRenewalService` — §4.8.
- `PlayerRefreshService`·`ClubRenewalService` — catch 타입만 바꾼다. 동작은 같다(`PlayerDeactivator` 가 `stats_player` 를 지우지 않게 되는 §4.7 변경만 따라간다).
- core-batch `BrawlStarsApiTasklet` — `HttpServerErrorException.ServiceUnavailable` 을 잡아 점검으로 보고 정상 종료(동작 같음).

### 4.6 DTO null 처리

- `BattleResultBrawlerResponse.name`, `PlayerResponse.nameColor`, `ClubMemberResponse.nameColor` 를 `String?` 로 바꾼다. `nameColor` 는 DB 컬럼(`player.name_color`, `club_member.name_color`)과 엔티티가 이미 nullable 이라 DTO 만 바꾸면 된다.
- 브롤러 이름은 저장 계층까지 nullable 로 둔다. 저장된 브롤러 이름을 읽는 곳은 없다(core-api 는 `brawler` 테이블 이름을 쓴다).
  - core-worker 도메인 `BattleTeamPlayerBrawler.name: String?`
  - `BattleTeamPlayerEntityBrawler.name: String?` (`@Column(nullable = true)`)
  - 통계 JSON `StatsBattleEntityTeamPlayerBrawler.name: String?` (jsonb 라 DDL 없음)
  - DDL: `alter table battle_team_player alter column brawler_name drop not null;` (메타데이터 변경)
- 숨김 브롤러가 섞인 전투도 버리지 않고 저장·집계한다.

### 4.7 battlelog 404 상태 관리 (통계 축적)

- core-enum 에 `StatsPlayerStatus { ACTIVE, BATTLE_LOG_UNAVAILABLE }` 를 추가한다.
- `stats_player.status varchar(25) not null default 'ACTIVE'` — 상수 기본값이라 Postgres 에서 메타데이터 변경으로 끝난다(행 약 555만 개).
- `StatsPlayerEntity`: `@Enumerated(EnumType.STRING) var status: StatsPlayerStatus = ACTIVE` (`protected set`) + `fun markBattleLogUnavailable()` — `PlayerEntity.status`/`toDeleted()` 와 같은 방식.
- 신규 `StatsPlayerStatusUpdater`(core-worker, `@Transactional`): 태그로 엔티티를 찾아 `markBattleLogUnavailable()` 을 부른다(변경 감지로 `updated_at` 갱신).
- 뽑기: `findFirstByIdGreaterThanEqualOrderByIdAsc` → `findFirstByIdGreaterThanEqualAndStatusOrderByIdAsc(id, ACTIVE)`. 최대 ID 조회는 그대로.
- 통계 축적 중 `NotFound` → `StatsPlayerStatusUpdater.markBattleLogUnavailable(tag)` + info 로그 `[배틀 통계 축적] battlelog 없음(404) — 수집 제외 tag=...`. `PlayerDeactivator` 는 부르지 않는다.
- 행이 남아 있으므로 `StatsPlayerSyncJobItemWriter`(기존 태그 제외 후 insert)가 다시 넣지 않는다 — 재등록·재삭제 루프가 끊긴다.
- 표시된 행은 재확인하지 않는다.
- `PlayerDeactivator` 에서 `statsPlayerRepository.deleteByBrawlStarsTag` 를 없애 player soft-delete 만 남긴다. 미사용이 되는 `StatsPlayerJpaRepository.deleteByBrawlStarsTag` 는 삭제한다. 갱신·새로고침 중 `/players` 404(진짜 삭제된 계정)의 player soft-delete 는 그대로 둔다 — core-api 가 이 상태로 410 을 돌려주는 사용자 기능이다.

### 4.8 사용자 갱신 경로

- `PlayerRenewalService`: `/players` 가 404 면 지금처럼 renewal `notFound` + `PlayerDeactivator.deactivate`.
- `/players` 는 200 인데 battlelog 가 `NotFound` 면 전투 목록을 빈 목록으로 보고 갱신을 완료한다(info 로그). 프로필(`Brawler`·트로피 0 이어도)은 갱신된다. 지금은 이 경우 계정이 삭제로 처리된다.

## 5. 테스트

| 대상 | 방식 | 검증 |
|------|------|------|
| `BrawlStarsClient` | `MockRestServiceServer` + 수동 `HttpServiceProxyFactory` 프록시 | 태그 `#` → `%23` 인코딩, 브롤러 이름 `null` 디코딩(픽스처), 모르는 필드 무시, `PlayerResponse` 의 `@field:JsonProperty("3vs3Victories")` 등 이름 매핑, `getApiSpec` 이 `application/octet-stream` 본문을 String 으로 받음, 404 → `HttpClientErrorException.NotFound`, 503 → `ServiceUnavailable` |
| 배치 메서드 | 단위 | 부분 실패 시 나머지 결과 유지, 동시 실행 수가 `batchConcurrency` 를 넘지 않음, 중복 태그 합침 |
| 인터셉터 | 단위 | 키 순환 순서, 요청마다 제한기 허가 획득(배치 포함), 로그에 키 없음 |
| HTTP 설정 배선 | core-worker 컨텍스트 테스트(`CoreWorkerContextTest` 상속, `@Tag("context")` 중복 선언 금지) | 응답하지 않는 소켓에 대해 read-timeout 안에 `ResourceAccessException` — HC5 팩토리·타임아웃이 그룹에 실제로 닿는지(`MockRestServiceServer` 는 이 설정을 보지 못한다) |
| `StatsBattleAccumulationService` | 기존 테스트 수정 | 404 → 상태 `BATTLE_LOG_UNAVAILABLE`, 행 유지, player 미변경; `ACTIVE` 만 뽑힘; 503 은 틱당 한 번 등록; 타임아웃은 warn 후 다음 플레이어 계속 |
| `PlayerRenewalService` | 기존 테스트 수정 | battlelog 404 → 갱신 완료(전투 0건); `/players` 404 → notFound + 비활성화 |
| `PlayerRefreshService`·`ClubRenewalService` | 기존 테스트 수정 | 예외 타입 변경 후 동작 동일 |
| `BrawlStarsApiTasklet` | 기존 테스트 수정 | `HttpServerErrorException.ServiceUnavailable` → 정상 종료 |

- 검증 명령: 커밋마다 `./gradlew build`(ktlint·단위·컨텍스트 테스트). CI(`pr-test.yml`)는 Testcontainers Postgres 만 있다 — 테스트가 Redis 에 직접 기대지 않게 한다.
- `BrawlStarsClientRealTest`(`develop` 태그)는 실제 키로 수동 실행해 전환 후 응답 디코딩을 확인한다.

## 6. 배포와 검증

1. **DDL 수동 적용** (이 프로젝트는 Flyway 를 쓰지 않는다). 두 DDL 모두 기존 앱과 호환된다(상태 기본값 `ACTIVE`, nullable 완화). `schema.sql` 도 같이 고친다.
   - `infrastructure/db-core/src/main/resources/db/migration/V261102a__add_status_to_stats_player.sql`
   - `infrastructure/db-core/src/main/resources/db/migration/V261102b__battle_team_player_brawler_name_nullable.sql`
2. **앱 배포**: core-worker·core-batch·core-api 를 Java 25 이미지로 배포한다(버전·로깅 변경이 세 앱 모두에 걸린다). PR 단계에서 `/release` 로 세 앱의 SNAPSHOT 버전을 올린다.
3. **배포 후 확인** (VictoriaLogs, 배포 후 24시간):
   - `"Read timed out"` 이 거의 0 (배포 전 core-worker 하루 약 1,700건)
   - `"플레이어 삭제됨"` 0 (배포 전 하루 약 4,300건)
   - `"battlelog 없음(404)"` 이 날마다 줄어듦(한 번 표시되면 다시 뽑히지 않음)
   - `DecodeException`·`RequestNotPermitted` 0
   - 통계 적재량(id 차분)이 배포 전 하루 약 74.8만 건 수준 유지
4. **롤백**: DDL 이 기존 앱과 호환되므로 앱만 이전 이미지로 되돌리면 된다(이전 앱은 `status` 컬럼을 무시한다).

## 7. 범위 제외 (향후 과제)

- ranking-collect 가 타임아웃 한 건에 스텝 전체가 실패해 하루 종일 재시작을 반복하는 문제 — 건별 skip 이나 배치 메서드 적용은 따로 다룬다.
- 레이트리밋 대기 600s 동안 SQS 리스너 스레드가 묶이는 문제(2026-07-07 core-worker 설계 P1).
- slf4j import 금지 Konsist 검사(architecture-test 모듈).
- core-worker 앱 지표 수집(웹 서버가 없어 Prometheus 수집 대상이 아니다).
- core-batch `CoreApiRestClientConfig` 를 `support:http-client` 기본값으로 옮기기.
- Jackson 2(`com.fasterxml.jackson.module:jackson-module-kotlin`) 의존 정리.
- `BATTLE_LOG_UNAVAILABLE` 행 재확인.

## 8. 위험과 대응

| 위험 | 대응 |
|------|------|
| Boot 4.1·Java 25·Kotlin 2.3 동시 업그레이드로 빌드·런타임 문제 | 커밋 1 단독으로 전체 빌드·테스트 통과 확인. 운영 문제는 커밋 단위로 되짚는다 |
| Spring Cloud BOM 제거 뒤 spring-cloud-aws(`io.awspring` BOM) 의존 해석 변화 | 커밋 3 에서 `./gradlew :core:core-worker:dependencies` 로 확인하고 SQS 컨텍스트 테스트로 검증 |
| `spring.http.clients.*` 가 그룹에 조용히 적용되지 않음 | `support:http-client` 가 `spring-boot-restclient` 를 의존하고, §5 배선 컨텍스트 테스트로 고정 |
| Jackson 3 컨버터의 모르는 필드 처리 | 모르는 필드가 섞인 픽스처 디코딩 테스트 |
| 배치 메서드가 레이트리밋을 우회 | 제한기를 인터셉터에 두고 배치 경로 포함 테스트 |
| read-timeout 10s 로 틱이 길어짐 | fixed-rate 따라잡기로 처리량은 유지된다. 배포 후 적재량으로 확인 |
