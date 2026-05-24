# Statistics Repository 리팩터링 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Statistics Service 의 슬롯/분모 계산 책임을 Repository 로 이관하고, `GameSlot`/`GameSlots` 도메인 객체와 `GameSlotRepository` 를 도입한다. 캐시 단위를 Statistics 객체로 교체한다.

**Architecture:** `BattleResultStatisticsRepository` 가 도메인 Statistics 객체를 직접 반환하고 `@Transactional(readOnly = true)` 안에서 분모 계산까지 묶는다. `GameSlotRepository` 는 `BattleEventRepositoryWithCache` 만 의존하는 얇은 래퍼. Service 는 `dateRange → LocalDate` 정규화 + Repository 위임만 한다.

**Tech Stack:** Kotlin, Spring Boot, Spring Cache (`@Cacheable`), JPA, AssertJ, JUnit 5, MockK.

**Spec:** `docs/superpowers/specs/2026-05-21-statistics-repository-refactor-design.md`

---

## File Structure

### 신규 생성

- `core/core-api/src/main/kotlin/com/imstargg/core/domain/statistics/GameSlot.kt`
- `core/core-api/src/main/kotlin/com/imstargg/core/domain/statistics/GameSlots.kt`
- `core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/GameSlotRepository.kt`
- `core/core-api/src/test/kotlin/com/imstargg/core/domain/statistics/GameSlotTest.kt`
- `core/core-api/src/test/kotlin/com/imstargg/core/domain/statistics/GameSlotsTest.kt`
- `core/core-api/src/test/kotlin/com/imstargg/core/application/statistics/GameSlotRepositoryTest.kt`
- `core/core-api/src/test/kotlin/com/imstargg/core/application/statistics/BattleResultStatisticsRepositoryTest.kt`
- `core/core-api/src/test/kotlin/com/imstargg/core/application/statistics/BattleRankStatisticsRepositoryTest.kt`

### 수정

- `core/core-api/src/main/kotlin/com/imstargg/core/domain/statistics/BrawlerResultStatistics.kt` — `create(...)` 시그니처
- `core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BattleResultStatisticsRepository.kt` — Statistics 반환 메서드로 재구성
- `core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BattleResultStatisticsRepositoryWithCache.kt` — 캐시 키/시그니처 교체
- `core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BattleResultStatisticsService.kt` — 슬림화
- `core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BattleRankStatisticsRepository.kt`
- `core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BattleRankStatisticsRepositoryWithCache.kt`
- `core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BattleRankStatisticsService.kt`
- `core/core-api/src/test/kotlin/com/imstargg/core/domain/statistics/BrawlerResultStatisticsTest.kt` — 입력 타입 교체

### 삭제

- `core/core-api/src/main/kotlin/com/imstargg/core/domain/statistics/SlotsPerGame.kt`
- `core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/RawCounts.kt`
- `core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/EventTotalSlotSum.kt`
- `core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BrawlerPairRankCounts.kt`
- `core/core-api/src/test/kotlin/com/imstargg/core/domain/statistics/SlotsPerGameTest.kt`

---

## 사전 준비

작업 디렉토리는 `imstargg-backend`. 모든 명령은 거기서 실행.

```bash
cd /Users/chodyoungrae/Projects/imstargg-v2/imstargg-backend
git status            # clean 상태 확인
git checkout -b refactor/statistics-repository
```

테스트 빠른 실행:
- 단위: `./gradlew :core:core-api:unitTest --tests "<FQCN>"`
- context (실 DB): `./gradlew :core:core-api:contextTest --tests "<FQCN>"`
- 전체: `./gradlew build`

---

## Task 1: GameSlot 도메인 객체 + 테스트

**Files:**
- Create: `core/core-api/src/main/kotlin/com/imstargg/core/domain/statistics/GameSlot.kt`
- Create: `core/core-api/src/test/kotlin/com/imstargg/core/domain/statistics/GameSlotTest.kt`

- [ ] **Step 1: 실패 테스트 작성**

`core/core-api/src/test/kotlin/com/imstargg/core/domain/statistics/GameSlotTest.kt`:

```kotlin
package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.event.BattleEventMode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class GameSlotTest {
    @Nested
    inner class ResultSlotsPerGame {
        @Test
        fun `3v3 은 6 슬롯`() {
            assertThat(GameSlot(listOf(3, 3)).resultSlotsPerGame).isEqualTo(6)
        }

        @Test
        fun `솔로 쇼다운 10팀은 10 슬롯`() {
            assertThat(GameSlot(List(10) { 1 }).resultSlotsPerGame).isEqualTo(10)
        }

        @Test
        fun `비대칭 1v5 는 6 슬롯`() {
            assertThat(GameSlot(listOf(1, 5)).resultSlotsPerGame).isEqualTo(6)
        }
    }

    @Nested
    inner class PairSlotsPerGame {
        @Test
        fun `3v3 은 12 페어 슬롯`() {
            assertThat(GameSlot(listOf(3, 3)).pairSlotsPerGame).isEqualTo(12)
        }

        @Test
        fun `솔로 쇼다운 10팀은 페어가 0`() {
            assertThat(GameSlot(List(10) { 1 }).pairSlotsPerGame).isEqualTo(0)
        }

        @Test
        fun `1v5 비대칭은 20 페어 슬롯`() {
            assertThat(GameSlot(listOf(1, 5)).pairSlotsPerGame).isEqualTo(20)
        }
    }

    @Nested
    inner class EnemySlotsPerGame {
        @Test
        fun `3v3 은 18 매치업 슬롯`() {
            assertThat(GameSlot(listOf(3, 3)).enemySlotsPerGame).isEqualTo(18)
        }

        @Test
        fun `솔로 쇼다운 10팀은 90 매치업 슬롯`() {
            assertThat(GameSlot(List(10) { 1 }).enemySlotsPerGame).isEqualTo(90)
        }

        @Test
        fun `1v5 비대칭은 10 매치업 슬롯`() {
            assertThat(GameSlot(listOf(1, 5)).enemySlotsPerGame).isEqualTo(10)
        }
    }

    @Nested
    inner class Default {
        @Test
        fun `default 는 BattleEventMode 의 DEFAULT_PLAYER_COUNTS 와 일치`() {
            assertThat(GameSlot.default().playerCounts).isEqualTo(BattleEventMode.DEFAULT_PLAYER_COUNTS)
        }
    }
}
```

- [ ] **Step 2: 컴파일 실패 확인**

Run: `./gradlew :core:core-api:compileTestKotlin`
Expected: `unresolved reference: GameSlot` 등 컴파일 에러.

- [ ] **Step 3: GameSlot 구현**

`core/core-api/src/main/kotlin/com/imstargg/core/domain/statistics/GameSlot.kt`:

```kotlin
package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.event.BattleEventMode

/**
 * 한 게임당 각 통계 엔티티에 누적되는 슬롯 수.
 *
 * 각 Aggregator 의 집계 로직과 일치해야 한다:
 *  - Result: `BrawlerBattleResultStatsAggregator` 가 양 팀 모든 슬롯을 카운트 → `Σ nᵢ`
 *  - Pair:   `BrawlerPairBattleResultStatsAggregator` 가 한 팀 내 방향성 페어를 카운트 → `Σ nᵢ(nᵢ−1)`
 *  - Enemy:  `BrawlerEnemyBattleResultStatsAggregator` 가 모든 적팀 양방향 매치업을 카운트 → `(Σ nᵢ)² − Σ nᵢ²`
 *
 * 환산: `gameCount = totalSlotSum / GameSlot.<x>SlotsPerGame`
 */
data class GameSlot(val playerCounts: List<Int>) {
    val resultSlotsPerGame: Int
        get() = playerCounts.sum()
    val pairSlotsPerGame: Int
        get() = playerCounts.sumOf { it * (it - 1) }
    val enemySlotsPerGame: Int
        get() {
            val total = playerCounts.sum()
            return total * total - playerCounts.sumOf { it * it }
        }

    companion object {
        fun default(): GameSlot = GameSlot(BattleEventMode.DEFAULT_PLAYER_COUNTS)
    }
}
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `./gradlew :core:core-api:unitTest --tests "com.imstargg.core.domain.statistics.GameSlotTest"`
Expected: BUILD SUCCESSFUL, 10 tests passed.

- [ ] **Step 5: ktlint 통과 확인**

Run: `./gradlew :core:core-api:ktlintCheck`
Expected: 통과 (실패 시 `./gradlew :core:core-api:ktlintFormat` 후 재확인)

- [ ] **Step 6: 커밋**

```bash
git add core/core-api/src/main/kotlin/com/imstargg/core/domain/statistics/GameSlot.kt \
        core/core-api/src/test/kotlin/com/imstargg/core/domain/statistics/GameSlotTest.kt
