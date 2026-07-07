# core-batch 성능·구조 개선 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** core-batch 통계 잡의 INSERT 배칭 복구(IDENTITY→SEQUENCE), ownership 리더 쿼리 안전화, offset→keyset 페이징 전환과 함께 데드코드(trio 포함)를 정리한다.

**Architecture:** 스펙 [2026-07-07-core-batch-performance-structure-design.md](../specs/2026-07-07-core-batch-performance-structure-design.md) 승인 완료. 통계 수직(Reader/Task/Writer 클론)은 "중복 > 추상화" 정책에 따라 **그대로 유지**하며, 유일한 신규 추상화는 support 유틸 `KeysetItemReader`(기존 offset 리더 3종 대체) 하나다. PR 3개 순서로 커밋을 쌓는다: PR1(무위험 정리) → PR2(keyset·ownership 쿼리) → PR3(SEQUENCE 전환 + DDL).

**Tech Stack:** Kotlin 2.2 / Spring Boot 4.0.2 / Spring Batch 6.0.2 / QueryDSL 7.1(KSP) / PostgreSQL. 테스트: JUnit5 + AssertJ + MockK, 컨텍스트 테스트는 `CoreBatchContextTest` 상속(내부 Testcontainers, `ddl-auto: create`).

---

## 실행 공통 규칙

- **작업 디렉토리**: 모든 명령은 `imstargg-backend/` 에서 실행한다 (gradle 프로젝트 루트).
- **브랜치/PR**: 커밋은 PR1→PR2→PR3 순서로 쌓는다. 각 PR 체크포인트에서 그때까지의 커밋으로 `develop` 대상 PR을 만들 수 있다 (stacked). PR 생성 시 리모트가 SSH 별칭이므로 `-R no-commit-today/imstargg-v2` 를 명시하고 Cho-D-YoungRae 계정을 사용한다. PR 생성 여부·시점은 사용자 확인 후 진행.
- **테스트 태그**: 컨텍스트 테스트는 `CoreBatchContextTest`/`DbCoreContextTest` 상속으로 충분 — **서브클래스에 `@Tag("context")` 를 중복 선언하지 않는다** (베이스 클래스에 이미 있음).
- **커밋 전**: 해당 모듈 `ktlintFormat` 실행 후 커밋. 예: `./gradlew :core:core-batch:ktlintFormat`
- **예상 출력**: gradle 명령의 성공 기준은 `BUILD SUCCESSFUL` 출력이다. 실패 시 그 태스크에서 멈추고 원인을 해결한다.

---

# PR1 — 무위험 정리 (동작 무관)

### Task 1: trio 휴면 테이블·엔티티 제거

**Files:**
- Delete: `infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/BrawlerTrioBattleResultStatsEntity.kt`
- Delete: `infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/BrawlerTrioBattleRankStatsEntity.kt`
- Modify: `infrastructure/db-core/src/main/resources/schema.sql:479-536`
- Create: `infrastructure/db-core/src/main/resources/db/migration/V260707__drop_trio_stats_tables.sql`

- [ ] **Step 1: 엔티티 2종 삭제**

```bash
git rm infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/BrawlerTrioBattleResultStatsEntity.kt \
       infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/BrawlerTrioBattleRankStatsEntity.kt
```

- [ ] **Step 2: schema.sql 에서 trio 블록 2개 제거**

`schema.sql` 479~536 라인의 다음 두 블록(주석 줄부터 인덱스 생성문까지, 사이 빈 줄 포함)을 통째로 삭제한다:

```sql
-- 브롤러 trio 전투 결과 통계 (현재 API/배치 미제공)
create table if not exists brawler_trio_battle_result_stats
(
    ... (constraint ck_brawler_trio_battle_result_stats_distinct_brawler 포함)
);
create unique index if not exists uk_brawler_trio_battle_result_stats__1
    on brawler_trio_battle_result_stats (
        event_brawlstars_id, start_battle_date, end_battle_date, tier_range,
        brawler_brawlstars_id, pair_brawler1_brawlstars_id, pair_brawler2_brawlstars_id
    );

-- 브롤러 trio 전투 랭크 통계 (현재 API/배치 미제공)
create table if not exists brawler_trio_battle_rank_stats
(
    ... (constraint ck_brawler_trio_battle_rank_stats_distinct_brawler 포함)
);
create unique index if not exists uk_brawler_trio_battle_rank_stats__1
    on brawler_trio_battle_rank_stats (
        event_brawlstars_id, start_battle_date, end_battle_date, tier_range,
        brawler_brawlstars_id, pair_brawler1_brawlstars_id, pair_brawler2_brawlstars_id
    );
```

삭제 후 확인: `grep -n "trio" infrastructure/db-core/src/main/resources/schema.sql` → 출력 없음.

- [ ] **Step 3: drop 마이그레이션 파일 생성**

`infrastructure/db-core/src/main/resources/db/migration/V260707__drop_trio_stats_tables.sql` (실제 반영일이 다르면 파일명 날짜를 그날로 조정):

```sql
-- 휴면 trio 통계 테이블 제거 (API/배치 미제공 상태로 사용처 없음 — 2026-07-07 설계서 참조)
-- 적용 전 확인: 두 테이블 모두 row count 0 이어야 한다.
--   select count(*) from brawler_trio_battle_result_stats;
--   select count(*) from brawler_trio_battle_rank_stats;
drop table if exists brawler_trio_battle_result_stats;
drop table if exists brawler_trio_battle_rank_stats;
```

- [ ] **Step 4: 컴파일 확인**

Run: `./gradlew :infrastructure:db-core:compileKotlin :infrastructure:db-core:compileTestKotlin`
Expected: `BUILD SUCCESSFUL` (Q-Class 재생성 포함. trio 참조가 남아 있으면 여기서 컴파일 에러)

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "refactor(db-core): 휴면 trio 통계 테이블·엔티티 제거"
```

---

### Task 2: 미사용 PlayerCombinator 제거

**Files:**
- Delete: `core/core-batch/src/main/kotlin/com/imstargg/core/batch/domain/statistics/PlayerCombinator.kt`
- Delete: `core/core-batch/src/test/kotlin/com/imstargg/core/batch/domain/statistics/PlayerCombinatorTest.kt`

- [ ] **Step 1: 파일 삭제**

```bash
git rm core/core-batch/src/main/kotlin/com/imstargg/core/batch/domain/statistics/PlayerCombinator.kt \
       core/core-batch/src/test/kotlin/com/imstargg/core/batch/domain/statistics/PlayerCombinatorTest.kt
```

- [ ] **Step 2: 참조 없음 확인 + 컴파일**

Run: `grep -rn "PlayerCombinator" core/ --include="*.kt"; ./gradlew :core:core-batch:compileKotlin :core:core-batch:compileTestKotlin`
Expected: grep 출력 없음, `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git commit -am "refactor(core-batch): 미사용 PlayerCombinator 제거"
```

---

### Task 3: WeekRange 파일 분리 (core-enum)

**Files:**
- Create: `core/core-enum/src/main/kotlin/com/imstargg/core/enums/WeekRange.kt`
- Modify: `core/core-enum/src/main/kotlin/com/imstargg/core/enums/StatsWeekRange.kt`

- [ ] **Step 1: WeekRange.kt 신규 생성**

```kotlin
package com.imstargg.core.enums

import java.time.LocalDate

data class WeekRange(
    val start: LocalDate,
    val end: LocalDate,
)
```

- [ ] **Step 2: StatsWeekRange.kt 에서 data class 제거**

`StatsWeekRange.kt` 상단의 다음 선언을 삭제한다 (object StatsWeekRange 는 그대로):

```kotlin
data class WeekRange(
    val start: LocalDate,
    val end: LocalDate,
)
```

- [ ] **Step 3: 컴파일 확인 (core-enum + 사용 모듈)**

Run: `./gradlew :core:core-enum:build :core:core-batch:compileKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "refactor(core-enum): WeekRange 를 별도 파일로 분리"
```

---

### Task 4: 파티션 보존 ↔ 주간 룩백 가드 (TDD)

**Files:**
- Test: `core/core-batch/src/test/kotlin/com/imstargg/core/batch/job/PartitionRetentionLookbackTest.kt` (Create)
- Modify: `core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/PartitionManageJobConfig.kt:33`

- [ ] **Step 1: 실패하는 테스트 작성**

```kotlin
package com.imstargg.core.batch.job

