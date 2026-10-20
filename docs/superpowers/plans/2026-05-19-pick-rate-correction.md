# Pick Rate Correction — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 결과형 통계(Result / Pair / Enemy)의 픽률을 "슬롯 점유율" → "전투(게임) 수 분모"로 정정. 통계 엔티티 무변경, 조회 시점에 `BattleEventMode.playerCounts` 로부터 게임수 환산.

**Architecture:**
- `BattleEventMode` 도메인·엔티티에 `playerCounts: List<Int>` (jsonb) 추가, 마이그레이션 기본값 `[3,3]`
- 통계 Repository를 event 단위 보존 형태로 확장 → Service에서 mode lookup 후 도메인의 `create()`에 `slotsPerGameByEvent` 주입 → 도메인에서 동적 환산
- Brawler / Pair / Enemy 통계 모두 동일 패턴, `slotsPerGame` 헬퍼만 통계 종류별로 다른 공식
- Rank 통계는 무변경 (집계 범위가 본인 팀만이라 게임수 환산 불가 — KDoc으로 명시)

**Tech Stack:** Spring Boot 4 + Kotlin, Hibernate 6 (`@JdbcTypeCode(SqlTypes.JSON)` 으로 jsonb 매핑, `RankCountsEmbedded`와 동일 패턴), QueryDSL, Caffeine cache, AssertJ 테스트

**참고 스펙:** [docs/superpowers/specs/2026-05-19-pick-rate-correction-design.md](../specs/2026-05-19-pick-rate-correction-design.md)

---

## File Structure

### Create
- `imstargg-backend/infrastructure/db-core/src/main/resources/db/migration/V260519__add_player_counts_to_battle_event_mode.sql`
- `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/domain/statistics/SlotsPerGame.kt`
- `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/domain/statistics/BrawlerEventResultCount.kt`
- `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/EventTotalSlotSum.kt`
- `imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/request/BattleEventModePlayerCountsUpdateRequest.kt`
- `imstargg-backend/core/core-api/src/test/kotlin/com/imstargg/core/domain/statistics/SlotsPerGameTest.kt`
- `imstargg-backend/core/core-api/src/test/kotlin/com/imstargg/core/domain/statistics/BrawlerResultStatisticsTest.kt`
- `imstargg-backend/infrastructure/db-core/src/test/kotlin/com/imstargg/infrastructure/db/core/brawlstars/event/BattleEventModeEntityTest.kt`

### Modify
- `imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/brawlstars/event/BattleEventModeEntity.kt` — playerCounts 컬럼 추가, KDoc
- `imstargg-backend/infrastructure/db-core/src/main/resources/schema.sql` — 신규 컬럼
- `imstargg-backend/infrastructure/db-core/src/testFixtures/kotlin/com/imstargg/infrastructure/db/core/test/fixture/BattleEventModeEntityFixture.kt` — fixture 메서드
- `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/domain/brawlstars/event/BattleEventMode.kt` — playerCounts 필드
- `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/brawlstars/BattleEventService.kt` — `findModesByEventIds(...)`
- `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BattleResultStatisticsRepository.kt` — event 단위 보존 메서드
- `imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/repository/BrawlerBattleResultStatsJpaRepositoryCustom.kt` + Impl — 신규 QueryDSL 메서드
- `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BattleResultStatisticsRepositoryWithCache.kt` — 시그니처 동기화
- `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BattleResultStatisticsService.kt` — mode lookup 합류
- `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/domain/statistics/BrawlerResultStatistics.kt` — create 시그니처 변경, KDoc
- `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/domain/statistics/BrawlerPairResultStatistics.kt` — 동일
- `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/domain/statistics/BrawlerEnemyResultStatistics.kt` — 동일
- `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BrawlerEnemyResultCounts.kt` / `BrawlerPairResultCounts.kt` — 필드 의미 정정 (이름 유지)
- `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/domain/statistics/StatsRate.kt` — KDoc
- `imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/BrawlerBattleResultStatsEntity.kt` — KDoc
- `imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/BrawlerBattleRankStatsEntity.kt` — KDoc
- `imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/application/BattleEventService.kt` — `updateModePlayerCounts(...)`
- `imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/controller/BattleEventModeController.kt` — PATCH 엔드포인트
- `imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/response/BattleEventModeResponse.kt` — playerCounts 필드
- `imstargg-backend/AGENTS.md` — KDoc 컨벤션 한 줄

---

## Phase 1 — `BattleEventMode` 에 `playerCounts` 추가

### Task 1: DB 스키마 / 마이그레이션

**Files:**
- Create: `imstargg-backend/infrastructure/db-core/src/main/resources/db/migration/V260519__add_player_counts_to_battle_event_mode.sql`
- Modify: `imstargg-backend/infrastructure/db-core/src/main/resources/schema.sql`

- [ ] **Step 1: 마이그레이션 SQL 작성**

[V260519__add_player_counts_to_battle_event_mode.sql](imstargg-backend/infrastructure/db-core/src/main/resources/db/migration/V260519__add_player_counts_to_battle_event_mode.sql) 새로 생성:

```sql
alter table battle_event_mode
    add column if not exists player_counts jsonb not null default '[3, 3]'::jsonb;
```

- [ ] **Step 2: schema.sql 동기화**

`schema.sql` 에서 `battle_event_mode` 테이블 정의에 동일 컬럼을 추가하라:

```sql
player_counts jsonb not null default '[3, 3]'::jsonb,
```

(컬럼 순서는 기존 컬럼 뒤에 추가)

- [ ] **Step 3: 컨텍스트 테스트로 마이그레이션이 깨지지 않는지 확인**

```bash
./gradlew :infrastructure:db-core:contextTest
```

Expected: PASS (Flyway 가 새 마이그레이션을 적용하고 컬럼이 생성됨)

- [ ] **Step 4: 커밋**

```bash
git add imstargg-backend/infrastructure/db-core/src/main/resources/db/migration/V260519__add_player_counts_to_battle_event_mode.sql \
        imstargg-backend/infrastructure/db-core/src/main/resources/schema.sql
git commit -m "feat(db-core): add player_counts jsonb column to battle_event_mode"
```

---

### Task 2: `BattleEventModeEntity` 에 jsonb 매핑 추가 + 검증 메서드

**Files:**
- Modify: `imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/brawlstars/event/BattleEventModeEntity.kt`
- Test: `imstargg-backend/infrastructure/db-core/src/test/kotlin/com/imstargg/infrastructure/db/core/brawlstars/event/BattleEventModeEntityTest.kt`

- [ ] **Step 1: round-trip 테스트 작성 (failing)**

[BattleEventModeEntityTest.kt](imstargg-backend/infrastructure/db-core/src/test/kotlin/com/imstargg/infrastructure/db/core/brawlstars/event/BattleEventModeEntityTest.kt) 새로 생성:

```kotlin
package com.imstargg.infrastructure.db.core.brawlstars.event

import com.imstargg.infrastructure.db.core.DbCoreContextTest
import com.imstargg.infrastructure.db.core.brawlstars.event.repository.BattleEventModeJpaRepository
import com.imstargg.infrastructure.db.core.test.fixture.BattleEventModeEntityFixture.Companion.aBattleEventModeEntity
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class BattleEventModeEntityTest : DbCoreContextTest() {
    @Autowired
    private lateinit var repository: BattleEventModeJpaRepository

    @Test
    fun `playerCounts jsonb round-trip 보존`() {
        // given
        val sut = aBattleEventModeEntity().withPlayerCounts(listOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1)).build()

        // when
        val saved = repository.save(sut)
        repository.flush()
        val reloaded = repository.findById(saved.id!!).orElseThrow()

        // then
        assertThat(reloaded.playerCounts).containsExactly(1, 1, 1, 1, 1, 1, 1, 1, 1, 1)
    }

    @Test
    fun `changePlayerCounts 빈 리스트는 거부`() {
        // given
        val sut = aBattleEventModeEntity().withPlayerCounts(listOf(3, 3)).build()

        // when
        val ex = catchThrowableOfType(IllegalArgumentException::class.java) {
            sut.changePlayerCounts(emptyList())
        }

        // then
        assertThat(ex).hasMessageContaining("playerCounts")
    }

    @Test
    fun `changePlayerCounts 0 이하 인원은 거부`() {
        // given
        val sut = aBattleEventModeEntity().withPlayerCounts(listOf(3, 3)).build()

        // when
        val ex = catchThrowableOfType(IllegalArgumentException::class.java) {
            sut.changePlayerCounts(listOf(3, 0))
        }

        // then
        assertThat(ex).hasMessageContaining(">= 1")
    }
}
```