git commit -m "feat(core-api): GameSlot 도메인 값 객체 추가"
```

---

## Task 2: GameSlots 도메인 객체 + 테스트

**Files:**
- Create: `core/core-api/src/main/kotlin/com/imstargg/core/domain/statistics/GameSlots.kt`
- Create: `core/core-api/src/test/kotlin/com/imstargg/core/domain/statistics/GameSlotsTest.kt`

- [ ] **Step 1: 실패 테스트 작성**

`core/core-api/src/test/kotlin/com/imstargg/core/domain/statistics/GameSlotsTest.kt`:

```kotlin
package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class GameSlotsTest {
    @Test
    fun `등록된 eventId 는 해당 GameSlot 을 반환`() {
        // given
        val eventId = BrawlStarsId(1L)
        val slot = GameSlot(listOf(3, 3))
        val sut = GameSlots(mapOf(eventId to slot))

        // when
        val result = sut[eventId]

        // then
        assertThat(result).isEqualTo(slot)
    }

    @Test
    fun `등록되지 않은 eventId 는 default GameSlot 으로 폴백`() {
        // given
        val sut = GameSlots(emptyMap())

        // when
        val result = sut[BrawlStarsId(999L)]

        // then
        assertThat(result).isEqualTo(GameSlot.default())
    }
}
```

- [ ] **Step 2: 컴파일 실패 확인**

Run: `./gradlew :core:core-api:compileTestKotlin`
Expected: `unresolved reference: GameSlots`.

- [ ] **Step 3: GameSlots 구현**

`core/core-api/src/main/kotlin/com/imstargg/core/domain/statistics/GameSlots.kt`:

```kotlin
package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId

/**
 * 이벤트별 [GameSlot] 묶음. 등록되지 않은 eventId 는 [GameSlot.default] 로 폴백한다.
 */
class GameSlots(
    private val byEventId: Map<BrawlStarsId, GameSlot>,
) {
    operator fun get(eventId: BrawlStarsId): GameSlot =
        byEventId[eventId] ?: GameSlot.default()
}
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `./gradlew :core:core-api:unitTest --tests "com.imstargg.core.domain.statistics.GameSlotsTest"`
Expected: BUILD SUCCESSFUL, 2 tests passed.

- [ ] **Step 5: 커밋**

```bash
git add core/core-api/src/main/kotlin/com/imstargg/core/domain/statistics/GameSlots.kt \
        core/core-api/src/test/kotlin/com/imstargg/core/domain/statistics/GameSlotsTest.kt
git commit -m "feat(core-api): GameSlots 도메인 객체 추가 (missing event 는 default 폴백)"
```

---

## Task 3: GameSlotRepository + 테스트

**Files:**
- Create: `core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/GameSlotRepository.kt`
- Create: `core/core-api/src/test/kotlin/com/imstargg/core/application/statistics/GameSlotRepositoryTest.kt`

- [ ] **Step 1: 실패 테스트 작성**

`core/core-api/src/test/kotlin/com/imstargg/core/application/statistics/GameSlotRepositoryTest.kt`:

> 참고: `BattleEventEntity.mode` 는 `@ManyToOne` 이고 cascade 가 없으므로 `BattleEventModeEntity` 를 먼저 save 한 뒤 `BattleEventEntity` 에 넘긴다.

```kotlin
package com.imstargg.core.application.statistics

import com.imstargg.CoreApiContextTest
import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.statistics.GameSlot
import com.imstargg.infrastructure.db.core.brawlstars.event.BattleEventEntity
import com.imstargg.infrastructure.db.core.brawlstars.event.BattleEventModeEntity
import com.imstargg.infrastructure.db.core.brawlstars.event.repository.BattleEventJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.event.repository.BattleEventModeJpaRepository
import com.imstargg.infrastructure.db.core.test.CleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class GameSlotRepositoryTest(
    private val sut: GameSlotRepository,
    private val battleEventJpaRepository: BattleEventJpaRepository,
    private val battleEventModeJpaRepository: BattleEventModeJpaRepository,
    private val cleanUp: CleanUp,
) : CoreApiContextTest() {
    @AfterEach
    fun tearDown() {
        cleanUp.all()
    }

    @Test
    fun `BattleEvent 의 mode playerCounts 를 GameSlot 으로 반환`() {
        // given
        val eventId = 15000001L
        val mode =
            battleEventModeJpaRepository.save(
                BattleEventModeEntity(
                    brawlStarsId = 48000001L,
                    brawlStarsName = "gemGrab",
                    playerCounts = listOf(3, 3),
                ),
            )
        battleEventJpaRepository.save(
            BattleEventEntity(brawlStarsId = eventId, mode = mode, mapName = "Hard Rock Mine"),
        )

        // when
        val result = sut.find(BrawlStarsId(eventId))

        // then
        assertThat(result.playerCounts).isEqualTo(listOf(3, 3))
    }

    @Test
    fun `BattleEvent 가 없으면 default GameSlot 반환`() {
        // given - 아무것도 저장 안 함

        // when
        val result = sut.find(BrawlStarsId(99999999L))

        // then
        assertThat(result).isEqualTo(GameSlot.default())
    }

    @Test
    fun `BattleEvent mode 가 null 이면 default GameSlot 반환`() {
        // given
        val eventId = 15000002L
        battleEventJpaRepository.save(
            BattleEventEntity(brawlStarsId = eventId, mode = null, mapName = "Some Map"),
        )

        // when
        val result = sut.find(BrawlStarsId(eventId))

        // then
        assertThat(result).isEqualTo(GameSlot.default())
    }

    @Test
    fun `findAllByEventIds 는 distinct 처리 후 각 eventId 에 대한 GameSlot 묶음 반환`() {
        // given
        val modeA =
            battleEventModeJpaRepository.save(
                BattleEventModeEntity(
                    brawlStarsId = 48000010L,
                    brawlStarsName = "gemGrab",
                    playerCounts = listOf(3, 3),
                ),
            )
        val modeB =
            battleEventModeJpaRepository.save(
                BattleEventModeEntity(
                    brawlStarsId = 48000011L,
                    brawlStarsName = "soloShowdown",
                    playerCounts = List(10) { 1 },
                ),
            )
        battleEventJpaRepository.save(BattleEventEntity(brawlStarsId = 100L, mode = modeA, mapName = "A"))
        battleEventJpaRepository.save(BattleEventEntity(brawlStarsId = 200L, mode = modeB, mapName = "B"))

        // when - 중복 + 미존재 eventId 섞임
        val result =
            sut.findAllByEventIds(
                listOf(BrawlStarsId(100L), BrawlStarsId(200L), BrawlStarsId(100L), BrawlStarsId(300L)),
            )

        // then
        assertThat(result[BrawlStarsId(100L)].playerCounts).isEqualTo(listOf(3, 3))
        assertThat(result[BrawlStarsId(200L)].playerCounts).isEqualTo(List(10) { 1 })
        assertThat(result[BrawlStarsId(300L)]).isEqualTo(GameSlot.default()) // missing 도 default 반환
    }
}
```

- [ ] **Step 2: 컴파일 실패 확인**

Run: `./gradlew :core:core-api:compileTestKotlin`
Expected: `unresolved reference: GameSlotRepository`.

- [ ] **Step 3: GameSlotRepository 구현**

`core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/GameSlotRepository.kt`:

```kotlin
package com.imstargg.core.application.statistics

import com.imstargg.core.application.brawlstars.BattleEventRepositoryWithCache
import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.statistics.GameSlot
import com.imstargg.core.domain.statistics.GameSlots
import org.springframework.stereotype.Repository

@Repository
class GameSlotRepository(
    private val battleEventRepositoryWithCache: BattleEventRepositoryWithCache,
) {
    fun find(eventId: BrawlStarsId): GameSlot =
        battleEventRepositoryWithCache.find(eventId)?.mode?.playerCounts
            ?.let(::GameSlot)
            ?: GameSlot.default()

    fun findAllByEventIds(eventIds: List<BrawlStarsId>): GameSlots =
        GameSlots(eventIds.distinct().associateWith { find(it) })
}
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `./gradlew :core:core-api:contextTest --tests "com.imstargg.core.application.statistics.GameSlotRepositoryTest"`
Expected: BUILD SUCCESSFUL, 4 tests passed.

- [ ] **Step 5: 커밋**

```bash
git add core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/GameSlotRepository.kt \
        core/core-api/src/test/kotlin/com/imstargg/core/application/statistics/GameSlotRepositoryTest.kt
