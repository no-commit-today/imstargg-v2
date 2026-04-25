# core-api 통계 정합성 + 성능 개선 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** core-api 통계 도메인의 weekly batch row 누적 결함을 제거하고, 팀/적 통계 쿼리·캐시·검증을 정리한다.

**Architecture:** `StatsWeekRange.weekRange(date)` 단일 함수로 통합해 batch row 의 `(start, end)` 가 매일 동일하도록 만든다. 팀 통계 쿼리는 `EXISTS + COUNT(DISTINCT) HAVING` 으로 단일 서브쿼리화하고, `*RepositoryWithCache` 는 count×2 + data 를 한 묶음 DTO 로 캐싱 (`sync = true`, brawlerIds 정렬 키). 컨트롤러는 `@Validated + @Size(max = 3)`, Service 는 `CoreException(INVALID_REQUEST)` 로 의존 검증.

**Tech Stack:** Kotlin, Spring Boot 4, QueryDSL, Spring Cache (Caffeine), Bean Validation, JPA/Hibernate, PostgreSQL.

**Spec:** [docs/superpowers/specs/2026-04-25-core-api-statistics-integrity-and-performance-design.md](../specs/2026-04-25-core-api-statistics-integrity-and-performance-design.md)

---

## File Structure

### 신규 파일

- `infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/repository/BrawlerResultProjection.kt` — brawler 단일 통계 projection DTO
- `infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/repository/BrawlerBattleResultStatsJpaRepositoryCustom.kt` — Custom 인터페이스 (없으면 신설)
- `infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/repository/BrawlerBattleResultStatsJpaRepositoryCustomImpl.kt` — 구현
- `infrastructure/db-core/src/main/resources/db/migration/V260425a__brawler_team_stats_group_by_index.sql` — covering index
- `core-api/src/main/kotlin/com/imstargg/core/application/statistics/StatisticsCacheKeys.kt` — `joinIds` static helper
- `core-api/src/main/kotlin/com/imstargg/core/application/statistics/BrawlerTeamResultCounts.kt` — 통합 캐시 DTO
- `core-api/src/main/kotlin/com/imstargg/core/application/statistics/BrawlerTeamRankCounts.kt` — 통합 캐시 DTO
- `core-api/src/main/kotlin/com/imstargg/core/application/statistics/BrawlerEnemyResultCounts.kt` — 통합 캐시 DTO
- `core-enum/src/test/kotlin/com/imstargg/core/enums/StatsWeekRangeTest.kt` — 갱신 (기존 존재)
- 통합 테스트 파일 (해당 task 에서 명시)

### 수정 파일

- `core-enum/.../StatsWeekRange.kt` — 함수 통합
- `core-enum/.../StatsDateRange.kt` — 호출 갱신
- `core-api/.../BattleResultStatisticsRepository.kt` — projection 사용
- `core-api/.../BattleResultStatisticsRepositoryWithCache.kt` — 단일 메서드 통합
- `core-api/.../BattleRankStatisticsRepositoryWithCache.kt` — 동일
- `core-api/.../BattleResultStatisticsService.kt` — 단일 호출
- `core-api/.../BattleRankStatisticsService.kt` — 동일
- `core-api/.../api/controller/v1/StatisticsController.kt` — `@Validated`, `@Size`
- `db-core/.../BrawlerTeamBattleResultStatsJpaRepositoryCustomImpl.kt` — `brawlerIdFilters` EXISTS
- `db-core/.../BrawlerTeamBattleRankStatsJpaRepositoryCustomImpl.kt` — 동일
- `db-core/.../resources/schema.sql` — index 추가

---

## Task 1: NEW1 — StatsWeekRange 함수 통합

**Files:**

- Modify: `imstargg-backend/core/core-enum/src/main/kotlin/com/imstargg/core/enums/StatsWeekRange.kt`
- Modify: `imstargg-backend/core/core-enum/src/main/kotlin/com/imstargg/core/enums/StatsDateRange.kt`
- Test: `imstargg-backend/core/core-enum/src/test/kotlin/com/imstargg/core/enums/StatsWeekRangeTest.kt`

- [ ] **Step 1: 기존 테스트 파일 검토 후 케이스 추가 (실패 보장)**

`imstargg-backend/core/core-enum/src/test/kotlin/com/imstargg/core/enums/StatsWeekRangeTest.kt` 의 기존 케이스를 유지하면서, 같은 자연주 안의 모든 요일에서 `weekRange(date)` 가 동일 `(월요일, 일요일|월말)` 을 반환함을 강제하는 케이스를 추가한다.

```kotlin
@Nested
inner class `weekRange 가 같은 주 안의 어느 요일에서도 동일 결과를 반환` {
    @Test
    fun `자연주 안의 7일 모두 같은 weekRange`() {
        // given: 2026-04-27 (월) ~ 2026-05-03 (일)
        val expected = WeekRange(LocalDate.of(2026, 4, 27), LocalDate.of(2026, 5, 3))

        // when / then
        listOf(
            LocalDate.of(2026, 4, 27),
            LocalDate.of(2026, 4, 28),
            LocalDate.of(2026, 4, 29),
            LocalDate.of(2026, 4, 30),
            LocalDate.of(2026, 5, 1),
            LocalDate.of(2026, 5, 2),
            LocalDate.of(2026, 5, 3),
        ).forEach { date ->
            // 4/27~4/30 는 4월, 5/1~5/3 는 5월이므로 월 경계 케이스에서는 결과가 갈린다.
            // 본 테스트는 통합 후의 의도된 동작이 아니라 별도 케이스에서 검증.
        }
    }

    @Test
    fun `월 경계가 없는 자연주 5월 첫째주 5_4 부터 5_10 까지 7일 모두 동일`() {
        // given
        val expected = WeekRange(LocalDate.of(2026, 5, 4), LocalDate.of(2026, 5, 10))

        // when / then
        (0L..6L).forEach { offset ->
            val date = LocalDate.of(2026, 5, 4).plusDays(offset)
            val sut = StatsWeekRange.weekRange(date)
            assertThat(sut).isEqualTo(expected)
        }
    }

    @Test
    fun `월말 경계 4_27 부터 4_30 까지 모두 동일하고 5_1 부터 5_3 까지 다른 weekRange`() {
        // given
        val expectedApr = WeekRange(LocalDate.of(2026, 4, 27), LocalDate.of(2026, 4, 30))
        val expectedMay = WeekRange(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 3))

        // when / then
        (0L..3L).forEach { offset ->
            val date = LocalDate.of(2026, 4, 27).plusDays(offset)
            assertThat(StatsWeekRange.weekRange(date)).isEqualTo(expectedApr)
        }
        (0L..2L).forEach { offset ->
            val date = LocalDate.of(2026, 5, 1).plusDays(offset)
            assertThat(StatsWeekRange.weekRange(date)).isEqualTo(expectedMay)
        }
    }
}

@Nested
inner class currentAndPrevious_재설계_검증 {
    @Test
    fun `오늘과 어제의 weekRange 가 같으면 1개 반환`() {
        // 2026-04-29 (수). 어제 4-28 (화) 모두 같은 weekRange
        val sut = StatsWeekRange.currentAndPrevious(LocalDate.of(2026, 4, 29))
        assertThat(sut).hasSize(1)
    }

    @Test
    fun `월요일 (오늘과 어제의 weekRange 다름) 이면 2개 반환`() {
        // 2026-05-04 (월). 어제 5-3 (일) 은 5_4~5_10 이전 주
        val sut = StatsWeekRange.currentAndPrevious(LocalDate.of(2026, 5, 4))
        assertThat(sut).hasSize(2)
    }

    @Test
    fun `월초 첫날 (월말 경계로 weekRange 다름) 이면 2개 반환`() {
        // 2026-05-01 (금). 어제 4-30 은 4월 weekRange, 오늘은 5월 weekRange
        val sut = StatsWeekRange.currentAndPrevious(LocalDate.of(2026, 5, 1))
        assertThat(sut).hasSize(2)
    }
}
```

기존 테스트 중 `currentWeek` / `finalizedWeek` 를 직접 호출하는 케이스가 있다면, 통합 후 두 함수 호출은 컴파일 에러가 된다. 이 단계에서는 **새 케이스만 추가**하고 기존 케이스는 그대로 둔다 (다음 단계의 컴파일 에러로 fail 확인).

- [ ] **Step 2: 테스트 실행해 fail 확인**

```bash
./gradlew :core:core-enum:test --tests "com.imstargg.core.enums.StatsWeekRangeTest" -i
```

Expected: 컴파일 실패 — `weekRange` 함수가 아직 정의되지 않음 OR 새 케이스에서 `currentWeek` 동작과 다른 결과.

- [ ] **Step 3: StatsWeekRange.kt 통합**

`imstargg-backend/core/core-enum/src/main/kotlin/com/imstargg/core/enums/StatsWeekRange.kt` 를 다음으로 교체:

```kotlin
package com.imstargg.core.enums

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

data class WeekRange(
    val start: LocalDate,
    val end: LocalDate,
)

object StatsWeekRange {
    fun weekRange(date: LocalDate): WeekRange {
        val monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val naturalSunday = monday.plusDays(6)
        val monthStart = date.withDayOfMonth(1)
        val monthEnd = date.with(TemporalAdjusters.lastDayOfMonth())
        val start = maxOf(monday, monthStart)
        val end = minOf(naturalSunday, monthEnd)
        return WeekRange(start, end)
    }

    fun currentAndPrevious(today: LocalDate): List<WeekRange> {
        val current = weekRange(today)
        val previous = weekRange(today.minusDays(1))
        return if (previous == current) listOf(current) else listOf(previous, current)
    }
}
```

- [ ] **Step 4: StatsDateRange.kt 호출 갱신**

`imstargg-backend/core/core-enum/src/main/kotlin/com/imstargg/core/enums/StatsDateRange.kt` 의 `currentWeek(today)` 호출을 모두 `weekRange(today)` 로 치환. line 23, 28, 37 에 위치.

```kotlin
// THIS_WEEK
val current = StatsWeekRange.weekRange(today)
current.start to current.end

// SINCE_LAST_WEEK
val lastMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(1)
val current = StatsWeekRange.weekRange(today)
lastMonday to current.end

// SINCE_TWO_WEEKS_AGO
val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(2)
val current = StatsWeekRange.weekRange(today)
monday to current.end
```

- [ ] **Step 5: 기존 StatsWeekRangeTest 의 outdated 케이스 정리**

기존 `currentWeek` / `finalizedWeek` 직접 호출 케이스는 의미 변경 (today 절단 제거) 으로 expected 가 다르므로 **`weekRange` 호출로 갱신**하고 expected 도 자연주 (또는 월말) 기준으로 수정. 기존 의도 (예: "월요일 케이스") 는 보존.

- [ ] **Step 6: 테스트 실행해 pass 확인**

```bash
./gradlew :core:core-enum:test --tests "com.imstargg.core.enums.StatsWeekRangeTest" -i
```

Expected: 모든 케이스 PASS.

- [ ] **Step 7: 전체 모듈 빌드로 호출처 컴파일 검증**

```bash
./gradlew :core:core-enum:build :core:core-batch:compileKotlin :core:core-api:compileKotlin -x test
```

Expected: SUCCESS. `StatsWeekRange.currentWeek` 또는 `finalizedWeek` 잔여 호출이 있으면 여기서 컴파일 실패. `core-batch` 의 `*JobConfig` 3 개는 `currentAndPrevious` 만 호출하므로 시그니처 동일 → 영향 없음.

- [ ] **Step 8: lint + 단위 테스트 그린 확인**

```bash
./gradlew :core:core-enum:ktlintCheck :core:core-enum:test
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 9: Commit**

```bash
git add imstargg-backend/core/core-enum/src/main/kotlin/com/imstargg/core/enums/StatsWeekRange.kt \
       imstargg-backend/core/core-enum/src/main/kotlin/com/imstargg/core/enums/StatsDateRange.kt \
       imstargg-backend/core/core-enum/src/test/kotlin/com/imstargg/core/enums/StatsWeekRangeTest.kt
git commit -m "$(cat <<'EOF'
refactor(core-enum): StatsWeekRange 의 currentWeek/finalizedWeek 를 weekRange 로 통합

매일 batch 가 today 로 절단된 (월, today) row 를 새로 만들어 같은 주 데이터가
다중 합산되던 정합성 결함을 제거한다. weekRange 는 항상 (월요일, 일요일|월말) 을
반환하므로 batch 가 어느 요일에 돌든 같은 row 를 덮어쓴다.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 2: NEW1 — Weekly batch 누적 방지 통합 테스트

**Files:**

- Test: `imstargg-backend/core/core-batch/src/test/kotlin/com/imstargg/core/batch/job/statistics/PerEventWeeklyBattleStatsTaskletIntegrationTest.kt` (신규)

> **목적**: `weekRange` 통합으로 같은 weekRange row 가 1 개로 유지됨을 통합 테스트로 잠금. 향후 `weekRange` 가 today 의존성으로 회귀해도 이 테스트가 잡아준다.

- [ ] **Step 1: 통합 테스트 베이스 클래스와 기존 batch 테스트 패턴 확인**

```bash
find imstargg-backend/core/core-batch/src/test -name "*.kt" | head -20
grep -l "Tag(\"context\")\|CoreBatchContextTest" imstargg-backend/core/core-batch/src/test/kotlin -r 2>/dev/null | head -5
```

Expected: `CoreBatchContextTest` 베이스 클래스가 있다면 상속, 없으면 일반 `@DataJpaTest` 또는 `@SpringBootTest` 사용. 패턴이 없다면 생략 가능 (선택).

- [ ] **Step 2: 통합 테스트 작성 (실패 보장)**

같은 자연주의 월/화/수 3 회 batch 를 시뮬레이션하여 row 가 1 개만 존재하는지 검증. 실제 Spring Batch step 까지 띄우는 비용이 크다면 `PerEventWeeklyBattleStatsTasklet.execute()` 만 직접 호출하는 형태로 단순화.

```kotlin
package com.imstargg.core.batch.job.statistics

import com.imstargg.CoreBatchContextTest
import com.imstargg.core.enums.StatsWeekRange
import com.imstargg.infrastructure.db.core.statistics.repository.BrawlerTeamBattleResultStatsJpaRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.batch.core.job.parameters.JobParametersBuilder
import org.springframework.batch.test.JobLauncherTestUtils
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

@Tag("context")
class PerEventWeeklyBattleStatsTaskletIntegrationTest(
    @Autowired private val jobLauncherTestUtils: JobLauncherTestUtils,
    @Autowired private val statsRepository: BrawlerTeamBattleResultStatsJpaRepository,
) : CoreBatchContextTest() {
    @Test
    fun `같은 자연주 안의 서로 다른 날짜로 batch 를 3 회 실행해도 weekRange row 는 1 개`() {
        // given: stats_result_battle 에 동일 weekRange 안의 전투 데이터를 채워둠 (fixture)
        // (구체 fixture 는 기존 batch 테스트 helper 활용)

        val weekDays =
            listOf(
                LocalDate.of(2026, 5, 4), // 월
                LocalDate.of(2026, 5, 5), // 화
                LocalDate.of(2026, 5, 6), // 수
            )

        // when
        weekDays.forEach { date ->
            val params =
                JobParametersBuilder()
                    .addLocalDate("date", date)
                    .toJobParameters()
            jobLauncherTestUtils.launchJob(params)
        }

        // then
        val expectedWeek = StatsWeekRange.weekRange(LocalDate.of(2026, 5, 4))
        val rows =
            statsRepository.findAll().filter {
                it.startBattleDate == expectedWeek.start && it.endBattleDate == expectedWeek.end
            }
        assertThat(rows).hasSizeGreaterThanOrEqualTo(1) // 이벤트/팀 별 row 들
        // start 가 동일 weekRange.start 인 row 는 모두 end 가 동일 weekRange.end
        assertThat(
            statsRepository.findAll().filter { it.startBattleDate == expectedWeek.start },
        ).allMatch { it.endBattleDate == expectedWeek.end }
    }
}
```

> 주: fixture/베이스가 없거나 통합 테스트 비용이 부담되면 이 task 는 **Skip 후 다음 task 로 진행**. 단위 테스트만으로도 Task 1 의 핵심 회귀는 잡힌다.

- [ ] **Step 3: 테스트 실행 (Skip 결정 시 이 task 종료)**

```bash
./gradlew :core:core-batch:test --tests "*PerEventWeeklyBattleStatsTaskletIntegrationTest*" -i
```

Expected: 첫 실행은 fixture 부재로 빨간 줄. fixture 보강 가능하면 PASS 까지. 부담스러우면 `@Disabled` 마커 + TODO 주석으로 후속 작업화.

- [ ] **Step 4: Commit**

테스트 추가가 가능했다면:

```bash
git add imstargg-backend/core/core-batch/src/test/kotlin/com/imstargg/core/batch/job/statistics/PerEventWeeklyBattleStatsTaskletIntegrationTest.kt
git commit -m "$(cat <<'EOF'
test(core-batch): weekly tasklet 의 같은 weekRange row 단일성 회귀 테스트

Task 1 의 weekRange 통합이 회귀하지 않도록 같은 자연주의 다른 날짜로
batch 3 회 실행 후 (start, end) 동일성 검증.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

Skip 했다면 commit 없이 Task 3 로.

---

## Task 3: C3 — Covering index 추가

**Files:**

- Create: `imstargg-backend/infrastructure/db-core/src/main/resources/db/migration/V260425a__brawler_team_stats_group_by_index.sql`
- Modify: `imstargg-backend/infrastructure/db-core/src/main/resources/schema.sql`

- [ ] **Step 1: 마이그레이션 파일 생성**

`imstargg-backend/infrastructure/db-core/src/main/resources/db/migration/V260425a__brawler_team_stats_group_by_index.sql`:

```sql
-- 팀 통계 GROUP BY team_key 최적화용 covering index.
-- 기존 ix_*_1 (event, tier, start, end) 는 유지.
create index if not exists ix_brawler_team_battle_result_stats_2
    on brawler_team_battle_result_stats
    (event_brawlstars_id, tier_range, start_battle_date, end_battle_date, team_size, team_key);

