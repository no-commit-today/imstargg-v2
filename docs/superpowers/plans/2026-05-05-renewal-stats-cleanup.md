# Renewal · Player Statistics Tab · Stats Cleanup — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Spec [`docs/superpowers/specs/2026-05-05-renewal-stats-cleanup-design.md`] 의 6개 토픽 (renewal toast 제거 + renewedAt 캡션, club 트로피 분포 버킷/순서, 플레이어 통계 탭, 시즌 목록 API, 별 아이콘 통일, stats_rank starPlayer 컬럼 정리) 을 구현한다.

**Architecture:** 6개 phase 로 나누어 진행. Phase A(§6) / Phase B(§4) / Phase C(§5) / Phase D(§1) / Phase E(§2) 는 서로 독립이고, Phase F(§3) 만 Phase B 의 시즌 API/hook 에 의존. 각 phase 는 독립 PR 로 배포 가능.

**Tech Stack:** Backend Spring Boot 4 + Kotlin / JPA / Flyway. Frontend Next.js App Router + TanStack Query + Vitest + Testing Library + shadcn/ui + lucide-react.

---

## Phase ordering & dependencies

```text
A (§6 stats_rank cleanup)   ─┐
B (§4 Season API)           ─┼─ 독립 PR — 병렬 가능
C (§5 별 아이콘)             ─┤
D (§1 Renewal)              ─┤
E (§2 Club 분포)             ─┘
F (§3 Player 통계 탭)        ─── B 머지 후 시작
```

---

## Phase A — `stats_rank_battle.star_player_brawlstars_tag` 제거 (§6)

영향 파일:

- Modify: `imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/battle/BaseStatsBattleEntity.kt`
- Modify: `imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/battle/StatsResultBattleEntity.kt`
- Modify: `imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/battle/StatsRankBattleEntity.kt`
- Modify: `imstargg-backend/infrastructure/db-core/src/main/resources/schema.sql`
- Create: `imstargg-backend/infrastructure/db-core/src/main/resources/db/migration/V260505__drop_stats_rank_star_player.sql`
- Modify: `imstargg-backend/infrastructure/db-core/src/testFixtures/kotlin/com/imstargg/infrastructure/db/core/test/fixture/BattleEntityFixture.kt`
- Modify: `imstargg-backend/core/core-worker/src/main/kotlin/com/imstargg/core/worker/application/StatsBattleAggregateApplier.kt`
- Modify: `imstargg-backend/core/core-worker/src/test/kotlin/com/imstargg/core/worker/application/StatsBattleAggregateApplierTest.kt`
- Modify: `imstargg-backend/core/core-batch/src/test/kotlin/com/imstargg/core/batch/domain/BrawlerPairBattleRankStatsAggregatorTest.kt`

(BattleEntity 의 `star_player_brawlstars_tag` 는 별도 — `BattleEntity` 는 raw 배틀 저장이라 RANK 도 starPlayer 보유 가능. 이 정리는 stats_rank 측만.)

### Task A1: BaseStatsBattleEntity 에서 `starPlayerBrawlStarsTag` 제거

**Files:**

- Modify: `imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/battle/BaseStatsBattleEntity.kt`

- [ ] **Step 1: 생성자 파라미터 + `@Column` 제거**

```kotlin
@MappedSuperclass
abstract class BaseStatsBattleEntity(
    @Column(name = "battle_key", updatable = false, nullable = false)
    val battleKey: String,
    @Column(name = "battle_at", updatable = false, nullable = false)
    val battleAt: OffsetDateTime,
    @Column(name = "event_brawlstars_id", updatable = false, nullable = false)
    val eventBrawlStarsId: Long,
    @Column(name = "event_mode_brawlstars_id", updatable = false, nullable = false)
    val eventModeBrawlStarsId: Long,
    @Column(name = "event_map", length = 65, updatable = false, nullable = false)
    val eventMap: String,
    @Column(name = "mode", length = 65, updatable = false, nullable = false)
    val mode: String,
    @Column(name = "type", length = 65, updatable = false, nullable = false)
    val type: String,
    @Column(name = "player_brawlstars_tag", length = 45, updatable = false, nullable = false)
    val playerBrawlStarsTag: String,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "teams", columnDefinition = "jsonb", updatable = false, nullable = false)
    val teams: List<List<StatsBattleEntityTeamPlayer>>,
) : BaseTimeEntity() {
    // 기존 init 블록과 메서드는 그대로
}
```

(`starPlayerBrawlStarsTag: String?` 줄과 `@Column(name = "star_player_brawlstars_tag", ...)` 어노테이션 같이 삭제.)

- [ ] **Step 2: 컴파일 확인 (이 시점엔 자식 클래스 두 개에서 컴파일 에러 발생)**

```bash
./gradlew :infrastructure:db-core:compileKotlin
```

Expected: `StatsResultBattleEntity.kt`, `StatsRankBattleEntity.kt` 의 super 호출에서 "no parameter starPlayerBrawlStarsTag" 류 컴파일 에러. 다음 task 에서 해결.

### Task A2: StatsResultBattleEntity 가 자체 컬럼으로 흡수

**Files:**

- Modify: `imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/battle/StatsResultBattleEntity.kt`

- [ ] **Step 1: 생성자 파라미터에 `starPlayerBrawlStarsTag` 를 자체 `val` 로 + super 호출에서 제거**

```kotlin
@Entity
@Table(
    name = "stats_result_battle",
    indexes = [
        Index(
            name = "uk_stats_result_battle__battle_date_battle_key",
            columnList = "battle_date, battle_key",
            unique = true,
        ),
        Index(
            name = "ix_stats_result_battle__battle_date_event",
            columnList = "battle_date, event_brawlstars_id",
        ),
    ],
)
class StatsResultBattleEntity(
    battleKey: String,
    battleAt: OffsetDateTime,
    eventBrawlStarsId: Long,
    eventModeBrawlStarsId: Long,
    eventMap: String,
    mode: String,
    type: String,
    playerBrawlStarsTag: String,
    teams: List<List<StatsBattleEntityTeamPlayer>>,
    @Column(name = "star_player_brawlstars_tag", length = 45, updatable = false)
    val starPlayerBrawlStarsTag: String?,
    @Column(name = "result", length = 25, updatable = false, nullable = false)
    val result: String,
) : BaseStatsBattleEntity(
        battleKey = battleKey,
        battleAt = battleAt,
        eventBrawlStarsId = eventBrawlStarsId,
        eventModeBrawlStarsId = eventModeBrawlStarsId,
        eventMap = eventMap,
        mode = mode,
        type = type,
        playerBrawlStarsTag = playerBrawlStarsTag,
        teams = teams,
    ) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stats_result_battle_id", nullable = false)
    val id: Long? = null
}
```

(`starPlayerBrawlStarsTag` 인자가 super 가 아니라 자체 `val` 이 됨. position 은 result 앞으로 정렬 — `result` 는 그대로 마지막.)

### Task A3: StatsRankBattleEntity 에서 `starPlayerBrawlStarsTag` 인자 제거

**Files:**

- Modify: `imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/battle/StatsRankBattleEntity.kt`

- [ ] **Step 1: 생성자 인자 + super 호출에서 `starPlayerBrawlStarsTag` 제거**

```kotlin
@Entity
@Table(
    name = "stats_rank_battle",
    indexes = [
        Index(
            name = "uk_stats_rank_battle__battle_date_battle_key",
            columnList = "battle_date, battle_key",
            unique = true,
        ),
        Index(
            name = "ix_stats_rank_battle__battle_date_event",
            columnList = "battle_date, event_brawlstars_id",
        ),
    ],
)
class StatsRankBattleEntity(
    battleKey: String,
    battleAt: OffsetDateTime,
    eventBrawlStarsId: Long,
    eventModeBrawlStarsId: Long,
    eventMap: String,
    mode: String,
    type: String,
    playerBrawlStarsTag: String,
    teams: List<List<StatsBattleEntityTeamPlayer>>,
    @Column(name = "battle_rank", updatable = false, nullable = false)
    val battleRank: Int,
) : BaseStatsBattleEntity(
        battleKey = battleKey,
        battleAt = battleAt,
        eventBrawlStarsId = eventBrawlStarsId,
        eventModeBrawlStarsId = eventModeBrawlStarsId,
        eventMap = eventMap,
        mode = mode,
        type = type,
        playerBrawlStarsTag = playerBrawlStarsTag,
        teams = teams,
    ) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stats_rank_battle_id", nullable = false)
    val id: Long? = null
}
```

- [ ] **Step 2: db-core 컴파일 통과 확인**

```bash
./gradlew :infrastructure:db-core:compileKotlin
```

Expected: 0 errors.

### Task A4: BattleEntityFixture 정리

**Files:**

- Modify: `imstargg-backend/infrastructure/db-core/src/testFixtures/kotlin/com/imstargg/infrastructure/db/core/test/fixture/BattleEntityFixture.kt`

- [ ] **Step 1: 현재 fixture 확인 후 어떤 entity 빌드용인지 결정**

```bash
grep -n "Entity\|build\(" /Users/chodyoungrae/Projects/imstargg-v2/imstargg-backend/infrastructure/db-core/src/testFixtures/kotlin/com/imstargg/infrastructure/db/core/test/fixture/BattleEntityFixture.kt
```

- [ ] **Step 2: fixture 가 `BattleEntity` (raw 배틀) 빌드용이면 `starPlayerBrawlStarsTag` 유지** — `BattleEntity.star_player_brawlstars_tag` 컬럼은 이번 정리 대상이 아님 (raw 배틀은 RANK 모드도 starPlayer 가질 수 있음)

만약 fixture 가 `StatsRankBattleEntity` 도 빌드한다면 그쪽 `build()` 의 인자에서 `starPlayerBrawlStarsTag` 제거. `BattleEntityFixture` 가 단일 entity (raw `BattleEntity`) 만 빌드한다면 변경 없음.

- [ ] **Step 3: testFixtures 컴파일 확인**

```bash
./gradlew :infrastructure:db-core:compileTestFixturesKotlin
```

Expected: 0 errors.

### Task A5: StatsBattleAggregateApplier 의 `toStatsRankBattle` 인자 제거

**Files:**

- Modify: `imstargg-backend/core/core-worker/src/main/kotlin/com/imstargg/core/worker/application/StatsBattleAggregateApplier.kt:88-101`

- [ ] **Step 1: `toStatsRankBattle` 함수에서 `starPlayerBrawlStarsTag` 인자 제거**

```kotlin
private fun toStatsRankBattle(battle: Battle) =
    StatsRankBattleEntity(
        battleKey = battle.battleKey,
        battleAt = battle.battleAt,
        eventBrawlStarsId = battle.event!!.brawlStarsId!!,
        eventModeBrawlStarsId = battle.event.modeBrawlStarsId!!.toLong(),
        eventMap = battle.event.map!!,
        mode = battle.mode!!,
        type = battle.type!!,
        playerBrawlStarsTag = battle.playerBrawlStarsTag,
        battleRank = battle.battleRank!!,
        teams = convertTeams(battle),
    )
```