git commit -m "feat(core-api): GameSlotRepository 추가 (BattleEvent → GameSlot 변환)"
```

---

## Task 4: `BrawlerResultStatistics.create` 시그니처 변경 + 호출자 갱신

`Map<BrawlStarsId, Int>` 입력을 `GameSlots` 로 교체. `error("playerCounts missing")` 분기 제거. 호출자 (`BattleResultStatisticsService`) 도 일시적으로 GameSlots 를 만들어 넘기도록 갱신 — Task 7 에서 Service 를 한 번 더 슬림화함.

**Files:**
- Modify: `core/core-api/src/main/kotlin/com/imstargg/core/domain/statistics/BrawlerResultStatistics.kt`
- Modify: `core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BattleResultStatisticsService.kt`
- Modify: `core/core-api/src/test/kotlin/com/imstargg/core/domain/statistics/BrawlerResultStatisticsTest.kt`

- [ ] **Step 1: 테스트 먼저 갱신**

`core/core-api/src/test/kotlin/com/imstargg/core/domain/statistics/BrawlerResultStatisticsTest.kt` 를 다음으로 교체:

```kotlin
package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class BrawlerResultStatisticsTest {
    @Nested
    inner class Create {
        @Test
        fun `단일 3v3 이벤트의 픽률은 게임 수 분모로 계산된다`() {
            // given: 3v3 이벤트 100게임 → 600 슬롯, 브롤러 A 가 300 슬롯
            val eventId = BrawlStarsId(1L)
            val brawlerA = BrawlStarsId(10L)
            val brawlerB = BrawlStarsId(11L)
            val rows =
                listOf(
                    BrawlerEventResultCount(eventId, brawlerA, ResultCount(150, 150, 0), 30),
                    BrawlerEventResultCount(eventId, brawlerB, ResultCount(150, 150, 0), 30),
                    BrawlerEventResultCount(eventId, BrawlStarsId(12L), ResultCount(150, 150, 0), 60),
                )
            val gameSlots = GameSlots(mapOf(eventId to GameSlot(listOf(3, 3))))

            // when
            val sut = BrawlerResultStatistics.create(rows, gameSlots)

            // then: totalBattleCount = 900/6 = 150
            assertThat(sut.totalBattleCount).isEqualTo(150)
            val itemA = sut.items.first { it.brawlerId == brawlerA }
            assertThat(itemA.pickRate).isEqualTo(2.0)
            assertThat(itemA.totalBattleCount).isEqualTo(150)
        }

        @Test
        fun `다중 이벤트 mix 시 게임수 분모는 이벤트별 환산 합`() {
            // given: 3v3 이벤트(slots=6) 60게임 = 360 슬롯, Solo SD(slots=10) 50게임 = 500 슬롯
            val event3v3 = BrawlStarsId(1L)
            val eventSolo = BrawlStarsId(2L)
            val brawler = BrawlStarsId(10L)
            val rows =
                listOf(
                    BrawlerEventResultCount(event3v3, brawler, ResultCount(60, 0, 0), 0),
                    BrawlerEventResultCount(event3v3, BrawlStarsId(99L), ResultCount(150, 150, 0), 0),
                    BrawlerEventResultCount(eventSolo, brawler, ResultCount(50, 0, 0), 0),
                    BrawlerEventResultCount(eventSolo, BrawlStarsId(99L), ResultCount(225, 225, 0), 0),
                )
            val gameSlots =
                GameSlots(
                    mapOf(
                        event3v3 to GameSlot(listOf(3, 3)),
                        eventSolo to GameSlot(List(10) { 1 }),
                    ),
                )

            // when
            val sut = BrawlerResultStatistics.create(rows, gameSlots)

            // then: totalBattleCount = 360/6 + 500/10 = 60 + 50 = 110
            assertThat(sut.totalBattleCount).isEqualTo(110)
            val item = sut.items.first { it.brawlerId == brawler }
            assertThat(item.pickRate).isEqualTo(1.0)
        }

        @Test
        fun `GameSlots 에 누락된 이벤트는 default 로 폴백`() {
            // given: 3v3 default(=BattleEventMode.DEFAULT_PLAYER_COUNTS = listOf(3,3)) → slots=6
            val eventId = BrawlStarsId(1L)
            val rows =
                listOf(
                    BrawlerEventResultCount(eventId, BrawlStarsId(10L), ResultCount(6, 0, 0), 0),
                )
            val gameSlots = GameSlots(emptyMap())

            // when
            val sut = BrawlerResultStatistics.create(rows, gameSlots)

            // then: totalBattleCount = 6 / 6 = 1
            assertThat(sut.totalBattleCount).isEqualTo(1)
        }

        @Test
        fun `빈 입력은 totalBattleCount 0`() {
            // given / when
            val sut = BrawlerResultStatistics.create(emptyList(), GameSlots(emptyMap()))

            // then
            assertThat(sut.totalBattleCount).isEqualTo(0)
            assertThat(sut.items).isEmpty()
        }
    }
}
```

기존 `slotsPerGameByEvent에 누락된 이벤트가 있으면 예외` 테스트는 default 폴백 동작으로 의미가 바뀌어 제거.

- [ ] **Step 2: 컴파일 실패 확인**

Run: `./gradlew :core:core-api:compileTestKotlin`
Expected: `BrawlerResultStatistics.create` 의 시그니처 mismatch 컴파일 에러.

- [ ] **Step 3: `BrawlerResultStatistics.create` 시그니처 변경**

`core/core-api/src/main/kotlin/com/imstargg/core/domain/statistics/BrawlerResultStatistics.kt` 의 `create` 함수를 다음으로 교체:

```kotlin
companion object {
    fun create(
        rows: List<BrawlerEventResultCount>,
        gameSlots: GameSlots,
    ): BrawlerResultStatistics {
        if (rows.isEmpty()) {
            return BrawlerResultStatistics(totalBattleCount = 0L, items = emptyList())
        }

        val totalBattleCount: Long =
            rows
                .groupBy { it.eventId }
                .map { (eventId, eventRows) ->
                    val eventSlotSum = eventRows.sumOf { it.resultCount.totalCount }
                    eventSlotSum / gameSlots[eventId].resultSlotsPerGame
                }.sum()

        val brawlerAggregates =
            rows
                .groupBy { it.brawlerId }
                .map { (brawlerId, brawlerRows) ->
                    BrawlerResultCount(
                        brawlerId = brawlerId,
                        resultCount = brawlerRows.map { it.resultCount }.reduce(ResultCount::merge),
                        starPlayerCount = brawlerRows.sumOf { it.starPlayerCount },
                    )
                }
        val tierScores = BrawlerTierScore.create(brawlerAggregates)

        val items =
            brawlerAggregates.map { agg ->
                val brawlerSlotSum = agg.resultCount.totalCount
                BrawlerResultStatisticsItem(
                    brawlerId = agg.brawlerId,
                    totalBattleCount = totalBattleCount,
                    pickRate = StatsRate.pickRate(brawlerSlotSum, totalBattleCount),
                    winRate = agg.resultCount.winRate,
                    starPlayerRate =
                        StatsRate.starPlayerRate(
                            agg.starPlayerCount,
                            agg.resultCount.totalCount,
                        ),
                    tierScore = tierScores[agg.brawlerId] ?: BrawlerTierScore(50),
                )
            }
        return BrawlerResultStatistics(
            totalBattleCount = totalBattleCount,
            items = items.sortedByDescending { it.tierScore.value },
        )
    }
}
```

추가로 같은 파일 상단의 KDoc 주석을 한 줄 정정: `(이전엔 슬롯 합이었음 — 2026-05-19 정정)` 은 그대로 둠.

- [ ] **Step 4: 호출자 (BattleResultStatisticsService) 가 GameSlots 를 만들도록 갱신**

`core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BattleResultStatisticsService.kt` 의 `getBrawlerResultStatistics` 와 `slotsPerGameByEvent` 메서드를 다음으로 교체 (다른 메서드는 Task 7 까지 그대로 유지):

```kotlin
fun getBrawlerResultStatistics(
    eventId: BrawlStarsId?,
    tierRange: StatsTierRange,
    dateRange: StatsDateRange,
): BrawlerResultStatistics {
    val (startDate, endDate) = dateRange.dateRange(clock)
    val rows =
        resultRepository.findBrawlerEventResultCounts(
            eventId = eventId,
            tierRange = tierRange,
            startDate = startDate,
            endDate = endDate,
        )
    val gameSlots = gameSlotsByEvent(rows.map { it.eventId })
    return BrawlerResultStatistics.create(rows = rows, gameSlots = gameSlots)
}

/**
 * 이벤트별 [GameSlot] 묶음을 만든다.
 *
 * mode 정보가 누락된 이벤트는 [GameSlot.default] 로 폴백하여
 * 호출자가 항상 모든 이벤트 ID 에 대해 값을 얻을 수 있도록 보장한다.
 */
private fun gameSlotsByEvent(eventIds: List<BrawlStarsId>): GameSlots =
    GameSlots(
        eventIds
            .distinct()
            .associateWith { id ->
                val playerCounts =
                    battleEventRepositoryWithCache.find(id)?.mode?.playerCounts
                        ?: BattleEventMode.DEFAULT_PLAYER_COUNTS
                GameSlot(playerCounts)
            },
    )
```

기존 `slotsPerGameByEvent` private 메서드는 제거. `computeTotalBattleCount` 와 다른 메서드 (`getBrawlerPairResultStatistics`, `getEnemyResultStatistics`, `getDailyResultStatistics`) 는 Task 5–7 에서 처리하므로 일단 그대로 유지.

import 추가:
```kotlin
import com.imstargg.core.domain.statistics.GameSlot
import com.imstargg.core.domain.statistics.GameSlots
```

기존 `import com.imstargg.core.domain.statistics.SlotsPerGame` 는 `computeTotalBattleCount` 가 아직 사용하므로 유지.

- [ ] **Step 5: 테스트 통과 확인**

Run: `./gradlew :core:core-api:unitTest --tests "com.imstargg.core.domain.statistics.BrawlerResultStatisticsTest"`
Expected: 4 tests passed.

Run: `./gradlew :core:core-api:compileKotlin :core:core-api:compileTestKotlin`
Expected: 컴파일 성공.

- [ ] **Step 6: ktlint + 커밋**

```bash
./gradlew :core:core-api:ktlintFormat
git add core/core-api/src/main/kotlin/com/imstargg/core/domain/statistics/BrawlerResultStatistics.kt \
        core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BattleResultStatisticsService.kt \
        core/core-api/src/test/kotlin/com/imstargg/core/domain/statistics/BrawlerResultStatisticsTest.kt