import com.imstargg.core.enums.StatsWeekRange
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class PartitionRetentionLookbackTest {
    /**
     * 주간 통계 잡(pair/enemy)은 date 파라미터(D = 실행일 T-1) 기준
     * [StatsWeekRange.currentAndPrevious] 범위의 원본 배틀 파티션을 재집계한다.
     * 파티션 삭제 잡은 실행일 T 기준 [PartitionManageJobConfig.RETENTION_DAYS] 이전을 드롭하므로,
     * 이 관계가 깨지면 주간 통계가 드롭된 파티션 구간만큼 조용히 불완전해진다.
     */
    @Test
    fun `주간 통계 최대 룩백이 파티션 보존기간 안에 있다`() {
        // given - 월 경계·연 경계를 모두 포함하는 2년 치 기준일
        val start = LocalDate.of(2026, 1, 1)
        val dates = generateSequence(start) { it.plusDays(1) }.takeWhile { it <= start.plusYears(2) }

        // when
        val maxLookbackFromDate =
            dates.maxOf { date ->
                val earliestStart = StatsWeekRange.currentAndPrevious(date).minOf { it.start }
                ChronoUnit.DAYS.between(earliestStart, date)
            }

        // then - 실행일 보정(+1일) + 안전 여유(+1일)까지 보존기간이 커버해야 한다
        val executionDateOffset = 1L
        val safetyMarginDays = 1L
        assertThat(maxLookbackFromDate + executionDateOffset + safetyMarginDays)
            .isLessThanOrEqualTo(PartitionManageJobConfig.RETENTION_DAYS.toLong())
    }
}
```

- [ ] **Step 2: 실패 확인**

Run: `./gradlew :core:core-batch:compileTestKotlin`
Expected: FAIL — `cannot access 'RETENTION_DAYS': it is private` 류의 컴파일 에러

- [ ] **Step 3: RETENTION_DAYS 공개 + KDoc 상호 참조**

`PartitionManageJobConfig.kt` companion object 의 `private const val RETENTION_DAYS = 10` 을 다음으로 교체:

```kotlin
        /**
         * 원본 배틀(stats_*_battle) 파티션 보존 일수.
         * 주간 통계 잡의 최대 룩백([com.imstargg.core.enums.StatsWeekRange.currentAndPrevious] 기준
         * 최대 7일 + 실행일 보정 1일)을 커버해야 한다 — 가드: PartitionRetentionLookbackTest
         */
        const val RETENTION_DAYS = 10
```

`core/core-enum/.../StatsWeekRange.kt` 의 `currentAndPrevious` 함수 위에 KDoc 추가:

```kotlin
    /**
     * 주간 통계 잡이 재집계할 주간 범위(현재 주 + 직전일이 속한 주).
     * 최대 룩백은 기준일로부터 7일 — 원본 배틀 파티션 보존기간
     * (core-batch PartitionManageJobConfig.RETENTION_DAYS)이 이를 커버해야 한다.
     */
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `./gradlew :core:core-batch:unitTest --tests "com.imstargg.core.batch.job.PartitionRetentionLookbackTest"`
Expected: PASS (룩백 7 + 1 + 1 = 9 ≤ 10)

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "test(core-batch): 파티션 보존기간이 주간 통계 룩백을 커버함을 가드"
```

---

### Task 5: ownership 집계 도메인을 domain 패키지로 이동

**Files:**
- Move(6): `core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/ownership/{BrawlerItemOwnershipStatsAccumulator,BrawlerItemOwnershipStatsResult,PlayerBrawlerOwnershipSnapshot,BrawlerTrophyKey,BrawlerTrophyItemKey,BrawlerTrophyBuffyKey}.kt` → `core/core-batch/src/main/kotlin/com/imstargg/core/batch/domain/statistics/ownership/`
- Move(1): `core/core-batch/src/test/kotlin/com/imstargg/core/batch/job/statistics/ownership/BrawlerItemOwnershipStatsAccumulatorTest.kt` → `core/core-batch/src/test/kotlin/com/imstargg/core/batch/domain/statistics/ownership/`
- Modify: `job/statistics/ownership/` 의 `BrawlerItemOwnershipStatsReader.kt`, `BrawlerItemOwnershipStatsTask.kt`, `BrawlerItemOwnershipStatsWriter.kt` 와 test 의 `BrawlerItemOwnershipStatsTaskTest.kt`, `BrawlerItemOwnershipStatsWriterTest.kt` (import 추가)

- [ ] **Step 1: 파일 이동**

```bash
mkdir -p core/core-batch/src/main/kotlin/com/imstargg/core/batch/domain/statistics/ownership \
         core/core-batch/src/test/kotlin/com/imstargg/core/batch/domain/statistics/ownership
cd core/core-batch/src/main/kotlin/com/imstargg/core/batch
git mv job/statistics/ownership/BrawlerItemOwnershipStatsAccumulator.kt domain/statistics/ownership/
git mv job/statistics/ownership/BrawlerItemOwnershipStatsResult.kt domain/statistics/ownership/
git mv job/statistics/ownership/PlayerBrawlerOwnershipSnapshot.kt domain/statistics/ownership/
git mv job/statistics/ownership/BrawlerTrophyKey.kt domain/statistics/ownership/
git mv job/statistics/ownership/BrawlerTrophyItemKey.kt domain/statistics/ownership/
git mv job/statistics/ownership/BrawlerTrophyBuffyKey.kt domain/statistics/ownership/
cd - && cd core/core-batch/src/test/kotlin/com/imstargg/core/batch
git mv job/statistics/ownership/BrawlerItemOwnershipStatsAccumulatorTest.kt domain/statistics/ownership/
cd -
```

- [ ] **Step 2: 이동한 7개 파일의 package 선언 변경**

7개 파일 모두 첫 줄을 다음으로 교체:

```kotlin
package com.imstargg.core.batch.domain.statistics.ownership
```

(기존: `package com.imstargg.core.batch.job.statistics.ownership`)

- [ ] **Step 3: job 패키지에 남은 파일들에 import 추가**

`job/statistics/ownership/BrawlerItemOwnershipStatsReader.kt` 의 import 블록에 추가:

```kotlin
import com.imstargg.core.batch.domain.statistics.ownership.PlayerBrawlerOwnershipSnapshot
```

`job/statistics/ownership/BrawlerItemOwnershipStatsTask.kt` 에 추가:

```kotlin
import com.imstargg.core.batch.domain.statistics.ownership.BrawlerItemOwnershipStatsAccumulator
```

`job/statistics/ownership/BrawlerItemOwnershipStatsWriter.kt` 에 추가:

```kotlin
import com.imstargg.core.batch.domain.statistics.ownership.BrawlerItemOwnershipStatsResult
```

test `job/statistics/ownership/BrawlerItemOwnershipStatsTaskTest.kt` 에 추가 (셋 중 미사용은 Step 5 ktlintFormat 이 제거):

```kotlin
import com.imstargg.core.batch.domain.statistics.ownership.BrawlerItemOwnershipStatsAccumulator
import com.imstargg.core.batch.domain.statistics.ownership.BrawlerItemOwnershipStatsResult
import com.imstargg.core.batch.domain.statistics.ownership.PlayerBrawlerOwnershipSnapshot
```

test `job/statistics/ownership/BrawlerItemOwnershipStatsWriterTest.kt` 에 추가:

```kotlin
import com.imstargg.core.batch.domain.statistics.ownership.BrawlerItemOwnershipStatsResult
```

- [ ] **Step 4: 컴파일 + 단위 테스트**

Run: `./gradlew :core:core-batch:ktlintFormat :core:core-batch:unitTest`
Expected: `BUILD SUCCESSFUL`, ownership 3개 테스트(Accumulator/Task/Writer) 포함 전체 PASS

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "refactor(core-batch): ownership 집계 도메인을 domain/statistics 로 이동"
```