- [ ] **Step 2: 테스트 실행 (fail 확인)**

```bash
./gradlew :infrastructure:db-core:contextTest --tests "com.imstargg.infrastructure.db.core.brawlstars.event.BattleEventModeEntityTest"
```

Expected: 컴파일 실패 (`withPlayerCounts`, `changePlayerCounts`, `playerCounts` 가 없음)

- [ ] **Step 3: BattleEventModeEntity 수정**

[BattleEventModeEntity.kt](imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/brawlstars/event/BattleEventModeEntity.kt) 전체 교체:

```kotlin
package com.imstargg.infrastructure.db.core.brawlstars.event

import com.imstargg.core.enums.BattleEventResultType
import com.imstargg.infrastructure.db.core.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

@Entity
@Table(
    name = "battle_event_mode",
    indexes = [
        Index(name = "uk_battle_event_mode__brawlstarsid", columnList = "brawlstars_id", unique = true),
    ],
)
class BattleEventModeEntity(
    @Column(name = "brawlstars_id", updatable = false, nullable = false)
    val brawlStarsId: Long,
    @Column(name = "brawlstars_name", updatable = false, nullable = false)
    val brawlStarsName: String,
    resultType: BattleEventResultType = BattleEventResultType.RESULT,
    playerCounts: List<Int> = DEFAULT_PLAYER_COUNTS,
) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "battle_event_mode_id")
    val id: Long? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "result_type", nullable = false)
    var resultType: BattleEventResultType = resultType
        protected set

    /**
     * 각 팀의 인원 수 리스트. 결과형 통계의 픽률 계산에서 게임 수 도출에 사용된다.
     *
     * 예시:
     *  - 3v3: [3, 3]
     *  - Solo Showdown: [1] * 10
     *  - Duels (1v1): [1, 1]
     *  - 비대칭(가상): [1, 5]
     *
     * Rank 모드(`BattleEventResultType.RANK`)에서도 의미는 동일하지만,
     * Rank 통계 엔티티(`BrawlerBattleRankStatsEntity`)는 본인 팀 슬롯만 집계하므로
     * 이 값으로 게임 수를 환산할 수 없다.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "player_counts", columnDefinition = "jsonb", nullable = false)
    var playerCounts: List<Int> = playerCounts
        protected set

    init {
        validatePlayerCounts(playerCounts)
    }

    fun changeResultType(newType: BattleEventResultType) {
        this.resultType = newType
    }

    fun changePlayerCounts(newCounts: List<Int>) {
        validatePlayerCounts(newCounts)
        this.playerCounts = newCounts
    }

    private fun validatePlayerCounts(counts: List<Int>) {
        require(counts.isNotEmpty()) { "playerCounts must not be empty" }
        require(counts.all { it >= 1 }) { "every team size must be >= 1" }
    }

    companion object {
        // API 응답에서 name이 null인 경우 사용
        const val UNKNOWN_NAME = "UNKNOWN"

        /** 절대다수의 모드가 3v3 이므로 기본값. 마이그레이션과 일치. */
        val DEFAULT_PLAYER_COUNTS: List<Int> = listOf(3, 3)
    }
}
```

- [ ] **Step 4: Fixture 에 `withPlayerCounts` 추가**

[BattleEventModeEntityFixture.kt](imstargg-backend/infrastructure/db-core/src/testFixtures/kotlin/com/imstargg/infrastructure/db/core/test/fixture/BattleEventModeEntityFixture.kt) 전체 교체:

```kotlin
package com.imstargg.infrastructure.db.core.test.fixture

import com.imstargg.core.enums.BattleEventResultType
import com.imstargg.infrastructure.db.core.brawlstars.event.BattleEventModeEntity

class BattleEventModeEntityFixture {
    private var brawlStarsId: Long = 1L
    private var brawlStarsName: String = "gemGrab"
    private var resultType: BattleEventResultType = BattleEventResultType.RESULT
    private var playerCounts: List<Int> = listOf(3, 3)

    companion object {
        fun aBattleEventModeEntity() = BattleEventModeEntityFixture()
    }

    fun withBrawlStarsId(value: Long) = apply { brawlStarsId = value }

    fun withBrawlStarsName(value: String) = apply { brawlStarsName = value }

    fun withResultType(value: BattleEventResultType) = apply { resultType = value }

    fun withPlayerCounts(value: List<Int>) = apply { playerCounts = value }

    fun build() =
        BattleEventModeEntity(
            brawlStarsId = brawlStarsId,
            brawlStarsName = brawlStarsName,
            resultType = resultType,
            playerCounts = playerCounts,
        )
}
```

- [ ] **Step 5: 테스트 통과 확인**

```bash
./gradlew :infrastructure:db-core:contextTest --tests "com.imstargg.infrastructure.db.core.brawlstars.event.BattleEventModeEntityTest"
```

Expected: 3개 테스트 모두 PASS

- [ ] **Step 6: 커밋**

```bash
git add imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/brawlstars/event/BattleEventModeEntity.kt \
        imstargg-backend/infrastructure/db-core/src/testFixtures/kotlin/com/imstargg/infrastructure/db/core/test/fixture/BattleEventModeEntityFixture.kt \
        imstargg-backend/infrastructure/db-core/src/test/kotlin/com/imstargg/infrastructure/db/core/brawlstars/event/BattleEventModeEntityTest.kt
git commit -m "feat(db-core): map playerCounts as jsonb on BattleEventModeEntity"
```

---

### Task 3: `BattleEventMode` 도메인에 `playerCounts` 추가

**Files:**
- Modify: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/domain/brawlstars/event/BattleEventMode.kt`
- Find all converters (entity → domain) that build `BattleEventMode` and update them

- [ ] **Step 1: 도메인 클래스에 필드 추가**

[BattleEventMode.kt](imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/domain/brawlstars/event/BattleEventMode.kt) 전체 교체:

```kotlin
package com.imstargg.core.domain.brawlstars.event

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.brawlstars.Messages
import com.imstargg.core.enums.BattleEventResultType

data class BattleEventMode(
    val id: BrawlStarsId,
    val name: Messages,
    val iconImageUrl: String?,
    val resultType: BattleEventResultType,
    /** 각 팀의 인원 수. 결과형 통계 픽률 계산의 분모(게임 수) 환산에 사용. */
    val playerCounts: List<Int>,
)
```

- [ ] **Step 2: BattleEventMode 를 생성하는 변환 지점을 모두 찾기**

```bash
grep -rn "BattleEventMode(" imstargg-backend/core --include="*.kt" \
    | grep -v "BattleEventModeEntity\|BattleEventModeResponse\|BattleEventModeCreate\|BattleEventModeResult" \
    | grep -v "test/"
```

각 위치에서 생성자 호출에 `playerCounts = mode.playerCounts` 를 추가. core-api의 `BattleEventService` 가 사용하는 `BattleEventRepository` / `BattleEventRepositoryWithCache` 내부의 entity → domain mapper 들이 주된 대상.

- [ ] **Step 3: 컴파일 확인**

```bash
./gradlew :core:core-api:compileKotlin
```

Expected: BUILD SUCCESSFUL. 빌드 실패 시 누락된 변환 지점 발견 → 추가 수정.

- [ ] **Step 4: 커밋**

```bash
git add imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/domain/brawlstars/event/BattleEventMode.kt
# + step 2 에서 수정한 mapper 파일들
git commit -m "feat(core-api): add playerCounts to BattleEventMode domain"
```

---

## Phase 2 — `SlotsPerGame` 헬퍼

### Task 4: `SlotsPerGame` 도메인 헬퍼 + 단위 테스트

**Files:**
- Create: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/domain/statistics/SlotsPerGame.kt`
- Test: `imstargg-backend/core/core-api/src/test/kotlin/com/imstargg/core/domain/statistics/SlotsPerGameTest.kt`