`toStatsResultBattle` 의 `starPlayerBrawlStarsTag = battle.starPlayerBrawlStarsTag` 인자는 그대로 유지. (StatsResultBattleEntity 가 여전히 받음.)

- [ ] **Step 2: core-worker 컴파일 확인**

```bash
./gradlew :core:core-worker:compileKotlin
```

Expected: 0 errors.

### Task A6: 영향 받는 테스트 fixture 인자 정리

**Files:**

- Modify: `imstargg-backend/core/core-worker/src/test/kotlin/com/imstargg/core/worker/application/StatsBattleAggregateApplierTest.kt`
- Modify: `imstargg-backend/core/core-batch/src/test/kotlin/com/imstargg/core/batch/domain/BrawlerPairBattleRankStatsAggregatorTest.kt`

- [ ] **Step 1: `StatsBattleAggregateApplierTest.kt:92,117` 의 `StatsRankBattleEntity` 생성에서 `starPlayerBrawlStarsTag = null` 라인 삭제**

`grep -n "starPlayerBrawlStarsTag" .../StatsBattleAggregateApplierTest.kt` 으로 두 위치 확인 후 RANK 측 (battleRank 가 함께 있는 쪽) 만 삭제. RESULT 측 (`result =` 가 함께 있는 쪽) 은 유지.

- [ ] **Step 2: `BrawlerPairBattleRankStatsAggregatorTest.kt:96` 에서 `StatsRankBattleEntity` 생성 시 `starPlayerBrawlStarsTag = null` 라인 삭제**

- [ ] **Step 3: core-worker / core-batch unitTest 통과 확인**

```bash
./gradlew :core:core-worker:unitTest :core:core-batch:unitTest
```

Expected: 모든 테스트 PASS.

### Task A7: schema.sql 컬럼 라인 삭제

**Files:**

- Modify: `imstargg-backend/infrastructure/db-core/src/main/resources/schema.sql:287`

- [ ] **Step 1: `stats_rank_battle` 정의에서 `star_player_brawlstars_tag varchar(45)   null,` 라인 삭제**

```sql
-- before
    type                       varchar(65)   not null,
    star_player_brawlstars_tag varchar(45)   null,
    player_brawlstars_tag      varchar(45)   not null,

-- after
    type                       varchar(65)   not null,
    player_brawlstars_tag      varchar(45)   not null,
```

`stats_result_battle` 정의 (line 315 부근) 의 `star_player_brawlstars_tag` 는 유지.

### Task A8: 마이그레이션 파일 생성

**Files:**

- Create: `imstargg-backend/infrastructure/db-core/src/main/resources/db/migration/V260505__drop_stats_rank_star_player.sql`

- [ ] **Step 1: 마이그레이션 SQL 작성 (idempotent)**

```sql
alter table stats_rank_battle drop column if exists star_player_brawlstars_tag;
```

### Task A9: Phase A 검증 + 커밋

- [ ] **Step 1: 전체 backend 빌드 + 테스트**

```bash
./gradlew :infrastructure:db-core:ktlintCheck :core:core-worker:ktlintCheck :core:core-batch:ktlintCheck
./gradlew :infrastructure:db-core:unitTest :core:core-worker:unitTest :core:core-batch:unitTest
./gradlew build -x developTest
```

Expected: 모두 PASS.

- [ ] **Step 2: 커밋**

```bash
git add imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/battle/BaseStatsBattleEntity.kt \
        imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/battle/StatsResultBattleEntity.kt \
        imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/battle/StatsRankBattleEntity.kt \
        imstargg-backend/infrastructure/db-core/src/main/resources/schema.sql \
        imstargg-backend/infrastructure/db-core/src/main/resources/db/migration/V260505__drop_stats_rank_star_player.sql \
        imstargg-backend/infrastructure/db-core/src/testFixtures/kotlin/com/imstargg/infrastructure/db/core/test/fixture/BattleEntityFixture.kt \
        imstargg-backend/core/core-worker/src/main/kotlin/com/imstargg/core/worker/application/StatsBattleAggregateApplier.kt \
        imstargg-backend/core/core-worker/src/test/kotlin/com/imstargg/core/worker/application/StatsBattleAggregateApplierTest.kt \
        imstargg-backend/core/core-batch/src/test/kotlin/com/imstargg/core/batch/domain/BrawlerPairBattleRankStatsAggregatorTest.kt
git commit -m "$(cat <<'EOF'
refactor(stats): stats_rank_battle 의 star_player_brawlstars_tag 제거

RANK 배틀(쇼다운 등 순위형)에는 star player 개념이 없으므로
BaseStatsBattleEntity 의 공통 컬럼에서 제거하고
StatsResultBattleEntity 자체 컬럼으로 이동.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Phase B — 시즌 목록 API (§4)

영향 파일 (백엔드):

- Modify: `imstargg-backend/core/core-enum/src/main/kotlin/com/imstargg/core/enums/SoloRankSeason.kt`
- Modify: `imstargg-backend/core/core-enum/src/main/kotlin/com/imstargg/core/enums/BrawlPassSeason.kt`
- Create: `imstargg-backend/core/core-enum/src/test/kotlin/com/imstargg/core/enums/SoloRankSeasonAllTest.kt` (또는 기존 `SoloRankSeasonTest.kt` 에 `@Nested All` 추가)
- Create: `imstargg-backend/core/core-enum/src/test/kotlin/com/imstargg/core/enums/BrawlPassSeasonAllTest.kt` (또는 기존 추가)
- Create: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/api/controller/v1/response/BrawlPassSeasonResponse.kt`
- Create: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/api/controller/v1/response/SoloRankSeasonResponse.kt`
- Create: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/api/controller/v1/SeasonController.kt`
- Create: `imstargg-backend/core/core-api/src/test/kotlin/com/imstargg/core/api/controller/v1/SeasonControllerTest.kt`

영향 파일 (프론트):

- Modify: `imstargg-frontend/src/api/types.ts` (응답 타입 추가)
- Create: `imstargg-frontend/src/api/endpoints/season.ts`
- Modify: `imstargg-frontend/src/hooks/queries/queryKeys.ts`
- Create: `imstargg-frontend/src/hooks/queries/useSeasons.ts`
- Create: `imstargg-frontend/tests/unit/api/season.test.ts` (옵션 — 단순 endpoint 면 skip 가능)

### Task B1: SoloRankSeason — `number/month` public 화 + `all(clock)` 메서드

**Files:**

- Modify: `imstargg-backend/core/core-enum/src/main/kotlin/com/imstargg/core/enums/SoloRankSeason.kt`
- Modify: `imstargg-backend/core/core-enum/src/test/kotlin/com/imstargg/core/enums/SoloRankSeasonTest.kt` (새 nested class 추가)

- [ ] **Step 1: 실패 테스트 작성**

`SoloRankSeasonTest.kt` 에 새 `@Nested inner class All` 추가:

```kotlin
@Nested
inner class All {
    @Test
    fun `첫 시즌 기간 중에는 첫 시즌 1개만 반환한다`() {
        // given
        val clock = fixedClock("2025-03-15T12:00:00+09:00")

        // when
        val result = SoloRankSeason.all(clock)

        // then
        assertThat(result).hasSize(1)
        assertThat(result[0].key).isEqualTo("1-1")
    }

    @Test
    fun `시즌 2의 1개월차에는 1-1, 1-2, 1-3, 1-4, 2-1 5개를 순서대로 반환한다`() {
        // given
        val clock = fixedClock("2025-08-01T12:00:00+09:00")

        // when
        val result = SoloRankSeason.all(clock)

        // then
        assertThat(result.map { it.key }).containsExactly("1-1", "1-2", "1-3", "1-4", "2-1")
    }
}
```

또한 `number/month` 가시성 테스트는 컴파일 자체로 검증되므로 별도 테스트 불필요.

- [ ] **Step 2: 테스트 실패 확인**

```bash
./gradlew :core:core-enum:test --tests "com.imstargg.core.enums.SoloRankSeasonTest"
```

Expected: All 클래스의 두 테스트 FAIL — `SoloRankSeason.all` 미정의.

- [ ] **Step 3: SoloRankSeason 수정 — 가시성 + `all` companion 함수**

```kotlin
class SoloRankSeason private constructor(
    val number: Int,
    val month: Int,
    val startAt: OffsetDateTime,
    val endAt: OffsetDateTime,
) {
    val key: String = "$number-$month"

    // ... fun next(), isLast() 그대로 ...

    companion object {
        private const val SEASON_MAX_MONTH = 4

        // ... createFirst(), of(), current(), ofKey() 그대로 ...

        fun all(clock: Clock): List<SoloRankSeason> {
            val end = current(clock)
            val list = mutableListOf<SoloRankSeason>()
            var s = createFirst()
            while (s.key != end.key) {
                list += s
                s = s.next()
            }
            list += end
            return list
        }
    }
}
```

(변경: `private val number/month` → `val number/month` public.)

- [ ] **Step 4: 테스트 통과 확인**

```bash
./gradlew :core:core-enum:test --tests "com.imstargg.core.enums.SoloRankSeasonTest"
```

Expected: 모든 테스트 PASS.

### Task B2: BrawlPassSeason — `all(clock)` 메서드

**Files:**

- Modify: `imstargg-backend/core/core-enum/src/main/kotlin/com/imstargg/core/enums/BrawlPassSeason.kt`
- Modify: `imstargg-backend/core/core-enum/src/test/kotlin/com/imstargg/core/enums/BrawlPassSeasonTest.kt`

- [ ] **Step 1: 실패 테스트 작성**

```kotlin
@Nested
inner class All {
    @Test
    fun `현재 시즌이 5번이면 1~5번 5개를 순서대로 반환한다`() {
        // given - createFirst 의 endAt 으로부터 약 4 * 30일 = ~4개월 후라면 5번 정도
        // BrawlPass 의 next() 가 약 1개월 간격이므로 첫 시즌 endAt(2022-05-05) + 4개월 = 2022-09-05 시점에 5번째
        val clock = fixedClock("2022-09-10T12:00:00+09:00")

        // when
        val result = BrawlPassSeason.all(clock)

        // then
        assertThat(result.map { it.number }).containsExactly(1, 2, 3, 4, 5)
    }
}
```

(`current` 의 동작 — `endAt 이후` 에 다음 시즌으로 넘어감. 정확한 시즌 수는 코드의 `next()` 동작에 달려있으므로 작성 후 실행하여 실제 값 확인 후 테스트 데이터 보정 필요. 가급적 `current(clock).number` 와 `all(clock).size` 가 일치한다는 invariant 검증으로 작성:)