---

### ✅ PR1 체크포인트

- [ ] Run: `./gradlew ktlintCheck :core:core-enum:build :infrastructure:db-core:test :core:core-batch:test`
- Expected: `BUILD SUCCESSFUL` (전 모듈 lint + db-core/core-batch 테스트 통과)
- 여기까지 커밋으로 `develop` 대상 PR1 생성 가능 (제목 예: `refactor: core-batch 데드코드·trio 테이블 정리 및 도메인 정렬`). 사용자 확인 후 진행.

---

# PR2 — keyset 전환 · ownership 쿼리 안전화

### Task 6: KeysetItemReader (TDD)

**Files:**
- Create: `core/core-batch/src/main/kotlin/com/imstargg/core/batch/support/querydsl/KeysetItemReader.kt`
- Test: `core/core-batch/src/test/kotlin/com/imstargg/core/batch/support/querydsl/KeysetItemReaderTest.kt` (Create)

- [ ] **Step 1: 실패하는 컨텍스트 테스트 작성**

```kotlin
package com.imstargg.core.batch.support.querydsl

import com.imstargg.CoreBatchContextTest
import com.imstargg.infrastructure.db.core.statistics.battle.QStatsPlayerEntity.Companion.statsPlayerEntity
import com.imstargg.infrastructure.db.core.statistics.battle.StatsPlayerEntity
import com.imstargg.infrastructure.db.core.statistics.battle.repository.StatsPlayerJpaRepository
import com.imstargg.infrastructure.db.core.test.CleanUp
import jakarta.persistence.EntityManagerFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.batch.infrastructure.item.ExecutionContext

class KeysetItemReaderTest(
    private val entityManagerFactory: EntityManagerFactory,
    private val statsPlayerJpaRepository: StatsPlayerJpaRepository,
    private val cleanUp: CleanUp,
) : CoreBatchContextTest() {
    @AfterEach
    fun tearDown() {
        cleanUp.all()
    }

    @Test
    fun `키셋으로 전체 행을 id 오름차순으로 읽는다`() {
        // given - 페이지 크기(7)로 나누어떨어지지 않는 25건
        statsPlayerJpaRepository.saveAll((1..25).map { StatsPlayerEntity(brawlStarsTag = "#TAG$it") })
        val sut = keysetItemReader()

        // when
        sut.open(ExecutionContext())
        val read = generateSequence { sut.read() }.toList()
        sut.close()

        // then
        assertThat(read).hasSize(25)
        assertThat(read.map { it.id!! }).isSorted()
        assertThat(read.map { it.brawlStarsTag }).containsExactlyInAnyOrderElementsOf((1..25).map { "#TAG$it" })
    }

    @Test
    fun `ExecutionContext 로 재시작하면 마지막으로 읽은 항목 다음부터 읽는다`() {
        // given - 20건 저장 후 5건만 읽고 상태 저장
        statsPlayerJpaRepository.saveAll((1..20).map { StatsPlayerEntity(brawlStarsTag = "#TAG$it") })
        val executionContext = ExecutionContext()
        val first = keysetItemReader()
        first.open(executionContext)
        val firstRead = (1..5).map { first.read()!! }
        first.update(executionContext)
        first.close()

        // when - 같은 ExecutionContext 로 새 리더 open
        val second = keysetItemReader()
        second.open(executionContext)
        val secondRead = generateSequence { second.read() }.toList()
        second.close()

        // then - 중복·누락 없이 이어 읽는다
        assertThat(secondRead).hasSize(15)
        assertThat(firstRead.map { it.id!! } + secondRead.map { it.id!! })
            .isSorted()
            .doesNotHaveDuplicates()
    }

    private fun keysetItemReader(): KeysetItemReader<StatsPlayerEntity> =
        KeysetItemReader(
            name = "keysetItemReaderTest",
            entityManagerFactory = entityManagerFactory,
            pageSize = 7,
            idPath = statsPlayerEntity.id,
            idOf = { it.id!! },
        ) { queryFactory -> queryFactory.selectFrom(statsPlayerEntity) }
}
```

- [ ] **Step 2: 실패 확인**

Run: `./gradlew :core:core-batch:compileTestKotlin`
Expected: FAIL — `unresolved reference 'KeysetItemReader'` 컴파일 에러

- [ ] **Step 3: KeysetItemReader 구현**

`core/core-batch/src/main/kotlin/com/imstargg/core/batch/support/querydsl/KeysetItemReader.kt`:

```kotlin
package com.imstargg.core.batch.support.querydsl

import com.querydsl.core.types.dsl.NumberPath
import com.querydsl.jpa.impl.JPAQuery
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import org.springframework.batch.infrastructure.item.ExecutionContext
import org.springframework.batch.infrastructure.item.ItemStreamException
import org.springframework.batch.infrastructure.item.ItemStreamReader

/**
 * 키셋(`id > lastId`) 페이징 ItemStreamReader.
 *
 * offset 페이징(AbstractPagingItemReader)은 뒤 페이지로 갈수록 스킵 비용이 누적되지만
 * 키셋은 페이지 위치와 무관하게 일정하다. 키셋 조건(`id > lastId` + `order by id asc` + `limit`)은
 * 리더가 직접 주입한다 — 호출자는 from/where 만 담은 base 쿼리를 제공하므로
 * 키셋 조건 누락 실수가 원천 차단된다.
 *
 * read() 가 마지막으로 반환한 항목의 id 를 ExecutionContext 에 저장해
 * 스텝 재시작 시 커밋된 지점 다음부터 이어 읽는다.
 *
 * `@StepScope` 빈으로 등록되어 CGLIB 스코프 프록시 대상이므로 open class 여야 한다
 * (StatsPlayerSyncJobItemWriter 가 open 인 것과 같은 이유).
 */
open class KeysetItemReader<T : Any>(
    name: String,
    private val entityManagerFactory: EntityManagerFactory,
    private val pageSize: Long,
    private val idPath: NumberPath<Long>,
    private val idOf: (T) -> Long,
    private val queryFunction: (JPAQueryFactory) -> JPAQuery<T>,
) : ItemStreamReader<T> {
    private val lastReadIdKey = "$name.lastReadId"

    private var entityManager: EntityManager? = null
    private var lastReadId = 0L
    private var lastFetchedId = 0L
    private var buffer: List<T> = emptyList()
    private var bufferIndex = 0
    private var exhausted = false

    override fun open(executionContext: ExecutionContext) {
        entityManager = entityManagerFactory.createEntityManager()
        lastReadId = executionContext.getLong(lastReadIdKey, 0L)
        lastFetchedId = lastReadId
        buffer = emptyList()
        bufferIndex = 0
        exhausted = false
    }

    override fun read(): T? {
        if (bufferIndex >= buffer.size) {
            if (exhausted) return null
            fetchNextPage()
            if (buffer.isEmpty()) return null
        }
        val item = buffer[bufferIndex++]
        lastReadId = idOf(item)
        return item
    }

    private fun fetchNextPage() {
        val em = entityManager ?: throw ItemStreamException("리더가 open 되지 않았습니다.")
        em.clear()
        buffer =
            queryFunction(JPAQueryFactory(em))
                .where(idPath.gt(lastFetchedId))
                .orderBy(idPath.asc())
                .limit(pageSize)
                .fetch()
        bufferIndex = 0
        if (buffer.isEmpty()) {
            exhausted = true
        } else {
            lastFetchedId = idOf(buffer.last())
        }
    }

    override fun update(executionContext: ExecutionContext) {
        executionContext.putLong(lastReadIdKey, lastReadId)
    }

    override fun close() {
        entityManager?.close()
        entityManager = null
    }
}
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `./gradlew :core:core-batch:contextTest --tests "com.imstargg.core.batch.support.querydsl.KeysetItemReaderTest"`
Expected: PASS (2개 테스트)

- [ ] **Step 5: Commit**

```bash
./gradlew :core:core-batch:ktlintFormat
git add -A
git commit -m "feat(core-batch): 키셋 페이징 KeysetItemReader 추가"
```

---

### Task 7: battleEventSyncJob 리더 교체

**Files:**
- Modify: `core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/brawlstars/BattleEventSyncJobConfig.kt:78-100`

- [ ] **Step 1: 리더 빈 2개 교체**

import 변경: `import com.imstargg.core.batch.support.querydsl.QuerydslPagingItemReader` 를 삭제하고 다음 추가:

```kotlin
import com.imstargg.core.batch.support.querydsl.KeysetItemReader
```

기존 두 빈을 다음으로 교체:

```kotlin
    @Bean
    @StepScope
    fun battleEventSyncRankBattleReader(): KeysetItemReader<StatsRankBattleEntity> {
        val rankBattle = QStatsRankBattleEntity.statsRankBattleEntity
        return KeysetItemReader(
            name = "battleEventSyncRankBattleReader",
            entityManagerFactory = entityManagerFactory,
            pageSize = CHUNK_SIZE.toLong(),
            idPath = rankBattle.id,
            idOf = { it.id!! },
        ) { queryFactory ->
            queryFactory
                .selectFrom(rankBattle)
                .where(rankBattle.battleDate.eq(dateJobParameter().date))
        }
    }

    @Bean
    @StepScope
    fun battleEventSyncResultBattleReader(): KeysetItemReader<StatsResultBattleEntity> {
        val resultBattle = QStatsResultBattleEntity.statsResultBattleEntity
        return KeysetItemReader(
            name = "battleEventSyncResultBattleReader",
            entityManagerFactory = entityManagerFactory,
            pageSize = CHUNK_SIZE.toLong(),
            idPath = resultBattle.id,
            idOf = { it.id!! },
        ) { queryFactory ->
            queryFactory
                .selectFrom(resultBattle)
                .where(resultBattle.battleDate.eq(dateJobParameter().date))
        }
    }