git commit -m "refactor(core-api): BrawlerResultStatistics.create 가 GameSlots 를 입력으로 받도록 변경"
```

---

## Task 5: `BattleResultStatisticsRepository` 재구성 (Statistics 반환)

기존 count/raw 반환 메서드를 Statistics 객체 반환 메서드로 교체. `computeTotalGameCount` 를 private 메서드로 흡수.

**Files:**
- Modify: `core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BattleResultStatisticsRepository.kt`
- Create: `core/core-api/src/test/kotlin/com/imstargg/core/application/statistics/BattleResultStatisticsRepositoryTest.kt`

- [ ] **Step 1: 통합 테스트 작성 (실패)**

`core/core-api/src/test/kotlin/com/imstargg/core/application/statistics/BattleResultStatisticsRepositoryTest.kt`:

> 주요 entity 시그니처 (실제 코드 기준):
> - `BrawlerBattleResultStatsEntity(eventBrawlStarsId, tierRange, battleDate, brawlerBrawlStarsId, resultCount, starPlayerCount)`
> - `BrawlerPairBattleResultStatsEntity(eventBrawlStarsId, tierRange, startBattleDate, endBattleDate, brawlerBrawlStarsId, pairBrawlerBrawlStarsId, resultCount)` (Pair/Enemy 는 start/end 날짜 분리)
> - `BrawlerEnemyBattleResultStatsEntity(eventBrawlStarsId, tierRange, startBattleDate, endBattleDate, brawlerBrawlStarsId, enemyBrawlerBrawlStarsId, resultCount)`
> - `ResultCountEmbedded` 는 `internal constructor` → core-api 모듈에서는 `ResultCountEmbedded.create(victory, defeat, draw)` 팩토리 사용
> - `BattleEventEntity.mode` 는 `@ManyToOne` cascade 없음 → `BattleEventModeEntity` 를 먼저 save

```kotlin
package com.imstargg.core.application.statistics