```kotlin
@Test
fun `반환 리스트의 크기는 current 시즌 number 와 같다`() {
    // given
    val clock = fixedClock("2026-05-01T00:00:00+09:00")

    // when
    val all = BrawlPassSeason.all(clock)
    val current = BrawlPassSeason.current(clock)

    // then
    assertThat(all).hasSize(current.number)
    assertThat(all.last().number).isEqualTo(current.number)
    assertThat(all.first().number).isEqualTo(1)
}
```

- [ ] **Step 2: 테스트 실패 확인**

```bash
./gradlew :core:core-enum:test --tests "com.imstargg.core.enums.BrawlPassSeasonTest"
```

Expected: FAIL — `BrawlPassSeason.all` 미정의.

- [ ] **Step 3: `BrawlPassSeason.all` 추가**

```kotlin
companion object {
    // ... 기존 createFirst(), current(), of(), ofNumber() 그대로 ...

    fun all(clock: Clock): List<BrawlPassSeason> {
        val end = current(clock)
        val list = mutableListOf<BrawlPassSeason>()
        var s = createFirst()
        while (s.number != end.number) {
            list += s
            s = s.next()
        }
        list += end
        return list
    }
}
```

- [ ] **Step 4: 테스트 통과 확인**

```bash
./gradlew :core:core-enum:test --tests "com.imstargg.core.enums.BrawlPassSeasonTest"
```

Expected: PASS.

### Task B3: Response DTO 작성

**Files:**

- Create: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/api/controller/v1/response/BrawlPassSeasonResponse.kt`
- Create: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/api/controller/v1/response/SoloRankSeasonResponse.kt`

- [ ] **Step 1: `BrawlPassSeasonResponse.kt`**

```kotlin
package com.imstargg.core.api.controller.v1.response

import com.imstargg.core.enums.BrawlPassSeason
import java.time.OffsetDateTime

data class BrawlPassSeasonResponse(
    val number: Int,
    val startAt: OffsetDateTime,
    val endAt: OffsetDateTime,
) {
    companion object {
        fun of(season: BrawlPassSeason) = BrawlPassSeasonResponse(
            number = season.number,
            startAt = season.startAt,
            endAt = season.endAt,
        )
    }
}
```

- [ ] **Step 2: `SoloRankSeasonResponse.kt`**

```kotlin
package com.imstargg.core.api.controller.v1.response

import com.imstargg.core.enums.SoloRankSeason
import java.time.OffsetDateTime

data class SoloRankSeasonResponse(
    val key: String,
    val number: Int,
    val month: Int,
    val startAt: OffsetDateTime,
    val endAt: OffsetDateTime,
) {
    companion object {
        fun of(season: SoloRankSeason) = SoloRankSeasonResponse(
            key = season.key,
            number = season.number,
            month = season.month,
            startAt = season.startAt,
            endAt = season.endAt,
        )
    }
}
```

- [ ] **Step 3: 컴파일 확인**

```bash
./gradlew :core:core-api:compileKotlin
```

Expected: 0 errors.

### Task B4: SeasonController 작성 + WebMvcTest

**Files:**

- Create: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/api/controller/v1/SeasonController.kt`
- Create: `imstargg-backend/core/core-api/src/test/kotlin/com/imstargg/core/api/controller/v1/SeasonControllerTest.kt`

- [ ] **Step 1: 실패 테스트 작성 (`SeasonControllerTest.kt`)**

```kotlin
package com.imstargg.core.api.controller.v1