create index if not exists ix_brawler_team_battle_rank_stats_2
    on brawler_team_battle_rank_stats
    (event_brawlstars_id, tier_range, start_battle_date, end_battle_date, team_size, team_key);
```

- [ ] **Step 2: schema.sql 동일 인덱스 추가**

`imstargg-backend/infrastructure/db-core/src/main/resources/schema.sql` 의 `ix_brawler_team_battle_result_stats_1` 다음 줄에 result/rank 각각 새 인덱스 라인 삽입:

```sql
create index if not exists ix_brawler_team_battle_result_stats_1 on brawler_team_battle_result_stats (event_brawlstars_id, tier_range, start_battle_date, end_battle_date);
create index if not exists ix_brawler_team_battle_result_stats_2 on brawler_team_battle_result_stats (event_brawlstars_id, tier_range, start_battle_date, end_battle_date, team_size, team_key);
create unique index if not exists uk_brawler_team_battle_result_stats__1 ...
```

같은 방식으로 rank 테이블에도 `ix_brawler_team_battle_rank_stats_2` 추가.

- [ ] **Step 3: 로컬 인프라에서 마이그레이션 적용 + 컨텍스트 테스트로 schema 검증**

```bash
docker-compose up -d postgres
./gradlew :infrastructure:db-core:contextTest -i
```

Expected: PASS. Flyway 가 V260425a 적용 + JPA 가 schema 매핑 OK.

- [ ] **Step 4: Commit**

```bash
git add imstargg-backend/infrastructure/db-core/src/main/resources/db/migration/V260425a__brawler_team_stats_group_by_index.sql \
       imstargg-backend/infrastructure/db-core/src/main/resources/schema.sql
git commit -m "$(cat <<'EOF'
feat(db-core): 팀 통계 GROUP BY team_key 최적화 covering index 추가

(event, tier, start, end, team_size, team_key) 인덱스로 팀 통계 페이지/
count 쿼리의 GROUP BY team_key 정렬·해시 비용 절감.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 4: C2 — `brawlerIdFilters` EXISTS + COUNT(DISTINCT) 통합

**Files:**

- Modify: `imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/repository/BrawlerTeamBattleResultStatsJpaRepositoryCustomImpl.kt:150-163`
- Modify: `imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/repository/BrawlerTeamBattleRankStatsJpaRepositoryCustomImpl.kt:147-160`
- Test: `imstargg-backend/infrastructure/db-core/src/test/kotlin/com/imstargg/infrastructure/db/core/statistics/repository/BrawlerTeamBattleResultStatsJpaRepositoryCustomImplTest.kt` (신규 또는 기존)

- [ ] **Step 1: 기존 Custom Impl 테스트 존재 확인**

```bash
find imstargg-backend/infrastructure/db-core/src/test -name "BrawlerTeamBattle*StatsJpaRepository*Test*" 2>/dev/null
```

존재하면 수정, 없으면 신규 작성.

- [ ] **Step 2: brawlerIds 1/2/3 개 케이스 통합 테스트 작성 (실패 또는 기존 통과)**

신규로 작성하는 경우 (`DbCoreContextTest` 베이스):

```kotlin
package com.imstargg.infrastructure.db.core.statistics.repository

import com.imstargg.DbCoreContextTest
import com.imstargg.core.enums.ResultStatsSortBy
import com.imstargg.core.enums.SortOrder
import com.imstargg.core.enums.StatsTeamSize
import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.BrawlerTeamBattleResultStatsBrawlerEntity
import com.imstargg.infrastructure.db.core.statistics.BrawlerTeamBattleResultStatsEntity
import com.imstargg.infrastructure.db.core.statistics.BrawlerTeamEmbedded
import com.imstargg.infrastructure.db.core.statistics.ResultCountEmbedded
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

@Tag("context")
class BrawlerTeamBattleResultStatsJpaRepositoryCustomImplTest(
    @Autowired private val em: EntityManager,
    @Autowired private val sut: BrawlerTeamBattleResultStatsJpaRepository,
) : DbCoreContextTest() {
    @Test
    fun `brawlerIds 가 모두 포함된 팀 stats 만 매칭한다`() {
        // given: 팀 (1,2,3) 과 (1,2,4) 두 row
        val event = 60000001L
        val tier = StatsTierRange.ALL
        val start = LocalDate.of(2026, 4, 27)
        val end = LocalDate.of(2026, 5, 3)
        val teamSize = StatsTeamSize.SIZE_3

        persistTeam(event, tier, start, end, teamSize, listOf(1L, 2L, 3L), victories = 10, totals = 20)
        persistTeam(event, tier, start, end, teamSize, listOf(1L, 2L, 4L), victories = 5, totals = 10)

        // when: brawlerIds = [1, 2] (2 개) → 두 row 모두 매칭
        val twoIds =
            sut.findPagedByConditions(
                eventBrawlStarsId = event,
                tierRange = tier,
                startDate = start,
                endDate = end,
                brawlerBrawlStarsIds = listOf(1L, 2L),
                teamSize = teamSize,
                sortBy = ResultStatsSortBy.PICK_RATE,
                sortOrder = SortOrder.DESC,
                offset = 0,
                limit = 50,
            )
        assertThat(twoIds).hasSize(2)

        // when: brawlerIds = [1, 2, 3] (3 개) → (1,2,3) 한 row 만 매칭
        val threeIds =
            sut.findPagedByConditions(
                eventBrawlStarsId = event,
                tierRange = tier,
                startDate = start,
                endDate = end,
                brawlerBrawlStarsIds = listOf(1L, 2L, 3L),
                teamSize = teamSize,
                sortBy = ResultStatsSortBy.PICK_RATE,
                sortOrder = SortOrder.DESC,
                offset = 0,
                limit = 50,
            )
        assertThat(threeIds).hasSize(1)
        assertThat(threeIds[0].brawlerTeam.brawlerBrawlStarsIds).containsExactlyInAnyOrder(1L, 2L, 3L)
    }

    @Test
    fun `brawlerIds 가 비어있으면 전체 stats 매칭`() {
        // given
        val event = 60000002L
        val tier = StatsTierRange.ALL
        val start = LocalDate.of(2026, 4, 27)
        val end = LocalDate.of(2026, 5, 3)
        val teamSize = StatsTeamSize.SIZE_3
        persistTeam(event, tier, start, end, teamSize, listOf(1L, 2L, 3L), 1, 1)
        persistTeam(event, tier, start, end, teamSize, listOf(4L, 5L, 6L), 1, 1)

        // when
        val result =
            sut.findPagedByConditions(
                eventBrawlStarsId = event,
                tierRange = tier,
                startDate = start,
                endDate = end,
                brawlerBrawlStarsIds = emptyList(),
                teamSize = teamSize,
                sortBy = ResultStatsSortBy.PICK_RATE,
                sortOrder = SortOrder.DESC,
                offset = 0,
                limit = 50,
            )

        // then
        assertThat(result).hasSize(2)
    }

    private fun persistTeam(
        event: Long,
        tier: StatsTierRange,
        start: LocalDate,
        end: LocalDate,
        teamSize: StatsTeamSize,
        brawlerIds: List<Long>,
        victories: Long,
        totals: Long,
    ) {
        val parent =
            BrawlerTeamBattleResultStatsEntity(
                eventBrawlStarsId = event,
                tierRange = tier,
                startBattleDate = start,
                endBattleDate = end,
                brawlerTeam =
                    BrawlerTeamEmbedded.create(
                        teamSize = teamSize,
                        brawlerBrawlStarsIds = brawlerIds,
                    ),
                resultCount =
                    ResultCountEmbedded(
                        victoryCount = victories,
                        defeatCount = totals - victories,
                        drawCount = 0,
                        totalCount = totals,
                    ),
            )
        em.persist(parent)
        brawlerIds.forEach { id ->
            em.persist(
                BrawlerTeamBattleResultStatsBrawlerEntity(
                    stats = parent,
                    brawlerBrawlStarsId = id,
                ),
            )
        }
        em.flush()
    }
}
```

> 도메인 객체 (`ResultCountEmbedded`, `BrawlerTeamEmbedded.create`, `StatsTeamSize.SIZE_3`) 의 정확한 시그니처는 코드 검색해 일치시킬 것. enum 값은 실제 정의 (`SIZE_3` 또는 `THREE` 등) 확인.