- [ ] **Step 1: 실패하는 단위 테스트 작성**

[SlotsPerGameTest.kt](imstargg-backend/core/core-api/src/test/kotlin/com/imstargg/core/domain/statistics/SlotsPerGameTest.kt):

```kotlin
package com.imstargg.core.domain.statistics

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class SlotsPerGameTest {
    @Nested
    inner class forResult {
        @Test
        fun `3v3 은 6 슬롯`() {
            assertThat(SlotsPerGame.forResult(listOf(3, 3))).isEqualTo(6)
        }

        @Test
        fun `솔로 쇼다운 10팀은 10 슬롯`() {
            assertThat(SlotsPerGame.forResult(List(10) { 1 })).isEqualTo(10)
        }

        @Test
        fun `비대칭 1v5는 6 슬롯`() {
            assertThat(SlotsPerGame.forResult(listOf(1, 5))).isEqualTo(6)
        }
    }

    @Nested
    inner class forPair {
        @Test
        fun `3v3 은 12 페어 슬롯`() {
            assertThat(SlotsPerGame.forPair(listOf(3, 3))).isEqualTo(12)
        }

        @Test
        fun `솔로 쇼다운 10팀은 페어가 0`() {
            assertThat(SlotsPerGame.forPair(List(10) { 1 })).isEqualTo(0)
        }

        @Test
        fun `1v5 비대칭은 20 페어 슬롯`() {
            // 0 + 5*4 = 20
            assertThat(SlotsPerGame.forPair(listOf(1, 5))).isEqualTo(20)
        }
    }

    @Nested
    inner class forEnemy {
        @Test
        fun `3v3 은 18 매치업 슬롯`() {
            // (3+3)^2 - (9+9) = 36 - 18 = 18
            assertThat(SlotsPerGame.forEnemy(listOf(3, 3))).isEqualTo(18)
        }

        @Test
        fun `솔로 쇼다운 10팀은 90 매치업 슬롯`() {
            // 10^2 - 10*1 = 100 - 10 = 90
            assertThat(SlotsPerGame.forEnemy(List(10) { 1 })).isEqualTo(90)
        }

        @Test
        fun `1v5 비대칭은 10 매치업 슬롯`() {
            // (1+5)^2 - (1+25) = 36 - 26 = 10
            assertThat(SlotsPerGame.forEnemy(listOf(1, 5))).isEqualTo(10)
        }
    }
}
```

- [ ] **Step 2: 테스트 실행 (fail 확인)**

```bash
./gradlew :core:core-api:unitTest --tests "com.imstargg.core.domain.statistics.SlotsPerGameTest"
```

Expected: 컴파일 실패 (`SlotsPerGame` 미존재)

- [ ] **Step 3: SlotsPerGame 구현**

[SlotsPerGame.kt](imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/domain/statistics/SlotsPerGame.kt):

```kotlin
package com.imstargg.core.domain.statistics

/**
 * 한 게임당 통계 엔티티에 누적되는 "슬롯 수" 를 통계 종류별로 도출한다.
 *
 * 각 Aggregator 의 집계 로직과 일치해야 한다:
 *  - Result: `BrawlerBattleResultStatsAggregator` 가 양 팀 모든 슬롯을 카운트 → `Σ nᵢ`
 *  - Pair:   `BrawlerPairBattleResultStatsAggregator` 가 한 팀 내 방향성 페어를 카운트 → `Σ nᵢ(nᵢ−1)`
 *  - Enemy:  `BrawlerEnemyBattleResultStatsAggregator` 가 모든 적팀 양방향 매치업을 카운트 → `(Σ nᵢ)² − Σ nᵢ²`
 *
 * 환산: `gameCount = totalSlotSum / slotsPerGame(playerCounts)`
 */
object SlotsPerGame {
    fun forResult(playerCounts: List<Int>): Int = playerCounts.sum()

    fun forPair(playerCounts: List<Int>): Int = playerCounts.sumOf { it * (it - 1) }

    fun forEnemy(playerCounts: List<Int>): Int {
        val total = playerCounts.sum()
        return total * total - playerCounts.sumOf { it * it }
    }
}
```

- [ ] **Step 4: 테스트 통과 확인**

```bash
./gradlew :core:core-api:unitTest --tests "com.imstargg.core.domain.statistics.SlotsPerGameTest"
```

Expected: 9개 테스트 모두 PASS

- [ ] **Step 5: 커밋**

```bash
git add imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/domain/statistics/SlotsPerGame.kt \
        imstargg-backend/core/core-api/src/test/kotlin/com/imstargg/core/domain/statistics/SlotsPerGameTest.kt
git commit -m "feat(core-api): add SlotsPerGame helper for result/pair/enemy stats"
```

---

## Phase 3 — Repository: event 단위 슬롯 합 조회

### Task 5: `EventTotalSlotSum` projection + QueryDSL 메서드

**Files:**
- Create: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/EventTotalSlotSum.kt`
- Modify: `imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/repository/BrawlerBattleResultStatsJpaRepositoryCustom.kt`
- Modify: `imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/repository/BrawlerBattleResultStatsJpaRepositoryCustomImpl.kt`

- [ ] **Step 1: domain DTO 생성**

[EventTotalSlotSum.kt](imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/EventTotalSlotSum.kt):

```kotlin
package com.imstargg.core.application.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId

data class EventTotalSlotSum(
    val eventId: BrawlStarsId,
    val totalSlotCount: Long,
)
```

- [ ] **Step 2: Custom 인터페이스에 메서드 추가**

[BrawlerBattleResultStatsJpaRepositoryCustom.kt](imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/repository/BrawlerBattleResultStatsJpaRepositoryCustom.kt) 에 추가:

```kotlin
fun findEventTotalSlotSums(
    eventBrawlStarsId: Long?,
    tierRange: StatsTierRange,
    startDate: LocalDate,
    endDate: LocalDate,
): List<EventSlotSumProjection>

fun findGroupedByEventAndBrawler(
    eventBrawlStarsId: Long?,
    tierRange: StatsTierRange,
    startDate: LocalDate,
    endDate: LocalDate,
): List<BrawlerEventResultProjection>
```

같은 파일 또는 인접한 위치(`repository/` 패키지 — projection 파일들이 어디 있는지 grep으로 확인) 에 projection 추가:

```kotlin
package com.imstargg.infrastructure.db.core.statistics.repository

data class EventSlotSumProjection(
    val eventBrawlStarsId: Long,
    val totalSlotCount: Long,
)

data class BrawlerEventResultProjection(
    val eventBrawlStarsId: Long,
    val brawlerBrawlStarsId: Long,
    val victoryCount: Long,
    val defeatCount: Long,
    val drawCount: Long,
    val starPlayerCount: Long,
)
```

(`BrawlerResultProjection` 이 어디에 있는지 grep으로 찾아 같은 파일 안에 두거나 새 파일로 분리. 프로젝트 컨벤션은 "파일당 1개 클래스" 이므로 파일 분리 권장)

- [ ] **Step 3: QueryDSL 구현 추가**

[BrawlerBattleResultStatsJpaRepositoryCustomImpl.kt](imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/repository/BrawlerBattleResultStatsJpaRepositoryCustomImpl.kt) 에 두 메서드 추가:

```kotlin
override fun findEventTotalSlotSums(
    eventBrawlStarsId: Long?,
    tierRange: StatsTierRange,
    startDate: LocalDate,
    endDate: LocalDate,
): List<EventSlotSumProjection> {
    val eventId = brawlerBattleResultStatsEntity.eventBrawlStarsId
    val totalSlotSum =
        brawlerBattleResultStatsEntity.resultCount.victoryCount.sumLong()
            .add(brawlerBattleResultStatsEntity.resultCount.defeatCount.sumLong())
            .add(brawlerBattleResultStatsEntity.resultCount.drawCount.sumLong())

    return queryFactory
        .select(eventId, totalSlotSum)
        .from(brawlerBattleResultStatsEntity)
        .where(
            eventBrawlStarsId?.let { brawlerBattleResultStatsEntity.eventBrawlStarsId.eq(it) },
            brawlerBattleResultStatsEntity.tierRange.eq(tierRange),
            brawlerBattleResultStatsEntity.battleDate.goe(startDate),
            brawlerBattleResultStatsEntity.battleDate.loe(endDate),
        ).groupBy(eventId)
        .fetch()
        .map { tuple ->
            EventSlotSumProjection(
                eventBrawlStarsId = tuple[eventId]!!,
                totalSlotCount = tuple[totalSlotSum] ?: 0L,
            )
        }
}