```

(키셋 조건과 `orderBy(id.asc())` 는 `KeysetItemReader` 가 주입하므로 쿼리에서 제거된 것이 맞다.)

- [ ] **Step 2: 컨텍스트 로드 확인**

Run: `./gradlew :core:core-batch:contextTest --tests "com.imstargg.CoreBatchApplicationTest"`
Expected: PASS (빈 배선 검증)

- [ ] **Step 3: Commit**

```bash
./gradlew :core:core-batch:ktlintFormat
git add -A
git commit -m "refactor(core-batch): battleEventSyncJob 리더를 키셋 페이징으로 전환"
```

---

### Task 8: statsPlayerSyncJob 리더 교체

**Files:**
- Modify: `core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/playersync/StatsPlayerSyncJobConfig.kt:105-127`

- [ ] **Step 1: 리더 빈 2개 교체**

import 변경: `import com.imstargg.core.batch.support.querydsl.QuerydslPagingItemReader` 삭제, 다음 추가:

```kotlin
import com.imstargg.core.batch.support.querydsl.KeysetItemReader
```

기존 두 빈을 다음으로 교체:

```kotlin
    @Bean
    @StepScope
    fun statsRankBattleReader(): KeysetItemReader<StatsRankBattleEntity> {
        val rankBattle = QStatsRankBattleEntity.statsRankBattleEntity
        return KeysetItemReader(
            name = "statsPlayerSyncRankBattleReader",
            entityManagerFactory = entityManagerFactory,
            pageSize = CHUNK_SIZE.toLong(),
            idPath = rankBattle.id,
            idOf = { it.id!! },
        ) { queryFactory ->
            queryFactory
                .selectFrom(rankBattle)
                .where(rankBattle.battleDate.eq(dateJobParameter().date))
        }
    }

    @Bean
    @StepScope
    fun statsResultBattleReader(): KeysetItemReader<StatsResultBattleEntity> {
        val resultBattle = QStatsResultBattleEntity.statsResultBattleEntity
        return KeysetItemReader(
            name = "statsPlayerSyncResultBattleReader",
            entityManagerFactory = entityManagerFactory,
            pageSize = CHUNK_SIZE.toLong(),
            idPath = resultBattle.id,
            idOf = { it.id!! },
        ) { queryFactory ->
            queryFactory
                .selectFrom(resultBattle)
                .where(resultBattle.battleDate.eq(dateJobParameter().date))
        }
    }
```

- [ ] **Step 2: 컨텍스트 로드 확인**

Run: `./gradlew :core:core-batch:contextTest --tests "com.imstargg.CoreBatchApplicationTest"`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
./gradlew :core:core-batch:ktlintFormat
git add -A
git commit -m "refactor(core-batch): statsPlayerSyncJob 리더를 키셋 페이징으로 전환"
```

---

### Task 9: ownership 리더 쿼리 안전화 (특성화 테스트 → 개선)

**Files:**
- Test: `core/core-batch/src/test/kotlin/com/imstargg/core/batch/job/statistics/ownership/BrawlerItemOwnershipStatsReaderTest.kt` (Create)
- Modify: `core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/ownership/BrawlerItemOwnershipStatsReader.kt` (전면 교체)

- [ ] **Step 1: 현재 동작을 고정하는 특성화 테스트 작성 (기존 구현으로 먼저 통과시킨다)**

