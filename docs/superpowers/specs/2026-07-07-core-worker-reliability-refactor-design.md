# core-worker 신뢰성 개선 + 아키텍처 정리 설계

- 작성일: 2026-07-07
- 대상 모듈: `imstargg-backend/core/core-worker` (+ `infrastructure/db-core` 쿼리 메서드 1개, `imstargg-backend/docs` 2개 문서)
- 상태: 설계 확정 (구현 전)

## 1. 개요

core-worker 전체 코드 리뷰(소스 37개 파일, 의존 인프라, 기존 설계 문서 5개)에서 발견한
개선 후보 중, 사용자와 합의한 범위 — **신뢰성/정합성** 과 **아키텍처(DDD, 중간 깊이)** — 를
개선하는 설계다. 모든 변경은 동작 보존(behavior-preserving)을 원칙으로 하되, 아래 명시된
결함(통계 유실 경로, INSERT race 시 FAILED 처리, 같은 초 배틀 누락 엣지)은 의도적으로 동작을
교정한다.

## 2. 검토 결과 전체 요약

리뷰에서 발견한 항목과 이번 범위 채택 여부. 전반적으로 core-worker 는 갱신 상태 머신,
낙관적/비관적 락 조합, battleKey 멱등 수집, 점검 자동 등록까지 문서(`imstargg-backend/docs/`)와
코드가 잘 맞물린 모듈이며, 아래는 그 위의 개선 후보다.

### 2.1 성능/처리량 — 이번 범위 제외 (향후 과제, §8)

| # | 발견 | 내용 |
|---|------|------|
| P1 | rate limiter 장기 블로킹 | `brawlstars-client.yml` 의 `timeout-duration: 600s`. 한도 초과 시 SQS 리스너 스레드가 최대 10분 블록. visibility timeout(30s)을 넘겨 재전달 반복(낙관적 락으로 중복 처리는 방어됨), 갱신 폭주 시 리스너 스레드 고갈 가능 |
| P2 | maintenance 체커 매회 DB 조회 | `BrawlStarsApiMaintenanceChecker` 가 매 메시지·매 스케줄 틱마다 exists 쿼리. 짧은 TTL 로컬 캐시로 완화 가능 |
| P3 | 랜덤 샘플링 편향 | `PlayerRefreshService`/`StatsBattleAccumulationService` 의 maxId 기반 랜덤 시작점 방식은 id 공백·삭제 row 만큼 편향되고 중복 수집률을 계측할 수 없음 |

### 2.2 신뢰성/정합성 — 채택

| # | 발견 | 내용 | 설계 |
|---|------|------|------|
| R1 | 통계 집계 유실 경로 | `@Async` 집계는 executor 큐(10,000) 포화 시 `TaskRejectedException` 으로 유실. 그 예외가 renewal COMPLETED 이후 던져져 `fail()` 전이 시도 → `IllegalStateException` → SQS 리스너 전파 → 무의미한 재전달. 비동기 실행 중 transient DB 오류도 로그만 남고 유실 | §4.1 |
| R2 | INSERT race 시 FAILED | 신규 플레이어/클럽 최초 동시 갱신 시 유니크 충돌 → catch(Exception) → FAILED. `worker-concurrency-strategy.md` 의 "재시도는 상위 호출자(큐 재처리)에서 처리" 서술과 불일치 (메시지는 정상 ACK 되므로 큐 재처리는 일어나지 않음) | §4.2 |
| R3 | 같은 초 배틀 누락 엣지 | `PlayerUpdater.filterNewBattles` 가 `battleAt > latest` 단순 비교. 이전 수집의 최신 battleAt 과 같은 초의 미수집 배틀이 걸러질 수 있는 이론적 엣지 | §4.3 |

### 2.3 아키텍처 (DDD) — 채택 (중간 깊이)