override fun findGroupedByEventAndBrawler(
    eventBrawlStarsId: Long?,
    tierRange: StatsTierRange,
    startDate: LocalDate,
    endDate: LocalDate,
): List<BrawlerEventResultProjection> {
    val eventId = brawlerBattleResultStatsEntity.eventBrawlStarsId
    val brawlerId = brawlerBattleResultStatsEntity.brawlerBrawlStarsId
    val victorySum = brawlerBattleResultStatsEntity.resultCount.victoryCount.sumLong()
    val defeatSum = brawlerBattleResultStatsEntity.resultCount.defeatCount.sumLong()
    val drawSum = brawlerBattleResultStatsEntity.resultCount.drawCount.sumLong()
    val starPlayerSum = brawlerBattleResultStatsEntity.starPlayerCount.sumLong()

    return queryFactory
        .select(eventId, brawlerId, victorySum, defeatSum, drawSum, starPlayerSum)
        .from(brawlerBattleResultStatsEntity)
        .where(
            eventBrawlStarsId?.let { brawlerBattleResultStatsEntity.eventBrawlStarsId.eq(it) },
            brawlerBattleResultStatsEntity.tierRange.eq(tierRange),
            brawlerBattleResultStatsEntity.battleDate.goe(startDate),
            brawlerBattleResultStatsEntity.battleDate.loe(endDate),
        ).groupBy(eventId, brawlerId)
        .fetch()
        .map { tuple ->
            BrawlerEventResultProjection(
                eventBrawlStarsId = tuple[eventId]!!,
                brawlerBrawlStarsId = tuple[brawlerId]!!,
                victoryCount = tuple[victorySum] ?: 0L,
                defeatCount = tuple[defeatSum] ?: 0L,
                drawCount = tuple[drawSum] ?: 0L,
                starPlayerCount = tuple[starPlayerSum] ?: 0L,
            )
        }
}
```

- [ ] **Step 4: 컴파일 확인**

```bash
./gradlew :infrastructure:db-core:compileKotlin
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 5: 커밋**

```bash
git add imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/EventTotalSlotSum.kt \
        imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/repository/BrawlerBattleResultStatsJpaRepositoryCustom.kt \
        imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/repository/BrawlerBattleResultStatsJpaRepositoryCustomImpl.kt
# + projection 파일 (분리한 경우)
git commit -m "feat(db-core): add event-preserving queries on brawler result stats"
```

---

### Task 6: Application `BattleResultStatisticsRepository` 에 event 보존 메서드 추가

**Files:**
- Create: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/domain/statistics/BrawlerEventResultCount.kt`
- Modify: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BattleResultStatisticsRepository.kt`

- [ ] **Step 1: domain record 생성**

[BrawlerEventResultCount.kt](imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/domain/statistics/BrawlerEventResultCount.kt):

```kotlin
package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId

data class BrawlerEventResultCount(
    val eventId: BrawlStarsId,
    val brawlerId: BrawlStarsId,
    val resultCount: ResultCount,
    val starPlayerCount: Long,
)
```

- [ ] **Step 2: Repository에 두 메서드 추가**

[BattleResultStatisticsRepository.kt](imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BattleResultStatisticsRepository.kt) 의 클래스 본문에 추가:

```kotlin
fun findBrawlerEventResultCounts(
    eventId: BrawlStarsId?,
    tierRange: StatsTierRange,
    startDate: LocalDate,
    endDate: LocalDate,
): List<BrawlerEventResultCount> =
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

fun findEventTotalSlotSums(
    eventId: BrawlStarsId?,
    tierRange: StatsTierRange,
    startDate: LocalDate,
    endDate: LocalDate,
): List<EventTotalSlotSum> =
    battleResultJpaRepository
        .findEventTotalSlotSums(
            eventBrawlStarsId = eventId?.value,
            tierRange = tierRange,
            startDate = startDate,
            endDate = endDate,
        ).map { projection ->
            EventTotalSlotSum(
                eventId = BrawlStarsId(projection.eventBrawlStarsId),
                totalSlotCount = projection.totalSlotCount,
            )
        }
```

기존 `findBrawlerResultCounts` 는 호환을 위해 일단 그대로 둔다 (Task 9에서 호출부 정리 후 제거).

- [ ] **Step 3: 컴파일 확인**

```bash
./gradlew :core:core-api:compileKotlin
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 커밋**

```bash
git add imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/domain/statistics/BrawlerEventResultCount.kt \
        imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BattleResultStatisticsRepository.kt
git commit -m "feat(core-api): expose event-preserving result count queries"
```

---

## Phase 4 — `BattleEventService.findModesByEventIds`

### Task 7: 다건 이벤트 → mode 조회 메서드

**Files:**
- Modify: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/brawlstars/BattleEventService.kt`

- [ ] **Step 1: 메서드 추가**

[BattleEventService.kt](imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/brawlstars/BattleEventService.kt) 클래스 본문에:

```kotlin
/**
 * 여러 이벤트 ID 에 대해 한 번에 mode 정보를 조회한다.
 *
 * 결과형 통계 픽률 계산에서 event 단위 slotsPerGame 환산에 사용된다.
 * 결과 Map은 mode 정보가 존재하는 이벤트만 포함한다 (mode 미연결 이벤트는 제외).
 */
fun findModesByEventIds(eventIds: Collection<BrawlStarsId>): Map<BrawlStarsId, BattleEventMode> =
    eventIds.toSet()
        .mapNotNull { id -> battleEventRepositoryWithCache.find(id)?.mode?.let { id to it } }
        .toMap()
```

> 주의: 위 구현은 `BattleEvent.mode: BattleEventMode?` 라고 가정. 실제 `BattleEvent` 정의에 따라 mode가 non-nullable 이면 단순화. `find()` 가 캐시를 활용하므로 N+1 우려 적음.

- [ ] **Step 2: 컴파일 확인**

```bash
./gradlew :core:core-api:compileKotlin
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 커밋**

```bash
git add imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/brawlstars/BattleEventService.kt
git commit -m "feat(core-api): add findModesByEventIds bulk lookup on BattleEventService"
```

---

## Phase 5 — 도메인 통계 모델 갱신

### Task 8: `BrawlerResultStatistics.create` 시그니처 변경

**Files:**
- Modify: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/domain/statistics/BrawlerResultStatistics.kt`
- Test: `imstargg-backend/core/core-api/src/test/kotlin/com/imstargg/core/domain/statistics/BrawlerResultStatisticsTest.kt`

- [ ] **Step 1: 실패하는 단위 테스트 작성**

[BrawlerResultStatisticsTest.kt](imstargg-backend/core/core-api/src/test/kotlin/com/imstargg/core/domain/statistics/BrawlerResultStatisticsTest.kt):