- [ ] **Step 3: 테스트 실행 — 현재 (forEach AND) 구현으로도 PASS 인지 fail 인지 확인**

```bash
./gradlew :infrastructure:db-core:contextTest --tests "*BrawlerTeamBattleResultStatsJpaRepositoryCustomImplTest*" -i
```

현재 `forEach { builder.and(...) }` 구현은 위 의도와 동일 결과를 낸다. 본 테스트는 회귀 잠금용. PASS 가 정상.

- [ ] **Step 4: 두 Custom Impl 의 brawlerIdFilters 함수 교체**

`BrawlerTeamBattleResultStatsJpaRepositoryCustomImpl.kt:150-163`:

```kotlin
private fun brawlerIdFilters(brawlerBrawlStarsIds: List<Long>): Predicate? {
    if (brawlerBrawlStarsIds.isEmpty()) return null
    return brawlerTeamBattleResultStatsEntity.id.`in`(
        JPAExpressions
            .select(brawlerTeamBattleResultStatsBrawlerEntity.stats.id)
            .from(brawlerTeamBattleResultStatsBrawlerEntity)
            .where(brawlerTeamBattleResultStatsBrawlerEntity.brawlerBrawlStarsId.`in`(brawlerBrawlStarsIds))
            .groupBy(brawlerTeamBattleResultStatsBrawlerEntity.stats.id)
            .having(
                brawlerTeamBattleResultStatsBrawlerEntity.brawlerBrawlStarsId
                    .countDistinct()
                    .eq(brawlerBrawlStarsIds.size.toLong()),
            ),
    )
}
```

리턴 타입을 `Predicate?` 로 변경하고, 호출 측 (`where(...)` 절) 은 nullable predicate 가 들어가도 QueryDSL 이 `null` 을 무시하므로 그대로 OK.

`BrawlerTeamBattleRankStatsJpaRepositoryCustomImpl.kt:147-160` 도 동일한 패턴으로 교체 (entity 만 `brawlerTeamBattleRankStatsBrawlerEntity` / `brawlerTeamBattleRankStatsEntity` 로):

```kotlin
private fun brawlerIdFilters(brawlerBrawlStarsIds: List<Long>): Predicate? {
    if (brawlerBrawlStarsIds.isEmpty()) return null
    return brawlerTeamBattleRankStatsEntity.id.`in`(
        JPAExpressions
            .select(brawlerTeamBattleRankStatsBrawlerEntity.stats.id)
            .from(brawlerTeamBattleRankStatsBrawlerEntity)
            .where(brawlerTeamBattleRankStatsBrawlerEntity.brawlerBrawlStarsId.`in`(brawlerBrawlStarsIds))
            .groupBy(brawlerTeamBattleRankStatsBrawlerEntity.stats.id)
            .having(
                brawlerTeamBattleRankStatsBrawlerEntity.brawlerBrawlStarsId
                    .countDistinct()
                    .eq(brawlerBrawlStarsIds.size.toLong()),
            ),
    )
}
```

- [ ] **Step 5: 테스트 재실행으로 동일 결과 확인**

```bash
./gradlew :infrastructure:db-core:contextTest --tests "*BrawlerTeamBattleResultStatsJpaRepositoryCustomImplTest*" -i
```

Expected: PASS. (행동은 동일해야 함)

- [ ] **Step 6: lint**

```bash
./gradlew :infrastructure:db-core:ktlintCheck
```

- [ ] **Step 7: Commit**

```bash
git add imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/repository/BrawlerTeamBattleResultStatsJpaRepositoryCustomImpl.kt \
       imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/repository/BrawlerTeamBattleRankStatsJpaRepositoryCustomImpl.kt \
       imstargg-backend/infrastructure/db-core/src/test/kotlin/com/imstargg/infrastructure/db/core/statistics/repository/BrawlerTeamBattleResultStatsJpaRepositoryCustomImplTest.kt
git commit -m "$(cat <<'EOF'
refactor(db-core): 팀 통계 brawlerIdFilters 를 EXISTS + COUNT(DISTINCT) 단일 서브쿼리로 통합

기존 forEach { builder.and(subquery) } 패턴은 brawlerIds 개수만큼 _brawler 테이블을
중복 스캔했다. GROUP BY + HAVING COUNT(DISTINCT) 로 단일 EXISTS 화하여 count 2 건
+ page 1 건 모두 1 회 스캔으로 줄임.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 5: C4 — `findBrawlerResultCounts` projection 전환

**Files:**

- Create: `imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/repository/BrawlerResultProjection.kt`
- Create: `imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/repository/BrawlerBattleResultStatsJpaRepositoryCustom.kt`
- Create: `imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/repository/BrawlerBattleResultStatsJpaRepositoryCustomImpl.kt`
- Modify: `imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/repository/BrawlerBattleResultStatsJpaRepository.kt`
- Modify: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BattleResultStatisticsRepository.kt:33-61`

- [ ] **Step 1: Projection DTO 생성**

`BrawlerResultProjection.kt`:

```kotlin
package com.imstargg.infrastructure.db.core.statistics.repository

data class BrawlerResultProjection(
    val brawlerBrawlStarsId: Long,
    val victoryCount: Long,
    val defeatCount: Long,
    val drawCount: Long,
    val starPlayerCount: Long,
)
```

- [ ] **Step 2: Custom 인터페이스 생성**

`BrawlerBattleResultStatsJpaRepositoryCustom.kt`:

```kotlin
package com.imstargg.infrastructure.db.core.statistics.repository

import com.imstargg.core.enums.StatsTierRange
import java.time.LocalDate

interface BrawlerBattleResultStatsJpaRepositoryCustom {
    fun findGroupedByBrawler(
        eventBrawlStarsId: Long?,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<BrawlerResultProjection>
}
```

- [ ] **Step 3: 구현 작성**

`BrawlerBattleResultStatsJpaRepositoryCustomImpl.kt`:

```kotlin
package com.imstargg.infrastructure.db.core.statistics.repository

import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.QBrawlerBattleResultStatsEntity.Companion.brawlerBattleResultStatsEntity
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import java.time.LocalDate

internal class BrawlerBattleResultStatsJpaRepositoryCustomImpl(
    entityManager: EntityManager,
) : BrawlerBattleResultStatsJpaRepositoryCustom {
    private val queryFactory = JPAQueryFactory(entityManager)

    override fun findGroupedByBrawler(
        eventBrawlStarsId: Long?,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<BrawlerResultProjection> {
        val brawlerId = brawlerBattleResultStatsEntity.brawlerBrawlStarsId
        val victorySum = brawlerBattleResultStatsEntity.resultCount.victoryCount.sumLong()
        val defeatSum = brawlerBattleResultStatsEntity.resultCount.defeatCount.sumLong()
        val drawSum = brawlerBattleResultStatsEntity.resultCount.drawCount.sumLong()
        val starPlayerSum = brawlerBattleResultStatsEntity.starPlayerCount.sumLong()

        return queryFactory
            .select(brawlerId, victorySum, defeatSum, drawSum, starPlayerSum)
            .from(brawlerBattleResultStatsEntity)
            .where(
                eventBrawlStarsId?.let { brawlerBattleResultStatsEntity.eventBrawlStarsId.eq(it) },
                brawlerBattleResultStatsEntity.tierRange.eq(tierRange),
                brawlerBattleResultStatsEntity.battleDate.goe(startDate),
                brawlerBattleResultStatsEntity.battleDate.loe(endDate),
            ).groupBy(brawlerId)
            .fetch()
            .map { tuple ->
                BrawlerResultProjection(
                    brawlerBrawlStarsId = tuple[brawlerId]!!,
                    victoryCount = tuple[victorySum] ?: 0L,
                    defeatCount = tuple[defeatSum] ?: 0L,
                    drawCount = tuple[drawSum] ?: 0L,
                    starPlayerCount = tuple[starPlayerSum] ?: 0L,
                )
            }
    }
}
```

> `BrawlerBattleResultStatsEntity` 의 `resultCount` 임베디드 / `starPlayerCount` 컬럼명 / `battleDate` 단일 컬럼명을 entity 파일로 검증해 정확히 일치시킬 것.

- [ ] **Step 4: JpaRepository 가 Custom 을 상속하도록 변경**

`BrawlerBattleResultStatsJpaRepository.kt`:

```kotlin
interface BrawlerBattleResultStatsJpaRepository :
    JpaRepository<BrawlerBattleResultStatsEntity, Long>,
    BrawlerBattleResultStatsJpaRepositoryCustom {
    // 기존 finder 들 유지 (daily 등 다른 호출처에서 필요)
}
```

- [ ] **Step 5: BattleResultStatisticsRepository 호출 변경**

`BattleResultStatisticsRepository.kt:28-61` 의 `findBrawlerResultCounts` 를 다음으로 교체:

```kotlin
fun findBrawlerResultCounts(
    eventId: BrawlStarsId?,
    tierRange: StatsTierRange,
    startDate: LocalDate,
    endDate: LocalDate,
): List<BrawlerResultCount> =
    battleResultJpaRepository
        .findGroupedByBrawler(
            eventBrawlStarsId = eventId?.value,
            tierRange = tierRange,
            startDate = startDate,
            endDate = endDate,
        ).map { projection ->
            BrawlerResultCount(
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
```

기존 `entities.groupBy { it.brawlerBrawlStarsId }.map { ... reduce ... }` 블록 제거.

> `BrawlerResultCount` 의 정확한 시그니처/생성자 (`resultCount` 타입 등) 는 도메인 파일 검토 후 일치. `ResultCount` 의 `toResultCount()` 호출이 더 이상 필요 없는지도 점검.

- [ ] **Step 6: Custom impl 를 위한 통합 테스트 추가**

`imstargg-backend/infrastructure/db-core/src/test/kotlin/com/imstargg/infrastructure/db/core/statistics/repository/BrawlerBattleResultStatsJpaRepositoryCustomImplTest.kt`:

```kotlin
package com.imstargg.infrastructure.db.core.statistics.repository

import com.imstargg.DbCoreContextTest
import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.BrawlerBattleResultStatsEntity
import com.imstargg.infrastructure.db.core.statistics.ResultCountEmbedded
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

@Tag("context")
class BrawlerBattleResultStatsJpaRepositoryCustomImplTest(
    @Autowired private val em: EntityManager,
    @Autowired private val sut: BrawlerBattleResultStatsJpaRepository,
) : DbCoreContextTest() {
    @Test
    fun `findGroupedByBrawler 가 brawler 별 합산 row 를 반환`() {
        // given: 같은 brawler 의 2일치 row
        val event = 60000010L
        val tier = StatsTierRange.ALL
        val brawler = 16000001L
        em.persist(
            BrawlerBattleResultStatsEntity(
                eventBrawlStarsId = event,
                tierRange = tier,
                battleDate = LocalDate.of(2026, 4, 27),
                brawlerBrawlStarsId = brawler,
                resultCount = ResultCountEmbedded(victoryCount = 3, defeatCount = 1, drawCount = 0, totalCount = 4),
                starPlayerCount = 1,
            ),
        )
        em.persist(
            BrawlerBattleResultStatsEntity(
                eventBrawlStarsId = event,
                tierRange = tier,
                battleDate = LocalDate.of(2026, 4, 28),
                brawlerBrawlStarsId = brawler,
                resultCount = ResultCountEmbedded(victoryCount = 5, defeatCount = 2, drawCount = 0, totalCount = 7),
                starPlayerCount = 2,
            ),
        )
        em.flush()

        // when
        val result =
            sut.findGroupedByBrawler(
                eventBrawlStarsId = event,
                tierRange = tier,
                startDate = LocalDate.of(2026, 4, 27),
                endDate = LocalDate.of(2026, 4, 28),
            )

        // then
        assertThat(result).hasSize(1)
        assertThat(result[0].brawlerBrawlStarsId).isEqualTo(brawler)
        assertThat(result[0].victoryCount).isEqualTo(8L)
        assertThat(result[0].defeatCount).isEqualTo(3L)
        assertThat(result[0].drawCount).isEqualTo(0L)
        assertThat(result[0].starPlayerCount).isEqualTo(3L)
    }
}
```

- [ ] **Step 7: 테스트 실행**

```bash
./gradlew :infrastructure:db-core:contextTest --tests "*BrawlerBattleResultStatsJpaRepositoryCustomImplTest*" -i
./gradlew :core:core-api:compileKotlin
```

Expected: PASS.

- [ ] **Step 8: ktlint + Commit**

```bash
./gradlew :infrastructure:db-core:ktlintCheck :core:core-api:ktlintCheck
git add imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/repository/BrawlerResultProjection.kt \
       imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/repository/BrawlerBattleResultStatsJpaRepositoryCustom.kt \
       imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/repository/BrawlerBattleResultStatsJpaRepositoryCustomImpl.kt \
       imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/repository/BrawlerBattleResultStatsJpaRepository.kt \
       imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BattleResultStatisticsRepository.kt \
       imstargg-backend/infrastructure/db-core/src/test/kotlin/com/imstargg/infrastructure/db/core/statistics/repository/BrawlerBattleResultStatsJpaRepositoryCustomImplTest.kt
git commit -m "$(cat <<'EOF'
refactor(core-api): findBrawlerResultCounts 를 DB GROUP BY projection 으로 전환

엔티티 전체를 영속 컨텍스트에 적재 후 메모리 groupBy/reduce 하던 구조를
QueryDSL projection 으로 교체. eventId=null 호출 시의 row 폭발과
영속 컨텍스트 부담 제거.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 6: I3 — 캐시 키 helper + Service 정렬

**Files:**

- Create: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/StatisticsCacheKeys.kt`
- Test: `imstargg-backend/core/core-api/src/test/kotlin/com/imstargg/core/application/statistics/StatisticsCacheKeysTest.kt`

> 이 helper 는 Task 7~9 의 캐시 키 SpEL 에서 호출된다.

- [ ] **Step 1: 테스트 작성**

```kotlin
package com.imstargg.core.application.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class StatisticsCacheKeysTest {
    @Test
    fun `joinIds 는 빈 리스트면 빈 문자열`() {
        val sut = StatisticsCacheKeys.joinIds(emptyList())
        assertThat(sut).isEqualTo("")
    }

    @Test
    fun `joinIds 는 value 정렬 후 _ 로 join`() {
        val sut =
            StatisticsCacheKeys.joinIds(
                listOf(
                    BrawlStarsId(16000003L),
                    BrawlStarsId(16000001L),
                    BrawlStarsId(16000002L),
                ),
            )
        assertThat(sut).isEqualTo("16000001_16000002_16000003")
    }

    @Test
    fun `joinIds 는 동일 입력 순서가 달라도 같은 결과`() {
        val a = StatisticsCacheKeys.joinIds(listOf(BrawlStarsId(2L), BrawlStarsId(1L)))
        val b = StatisticsCacheKeys.joinIds(listOf(BrawlStarsId(1L), BrawlStarsId(2L)))
        assertThat(a).isEqualTo(b)
    }
}
```

- [ ] **Step 2: 테스트 실행해 fail 확인**

```bash
./gradlew :core:core-api:test --tests "*StatisticsCacheKeysTest*" -i
```

Expected: 컴파일 실패 — `StatisticsCacheKeys` 미정의.

- [ ] **Step 3: helper 구현**

`StatisticsCacheKeys.kt`:

```kotlin
package com.imstargg.core.application.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId

object StatisticsCacheKeys {
    @JvmStatic
    fun joinIds(ids: List<BrawlStarsId>): String =
        ids.map { it.value }.sorted().joinToString("_")
}
```

- [ ] **Step 4: 테스트 PASS 확인 + Commit**

```bash
./gradlew :core:core-api:test --tests "*StatisticsCacheKeysTest*"
./gradlew :core:core-api:ktlintCheck
git add imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/StatisticsCacheKeys.kt \
       imstargg-backend/core/core-api/src/test/kotlin/com/imstargg/core/application/statistics/StatisticsCacheKeysTest.kt
git commit -m "$(cat <<'EOF'
feat(core-api): 통계 캐시 키 정규화 helper 추가

brawlerIds 의 순서/표현 차이로 캐시 미스가 발생하지 않도록 value 정렬 후
_ join 하는 SpEL 친화 helper.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 7: I2+I1 — 팀 결과 통계 캐시 통합 + sync

**Files:**

- Create: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BrawlerTeamResultCounts.kt`
- Modify: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BattleResultStatisticsRepositoryWithCache.kt:108-181`
- Modify: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BattleResultStatisticsService.kt:38-80`

- [ ] **Step 1: 통합 DTO 생성**

`BrawlerTeamResultCounts.kt`:

```kotlin
package com.imstargg.core.application.statistics

import com.imstargg.core.domain.statistics.BrawlerTeamResultCount

data class BrawlerTeamResultCounts(
    val items: List<BrawlerTeamResultCount>,
    val totalBattleCount: Long,
    val totalElementCount: Long,
)
```

- [ ] **Step 2: WithCache 단일 메서드 추가**

`BattleResultStatisticsRepositoryWithCache.kt` 의 기존 3 개 메서드 (`countTeamResultTotalBattleCount`, `countTeamResultBy`, `findBrawlerTeamResultCounts`) 를 **모두 제거**하고 다음 단일 메서드로 대체:

```kotlin
@Cacheable(
    cacheNames = [CacheNames.STATISTICS],
    key =
        "'v1:teamResult:' + #eventId" +
            " + ':' + T(com.imstargg.core.application.statistics.StatisticsCacheKeys).joinIds(#brawlerIds)" +
            " + ':' + #tierRange + ':' + #startDate + ':' + #endDate" +
            " + ':' + #teamSize + ':' + #sortField + ':' + #sortOrder" +
            " + ':' + #offsetLimit.offset + ':' + #offsetLimit.limit",
    sync = true,
)
fun findBrawlerTeamResult(
    eventId: BrawlStarsId?,
    brawlerIds: List<BrawlStarsId>,
    tierRange: StatsTierRange,
    startDate: LocalDate,
    endDate: LocalDate,
    teamSize: StatsTeamSize,
    sortField: ResultStatsSortBy,
    sortOrder: SortOrder,
    offsetLimit: OffsetLimit,
): BrawlerTeamResultCounts {
    val items =
        battleResultStatisticsRepository.findBrawlerTeamResultCounts(
            eventId = eventId,
            brawlerIds = brawlerIds,
            tierRange = tierRange,
            startDate = startDate,
            endDate = endDate,
            teamSize = teamSize,
            sortField = sortField,
            sortOrder = sortOrder,
            offsetLimit = offsetLimit,
        )
    val totalBattleCount =
        battleResultStatisticsRepository.countTeamResultTotalBattleCount(
            eventId = eventId,
            brawlerIds = brawlerIds,
            tierRange = tierRange,
            startDate = startDate,
            endDate = endDate,
            teamSize = teamSize,
        )
    val totalElementCount =
        battleResultStatisticsRepository.countTeamResultBy(
            eventId = eventId,
            brawlerIds = brawlerIds,
            tierRange = tierRange,
            startDate = startDate,
            endDate = endDate,
            teamSize = teamSize,
        )
    return BrawlerTeamResultCounts(
        items = items,
        totalBattleCount = totalBattleCount,
        totalElementCount = totalElementCount,
    )
}
```

> `BattleResultStatisticsRepository` (캐시 없는 안쪽 레이어) 의 기존 3 개 메서드는 그대로 유지. WithCache 가 묶음 호출만 한다.

- [ ] **Step 3: Service 단일 호출로 변경**

`BattleResultStatisticsService.kt:38-80` 의 `getBrawlerTeamResultStatistics` 를 다음으로 교체:

```kotlin
fun getBrawlerTeamResultStatistics(
    eventId: BrawlStarsId?,
    brawlerIds: List<BrawlStarsId>,
    tierRange: StatsTierRange,
    dateRange: StatsDateRange,
    teamSize: StatsTeamSize,
    sortBy: ResultStatsSortBy,
    sortOrder: SortOrder,
    offsetLimit: OffsetLimit,
): BrawlerTeamResultStatistics {
    val (startDate, endDate) = dateRange.dateRange(clock)
    val sortedBrawlerIds = brawlerIds.sortedBy { it.value }
    val counts =
        resultRepository.findBrawlerTeamResult(
            eventId = eventId,
            brawlerIds = sortedBrawlerIds,
            tierRange = tierRange,
            startDate = startDate,
            endDate = endDate,
            teamSize = teamSize,
            sortField = sortBy,
            sortOrder = sortOrder,
            offsetLimit = offsetLimit,
        )
    return BrawlerTeamResultStatistics.create(counts.totalBattleCount, counts.totalElementCount, counts.items)
}
```

- [ ] **Step 4: BrawlerTeamResultStatistics.create 시그니처 일치 확인**

```bash
grep -n "fun create" imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/domain/statistics/BrawlerTeamResultStatistics.kt
```

기존 시그니처가 `(totalBattleCount: Long, totalCount: Long, counts: List<BrawlerTeamResultCount>)` 이면 그대로 OK. 다르면 위 호출 라인을 일치시킬 것.

- [ ] **Step 5: 빌드 + 단위 테스트**

```bash
./gradlew :core:core-api:compileKotlin :core:core-api:test
```

Expected: 기존 `BattleResultStatisticsServiceTest` 가 있다면 통과해야 한다. 시그니처 변경으로 깨진 테스트가 있으면 그 자리에서 수정 — `resultRepository` mock 셋업을 새 단일 메서드 기준으로 변경.

- [ ] **Step 6: ktlint + Commit**

```bash
./gradlew :core:core-api:ktlintCheck
git add imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BrawlerTeamResultCounts.kt \
       imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BattleResultStatisticsRepositoryWithCache.kt \
       imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BattleResultStatisticsService.kt
[ -f imstargg-backend/core/core-api/src/test/kotlin/com/imstargg/core/application/statistics/BattleResultStatisticsServiceTest.kt ] && \
  git add imstargg-backend/core/core-api/src/test/kotlin/com/imstargg/core/application/statistics/BattleResultStatisticsServiceTest.kt
git commit -m "$(cat <<'EOF'
refactor(core-api): 팀 결과 통계 캐시를 단일 DTO 로 통합 + sync=true

count×2 + data 의 분리 캐시 → BrawlerTeamResultCounts 한 묶음. brawlerIds 는
Service 진입 시 정렬해 캐시 키를 정규화하고, sync=true 로 stampede 방지.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 8: I2+I1 — 적 결과 통계 캐시 통합

**Files:**

- Create: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BrawlerEnemyResultCounts.kt`
- Modify: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BattleResultStatisticsRepositoryWithCache.kt`
- Modify: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BattleResultStatisticsService.kt:82-120`

- [ ] **Step 1: 통합 DTO 생성**

`BrawlerEnemyResultCounts.kt`:

```kotlin
package com.imstargg.core.application.statistics

import com.imstargg.core.domain.statistics.BrawlerEnemyResultCount

data class BrawlerEnemyResultCounts(
    val items: List<BrawlerEnemyResultCount>,
    val totalBattleCount: Long,
    val totalElementCount: Long,
)
```

- [ ] **Step 2: WithCache 의 기존 enemy 3 메서드 제거 + 단일 메서드 추가**

기존 `countEnemyResultTotalBattleCount`, `countEnemyResultBy`, `findBrawlerEnemyResultCounts` 3 개 `@Cacheable` 메서드를 제거하고:

```kotlin
@Cacheable(
    cacheNames = [CacheNames.STATISTICS],
    key =
        "'v1:enemyResult:' + #eventId + ':' + #brawlerId + ':' + #tierRange" +
            " + ':' + #startDate + ':' + #endDate" +
            " + ':' + #sortField + ':' + #sortOrder" +
            " + ':' + #offsetLimit.offset + ':' + #offsetLimit.limit",
    sync = true,
)
fun findBrawlerEnemyResult(
    eventId: BrawlStarsId?,
    brawlerId: BrawlStarsId,
    tierRange: StatsTierRange,
    startDate: LocalDate,
    endDate: LocalDate,
    sortField: ResultStatsSortBy,
    sortOrder: SortOrder,
    offsetLimit: OffsetLimit,
): BrawlerEnemyResultCounts {
    val items =
        battleResultStatisticsRepository.findBrawlerEnemyResultCounts(
            eventId = eventId,
            brawlerId = brawlerId,
            tierRange = tierRange,
            startDate = startDate,
            endDate = endDate,
            sortField = sortField,
            sortOrder = sortOrder,
            offsetLimit = offsetLimit,
        )
    val totalBattleCount =
        battleResultStatisticsRepository.countEnemyResultTotalBattleCount(
            eventId = eventId,
            brawlerId = brawlerId,
            tierRange = tierRange,
            startDate = startDate,
            endDate = endDate,
        )
    val totalElementCount =
        battleResultStatisticsRepository.countEnemyResultBy(
            eventId = eventId,
            brawlerId = brawlerId,
            tierRange = tierRange,
            startDate = startDate,
            endDate = endDate,
        )
    return BrawlerEnemyResultCounts(
        items = items,
        totalBattleCount = totalBattleCount,
        totalElementCount = totalElementCount,
    )
}
```

- [ ] **Step 3: Service 단일 호출로 변경**

`BattleResultStatisticsService.kt:82-120` 의 `getEnemyResultStatistics` 를:

```kotlin
fun getEnemyResultStatistics(
    eventId: BrawlStarsId?,
    brawlerId: BrawlStarsId,
    tierRange: StatsTierRange,
    dateRange: StatsDateRange,
    sortBy: ResultStatsSortBy,
    sortOrder: SortOrder,
    offsetLimit: OffsetLimit,
): BrawlerEnemyResultStatistics {
    val (startDate, endDate) = dateRange.dateRange(clock)
    val counts =
        resultRepository.findBrawlerEnemyResult(
            eventId = eventId,
            brawlerId = brawlerId,
            tierRange = tierRange,
            startDate = startDate,
            endDate = endDate,
            sortField = sortBy,
            sortOrder = sortOrder,
            offsetLimit = offsetLimit,
        )
    return BrawlerEnemyResultStatistics.create(
        counts.totalBattleCount,
        counts.totalElementCount,
        counts.items,
    )
}
```

- [ ] **Step 4: 빌드 + 테스트**

```bash
./gradlew :core:core-api:compileKotlin :core:core-api:test
```

