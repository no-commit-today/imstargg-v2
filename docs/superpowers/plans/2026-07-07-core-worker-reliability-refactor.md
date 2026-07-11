# core-worker 신뢰성 개선 + 아키텍처 정리 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** core-worker 의 통계 집계 유실 경로 제거(동기 전환+격리), INSERT race 재시도, filterNewBattles 경계 보강, Renewal Finder/Processor 통합, 하위 도메인 패키지 분리.

**Architecture:** 스펙 `docs/superpowers/specs/2026-07-07-core-worker-reliability-refactor-design.md` 를 구현한다. `@Async` 통계 집계를 동기 전환하고 호출부에서 예외를 격리한다. INSERT race 는 Updater 의 `@Retryable`(트랜잭션 바깥 재시도)로 흡수한다. Player/Club 중복 클래스 쌍(Finder/Processor)을 `application/renewal/` 로 통합하고, application 패키지를 하위 도메인(player/club/renewal/stats/maintenance)으로 나눈다. `RenewalWorkflow`(골격 추출)는 스펙 §4.4 결정 기록에 따라 도입하지 않는다.

**Tech Stack:** Spring Boot 4 + Kotlin, Spring Retry(`@Retryable`), JPA(QueryDSL), JUnit5 + AssertJ + springmockk, ktlint.

---

## 사전 조건

- 모든 gradle 명령은 `imstargg-backend/` 디렉터리에서 실행한다.
- context 테스트(`@Tag("context")`, 모든 `CoreWorkerContextTest` 서브클래스)는 로컬 Docker 인프라가 필요하다:

```bash
cd imstargg-backend && docker-compose up -d   # PostgreSQL, Valkey, floci
```

- 테스트 베이스 클래스 `CoreWorkerContextTest` 는 이미 `@Tag("context")` 와 `@MockkBean BrawlStarsClient` 를 포함한다. **서브클래스에서 `@Tag("context")` 를 중복 선언하지 않는다.**
- 커밋 메시지는 Conventional Commits + 한국어 요약, `Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>` 트레일러를 붙인다.

## 파일 구조 (최종 상태)

```
imstargg-backend/core/core-worker/src/main/kotlin/com/imstargg/core/worker/
├── application/
│   ├── player/       PlayerRenewalService, PlayerUpdater, PlayerRepository,
│   │                 PlayerDeactivator, PlayerRefreshService
│   ├── club/         ClubRenewalService, ClubUpdater, ClubRepository, ClubDeactivator
│   ├── renewal/      RenewalFinder, RenewalProcessor                  ← Task 4 신규
│   ├── stats/        StatsBattleAccumulationService, StatsBattleAggregator(rename),
│   │                 StatsBattleAggregateApplier
│   └── maintenance/  BrawlStarsApiMaintenanceChecker, BrawlStarsApiMaintenanceAutoRegistrar
├── domain/           Battle, BattleBuilder, Player, Club              (이동 없음)
├── consumer/ schedule/                                                 (import 만 갱신)
└── config/           ClockConfig, RetryConfig, ScheduleConfig, *Properties
                      (AsyncConfig, AsyncExceptionHandler 삭제)
```

패키지 간 참조: `player`/`club` → `renewal`·`maintenance`, `player` → `stats`(갱신 후 집계),
`stats` → `player`(`PlayerDeactivator`). stats↔player 는 패키지 수준 상호 참조지만 빈 그래프에는
순환이 없어(각 클래스의 생성자 의존은 단방향) 수용한다.

---

### Task 1: 통계 집계 동기 전환 + 예외 격리 (스펙 §4.1)

**Files:**
- Delete: `imstargg-backend/core/core-worker/src/main/kotlin/com/imstargg/core/worker/application/StatsBattleAggregator.kt` (async 래퍼)
- Delete: `imstargg-backend/core/core-worker/src/main/kotlin/com/imstargg/core/worker/config/AsyncConfig.kt`
- Delete: `imstargg-backend/core/core-worker/src/main/kotlin/com/imstargg/core/worker/config/AsyncExceptionHandler.kt`
- Delete: `imstargg-backend/core/core-worker/src/test/kotlin/com/imstargg/core/worker/config/AsyncConfigTest.kt`
- Rename: `.../application/StatsBattleRetryableAggregator.kt` → `.../application/StatsBattleAggregator.kt`
- Modify: `.../application/PlayerRenewalService.kt`
- Modify: `imstargg-backend/core/core-worker/src/test/resources/application.yml` (`spring.task` 블록 제거)
- Create: `imstargg-backend/core/core-worker/src/test/kotlin/com/imstargg/core/worker/application/PlayerRenewalServiceTest.kt`

`StatsBattleAccumulationService` 는 수정 불필요 — 참조 타입명(`StatsBattleAggregator`)과 메서드명(`aggregate`)이 rename 후에도 동일하다.

- [ ] **Step 1: async 인프라 삭제 + Retryable rename**

```bash
cd imstargg-backend
git rm core/core-worker/src/main/kotlin/com/imstargg/core/worker/application/StatsBattleAggregator.kt
git rm core/core-worker/src/main/kotlin/com/imstargg/core/worker/config/AsyncConfig.kt
git rm core/core-worker/src/main/kotlin/com/imstargg/core/worker/config/AsyncExceptionHandler.kt
git rm core/core-worker/src/test/kotlin/com/imstargg/core/worker/config/AsyncConfigTest.kt
git mv core/core-worker/src/main/kotlin/com/imstargg/core/worker/application/StatsBattleRetryableAggregator.kt \
       core/core-worker/src/main/kotlin/com/imstargg/core/worker/application/StatsBattleAggregator.kt
```

rename 된 `StatsBattleAggregator.kt` 전체를 다음으로 교체:

```kotlin
package com.imstargg.core.worker.application

import com.imstargg.core.worker.domain.Battle
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.TransientDataAccessException
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component

/**
 * 통계 배틀 집계 진입점 (동기). select-then-insert 레이스(유니크 충돌)와 일시적 DB 오류를
 * 짧은 재시도로 흡수한다. 재시도 소진 후의 예외는 호출측이 격리한다 — renewal 경로는
 * COMPLETED 유지 + error 로그(허용된 드문 유실, 대상 플레이어가 stats_player 에 있으면
 * 축적 스케줄러가 7일 내 재수집 가능), 축적 스케줄러 경로는 per-player catch.
 */
@Component
class StatsBattleAggregator(
    private val statsBattleAggregateApplier: StatsBattleAggregateApplier,
) {
    @Retryable(
        retryFor = [DataIntegrityViolationException::class, TransientDataAccessException::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 100),
    )
    fun aggregate(battles: List<Battle>) {
        statsBattleAggregateApplier.apply(battles)
    }
}
```

- [ ] **Step 2: PlayerRenewalService 를 동기 호출로 전환 (격리는 아직 넣지 않음 — RED 확인용)**

`PlayerRenewalService.kt` 의 renew() 내 해당 라인만 변경:

```kotlin
// 변경 전
            if (updateRenewalStatus(renewal) { playerRenewalProcessor.complete(it) }) {
                statsBattleAggregator.asyncAggregate(savedBattles)
            }
// 변경 후
            if (updateRenewalStatus(renewal) { playerRenewalProcessor.complete(it) }) {
                statsBattleAggregator.aggregate(savedBattles)
            }
```

- [ ] **Step 3: test yml 정리 + 기존 테스트 회귀 확인**

`core/core-worker/src/test/resources/application.yml` 에서 `spring.task:` 블록(execution 하위 전체, `thread-name-prefix` 부터 `await-termination-period: 30s` 까지)을 삭제한다. `@Async` 사용처가 사라져 해당 executor 설정은 더 이상 쓰이지 않는다. `lifecycle`/`server.shutdown` 블록은 `CoreWorkerApplicationTest` 가 검증하므로 유지.

```bash
./gradlew :core:core-worker:test
```

Expected: BUILD SUCCESSFUL (기존 테스트 전부 통과)

- [ ] **Step 4: 격리 검증 실패 테스트 작성**

Create `core/core-worker/src/test/kotlin/com/imstargg/core/worker/application/PlayerRenewalServiceTest.kt`:

```kotlin
package com.imstargg.core.worker.application

import com.imstargg.CoreWorkerContextTest
import com.imstargg.client.brawlstars.BrawlStarsClient
import com.imstargg.client.brawlstars.response.BrawlerStatResponse
import com.imstargg.client.brawlstars.response.ListResponse
import com.imstargg.client.brawlstars.response.PlayerClubResponse
import com.imstargg.client.brawlstars.response.PlayerIconResponse
import com.imstargg.client.brawlstars.response.PlayerResponse
import com.imstargg.core.enums.RenewalStatus
import com.imstargg.core.enums.RenewalType
import com.imstargg.infrastructure.db.core.renewal.repository.RenewalJpaRepository
import com.imstargg.infrastructure.db.core.test.CleanUp
import com.imstargg.infrastructure.db.core.test.fixture.RenewalEntityFixture.Companion.aRenewalEntity
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class PlayerRenewalServiceTest(
    private val sut: PlayerRenewalService,
    private val brawlStarsClient: BrawlStarsClient,
    private val renewalRepository: RenewalJpaRepository,
    private val cleanUp: CleanUp,
) : CoreWorkerContextTest() {
    @MockkBean
    private lateinit var statsBattleAggregator: StatsBattleAggregator

    @AfterEach
    fun tearDown() {
        cleanUp.all()
    }

    @Test
    fun `통계 집계가 실패해도 renewal 은 COMPLETED 를 유지한다`() {
        // given
        val tag = "#RENEW1"
        val renewal = aRenewalEntity().withBrawlStarsTag(tag).build()
        renewal.toPending()
        renewalRepository.save(renewal)

        every { brawlStarsClient.getPlayerInformation(tag) } returns createPlayerResponse(tag)
        every { brawlStarsClient.getPlayerRecentBattles(tag) } returns
            ListResponse(items = emptyList(), paging = null)
        every { statsBattleAggregator.aggregate(any()) } throws IllegalStateException("집계 실패")

        // when
        sut.renew(tag)

        // then
        val saved = renewalRepository.findByBrawlStarsTagAndType(tag, RenewalType.PLAYER)!!
        assertThat(saved.status).isEqualTo(RenewalStatus.COMPLETED)
    }

    private fun createPlayerResponse(tag: String) =
        PlayerResponse(
            tag = tag,
            name = "Test",
            nameColor = "0xff0000ff",
            icon = PlayerIconResponse(id = 1L),
            trophies = 1000,
            highestTrophies = 1200,
            expLevel = 50,
            expPoints = 5000,
            isQualifiedFromChampionshipChallenge = false,
            victories3vs3 = 10,
            victoriesSolo = 5,
            victoriesDuo = 3,
            bestRoboRumbleTime = 0,
            bestTimeAsBigBrawler = 0,
            totalPrestigeLevel = 0,
            club = PlayerClubResponse(tag = "#CLUBTEST", name = "Test Club"),
            brawlers = emptyList<BrawlerStatResponse>(),
        )
}
```

참고: `brawlStarsClient` 는 베이스 클래스의 `@MockkBean` 이 컨텍스트 빈을 mock 으로 교체하므로 생성자 주입으로 그 mock 인스턴스를 받는다.

- [ ] **Step 5: 테스트 실행 — 실패 확인**

```bash
./gradlew :core:core-worker:test --tests "com.imstargg.core.worker.application.PlayerRenewalServiceTest"
```

Expected: FAIL — `renew()` 안에서 집계 예외가 generic catch 로 흘러 COMPLETED 상태에 `fail()` 전이를 시도, `IllegalStateException: Cannot transition to FAILED from COMPLETED` 가 테스트까지 전파된다.

- [ ] **Step 6: 격리 구현**

`PlayerRenewalService.kt` 에서 Step 2 의 호출부를 헬퍼로 교체하고 헬퍼를 추가한다:

```kotlin
            if (updateRenewalStatus(renewal) { playerRenewalProcessor.complete(it) }) {
                aggregateStats(brawlStarsTag, savedBattles)
            }
```

클래스 하단(`updateRenewalStatus` 위)에 추가:

```kotlin
    /**
     * 통계 집계는 renewal COMPLETED 확정 이후의 부가 작업이다. 예외가 전파되면 완료된 renewal 에
     * fail 전이를 시도(IllegalStateException)하고 SQS 재전달로 이어지므로 로그만 남기고 삼킨다.
     * 재시도(@Retryable) 소진 후의 실패는 허용된 드문 유실이다 — 대상 플레이어가 stats_player 에
     * 있으면 축적 스케줄러가 7일 내 재수집할 수 있다.
     */
    private fun aggregateStats(
        brawlStarsTag: String,
        battles: List<Battle>,
    ) {
        try {
            statsBattleAggregator.aggregate(battles)
        } catch (ex: Exception) {
            log.error("[플레이어 갱신] 통계 집계 실패 (renewal 은 COMPLETED 유지) tag={}", brawlStarsTag, ex)
        }
    }
```

import 추가: `import com.imstargg.core.worker.domain.Battle`

- [ ] **Step 7: 테스트 실행 — 통과 확인**

```bash
./gradlew :core:core-worker:test
```

Expected: BUILD SUCCESSFUL (신규 테스트 포함 전부 통과)

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -m "fix(core-worker): 통계 집계 동기 전환으로 유실 경로 제거

@Async 집계는 executor 큐 포화 시 TaskRejectedException 으로 유실되고,
COMPLETED 이후 예외가 fail 전이 시도로 이어지는 함정이 있었다.
동기 전환 + 호출부 격리로 두 경로를 제거하고 async 인프라
(AsyncConfig/AsyncExceptionHandler/async 래퍼)를 삭제한다.
재시도 대상에 TransientDataAccessException 을 추가한다.

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 2: 신규 태그 INSERT race 를 @Retryable 로 흡수 (스펙 §4.2)

**Files:**
- Modify: `imstargg-backend/core/core-worker/src/main/kotlin/com/imstargg/core/worker/application/PlayerUpdater.kt`
- Modify: `imstargg-backend/core/core-worker/src/main/kotlin/com/imstargg/core/worker/application/ClubUpdater.kt`
- Test: `.../src/test/kotlin/com/imstargg/core/worker/application/PlayerUpdaterConcurrencyTest.kt`
- Test: `.../src/test/kotlin/com/imstargg/core/worker/application/ClubUpdaterConcurrencyTest.kt`

- [ ] **Step 1: PlayerUpdaterConcurrencyTest 기대값 갱신 (실패 테스트)**

`PlayerUpdaterConcurrencyTest.kt` 의 첫 테스트를 다음으로 교체 (메서드명·then 블록 변경):

```kotlin
    @Test
    fun `동시에 같은 태그로 처음 update하면 재시도로 모두 성공하고 하나만 저장된다`() {
        // given
        val tag = "#PLAYER1"
        val barrier = CyclicBarrier(2)
        val executor = Executors.newFixedThreadPool(2)

        // when
        val futures =
            (1..2).map {
                executor.submit<Exception?> {
                    barrier.await()
                    try {
                        sut.update(createPlayerResponse(tag), emptyList())
                        null
                    } catch (e: Exception) {
                        e
                    }
                }
            }
        val results = futures.map { it.get(10, TimeUnit.SECONDS) }
        executor.shutdown()

        // then
        assertThat(results).allMatch { it == null }
        assertThat(playerRepository.count()).isEqualTo(1)
    }
```

- [ ] **Step 2: 실행 — 실패 확인**

```bash
./gradlew :core:core-worker:test --tests "com.imstargg.core.worker.application.PlayerUpdaterConcurrencyTest"
```

Expected: FAIL — `@Retryable` 이 없어 한 스레드가 `DataIntegrityViolationException` 을 반환, `allMatch { it == null }` 위배.

- [ ] **Step 3: PlayerUpdater.update 에 @Retryable 추가**

`PlayerUpdater.kt` 의 `update` 메서드 어노테이션을 다음으로 변경:

```kotlin
    /**
     * 신규 플레이어 동시 INSERT race(유니크 충돌) 는 1회 재시도로 흡수한다. Spring Retry
     * 인터셉터가 트랜잭션 인터셉터 바깥에 적용되어 재시도마다 새 트랜잭션이 열리고,
     * 재시도 시점에는 row 가 존재해 SELECT FOR UPDATE 가 정상 동작한다.
     */
    @Retryable(retryFor = [DataIntegrityViolationException::class], maxAttempts = 2)
    @Transactional
    fun update(
```

import 추가:

```kotlin
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.retry.annotation.Retryable
```

`updatePlayer()` 에는 붙이지 않는다 (refresh 경로는 기존 플레이어만 대상 — 스펙 §4.2).

- [ ] **Step 4: 실행 — 통과 확인**

```bash
./gradlew :core:core-worker:test --tests "com.imstargg.core.worker.application.PlayerUpdaterConcurrencyTest"
```

Expected: PASS (4개 테스트 전부)

- [ ] **Step 5: ClubUpdaterConcurrencyTest 기대값 갱신 (실패 테스트)**

`ClubUpdaterConcurrencyTest.kt` 의 첫 테스트를 동일 패턴으로 교체:

```kotlin
    @Test
    fun `동시에 같은 태그로 처음 update하면 재시도로 모두 성공하고 하나만 저장된다`() {
        // given
        val tag = "#CLUB1"
        val barrier = CyclicBarrier(2)
        val executor = Executors.newFixedThreadPool(2)

        // when
        val futures =
            (1..2).map {
                executor.submit<Exception?> {
                    barrier.await()
                    try {
                        sut.update(createClubResponse(tag))
                        null
                    } catch (e: Exception) {
                        e
                    }
                }
            }
        val results = futures.map { it.get(10, TimeUnit.SECONDS) }
        executor.shutdown()

        // then
        assertThat(results).allMatch { it == null }
        assertThat(clubRepository.count()).isEqualTo(1)
    }
```

- [ ] **Step 6: 실행 — 실패 확인**

```bash
./gradlew :core:core-worker:test --tests "com.imstargg.core.worker.application.ClubUpdaterConcurrencyTest"
```

Expected: FAIL (첫 테스트)

- [ ] **Step 7: ClubUpdater.update 에 @Retryable 추가**

`ClubUpdater.kt` 전체를 다음으로 교체:

```kotlin
package com.imstargg.core.worker.application

import com.imstargg.client.brawlstars.response.ClubResponse
import com.imstargg.core.worker.domain.Club
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class ClubUpdater(
    private val clubRepository: ClubRepository,
) {
    /**
     * 신규 클럽 동시 INSERT race(유니크 충돌) 는 1회 재시도로 흡수한다. Spring Retry
     * 인터셉터가 트랜잭션 인터셉터 바깥에 적용되어 재시도마다 새 트랜잭션이 열리고,
     * 재시도 시점에는 row 가 존재해 SELECT FOR UPDATE 가 정상 동작한다.
     */
    @Retryable(retryFor = [DataIntegrityViolationException::class], maxAttempts = 2)
    @Transactional
    fun update(clubResponse: ClubResponse) {
        val club =
            clubRepository.findWithLockByBrawlStarsTag(clubResponse.tag)
                ?: Club.create(clubResponse)
        club.update(clubResponse)
        clubRepository.save(club)
    }
}
```

- [ ] **Step 8: 실행 — 통과 확인**

```bash
./gradlew :core:core-worker:test
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 9: Commit**

```bash
git add -A
git commit -m "fix(core-worker): 신규 플레이어·클럽 INSERT race 를 @Retryable 재시도로 흡수

SELECT FOR UPDATE 는 존재하는 row 만 잠글 수 있어 신규 태그 동시 갱신 시
유니크 충돌로 FAILED 처리되던 것을, 새 트랜잭션 1회 재시도로 성공시킨다.
API 응답은 이미 확보된 상태라 재호출 없이 DB 갱신만 재시도된다.

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 3: filterNewBattles 같은 초 경계 보강 (스펙 §4.3)

**Files:**
- Modify: `imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/brawlstars/battle/repository/BattleJpaRepository.kt`
- Modify: `imstargg-backend/core/core-worker/src/main/kotlin/com/imstargg/core/worker/application/PlayerUpdater.kt`
- Test: `.../src/test/kotlin/com/imstargg/core/worker/application/PlayerUpdaterUpdateBattlesTest.kt`

- [ ] **Step 1: 실패 테스트 작성**

`PlayerUpdaterUpdateBattlesTest.kt` 의 `createBattle` 헬퍼에 battleKey 파라미터를 추가:

```kotlin
    private fun createBattle(
        playerBrawlStarsTag: String,
        battleTime: Instant,
        battleKey: String = "battle_key_${battleTime.epochSecond}",
    ) = Battle(
        battleKey = battleKey,
        battleAt = battleTime,
```

(나머지 필드는 기존 그대로. 기존 테스트 3개는 기본값으로 동작이 불변이다.)

테스트 추가:

```kotlin
    @Test
    fun `최신_배틀과_같은_시각의_미저장_배틀도_저장한다`() {
        // given
        val tag = "#PLAYER4"
        val player = playerRepository.save(aPlayerEntity().withBrawlStarsTag(tag).build())

        val boundaryTime = Instant.parse("2024-01-01T12:00:00Z")
        battleRepository.save(createBattleEntity(player, boundaryTime))

        val battles =
            listOf(
                // 같은 초에 종료됐지만 아직 저장되지 않은 다른 배틀
                createBattle(tag, boundaryTime, battleKey = "battle_key_boundary_other"),
                // 이미 저장된 배틀과 같은 키 — 중복 저장 금지
                createBattle(tag, boundaryTime),
                createBattle(tag, Instant.parse("2024-01-01T13:00:00Z")),
            )

        // when
        val result = sut.update(createPlayerResponse(tag), battles)

        // then
        assertThat(result).hasSize(2)
        assertThat(result.map { it.battleKey }).containsExactlyInAnyOrder(
            "battle_key_boundary_other",
            "battle_key_${Instant.parse("2024-01-01T13:00:00Z").epochSecond}",
        )
        assertThat(battleRepository.count()).isEqualTo(3)
    }
```

- [ ] **Step 2: 실행 — 실패 확인**

```bash
./gradlew :core:core-worker:test --tests "com.imstargg.core.worker.application.PlayerUpdaterUpdateBattlesTest"
```

Expected: FAIL — 현재 로직은 `battleAt > latest` 만 통과시켜 `result` 가 1건(13:00)이 된다.

- [ ] **Step 3: 리포지토리 메서드 추가**

`BattleJpaRepository.kt` 인터페이스에 추가:

```kotlin
    fun findAllByPlayerIdAndBattleAtGreaterThanEqual(
        playerId: Long,
        battleAt: Instant,
    ): List<BattleEntity>
```

import 추가: `import java.time.Instant`

(기존 인덱스 `ix_battle__playerid_battleat` 로 커버 — DB 스키마 변경 없음.)

- [ ] **Step 4: filterNewBattles 교체**

`PlayerUpdater.kt` 의 `filterNewBattles` 를 다음으로 교체:

```kotlin
    /**
     * 최신 저장 배틀과 같은 초(경계)의 배틀도 후보에 포함하고, battleKey 로 이미 저장된
     * 것을 제외한다. battleAt 단순 비교(>)만으로는 같은 초에 걸친 미저장 배틀이 누락된다.
     */
    private fun filterNewBattles(
        player: PlayerEntity,
        battles: List<Battle>,
    ): List<Battle> {
        val latestBattleTime =
            battleJpaRepository
                .findTopByPlayerIdOrderByBattleAtDesc(player.id!!)
                ?.battleAt
                ?: return battles
        val candidates = battles.filter { !it.battleAt.isBefore(latestBattleTime) }
        if (candidates.isEmpty()) {
            return emptyList()
        }
        val existingKeys =
            battleJpaRepository
                .findAllByPlayerIdAndBattleAtGreaterThanEqual(player.id!!, latestBattleTime)
                .map { it.battleKey }
                .toSet()
        return candidates.filter { it.battleKey !in existingKeys }
    }
```

- [ ] **Step 5: 실행 — 통과 확인**

```bash
./gradlew :core:core-worker:test --tests "com.imstargg.core.worker.application.PlayerUpdaterUpdateBattlesTest"
```

Expected: PASS (기존 3개 + 신규 1개)

- [ ] **Step 6: 모듈 전체 테스트 + Commit**