import com.imstargg.CoreApiContextTest
import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.brawlstars.event.BattleEventEntity
import com.imstargg.infrastructure.db.core.brawlstars.event.BattleEventModeEntity
import com.imstargg.infrastructure.db.core.brawlstars.event.repository.BattleEventJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.event.repository.BattleEventModeJpaRepository
import com.imstargg.infrastructure.db.core.statistics.BrawlerBattleResultStatsEntity
import com.imstargg.infrastructure.db.core.statistics.BrawlerEnemyBattleResultStatsEntity
import com.imstargg.infrastructure.db.core.statistics.BrawlerPairBattleResultStatsEntity
import com.imstargg.infrastructure.db.core.statistics.ResultCountEmbedded
import com.imstargg.infrastructure.db.core.statistics.repository.BrawlerBattleResultStatsJpaRepository
import com.imstargg.infrastructure.db.core.statistics.repository.BrawlerEnemyBattleResultStatsJpaRepository
import com.imstargg.infrastructure.db.core.statistics.repository.BrawlerPairBattleResultStatsJpaRepository
import com.imstargg.infrastructure.db.core.test.CleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BattleResultStatisticsRepositoryTest(
    private val sut: BattleResultStatisticsRepository,
    private val battleEventJpaRepository: BattleEventJpaRepository,
    private val battleEventModeJpaRepository: BattleEventModeJpaRepository,
    private val resultStatsJpaRepository: BrawlerBattleResultStatsJpaRepository,
    private val pairStatsJpaRepository: BrawlerPairBattleResultStatsJpaRepository,
    private val enemyStatsJpaRepository: BrawlerEnemyBattleResultStatsJpaRepository,
    private val cleanUp: CleanUp,
) : CoreApiContextTest() {
    private val tierRange = StatsTierRange.HIGH
    private val date = LocalDate.of(2026, 5, 1)

    @AfterEach
    fun tearDown() {
        cleanUp.all()
    }

    private fun save3v3Event(eventId: Long): BrawlStarsId {
        val mode =
            battleEventModeJpaRepository.save(
                BattleEventModeEntity(
                    brawlStarsId = eventId + 30_000_000L,
                    brawlStarsName = "gemGrab",
                    playerCounts = listOf(3, 3),
                ),
            )
        battleEventJpaRepository.save(BattleEventEntity(brawlStarsId = eventId, mode = mode, mapName = "X"))
        return BrawlStarsId(eventId)
    }

    @Test
    fun `findBrawlerResultStatistics - 3v3 이벤트의 게임 수 분모로 픽률 계산`() {
        // given: 3v3 이벤트, brawler A 가 양 팀 모두 카운트되어 슬롯 6 = 1 게임
        val event = save3v3Event(eventId = 100L)
        resultStatsJpaRepository.save(
            BrawlerBattleResultStatsEntity(
                eventBrawlStarsId = event.value,
                tierRange = tierRange,
                battleDate = date,
                brawlerBrawlStarsId = 10L,
                resultCount = ResultCountEmbedded.create(victoryCount = 3, defeatCount = 3, drawCount = 0),
                starPlayerCount = 1,
            ),
        )

        // when
        val result = sut.findBrawlerResultStatistics(event, tierRange, date, date)

        // then: totalSlotCount = 6, gameCount = 6/6 = 1
        assertThat(result.totalBattleCount).isEqualTo(1)
        assertThat(result.items).hasSize(1)
        assertThat(result.items.first().brawlerId).isEqualTo(BrawlStarsId(10L))
    }

    @Test
    fun `findBrawlerPairResultStatistics - 분모는 Result 슬롯합 기반 게임 수`() {
        // given: 3v3 이벤트 1게임 — Result 슬롯 6 (= 1 게임 분모)
        val event = save3v3Event(eventId = 101L)
        resultStatsJpaRepository.save(
            BrawlerBattleResultStatsEntity(
                eventBrawlStarsId = event.value,
                tierRange = tierRange,
                battleDate = date,
                brawlerBrawlStarsId = 10L,
                resultCount = ResultCountEmbedded.create(victoryCount = 3, defeatCount = 3, drawCount = 0),
                starPlayerCount = 0,
            ),
        )
        pairStatsJpaRepository.save(
            BrawlerPairBattleResultStatsEntity(
                eventBrawlStarsId = event.value,
                tierRange = tierRange,
                startBattleDate = date,
                endBattleDate = date,
                brawlerBrawlStarsId = 10L,
                pairBrawlerBrawlStarsId = 20L,
                resultCount = ResultCountEmbedded.create(victoryCount = 1, defeatCount = 0, drawCount = 0),
            ),
        )

        // when
        val result =
            sut.findBrawlerPairResultStatistics(
                eventId = event,
                brawlerId = BrawlStarsId(10L),
                pairBrawlerId = null,
                tierRange = tierRange,
                startDate = date,
                endDate = date,
            )

        // then: 분모 (totalBattleCount) = 1 게임
        assertThat(result.totalBattleCount).isEqualTo(1)
        assertThat(result.items).hasSize(1)
        assertThat(result.items.first().pairBrawlerId).isEqualTo(BrawlStarsId(20L))
    }

    @Test
    fun `findBrawlerEnemyResultStatistics - 분모는 Result 슬롯합 기반 게임 수`() {
        // given: 3v3 이벤트 1게임 — Result 슬롯 6 (= 1 게임 분모)
        val event = save3v3Event(eventId = 102L)
        resultStatsJpaRepository.save(
            BrawlerBattleResultStatsEntity(
                eventBrawlStarsId = event.value,
                tierRange = tierRange,
                battleDate = date,
                brawlerBrawlStarsId = 10L,
                resultCount = ResultCountEmbedded.create(victoryCount = 3, defeatCount = 3, drawCount = 0),
                starPlayerCount = 0,
            ),
        )
        enemyStatsJpaRepository.save(
            BrawlerEnemyBattleResultStatsEntity(
                eventBrawlStarsId = event.value,
                tierRange = tierRange,
                startBattleDate = date,
                endBattleDate = date,
                brawlerBrawlStarsId = 10L,
                enemyBrawlerBrawlStarsId = 30L,
                resultCount = ResultCountEmbedded.create(victoryCount = 1, defeatCount = 0, drawCount = 0),
            ),
        )

        // when
        val result =
            sut.findBrawlerEnemyResultStatistics(
                eventId = event,
                brawlerId = BrawlStarsId(10L),
                tierRange = tierRange,
                startDate = date,
                endDate = date,
            )

        // then
        assertThat(result.totalBattleCount).isEqualTo(1)
        assertThat(result.items).hasSize(1)
        assertThat(result.items.first().enemyBrawlerId).isEqualTo(BrawlStarsId(30L))
    }

    @Test
    fun `findBrawlerDailyResultStatistics - 같은 날짜의 entity 들을 합산해서 일별 row 반환`() {
        // given
        val event = save3v3Event(eventId = 103L)
        resultStatsJpaRepository.save(
            BrawlerBattleResultStatsEntity(
                eventBrawlStarsId = event.value,
                tierRange = tierRange,
                battleDate = date,
                brawlerBrawlStarsId = 10L,
                resultCount = ResultCountEmbedded.create(victoryCount = 2, defeatCount = 1, drawCount = 0),
                starPlayerCount = 1,
            ),
        )

        // when
        val result =
            sut.findBrawlerDailyResultStatistics(
                brawlerId = BrawlStarsId(10L),
                tierRange = tierRange,
                startDate = date,
                endDate = date,
            )

        // then
        assertThat(result.items).hasSize(1)
        assertThat(result.items.first().date).isEqualTo(date)
    }
}
```

> 위 네 케이스가 Repository 의 외부 메서드 4개를 1회씩 커버한다. `BrawlerResultStatistics.create` 의 게임 수 계산 알고리즘 자체는 `BrawlerResultStatisticsTest` (Task 4) 가 단위로 충분히 검증하므로 Repository 통합 테스트는 "JPA → 도메인 매핑 + computeTotalGameCount 가 분모로 흘러간다" 까지만 본다.

- [ ] **Step 2: 컴파일 실패 확인**

Run: `./gradlew :core:core-api:compileTestKotlin`
Expected: `sut.findBrawlerResultStatistics` 등 메서드 unresolved 또는 시그니처 mismatch.

- [ ] **Step 3: Repository 재구성**

`core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BattleResultStatisticsRepository.kt` 전체 교체:

```kotlin
package com.imstargg.core.application.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.statistics.BrawlerEnemyResultCount
import com.imstargg.core.domain.statistics.BrawlerEnemyResultStatistics
import com.imstargg.core.domain.statistics.BrawlerEventResultCount
import com.imstargg.core.domain.statistics.BrawlerPairResultCount
import com.imstargg.core.domain.statistics.BrawlerPairResultStatistics
import com.imstargg.core.domain.statistics.BrawlerResultCount
import com.imstargg.core.domain.statistics.BrawlerResultDailyCount
import com.imstargg.core.domain.statistics.BrawlerResultDailyStatistics
import com.imstargg.core.domain.statistics.BrawlerResultStatistics
import com.imstargg.core.domain.statistics.ResultCount
import com.imstargg.core.enums.StatsTierRange
import com.imstargg.core.infrastructure.jpa.toResultCount
import com.imstargg.infrastructure.db.core.statistics.repository.BrawlerBattleResultStatsJpaRepository
import com.imstargg.infrastructure.db.core.statistics.repository.BrawlerEnemyBattleResultStatsJpaRepository
import com.imstargg.infrastructure.db.core.statistics.repository.BrawlerPairBattleResultStatsJpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Repository
@Transactional(readOnly = true)
class BattleResultStatisticsRepository(
    private val battleResultJpaRepository: BrawlerBattleResultStatsJpaRepository,
    private val enemyBattleResultJpaRepository: BrawlerEnemyBattleResultStatsJpaRepository,
    private val pairBattleResultJpaRepository: BrawlerPairBattleResultStatsJpaRepository,
    private val gameSlotRepository: GameSlotRepository,
) {
    fun findBrawlerResultStatistics(
        eventId: BrawlStarsId?,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): BrawlerResultStatistics {
        val rows: List<BrawlerEventResultCount> =
            battleResultJpaRepository
                .findGroupedByEventAndBrawler(
                    eventBrawlStarsId = eventId?.value,
                    tierRange = tierRange,
                    startDate = startDate,
                    endDate = endDate,
                ).map { projection ->
                    BrawlerEventResultCount(
                        eventId = BrawlStarsId(projection.eventBrawlStarsId),
                        brawlerId = BrawlStarsId(projection.brawlerBrawlStarsId),
                        resultCount =
                            ResultCount(
                                victoryCount = projection.victoryCount,
                                defeatCount = projection.defeatCount,
                                drawCount = projection.drawCount,
                            ),
                        starPlayerCount = projection.starPlayerCount,
                    )
                }
        val gameSlots = gameSlotRepository.findAllByEventIds(rows.map { it.eventId })
        return BrawlerResultStatistics.create(rows = rows, gameSlots = gameSlots)
    }

    fun findBrawlerPairResultStatistics(
        eventId: BrawlStarsId?,
        brawlerId: BrawlStarsId,
        pairBrawlerId: BrawlStarsId?,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): BrawlerPairResultStatistics {
        val counts: List<BrawlerPairResultCount> =
            pairBattleResultJpaRepository
                .findAllByConditions(
                    eventBrawlStarsId = eventId?.value,
                    brawlerBrawlStarsId = brawlerId.value,
                    pairBrawlerBrawlStarsId = pairBrawlerId?.value,
                    tierRange = tierRange,
                    startDate = startDate,
                    endDate = endDate,
                ).map { projection ->
                    BrawlerPairResultCount(
                        brawlerId = BrawlStarsId(projection.brawlerBrawlStarsId),
                        pairBrawlerId = BrawlStarsId(projection.pairBrawlerBrawlStarsId),
                        resultCount =
                            ResultCount(
                                victoryCount = projection.victoryCount,
                                defeatCount = projection.defeatCount,
                                drawCount = projection.drawCount,
                            ),
                    )
                }
        val totalElementCount =
            pairBattleResultJpaRepository.countDistinctPairBrawlers(
                eventBrawlStarsId = eventId?.value,
                brawlerBrawlStarsId = brawlerId.value,
                pairBrawlerBrawlStarsId = pairBrawlerId?.value,
                tierRange = tierRange,
                startDate = startDate,
                endDate = endDate,
            )
        val totalGameCount = computeTotalGameCount(eventId, tierRange, startDate, endDate)
        return BrawlerPairResultStatistics.create(
            totalBattleCount = totalGameCount,
            totalElementCount = totalElementCount,
            counts = counts,
        )
    }

    fun findBrawlerEnemyResultStatistics(
        eventId: BrawlStarsId?,
        brawlerId: BrawlStarsId,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): BrawlerEnemyResultStatistics {
        val counts: List<BrawlerEnemyResultCount> =
            enemyBattleResultJpaRepository
                .findAllByConditions(
                    eventBrawlStarsId = eventId?.value,
                    brawlerBrawlStarsId = brawlerId.value,
                    tierRange = tierRange,
                    startDate = startDate,
                    endDate = endDate,
                ).map { projection ->
                    BrawlerEnemyResultCount(
                        brawlerId = BrawlStarsId(projection.brawlerBrawlStarsId),
                        enemyBrawlerId = BrawlStarsId(projection.enemyBrawlerBrawlStarsId),
                        resultCount =
                            ResultCount(
                                victoryCount = projection.victoryCount,
                                defeatCount = projection.defeatCount,
                                drawCount = projection.drawCount,
                            ),
                    )
                }
        val totalElementCount =
            enemyBattleResultJpaRepository.countDistinctEnemyBrawlers(
                eventBrawlStarsId = eventId?.value,
                brawlerBrawlStarsId = brawlerId.value,
                tierRange = tierRange,
                startDate = startDate,
                endDate = endDate,
            )
        val totalGameCount = computeTotalGameCount(eventId, tierRange, startDate, endDate)
        return BrawlerEnemyResultStatistics.create(
            totalBattleCount = totalGameCount,
            totalElementCount = totalElementCount,
            counts = counts,
        )
    }

    fun findBrawlerDailyResultStatistics(
        brawlerId: BrawlStarsId,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): BrawlerResultDailyStatistics {
        val dailyCounts: List<BrawlerResultDailyCount> =
            battleResultJpaRepository
                .findAllByBrawlerBrawlStarsIdAndTierRangeAndBattleDateBetween(
                    brawlerBrawlStarsId = brawlerId.value,
                    tierRange = tierRange,
                    startDate = startDate,
                    endDate = endDate,
                ).groupBy { it.battleDate }
                .map { (battleDate, grouped) ->
                    BrawlerResultDailyCount(
                        date = battleDate,
                        brawlerResultCount =
                            BrawlerResultCount(
                                brawlerId = brawlerId,
                                resultCount =
                                    grouped
                                        .map { it.resultCount }
                                        .reduce { acc, r -> acc + r }
                                        .toResultCount(),
                                starPlayerCount = grouped.sumOf { it.starPlayerCount },
                            ),
                    )
                }.sortedBy { it.date }
        return BrawlerResultDailyStatistics.create(brawlerId = brawlerId, dailyCounts = dailyCounts)
    }

    /**
     * 조회 범위의 총 게임 수.
     *
     * Pair / Enemy 통계의 분모도 동일한 게임 수를 사용하므로 항상
     * `brawler_battle_result_stats` (Result 슬롯) + [GameSlot.resultSlotsPerGame] 로 계산한다.
     * 수학적으로 `pair_slot/pairSlotsPerGame == enemy_slot/enemySlotsPerGame == result_slot/resultSlotsPerGame == gameCount`
     * 가 보장되므로 Result 테이블을 단일 진실 출처로 사용하는 것이 더 단순하다.
     */
    private fun computeTotalGameCount(
        eventId: BrawlStarsId?,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): Long {
        val sums =
            battleResultJpaRepository.findEventTotalSlotSums(
                eventBrawlStarsId = eventId?.value,
                tierRange = tierRange,
                startDate = startDate,
                endDate = endDate,
            )
        val gameSlots = gameSlotRepository.findAllByEventIds(sums.map { BrawlStarsId(it.eventBrawlStarsId) })
        return sums.sumOf { projection ->
            projection.totalSlotCount / gameSlots[BrawlStarsId(projection.eventBrawlStarsId)].resultSlotsPerGame
        }
    }
}
```

- [ ] **Step 4: 컴파일 확인**

Run: `./gradlew :core:core-api:compileKotlin`
Expected: 성공. 이 단계에서는 `BattleResultStatisticsService` / `BattleResultStatisticsRepositoryWithCache` 가 아직 옛 메서드를 호출 중이라 둘은 다음 Task 에서 동기화한다. 컴파일이 실패하면 Repository 의 옛 public 메서드 (`findBrawlerEventResultCounts`, `findBrawlerEnemyResultCounts`, `findBrawlerPairResultCounts`, `countEnemyResultBy`, `countPairResultBy`, `findBrawlerDailyResultCounts`, `findEventTotalSlotSums`) 가 모두 제거되었기 때문 — 다음 Task 에서 호출자를 갱신한다. 일단 다음 Task 로 넘어가도 됨 (commit 은 Task 6 이후 묶음).

- [ ] **Step 5: 다음 Task 로**

이 Task 의 commit 은 Task 6 와 묶어서 Task 6 의 step 마지막에 함께 수행 (그래야 main 빌드가 깨지지 않은 상태로 commit).

---

## Task 6: `BattleResultStatisticsRepositoryWithCache` 재구성 (캐시 키)

**Files:**
- Modify: `core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BattleResultStatisticsRepositoryWithCache.kt`

- [ ] **Step 1: 전체 교체**

`core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BattleResultStatisticsRepositoryWithCache.kt` 전체 교체:

```kotlin
package com.imstargg.core.application.statistics