| # | 발견 | 내용 | 설계 |
|---|------|------|------|
| A1 | application 패키지 평면화 | 17개 클래스가 한 패키지에 존재, player/club/stats/maintenance 하위 도메인 경계가 코드 구조에 드러나지 않음 | §4.5 |
| A2 | renewal 골격 중복 | `PlayerRenewalService` ↔ `ClubRenewalService` 의 상태 전이+예외 분류 골격(~70줄)이 동일. Finder/Processor 도 각각 type·deactivator 만 다른 중복 쌍 | §4.4 |
| A3 | 도메인→인프라 의존 | `BattleBuilder`(domain)가 `db-core` 의 `BattleKeyEmbedded` 에 의존 — 이번 범위(중간 깊이)에서는 보류, 향후 과제 | §8 |
| A4 | JpaRepository 접근 혼재 | worker 는 core-api 의 Application Layer Repository 컨벤션과 달리 JpaRepository 직접 주입이 혼재 — 워커 특성상 허용으로 판단, 컨벤션 명문화만 향후 과제 | §8 |

### 2.4 관측성/운영 — 이번 범위 제외 (향후 과제, §8)

| # | 발견 | 내용 |
|---|------|------|
| O1 | 커스텀 메트릭 부재 | 갱신 소요 시간(requested→completed)·실패율·rate limiter 대기·통계 유실 카운트 등. Prometheus 인프라는 이미 구성됨 |
| O2 | 문서 드리프트 | `aws-sqs.md` 큐 이름 표(`local-player-renewal`)가 실제(`imstargg-player-renewal-local`)와 다름, `stats-battle-data-strategy.md` 의 `StatsBattleHelper` 참조(실제는 `StatsBattleAggregateApplier`) 등. 이번 변경으로 낡게 되는 문서 2개만 §6 에서 갱신 |

## 3. 확정 범위와 목표

1. **통계 집계 유실 경로 제거**: `@Async` 제거(동기 전환) + 실행 격리 + transient 재시도
2. **INSERT race 를 재시도로 흡수**: 사용자 재요청 없이 1회 재시도로 성공시키고 문서와 일치화
3. **filterNewBattles 경계 보강**: 같은 초 엣지 제거
4. **renewal 골격 통합**: Finder/Processor 통합 + `RenewalWorkflow` 컴포지션 추출
5. **하위 도메인 패키지 분리**: `application/{player,club,renewal,stats,maintenance}`

비목표(non-goal): 성능 튜닝(P1–P3), 메트릭 추가(O1), 도메인→인프라 의존 방향 정리(A3),
outbox 기반 무유실 집계. §8 에 기록.

## 4. 설계

### 4.1 통계 집계 동기 전환 (R1)

```text
현재: renew() ─complete─▶ StatsBattleAggregator.asyncAggregate()   @Async (worker-task- 풀)
                             └▶ StatsBattleRetryableAggregator.aggregate()   @Retryable
                                   └▶ StatsBattleAggregateApplier.apply()    @Transactional

변경: renew() ─complete─▶ StatsBattleAggregator.aggregate()   동기 (기존 Retryable 클래스를 rename)
                             └▶ StatsBattleAggregateApplier.apply()          @Transactional (불변)
```

- **삭제**: `StatsBattleAggregator`(async 래퍼), `config/AsyncConfig`, `config/AsyncExceptionHandler`,
  `AsyncConfigTest`. `@Async` 사용처가 이 한 곳뿐이므로 비동기 인프라 전체를 제거한다.
- **rename**: `StatsBattleRetryableAggregator` → `StatsBattleAggregator`. `@Retryable` 은 유지하며
  `RetryConfig`(`@EnableRetry`)와 `spring-boot-starter-aspectj` 의존도 유지한다.
- **재시도 확대**: `retryFor = [DataIntegrityViolationException::class, TransientDataAccessException::class]`.
  select-then-insert 레이스(유니크 충돌)에 더해 락 타임아웃·순단 같은 일시 오류를 3회×100ms
  백오프로 흡수한다. 동기 전환 후 호출 스레드 점유 증가는 최대 수백 ms 수준.