```kotlin
package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class BrawlerResultStatisticsTest {
    @Nested
    inner class create {
        @Test
        fun `단일 3v3 이벤트의 픽률은 게임 수 분모로 계산된다`() {
            // given: 3v3 이벤트 100게임 → 600 슬롯, 브롤러 A 가 300 슬롯
            val eventId = BrawlStarsId(1L)
            val brawlerA = BrawlStarsId(10L)
            val brawlerB = BrawlStarsId(11L)
            val rows = listOf(
                BrawlerEventResultCount(eventId, brawlerA, ResultCount(150, 150, 0), 30),
                BrawlerEventResultCount(eventId, brawlerB, ResultCount(150, 150, 0), 30),
                // 잔여 300 슬롯이 다른 브롤러들에 분포 (테스트에선 단순화: 다른 브롤러 1명에 300)
                BrawlerEventResultCount(eventId, BrawlStarsId(12L), ResultCount(150, 150, 0), 60),
            )
            val slotsPerGameByEvent = mapOf(eventId to 6)

            // when
            val sut = BrawlerResultStatistics.create(rows, slotsPerGameByEvent)

            // then: totalBattleCount = 900/6 = 150
            assertThat(sut.totalBattleCount).isEqualTo(150)
            // pickRate(A) = 300 / 150 = 2.0 (3v3에서 가능한 최댓값 부근)
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
            val rows = listOf(
                BrawlerEventResultCount(event3v3, brawler, ResultCount(60, 0, 0), 0),
                BrawlerEventResultCount(event3v3, BrawlStarsId(99L), ResultCount(150, 150, 0), 0),
                BrawlerEventResultCount(eventSolo, brawler, ResultCount(50, 0, 0), 0),
                BrawlerEventResultCount(eventSolo, BrawlStarsId(99L), ResultCount(225, 225, 0), 0),
            )
            val slotsPerGameByEvent = mapOf(event3v3 to 6, eventSolo to 10)

            // when
            val sut = BrawlerResultStatistics.create(rows, slotsPerGameByEvent)

            // then: totalBattleCount = 360/6 + 500/10 = 60 + 50 = 110
            assertThat(sut.totalBattleCount).isEqualTo(110)
            // brawler 슬롯 합 = 60 + 50 = 110, pickRate = 110/110 = 1.0
            val item = sut.items.first { it.brawlerId == brawler }
            assertThat(item.pickRate).isEqualTo(1.0)
        }

        @Test
        fun `slotsPerGameByEvent에 누락된 이벤트가 있으면 예외`() {
            // given
            val eventId = BrawlStarsId(1L)
            val rows = listOf(
                BrawlerEventResultCount(eventId, BrawlStarsId(10L), ResultCount(1, 0, 0), 0),
            )
            val slotsPerGameByEvent = emptyMap<BrawlStarsId, Int>()

            // when
            val ex = catchThrowableOfType(IllegalStateException::class.java) {
                BrawlerResultStatistics.create(rows, slotsPerGameByEvent)
            }

            // then
            assertThat(ex).hasMessageContaining("playerCounts")
        }

        @Test
        fun `빈 입력은 totalBattleCount 0`() {
            // given / when
            val sut = BrawlerResultStatistics.create(emptyList(), emptyMap())

            // then
            assertThat(sut.totalBattleCount).isEqualTo(0)
            assertThat(sut.items).isEmpty()
        }
    }
}
```

- [ ] **Step 2: 테스트 실행 (fail 확인)**

```bash
./gradlew :core:core-api:unitTest --tests "com.imstargg.core.domain.statistics.BrawlerResultStatisticsTest"
```

Expected: 컴파일 실패 또는 메서드 시그니처 불일치

- [ ] **Step 3: BrawlerResultStatistics 재작성**

[BrawlerResultStatistics.kt](imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/domain/statistics/BrawlerResultStatistics.kt) 전체 교체:

```kotlin
package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId

data class BrawlerResultStatisticsItem(
    val brawlerId: BrawlStarsId,
    /** 조회 범위 전체의 전투(게임) 수. 분모와 동일. */
    val totalBattleCount: Long,
    val pickRate: Double,
    val winRate: Double,
    val starPlayerRate: Double,
    val tierScore: BrawlerTierScore,
)

/**
 * 결과형 통계 - 브롤러 단위.
 *
 * `totalBattleCount` 의 의미: 조회 범위의 **게임 수** (이전엔 슬롯 합이었음 — 2026-05-19 정정).
 * 픽률 = 해당 브롤러가 등장한 슬롯 수 / 게임 수 → 3v3 의 경우 픽률 합이 ≈ 6.0.
 */
data class BrawlerResultStatistics(
    val totalBattleCount: Long,
    val items: List<BrawlerResultStatisticsItem>,
) {
    companion object {
        fun create(
            rows: List<BrawlerEventResultCount>,
            slotsPerGameByEvent: Map<BrawlStarsId, Int>,
        ): BrawlerResultStatistics {
            if (rows.isEmpty()) {
                return BrawlerResultStatistics(totalBattleCount = 0L, items = emptyList())
            }

            val totalBattleCount: Long =
                rows.groupBy { it.eventId }
                    .map { (eventId, eventRows) ->
                        val eventSlotSum = eventRows.sumOf { it.resultCount.totalCount }
                        val slotsPerGame =
                            slotsPerGameByEvent[eventId]
                                ?: error("playerCounts missing for event=$eventId")
                        eventSlotSum / slotsPerGame
                    }.sum()

            val brawlerAggregates =
                rows.groupBy { it.brawlerId }
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
}
```

> 주의: 기존 `create(counts: List<BrawlerResultCount>)` 는 시그니처가 바뀐다. 사용 지점은 Task 9에서 정리.

- [ ] **Step 4: 테스트 통과 확인**

```bash
./gradlew :core:core-api:unitTest --tests "com.imstargg.core.domain.statistics.BrawlerResultStatisticsTest"
```

Expected: 4 테스트 PASS

- [ ] **Step 5: 커밋**

```bash
git add imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/domain/statistics/BrawlerResultStatistics.kt \
        imstargg-backend/core/core-api/src/test/kotlin/com/imstargg/core/domain/statistics/BrawlerResultStatisticsTest.kt
git commit -m "refactor(core-api): redefine BrawlerResultStatistics pickRate by gameCount"
```

---

### Task 9: Service / Cache wrapper 호출부 정정 (Brawler)

**Files:**
- Modify: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BattleResultStatisticsRepositoryWithCache.kt`
- Modify: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BattleResultStatisticsService.kt`
- Modify: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BattleResultStatisticsRepository.kt` (기존 unused 메서드 제거)

- [ ] **Step 1: Cache wrapper 의 `findBrawlerResultCounts` 를 event-preserving 으로 교체**

[BattleResultStatisticsRepositoryWithCache.kt](imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BattleResultStatisticsRepositoryWithCache.kt) 의 `findBrawlerResultCounts` 를 다음으로 교체:

```kotlin
@Cacheable(
    cacheNames = [CacheNames.STATISTICS],
    key = "'brawlerEventResult:' + #eventId + ':' + #tierRange + ':' + #startDate + ':' + #endDate",
    sync = true,
)
fun findBrawlerEventResultCounts(
    eventId: BrawlStarsId?,
    tierRange: StatsTierRange,
    startDate: LocalDate,
    endDate: LocalDate,
): List<BrawlerEventResultCount> =
    battleResultStatisticsRepository.findBrawlerEventResultCounts(
        eventId = eventId,
        tierRange = tierRange,
        startDate = startDate,
        endDate = endDate,
    )
```

기존 `findBrawlerResultCounts` 캐시 메서드는 삭제하라 (호출부가 Service만이고 Service도 곧 갱신됨).

- [ ] **Step 2: Service.getBrawlerResultStatistics 재작성**

[BattleResultStatisticsService.kt](imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BattleResultStatisticsService.kt) 에 의존성 추가 및 메서드 교체:

```kotlin
@Service
class BattleResultStatisticsService(
    private val clock: Clock,
    private val resultRepository: BattleResultStatisticsRepositoryWithCache,
    private val battleEventService: BattleEventService,
) {
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
        val slotsPerGameByEvent =
            battleEventService
                .findModesByEventIds(rows.map { it.eventId }.distinct())
                .mapValues { (_, mode) -> SlotsPerGame.forResult(mode.playerCounts) }
        return BrawlerResultStatistics.create(rows, slotsPerGameByEvent)
    }

    // 기존 getBrawlerPairResultStatistics / getEnemyResultStatistics / getDailyResultStatistics 는 다음 Task 에서 갱신
    ...
}
```

> `import com.imstargg.core.application.brawlstars.BattleEventService` 추가, `com.imstargg.core.domain.statistics.SlotsPerGame` 추가.

- [ ] **Step 3: Application Repository 의 기존 `findBrawlerResultCounts` 제거**

[BattleResultStatisticsRepository.kt](imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BattleResultStatisticsRepository.kt) 의 기존 `findBrawlerResultCounts(eventId, tierRange, startDate, endDate): List<BrawlerResultCount>` 메서드를 삭제. JPA repo 의 `findGroupedByBrawler` 는 다른 호출자 없으면 같이 삭제.

호출자 확인:

```bash
grep -rn "findGroupedByBrawler\|\.findBrawlerResultCounts(" imstargg-backend --include="*.kt"
```

다른 호출자가 없으면 제거. 있으면 그것도 갱신 필요.

- [ ] **Step 4: 컴파일 + 기존 컨텍스트 테스트 실행**

```bash
./gradlew :core:core-api:compileKotlin
./gradlew :core:core-api:contextTest --tests "*Statistics*"
```

Expected: BUILD SUCCESSFUL, 기존 테스트가 깨졌다면 새 시그니처에 맞게 갱신 (test fixture 보강)

- [ ] **Step 5: 커밋**

```bash
git add imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BattleResultStatisticsRepositoryWithCache.kt \
        imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BattleResultStatisticsService.kt \
        imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BattleResultStatisticsRepository.kt