import com.imstargg.core.config.CacheNames
import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.statistics.BrawlerEnemyResultStatistics
import com.imstargg.core.domain.statistics.BrawlerPairResultStatistics
import com.imstargg.core.domain.statistics.BrawlerResultDailyStatistics
import com.imstargg.core.domain.statistics.BrawlerResultStatistics
import com.imstargg.core.enums.StatsTierRange
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class BattleResultStatisticsRepositoryWithCache(
    private val battleResultStatisticsRepository: BattleResultStatisticsRepository,
) {
    @Cacheable(
        cacheNames = [CacheNames.STATISTICS],
        key = "'v1:brawlerResultStats:' + #eventId + ':' + #tierRange + ':' + #startDate + ':' + #endDate",
        sync = true,
    )
    fun findBrawlerResultStatistics(
        eventId: BrawlStarsId?,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): BrawlerResultStatistics =
        battleResultStatisticsRepository.findBrawlerResultStatistics(eventId, tierRange, startDate, endDate)

    @Cacheable(
        cacheNames = [CacheNames.STATISTICS],
        key =
            "'v1:brawlerPairResultStats:' + #eventId + ':' + #brawlerId + ':' + #pairBrawlerId" +
                " + ':' + #tierRange + ':' + #startDate + ':' + #endDate",
        sync = true,
    )
    fun findBrawlerPairResultStatistics(
        eventId: BrawlStarsId?,
        brawlerId: BrawlStarsId,
        pairBrawlerId: BrawlStarsId?,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): BrawlerPairResultStatistics =
        battleResultStatisticsRepository.findBrawlerPairResultStatistics(
            eventId, brawlerId, pairBrawlerId, tierRange, startDate, endDate,
        )

    @Cacheable(
        cacheNames = [CacheNames.STATISTICS],
        key =
            "'v1:brawlerEnemyResultStats:' + #eventId + ':' + #brawlerId + ':' + #tierRange" +
                " + ':' + #startDate + ':' + #endDate",
        sync = true,
    )
    fun findBrawlerEnemyResultStatistics(
        eventId: BrawlStarsId?,
        brawlerId: BrawlStarsId,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): BrawlerEnemyResultStatistics =
        battleResultStatisticsRepository.findBrawlerEnemyResultStatistics(
            eventId, brawlerId, tierRange, startDate, endDate,
        )

    @Cacheable(
        cacheNames = [CacheNames.STATISTICS],
        key = "'v1:brawlerDailyResultStats:' + #brawlerId + ':' + #tierRange + ':' + #startDate + ':' + #endDate",
        sync = true,
    )
    fun findBrawlerDailyResultStatistics(
        brawlerId: BrawlStarsId,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): BrawlerResultDailyStatistics =
        battleResultStatisticsRepository.findBrawlerDailyResultStatistics(
            brawlerId, tierRange, startDate, endDate,
        )
}
```

`RawCounts` / `EventTotalSlotSum` import 는 모두 제거됨. 옛 메서드 (`findBrawlerEventResultCounts`, `findBrawlerEnemyResultRaw`, `findBrawlerPairResultRaw`, `findEventTotalSlotSums`, `findBrawlerDailyResultCounts`) 도 모두 제거.

- [ ] **Step 2: 컴파일 — Service 가 옛 메서드를 호출 중이라 실패**

Run: `./gradlew :core:core-api:compileKotlin`
Expected: `BattleResultStatisticsService` 에서 `resultRepository.findBrawlerEventResultCounts(...)` 등 unresolved 컴파일 에러. → 다음 Task 7 에서 Service 를 갱신해 동시에 해결.

다음 Task 로 진행.

---

## Task 7: `BattleResultStatisticsService` 슬림화

**Files:**
- Modify: `core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BattleResultStatisticsService.kt`

- [ ] **Step 1: 전체 교체**

`core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BattleResultStatisticsService.kt` 전체 교체:

```kotlin
package com.imstargg.core.application.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.statistics.BrawlerEnemyResultStatistics
import com.imstargg.core.domain.statistics.BrawlerPairResultStatistics
import com.imstargg.core.domain.statistics.BrawlerResultDailyStatistics
import com.imstargg.core.domain.statistics.BrawlerResultStatistics
import com.imstargg.core.enums.StatsDateRange
import com.imstargg.core.enums.StatsTierRange
import org.springframework.stereotype.Service
import java.time.Clock

@Service
class BattleResultStatisticsService(
    private val clock: Clock,
    private val resultRepository: BattleResultStatisticsRepositoryWithCache,
) {
    fun getBrawlerResultStatistics(
        eventId: BrawlStarsId?,
        tierRange: StatsTierRange,
        dateRange: StatsDateRange,
    ): BrawlerResultStatistics {
        val (startDate, endDate) = dateRange.dateRange(clock)
        return resultRepository.findBrawlerResultStatistics(eventId, tierRange, startDate, endDate)
    }

    fun getBrawlerPairResultStatistics(
        eventId: BrawlStarsId?,
        brawlerId: BrawlStarsId,
        pairBrawlerId: BrawlStarsId?,
        tierRange: StatsTierRange,
        dateRange: StatsDateRange,
    ): BrawlerPairResultStatistics {
        val (startDate, endDate) = dateRange.dateRange(clock)
        return resultRepository.findBrawlerPairResultStatistics(
            eventId, brawlerId, pairBrawlerId, tierRange, startDate, endDate,
        )
    }

    fun getEnemyResultStatistics(
        eventId: BrawlStarsId?,
        brawlerId: BrawlStarsId,
        tierRange: StatsTierRange,
        dateRange: StatsDateRange,
    ): BrawlerEnemyResultStatistics {
        val (startDate, endDate) = dateRange.dateRange(clock)
        return resultRepository.findBrawlerEnemyResultStatistics(
            eventId, brawlerId, tierRange, startDate, endDate,
        )
    }

    fun getDailyResultStatistics(
        brawlerId: BrawlStarsId,
        tierRange: StatsTierRange,
        dateRange: StatsDateRange,
    ): BrawlerResultDailyStatistics {
        val (startDate, endDate) = dateRange.dateRange(clock)
        return resultRepository.findBrawlerDailyResultStatistics(
            brawlerId, tierRange, startDate, endDate,
        )
    }
}
```

`BattleEventRepositoryWithCache`, `BattleEventMode`, `SlotsPerGame`, `LocalDate`, `gameSlotsByEvent`, `computeTotalBattleCount` 모두 제거.

- [ ] **Step 2: 컴파일 확인**

Run: `./gradlew :core:core-api:compileKotlin :core:core-api:compileTestKotlin`
Expected: 컴파일 성공. 옛 Service 테스트 (`BattleResultStatisticsServiceTest` — 존재한다면) 가 옛 시그니처/내부 동작에 묶여 있다면 컴파일/실행 실패. 그 경우 Service 테스트 파일을 열어 다음 형태로 갱신:

```kotlin
// 이제 Service 는 dateRange 정규화 + 위임만이므로,
// Repository 를 mockk 으로 두고 위임이 올바른 인자로 한 번 일어나는지만 검증한다.
```

존재 여부 확인:
```bash
find core/core-api/src/test -name "BattleResultStatisticsServiceTest.kt"
```
존재하면 위 가이드로 갱신하거나, 새 Repository 통합 테스트가 동작 검증을 담당하므로 삭제 가능.

- [ ] **Step 3: Task 5 의 Repository 통합 테스트 실행**

Run: `./gradlew :core:core-api:contextTest --tests "com.imstargg.core.application.statistics.BattleResultStatisticsRepositoryTest"`
Expected: 정의된 케이스 모두 통과.

- [ ] **Step 4: 빌드 + ktlint + 커밋 (Task 5~7 묶음)**

```bash
./gradlew :core:core-api:ktlintFormat
./gradlew :core:core-api:build
```
Expected: BUILD SUCCESSFUL.

```bash
git add core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BattleResultStatisticsRepository.kt \
        core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BattleResultStatisticsRepositoryWithCache.kt \
        core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BattleResultStatisticsService.kt \
        core/core-api/src/test/kotlin/com/imstargg/core/application/statistics/BattleResultStatisticsRepositoryTest.kt