```bash
./gradlew :core:core-worker:test
git add -A
git commit -m "fix(core-worker): 같은 초 경계 배틀 누락 없도록 filterNewBattles 보강

battleAt > latest 단순 비교는 최신 배틀과 같은 초의 미저장 배틀을 걸렀다.
경계 포함(>=) 후보에서 battleKey 로 기저장분을 제외하는 방식으로 교체.
BattleJpaRepository 에 playerId+battleAt 조회 메서드 추가 (기존 인덱스 커버).

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 4: RenewalFinder/RenewalProcessor 통합 (스펙 §4.4)

**Files:**
- Create: `imstargg-backend/core/core-worker/src/main/kotlin/com/imstargg/core/worker/application/renewal/RenewalFinder.kt`
- Create: `imstargg-backend/core/core-worker/src/main/kotlin/com/imstargg/core/worker/application/renewal/RenewalProcessor.kt`
- Modify: `.../application/PlayerRenewalService.kt`, `.../application/ClubRenewalService.kt`
- Move+Modify: `.../src/test/kotlin/com/imstargg/core/worker/application/PlayerRenewalProcessorConcurrencyTest.kt` → `.../application/renewal/RenewalProcessorConcurrencyTest.kt`
- Delete: `PlayerRenewalFinder.kt`, `ClubRenewalFinder.kt`, `PlayerRenewalProcessor.kt`, `ClubRenewalProcessor.kt`

- [ ] **Step 1: RenewalFinder 생성**

```kotlin
package com.imstargg.core.worker.application.renewal

import com.imstargg.core.enums.RenewalType
import com.imstargg.infrastructure.db.core.renewal.RenewalEntity
import com.imstargg.infrastructure.db.core.renewal.repository.RenewalJpaRepository
import org.springframework.stereotype.Component

@Component
class RenewalFinder(
    private val renewalRepository: RenewalJpaRepository,
) {
    fun findByTag(
        brawlStarsTag: String,
        type: RenewalType,
    ): RenewalEntity =
        renewalRepository.findByBrawlStarsTagAndType(brawlStarsTag, type)
            ?: throw IllegalArgumentException("Renewal not found for tag: $brawlStarsTag, type: $type")
}
```

- [ ] **Step 2: RenewalProcessor 생성**

```kotlin
package com.imstargg.core.worker.application.renewal

import com.imstargg.infrastructure.db.core.renewal.RenewalEntity
import com.imstargg.infrastructure.db.core.renewal.repository.RenewalJpaRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

/**
 * Renewal 상태 전이 트랜잭션 경계. Player/Club 공통 — 유일한 차이였던 NOT_FOUND 시
 * 비활성화는 deactivate 액션으로 호출부가 주입한다 (상태 전이와 같은 트랜잭션에서 실행).
 */
@Component
class RenewalProcessor(
    private val renewalRepository: RenewalJpaRepository,
    private val clock: Clock,
) {
    @Transactional
    fun executing(renewal: RenewalEntity): RenewalEntity {
        renewal.toExecuting(clock)
        return renewalRepository.save(renewal)
    }

    @Transactional
    fun complete(renewal: RenewalEntity): RenewalEntity {
        renewal.toCompleted(clock)
        return renewalRepository.save(renewal)
    }

    @Transactional
    fun notFound(
        renewal: RenewalEntity,
        deactivate: () -> Unit,
    ): RenewalEntity {
        deactivate()
        renewal.toNotFound(clock)
        return renewalRepository.save(renewal)
    }

    @Transactional
    fun fail(renewal: RenewalEntity): RenewalEntity {
        renewal.toFailed(clock)
        return renewalRepository.save(renewal)
    }
}
```

- [ ] **Step 3: PlayerRenewalService 전환**

`PlayerRenewalService.kt` 전체를 다음으로 교체 (Task 1 의 `aggregateStats` 포함, finder/processor 를 통합 클래스로 교체, `playerDeactivator` 주입 추가):

```kotlin
package com.imstargg.core.worker.application

import com.imstargg.client.brawlstars.BrawlStarsApiClientException
import com.imstargg.client.brawlstars.BrawlStarsApiServerException
import com.imstargg.client.brawlstars.BrawlStarsClient
import com.imstargg.core.enums.RenewalStatus
import com.imstargg.core.enums.RenewalType
import com.imstargg.core.worker.application.renewal.RenewalFinder
import com.imstargg.core.worker.application.renewal.RenewalProcessor
import com.imstargg.core.worker.domain.Battle
import com.imstargg.core.worker.domain.BattleBuilder
import com.imstargg.infrastructure.db.core.renewal.RenewalEntity
import org.slf4j.LoggerFactory
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.stereotype.Service