import io.mockk.every
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import com.ninjasquad.springmockk.MockkBean
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@Tag("context")
@WebMvcTest(SeasonController::class)
class SeasonControllerTest(
    @Autowired private val mockMvc: MockMvc,
) {
    @MockkBean(relaxed = true)
    private lateinit var clock: Clock

    @Test
    fun `브롤패스 시즌 목록을 반환한다`() {
        every { clock.instant() } returns Instant.parse("2026-05-05T00:00:00Z")
        every { clock.zone } returns ZoneOffset.UTC

        mockMvc
            .get("/v1/seasons/brawl-pass")
            .andExpect {
                status { isOk() }
                jsonPath("$.content") { isArray() }
                jsonPath("$.content[0].number") { value(1) }
                jsonPath("$.content[0].startAt") { value("2022-04-07T18:00:00+09:00") }
            }
    }

    @Test
    fun `솔로랭크 시즌 목록을 반환한다`() {
        every { clock.instant() } returns Instant.parse("2025-04-01T00:00:00Z")
        every { clock.zone } returns ZoneOffset.UTC

        mockMvc
            .get("/v1/seasons/solo-rank")
            .andExpect {
                status { isOk() }
                jsonPath("$.content[0].key") { value("1-1") }
                jsonPath("$.content[0].number") { value(1) }
                jsonPath("$.content[0].month") { value(1) }
            }
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

```bash
./gradlew :core:core-api:test --tests "com.imstargg.core.api.controller.v1.SeasonControllerTest"
```

Expected: 컴파일 에러 — `SeasonController` 미존재.

- [ ] **Step 3: SeasonController 작성**

```kotlin
package com.imstargg.core.api.controller.v1

import com.imstargg.core.api.controller.v1.response.BrawlPassSeasonResponse
import com.imstargg.core.api.controller.v1.response.SoloRankSeasonResponse
import com.imstargg.core.api.controller.v1.response.ListResponse
import com.imstargg.core.enums.BrawlPassSeason
import com.imstargg.core.enums.SoloRankSeason
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Clock

@RestController
class SeasonController(
    private val clock: Clock,
) {
    @GetMapping("/v1/seasons/brawl-pass")
    fun brawlPass(): ResponseEntity<ListResponse<BrawlPassSeasonResponse>> =
        ResponseEntity.ok(
            ListResponse(BrawlPassSeason.all(clock).map(BrawlPassSeasonResponse::of)),
        )

    @GetMapping("/v1/seasons/solo-rank")
    fun soloRank(): ResponseEntity<ListResponse<SoloRankSeasonResponse>> =
        ResponseEntity.ok(
            ListResponse(SoloRankSeason.all(clock).map(SoloRankSeasonResponse::of)),
        )
}
```

(`ListResponse<T>` 의 정확한 위치는 `grep -rn "data class ListResponse" core-api/src/main` 으로 확인 후 import. 기존 `BrawlerController` 등에서 import 경로 동일하게 사용.)

- [ ] **Step 4: 테스트 통과 확인**

```bash
./gradlew :core:core-api:test --tests "com.imstargg.core.api.controller.v1.SeasonControllerTest"
```

Expected: PASS.

### Task B5: Phase B 백엔드 검증 + 커밋

- [ ] **Step 1: ktlint + 전체 unitTest + contextTest**

```bash
./gradlew :core:core-enum:ktlintCheck :core:core-api:ktlintCheck
./gradlew :core:core-enum:unitTest :core:core-api:unitTest :core:core-api:contextTest
```

Expected: 모두 PASS.

- [ ] **Step 2: 커밋**

```bash
git add imstargg-backend/core/core-enum/src/main/kotlin/com/imstargg/core/enums/SoloRankSeason.kt \
        imstargg-backend/core/core-enum/src/main/kotlin/com/imstargg/core/enums/BrawlPassSeason.kt \
        imstargg-backend/core/core-enum/src/test/kotlin/com/imstargg/core/enums/SoloRankSeasonTest.kt \
        imstargg-backend/core/core-enum/src/test/kotlin/com/imstargg/core/enums/BrawlPassSeasonTest.kt \
        imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/api/controller/v1/response/BrawlPassSeasonResponse.kt \
        imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/api/controller/v1/response/SoloRankSeasonResponse.kt \
        imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/api/controller/v1/SeasonController.kt \
        imstargg-backend/core/core-api/src/test/kotlin/com/imstargg/core/api/controller/v1/SeasonControllerTest.kt
git commit -m "$(cat <<'EOF'
feat(season): 시즌 목록 API (BrawlPass / SoloRank)

GET /v1/seasons/brawl-pass
GET /v1/seasons/solo-rank

각 enum 에 all(clock) 추가, SoloRankSeason.number/month public 화.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

### Task B6: 프론트엔드 — 시즌 응답 타입 + endpoint + hook

**Files:**

- Modify: `imstargg-frontend/src/api/types.ts`
- Create: `imstargg-frontend/src/api/endpoints/season.ts`
- Modify: `imstargg-frontend/src/hooks/queries/queryKeys.ts`
- Create: `imstargg-frontend/src/hooks/queries/useSeasons.ts`

- [ ] **Step 1: `src/api/types.ts` — 응답 타입 추가 (`Statistics` 섹션 아래에)**

```ts
// === Season ===
export interface BrawlPassSeasonResponse {
  number: number;
  startAt: string;
  endAt: string;
}

export interface SoloRankSeasonResponse {
  key: string;
  number: number;
  month: number;
  startAt: string;
  endAt: string;
}
```

- [ ] **Step 2: `src/api/endpoints/season.ts` 작성**

```ts
import { apiFetch, type FetchOpts } from "@/api/client";
import type {
  BrawlPassSeasonResponse,
  ListResponse,
  SoloRankSeasonResponse,
} from "@/api/types";

export const seasonApi = {
  brawlPass: (opts?: FetchOpts) =>
    apiFetch<ListResponse<BrawlPassSeasonResponse>>("/v1/seasons/brawl-pass", opts),
  soloRank: (opts?: FetchOpts) =>
    apiFetch<ListResponse<SoloRankSeasonResponse>>("/v1/seasons/solo-rank", opts),
};
```

- [ ] **Step 3: `src/hooks/queries/queryKeys.ts` — season 키 추가**

`statistics` 객체 아래에 추가:

```ts
season: {
  all: ["season"] as const,
  brawlPass: () => [...queryKeys.season.all, "brawl-pass"] as const,
  soloRank: () => [...queryKeys.season.all, "solo-rank"] as const,
},
```

- [ ] **Step 4: `src/hooks/queries/useSeasons.ts` 작성**

```ts
"use client";

import { useQuery } from "@tanstack/react-query";
import { seasonApi } from "@/api/endpoints/season";
import { queryKeys } from "@/hooks/queries/queryKeys";

export function useBrawlPassSeasons() {
  return useQuery({
    queryKey: queryKeys.season.brawlPass(),
    queryFn: ({ signal }) => seasonApi.brawlPass({ signal }),
    staleTime: Infinity,
  });
}

export function useSoloRankSeasons() {
  return useQuery({
    queryKey: queryKeys.season.soloRank(),
    queryFn: ({ signal }) => seasonApi.soloRank({ signal }),
    staleTime: Infinity,
  });
}
```

- [ ] **Step 5: typecheck + lint**

```bash
cd imstargg-frontend && pnpm typecheck && pnpm lint
```

Expected: 0 errors.

- [ ] **Step 6: 커밋**

```bash
git add imstargg-frontend/src/api/types.ts \
        imstargg-frontend/src/api/endpoints/season.ts \
        imstargg-frontend/src/hooks/queries/queryKeys.ts \
        imstargg-frontend/src/hooks/queries/useSeasons.ts
git commit -m "$(cat <<'EOF'
feat(frontend): season list API client + hooks

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Phase C — 별 아이콘 통일 (§5)

영향 파일:

- Create: `imstargg-frontend/src/components/primitives/StarIcon.tsx`
- Modify: `imstargg-frontend/app/_components/statistics/SortableHeader.tsx` (label: ReactNode)
- Modify: `imstargg-frontend/app/brawlers/_components/BrawlerStatTable.tsx`
- Modify: `imstargg-frontend/app/events/[id]/_components/EventResultStatsTable.tsx`
- Modify: `imstargg-frontend/app/brawlers/[id]/_components/DailyTrendChart.tsx`
- Modify: `imstargg-frontend/app/players/[tag]/_components/battle/BattlePlayerRow.tsx`
- Modify: `imstargg-frontend/app/events/[id]/_components/EventStatsSection.tsx`

### Task C1: `StarIcon` primitive 생성

**Files:**

- Create: `imstargg-frontend/src/components/primitives/StarIcon.tsx`
- Create: `imstargg-frontend/tests/unit/components/primitives/StarIcon.test.tsx`

- [ ] **Step 1: 실패 테스트 작성**

```tsx
import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import StarIcon from "@/components/primitives/StarIcon";

describe("StarIcon", () => {
  it("기본 aria-label '스타플레이어' 가 노출된다", () => {
    render(<StarIcon />);
    expect(screen.getByLabelText("스타플레이어")).toBeInTheDocument();
  });

  it("label prop 로 커스텀 라벨을 지정한다", () => {
    render(<StarIcon label="MVP" />);
    expect(screen.getByLabelText("MVP")).toBeInTheDocument();
  });
});
```

- [ ] **Step 2: 테스트 실패 확인**

```bash
cd imstargg-frontend && pnpm test:run tests/unit/components/primitives/StarIcon.test.tsx
```

Expected: FAIL — `StarIcon` 미정의.

- [ ] **Step 3: StarIcon 구현**

```tsx
import { Star } from "lucide-react";

interface Props {
  size?: number;
  label?: string;
  className?: string;
}

export default function StarIcon({ size = 14, label = "스타플레이어", className }: Props) {
  return (
    <Star
      size={size}
      fill="#FFC60A"
      stroke="#FFC60A"
      aria-label={label}
      className={className}
    />
  );
}
```

- [ ] **Step 4: 테스트 통과 확인**

```bash
pnpm test:run tests/unit/components/primitives/StarIcon.test.tsx
```

Expected: PASS.

### Task C2: `SortableHeader` label prop 시그니처 확장

**Files:**

- Modify: `imstargg-frontend/app/_components/statistics/SortableHeader.tsx`

- [ ] **Step 1: 현재 시그니처 확인**

```bash
cat /Users/chodyoungrae/Projects/imstargg-v2/imstargg-frontend/app/_components/statistics/SortableHeader.tsx
```

`label: string` → `label: React.ReactNode` 로 변경. 또한 옵션 prop `title?: string` 추가하여 hover tooltip 지원 (`<button title={...}>`).

- [ ] **Step 2: 시그니처 수정**

```tsx
import type { ReactNode } from "react";
// ... 기존 imports

interface Props {
  column: string;
  label: ReactNode;
  title?: string;
  currentSort: string;
  currentOrder: SortOrder;
}

export function SortableHeader({ column, label, title, currentSort, currentOrder }: Props) {
  // ... 내부 button 에 title={title} 속성 추가, 기타 동일
}
```

- [ ] **Step 3: typecheck**

```bash
pnpm typecheck
```

Expected: 0 errors. (label 을 받는 호출처들이 string 만 사용 중이므로 ReactNode 호환.)

### Task C3: 헤더 라벨/배틀 행 별 아이콘 교체

**Files:**

- Modify: `imstargg-frontend/app/brawlers/_components/BrawlerStatTable.tsx:67`
- Modify: `imstargg-frontend/app/events/[id]/_components/EventResultStatsTable.tsx:45`
- Modify: `imstargg-frontend/app/players/[tag]/_components/battle/BattlePlayerRow.tsx:54`
- Modify: `imstargg-frontend/app/events/[id]/_components/EventStatsSection.tsx`
- Modify: `imstargg-frontend/app/brawlers/[id]/_components/DailyTrendChart.tsx`

- [ ] **Step 1: BrawlerStatTable**

```tsx
import StarIcon from "@/components/primitives/StarIcon";
// ...
<SortableHeader column="starPlayerRate" label={<StarIcon />} title="스타플레이어 비율" currentSort={sort} currentOrder={order} />
```

- [ ] **Step 2: EventResultStatsTable**

```tsx
import StarIcon from "@/components/primitives/StarIcon";
// ...
<SortableHeader column="starPlayerRate" label={<StarIcon />} title="스타플레이어 비율" currentSort={sort} currentOrder={order} />
```

- [ ] **Step 3: BattlePlayerRow — Unicode `★` → StarIcon**

```tsx
import StarIcon from "@/components/primitives/StarIcon";
// ...
{player.name}
{isStar && <StarIcon size={12} className="ml-1" />}
```

(Unicode `★` 와 `<span style={{ marginLeft: 4, color: "#FFC60A" }}>★</span>` 제거.)

- [ ] **Step 4: EventStatsSection — "MVP" 등 텍스트 라벨 사용처 교체**

```bash
grep -n "MVP\|스타플레이어\|스타률" /Users/chodyoungrae/Projects/imstargg-v2/imstargg-frontend/app/events/\[id\]/_components/EventStatsSection.tsx
```

해당 텍스트가 헤더/툴팁에서 표시되는 경우 `<StarIcon />` 으로 교체. 만약 정렬 옵션 string 키 ("starPlayerRate") 자체는 유지 — 라벨만.

- [ ] **Step 5: DailyTrendChart — chart legend / tooltip**

```tsx
// chartConfig 의 label 은 string 이어야 할 수 있음 — Recharts 의 ChartConfig 타입 확인 후
// label: "스타플레이어" (짧은 한글) 로 두고, 툴팁 formatter 만 별 아이콘 표시:
const chartConfig: ChartConfig = {
  winRate: { label: "승률", color: "#FFC60A" },
  starPlayerRate: { label: "스타플레이어", color: "#9146FF" },
};
// ...
<Tooltip formatter={(v, name) => [`${v}%`, name === "winRate" ? "승률" : <StarIcon size={12} />]} />
```

(만약 ChartConfig.label 이 ReactNode 받는다면 직접 `<StarIcon/>` 할당. typecheck 로 검증.)

- [ ] **Step 6: typecheck + lint**

```bash
pnpm typecheck && pnpm lint
```

Expected: 0 errors.

- [ ] **Step 7: 기존 테스트 통과 확인 (Stat 관련 컴포넌트 테스트)**

```bash
pnpm test:run tests/unit/components/statistics/
```

Expected: 모두 PASS (이 변경은 라벨만 — 동작 변화 없음).

### Task C4: Phase C 커밋

- [ ] **Step 1: 커밋**

```bash
git add imstargg-frontend/src/components/primitives/StarIcon.tsx \
        imstargg-frontend/tests/unit/components/primitives/StarIcon.test.tsx \
        imstargg-frontend/app/_components/statistics/SortableHeader.tsx \
        imstargg-frontend/app/brawlers/_components/BrawlerStatTable.tsx \
        imstargg-frontend/app/events/\[id\]/_components/EventResultStatsTable.tsx \
        imstargg-frontend/app/events/\[id\]/_components/EventStatsSection.tsx \
        imstargg-frontend/app/brawlers/\[id\]/_components/DailyTrendChart.tsx \
        imstargg-frontend/app/players/\[tag\]/_components/battle/BattlePlayerRow.tsx
git commit -m "$(cat <<'EOF'
feat(frontend): MVP/스타률/스타플레이어 → 별 아이콘 통일 (StarIcon primitive)

- StarIcon (lucide Star + brand yellow) 신규 primitive
- SortableHeader.label 시그니처 string → ReactNode + title 옵션
- BattlePlayerRow 의 Unicode ★ 도 StarIcon 으로 통일

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Phase D — Renewal (§1)

영향 파일:

- Modify: `imstargg-frontend/src/hooks/usePlayerRenewal.ts`
- Modify: `imstargg-frontend/src/hooks/useClubRenewal.ts`
- Create: `imstargg-frontend/src/components/shared/RenewalStatus.tsx`
- Create: `imstargg-frontend/tests/unit/components/shared/RenewalStatus.test.tsx`
- Modify: `imstargg-frontend/app/players/[tag]/_components/PlayerHero.tsx`
- Modify: `imstargg-frontend/app/clubs/[tag]/_components/ClubHeader.tsx`
- Modify: `imstargg-frontend/tests/integration/player/playerRenewal.test.tsx`
- Modify: `imstargg-frontend/tests/integration/club/clubRenewal.test.tsx`

### Task D1: hook 들에서 toast 제거

**Files:**

- Modify: `imstargg-frontend/src/hooks/usePlayerRenewal.ts`
- Modify: `imstargg-frontend/src/hooks/useClubRenewal.ts`

- [ ] **Step 1: `usePlayerRenewal.ts` 수정 — toast import + 3개 호출 제거 + renewedAt expose**

```ts
"use client";

import { useEffect, useRef } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { renewalApi } from "@/api/endpoints/renewal";
import { NotFoundError } from "@/api/errors";
import { emptyRenewal } from "@/domain/renewal/empty";
import { queryKeys } from "@/hooks/queries/queryKeys";

export function usePlayerRenewal(tag: string) {
  const qc = useQueryClient();

  const renewalQuery = useQuery({
    queryKey: queryKeys.player.renewal(tag),
    queryFn: async ({ signal }) => {
      try {
        return await renewalApi.getPlayer(tag, { signal });
      } catch (e) {
        if (e instanceof NotFoundError) return emptyRenewal(tag);
        throw e;
      }
    },
    refetchInterval: (q) => (q.state.data?.progressing ? 1000 : false),
    refetchIntervalInBackground: false,
    staleTime: 0,
  });

  const prevProgressing = useRef<boolean>(
    renewalQuery.data?.progressing ?? false,
  );
  useEffect(() => {
    const cur = renewalQuery.data?.progressing ?? false;
    if (prevProgressing.current && !cur) {
      qc.invalidateQueries({ queryKey: queryKeys.player.all });
    }
    prevProgressing.current = cur;
  }, [renewalQuery.data?.progressing, qc]);

  const trigger = useMutation({
    mutationFn: () => renewalApi.renewPlayer(tag),
    onSuccess: (data) => {
      qc.setQueryData(queryKeys.player.renewal(tag), data);
    },
  });

  return {
    renewal: renewalQuery.data,
    progressing: renewalQuery.data?.progressing ?? false,
    canRenew: renewalQuery.data?.canRenew ?? false,
    renewedAt: renewalQuery.data?.renewedAt ?? null,
    trigger: () => trigger.mutate(),
    triggering: trigger.isPending,
  };
}
```

- [ ] **Step 2: `useClubRenewal.ts` 동일 수정**

```ts
"use client";

import { useEffect, useRef } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { renewalApi } from "@/api/endpoints/renewal";
import { NotFoundError } from "@/api/errors";
import { emptyRenewal } from "@/domain/renewal/empty";
import { queryKeys } from "@/hooks/queries/queryKeys";

export function useClubRenewal(tag: string) {
  const qc = useQueryClient();

  const renewalQuery = useQuery({
    queryKey: queryKeys.club.renewal(tag),
    queryFn: async ({ signal }) => {
      try {
        return await renewalApi.getClub(tag, { signal });
      } catch (e) {
        if (e instanceof NotFoundError) return emptyRenewal(tag);
        throw e;
      }
    },
    refetchInterval: (q) => (q.state.data?.progressing ? 1000 : false),
    refetchIntervalInBackground: false,
    staleTime: 0,
  });

  const prevProgressing = useRef<boolean>(
    renewalQuery.data?.progressing ?? false,
  );
  useEffect(() => {
    const cur = renewalQuery.data?.progressing ?? false;
    if (prevProgressing.current && !cur) {
      qc.invalidateQueries({ queryKey: queryKeys.club.all });
    }
    prevProgressing.current = cur;
  }, [renewalQuery.data?.progressing, qc]);

  const trigger = useMutation({
    mutationFn: () => renewalApi.renewClub(tag),
    onSuccess: (data) => {
      qc.setQueryData(queryKeys.club.renewal(tag), data);
    },
  });

  return {
    renewal: renewalQuery.data,
    progressing: renewalQuery.data?.progressing ?? false,
    canRenew: renewalQuery.data?.canRenew ?? false,
    renewedAt: renewalQuery.data?.renewedAt ?? null,
    trigger: () => trigger.mutate(),
    triggering: trigger.isPending,
  };
}
```

- [ ] **Step 3: typecheck**

```bash
pnpm typecheck
```

Expected: 0 errors.

### Task D2: 기존 renewal 통합 테스트 — toast assertion 제거

**Files:**

- Modify: `imstargg-frontend/tests/integration/player/playerRenewal.test.tsx`
- Modify: `imstargg-frontend/tests/integration/club/clubRenewal.test.tsx`

- [ ] **Step 1: playerRenewal.test.tsx — toast assertion 두 개 제거 / 대체**

`screen.findByText(/갱신 요청됨/)` 와 `screen.findByText(/갱신 완료/)` 제거. 그 자리에 "버튼이 disabled 로 전환됐다가 다시 enabled 로 돌아온다" 검증으로 대체:

```tsx
it("갱신 클릭 → 버튼이 progressing 동안 비활성화되고 폴링 후 다시 활성화된다", async () => {
  renderWithProviders(<RenewalButton kind="player" tag={TAG} />);

  const button = await screen.findByRole("button", { name: /갱신/ });
  await waitFor(() => expect(button).not.toBeDisabled());
  await userEvent.click(button);

  await waitFor(() => expect(button).toBeDisabled());
  await waitFor(() => expect(button).not.toBeDisabled(), { timeout: 5000 });
});
```

두 번째 describe (`첫 갱신`) 테스트도 동일 패턴으로 수정. 핵심: toast text 검증 → button disabled 상태 전환 검증.

- [ ] **Step 2: clubRenewal.test.tsx — 동일 패턴 적용**

- [ ] **Step 3: 테스트 실행 — 통과 확인**

```bash
pnpm test:run tests/integration/player/playerRenewal.test.tsx tests/integration/club/clubRenewal.test.tsx
```

Expected: PASS.

### Task D3: `RenewalStatus` 컴포넌트 생성

**Files:**

- Create: `imstargg-frontend/src/components/shared/RenewalStatus.tsx`
- Create: `imstargg-frontend/tests/unit/components/shared/RenewalStatus.test.tsx`

- [ ] **Step 1: 실패 테스트 작성**

```tsx
import { describe, it, expect } from "vitest";
import { http, HttpResponse } from "msw";
import { server } from "../../../setup/msw";
import { renderWithProviders } from "../../../setup/renderWithProviders";
import { screen } from "@testing-library/react";
import { RenewalStatus } from "@/components/shared/RenewalStatus";

const TAG = "#88J8Q9RPV";
const ENC = "%2388J8Q9RPV";

describe("RenewalStatus (player)", () => {
  it("renewedAt 이 있으면 '최근 업데이트: ~전' 텍스트를 보여준다", async () => {
    server.use(
      http.get(`*/v1/players/${ENC}/renewal`, () =>
        HttpResponse.json({
          tag: TAG,
          status: "COMPLETED",
          progressing: false,
          canRenew: true,
          requestedAt: "2026-04-27T00:00:00Z",
          executedAt: null,
          completedAt: "2026-04-27T00:00:01Z",
          renewedAt: "2026-04-27T00:00:01Z",
        }),
      ),
    );
    renderWithProviders(<RenewalStatus kind="player" tag={TAG} />);
    expect(await screen.findByText(/최근 업데이트:/)).toBeInTheDocument();
  });

  it("renewedAt 이 null 이면 '최근 업데이트 없음' 표시", async () => {
    server.use(
      http.get(`*/v1/players/${ENC}/renewal`, () => new HttpResponse(null, { status: 404 })),
    );
    renderWithProviders(<RenewalStatus kind="player" tag={TAG} />);
    expect(await screen.findByText(/최근 업데이트 없음/)).toBeInTheDocument();
  });

  it("progressing 일 때 '진행 중…' 표시", async () => {
    server.use(
      http.get(`*/v1/players/${ENC}/renewal`, () =>
        HttpResponse.json({
          tag: TAG,
          status: "EXECUTING",
          progressing: true,
          canRenew: false,
          requestedAt: "2026-04-27T00:00:00Z",
          executedAt: null,
          completedAt: null,
          renewedAt: null,
        }),
      ),
    );
    renderWithProviders(<RenewalStatus kind="player" tag={TAG} />);
    expect(await screen.findByText(/진행 중…/)).toBeInTheDocument();
  });
});
```

- [ ] **Step 2: 테스트 실패 확인**

```bash
pnpm test:run tests/unit/components/shared/RenewalStatus.test.tsx
```

Expected: FAIL — `RenewalStatus` 미정의.

- [ ] **Step 3: `RenewalStatus.tsx` 작성**

```tsx
"use client";

import { usePlayerRenewal } from "@/hooks/usePlayerRenewal";
import { useClubRenewal } from "@/hooks/useClubRenewal";
import { formatRelativeTime } from "@/lib/relativeTime";

type Props =
  | { kind: "player"; tag: string }
  | { kind: "club"; tag: string };

export function RenewalStatus(props: Props) {
  if (props.kind === "player") return <PlayerRenewalStatus tag={props.tag} />;
  return <ClubRenewalStatus tag={props.tag} />;
}

function PlayerRenewalStatus({ tag }: { tag: string }) {
  const { renewedAt, progressing } = usePlayerRenewal(tag);
  return <StatusText progressing={progressing} renewedAt={renewedAt} />;
}

function ClubRenewalStatus({ tag }: { tag: string }) {
  const { renewedAt, progressing } = useClubRenewal(tag);
  return <StatusText progressing={progressing} renewedAt={renewedAt} />;
}

function StatusText({ progressing, renewedAt }: { progressing: boolean; renewedAt: string | null }) {
  const text = (() => {
    if (progressing) return "최근 업데이트: 진행 중…";
    if (!renewedAt) return "최근 업데이트 없음";
    return `최근 업데이트: ${formatRelativeTime(renewedAt)}`;
  })();
  return <span className="text-sm text-[#5C6271]">{text}</span>;
}
```

- [ ] **Step 4: 테스트 통과 확인**

```bash
pnpm test:run tests/unit/components/shared/RenewalStatus.test.tsx
```

Expected: 3개 테스트 PASS.

### Task D4: PlayerHero / ClubHeader 에 RenewalStatus 배치

**Files:**

- Modify: `imstargg-frontend/app/players/[tag]/_components/PlayerHero.tsx:50`
- Modify: `imstargg-frontend/app/clubs/[tag]/_components/ClubHeader.tsx:60`

- [ ] **Step 1: PlayerHero — `<RenewalButton/>` 옆에 `<RenewalStatus/>` 추가**

```tsx
import { RenewalButton } from "@/components/shared/RenewalButton";
import { RenewalStatus } from "@/components/shared/RenewalStatus";
// ...
<header className="flex items-center gap-4 sm:gap-6">
  {/* avatar, name, etc. */}
  <div className="flex flex-col items-end gap-1 sm:flex-row sm:items-center sm:gap-3">
    <RenewalStatus kind="player" tag={player.tag} />
    <RenewalButton kind="player" tag={player.tag} />
  </div>
</header>
```

- [ ] **Step 2: ClubHeader — 우측 컬럼 안에 `<RenewalStatus/>` 추가**

```tsx
import { RenewalStatus } from "@/components/shared/RenewalStatus";
// ...
<div className="flex flex-col justify-center gap-3 border-t border-white/5 p-6 lg:border-l lg:border-t-0">
  <div className="rounded bg-white/5 p-3 text-[11px] leading-relaxed text-white/70">
    {/* 기존 클럽 타입 박스 */}
  </div>
  <div className="flex flex-col items-end gap-1 sm:flex-row sm:items-center sm:gap-3">
    <RenewalStatus kind="club" tag={tag} />
    <RenewalButton kind="club" tag={tag} />
  </div>
</div>
```

(Hero 가 다크 배경이라 `text-[#5C6271]` 가독성 부족 시 `RenewalStatus` 의 색상은 그대로 두고 ClubHeader 측 wrapper 에서 `[&>span]:text-white/70` 같은 처리 또는 별도 prop — plan 단계 단순화: ClubHeader 사용 시 `<span className="text-white/70 text-sm">` wrapper 로 감싸고 RenewalStatus 의 자체 색상은 light theme 기본을 유지. 구현 시 디자인 검수.)

- [ ] **Step 3: typecheck**

```bash
pnpm typecheck
```

Expected: 0 errors.

### Task D5: Phase D 검증 + 커밋

- [ ] **Step 1: 전체 frontend 테스트**

```bash
pnpm test:run
```

Expected: 모든 테스트 PASS.

- [ ] **Step 2: 커밋**

```bash
git add imstargg-frontend/src/hooks/usePlayerRenewal.ts \
        imstargg-frontend/src/hooks/useClubRenewal.ts \
        imstargg-frontend/src/components/shared/RenewalStatus.tsx \
        imstargg-frontend/tests/unit/components/shared/RenewalStatus.test.tsx \
        imstargg-frontend/tests/integration/player/playerRenewal.test.tsx \
        imstargg-frontend/tests/integration/club/clubRenewal.test.tsx \
        imstargg-frontend/app/players/\[tag\]/_components/PlayerHero.tsx \
        imstargg-frontend/app/clubs/\[tag\]/_components/ClubHeader.tsx
git commit -m "$(cat <<'EOF'
feat(frontend): renewal toast 제거 + RenewalStatus 캡션 추가

usePlayerRenewal/useClubRenewal 의 모든 toast 제거.
RenewalStatus 컴포넌트가 renewedAt 기반 '최근 업데이트: n시간 전' 노출.
PlayerHero / ClubHeader 에 배치.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Phase E — Club 트로피 분포 (§2)

영향 파일:

- Modify: `imstargg-frontend/src/domain/club/members.ts`
- Modify: `imstargg-frontend/tests/unit/domain/club/members.test.ts` (있다면)
- Modify: `imstargg-frontend/app/clubs/[tag]/_components/ClubMembers.tsx` (className prop)
- Modify: `imstargg-frontend/app/clubs/[tag]/_components/ClubTrophyDistribution.tsx` (className prop)
- Modify: `imstargg-frontend/app/clubs/[tag]/_components/ClubContent.tsx`

### Task E1: BUCKETS 재정의 + 테스트

**Files:**

- Modify: `imstargg-frontend/src/domain/club/members.ts:27`
- Modify (or Create): `imstargg-frontend/tests/unit/domain/club/members.test.ts`

- [ ] **Step 1: 기존 테스트 확인**

```bash
ls /Users/chodyoungrae/Projects/imstargg-v2/imstargg-frontend/tests/unit/domain/club/
```

기존 테스트가 90k/80k 등 옛 라벨을 검증한다면 새 라벨로 갱신.

- [ ] **Step 2: 실패 테스트 작성 (또는 갱신)**

```ts
import { describe, it, expect } from "vitest";
import { trophyDistribution } from "@/domain/club/members";

describe("trophyDistribution", () => {
  it("11개 버킷 (200K+ ... <20K) 을 생성한다", () => {
    const result = trophyDistribution([]);
    expect(result.map((b) => b.label)).toEqual([
      "200K+",
      "180–200K",
      "160–180K",
      "140–160K",
      "120–140K",
      "100–120K",
      "80–100K",
      "60–80K",
      "40–60K",
      "20–40K",
      "<20K",
    ]);
  });

  it("멤버 트로피를 올바른 버킷에 분류한다", () => {
    const members = [
      { trophies: 250000 } as any,
      { trophies: 195000 } as any,
      { trophies: 35000 } as any,
      { trophies: 5000 } as any,
    ];
    const result = trophyDistribution(members);
    const counts = Object.fromEntries(result.map((b) => [b.label, b.count]));
    expect(counts["200K+"]).toBe(1);
    expect(counts["180–200K"]).toBe(1);
    expect(counts["20–40K"]).toBe(1);
    expect(counts["<20K"]).toBe(1);
  });
});
```

- [ ] **Step 3: 테스트 실패 확인**

```bash
pnpm test:run tests/unit/domain/club/members.test.ts
```

Expected: FAIL — 옛 BUCKETS.

- [ ] **Step 4: BUCKETS 재정의**

`src/domain/club/members.ts:27` 의 `BUCKETS` 배열을 다음으로 교체:

```ts
const BUCKETS: ReadonlyArray<Omit<TrophyBucket, "count">> = [
  { label: "200K+",    min: 200000, max: null   },
  { label: "180–200K", min: 180000, max: 200000 },
  { label: "160–180K", min: 160000, max: 180000 },
  { label: "140–160K", min: 140000, max: 160000 },
  { label: "120–140K", min: 120000, max: 140000 },
  { label: "100–120K", min: 100000, max: 120000 },
  { label: "80–100K",  min: 80000,  max: 100000 },
  { label: "60–80K",   min: 60000,  max: 80000  },
  { label: "40–60K",   min: 40000,  max: 60000  },
  { label: "20–40K",   min: 20000,  max: 40000  },
  { label: "<20K",     min: 0,      max: 20000  },
];
```

- [ ] **Step 5: 테스트 통과 확인**

```bash
pnpm test:run tests/unit/domain/club/members.test.ts
```

Expected: PASS.

### Task E2: ClubMembers / ClubTrophyDistribution className prop 확장

**Files:**

- Modify: `imstargg-frontend/app/clubs/[tag]/_components/ClubMembers.tsx`
- Modify: `imstargg-frontend/app/clubs/[tag]/_components/ClubTrophyDistribution.tsx`

- [ ] **Step 1: ClubMembers — className prop 추가**

```tsx
export function ClubMembers({ members, className }: { members: ClubMember[]; className?: string }) {
  // ... 기존 내부
  // root element 의 className 에 `${className ?? ""}` 합성
  return (
    <Card className={`${rootCls} ${className ?? ""}`}>
      {/* ... */}
    </Card>
  );
}
```

(현재 root 가 어떤 element 인지 확인 후 정확히 합성. `cn()` 헬퍼가 있으면 활용 — `import { cn } from "@/lib/cn"`.)

- [ ] **Step 2: ClubTrophyDistribution — className prop 추가**

```tsx
export function ClubTrophyDistribution({ members, className }: { members: ClubMember[]; className?: string }) {
  return (
    <section className={`card ${className ?? ""}`}>
      {/* ... */}
    </section>
  );
}
```

- [ ] **Step 3: typecheck**

```bash
pnpm typecheck
```

Expected: 0 errors.

### Task E3: ClubContent 레이아웃 순서 변경

**Files:**

- Modify: `imstargg-frontend/app/clubs/[tag]/_components/ClubContent.tsx:37`

- [ ] **Step 1: 현재 grid 영역 순서 변경**

```tsx
<div className="grid gap-4 lg:grid-cols-[1fr_320px]">
  <ClubTrophyDistribution members={club.members} className="lg:order-2" />
  <ClubMembers members={sorted} className="lg:order-1" />
</div>
```

(DOM 은 Distribution → Members → 모바일 stack 시 Distribution 이 위에 노출. 데스크탑은 `lg:order-*` 로 시각 순서를 Members(좌측 1fr) → Distribution(우측 320px) 으로 보정.)

- [ ] **Step 2: typecheck**

```bash
pnpm typecheck
```

Expected: 0 errors.

### Task E4: Phase E 검증 + 커밋

- [ ] **Step 1: 전체 테스트**

```bash
pnpm test:run
```

Expected: 모든 테스트 PASS.

- [ ] **Step 2: 커밋**

```bash
git add imstargg-frontend/src/domain/club/members.ts \
        imstargg-frontend/tests/unit/domain/club/members.test.ts \
        imstargg-frontend/app/clubs/\[tag\]/_components/ClubMembers.tsx \
        imstargg-frontend/app/clubs/\[tag\]/_components/ClubTrophyDistribution.tsx \
        imstargg-frontend/app/clubs/\[tag\]/_components/ClubContent.tsx
git commit -m "$(cat <<'EOF'
feat(frontend): club 트로피 분포 — 11개 버킷(200K+ … <20K) + 모바일 stack 시 분포 위에 노출

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Phase F — 플레이어 상세 통계 탭 (§3)

> Phase B 가 머지된 후 시작.

영향 파일:

- Modify: `imstargg-frontend/src/api/types.ts` (PlayerBrawlerStatistics 응답 타입)
- Create: `imstargg-frontend/src/api/endpoints/playerStatistics.ts`
- Create: `imstargg-frontend/src/domain/statistics/player.ts`
- Modify: `imstargg-frontend/src/hooks/queries/queryKeys.ts`
- Create: `imstargg-frontend/src/hooks/queries/usePlayerStatistics.ts`
- Create: `imstargg-frontend/app/players/[tag]/_components/statistics/PlayerStatistics.tsx`
- Create: `imstargg-frontend/app/players/[tag]/_components/statistics/PlayerStatisticsControls.tsx`
- Create: `imstargg-frontend/app/players/[tag]/_components/statistics/PlayerBrawlerRoleSummary.tsx`
- Create: `imstargg-frontend/app/players/[tag]/_components/statistics/PlayerBrawlerStatsList.tsx`
- Modify: `imstargg-frontend/app/players/[tag]/_components/PlayerContent.tsx`

### Task F1: API 응답 타입 추가

**Files:**

- Modify: `imstargg-frontend/src/api/types.ts`

- [ ] **Step 1: `Statistics` 섹션에 추가** (백엔드 `LocalDate` 는 ISO `yyyy-MM-dd` string 으로 직렬화됨)

```ts
// === Player Statistics ===
export interface PlayerBrawlerStatisticsItemResponse {
  brawler: BrawlerResponse | null;
  totalBattleCount: number;
  victoryCount: number;
  defeatCount: number;
  drawCount: number;
  winRate: number;
}

export interface PlayerBrawlerStatisticsResponse {
  startBattleDate: string; // yyyy-MM-dd
  endBattleDate: string;
  totalBattleCount: number;
  items: PlayerBrawlerStatisticsItemResponse[];
}

export interface PlayerBrawlerRoleStatisticsItemResponse {
  role: BrawlerRole;
  totalBattleCount: number;
  victoryCount: number;
  defeatCount: number;
  drawCount: number;
  winRate: number;
}

export interface PlayerBrawlerRoleStatisticsResponse {
  startBattleDate: string;
  endBattleDate: string;
  totalBattleCount: number;
  items: PlayerBrawlerRoleStatisticsItemResponse[];
}
```

- [ ] **Step 2: typecheck**

```bash
pnpm typecheck
```

Expected: 0 errors.

### Task F2: 도메인 타입 + 매퍼

**Files:**

- Create: `imstargg-frontend/src/domain/statistics/player.ts`
- Create: `imstargg-frontend/tests/unit/domain/statistics/player.test.ts`

- [ ] **Step 1: 실패 테스트 작성**

```ts
import { describe, it, expect } from "vitest";
import { toPlayerBrawlerStat, toPlayerBrawlerRoleStat } from "@/domain/statistics/player";

describe("toPlayerBrawlerStat", () => {
  it("brawler 정보와 결과 카운트를 매핑한다", () => {
    const dto = {
      brawler: { id: 1, name: { ko: "샐리", en: "Shelly" }, rarity: "STARTING_BRAWLER", role: "DAMAGE_DEALER", portraitImageUrl: null },
      totalBattleCount: 100,
      victoryCount: 60,
      defeatCount: 35,
      drawCount: 5,
      winRate: 0.6,
    };
    const result = toPlayerBrawlerStat(dto as any);
    expect(result.brawler?.id).toBe(1);
    expect(result.brawler?.name).toBe("샐리");
    expect(result.totalBattleCount).toBe(100);
    expect(result.winRate).toBe(0.6);
  });

  it("brawler 가 null 이면 brawler 도 null", () => {
    const dto = {
      brawler: null,
      totalBattleCount: 10, victoryCount: 5, defeatCount: 5, drawCount: 0, winRate: 0.5,
    };
    const result = toPlayerBrawlerStat(dto as any);
    expect(result.brawler).toBeNull();
  });
});
```

- [ ] **Step 2: 테스트 실패 확인**

```bash
pnpm test:run tests/unit/domain/statistics/player.test.ts
```

Expected: FAIL — `toPlayerBrawlerStat` 미정의.

- [ ] **Step 3: `src/domain/statistics/player.ts` 작성**

```ts
import type {
  PlayerBrawlerStatisticsItemResponse,
  PlayerBrawlerRoleStatisticsItemResponse,
} from "@/api/types";
import type { Brawler } from "@/domain/brawler/types";
import { toBrawler } from "@/domain/brawler/mappers";
import type { BrawlerRole } from "@/api/types";

export interface PlayerBrawlerStat {
  brawler: Brawler | null;
  totalBattleCount: number;
  victoryCount: number;
  defeatCount: number;
  drawCount: number;
  winRate: number;
}

export interface PlayerBrawlerRoleStat {
  role: BrawlerRole;
  totalBattleCount: number;
  victoryCount: number;
  defeatCount: number;
  drawCount: number;
  winRate: number;
}

export function toPlayerBrawlerStat(dto: PlayerBrawlerStatisticsItemResponse): PlayerBrawlerStat {
  return {
    brawler: dto.brawler ? toBrawler(dto.brawler) : null,
    totalBattleCount: dto.totalBattleCount,
    victoryCount: dto.victoryCount,
    defeatCount: dto.defeatCount,
    drawCount: dto.drawCount,
    winRate: dto.winRate,
  };
}

export function toPlayerBrawlerRoleStat(dto: PlayerBrawlerRoleStatisticsItemResponse): PlayerBrawlerRoleStat {
  return {
    role: dto.role,
    totalBattleCount: dto.totalBattleCount,
    victoryCount: dto.victoryCount,
    defeatCount: dto.defeatCount,
    drawCount: dto.drawCount,
    winRate: dto.winRate,
  };
}
```

(`toBrawler` 의 정확한 시그니처는 `imstargg-frontend/src/domain/brawler/mappers.ts` 의 export 확인 후 사용.)

- [ ] **Step 4: 테스트 통과 확인**

```bash
pnpm test:run tests/unit/domain/statistics/player.test.ts
```

Expected: PASS.

### Task F3: API endpoint 작성

**Files:**

- Create: `imstargg-frontend/src/api/endpoints/playerStatistics.ts`

- [ ] **Step 1: endpoint 작성**

```ts
import { apiFetch, type FetchOpts } from "@/api/client";
import { toUrlPath } from "@/lib/tag";
import type {
  PlayerBrawlerRoleStatisticsResponse,
  PlayerBrawlerStatisticsResponse,
} from "@/api/types";

export const playerStatisticsApi = {
  rankedBrawler: (tag: string, seasonNumber: number, opts?: FetchOpts) =>
    apiFetch<PlayerBrawlerStatisticsResponse>(
      `/v1/statistics/players/${toUrlPath(tag)}/brawler/ranked?seasonNumber=${seasonNumber}`,
      opts,
    ),
  soloRankedBrawler: (tag: string, seasonKey: string, opts?: FetchOpts) =>
    apiFetch<PlayerBrawlerStatisticsResponse>(
      `/v1/statistics/players/${toUrlPath(tag)}/brawler/solo-ranked?seasonKey=${encodeURIComponent(seasonKey)}`,
      opts,
    ),
  rankedRole: (tag: string, seasonNumber: number, opts?: FetchOpts) =>
    apiFetch<PlayerBrawlerRoleStatisticsResponse>(
      `/v1/statistics/players/${toUrlPath(tag)}/brawler-role/ranked?seasonNumber=${seasonNumber}`,
      opts,
    ),
  soloRankedRole: (tag: string, seasonKey: string, opts?: FetchOpts) =>
    apiFetch<PlayerBrawlerRoleStatisticsResponse>(
      `/v1/statistics/players/${toUrlPath(tag)}/brawler-role/solo-ranked?seasonKey=${encodeURIComponent(seasonKey)}`,
      opts,
    ),
};
```

- [ ] **Step 2: typecheck**

```bash
pnpm typecheck
```

Expected: 0 errors.

### Task F4: queryKeys 에 player statistics 키 추가

**Files:**

- Modify: `imstargg-frontend/src/hooks/queries/queryKeys.ts`

- [ ] **Step 1: `player` 객체 안에 추가**

```ts
player: {
  // ... 기존
  statistics: {
    brawler: (tag: string, battleType: string, season: string) =>
      [...queryKeys.player.all, "statistics-brawler", normalizeTag(tag), battleType, season] as const,
    role: (tag: string, battleType: string, season: string) =>
      [...queryKeys.player.all, "statistics-role", normalizeTag(tag), battleType, season] as const,
  },
},
```

(`season` 인자는 `seasonNumber.toString()` 또는 `seasonKey` 그대로.)

### Task F5: `usePlayerStatistics` hooks

**Files:**

- Create: `imstargg-frontend/src/hooks/queries/usePlayerStatistics.ts`

- [ ] **Step 1: hook 작성**

```ts
"use client";

import { useQuery } from "@tanstack/react-query";
import { playerStatisticsApi } from "@/api/endpoints/playerStatistics";
import { queryKeys } from "@/hooks/queries/queryKeys";

export type PlayerStatBattleType = "RANKED" | "SOLO_RANKED";

interface BrawlerStatsParams {
  tag: string;
  battleType: PlayerStatBattleType;
  brawlPassSeasonNumber: number | null;
  soloRankSeasonKey: string | null;
}

export function usePlayerBrawlerStatistics({ tag, battleType, brawlPassSeasonNumber, soloRankSeasonKey }: BrawlerStatsParams) {
  const seasonId = battleType === "RANKED"
    ? brawlPassSeasonNumber?.toString() ?? ""
    : soloRankSeasonKey ?? "";
  const enabled = seasonId !== "";
  return useQuery({
    queryKey: queryKeys.player.statistics.brawler(tag, battleType, seasonId),
    queryFn: ({ signal }) =>
      battleType === "RANKED"
        ? playerStatisticsApi.rankedBrawler(tag, brawlPassSeasonNumber!, { signal })
        : playerStatisticsApi.soloRankedBrawler(tag, soloRankSeasonKey!, { signal }),
    enabled,
  });
}

export function usePlayerBrawlerRoleStatistics({ tag, battleType, brawlPassSeasonNumber, soloRankSeasonKey }: BrawlerStatsParams) {
  const seasonId = battleType === "RANKED"
    ? brawlPassSeasonNumber?.toString() ?? ""
    : soloRankSeasonKey ?? "";
  const enabled = seasonId !== "";
  return useQuery({
    queryKey: queryKeys.player.statistics.role(tag, battleType, seasonId),
    queryFn: ({ signal }) =>
      battleType === "RANKED"
        ? playerStatisticsApi.rankedRole(tag, brawlPassSeasonNumber!, { signal })
        : playerStatisticsApi.soloRankedRole(tag, soloRankSeasonKey!, { signal }),
    enabled,
  });
}
```

### Task F6: `PlayerBrawlerRoleSummary` 컴포넌트

**Files:**

- Create: `imstargg-frontend/app/players/[tag]/_components/statistics/PlayerBrawlerRoleSummary.tsx`

- [ ] **Step 1: 컴포넌트 작성 — Role 7개 카드**

```tsx
import type { PlayerBrawlerRoleStat } from "@/domain/statistics/player";
import { Card } from "@/components/ui/card";

const ROLE_LABEL: Record<string, string> = {
  DAMAGE_DEALER: "데미지", TANK: "탱커", MARKSMAN: "저격수",
  ASSASSIN: "암살자", SUPPORT: "서포터", CONTROLLER: "컨트롤러", ARTILLERY: "투척",
};

export function PlayerBrawlerRoleSummary({ items }: { items: PlayerBrawlerRoleStat[] }) {
  if (items.every((i) => i.totalBattleCount === 0)) {
    return <Card className="p-4 text-center text-sm text-[#5C6271]">이 시즌 통계 데이터가 없습니다.</Card>;
  }
  return (
    <div className="grid grid-cols-2 gap-2 sm:grid-cols-4 md:grid-cols-7">
      {items.map((it) => (
        <Card key={it.role} className="p-3">
          <div className="text-xs font-medium text-[#5C6271]">{ROLE_LABEL[it.role] ?? it.role}</div>
          <div className="mt-1 text-xl font-bold tabular-nums">{(it.winRate * 100).toFixed(1)}%</div>
          <div className="text-xs text-[#8A90A0] tabular-nums">{it.totalBattleCount}전</div>
        </Card>
      ))}
    </div>
  );
}
```

### Task F7: `PlayerBrawlerStatsList` 컴포넌트

**Files:**

- Create: `imstargg-frontend/app/players/[tag]/_components/statistics/PlayerBrawlerStatsList.tsx`

- [ ] **Step 1: 컴포넌트 작성 — 표 + 정렬**

```tsx
"use client";

import { useMemo } from "react";
import { BrawlerAvatar } from "@/components/primitives/BrawlerAvatar";
import { formatNumber } from "@/lib/format";
import type { PlayerBrawlerStat } from "@/domain/statistics/player";

export function PlayerBrawlerStatsList({ items }: { items: PlayerBrawlerStat[] }) {
  const sorted = useMemo(
    () => [...items].sort((a, b) => b.winRate - a.winRate),
    [items],
  );
  if (sorted.length === 0) {
    return <div className="rounded-lg border border-[#D8DCE5] bg-white px-4 py-12 text-center text-sm text-[#5C6271]">이 시즌 통계 데이터가 없습니다.</div>;
  }
  return (
    <div className="rounded-lg border border-[#D8DCE5] bg-white">
      <div className="grid grid-cols-[40px_1fr_72px_72px_120px] items-center gap-2 border-b border-[#D8DCE5] bg-[#F6F7FA] px-3 py-2 text-[11px] font-bold uppercase tracking-wider text-[#5C6271]">
        <span>#</span>
        <span>브롤러</span>
        <span className="text-right">전투수</span>
        <span className="text-right">승률</span>
        <span className="text-right">V-D-Dr</span>
      </div>
      {sorted.map((it, i) => (
        <div
          key={it.brawler?.id ?? `unknown-${i}`}
          className={`grid grid-cols-[40px_1fr_72px_72px_120px] items-center gap-2 border-b border-[#ECEEF3] px-3 py-2 text-sm ${it.totalBattleCount === 0 ? "opacity-40" : ""}`}
        >
          <span className="font-bold text-[#5C6271] tabular-nums">{i + 1}</span>
          <div className="flex items-center gap-2 overflow-hidden">
            {it.brawler && (
              <BrawlerAvatar
                name={it.brawler.name}
                imageUrl={it.brawler.portraitImageUrl}
                rarity={it.brawler.rarity}
                size={28}
                className="shrink-0"
              />
            )}
            <span className="truncate font-medium">{it.brawler?.name ?? "-"}</span>
          </div>
          <span className="text-right tabular-nums">{formatNumber(it.totalBattleCount)}</span>
          <span className="text-right tabular-nums">{(it.winRate * 100).toFixed(1)}%</span>
          <span className="text-right tabular-nums text-[#5C6271]">{it.victoryCount}-{it.defeatCount}-{it.drawCount}</span>
        </div>
      ))}
    </div>
  );
}
```

### Task F8: `PlayerStatisticsControls` (battleType + season select)

**Files:**

- Create: `imstargg-frontend/app/players/[tag]/_components/statistics/PlayerStatisticsControls.tsx`

- [ ] **Step 1: 컴포넌트 작성**

```tsx
"use client";