# Service 테스트 파일을 갱신/삭제했다면 함께 add
git commit -m "refactor(core-api): BattleResult 통계 Repository 가 Statistics 객체 반환하도록 재구성

- Repository: count/raw 반환 → Statistics 객체 반환 (@Transactional readOnly 안에서 분모 계산)
- WithCache: @Cacheable 키/시그니처를 Statistics 단위로 교체
- Service: dateRange 정규화 + 위임만으로 슬림화 (BattleEvent 의존 제거)"
```

---

## Task 8: `BattleRankStatisticsRepository` 재구성 (Statistics 반환)

**Files:**
- Modify: `core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BattleRankStatisticsRepository.kt`
- Create: `core/core-api/src/test/kotlin/com/imstargg/core/application/statistics/BattleRankStatisticsRepositoryTest.kt`

- [ ] **Step 1: 통합 테스트 작성 (실패)**

`core/core-api/src/test/kotlin/com/imstargg/core/application/statistics/BattleRankStatisticsRepositoryTest.kt`:

> 주요 entity 시그니처:
> - `BrawlerBattleRankStatsEntity(eventBrawlStarsId, tierRange, battleDate, brawlerBrawlStarsId, rankCounts)`
> - `BrawlerPairBattleRankStatsEntity(eventBrawlStarsId, tierRange, startBattleDate, endBattleDate, brawlerBrawlStarsId, pairBrawlerBrawlStarsId, rankCounts)` (Pair 는 start/end 날짜 분리)
> - `RankCountsEmbedded` 는 `internal constructor` → `RankCountsEmbedded.fromMap(mapOf(rank to count, ...))` 팩토리 사용

```kotlin
package com.imstargg.core.application.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.CoreApiContextTest
import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.BrawlerBattleRankStatsEntity
import com.imstargg.infrastructure.db.core.statistics.BrawlerPairBattleRankStatsEntity
import com.imstargg.infrastructure.db.core.statistics.RankCountsEmbedded
import com.imstargg.infrastructure.db.core.statistics.repository.BrawlerBattleRankStatsJpaRepository
import com.imstargg.infrastructure.db.core.statistics.repository.BrawlerPairBattleRankStatsJpaRepository
import com.imstargg.infrastructure.db.core.test.CleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BattleRankStatisticsRepositoryTest(
    private val sut: BattleRankStatisticsRepository,
    private val soloRankStatsJpaRepository: BrawlerBattleRankStatsJpaRepository,
    private val pairRankStatsJpaRepository: BrawlerPairBattleRankStatsJpaRepository,
    private val cleanUp: CleanUp,
) : CoreApiContextTest() {
    private val tierRange = StatsTierRange.HIGH
    private val date = LocalDate.of(2026, 5, 1)

    @AfterEach
    fun tearDown() {
        cleanUp.all()
    }

    @Test
    fun `findBrawlerRankStatistics - solo rank entity 들을 브롤러별로 합산한 items 반환`() {
        // given
        val event = BrawlStarsId(200L)
        soloRankStatsJpaRepository.save(
            BrawlerBattleRankStatsEntity(
                eventBrawlStarsId = event.value,
                tierRange = tierRange,
                battleDate = date,
                brawlerBrawlStarsId = 10L,
                rankCounts = RankCountsEmbedded.fromMap(mapOf(1 to 2L, 3 to 1L)),
            ),
        )

        // when
        val result = sut.findBrawlerRankStatistics(event, tierRange, date, date)

        // then
        assertThat(result.items).hasSize(1)
        assertThat(result.items.first().brawlerId).isEqualTo(BrawlStarsId(10L))
    }

    @Test
    fun `findBrawlerPairRankStatistics - pair rank entity 의 averageRank 가 weightedRankSum totalCount 로 계산된다`() {
        // given: 1위 2회, 3위 1회 → weightedRankSum = 1*2 + 3*1 = 5, totalCount = 3, avg = 5/3
        val event = BrawlStarsId(201L)
        pairRankStatsJpaRepository.save(
            BrawlerPairBattleRankStatsEntity(
                eventBrawlStarsId = event.value,
                tierRange = tierRange,
                startBattleDate = date,
                endBattleDate = date,
                brawlerBrawlStarsId = 10L,
                pairBrawlerBrawlStarsId = 20L,
                rankCounts = RankCountsEmbedded.fromMap(mapOf(1 to 2L, 3 to 1L)),
            ),
        )

        // when
        val result =
            sut.findBrawlerPairRankStatistics(
                eventId = event,
                brawlerId = BrawlStarsId(10L),
                pairBrawlerId = null,
                tierRange = tierRange,
                startDate = date,
                endDate = date,
            )

        // then
        assertThat(result.items).hasSize(1)
        val item = result.items.first()
        assertThat(item.pairBrawlerId).isEqualTo(BrawlStarsId(20L))
        assertThat(item.totalBattleCount).isEqualTo(3)
        assertThat(item.averageRank).isCloseTo(5.0 / 3.0, org.assertj.core.data.Offset.offset(1e-9))
    }
}
```

- [ ] **Step 2: 컴파일 실패 확인**

Run: `./gradlew :core:core-api:compileTestKotlin`
Expected: `sut.findBrawlerRankStatistics` 등 unresolved.

- [ ] **Step 3: Repository 재구성**

`core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BattleRankStatisticsRepository.kt` 전체 교체:

```kotlin
package com.imstargg.core.application.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.statistics.BrawlerPairRankStatistics
import com.imstargg.core.domain.statistics.BrawlerPairRankStatisticsItem
import com.imstargg.core.domain.statistics.BrawlerRankCount
import com.imstargg.core.domain.statistics.BrawlerRankStatistics
import com.imstargg.core.enums.StatsTierRange
import com.imstargg.core.infrastructure.jpa.toRankCount
import com.imstargg.infrastructure.db.core.statistics.repository.BrawlerBattleRankStatsJpaRepository
import com.imstargg.infrastructure.db.core.statistics.repository.BrawlerPairBattleRankStatsJpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Repository
@Transactional(readOnly = true)
class BattleRankStatisticsRepository(
    private val soloBattleRankJpaRepository: BrawlerBattleRankStatsJpaRepository,
    private val pairBattleRankJpaRepository: BrawlerPairBattleRankStatsJpaRepository,
) {
    fun findBrawlerRankStatistics(
        eventId: BrawlStarsId,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): BrawlerRankStatistics {
        val items =
            soloBattleRankJpaRepository
                .findAllByEventBrawlStarsIdAndTierRangeAndBattleDateBetween(
                    eventId.value, tierRange, startDate, endDate,
                ).groupBy { it.brawlerBrawlStarsId }
                .map { (brawlerBrawlStarsId, grouped) ->
                    BrawlerRankCount(
                        brawlerId = BrawlStarsId(brawlerBrawlStarsId),
                        rankCount = grouped.map { it.rankCounts }.reduce { acc, r -> acc + r }.toRankCount(),
                    )
                }.map { it.toStatisticsItem() }
        return BrawlerRankStatistics(items = items)
    }

    fun findBrawlerPairRankStatistics(
        eventId: BrawlStarsId,
        brawlerId: BrawlStarsId,
        pairBrawlerId: BrawlStarsId?,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): BrawlerPairRankStatistics {
        val items: List<BrawlerPairRankStatisticsItem> =
            pairBattleRankJpaRepository
                .findAllByConditions(
                    eventBrawlStarsId = eventId.value,
                    brawlerBrawlStarsId = brawlerId.value,
                    pairBrawlerBrawlStarsId = pairBrawlerId?.value,
                    tierRange = tierRange,
                    startDate = startDate,
                    endDate = endDate,
                ).map { projection ->
                    BrawlerPairRankStatisticsItem(
                        brawlerId = BrawlStarsId(projection.brawlerBrawlStarsId),
                        pairBrawlerId = BrawlStarsId(projection.pairBrawlerBrawlStarsId),
                        totalBattleCount = projection.totalCount,
                        averageRank =
                            if (projection.totalCount > 0) {
                                projection.weightedRankSum.toDouble() / projection.totalCount
                            } else {
                                0.0
                            },
                    )
                }
        val totalBattleCount =
            pairBattleRankJpaRepository.countTotalBattleCount(
                eventBrawlStarsId = eventId.value,
                brawlerBrawlStarsId = brawlerId.value,
                tierRange = tierRange,
                startDate = startDate,
                endDate = endDate,
            )
        val totalElementCount =
            pairBattleRankJpaRepository.countDistinctPairBrawlers(
                eventBrawlStarsId = eventId.value,
                brawlerBrawlStarsId = brawlerId.value,
                pairBrawlerBrawlStarsId = pairBrawlerId?.value,
                tierRange = tierRange,
                startDate = startDate,
                endDate = endDate,
            )
        return BrawlerPairRankStatistics(
            totalBattleCount = totalBattleCount,
            totalElementCount = totalElementCount,
            items = items,
        )
    }
}
```

> `BrawlerRankCount.toStatisticsItem()` extension 의 위치는 기존 `BattleRankStatisticsService` 에서 사용되던 것과 동일. import 가 안 되면 정의 파일을 grep 해서 import 추가.

- [ ] **Step 4: 컴파일 확인**

Run: `./gradlew :core:core-api:compileKotlin`
Expected: Service / WithCache 가 옛 메서드 호출 중이라 실패 — 다음 Task 9, 10 에서 해결. 다음 Task 로 진행.

---

## Task 9: `BattleRankStatisticsRepositoryWithCache` 재구성

**Files:**
- Modify: `core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BattleRankStatisticsRepositoryWithCache.kt`

- [ ] **Step 1: 전체 교체**

```kotlin
package com.imstargg.core.application.statistics