- **호출 지점**: `PlayerRenewalService` 는 COMPLETED 확정 후 `afterCompleted` 훅(§4.4)에서
  `statsBattleAggregator.aggregate(savedBattles)` 를 호출한다.
  `StatsBattleAccumulationService.accumulate()` 는 rename 반영 외 변경 없음.
- **성능 판단 근거**: 처리량 병목은 rate limiter(prod 10 req/s)이지 리스너 처리 시간이 아니다.
  renewal 1건당 집계는 SELECT 2회 + batch INSERT (hibernate `batch_size: 100`) 수준으로 수십 ms.
- **허용된 유실의 명문화**: 재시도 소진 후에도 실패하면 그 배틀 묶음은 통계에 반영되지 않는다
  (배틀은 `battle` 테이블에 저장돼 재갱신 시 신규로 잡히지 않음). 해당 플레이어가
  `stats_player` 대상이면 축적 스케줄러가 7일 내 재수집할 수 있다. 이 성질을
  `StatsBattleAggregator` KDoc 과 `renewal-flow.md` 에 "error 로그를 남기는 허용된 드문 유실"로
  기록한다.

### 4.2 INSERT race 1회 재시도 (R2)

- 발생 지점: `PlayerUpdater.update()`/`ClubUpdater.update()` 트랜잭션 커밋 시 신규 row 동시
  INSERT 로 인한 유니크 충돌(`DataIntegrityViolationException`). `SELECT FOR UPDATE` 는 존재하는
  row 만 잠글 수 있어 신규 케이스에서만 발생한다.
- 처리: `RenewalWorkflow`(§4.4)가 action 실행 중 `DataIntegrityViolationException` 을 잡으면
  **action 전체를 1회 재실행**한다. 재시도 시점에는 row 가 존재하므로 비관적 락이 정상 동작한다.
- 재시도 단위를 action 전체(API 호출 포함)로 두는 이유: fetch/apply 분리는 워크플로 시그니처를
  복잡하게 만들고, 이 race 는 "신규 태그 최초 동시 갱신"이라는 희귀 케이스라 API 2콜 재소모가
  무시 가능하기 때문. 재시도 후에도 실패하면 기존과 같이 FAILED 처리한다.

### 4.3 filterNewBattles 경계 보강 (R3)

`PlayerUpdater.filterNewBattles` 를 경계 포함 + battleKey 제외 방식으로 바꾼다.

```kotlin
val latestBattleTime = battleJpaRepository
    .findTopByPlayerIdOrderByBattleAtDesc(player.id!!)?.battleAt ?: return battles
val candidates = battles.filter { !it.battleAt.isBefore(latestBattleTime) }   // >= 경계 포함
if (candidates.isEmpty()) return emptyList()
val existingKeys = battleJpaRepository
    .findAllByPlayerIdAndBattleAtGreaterThanEqual(player.id!!, latestBattleTime)
    .map { it.battleKey }.toSet()
return candidates.filter { it.battleKey !in existingKeys }
```

- `BattleJpaRepository` 에 `findAllByPlayerIdAndBattleAtGreaterThanEqual(playerId, battleAt)` 쿼리
  메서드를 추가한다. 기존 인덱스 `ix_battle__playerid_battleat` 로 커버되며, 경계 시각과 같은
  row 는 소수라 추가 비용은 무시 가능하다. DB 스키마 변경은 없다.
- 멱등성 근거: battleKey 는 `SHA-256(battleTime + sorted tags)` 라 같은 초·같은 구성 재대결이
  물리적으로 불가능한 도메인 특성상 충돌하지 않는다.

### 4.4 Renewal 골격 통합 (A2) — `application/renewal/`