# + JPA repo 정리 / 컨텍스트 테스트 보정
git commit -m "refactor(core-api): wire BrawlerResultStatistics via event-aware repository"
```

---

### Task 10: `BrawlerPairResultStatistics` / `BrawlerEnemyResultStatistics` 동일 적용

**Files:**
- Modify: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/domain/statistics/BrawlerPairResultStatistics.kt`
- Modify: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/domain/statistics/BrawlerEnemyResultStatistics.kt`
- Modify: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BrawlerEnemyResultCounts.kt`
- Modify: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BrawlerPairResultCounts.kt`
- Modify: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BattleResultStatisticsRepositoryWithCache.kt`
- Modify: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BattleResultStatisticsService.kt`

> 변경 핵심: Pair/Enemy 의 `totalBattleCount` 는 "조회 범위의 게임 수" 로 의미가 통일된다. 기존 `countXxxTotalBattleCount` (브롤러 필터 슬롯 합) 호출은 제거하고, `findEventTotalSlotSums` 로 산출.

- [ ] **Step 1: `BrawlerPairResultCounts` / `BrawlerEnemyResultCounts` 의미 정정**

[BrawlerPairResultCounts.kt](imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BrawlerPairResultCounts.kt):

```kotlin
package com.imstargg.core.application.statistics

import com.imstargg.core.domain.statistics.BrawlerPairResultCount

/**
 * @property totalBattleCount 조회 범위의 게임 수 (이벤트별 slots 합 / slotsPerGame.forResult)
 * @property totalElementCount (브롤러, 페어 브롤러) 조합의 distinct count
 */
data class BrawlerPairResultCounts(
    val items: List<BrawlerPairResultCount>,
    val totalBattleCount: Long,
    val totalElementCount: Long,
)
```

[BrawlerEnemyResultCounts.kt](imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BrawlerEnemyResultCounts.kt) 도 동일하게 KDoc 만 갱신.

- [ ] **Step 2: Cache wrapper 의 pair / enemy 메서드 재작성**

[BattleResultStatisticsRepositoryWithCache.kt](imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BattleResultStatisticsRepositoryWithCache.kt) 의 `findBrawlerEnemyResult` / `findBrawlerPairResult` 안의 `countXxxTotalBattleCount(...)` 호출을 제거하고, totalBattleCount 계산은 Service 레이어로 옮긴다.

즉, Cache wrapper 는 raw items 와 totalElementCount 만 반환 (캐시 이점 유지). totalBattleCount 는 Service 에서 별도 계산.

```kotlin
@Cacheable(
    cacheNames = [CacheNames.STATISTICS],
    key = "'enemyResultRaw:' + #eventId + ':' + #brawlerId + ':' + #tierRange + ':' + #startDate + ':' + #endDate",
    sync = true,
)
fun findBrawlerEnemyResultRaw(
    eventId: BrawlStarsId?,
    brawlerId: BrawlStarsId,
    tierRange: StatsTierRange,
    startDate: LocalDate,
    endDate: LocalDate,
): RawCounts<BrawlerEnemyResultCount> {
    val items = battleResultStatisticsRepository.findBrawlerEnemyResultCounts(eventId, brawlerId, tierRange, startDate, endDate)
    val totalElementCount = battleResultStatisticsRepository.countEnemyResultBy(eventId, brawlerId, tierRange, startDate, endDate)
    return RawCounts(items, totalElementCount)
}

// Pair 동일 패턴
```

신규 dto:

```kotlin
package com.imstargg.core.application.statistics

data class RawCounts<T>(
    val items: List<T>,
    val totalElementCount: Long,
)
```

또한 Cache wrapper 에 `findEventTotalSlotSums` 의 캐시 래퍼 추가:

```kotlin
@Cacheable(
    cacheNames = [CacheNames.STATISTICS],
    key = "'eventSlotSums:' + #eventId + ':' + #tierRange + ':' + #startDate + ':' + #endDate",
    sync = true,
)
fun findEventTotalSlotSums(
    eventId: BrawlStarsId?,
    tierRange: StatsTierRange,
    startDate: LocalDate,
    endDate: LocalDate,
): List<EventTotalSlotSum> =
    battleResultStatisticsRepository.findEventTotalSlotSums(eventId, tierRange, startDate, endDate)
```

- [ ] **Step 3: 도메인 `BrawlerPairResultStatistics.create` 재작성**

[BrawlerPairResultStatistics.kt](imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/domain/statistics/BrawlerPairResultStatistics.kt) 전체 교체:

```kotlin
package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId

data class BrawlerPairResultStatisticsItem(
    val brawlerId: BrawlStarsId,
    val pairBrawlerId: BrawlStarsId,
    /** 조회 범위의 게임 수 (분모와 동일) */
    val totalBattleCount: Long,
    val pickRate: Double,
    val winRate: Double,
)

data class BrawlerPairResultStatistics(
    val totalBattleCount: Long,
    val totalElementCount: Long,
    val items: List<BrawlerPairResultStatisticsItem>,
) {
    companion object {
        fun create(
            totalBattleCount: Long,
            totalElementCount: Long,
            counts: List<BrawlerPairResultCount>,
        ): BrawlerPairResultStatistics {
            val items =
                counts.map { count ->
                    BrawlerPairResultStatisticsItem(
                        brawlerId = count.brawlerId,
                        pairBrawlerId = count.pairBrawlerId,
                        totalBattleCount = totalBattleCount,
                        pickRate = StatsRate.pickRate(count.resultCount.totalCount, totalBattleCount),
                        winRate = count.resultCount.winRate,
                    )
                }
            return BrawlerPairResultStatistics(totalBattleCount, totalElementCount, items)
        }
    }
}
```

[BrawlerEnemyResultStatistics.kt](imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/domain/statistics/BrawlerEnemyResultStatistics.kt) 도 동일한 패턴 적용 (시그니처 무변경, 의미만 통일).

- [ ] **Step 4: Service 의 pair / enemy 메서드에서 totalBattleCount 계산**

[BattleResultStatisticsService.kt](imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BattleResultStatisticsService.kt) 의 두 메서드를 다음으로 교체:

```kotlin
fun getBrawlerPairResultStatistics(
    eventId: BrawlStarsId?,
    brawlerId: BrawlStarsId,
    pairBrawlerId: BrawlStarsId?,
    tierRange: StatsTierRange,
    dateRange: StatsDateRange,
): BrawlerPairResultStatistics {
    val (startDate, endDate) = dateRange.dateRange(clock)
    val raw = resultRepository.findBrawlerPairResultRaw(eventId, brawlerId, pairBrawlerId, tierRange, startDate, endDate)
    val totalBattleCount = computeTotalBattleCount(eventId, tierRange, startDate, endDate)
    return BrawlerPairResultStatistics.create(totalBattleCount, raw.totalElementCount, raw.items)
}