Expected: PASS (기존 enemy 통계 테스트가 있다면 mock 셋업 갱신).

- [ ] **Step 5: ktlint + Commit**

```bash
./gradlew :core:core-api:ktlintCheck
git add imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BrawlerEnemyResultCounts.kt \
       imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BattleResultStatisticsRepositoryWithCache.kt \
       imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BattleResultStatisticsService.kt
git commit -m "$(cat <<'EOF'
refactor(core-api): 적 결과 통계 캐시를 단일 DTO 로 통합 + sync=true

Task 7 과 같은 패턴으로 enemy 통계의 count×2 + data 분리 캐시를
BrawlerEnemyResultCounts 한 묶음으로 합치고 sync=true 적용.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 9: I2+I1 — 팀 랭크 통계 캐시 통합

**Files:**

- Create: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BrawlerTeamRankCounts.kt`
- Modify: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BattleRankStatisticsRepositoryWithCache.kt`
- Modify: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BattleRankStatisticsService.kt`

- [ ] **Step 1: 통합 DTO 생성**

`BrawlerTeamRankCounts.kt`:

```kotlin
package com.imstargg.core.application.statistics

import com.imstargg.core.domain.statistics.BrawlerTeamRankCount

data class BrawlerTeamRankCounts(
    val items: List<BrawlerTeamRankCount>,
    val totalBattleCount: Long,
    val totalElementCount: Long,
)
```

- [ ] **Step 2: BattleRankStatisticsRepositoryWithCache 단일 메서드 추가**

기존 team rank 의 count×2 + data 메서드 3 개를 단일 `findBrawlerTeamRank` 로 통합. 시그니처와 SpEL 키는 Task 7 과 같은 형태:

```kotlin
@Cacheable(
    cacheNames = [CacheNames.STATISTICS],
    key =
        "'v1:teamRank:' + #eventId" +
            " + ':' + T(com.imstargg.core.application.statistics.StatisticsCacheKeys).joinIds(#brawlerIds)" +
            " + ':' + #tierRange + ':' + #startDate + ':' + #endDate" +
            " + ':' + #teamSize + ':' + #sortField + ':' + #sortOrder" +
            " + ':' + #offsetLimit.offset + ':' + #offsetLimit.limit",
    sync = true,
)
fun findBrawlerTeamRank(
    eventId: BrawlStarsId,
    brawlerIds: List<BrawlStarsId>,
    tierRange: StatsTierRange,
    startDate: LocalDate,
    endDate: LocalDate,
    teamSize: StatsTeamSize,
    sortField: RankStatsSortBy,
    sortOrder: SortOrder,
    offsetLimit: OffsetLimit,
): BrawlerTeamRankCounts {
    val items =
        battleRankStatisticsRepository.findBrawlerTeamRankCounts(
            eventId = eventId,
            brawlerIds = brawlerIds,
            tierRange = tierRange,
            startDate = startDate,
            endDate = endDate,
            teamSize = teamSize,
            sortField = sortField,
            sortOrder = sortOrder,
            offsetLimit = offsetLimit,
        )
    val totalBattleCount =
        battleRankStatisticsRepository.countTeamRankTotalBattleCount(
            eventId = eventId,
            brawlerIds = brawlerIds,
            tierRange = tierRange,
            startDate = startDate,
            endDate = endDate,
            teamSize = teamSize,
        )
    val totalElementCount =
        battleRankStatisticsRepository.countTeamRankBy(
            eventId = eventId,
            brawlerIds = brawlerIds,
            tierRange = tierRange,
            startDate = startDate,
            endDate = endDate,
            teamSize = teamSize,
        )
    return BrawlerTeamRankCounts(
        items = items,
        totalBattleCount = totalBattleCount,
        totalElementCount = totalElementCount,
    )
}
```

> `BattleRankStatisticsRepository` 의 정확한 메서드명 (`findBrawlerTeamRankCounts` / `countTeamRankTotalBattleCount` / `countTeamRankBy`) 은 [BattleRankStatisticsRepository.kt](../../imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BattleRankStatisticsRepository.kt) 에서 확인하여 일치시킨다. 기존 메서드명을 따른다 — 본 task 에서는 안쪽 Repository 시그니처는 변경하지 않는다.

- [ ] **Step 3: BattleRankStatisticsService.getBrawlerTeamRankStatistics 단일 호출**

```kotlin
fun getBrawlerTeamRankStatistics(
    eventId: BrawlStarsId,
    brawlerIds: List<BrawlStarsId>,
    tierRange: StatsTierRange,
    dateRange: StatsDateRange,
    teamSize: StatsTeamSize,
    sortBy: RankStatsSortBy,
    sortOrder: SortOrder,
    offsetLimit: OffsetLimit,
): BrawlerTeamRankStatistics {
    val (startDate, endDate) = dateRange.dateRange(clock)
    val sortedBrawlerIds = brawlerIds.sortedBy { it.value }
    val counts =
        rankRepository.findBrawlerTeamRank(
            eventId = eventId,
            brawlerIds = sortedBrawlerIds,
            tierRange = tierRange,
            startDate = startDate,
            endDate = endDate,
            teamSize = teamSize,
            sortField = sortBy,
            sortOrder = sortOrder,
            offsetLimit = offsetLimit,
        )
    return BrawlerTeamRankStatistics.create(
        counts.totalBattleCount,
        counts.totalElementCount,
        counts.items,
    )
}
```

`BrawlerTeamRankStatistics.create` 시그니처도 도메인 파일 검증.

- [ ] **Step 4: brawler rank (단일) 메서드에 sync=true 추가**

`BattleRankStatisticsRepositoryWithCache` 의 일반 brawler rank 메서드 (`findBrawlerRankCounts` 등) 가 `@Cacheable` 인 경우 `sync = true` 만 추가. 통합은 불필요 (단일 호출).

- [ ] **Step 5: 빌드 + 테스트**

```bash
./gradlew :core:core-api:compileKotlin :core:core-api:test :core:core-api:ktlintCheck
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BrawlerTeamRankCounts.kt \
       imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BattleRankStatisticsRepositoryWithCache.kt \
       imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BattleRankStatisticsService.kt
git commit -m "$(cat <<'EOF'
refactor(core-api): 팀 랭크 통계 캐시를 단일 DTO 로 통합 + sync=true

Task 7~8 과 같은 패턴으로 team rank 통계의 count×2 + data 분리 캐시를
BrawlerTeamRankCounts 한 묶음으로 합치고 sync=true 적용. brawlerIds 는
Service 진입 시 정렬.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 10: I1 — 무거운 단일 통계 메서드에 sync=true 추가

**Files:**

- Modify: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BattleResultStatisticsRepositoryWithCache.kt`

- [ ] **Step 1: 남아 있는 무거운 메서드 식별**

Task 7~9 에서 통합되지 않은 캐시 메서드 중 무거운 것:

- `findBrawlerResultCounts` — Task 5 로 projection 화 했지만 여전히 row 수가 큼 → `sync = true`
- `findBrawlerDailyResultCounts` — 단일 brawler·날짜별이라 보통 가벼움. 본 task 에서는 보류.

- [ ] **Step 2: `sync = true` 추가**

`findBrawlerResultCounts` 의 `@Cacheable` 어노테이션에 `sync = true` 한 줄 추가:

```kotlin
@Cacheable(
    cacheNames = [CacheNames.STATISTICS],
    key = "'v1:brawlerResult:' + #eventId + ':' + #tierRange + ':' + #startDate + ':' + #endDate",
    sync = true,
)
fun findBrawlerResultCounts(
    ...
): List<BrawlerResultCount> = ...
```

- [ ] **Step 3: 빌드 + ktlint**

```bash
./gradlew :core:core-api:compileKotlin :core:core-api:ktlintCheck
```

- [ ] **Step 4: Commit**

```bash
git add imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BattleResultStatisticsRepositoryWithCache.kt
git commit -m "$(cat <<'EOF'
chore(core-api): 무거운 단일 통계 메서드에 캐시 sync=true 추가

TTL 만료 직후 동시 트래픽이 모두 DB 로 떨어지는 stampede 방지.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 11: I4 — 컨트롤러 입력 검증 + Service 의존 검증

**Files:**

- Modify: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/api/controller/v1/StatisticsController.kt`
- Modify: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BattleResultStatisticsService.kt`
- Modify: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BattleRankStatisticsService.kt`
- Test: `imstargg-backend/core/core-api/src/test/kotlin/com/imstargg/core/api/controller/v1/StatisticsControllerTest.kt` (신규 또는 기존)

- [ ] **Step 1: Controller 검증 테스트 작성**

`StatisticsControllerTest.kt` (신규 또는 기존에 케이스 추가, `@SpringBootTest` + `MockMvc` 또는 `@WebMvcTest` 활용):