```kotlin
package com.imstargg.core.batch.job.statistics.ownership

import com.imstargg.CoreBatchContextTest
import com.imstargg.core.batch.domain.statistics.ownership.PlayerBrawlerOwnershipSnapshot
import com.imstargg.core.enums.BuffyType
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.PlayerBrawlerBuffyJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.PlayerBrawlerGadgetJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.PlayerBrawlerGearJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.PlayerBrawlerHyperChargeJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.PlayerBrawlerJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.PlayerBrawlerStarPowerJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.PlayerJpaRepository
import com.imstargg.infrastructure.db.core.statistics.battle.StatsPlayerEntity
import com.imstargg.infrastructure.db.core.statistics.battle.repository.StatsPlayerJpaRepository
import com.imstargg.infrastructure.db.core.test.CleanUp
import com.imstargg.infrastructure.db.core.test.fixture.PlayerBrawlerBuffyEntityFixture.Companion.aPlayerBrawlerBuffyEntity
import com.imstargg.infrastructure.db.core.test.fixture.PlayerBrawlerEntityFixture.Companion.aPlayerBrawlerEntity
import com.imstargg.infrastructure.db.core.test.fixture.PlayerBrawlerGadgetEntityFixture.Companion.aPlayerBrawlerGadgetEntity
import com.imstargg.infrastructure.db.core.test.fixture.PlayerBrawlerGearEntityFixture.Companion.aPlayerBrawlerGearEntity
import com.imstargg.infrastructure.db.core.test.fixture.PlayerBrawlerHyperChargeEntityFixture.Companion.aPlayerBrawlerHyperChargeEntity
import com.imstargg.infrastructure.db.core.test.fixture.PlayerBrawlerStarPowerEntityFixture.Companion.aPlayerBrawlerStarPowerEntity
import com.imstargg.infrastructure.db.core.test.fixture.PlayerEntityFixture.Companion.aPlayerEntity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class BrawlerItemOwnershipStatsReaderTest(
    private val sut: BrawlerItemOwnershipStatsReader,
    private val statsPlayerJpaRepository: StatsPlayerJpaRepository,
    private val playerJpaRepository: PlayerJpaRepository,
    private val playerBrawlerJpaRepository: PlayerBrawlerJpaRepository,
    private val playerBrawlerGearJpaRepository: PlayerBrawlerGearJpaRepository,
    private val playerBrawlerGadgetJpaRepository: PlayerBrawlerGadgetJpaRepository,
    private val playerBrawlerStarPowerJpaRepository: PlayerBrawlerStarPowerJpaRepository,
    private val playerBrawlerHyperChargeJpaRepository: PlayerBrawlerHyperChargeJpaRepository,
    private val playerBrawlerBuffyJpaRepository: PlayerBrawlerBuffyJpaRepository,
    private val cleanUp: CleanUp,
) : CoreBatchContextTest() {
    @AfterEach
    fun tearDown() {
        cleanUp.all()
    }

    @Test
    fun `stats_player 에 등록된 플레이어의 브롤러별 아이템 소유 스냅샷을 읽는다`() {
        // given - stats_player 에 등록된 P1 (브롤러 A: 아이템 5종, 브롤러 B: 아이템 없음)
        statsPlayerJpaRepository.save(StatsPlayerEntity(brawlStarsTag = "#P1"))
        val player1 = playerJpaRepository.save(aPlayerEntity().withBrawlStarsTag("#P1").build())
        val brawlerA =
            playerBrawlerJpaRepository.save(
                aPlayerBrawlerEntity().withPlayer(player1).withBrawlerBrawlStarsId(16000001L).withTrophies(600).build(),
            )
        playerBrawlerJpaRepository.save(
            aPlayerBrawlerEntity().withPlayer(player1).withBrawlerBrawlStarsId(16000002L).withTrophies(100).build(),
        )
        playerBrawlerGearJpaRepository.save(
            aPlayerBrawlerGearEntity().withPlayerBrawler(brawlerA).withGearBrawlStarsId(62000001L).build(),
        )
        playerBrawlerGadgetJpaRepository.save(
            aPlayerBrawlerGadgetEntity().withPlayerBrawler(brawlerA).withGadgetBrawlStarsId(23000001L).build(),
        )
        playerBrawlerStarPowerJpaRepository.save(
            aPlayerBrawlerStarPowerEntity().withPlayerBrawler(brawlerA).withStarPowerBrawlStarsId(23000101L).build(),
        )
        playerBrawlerHyperChargeJpaRepository.save(
            aPlayerBrawlerHyperChargeEntity().withPlayerBrawler(brawlerA).withHyperChargeBrawlStarsId(23000201L).build(),
        )
        playerBrawlerBuffyJpaRepository.save(
            aPlayerBrawlerBuffyEntity().withPlayerBrawler(brawlerA).withBuffyType(BuffyType.entries.first()).build(),
        )

        // stats_player 미등록 P2 는 결과에서 제외되어야 한다
        val player2 = playerJpaRepository.save(aPlayerEntity().withBrawlStarsTag("#P2").build())
        playerBrawlerJpaRepository.save(
            aPlayerBrawlerEntity().withPlayer(player2).withBrawlerBrawlStarsId(16000003L).build(),
        )

        // when
        val snapshots = mutableListOf<PlayerBrawlerOwnershipSnapshot>()
        val totalRead = sut.read { snapshots += it }

        // then - totalRead 는 stats_player 수
        assertThat(totalRead).isEqualTo(1L)
        assertThat(snapshots).hasSize(2)
        assertThat(snapshots.map { it.brawlerBrawlStarsId })
            .containsExactlyInAnyOrder(16000001L, 16000002L)

        val snapshotA = snapshots.first { it.brawlerBrawlStarsId == 16000001L }
        assertThat(snapshotA.playerBrawlerId).isEqualTo(brawlerA.id)
        assertThat(snapshotA.trophies).isEqualTo(600)
        assertThat(snapshotA.gearBrawlStarsIds).containsExactly(62000001L)
        assertThat(snapshotA.gadgetBrawlStarsIds).containsExactly(23000001L)
        assertThat(snapshotA.starPowerBrawlStarsIds).containsExactly(23000101L)
        assertThat(snapshotA.hyperChargeBrawlStarsIds).containsExactly(23000201L)
        assertThat(snapshotA.buffyTypes).containsExactly(BuffyType.entries.first())

        val snapshotB = snapshots.first { it.brawlerBrawlStarsId == 16000002L }
        assertThat(snapshotB.gearBrawlStarsIds).isEmpty()
        assertThat(snapshotB.buffyTypes).isEmpty()
    }
}
```

(픽스처 7종의 패키지 `com.imstargg.infrastructure.db.core.test.fixture` 와 위 with-메서드·`build()` 시그니처는 계획 작성 시점에 원본 파일로 검증 완료.)

- [ ] **Step 2: 기존 구현으로 통과 확인 (특성화 기준선)**

Run: `./gradlew :core:core-batch:contextTest --tests "com.imstargg.core.batch.job.statistics.ownership.BrawlerItemOwnershipStatsReaderTest"`
Expected: PASS — 이 테스트가 리팩터링의 동작 보존 기준선이 된다

- [ ] **Step 3: Reader 를 tag join + 프로젝션으로 전면 교체**

`BrawlerItemOwnershipStatsReader.kt` 전체를 다음으로 교체:

```kotlin
package com.imstargg.core.batch.job.statistics.ownership

import com.imstargg.core.batch.domain.statistics.ownership.PlayerBrawlerOwnershipSnapshot
import com.imstargg.infrastructure.db.core.brawlstars.player.QPlayerBrawlerBuffyEntity.Companion.playerBrawlerBuffyEntity
import com.imstargg.infrastructure.db.core.brawlstars.player.QPlayerBrawlerEntity.Companion.playerBrawlerEntity
import com.imstargg.infrastructure.db.core.brawlstars.player.QPlayerBrawlerGadgetEntity.Companion.playerBrawlerGadgetEntity
import com.imstargg.infrastructure.db.core.brawlstars.player.QPlayerBrawlerGearEntity.Companion.playerBrawlerGearEntity
import com.imstargg.infrastructure.db.core.brawlstars.player.QPlayerBrawlerHyperChargeEntity.Companion.playerBrawlerHyperChargeEntity
import com.imstargg.infrastructure.db.core.brawlstars.player.QPlayerBrawlerStarPowerEntity.Companion.playerBrawlerStarPowerEntity
import com.imstargg.infrastructure.db.core.brawlstars.player.QPlayerEntity.Companion.playerEntity
import com.imstargg.infrastructure.db.core.statistics.battle.QStatsPlayerEntity.Companion.statsPlayerEntity
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManagerFactory
import org.springframework.stereotype.Component

/**
 * stats_player 를 키셋으로 순회하며, 페이지의 플레이어 태그(최대 [PAGE_SIZE]개)를 기준으로
 * player_brawler 와 아이템 5종을 조회해 소유 스냅샷을 만든다.
 *
 * 아이템 조회는 player_brawler id 목록(IN, 페이지당 최대 수만 개)이 아니라
 * 플레이어 태그 join(IN 크기 = 페이지 크기 고정) 으로 필터한다.
 * 아이템 5종의 조회 반복은 의도된 명시성이다 (2026-05-22 설계 참조).
 */
@Component
class BrawlerItemOwnershipStatsReader(
    private val entityManagerFactory: EntityManagerFactory,
) {
    fun read(consume: (List<PlayerBrawlerOwnershipSnapshot>) -> Unit): Long {
        val entityManager = entityManagerFactory.createEntityManager()

        entityManager.use { em ->
            val queryFactory = JPAQueryFactory(em)
            var lastId = 0L
            var totalRead = 0L

            while (true) {
                em.clear()

                val statsPlayers =
                    queryFactory
                        .selectFrom(statsPlayerEntity)
                        .where(statsPlayerEntity.id.gt(lastId))
                        .orderBy(statsPlayerEntity.id.asc())
                        .limit(PAGE_SIZE)
                        .fetch()

                if (statsPlayers.isEmpty()) break
                totalRead += statsPlayers.size
                lastId = statsPlayers.last().id!!
                val tags = statsPlayers.map { it.brawlStarsTag }

                val playerBrawlerRows =
                    queryFactory
                        .select(
                            playerBrawlerEntity.id,
                            playerBrawlerEntity.brawlerBrawlStarsId,
                            playerBrawlerEntity.trophies,
                        ).from(playerBrawlerEntity)
                        .join(playerBrawlerEntity.player, playerEntity)
                        .where(playerEntity.brawlStarsTag.`in`(tags))
                        .fetch()

                if (playerBrawlerRows.isEmpty()) continue

                val gearIdsByPlayerBrawlerId =
                    queryFactory
                        .select(playerBrawlerGearEntity.playerBrawler.id, playerBrawlerGearEntity.gearBrawlStarsId)
                        .from(playerBrawlerGearEntity)
                        .join(playerBrawlerGearEntity.playerBrawler, playerBrawlerEntity)
                        .join(playerBrawlerEntity.player, playerEntity)
                        .where(playerEntity.brawlStarsTag.`in`(tags))
                        .fetch()
                        .groupBy(
                            keySelector = { it.get(playerBrawlerGearEntity.playerBrawler.id)!! },
                            valueTransform = { it.get(playerBrawlerGearEntity.gearBrawlStarsId)!! },
                        )
                val gadgetIdsByPlayerBrawlerId =
                    queryFactory
                        .select(playerBrawlerGadgetEntity.playerBrawler.id, playerBrawlerGadgetEntity.gadgetBrawlStarsId)
                        .from(playerBrawlerGadgetEntity)
                        .join(playerBrawlerGadgetEntity.playerBrawler, playerBrawlerEntity)
                        .join(playerBrawlerEntity.player, playerEntity)
                        .where(playerEntity.brawlStarsTag.`in`(tags))
                        .fetch()
                        .groupBy(
                            keySelector = { it.get(playerBrawlerGadgetEntity.playerBrawler.id)!! },
                            valueTransform = { it.get(playerBrawlerGadgetEntity.gadgetBrawlStarsId)!! },
                        )
                val starPowerIdsByPlayerBrawlerId =
                    queryFactory
                        .select(playerBrawlerStarPowerEntity.playerBrawler.id, playerBrawlerStarPowerEntity.starPowerBrawlStarsId)
                        .from(playerBrawlerStarPowerEntity)
                        .join(playerBrawlerStarPowerEntity.playerBrawler, playerBrawlerEntity)
                        .join(playerBrawlerEntity.player, playerEntity)
                        .where(playerEntity.brawlStarsTag.`in`(tags))
                        .fetch()
                        .groupBy(
                            keySelector = { it.get(playerBrawlerStarPowerEntity.playerBrawler.id)!! },
                            valueTransform = { it.get(playerBrawlerStarPowerEntity.starPowerBrawlStarsId)!! },
                        )
                val hyperChargeIdsByPlayerBrawlerId =
                    queryFactory
                        .select(playerBrawlerHyperChargeEntity.playerBrawler.id, playerBrawlerHyperChargeEntity.hyperChargeBrawlStarsId)
                        .from(playerBrawlerHyperChargeEntity)
                        .join(playerBrawlerHyperChargeEntity.playerBrawler, playerBrawlerEntity)
                        .join(playerBrawlerEntity.player, playerEntity)
                        .where(playerEntity.brawlStarsTag.`in`(tags))
                        .fetch()
                        .groupBy(
                            keySelector = { it.get(playerBrawlerHyperChargeEntity.playerBrawler.id)!! },
                            valueTransform = { it.get(playerBrawlerHyperChargeEntity.hyperChargeBrawlStarsId)!! },
                        )
                val buffyTypesByPlayerBrawlerId =
                    queryFactory
                        .select(playerBrawlerBuffyEntity.playerBrawler.id, playerBrawlerBuffyEntity.buffyType)
                        .from(playerBrawlerBuffyEntity)
                        .join(playerBrawlerBuffyEntity.playerBrawler, playerBrawlerEntity)
                        .join(playerBrawlerEntity.player, playerEntity)
                        .where(playerEntity.brawlStarsTag.`in`(tags))
                        .fetch()
                        .groupBy(
                            keySelector = { it.get(playerBrawlerBuffyEntity.playerBrawler.id)!! },
                            valueTransform = { it.get(playerBrawlerBuffyEntity.buffyType)!! },
                        )

                val snapshots =
                    playerBrawlerRows.mapNotNull { row ->
                        val playerBrawlerId = row.get(playerBrawlerEntity.id) ?: return@mapNotNull null
                        PlayerBrawlerOwnershipSnapshot(
                            playerBrawlerId = playerBrawlerId,
                            brawlerBrawlStarsId = row.get(playerBrawlerEntity.brawlerBrawlStarsId)!!,
                            trophies = row.get(playerBrawlerEntity.trophies)!!,
                            gearBrawlStarsIds = gearIdsByPlayerBrawlerId[playerBrawlerId].orEmpty(),
                            gadgetBrawlStarsIds = gadgetIdsByPlayerBrawlerId[playerBrawlerId].orEmpty(),
                            starPowerBrawlStarsIds = starPowerIdsByPlayerBrawlerId[playerBrawlerId].orEmpty(),
                            hyperChargeBrawlStarsIds = hyperChargeIdsByPlayerBrawlerId[playerBrawlerId].orEmpty(),
                            buffyTypes = buffyTypesByPlayerBrawlerId[playerBrawlerId].orEmpty(),
                        )
                    }

                if (snapshots.isNotEmpty()) {
                    consume(snapshots)
                }
            }

            return totalRead
        }
    }

    companion object {
        private const val PAGE_SIZE = 1000L
    }
}
```

- [ ] **Step 4: 특성화 테스트 재실행 (동작 보존 증명)**

Run: `./gradlew :core:core-batch:contextTest --tests "com.imstargg.core.batch.job.statistics.ownership.BrawlerItemOwnershipStatsReaderTest"`
Expected: PASS — Step 2 와 동일한 테스트가 새 구현에서도 통과

- [ ] **Step 5: Commit**

```bash
./gradlew :core:core-batch:ktlintFormat
git add -A
git commit -m "refactor(core-batch): ownership 리더 IN 절을 태그 조인·프로젝션으로 전환"
```

---

### Task 10: offset 페이징 리더 3종 삭제

**Files:**
- Delete: `core/core-batch/src/main/kotlin/com/imstargg/core/batch/support/querydsl/QuerydslPagingItemReader.kt`
- Delete: `core/core-batch/src/main/kotlin/com/imstargg/core/batch/support/querydsl/QuerydslEntityCursorItemReader.kt`
- Delete: `core/core-batch/src/main/kotlin/com/imstargg/core/batch/support/querydsl/QuerydslZeroPagingItemReader.kt`

- [ ] **Step 1: 잔여 참조 확인 후 삭제**

```bash
grep -rn "QuerydslPagingItemReader\|QuerydslEntityCursorItemReader\|QuerydslZeroPagingItemReader" core/ --include="*.kt"
# 기대: 3개 정의 파일 자신만 출력 (Task 7·8 에서 사용처가 이미 제거됨)
git rm core/core-batch/src/main/kotlin/com/imstargg/core/batch/support/querydsl/QuerydslPagingItemReader.kt \
       core/core-batch/src/main/kotlin/com/imstargg/core/batch/support/querydsl/QuerydslEntityCursorItemReader.kt \
       core/core-batch/src/main/kotlin/com/imstargg/core/batch/support/querydsl/QuerydslZeroPagingItemReader.kt
```

- [ ] **Step 2: 컴파일 확인**

Run: `./gradlew :core:core-batch:compileKotlin :core:core-batch:compileTestKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git commit -am "refactor(core-batch): 미사용 offset 페이징 리더 3종 제거"
```

---

### ✅ PR2 체크포인트

- [ ] Run: `./gradlew ktlintCheck :core:core-batch:test`
- Expected: `BUILD SUCCESSFUL` (unit + context 전체 통과)
- PR2 생성 가능 (제목 예: `refactor: core-batch 리더 키셋 전환 및 ownership 쿼리 안전화`). 사용자 확인 후 진행.

---

# PR3 — 통계 집계 테이블 SEQUENCE 전환

### Task 11: 엔티티 10종 ID 전략 전환

**Files (Modify, 10):**
- `infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/BrawlerBattleResultStatsEntity.kt:77-80`
- `.../statistics/BrawlerBattleRankStatsEntity.kt:69-72`
- `.../statistics/BrawlerPairBattleResultStatsEntity.kt:64-67`
- `.../statistics/BrawlerPairBattleRankStatsEntity.kt:56-59`
- `.../statistics/BrawlerEnemyBattleResultStatsEntity.kt:64-67`
- `.../statistics/brawler/BrawlerGearOwnershipStatsEntity.kt:38-41`
- `.../statistics/brawler/BrawlerGadgetOwnershipStatsEntity.kt:38-41`
- `.../statistics/brawler/BrawlerStarPowerOwnershipStatsEntity.kt:38-41`
- `.../statistics/brawler/BrawlerHyperChargeOwnershipStatsEntity.kt:38-41`
- `.../statistics/brawler/BrawlerBuffyOwnershipStatsEntity.kt:40-43`