@Service
class PlayerRenewalService(
    private val brawlStarsClient: BrawlStarsClient,
    private val renewalFinder: RenewalFinder,
    private val renewalProcessor: RenewalProcessor,
    private val playerUpdater: PlayerUpdater,
    private val playerDeactivator: PlayerDeactivator,
    private val statsBattleAggregator: StatsBattleAggregator,
    private val brawlStarsApiMaintenanceChecker: BrawlStarsApiMaintenanceChecker,
    private val brawlStarsApiMaintenanceAutoRegistrar: BrawlStarsApiMaintenanceAutoRegistrar,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun renew(brawlStarsTag: String) {
        var renewal = renewalFinder.findByTag(brawlStarsTag, RenewalType.PLAYER)

        if (renewal.status != RenewalStatus.PENDING) {
            log.info("[플레이어 갱신] 이미 처리됨 tag={}, status={}", brawlStarsTag, renewal.status)
            return
        }

        if (brawlStarsApiMaintenanceChecker.isUnderMaintenance()) {
            updateRenewalStatus(renewal) { renewalProcessor.fail(it) }
            log.info("[플레이어 갱신] API 점검 중 tag={}", brawlStarsTag)
            return
        }

        try {
            renewal = renewalProcessor.executing(renewal)
        } catch (ex: ObjectOptimisticLockingFailureException) {
            log.warn("[플레이어 갱신] 중복 처리 감지 tag={}", brawlStarsTag, ex)
            return
        }

        try {
            val playerResponse = brawlStarsClient.getPlayerInformation(brawlStarsTag)
            val battleResponses = brawlStarsClient.getPlayerRecentBattles(brawlStarsTag).items
            val battles = battleResponses.map { BattleBuilder(playerResponse.tag, it).build() }

            val savedBattles = playerUpdater.update(playerResponse, battles)

            if (updateRenewalStatus(renewal) { renewalProcessor.complete(it) }) {
                aggregateStats(brawlStarsTag, savedBattles)
            }
        } catch (ex: BrawlStarsApiClientException.NotFound) {
            updateRenewalStatus(renewal) {
                renewalProcessor.notFound(it) { playerDeactivator.deactivate(brawlStarsTag) }
            }
        } catch (ex: BrawlStarsApiServerException.ServiceUnavailable) {
            log.warn("[플레이어 갱신] ServiceUnavailable tag={}", brawlStarsTag, ex)
            brawlStarsApiMaintenanceAutoRegistrar.register()
            updateRenewalStatus(renewal) { renewalProcessor.fail(it) }
        } catch (ex: Exception) {
            log.error("[플레이어 갱신] 실패 tag={}", brawlStarsTag, ex)
            updateRenewalStatus(renewal) { renewalProcessor.fail(it) }
        }
    }

    /**
     * 통계 집계는 renewal COMPLETED 확정 이후의 부가 작업이다. 예외가 전파되면 완료된 renewal 에
     * fail 전이를 시도(IllegalStateException)하고 SQS 재전달로 이어지므로 로그만 남기고 삼킨다.
     * 재시도(@Retryable) 소진 후의 실패는 허용된 드문 유실이다 — 대상 플레이어가 stats_player 에
     * 있으면 축적 스케줄러가 7일 내 재수집할 수 있다.
     */
    private fun aggregateStats(
        brawlStarsTag: String,
        battles: List<Battle>,
    ) {
        try {
            statsBattleAggregator.aggregate(battles)
        } catch (ex: Exception) {
            log.error("[플레이어 갱신] 통계 집계 실패 (renewal 은 COMPLETED 유지) tag={}", brawlStarsTag, ex)
        }
    }

    private fun updateRenewalStatus(
        renewal: RenewalEntity,
        action: (RenewalEntity) -> Unit,
    ): Boolean =
        try {
            action(renewal)
            true
        } catch (ex: ObjectOptimisticLockingFailureException) {
            log.warn("[플레이어 갱신] 갱신 상태 업데이트 중 버전 충돌 tag={}", renewal.brawlStarsTag, ex)
            false
        }
}
```

- [ ] **Step 4: ClubRenewalService 전환**

`ClubRenewalService.kt` 전체를 다음으로 교체:

```kotlin
package com.imstargg.core.worker.application

import com.imstargg.client.brawlstars.BrawlStarsApiClientException
import com.imstargg.client.brawlstars.BrawlStarsApiServerException
import com.imstargg.client.brawlstars.BrawlStarsClient
import com.imstargg.core.enums.RenewalStatus
import com.imstargg.core.enums.RenewalType
import com.imstargg.core.worker.application.renewal.RenewalFinder
import com.imstargg.core.worker.application.renewal.RenewalProcessor
import com.imstargg.infrastructure.db.core.renewal.RenewalEntity
import org.slf4j.LoggerFactory
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.stereotype.Service

@Service
class ClubRenewalService(
    private val brawlStarsClient: BrawlStarsClient,
    private val renewalFinder: RenewalFinder,
    private val renewalProcessor: RenewalProcessor,
    private val clubUpdater: ClubUpdater,
    private val clubDeactivator: ClubDeactivator,
    private val brawlStarsApiMaintenanceChecker: BrawlStarsApiMaintenanceChecker,
    private val brawlStarsApiMaintenanceAutoRegistrar: BrawlStarsApiMaintenanceAutoRegistrar,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun renew(brawlStarsTag: String) {
        var renewal = renewalFinder.findByTag(brawlStarsTag, RenewalType.CLUB)

        if (renewal.status != RenewalStatus.PENDING) {
            log.info("[클럽 갱신] 이미 처리됨 tag={}, status={}", brawlStarsTag, renewal.status)
            return
        }

        if (brawlStarsApiMaintenanceChecker.isUnderMaintenance()) {
            updateRenewalStatus(renewal) { renewalProcessor.fail(it) }
            log.info("[클럽 갱신] API 점검 중 tag={}", brawlStarsTag)
            return
        }

        try {
            renewal = renewalProcessor.executing(renewal)
        } catch (ex: ObjectOptimisticLockingFailureException) {
            log.warn("[클럽 갱신] 중복 처리 감지 tag={}", brawlStarsTag, ex)
            return
        }

        try {
            val clubResponse = brawlStarsClient.getClub(brawlStarsTag)
            clubUpdater.update(clubResponse)
            updateRenewalStatus(renewal) { renewalProcessor.complete(it) }
        } catch (ex: BrawlStarsApiClientException.NotFound) {
            updateRenewalStatus(renewal) {
                renewalProcessor.notFound(it) { clubDeactivator.deactivate(brawlStarsTag) }
            }
        } catch (ex: BrawlStarsApiServerException.ServiceUnavailable) {
            log.warn("[클럽 갱신] ServiceUnavailable tag={}", brawlStarsTag, ex)
            brawlStarsApiMaintenanceAutoRegistrar.register()
            updateRenewalStatus(renewal) { renewalProcessor.fail(it) }
        } catch (ex: Exception) {
            log.error("[클럽 갱신] 실패 tag={}", brawlStarsTag, ex)
            updateRenewalStatus(renewal) { renewalProcessor.fail(it) }
        }
    }

    private fun updateRenewalStatus(
        renewal: RenewalEntity,
        action: (RenewalEntity) -> Unit,
    ): Boolean =
        try {
            action(renewal)
            true
        } catch (ex: ObjectOptimisticLockingFailureException) {
            log.warn("[클럽 갱신] 갱신 상태 업데이트 중 버전 충돌 tag={}", renewal.brawlStarsTag, ex)
            false
        }
}
```

- [ ] **Step 5: 동시성 테스트 이동+전환**

```bash
mkdir -p core/core-worker/src/test/kotlin/com/imstargg/core/worker/application/renewal
git mv core/core-worker/src/test/kotlin/com/imstargg/core/worker/application/PlayerRenewalProcessorConcurrencyTest.kt \
       core/core-worker/src/test/kotlin/com/imstargg/core/worker/application/renewal/RenewalProcessorConcurrencyTest.kt