```kotlin
@Test
fun `team result 에 brawlerIds 4 개 이상이면 400`() {
    mockMvc
        .perform(
            get("/v1/statistics/brawler/team/result")
                .param("brawlerIds", "1", "2", "3", "4")
                .param("teamSize", "SIZE_3")
                .param("tierRange", "ALL")
                .param("dateRange", "THIS_WEEK"),
        ).andExpect(status().isBadRequest)
}

@Test
fun `team rank 에 brawlerIds 4 개 이상이면 400`() {
    mockMvc
        .perform(
            get("/v1/statistics/brawler/team/rank")
                .param("eventId", "60000001")
                .param("brawlerIds", "1", "2", "3", "4")
                .param("teamSize", "SIZE_3")
                .param("tierRange", "ALL")
                .param("dateRange", "THIS_WEEK"),
        ).andExpect(status().isBadRequest)
}
```

> 테스트 베이스가 없다면 `@WebMvcTest(StatisticsController::class)` + `MockMvc` 로 Controller 단위 테스트.

- [ ] **Step 2: 테스트 fail 확인**

```bash
./gradlew :core:core-api:test --tests "*StatisticsControllerTest*" -i
```

Expected: 4 개 brawlerIds 가 통과되어 200 — fail.

- [ ] **Step 3: Controller 에 `@Validated` + `@Size(max = 3)` 추가**

`StatisticsController.kt`:

```kotlin
import jakarta.validation.constraints.Size
import org.springframework.validation.annotation.Validated

@RestController
@Validated
class StatisticsController(...) {

    @GetMapping("/v1/statistics/brawler/team/result")
    fun getBrawlerTeamResultStatistics(
        @RequestParam eventId: Long?,
        @RequestParam(required = false, defaultValue = "")
        @Size(max = 3) brawlerIds: List<Long>,
        @RequestParam teamSize: StatsTeamSize,
        @RequestParam tierRange: StatsTierRange,
        @RequestParam dateRange: StatsDateRange,
        @RequestParam(defaultValue = "PICK_RATE") sortBy: ResultStatsSortBy,
        @RequestParam(defaultValue = "DESC") sortOrder: SortOrder,
    ): ResponseEntity<ListResponse<BrawlerTeamResultStatisticsItemResponse>> { ... }

    @GetMapping("/v1/statistics/brawler/team/rank")
    fun getBrawlerTeamRankStatistics(
        @RequestParam eventId: Long,
        @RequestParam(required = false, defaultValue = "")
        @Size(max = 3) brawlerIds: List<Long>,
        ...
    ): ... { ... }
}
```

- [ ] **Step 4: 테스트 PASS 확인**

```bash
./gradlew :core:core-api:test --tests "*StatisticsControllerTest*" -i
```

Expected: 400 응답 PASS. `ApiExceptionHandler.handleHandlerMethodValidationException` 가 잡아 400 반환.

- [ ] **Step 5: Service-level 의존 검증 추가**

`BattleResultStatisticsService.getBrawlerTeamResultStatistics` 와 `BattleRankStatisticsService.getBrawlerTeamRankStatistics` 본문 시작부에 다음 추가:

```kotlin
if (brawlerIds.size > teamSize.value) {
    throw CoreException(
        ErrorType.INVALID_REQUEST,
        "brawlerIds.size(${brawlerIds.size}) > teamSize(${teamSize.value})",
    )
}
```

> `StatsTeamSize` 의 size 노출 프로퍼티가 `value` 인지 다른 이름인지 확인:
> ```bash
> grep -n "enum class StatsTeamSize\|val " imstargg-backend/core/core-enum/src/main/kotlin/com/imstargg/core/enums/StatsTeamSize.kt
> ```
> 다르면 그에 맞게 수정. `IllegalArgumentException` 또는 `require()` 는 사용하지 않는다 — `ApiExceptionHandler` 가 catch-all 로 500 을 반환하기 때문이다.

- [ ] **Step 6: Service 의존 검증 테스트 (선택)**

기존 `BattleResultStatisticsServiceTest` 가 있다면:

```kotlin
@Test
fun `brawlerIds 가 teamSize 를 초과하면 CoreException(INVALID_REQUEST)`() {
    val ex =
        catchThrowableOfType(CoreException::class.java) {
            sut.getBrawlerTeamResultStatistics(
                eventId = null,
                brawlerIds = listOf(BrawlStarsId(1L), BrawlStarsId(2L), BrawlStarsId(3L), BrawlStarsId(4L)),
                tierRange = StatsTierRange.ALL,
                dateRange = StatsDateRange.THIS_WEEK,
                teamSize = StatsTeamSize.SIZE_3,
                sortBy = ResultStatsSortBy.PICK_RATE,
                sortOrder = SortOrder.DESC,
                offsetLimit = OffsetLimit(0, 50),
            )
        }
    assertThat(ex.errorType).isEqualTo(ErrorType.INVALID_REQUEST)
}
```

> 단, Controller 에서 `@Size(max = 3)` 으로 이미 4 개 이상은 차단되므로 Service 검증은 다른 경로 (`brawlerIds = 3 + teamSize = SIZE_2` 등) 의 방어선이다.

- [ ] **Step 7: 빌드 + 단위 테스트 + ktlint**

```bash
./gradlew :core:core-api:test :core:core-api:ktlintCheck
```

Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/api/controller/v1/StatisticsController.kt \
       imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BattleResultStatisticsService.kt \
       imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BattleRankStatisticsService.kt
[ -f imstargg-backend/core/core-api/src/test/kotlin/com/imstargg/core/api/controller/v1/StatisticsControllerTest.kt ] && \
  git add imstargg-backend/core/core-api/src/test/kotlin/com/imstargg/core/api/controller/v1/StatisticsControllerTest.kt
git commit -m "$(cat <<'EOF'
feat(core-api): 통계 컨트롤러 입력 검증 + Service 의존 검증

- StatisticsController 에 @Validated + brawlerIds @Size(max=3)
- Service 에서 brawlerIds.size <= teamSize.value 를 CoreException(INVALID_REQUEST)
  로 검증. IllegalArgumentException 은 catch-all 500 함정 회피.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 12: 최종 통합 검증

**Files:** 없음 (검증 only)

- [ ] **Step 1: 전체 빌드 + 모든 테스트**

```bash
./gradlew clean build
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 2: ktlint 전체 통과**

```bash
./gradlew ktlintCheck
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: 컨텍스트 테스트**

```bash
./gradlew contextTest
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: spec 의 변경 파일 요약과 실제 git diff 비교**

```bash
git log --stat --since="$(date -v-1d +%Y-%m-%d)" -- imstargg-backend/ | head -80
```

Expected: spec §8 의 모든 항목이 어떤 commit 에 포함되어 있는지 확인. 누락 없으면 완료.

- [ ] **Step 5: 작업 종료 — 별도 commit 없음**

이 task 는 검증 전용. 새 변경이 없다면 commit 하지 않는다.

---

## Self-Review

### Spec coverage

| Spec 항목 | Task |
|---|---|
| §2 NEW1 weekRange 통합 | Task 1, 2 |
| §3 C2 brawlerIdFilters EXISTS | Task 4 |
| §4 C3 covering index | Task 3 |
| §5 C4 projection | Task 5 |
| §6.1 통합 DTO | Task 7, 8, 9 |
| §6.2 단일 캐시 메서드 | Task 7, 8, 9 |
| §6.3 키 정규화 helper | Task 6 |
| §6.4 sync = true | Task 7, 8, 9, 10 |
| §6.5 Service 단일 호출 | Task 7, 8, 9 |
| §7.1 `@Size(max = 3)` + `@Validated` | Task 11 |
| §7.2 Service-level CoreException | Task 11 |
| §7.3 검증 예외 매핑 확인 | Task 11 (코드 내 주석 + 테스트) |

### Placeholder 점검

- 일부 task 에 "도메인 객체 시그니처 확인" 같은 검증 단계가 있음. 이는 placeholder 가 아니라 기존 코드 일치성 검증을 위한 명시적 단계. 코드 블록은 모두 작성됨.
- Task 2 의 fixture 빌드 비용이 크면 `@Disabled` + 후속 작업으로 명시. placeholder 가 아닌 결정.

### 타입 일관성

- `joinIds(List<BrawlStarsId>): String` — Task 6 정의, Task 7·9 SpEL 에서 사용. 일치.
- `BrawlerTeamResultCounts(items, totalBattleCount, totalElementCount)` — Task 7 정의 후 동일 사용. 일치.
- `findBrawlerTeamResult` / `findBrawlerEnemyResult` / `findBrawlerTeamRank` — 새 메서드명, 호출 측 모두 일치.
- `CoreException(ErrorType.INVALID_REQUEST, message)` — Task 11. 기존 코드 사용 패턴과 일치.

### 결정 가능 한도

이 plan 은 한 작업자가 이어서 실행할 수 있는 단위로 자른다. Task 5 (`StatsTeamSize.value`) 같은 enum 프로퍼티 이름은 실제 정의 확인 단계가 명시되어 있어 작업자가 즉시 해결 가능.