- [ ] **Step 1: 10개 엔티티의 @Id 블록 교체**

각 파일에서 기존 블록(컬럼명만 다르고 동일한 형태):

```kotlin
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "<컬럼명>", nullable = false)
    val id: Long? = null
```

을 아래 형태로 교체하고, import 에 `jakarta.persistence.SequenceGenerator` 를 추가한다. 파일별 치환 값:

| 파일 | `<컬럼명>` (불변) | generator/name | sequenceName |
|---|---|---|---|
| BrawlerBattleResultStatsEntity | brawler_battle_result_stats_id | brawlerBattleResultStatsSeq | brawler_battle_result_stats_seq |
| BrawlerBattleRankStatsEntity | brawler_battle_rank_stats_id | brawlerBattleRankStatsSeq | brawler_battle_rank_stats_seq |
| BrawlerPairBattleResultStatsEntity | brawler_pair_battle_result_stats_id | brawlerPairBattleResultStatsSeq | brawler_pair_battle_result_stats_seq |
| BrawlerPairBattleRankStatsEntity | brawler_pair_battle_rank_stats_id | brawlerPairBattleRankStatsSeq | brawler_pair_battle_rank_stats_seq |
| BrawlerEnemyBattleResultStatsEntity | brawler_enemy_battle_result_stats_id | brawlerEnemyBattleResultStatsSeq | brawler_enemy_battle_result_stats_seq |
| BrawlerGearOwnershipStatsEntity | brawler_gear_ownership_stats_id | brawlerGearOwnershipStatsSeq | brawler_gear_ownership_stats_seq |
| BrawlerGadgetOwnershipStatsEntity | brawler_gadget_ownership_stats_id | brawlerGadgetOwnershipStatsSeq | brawler_gadget_ownership_stats_seq |
| BrawlerStarPowerOwnershipStatsEntity | brawler_star_power_ownership_stats_id | brawlerStarPowerOwnershipStatsSeq | brawler_star_power_ownership_stats_seq |
| BrawlerHyperChargeOwnershipStatsEntity | brawler_hyper_charge_ownership_stats_id | brawlerHyperChargeOwnershipStatsSeq | brawler_hyper_charge_ownership_stats_seq |
| BrawlerBuffyOwnershipStatsEntity | brawler_buffy_ownership_stats_id | brawlerBuffyOwnershipStatsSeq | brawler_buffy_ownership_stats_seq |

교체 형태 (예: BrawlerBattleResultStatsEntity — 나머지 9개도 표의 값만 바꿔 동일 형태 적용):

```kotlin
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "brawlerBattleResultStatsSeq")
    @SequenceGenerator(
        name = "brawlerBattleResultStatsSeq",
        sequenceName = "brawler_battle_result_stats_seq",
        allocationSize = 100,
    )
    @Column(name = "brawler_battle_result_stats_id", nullable = false)
    val id: Long? = null
```

`allocationSize = 100` 은 Hibernate pooled optimizer 를 활성화한다 — 시퀀스 왕복도 100건당 1회. DB 시퀀스의 `increment by 100` (Task 12) 과 반드시 일치해야 한다.

- [ ] **Step 2: db-core 테스트로 매핑 검증**

Run: `./gradlew :infrastructure:db-core:ktlintFormat :infrastructure:db-core:test`
Expected: `BUILD SUCCESSFUL` — 테스트 DB 는 `ddl-auto: create` 라서 Hibernate 가 시퀀스를 생성하며, 통계 엔티티 save 경로가 새 전략으로 동작함을 검증

- [ ] **Step 3: core-batch 컨텍스트 테스트로 통계 잡 경로 회귀 확인**

Run: `./gradlew :core:core-batch:test`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "refactor(db-core): 통계 집계 엔티티 10종 ID 를 SEQUENCE 전략으로 전환"
```

---

### Task 12: 마이그레이션 SQL + schema.sql 동기화

**Files:**
- Create: `infrastructure/db-core/src/main/resources/db/migration/V260707__stats_identity_to_sequence.sql`
- Modify: `infrastructure/db-core/src/main/resources/schema.sql` (통계 테이블 10곳)

- [ ] **Step 1: 마이그레이션 파일 생성** (실제 반영일이 다르면 파일명 날짜 조정. Task 1 의 drop 파일과 같은 날짜여도 제목이 달라 무방 — 두 파일은 순서 무관)

```sql
-- 통계 집계 테이블 10종 PK 를 IDENTITY → SEQUENCE(increment 100) 로 전환한다.
-- Hibernate 는 IDENTITY 에서 JDBC insert 배칭을 비활성화한다. SEQUENCE(pooled, allocationSize=100)
-- 전환으로 core-batch 통계 쓰기의 INSERT 배칭이 활성화된다 (엔티티 @SequenceGenerator 와 일치 필수).
-- `set default nextval` 은 IDENTITY 를 기대하는 구버전 앱과의 호환 안전망이다 (DDL 선적용 → 앱 배포).
-- setval 은 max(id)+100 으로 초기화해 배포 전환 구간의 잔여 insert 와도 충돌하지 않는다.
-- 멱등: 재실행 시 setval 만 +100 재상승 (무해).

create sequence if not exists brawler_battle_result_stats_seq increment by 100 owned by brawler_battle_result_stats.brawler_battle_result_stats_id;
alter table brawler_battle_result_stats alter column brawler_battle_result_stats_id drop identity if exists;
alter table brawler_battle_result_stats alter column brawler_battle_result_stats_id set default nextval('brawler_battle_result_stats_seq');
select setval('brawler_battle_result_stats_seq', (select coalesce(max(brawler_battle_result_stats_id), 0) + 100 from brawler_battle_result_stats));

create sequence if not exists brawler_battle_rank_stats_seq increment by 100 owned by brawler_battle_rank_stats.brawler_battle_rank_stats_id;
alter table brawler_battle_rank_stats alter column brawler_battle_rank_stats_id drop identity if exists;
alter table brawler_battle_rank_stats alter column brawler_battle_rank_stats_id set default nextval('brawler_battle_rank_stats_seq');
select setval('brawler_battle_rank_stats_seq', (select coalesce(max(brawler_battle_rank_stats_id), 0) + 100 from brawler_battle_rank_stats));

create sequence if not exists brawler_pair_battle_result_stats_seq increment by 100 owned by brawler_pair_battle_result_stats.brawler_pair_battle_result_stats_id;
alter table brawler_pair_battle_result_stats alter column brawler_pair_battle_result_stats_id drop identity if exists;
alter table brawler_pair_battle_result_stats alter column brawler_pair_battle_result_stats_id set default nextval('brawler_pair_battle_result_stats_seq');
select setval('brawler_pair_battle_result_stats_seq', (select coalesce(max(brawler_pair_battle_result_stats_id), 0) + 100 from brawler_pair_battle_result_stats));

create sequence if not exists brawler_pair_battle_rank_stats_seq increment by 100 owned by brawler_pair_battle_rank_stats.brawler_pair_battle_rank_stats_id;
alter table brawler_pair_battle_rank_stats alter column brawler_pair_battle_rank_stats_id drop identity if exists;
alter table brawler_pair_battle_rank_stats alter column brawler_pair_battle_rank_stats_id set default nextval('brawler_pair_battle_rank_stats_seq');
select setval('brawler_pair_battle_rank_stats_seq', (select coalesce(max(brawler_pair_battle_rank_stats_id), 0) + 100 from brawler_pair_battle_rank_stats));

create sequence if not exists brawler_enemy_battle_result_stats_seq increment by 100 owned by brawler_enemy_battle_result_stats.brawler_enemy_battle_result_stats_id;
alter table brawler_enemy_battle_result_stats alter column brawler_enemy_battle_result_stats_id drop identity if exists;
alter table brawler_enemy_battle_result_stats alter column brawler_enemy_battle_result_stats_id set default nextval('brawler_enemy_battle_result_stats_seq');
select setval('brawler_enemy_battle_result_stats_seq', (select coalesce(max(brawler_enemy_battle_result_stats_id), 0) + 100 from brawler_enemy_battle_result_stats));