fun getEnemyResultStatistics(
    eventId: BrawlStarsId?,
    brawlerId: BrawlStarsId,
    tierRange: StatsTierRange,
    dateRange: StatsDateRange,
): BrawlerEnemyResultStatistics {
    val (startDate, endDate) = dateRange.dateRange(clock)
    val raw = resultRepository.findBrawlerEnemyResultRaw(eventId, brawlerId, tierRange, startDate, endDate)
    val totalBattleCount = computeTotalBattleCount(eventId, tierRange, startDate, endDate)
    return BrawlerEnemyResultStatistics.create(totalBattleCount, raw.totalElementCount, raw.items)
}

private fun computeTotalBattleCount(
    eventId: BrawlStarsId?,
    tierRange: StatsTierRange,
    startDate: LocalDate,
    endDate: LocalDate,
): Long {
    val eventSlotSums = resultRepository.findEventTotalSlotSums(eventId, tierRange, startDate, endDate)
    val modes = battleEventService.findModesByEventIds(eventSlotSums.map { it.eventId }.distinct())
    return eventSlotSums.sumOf { sum ->
        val mode = modes[sum.eventId] ?: error("playerCounts missing for event=${sum.eventId}")
        sum.totalSlotCount / SlotsPerGame.forResult(mode.playerCounts)
    }
}
```

> `computeTotalBattleCount` 가 brawler-only `SlotsPerGame.forResult` 를 사용하는 이유: 한 게임당 BrawlerBattleResultStatsEntity 슬롯 수가 결과형 공식이라 그렇다. Pair/Enemy entity 와 무관하게 게임수 자체는 Brawler entity 로부터 도출.

- [ ] **Step 5: 컴파일 + 통합 테스트**

```bash
./gradlew :core:core-api:contextTest --tests "*Statistics*"
```

Expected: BUILD SUCCESSFUL. 깨진 테스트는 새 시그니처에 맞게 갱신.

- [ ] **Step 6: 커밋**

```bash
git add imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/domain/statistics/BrawlerPairResultStatistics.kt \
        imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/domain/statistics/BrawlerEnemyResultStatistics.kt \
        imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BrawlerEnemyResultCounts.kt \
        imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BrawlerPairResultCounts.kt \
        imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BattleResultStatisticsRepositoryWithCache.kt \
        imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BattleResultStatisticsService.kt
# + 신규 RawCounts.kt, 테스트 보정
git commit -m "refactor(core-api): redefine pair/enemy pickRate via gameCount denominator"
```

---

## Phase 6 — `StatsRate` KDoc

### Task 11: `StatsRate.pickRate` 분모 의미를 KDoc 으로 명시

**Files:**
- Modify: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/domain/statistics/StatsRate.kt`

- [ ] **Step 1: KDoc 추가**

[StatsRate.kt](imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/domain/statistics/StatsRate.kt) 의 `pickRate` 함수 위에:

```kotlin
/**
 * 픽률 = 슬롯(브롤러 등장) 수 / 전투(게임) 수.
 *
 * 분모 `totalBattleCount` 는 **게임 수** 여야 한다 (이전에는 슬롯 합으로 잘못 사용됨 — 2026-05-19 정정).
 * 슬롯 합 ↔ 게임 수 환산은 `SlotsPerGame` 참조.
 */
fun pickRate(
    battleCount: Long,
    totalBattleCount: Long,
): Double = if (totalBattleCount > 0) battleCount.toDouble() / totalBattleCount else 0.0
```

- [ ] **Step 2: 커밋**

```bash
git add imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/domain/statistics/StatsRate.kt
git commit -m "docs(core-api): clarify pickRate denominator semantics in StatsRate"
```

---

## Phase 7 — KDoc 히스토리 보존 (통계 엔티티)

### Task 12: `BrawlerBattleResultStatsEntity` / `BrawlerBattleRankStatsEntity` KDoc

**Files:**
- Modify: `imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/BrawlerBattleResultStatsEntity.kt`
- Modify: `imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/BrawlerBattleRankStatsEntity.kt`

- [ ] **Step 1: Result 엔티티에 KDoc 추가**

[BrawlerBattleResultStatsEntity.kt](imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/BrawlerBattleResultStatsEntity.kt) 의 클래스 선언 위에:

```kotlin
/**
 * 결과형(승/패/무) 배틀 통계의 브롤러 단위 집계.
 *
 * ## 집계 단위
 * 브롤러 × 이벤트 × 티어 구간 × 날짜.
 *
 * ## 집계 범위
 * 한 배틀의 **양 팀 모든 슬롯**이 카운트된다 — 자기 팀은 본래 결과로, 적군 팀은 반대 결과로.
 * 3v3 의 경우 한 게임당 6 슬롯이 다수 row 에 분산 기록된다.
 *
 * ## 게임 수 도출
 * `BattleEventMode.playerCounts` 가 알려진다면 게임 수를 정확히 도출할 수 있다:
 *
 *     eventSlotSum = Σ totalCount over brawlers within event
 *     gameCount    = eventSlotSum / SlotsPerGame.forResult(playerCounts)
 *
 * 픽률 (= 브롤러 슬롯 수 / 게임 수) 계산이 이 관계에 의존한다.
 * `BrawlerBattleRankStatsEntity` 와의 결정적 차이는 이 엔티티가 양 팀을 모두 집계한다는 점이다.
 */
```

- [ ] **Step 2: Rank 엔티티에 KDoc 추가**

[BrawlerBattleRankStatsEntity.kt](imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/BrawlerBattleRankStatsEntity.kt) 의 클래스 선언 위에:

```kotlin
/**
 * 순위형(1위~N위) 배틀 통계의 브롤러 단위 집계.
 *
 * ## 집계 단위
 * 브롤러 × 이벤트 × 티어 구간 × 날짜.
 *
 * ## 집계 범위
 * 한 배틀의 **본인 팀 슬롯만** 카운트된다 — 순위형 배틀에는 적군 개념이 없기 때문.
 * 예: Solo Showdown 10팀 1게임은 통계 entity 상 단 1 슬롯만 기록된다.
 *
 * ## 게임 수 정확 계산 불가
 * Result 통계와 달리 한 게임의 **일부 슬롯만** 통계에 들어가므로
 * `BattleEventMode.playerCounts` 만으로는 게임 수를 도출할 수 없다.
 *
 * 따라서 결과형 통계(`BrawlerResultStatistics` 등)에 도입된
 * "게임 수 분모 픽률" 정의를 이 엔티티에 그대로 옮길 수 없다.
 * Rank 통계용 픽률을 도입하려면 별도의 게임 수 집계 메커니즘이 필요하다 (2026-05-19 결정).
 */
```

- [ ] **Step 3: 빌드 확인 (컴파일 + 린트)**

```bash
./gradlew :infrastructure:db-core:ktlintCheck :infrastructure:db-core:compileKotlin
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 커밋**

```bash
git add imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/BrawlerBattleResultStatsEntity.kt \
        imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/BrawlerBattleRankStatsEntity.kt
git commit -m "docs(db-core): record result-vs-rank aggregation difference on stats entities"
```

---

## Phase 8 — Admin PATCH 엔드포인트

### Task 13: admin `playerCounts` 수정 엔드포인트

**Files:**
- Create: `imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/request/BattleEventModePlayerCountsUpdateRequest.kt`
- Modify: `imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/application/BattleEventService.kt`
- Modify: `imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/controller/BattleEventModeController.kt`
- Modify: `imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/response/BattleEventModeResponse.kt`

- [ ] **Step 1: Request DTO 생성**

[BattleEventModePlayerCountsUpdateRequest.kt](imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/request/BattleEventModePlayerCountsUpdateRequest.kt):

```kotlin
package com.imstargg.core.admin.request