동작이 완전히 동일한 중복 쌍을 통합하고, 공통 흐름을 컴포지션으로 추출한다.

| 기존 | 변경 |
|------|------|
| `PlayerRenewalFinder` + `ClubRenewalFinder` | `RenewalFinder.findByTag(tag, type)` — type 파라미터화 |
| `PlayerRenewalProcessor` + `ClubRenewalProcessor` | `RenewalProcessor` — 상태 전이 + `@Transactional`. `notFound(renewal, deactivate: () -> Unit)` 로 deactivate 를 호출부가 주입 (트랜잭션 내 실행 의미 유지) |
| 두 Service 의 골격 ~70줄×2 | `RenewalWorkflow.execute(...)` 한 곳 |

```kotlin
@Component
class RenewalWorkflow(
    private val renewalFinder: RenewalFinder,
    private val renewalProcessor: RenewalProcessor,
    private val brawlStarsApiMaintenanceChecker: BrawlStarsApiMaintenanceChecker,
    private val brawlStarsApiMaintenanceAutoRegistrar: BrawlStarsApiMaintenanceAutoRegistrar,
) {
    fun <T> execute(
        tag: String,
        type: RenewalType,
        logPrefix: String,                 // "플레이어 갱신" / "클럽 갱신" — 기존 로그 형식 보존
        onNotFound: () -> Unit,            // deactivator 호출 (NOT_FOUND 트랜잭션 내)
        afterCompleted: (T) -> Unit = {},  // COMPLETED 확정 후 부가 작업 — 예외 격리 보장
        action: () -> T,                   // API 호출 + 업데이트 본문 — DataIntegrityViolation 1회 재시도
    )
}
```

골격 순서는 기존과 동일하다:

1. `renewalFinder.findByTag(tag, type)` — 없으면 `IllegalArgumentException` (기존 동일: 리스너로
   전파되어 SQS 재전달. producer 가 항상 row 를 만들므로 실질 발생 없음)
2. `status != PENDING` → info 로그 후 종료 (중복 메시지)
3. 점검 중 → fail 처리 후 종료
4. `executing()` — `ObjectOptimisticLockingFailureException` 시 warn 로그 후 종료
5. `action()` 실행 — `DataIntegrityViolationException` 1회 재시도
6. 성공 → `complete()` — 낙관적 락 충돌 시 상태 갱신만 포기(기존 `updateRenewalStatus` 의미),
   성공 시에만 `afterCompleted(result)` 호출
7. `afterCompleted` 의 예외는 **error 로그만 남기고 삼킨다** — COMPLETED 이후의 예외가
   `fail()` 전이 시도(`IllegalStateException`)나 SQS 재전달로 번지는 기존 함정을 구조적으로 제거
8. `NotFound` → `notFound(renewal, onNotFound)` / `ServiceUnavailable` → 점검 자동 등록 + fail /
   기타 예외 → error 로그 + fail

서비스는 본문만 소유한다:

```kotlin
// PlayerRenewalService.renew(brawlStarsTag)
renewalWorkflow.execute(
    tag = brawlStarsTag,
    type = RenewalType.PLAYER,
    logPrefix = "플레이어 갱신",
    onNotFound = { playerDeactivator.deactivate(brawlStarsTag) },
    afterCompleted = { savedBattles -> statsBattleAggregator.aggregate(savedBattles) },
    action = {
        val playerResponse = brawlStarsClient.getPlayerInformation(brawlStarsTag)
        val battles = brawlStarsClient.getPlayerRecentBattles(brawlStarsTag)
            .items.map { BattleBuilder(playerResponse.tag, it).build() }
        playerUpdater.update(playerResponse, battles)
    },
)

// ClubRenewalService.renew(brawlStarsTag)
renewalWorkflow.execute(
    tag = brawlStarsTag,
    type = RenewalType.CLUB,
    logPrefix = "클럽 갱신",
    onNotFound = { clubDeactivator.deactivate(brawlStarsTag) },
    action = { clubUpdater.update(brawlStarsClient.getClub(brawlStarsTag)) },
)
```