import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Button } from "@/components/ui/button";
import type { BrawlPassSeasonResponse, SoloRankSeasonResponse } from "@/api/types";
import type { PlayerStatBattleType } from "@/hooks/queries/usePlayerStatistics";

interface Props {
  battleType: PlayerStatBattleType;
  onBattleTypeChange: (v: PlayerStatBattleType) => void;
  brawlPassSeasons: BrawlPassSeasonResponse[];
  soloRankSeasons: SoloRankSeasonResponse[];
  brawlPassSeasonNumber: number | null;
  soloRankSeasonKey: string | null;
  onBrawlPassChange: (n: number) => void;
  onSoloRankChange: (k: string) => void;
}

export function PlayerStatisticsControls({
  battleType,
  onBattleTypeChange,
  brawlPassSeasons,
  soloRankSeasons,
  brawlPassSeasonNumber,
  soloRankSeasonKey,
  onBrawlPassChange,
  onSoloRankChange,
}: Props) {
  return (
    <div className="flex flex-wrap items-center gap-3">
      <div className="flex rounded-md border border-[#D8DCE5] bg-white p-0.5">
        <Button
          variant={battleType === "RANKED" ? "default" : "ghost"}
          size="sm"
          onClick={() => onBattleTypeChange("RANKED")}
        >
          랭크전
        </Button>
        <Button
          variant={battleType === "SOLO_RANKED" ? "default" : "ghost"}
          size="sm"
          onClick={() => onBattleTypeChange("SOLO_RANKED")}
        >
          경쟁전
        </Button>
      </div>

      {battleType === "RANKED" ? (
        <Select
          value={brawlPassSeasonNumber?.toString() ?? ""}
          onValueChange={(v) => onBrawlPassChange(Number(v))}
        >
          <SelectTrigger className="w-40"><SelectValue placeholder="시즌 선택" /></SelectTrigger>
          <SelectContent>
            {[...brawlPassSeasons].reverse().map((s) => (
              <SelectItem key={s.number} value={s.number.toString()}>
                시즌 {s.number}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      ) : (
        <Select
          value={soloRankSeasonKey ?? ""}
          onValueChange={onSoloRankChange}
        >
          <SelectTrigger className="w-40"><SelectValue placeholder="시즌 선택" /></SelectTrigger>
          <SelectContent>
            {[...soloRankSeasons].reverse().map((s) => (
              <SelectItem key={s.key} value={s.key}>
                시즌 {s.number}-{s.month}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      )}
    </div>
  );
}
```

(시즌 select 의 옵션은 최신부터 노출 — `.reverse()` 사용. 백엔드는 첫 시즌부터 순서대로 반환.)

### Task F9: `PlayerStatistics` 컨테이너

**Files:**

- Create: `imstargg-frontend/app/players/[tag]/_components/statistics/PlayerStatistics.tsx`

- [ ] **Step 1: 컨테이너 작성**

```tsx
"use client";

import { useEffect, useState } from "react";
import { useBrawlPassSeasons, useSoloRankSeasons } from "@/hooks/queries/useSeasons";
import {
  usePlayerBrawlerStatistics,
  usePlayerBrawlerRoleStatistics,
  type PlayerStatBattleType,
} from "@/hooks/queries/usePlayerStatistics";
import { toPlayerBrawlerRoleStat, toPlayerBrawlerStat } from "@/domain/statistics/player";
import { PlayerStatisticsControls } from "./PlayerStatisticsControls";
import { PlayerBrawlerRoleSummary } from "./PlayerBrawlerRoleSummary";
import { PlayerBrawlerStatsList } from "./PlayerBrawlerStatsList";

export function PlayerStatistics({ tag }: { tag: string }) {
  const [battleType, setBattleType] = useState<PlayerStatBattleType>("RANKED");
  const [brawlPassSeasonNumber, setBrawlPassSeasonNumber] = useState<number | null>(null);
  const [soloRankSeasonKey, setSoloRankSeasonKey] = useState<string | null>(null);

  const { data: brawlPassSeasonsData } = useBrawlPassSeasons();
  const { data: soloRankSeasonsData } = useSoloRankSeasons();
  const brawlPassSeasons = brawlPassSeasonsData?.content ?? [];
  const soloRankSeasons = soloRankSeasonsData?.content ?? [];

  // 첫 로드 시 default = 가장 최신 시즌
  useEffect(() => {
    if (brawlPassSeasonNumber === null && brawlPassSeasons.length > 0) {
      setBrawlPassSeasonNumber(brawlPassSeasons[brawlPassSeasons.length - 1].number);
    }
  }, [brawlPassSeasons, brawlPassSeasonNumber]);
  useEffect(() => {
    if (soloRankSeasonKey === null && soloRankSeasons.length > 0) {
      setSoloRankSeasonKey(soloRankSeasons[soloRankSeasons.length - 1].key);
    }
  }, [soloRankSeasons, soloRankSeasonKey]);

  const brawlerQuery = usePlayerBrawlerStatistics({
    tag, battleType, brawlPassSeasonNumber, soloRankSeasonKey,
  });
  const roleQuery = usePlayerBrawlerRoleStatistics({
    tag, battleType, brawlPassSeasonNumber, soloRankSeasonKey,
  });

  const brawlerItems = (brawlerQuery.data?.items ?? []).map(toPlayerBrawlerStat);
  const roleItems = (roleQuery.data?.items ?? []).map(toPlayerBrawlerRoleStat);

  return (
    <div className="space-y-4 py-4">
      <PlayerStatisticsControls
        battleType={battleType}
        onBattleTypeChange={setBattleType}
        brawlPassSeasons={brawlPassSeasons}
        soloRankSeasons={soloRankSeasons}
        brawlPassSeasonNumber={brawlPassSeasonNumber}
        soloRankSeasonKey={soloRankSeasonKey}
        onBrawlPassChange={setBrawlPassSeasonNumber}
        onSoloRankChange={setSoloRankSeasonKey}
      />
      <PlayerBrawlerRoleSummary items={roleItems} />
      <PlayerBrawlerStatsList items={brawlerItems} />
    </div>
  );
}
```

### Task F10: `PlayerContent` 에 통계 탭 등록

**Files:**

- Modify: `imstargg-frontend/app/players/[tag]/_components/PlayerContent.tsx:43-64`

- [ ] **Step 1: TabsList + TabsContent 추가**

```tsx
import { PlayerStatistics } from "./statistics/PlayerStatistics";
// ...
<Tabs defaultValue="battles" className="w-full">
  <TabsList variant="line" className="h-11 w-full justify-start gap-4 border-b border-[#D8DCE5] px-0">
    <TabsTrigger value="battles" className="px-2 text-base font-semibold data-[state=active]:text-[#14161B]">배틀</TabsTrigger>
    <TabsTrigger value="brawlers" className="px-2 text-base font-semibold data-[state=active]:text-[#14161B]">브롤러</TabsTrigger>
    <TabsTrigger value="statistics" className="px-2 text-base font-semibold data-[state=active]:text-[#14161B]">통계</TabsTrigger>
  </TabsList>
  <TabsContent value="battles"><PlayerBattles tag={tag} /></TabsContent>
  <TabsContent value="brawlers"><PlayerBrawlers brawlers={brawlers ?? []} /></TabsContent>
  <TabsContent value="statistics"><PlayerStatistics tag={tag} /></TabsContent>
</Tabs>
```

### Task F11: Phase F 검증 + 커밋

- [ ] **Step 1: typecheck + lint + test**

```bash
pnpm typecheck && pnpm lint && pnpm test:run
```

Expected: 모두 PASS.

- [ ] **Step 2: dev 서버 띄워 통계 탭 수동 검증** (subagent execution 시 — 사람 검증 필요. CI 통과 후 PR 단계에서)

```bash
pnpm dev
# http://localhost:3000/players/%2388J8Q9RPV → 통계 탭 클릭 → 랭크전/경쟁전 토글, 시즌 변경
```

Expected: 4개 endpoint 모두 호출 정상 + Role/Brawler 영역 모두 렌더.

- [ ] **Step 3: 커밋**

```bash
git add imstargg-frontend/src/api/types.ts \
        imstargg-frontend/src/api/endpoints/playerStatistics.ts \
        imstargg-frontend/src/domain/statistics/player.ts \
        imstargg-frontend/tests/unit/domain/statistics/player.test.ts \
        imstargg-frontend/src/hooks/queries/queryKeys.ts \
        imstargg-frontend/src/hooks/queries/usePlayerStatistics.ts \
        imstargg-frontend/app/players/\[tag\]/_components/statistics/ \
        imstargg-frontend/app/players/\[tag\]/_components/PlayerContent.tsx
git commit -m "$(cat <<'EOF'
feat(frontend): 플레이어 상세 통계 탭 추가

- 랭크전(BrawlPass) / 경쟁전(SoloRank) 토글 + 시즌 select
- Role 요약 strip + Brawler 별 표 (winRate desc 기본)
- 4개 endpoint 사용 (brawler/role × ranked/solo-ranked)

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Self-review note

이 plan 은 spec 6개 토픽을 모두 커버:

- §1 Renewal → Phase D
- §2 Club 분포 → Phase E
- §3 Player 통계 탭 → Phase F (Phase B 의존)
- §4 Season API → Phase B
- §5 별 아이콘 → Phase C
- §6 stats_rank cleanup → Phase A

타입 일관성 check: `PlayerStatBattleType`, `BrawlPassSeasonResponse`, `SoloRankSeasonResponse`, `PlayerBrawlerStat`, `PlayerBrawlerRoleStat` — 모든 이름이 phase 간 일관됨.

placeholder check: 코드 블록 내 `// ... 기타 동일`, `// 기존 init 블록`, `placeholder` 같은 표현은 `BattleEntityFixture` 의 entity-범용성 확인 (A4) / `ClubMembers` root 확인 (E2) / `EventStatsSection` grep 으로 정확한 라인 찾기 (C3) / chartConfig.label 타입 확인 (C3) 처럼 plan 시점에 미정인 정확한 위치는 step 안에서 확인 명령을 함께 제시하여 실행자가 결정 가능.