data class BattleEventModePlayerCountsUpdateRequest(
    val playerCounts: List<Int>,
)
```

- [ ] **Step 2: Response 에 playerCounts 필드 추가**

[BattleEventModeResponse.kt](imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/response/BattleEventModeResponse.kt) 를 열어 생성자에 `val playerCounts: List<Int>` 추가. `findAllModes()` 에서 `playerCounts = mode.playerCounts` 매핑.

- [ ] **Step 3: admin BattleEventService 에 update 메서드 추가**

[BattleEventService.kt (admin)](imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/application/BattleEventService.kt) 에:

```kotlin
@Transactional
fun updateModePlayerCounts(
    brawlStarsId: Long,
    playerCounts: List<Int>,
) {
    val entity =
        battleEventModeJpaRepository.findByBrawlStarsId(brawlStarsId)
            ?: throw IllegalArgumentException("BattleEventMode not found: $brawlStarsId")
    entity.changePlayerCounts(playerCounts)
}
```

- [ ] **Step 4: Controller 에 엔드포인트 추가**

[BattleEventModeController.kt](imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/controller/BattleEventModeController.kt) 에 `updateResultType` 패턴을 그대로 따른 새 핸들러 추가:

```kotlin
@PutMapping("/event-modes/{brawlStarsId}/player-counts")
fun updatePlayerCounts(
    @PathVariable brawlStarsId: Long,
    @RequestBody request: BattleEventModePlayerCountsUpdateRequest,
): ResponseEntity<Unit> {
    battleEventService.updateModePlayerCounts(brawlStarsId, request.playerCounts)
    return ResponseEntity.ok().build()
}
```

> import 의 `BattleEventModeResultTypeUpdateRequest` 옆에 `BattleEventModePlayerCountsUpdateRequest` 추가.

- [ ] **Step 5: 컨텍스트 테스트로 동작 확인**

```bash
./gradlew :core:core-admin:contextTest --tests "*BattleEventModeController*"
```

해당 테스트가 없다면 기존 admin 테스트 패턴을 참조해 한 건 추가하라:

```kotlin
@Test
fun `playerCounts 변경 200`() {
    // given
    val mode = battleEventModeJpaRepository.save(aBattleEventModeEntity().withBrawlStarsId(1L).build())

    // when
    mockMvc.perform(
        put("/event-modes/1/player-counts")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"playerCounts": [1, 1, 1, 1, 1, 1, 1, 1, 1, 1]}"""),
    ).andExpect(status().isOk)

    // then
    val reloaded = battleEventModeJpaRepository.findById(mode.id!!).orElseThrow()
    assertThat(reloaded.playerCounts).containsExactly(1, 1, 1, 1, 1, 1, 1, 1, 1, 1)
}
```

- [ ] **Step 6: 커밋**

```bash
git add imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/request/BattleEventModePlayerCountsUpdateRequest.kt \
        imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/application/BattleEventService.kt \
        imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/controller/BattleEventModeController.kt \
        imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/response/BattleEventModeResponse.kt
# + test
git commit -m "feat(core-admin): add PUT /event-modes/{id}/player-counts endpoint"
```

---

## Phase 9 — AGENTS.md 컨벤션

### Task 14: `imstargg-backend/AGENTS.md` 에 KDoc 컨벤션 추가

**Files:**
- Modify: `imstargg-backend/AGENTS.md`

- [ ] **Step 1: "Code Organization" 섹션에 항목 추가**

[AGENTS.md](imstargg-backend/AGENTS.md) 의 `### Code Organization` 항목 마지막에 다음 줄 추가:

```markdown
- **도메인 히스토리는 KDoc 으로 보존**: 엔티티/도메인 클래스가 갖는 비자명한 집계 규칙·정합성 제약·"왜 이렇게 안 했는지" 같은 결정은 클래스/메서드 KDoc 으로 남긴다. 향후 유사 변경을 시도하는 사람이 코드만 읽고도 의도를 알 수 있어야 한다. (예: `BrawlerBattleRankStatsEntity` 가 자기 팀만 집계하는 이유)
```

- [ ] **Step 2: 커밋**

```bash
git add imstargg-backend/AGENTS.md
git commit -m "docs: require KDoc for non-obvious domain history/constraints"
```

---

## Phase 10 — 전체 빌드 검증

### Task 15: 최종 빌드 + 테스트 + 린트

- [ ] **Step 1: 린트 자동 수정**

```bash
./gradlew ktlintFormat
```

차이가 있으면 변경 사항 검토 후 커밋:

```bash
git add -A
git commit -m "style: apply ktlintFormat"
```

(변경 없으면 skip)

- [ ] **Step 2: 전체 빌드 + 테스트**

```bash
./gradlew build
```

Expected: BUILD SUCCESSFUL. 실패 시 실패 로그를 확인해 root cause 파악 후 수정.

- [ ] **Step 3: 컨텍스트 테스트까지 포함한 통합 확인**

```bash
./gradlew contextTest
```

Expected: 통과. 통계 관련 테스트(특히 `*StatisticsService*`, `BattleEventModeController*`)가 모두 통과.

- [ ] **Step 4: 잔여 호출부 정리 확인**

```bash
grep -rn "findGroupedByBrawler\b\|\.findBrawlerResultCounts(\|countEnemyResultTotalBattleCount\|countPairResultTotalBattleCount" \
    imstargg-backend --include="*.kt"
```

Expected: 출력 없음 (제거 대상이 모두 사라진 상태). 남아있으면 정리.

- [ ] **Step 5: 디자인 문서의 "미해결 항목" 반영 여부 확인**

[docs/superpowers/specs/2026-05-19-pick-rate-correction-design.md](docs/superpowers/specs/2026-05-19-pick-rate-correction-design.md) 의 § 미해결 항목 1번(Item의 totalBattleCount 정의) 은 본 plan 에서 "전체 분모와 동일" 로 통일 — Task 8 코드에 반영됨.

§ 미해결 항목 2번(mode lookup 위치) 은 Task 7 에서 `BattleEventService.findModesByEventIds` 로 결정. 캐시는 내부 `battleEventRepositoryWithCache.find` 가 처리.

- [ ] **Step 6: 변경 요약을 PR 본문에 사용할 수 있도록 git log 확인**

```bash
git log --oneline main..HEAD
```

확인 후 추가 정리 없으면 종료.

---

## Self-Review

**1. 스펙 커버리지 점검**

| 스펙 섹션 | 매핑된 Task |
|---|---|
| §1 `BattleEventMode.playerCounts` 도메인/엔티티 | Task 2 (엔티티), Task 3 (도메인) |
| §1.3 jsonb 형식 | Task 2 |
| §2 마이그레이션 + schema.sql | Task 1 |
| §3 Admin PATCH | Task 13 |
| §4.2 event 보존 Repository | Task 5, 6 |
| §4.3 `SlotsPerGame` 헬퍼 | Task 4 |
| §4.4 Service mode lookup 합류 | Task 7, 9, 10 |
| §4.5 도메인 `create()` 시그니처 변경 | Task 8 |
| §4.6 Pair / Enemy 동일 적용 | Task 10 |
| §5 Response 필드 의미 정정 (이름 유지) | Task 10 |
| §6 KDoc 통계 엔티티 / 모드 / StatsRate | Task 2, 11, 12 |
| §7 AGENTS.md 컨벤션 | Task 14 |
| § 캐시 무효화 불필요 | Task 13 (별도 invalidate 없음 확인) |
| § Rank 통계 무변경 | Task 12 (KDoc만 추가 — 행동 변경 없음) |

**2. Placeholder 스캔**: 본 plan 의 모든 코드는 실제 구현 가능한 완성 코드. "..." 가 등장한 곳은 Task 9 의 Service 메서드(다음 Task 에서 정리) 한 군데로, 다음 Task 에서 명시적으로 갱신됨.

**3. 타입 / 시그니처 일관성**: 
- `BrawlerEventResultCount` — Task 6 정의, Task 8 사용 ✓
- `findBrawlerEventResultCounts` — Task 6 (app repo), Task 9 (cache wrapper), Task 9 (service) 일관 ✓
- `findEventTotalSlotSums` — Task 5 (JPA), Task 6 (app repo), Task 10 (cache wrapper + service) 일관 ✓
- `SlotsPerGame.forResult/forPair/forEnemy` — Task 4 정의, Task 9 / Task 10 사용 ✓
- `BattleEventModeEntity.changePlayerCounts` — Task 2 정의, Task 13 사용 ✓
- `BattleEventService.findModesByEventIds` — Task 7 정의, Task 9 / Task 10 사용 ✓