`afterCompleted` 격리(§4.1)와 action 재시도(§4.2)가 골격 한 곳에 있으므로, 갱신 대상이
추가되어도 같은 보증을 자동으로 얻는다.

### 4.5 패키지 구조 (A1)

```text
core/worker/
├── application/
│   ├── player/       PlayerRenewalService, PlayerUpdater, PlayerRepository,
│   │                 PlayerDeactivator, PlayerRefreshService
│   ├── club/         ClubRenewalService, ClubUpdater, ClubRepository, ClubDeactivator
│   ├── renewal/      RenewalFinder, RenewalProcessor, RenewalWorkflow          ← 신규
│   ├── stats/        StatsBattleAccumulationService, StatsBattleAggregator,
│   │                 StatsBattleAggregateApplier
│   └── maintenance/  BrawlStarsApiMaintenanceChecker, BrawlStarsApiMaintenanceAutoRegistrar
├── domain/           Battle, BattleBuilder, Player, Club   (4개뿐이라 분리하지 않음)
├── consumer/         (유지)
├── schedule/         (유지)
└── config/           ClockConfig, RetryConfig, ScheduleConfig,
                      PlayerRefreshScheduleProperties, StatsBattleScheduleProperties
                      (AsyncConfig, AsyncExceptionHandler 삭제)
```

- 의존 방향: `player`/`club` → `renewal`(공통 골격) → `maintenance`. `stats` 는 `player` 와
  `schedule` 에서 사용된다. 하위 도메인 간 직접 참조는 골격을 통해서만 흐른다.
- 테스트 패키지도 동일 구조로 이동한다. 파일당 1클래스 원칙 유지.

### 4.6 에러 처리 총괄

`RenewalWorkflow` 기준. **굵은 행**만 기존 동작에서 달라진다.

| 상황 | 처리 | 결과 상태 |
|---|---|---|
| status ≠ PENDING | info 로그 후 스킵 (중복 메시지) | 불변 |
| API 점검 중 | fail 처리 | FAILED |
| EXECUTING 전환 낙관적 락 충돌 | warn 로그 후 스킵 | 불변 |
| **action 중 `DataIntegrityViolationException`** | **1회 재시도, 재실패 시 FAILED** | **COMPLETED 또는 FAILED** |
| action 중 `BrawlStarsApiClientException.NotFound` | deactivate + NOT_FOUND | NOT_FOUND |
| action 중 `BrawlStarsApiServerException.ServiceUnavailable` | 점검 자동 등록 + fail | FAILED |
| action 중 기타 예외 | error 로그 + fail | FAILED |
| **afterCompleted 예외** | **error 로그만, 상태 불변 (격리)** | **COMPLETED** |
| **통계 집계 transient 오류** | **`@Retryable` 3회×100ms 후 afterCompleted 격리로 흡수** | **COMPLETED** |

## 5. 테스트 전략

프로젝트 컨벤션 준수: 내부 데이터 계층은 실 DB(`CoreWorkerContextTest` + `CleanUp.all()`),
외부 시스템(BrawlStars client)은 MockK, 시간은 `Clock.fixed`. 베이스 클래스가 `@Tag("context")`
를 포함하므로 서브클래스에서 중복 선언하지 않는다.