create sequence if not exists brawler_gear_ownership_stats_seq increment by 100 owned by brawler_gear_ownership_stats.brawler_gear_ownership_stats_id;
alter table brawler_gear_ownership_stats alter column brawler_gear_ownership_stats_id drop identity if exists;
alter table brawler_gear_ownership_stats alter column brawler_gear_ownership_stats_id set default nextval('brawler_gear_ownership_stats_seq');
select setval('brawler_gear_ownership_stats_seq', (select coalesce(max(brawler_gear_ownership_stats_id), 0) + 100 from brawler_gear_ownership_stats));

create sequence if not exists brawler_gadget_ownership_stats_seq increment by 100 owned by brawler_gadget_ownership_stats.brawler_gadget_ownership_stats_id;
alter table brawler_gadget_ownership_stats alter column brawler_gadget_ownership_stats_id drop identity if exists;
alter table brawler_gadget_ownership_stats alter column brawler_gadget_ownership_stats_id set default nextval('brawler_gadget_ownership_stats_seq');
select setval('brawler_gadget_ownership_stats_seq', (select coalesce(max(brawler_gadget_ownership_stats_id), 0) + 100 from brawler_gadget_ownership_stats));

create sequence if not exists brawler_star_power_ownership_stats_seq increment by 100 owned by brawler_star_power_ownership_stats.brawler_star_power_ownership_stats_id;
alter table brawler_star_power_ownership_stats alter column brawler_star_power_ownership_stats_id drop identity if exists;
alter table brawler_star_power_ownership_stats alter column brawler_star_power_ownership_stats_id set default nextval('brawler_star_power_ownership_stats_seq');
select setval('brawler_star_power_ownership_stats_seq', (select coalesce(max(brawler_star_power_ownership_stats_id), 0) + 100 from brawler_star_power_ownership_stats));

create sequence if not exists brawler_hyper_charge_ownership_stats_seq increment by 100 owned by brawler_hyper_charge_ownership_stats.brawler_hyper_charge_ownership_stats_id;
alter table brawler_hyper_charge_ownership_stats alter column brawler_hyper_charge_ownership_stats_id drop identity if exists;
alter table brawler_hyper_charge_ownership_stats alter column brawler_hyper_charge_ownership_stats_id set default nextval('brawler_hyper_charge_ownership_stats_seq');
select setval('brawler_hyper_charge_ownership_stats_seq', (select coalesce(max(brawler_hyper_charge_ownership_stats_id), 0) + 100 from brawler_hyper_charge_ownership_stats));

create sequence if not exists brawler_buffy_ownership_stats_seq increment by 100 owned by brawler_buffy_ownership_stats.brawler_buffy_ownership_stats_id;
alter table brawler_buffy_ownership_stats alter column brawler_buffy_ownership_stats_id drop identity if exists;
alter table brawler_buffy_ownership_stats alter column brawler_buffy_ownership_stats_id set default nextval('brawler_buffy_ownership_stats_seq');
select setval('brawler_buffy_ownership_stats_seq', (select coalesce(max(brawler_buffy_ownership_stats_id), 0) + 100 from brawler_buffy_ownership_stats));
```

- [ ] **Step 2: schema.sql 의 10개 테이블 동기화**

각 통계 테이블에 대해 3가지 변경 (예시는 brawler_battle_result_stats — 나머지 9개 테이블도 표(Task 11)의 이름으로 동일 적용):

1. `create table` 문 **앞줄**에 시퀀스 생성 추가:

```sql
create sequence if not exists brawler_battle_result_stats_seq increment by 100;
```

2. id 컬럼 선언 교체 — 기존:

```sql
    brawler_battle_result_stats_id bigint generated always as identity primary key,
```

→ 신규:

```sql
    brawler_battle_result_stats_id bigint primary key default nextval('brawler_battle_result_stats_seq'),
```

3. 해당 테이블의 인덱스 생성문 **다음 줄**에 소유권 연결 추가 (테이블 drop 시 시퀀스 동반 정리):

```sql
alter sequence brawler_battle_result_stats_seq owned by brawler_battle_result_stats.brawler_battle_result_stats_id;
```

적용 후 확인: `grep -c "generated always as identity" infrastructure/db-core/src/main/resources/schema.sql` 결과가 기존보다 정확히 10 감소.

- [ ] **Step 3: 신규 프로비저닝 스키마 검증 (로컬 Docker)**

```bash
docker-compose up -d
docker exec -i $(docker ps -qf "name=postgres") psql -U postgres -c "create database schema_check" \
  && docker exec -i $(docker ps -qf "name=postgres") psql -U postgres -d schema_check < infrastructure/db-core/src/main/resources/schema.sql \
  && docker exec -i $(docker ps -qf "name=postgres") psql -U postgres -d schema_check -c "\ds brawler_*_seq" \
  && docker exec -i $(docker ps -qf "name=postgres") psql -U postgres -c "drop database schema_check"
```

Expected: `\ds` 출력에 10개 시퀀스. (docker-compose 서비스명이 다르면 `docker ps` 로 컨테이너명 확인 후 조정)

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat(db-core): 통계 집계 테이블 SEQUENCE 전환 마이그레이션·스키마 반영"
```

---

### Task 13: PR3 최종 검증

- [ ] **Step 1: 백엔드 전체 lint + 테스트**

Run: `./gradlew ktlintCheck test`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 2 (선택, 수동): 로컬에서 INSERT 배칭 확인**

로컬 DB 에 V260707 마이그레이션 2개를 적용한 뒤, 통계 잡 하나를 실행하고 Hibernate 배칭을 확인한다:

```bash
docker exec -i $(docker ps -qf "name=postgres") psql -U postgres -d imstargg \
  < infrastructure/db-core/src/main/resources/db/migration/V260707__stats_identity_to_sequence.sql
JAVA_TOOL_OPTIONS="-Dspring.jpa.properties.hibernate.generate_statistics=true" \
  ./gradlew :core:core-batch:bootRun --args="--job.name=brawlerBattleResultStatsJob --date=$(date -v-1d +%Y-%m-%d)"
```

Expected: 로그의 Hibernate 통계에서 `xxx nanoseconds spent executing N JDBC batches` — 배치 수 N 이 insert 건수의 약 1/100 수준 (IDENTITY 시절에는 batch 0 + statement 건수만큼).

- [ ] **Step 3: PR3 생성 시 본문에 운영 적용 절차 명시** (사용자 확인 후)

PR 본문 필수 내용:
1. 운영 DDL 선적용: `V260707__stats_identity_to_sequence.sql` (+ PR1 의 `V260707__drop_trio_stats_tables.sql` — trio 는 row count 0 확인 후)
2. DDL 적용 후 앱 배포 (역순 금지 아님 — `default nextval` 안전망으로 양방향 호환이지만, 선적용 권장)
3. 롤백: 앱만 이전 버전으로 되돌리면 됨 (default nextval 로 IDENTITY 스타일 insert 도 동작)

---

## 스펙 커버리지 매핑

| 스펙 항목 | 태스크 |
|---|---|
| §3.1 SEQUENCE 전환 (10종) | Task 11, 12, 13 |
| §3.2 ownership 리더 안전화 | Task 9 |
| §3.3 offset→keyset (`KeysetItemReader`) | Task 6, 7, 8, 10 |
| §4.1 공통화 미실행 (결정 기록) | 해당 코드 작업 없음 — 통계 Reader/Writer/Task 불변 |
| §4.2 ownership 도메인 이동 | Task 5 |
| §4.3 데드코드·trio·WeekRange | Task 1, 2, 3, 10 |
| §4.4 보존↔룩백 가드 | Task 4 |
| §7 테스트 전략 (동작 보존·신규 테스트) | Task 4, 6, 9 + 각 체크포인트 |
| §8 PR 3단계 | PR1/PR2/PR3 체크포인트 |