```

이동한 파일에서 다음만 변경 (시나리오·본문 유지):

```kotlin
package com.imstargg.core.worker.application.renewal
// ...(기존 import 유지)...

class RenewalProcessorConcurrencyTest(
    private val sut: RenewalProcessor,
    private val renewalRepository: RenewalJpaRepository,
    private val cleanUp: CleanUp,
) : CoreWorkerContextTest() {
```

- [ ] **Step 6: 구 클래스 삭제**

```bash
git rm core/core-worker/src/main/kotlin/com/imstargg/core/worker/application/PlayerRenewalFinder.kt \
       core/core-worker/src/main/kotlin/com/imstargg/core/worker/application/ClubRenewalFinder.kt \
       core/core-worker/src/main/kotlin/com/imstargg/core/worker/application/PlayerRenewalProcessor.kt \
       core/core-worker/src/main/kotlin/com/imstargg/core/worker/application/ClubRenewalProcessor.kt
```

- [ ] **Step 7: 실행 — 통과 확인**

```bash
./gradlew :core:core-worker:test
```

Expected: BUILD SUCCESSFUL (동작 보존 리팩터링 — `RenewalProcessorConcurrencyTest` 포함 전부 통과)

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -m "refactor(core-worker): Player/Club Renewal Finder·Processor 통합

type 상수·deactivator 주입만 다른 100% 중복 클래스 쌍을
application/renewal/ 의 RenewalFinder(type 파라미터)와
RenewalProcessor(notFound 에 deactivate 액션 주입)로 통합.
renew() 골격은 각 서비스가 소유 (RenewalWorkflow 추출은 스펙 결정 기록에 따라 기각).

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 5: application 하위 도메인 패키지 분리 (스펙 §4.5)

**Files:** main/test 소스 이동 + package 선언·import 갱신 (코드 로직 변경 없음)

- [ ] **Step 1: main 소스 이동**

```bash
cd imstargg-backend
BASE=core/core-worker/src/main/kotlin/com/imstargg/core/worker/application
mkdir -p $BASE/player $BASE/club $BASE/stats $BASE/maintenance
git mv $BASE/PlayerRenewalService.kt $BASE/PlayerUpdater.kt $BASE/PlayerRepository.kt \
       $BASE/PlayerDeactivator.kt $BASE/PlayerRefreshService.kt $BASE/player/
git mv $BASE/ClubRenewalService.kt $BASE/ClubUpdater.kt $BASE/ClubRepository.kt \
       $BASE/ClubDeactivator.kt $BASE/club/
git mv $BASE/StatsBattleAccumulationService.kt $BASE/StatsBattleAggregator.kt \
       $BASE/StatsBattleAggregateApplier.kt $BASE/stats/
git mv $BASE/BrawlStarsApiMaintenanceChecker.kt $BASE/BrawlStarsApiMaintenanceAutoRegistrar.kt \
       $BASE/maintenance/
```

- [ ] **Step 2: test 소스 이동**

```bash
TBASE=core/core-worker/src/test/kotlin/com/imstargg/core/worker/application
mkdir -p $TBASE/player $TBASE/club $TBASE/stats
git mv $TBASE/PlayerUpdaterConcurrencyTest.kt $TBASE/PlayerUpdaterUpdateBattlesTest.kt \
       $TBASE/PlayerUpdaterCacheEvictionTest.kt $TBASE/PlayerRenewalServiceTest.kt $TBASE/player/
git mv $TBASE/ClubUpdaterConcurrencyTest.kt $TBASE/club/
git mv $TBASE/StatsBattleAggregateApplierTest.kt $TBASE/stats/
```

- [ ] **Step 3: package 선언·import 갱신**

각 이동 파일의 `package` 선언을 새 경로로 바꾸고, 패키지가 갈라진 참조에 import 를 추가한다.

package 선언 (main):
- `player/` 5개 파일 → `package com.imstargg.core.worker.application.player`
- `club/` 4개 파일 → `package com.imstargg.core.worker.application.club`
- `stats/` 3개 파일 → `package com.imstargg.core.worker.application.stats`
- `maintenance/` 2개 파일 → `package com.imstargg.core.worker.application.maintenance`

package 선언 (test): 위와 동일한 규칙으로 `...application.player` / `...application.club` / `...application.stats`

추가할 import (같은 패키지가 아니게 된 참조):
- `player/PlayerRenewalService.kt`: `com.imstargg.core.worker.application.maintenance.BrawlStarsApiMaintenanceAutoRegistrar`, `...maintenance.BrawlStarsApiMaintenanceChecker`, `com.imstargg.core.worker.application.stats.StatsBattleAggregator` (renewal.* import 는 기존 유지)
- `player/PlayerRefreshService.kt`: `...maintenance.BrawlStarsApiMaintenanceAutoRegistrar`, `...maintenance.BrawlStarsApiMaintenanceChecker`
- `club/ClubRenewalService.kt`: `...maintenance.BrawlStarsApiMaintenanceAutoRegistrar`, `...maintenance.BrawlStarsApiMaintenanceChecker` (renewal.* 유지)
- `stats/StatsBattleAccumulationService.kt`: `com.imstargg.core.worker.application.player.PlayerDeactivator`, `...maintenance.BrawlStarsApiMaintenanceAutoRegistrar`, `...maintenance.BrawlStarsApiMaintenanceChecker`
- `consumer/PlayerRenewalMessageConsumer.kt`: `com.imstargg.core.worker.application.player.PlayerRenewalService`
- `consumer/ClubRenewalMessageConsumer.kt`: `com.imstargg.core.worker.application.club.ClubRenewalService`
- `schedule/PlayerRefreshScheduler.kt`: `com.imstargg.core.worker.application.player.PlayerRefreshService`
- `schedule/StatsBattleAccumulationScheduler.kt`: `com.imstargg.core.worker.application.stats.StatsBattleAccumulationService`
- test 파일들: 각자의 sut·헬퍼 참조가 같은 패키지로 함께 이동했으므로 package 선언 변경 외 추가 import 는 대부분 불필요. `player/PlayerRenewalServiceTest.kt` 만 `com.imstargg.core.worker.application.stats.StatsBattleAggregator` import 추가.

- [ ] **Step 4: 린트 + 전체 테스트**

```bash
./gradlew ktlintFormat :core:core-worker:test
```

Expected: BUILD SUCCESSFUL. (ktlintFormat 이 import 정렬을 정리한다. 컴파일 오류가 나면 Step 3 의 import 누락 — 오류 메시지의 미해결 참조를 해당 패키지로 import 하면 된다.)

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "refactor(core-worker): application 하위 도메인 패키지 분리

player/club/renewal/stats/maintenance 로 분리해 하위 도메인 경계를
패키지 구조로 드러낸다. 파일 이동 + package/import 갱신만 — 로직 불변.

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 6: 문서 현행화 (스펙 §6)

**Files:**
- Modify: `imstargg-backend/docs/renewal-flow.md`
- Modify: `imstargg-backend/docs/worker-concurrency-strategy.md`

- [ ] **Step 1: renewal-flow.md 갱신**

흐름도(§3)의 해당 라인 교체:

```text
(변경 전)  │   └─ statsBattleAggregator.asyncAggregate() (비동기)
(변경 후)  │   └─ statsBattleAggregator.aggregate() (동기 — 예외 격리, 실패해도 COMPLETED 유지)
```

§3 끝(§4 앞)에 문단 추가:

```markdown
통계 집계는 COMPLETED 확정 이후의 부가 작업이다. 집계 예외는 `PlayerRenewalService` 가 error
로그만 남기고 삼킨다 — 완료된 renewal 에 FAILED 전이를 시도해 SQS 재전달로 번지는 것을 막기
위한 격리다. 재시도(`@Retryable`, 유니크 충돌·일시 오류 대상) 소진 후의 실패는 허용된 드문
유실이며, 해당 플레이어가 `stats_player` 대상이면 축적 스케줄러가 7일 내 재수집할 수 있다.
```

- [ ] **Step 2: worker-concurrency-strategy.md 갱신**

§2.2 의 재시도 문단 교체:

```markdown
(변경 전)
이 경우 `uk_player__brawlstarstag` 유니크 제약 위반으로 한쪽이 `DataIntegrityViolationException`을 발생시키고 트랜잭션이 롤백됩니다. 재시도는 상위 호출자(큐 재처리)에서 처리하며, 다음 시도에서는 이미 row가 존재하므로 `SELECT FOR UPDATE`가 정상 동작합니다.

이 상황은 신규 플레이어가 처음 등록될 때만 발생하며 빈도가 낮아 수용 가능한 트레이드오프로 판단했습니다.

(변경 후)
이 경우 `uk_player__brawlstarstag` 유니크 제약 위반으로 한쪽이 `DataIntegrityViolationException`을 발생시키고 트랜잭션이 롤백됩니다. `PlayerUpdater.update()`/`ClubUpdater.update()`에 선언된 `@Retryable(retryFor = [DataIntegrityViolationException::class], maxAttempts = 2)`이 새 트랜잭션으로 1회 재시도하며(Spring Retry 인터셉터가 트랜잭션 인터셉터 바깥에 적용), 재시도 시점에는 이미 row가 존재하므로 `SELECT FOR UPDATE`가 정상 동작합니다. API 응답 객체는 이미 확보된 상태라 BrawlStars API 재호출 없이 DB 갱신만 재시도됩니다.

이 상황은 신규 플레이어/클럽이 처음 등록될 때만 발생하며, 재시도 1회로 흡수됩니다.
```

§2.3 관련 코드에서 `PlayerRenewalService` 항목의 `asyncAggregate()` 언급을 갱신:

```markdown
(변경 전) - `PlayerRenewalService`: 배틀 빌드 및 `statsBattleAggregator.asyncAggregate()` 호출 (트랜잭션 바깥)
(변경 후) - `PlayerRenewalService`: 배틀 빌드 및 `statsBattleAggregator.aggregate()` 호출 (트랜잭션 바깥, COMPLETED 후 예외 격리)
```

§4.3 관련 코드를 통합 클래스명으로 갱신:

```markdown
(변경 전)
- `RenewalEntity`: `@Version` 필드
- `PlayerRenewalService.renew()`: `executing()` 호출 시 `ObjectOptimisticLockingFailureException` catch
- `ClubRenewalService.renew()`: 동일 패턴

(변경 후)
- `RenewalEntity`: `@Version` 필드
- `RenewalProcessor` (application/renewal): Player/Club 공통 상태 전이 + 트랜잭션 경계. NOT_FOUND 시 비활성화는 호출부가 deactivate 액션으로 주입
- `PlayerRenewalService.renew()` / `ClubRenewalService.renew()`: `executing()` 호출 시 `ObjectOptimisticLockingFailureException` catch (동일 패턴)
```

- [ ] **Step 3: Commit**

```bash
git add imstargg-backend/docs
git commit -m "docs(core-worker): renewal 흐름·동시성 문서 현행화

통계 집계 동기 전환+격리, INSERT race @Retryable 재시도,
RenewalFinder/RenewalProcessor 통합을 반영.

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 7: 최종 검증

- [ ] **Step 1: 백엔드 린트 + 관련 모듈 테스트**

```bash
cd imstargg-backend
./gradlew ktlintCheck :core:core-worker:test :infrastructure:db-core:test
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 2: 스펙 대비 확인**

스펙 §4.6 에러 처리 총괄표의 굵은 행 3개가 모두 구현·테스트되었는지 확인:
- 유니크 충돌 재시도 → Task 2 (동시성 테스트)
- COMPLETED 후 집계 예외 격리 → Task 1 (`PlayerRenewalServiceTest`)
- 집계 transient 재시도 → Task 1 (`StatsBattleAggregator` retryFor)

**참고 (계획 범위 밖):** API 스펙 변경이 없으므로 프론트엔드 동기화는 불필요하다. core-worker 버전 bump(patch) 는 PR 준비 시 `/release` 스킬이 처리한다.