| 구분 | 테스트 | 내용 |
|---|---|---|
| 신규 | `RenewalWorkflowTest` | 골격 분기 전수: PENDING 아님 스킵 / 점검 중 FAILED / NotFound / 503 자동 등록 / DataIntegrityViolation 1회 재시도 성공·재실패 / afterCompleted 예외 격리(COMPLETED 유지) |
| 이동+rename | `PlayerRenewalProcessorConcurrencyTest` → `RenewalProcessorConcurrencyTest` | 낙관적 락 시나리오 유지 |
| 보강 | `PlayerUpdaterUpdateBattlesTest` | "같은 초 신규 배틀 포함, 기존 battleKey 제외" 케이스 추가 |
| 유지(이동만) | `PlayerUpdaterConcurrencyTest`, `ClubUpdaterConcurrencyTest`, `StatsBattleAggregateApplierTest`, `PlayerUpdaterCacheEvictionTest`, `BattleBuilderTest` | 패키지 이동 외 불변 |
| 삭제 | `AsyncConfigTest` | AsyncConfig 삭제에 따름 |

검증 명령: `./gradlew :core:core-worker:test` 및 `./gradlew ktlintCheck`.
(모노레포 규칙상 API 스펙 변경이 없으므로 프론트엔드 동기화는 불필요.)

## 6. 문서 갱신

이번 변경으로 서술이 낡는 문서만 갱신한다.

- `imstargg-backend/docs/renewal-flow.md`: `statsBattleAggregator.asyncAggregate() (비동기)` →
  동기 집계 + afterCompleted 격리 서술, `RenewalWorkflow` 구조 반영
- `imstargg-backend/docs/worker-concurrency-strategy.md`: INSERT race 의 "재시도는 상위
  호출자(큐 재처리)에서 처리" → "`RenewalWorkflow` 가 1회 재시도" 로 정정, 통합 클래스명 반영

## 7. 예상 변경 파일

| 구분 | 파일 |
|---|---|
| 신규 | `application/renewal/RenewalFinder.kt`, `application/renewal/RenewalProcessor.kt`, `application/renewal/RenewalWorkflow.kt`, `RenewalWorkflowTest.kt` |
| 삭제 | `PlayerRenewalFinder.kt`, `ClubRenewalFinder.kt`, `PlayerRenewalProcessor.kt`, `ClubRenewalProcessor.kt`, `StatsBattleAggregator.kt`(async 래퍼), `config/AsyncConfig.kt`, `config/AsyncExceptionHandler.kt`, `AsyncConfigTest.kt` |
| 수정 | `PlayerRenewalService.kt`, `ClubRenewalService.kt`, `StatsBattleRetryableAggregator.kt`(→rename), `StatsBattleAccumulationService.kt`, `PlayerUpdater.kt`, `db-core .../BattleJpaRepository.kt` |
| 이동 | application/도메인별 하위 패키지로 소스·테스트 전체 |
| 문서 | `renewal-flow.md`, `worker-concurrency-strategy.md` |

버전 관리: 구현 완료 후 `/release` 스킬 규칙에 따라 core-worker SNAPSHOT patch bump
(동작 보존 리팩터링 + 신뢰성 fix).

## 8. 향후 과제 (이번 범위 제외)

- **P1**: rate limiter `timeout-duration: 600s` 재설계 — 짧은 대기 + FAILED 전환 또는 visibility
  timeout 연장과의 정합 설계
- **P2**: `BrawlStarsApiMaintenanceChecker` 짧은 TTL 로컬 캐시
- **P3**: 랜덤 샘플링(maxId 방식) 편향 개선 및 중복 수집률 계측
- **O1**: Micrometer 커스텀 메트릭 — 갱신 소요 시간(requested→completed), 실패율, rate limiter
  대기 시간, 통계 집계 실패(유실) 카운트
- **O2**: 문서 드리프트 정리 — `aws-sqs.md` 큐 이름 표, `stats-battle-data-strategy.md` 의
  `StatsBattleHelper` 참조 등
- **A3**: `BattleBuilder`(domain) → `BattleKeyEmbedded`(db-core) 의존 방향 정리
- **A4**: worker 의 JpaRepository 직접 주입 허용 여부 컨벤션 명문화
- 완전 무유실 통계가 필요해지면 outbox(저장된 배틀 기반 미반영분 수집) 방식 검토