import com.imstargg.core.config.CacheNames
import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.statistics.BrawlerPairRankStatistics
import com.imstargg.core.domain.statistics.BrawlerRankStatistics
import com.imstargg.core.enums.StatsTierRange
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class BattleRankStatisticsRepositoryWithCache(
    private val battleRankStatisticsRepository: BattleRankStatisticsRepository,
) {
    @Cacheable(
        cacheNames = [CacheNames.STATISTICS],
        key = "'v1:brawlerRankStats:' + #eventId + ':' + #tierRange + ':' + #startDate + ':' + #endDate",
        sync = true,
    )
    fun findBrawlerRankStatistics(
        eventId: BrawlStarsId,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): BrawlerRankStatistics =
        battleRankStatisticsRepository.findBrawlerRankStatistics(eventId, tierRange, startDate, endDate)

    @Cacheable(
        cacheNames = [CacheNames.STATISTICS],
        key =
            "'v1:brawlerPairRankStats:' + #eventId + ':' + #brawlerId + ':' + #pairBrawlerId" +
                " + ':' + #tierRange + ':' + #startDate + ':' + #endDate",
        sync = true,
    )
    fun findBrawlerPairRankStatistics(
        eventId: BrawlStarsId,
        brawlerId: BrawlStarsId,
        pairBrawlerId: BrawlStarsId?,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): BrawlerPairRankStatistics =
        battleRankStatisticsRepository.findBrawlerPairRankStatistics(
            eventId, brawlerId, pairBrawlerId, tierRange, startDate, endDate,
        )
}
```

`BrawlerRankCount`, `BrawlerPairRankCounts` import 모두 제거.

---

## Task 10: `BattleRankStatisticsService` 슬림화

**Files:**
- Modify: `core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BattleRankStatisticsService.kt`

- [ ] **Step 1: 전체 교체**

```kotlin
package com.imstargg.core.application.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.statistics.BrawlerPairRankStatistics
import com.imstargg.core.domain.statistics.BrawlerRankStatistics
import com.imstargg.core.enums.StatsDateRange
import com.imstargg.core.enums.StatsTierRange
import org.springframework.stereotype.Service
import java.time.Clock

@Service
class BattleRankStatisticsService(
    private val clock: Clock,
    private val rankRepository: BattleRankStatisticsRepositoryWithCache,
) {
    fun getBrawlerRankStatistics(
        eventId: BrawlStarsId,
        tierRange: StatsTierRange,
        dateRange: StatsDateRange,
    ): BrawlerRankStatistics {
        val (startDate, endDate) = dateRange.dateRange(clock)
        return rankRepository.findBrawlerRankStatistics(eventId, tierRange, startDate, endDate)
    }

    fun getBrawlerPairRankStatistics(
        eventId: BrawlStarsId,
        brawlerId: BrawlStarsId,
        pairBrawlerId: BrawlStarsId?,
        tierRange: StatsTierRange,
        dateRange: StatsDateRange,
    ): BrawlerPairRankStatistics {
        val (startDate, endDate) = dateRange.dateRange(clock)
        return rankRepository.findBrawlerPairRankStatistics(
            eventId, brawlerId, pairBrawlerId, tierRange, startDate, endDate,
        )
    }
}
```

- [ ] **Step 2: 컴파일 + 테스트 확인**

```bash
./gradlew :core:core-api:compileKotlin :core:core-api:compileTestKotlin
./gradlew :core:core-api:contextTest --tests "com.imstargg.core.application.statistics.BattleRankStatisticsRepositoryTest"
```
Expected: 둘 다 성공.

기존 `BattleRankStatisticsServiceTest` 가 있으면 새 시그니처에 맞춰 갱신 (또는 Repository 통합 테스트로 갈음 후 삭제). 존재 확인:
```bash
find core/core-api/src/test -name "BattleRankStatisticsServiceTest.kt"
```

- [ ] **Step 3: ktlint + 빌드 + 커밋 (Task 8~10 묶음)**

```bash
./gradlew :core:core-api:ktlintFormat
./gradlew :core:core-api:build
```

```bash
git add core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BattleRankStatisticsRepository.kt \
        core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BattleRankStatisticsRepositoryWithCache.kt \
        core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BattleRankStatisticsService.kt \
        core/core-api/src/test/kotlin/com/imstargg/core/application/statistics/BattleRankStatisticsRepositoryTest.kt
# 옛 Service 테스트를 변경했다면 함께 add
git commit -m "refactor(core-api): BattleRank 통계 Repository 가 Statistics 객체 반환하도록 재구성"
```

---

## Task 11: 제거 — 잔존 컨테이너 / SlotsPerGame / 테스트 정리

**Files (Delete):**
- `core/core-api/src/main/kotlin/com/imstargg/core/domain/statistics/SlotsPerGame.kt`
- `core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/RawCounts.kt`
- `core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/EventTotalSlotSum.kt`
- `core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BrawlerPairRankCounts.kt`
- `core/core-api/src/test/kotlin/com/imstargg/core/domain/statistics/SlotsPerGameTest.kt`

- [ ] **Step 1: 잔존 참조 확인**

```bash
grep -rn "SlotsPerGame\|RawCounts\|EventTotalSlotSum\|BrawlerPairRankCounts" core/core-api/src 2>/dev/null
```
Expected: 결과 없음 (이 시점에서 모두 다른 코드로 대체됨). 결과가 있으면 해당 사용처를 먼저 마무리.

- [ ] **Step 2: 파일 삭제**

```bash
git rm core/core-api/src/main/kotlin/com/imstargg/core/domain/statistics/SlotsPerGame.kt \
       core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/RawCounts.kt \
       core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/EventTotalSlotSum.kt \
       core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BrawlerPairRankCounts.kt \
       core/core-api/src/test/kotlin/com/imstargg/core/domain/statistics/SlotsPerGameTest.kt
```

- [ ] **Step 3: 전체 빌드 확인**

```bash
./gradlew :core:core-api:ktlintCheck
./gradlew :core:core-api:build
```
Expected: BUILD SUCCESSFUL (단위 + context + 빌드 모두 통과).

- [ ] **Step 4: db-core / 다른 모듈 영향 확인**

`BrawlerBattleResultStatsEntity.kt` 의 KDoc 에 `SlotsPerGame.forResult` 참조가 있을 수 있음. 검색:
```bash
grep -rn "SlotsPerGame" infrastructure/db-core/src 2>/dev/null
```
나오면 해당 KDoc 의 표현을 `GameSlot.resultSlotsPerGame` 으로 갱신.

- [ ] **Step 5: 전체 모노레포 빌드 (스모크 테스트)**

```bash
./gradlew build
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: 커밋**

```bash
git add -A
git commit -m "refactor(core-api): SlotsPerGame / RawCounts / EventTotalSlotSum / BrawlerPairRankCounts 제거"
```

---

## 마무리

- [ ] **최종 brief 확인**

```bash
git log --oneline refactor/statistics-repository ^main   # 5~7개 커밋 예상
git diff main...refactor/statistics-repository --stat
```

- [ ] **`release-management` 스킬 트리거 (사용자 신호 받으면)**

작업이 완료되고 사용자가 PR/머지 신호를 주면 `release-management` 스킬이 변경된 앱 (core-api) 의 SNAPSHOT 을 자동 bump 한다. 이 plan 자체에서 트리거할 필요는 없다 (사용자 신호가 들어오는 시점에서 자동).

---

## 작업 메모

- **변경된 의존 그래프 (Task 7/10 완료 시점)**:
  ```
  BattleResultStatisticsService → BattleResultStatisticsRepositoryWithCache (Statistics 단위 v1 캐시)
                                    → BattleResultStatisticsRepository (@Transactional readOnly)
                                       ├─ BrawlerBattleResultStatsJpaRepository
                                       ├─ BrawlerEnemyBattleResultStatsJpaRepository
                                       ├─ BrawlerPairBattleResultStatsJpaRepository
                                       └─ GameSlotRepository
                                            └─ BattleEventRepositoryWithCache
  ```
- **API 응답 형태**: 변경 없음 (Service 반환 타입 동일).
- **frontend / admin-frontend / DB 마이그레이션**: 영향 없음.
- **roll-back**: 각 Task 가 별도 커밋이므로 `git revert <commit>` 로 부분 롤백 가능.
