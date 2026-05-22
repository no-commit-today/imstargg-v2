# core-batch 통계 잡 평탄화 & 트랜잭션 정리 — 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 5개 통계 잡(Daily 2개 + Weekly-Event 3개)을 ownership 잡과 동일한 Task/Reader/Writer 분리 패턴으로 평탄화하고, read를 트랜잭션 밖으로 빼서 긴 tasklet 트랜잭션을 해소한다.

**Architecture:** 각 통계 잡당 `*Reader`(자체 EntityManager, 트랜잭션 X) + `*Writer`(@Transactional, delete + chunked persist) + `*Task`(흐름 조립) 컴포넌트 신설. JobConfig 는 wiring 만 남기고 generic + 다수 람다 기반 `DailyBattleStatsAggregationTask` / `WeeklyEventBattleStatsAggregationTask` 두 공통 task 를 제거.

**Tech Stack:** Kotlin / Spring Boot / Spring Batch / QueryDSL JPA / MockK / AssertJ / JUnit 5

**선행 문서:** [docs/superpowers/specs/2026-05-22-core-batch-stats-flatten-design.md](../specs/2026-05-22-core-batch-stats-flatten-design.md)

---

## 도메인 컨텍스트 (사전 지식)

### 트랜잭션 분리 패턴 (ownership 잡 참고)

ownership 잡은 이미 다음과 같이 분리되어 있다. 통계 5개 잡도 이 모양을 그대로 따른다.

- `BrawlerItemOwnershipStatsReader` — `EntityManagerFactory.createEntityManager()` 로 자체 em 생성, `entityManager.use { em -> ... }` 안에서 페이지 단위 fetch. **트랜잭션 없음**.
- `BrawlerItemOwnershipStatsWriter` — 클래스 메서드에 `@Transactional`. `deleteAllInBatch()` 후 `saveAllAndFlush` 를 chunk 단위로 호출하면서 `em.clear()`.
- `BrawlerItemOwnershipStatsTask` — reader/writer 호출 흐름 조립. 진행/완료 로그.
- `BrawlerItemOwnershipStatsJobConfig` — Job/Step wiring 만 (40줄).

### 통계 잡의 두 가지 모양

- **Daily**: `date: LocalDate` 입력. `statsResultBattleEntity` 또는 `statsRankBattleEntity` 의 `battleDate.eq(date)` 로 페이지 read. delete 도 같은 조건.
- **Weekly-Event**: `weekRanges: List<WeekRange>` 입력. weekRange 별로 `eventBrawlStarsId` distinct 로 이벤트 목록 조회 → 각 이벤트별로 페이지 read → 이벤트별 aggregator 인스턴스 → 이벤트별 delete + write.

### 5개 통계 잡 대응표

| 잡 이름 | 종류 | Aggregator | 입력 Entity | 출력 Entity |
|---|---|---|---|---|
| `brawlerBattleResultStatsJob` | Daily | `BrawlerBattleResultStatsAggregator` | `StatsResultBattleEntity` | `BrawlerBattleResultStatsEntity` |
| `brawlerBattleRankStatsJob` | Daily | `BrawlerBattleRankStatsAggregator` | `StatsRankBattleEntity` | `BrawlerBattleRankStatsEntity` |
| `brawlerPairBattleResultStatsJob` | Weekly-Event | `BrawlerPairBattleResultStatsAggregator(start, end)` | `StatsResultBattleEntity` | `BrawlerPairBattleResultStatsEntity` |
| `brawlerPairBattleRankStatsJob` | Weekly-Event | `BrawlerPairBattleRankStatsAggregator(start, end)` | `StatsRankBattleEntity` | `BrawlerPairBattleRankStatsEntity` |
| `brawlerEnemyBattleResultStatsJob` | Weekly-Event | `BrawlerEnemyBattleResultStatsAggregator(start, end)` | `StatsResultBattleEntity` | `BrawlerEnemyBattleResultStatsEntity` |

### 코드 컨벤션 (imstargg-backend/CLAUDE.md)

- **파일당 1개 클래스**
- **테스트 베이스**: `DbCoreContextTest` (`@DataJpaTest`) Repository/Writer 테스트, 단위 테스트는 베이스 없음
- **테스트 메서드명**: 한글, 백틱 사용 가능
- **테스트 SUT 변수명**: `sut`
- **`// given / when / then`** 주석 패턴
- **AssertJ**: `assertThat`
- **테스트 픽스처 패턴**: `a{Entity}().with{Field}().build()` 있으면 사용 (없으면 직접 생성자)
- **named arguments**: 인자 3개 이상이면 사용
- **`@Tag("context")`**: SpringContext 필요한 통합 테스트
- **컨텍스트 테스트 정리**: `CleanUp.all()` 호출 후 `JpaRepository.save()` 로 데이터 삽입

### 빌드/실행

```bash
./gradlew :core:core-batch:unitTest      # 단위 테스트만
./gradlew :core:core-batch:contextTest   # Spring context 통합 테스트
./gradlew :core:core-batch:test          # 위 두개 + develop 태그 제외
./gradlew :core:core-batch:ktlintFormat  # 린트 자동 수정
./gradlew :core:core-batch:ktlintCheck   # 린트 검사
```

### 파일 위치 합의

- 통계 5개 잡은 기존 `job/statistics/` 평면 구조 유지. ownership 처럼 하위 패키지로 묶지 않는다 (선행 spec 의 패키지 옵션 중 후자 선택 — 구현 단순화).
- Reader/Writer/Task 모두 `job/statistics/` 직하에 둔다. 즉:
  - `job/statistics/BrawlerBattleResultStatsReader.kt`
  - `job/statistics/BrawlerBattleResultStatsWriter.kt`
  - `job/statistics/BrawlerBattleResultStatsTask.kt`
  - `job/statistics/BrawlerBattleResultStatsJobConfig.kt` (이미 있음, 슬림화)

---

## File Structure

### 신설 파일 (15개)

```
core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/
├── BrawlerBattleResultStatsReader.kt        (Daily, StatsResultBattleEntity)
├── BrawlerBattleResultStatsWriter.kt
├── BrawlerBattleResultStatsTask.kt
├── BrawlerBattleRankStatsReader.kt          (Daily, StatsRankBattleEntity)
├── BrawlerBattleRankStatsWriter.kt
├── BrawlerBattleRankStatsTask.kt
├── BrawlerPairBattleResultStatsReader.kt    (Weekly-Event, StatsResultBattleEntity)
├── BrawlerPairBattleResultStatsWriter.kt
├── BrawlerPairBattleResultStatsTask.kt
├── BrawlerPairBattleRankStatsReader.kt      (Weekly-Event, StatsRankBattleEntity)
├── BrawlerPairBattleRankStatsWriter.kt
├── BrawlerPairBattleRankStatsTask.kt
├── BrawlerEnemyBattleResultStatsReader.kt   (Weekly-Event, StatsResultBattleEntity)
├── BrawlerEnemyBattleResultStatsWriter.kt
└── BrawlerEnemyBattleResultStatsTask.kt
```

### 신설 테스트 파일 (15개)

각 신규 컴포넌트별 `*Test.kt` (단위 테스트, MockK 사용).

### 수정 파일 (6개)

- `BrawlerBattleResultStatsJobConfig.kt` — 슬림화
- `BrawlerBattleRankStatsJobConfig.kt` — 슬림화
- `BrawlerPairBattleResultStatsJobConfig.kt` — 슬림화
- `BrawlerPairBattleRankStatsJobConfig.kt` — 슬림화
- `BrawlerEnemyBattleResultStatsJobConfig.kt` — 슬림화
- `BrawlerSyncTask.kt` — `detectFor` 헬퍼 추출

### 삭제 파일 (4개)

- `task/DailyBattleStatsAggregationTask.kt`
- `task/WeeklyEventBattleStatsAggregationTask.kt`
- `test/.../task/DailyBattleStatsAggregationTaskTest.kt`
- `test/.../task/WeeklyEventBattleStatsAggregationTaskTest.kt`

---

## Task 1: 베이스라인 확인

리팩터링 전 모든 테스트가 통과하는지 확인. 이후 회귀 비교 기준.

- [ ] **Step 1.1: 베이스 단위 테스트 통과 확인**

Run:
```bash
./gradlew :core:core-batch:unitTest
```
Expected: BUILD SUCCESSFUL. 모든 테스트 통과.

- [ ] **Step 1.2: 컨텍스트 테스트 통과 확인**

Run:
```bash
./gradlew :core:core-batch:contextTest
```
Expected: BUILD SUCCESSFUL. `CoreBatchContextTest` 가 모든 Job/Step bean 을 로딩.

- [ ] **Step 1.3: 린트 통과 확인**

Run:
```bash
./gradlew :core:core-batch:ktlintCheck
```
Expected: BUILD SUCCESSFUL.

---

## Task 2: BrawlerBattleResultStatsJob 평탄화

**Files:**
- Create: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerBattleResultStatsReader.kt`
- Create: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerBattleResultStatsWriter.kt`
- Create: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerBattleResultStatsTask.kt`
- Create: `imstargg-backend/core/core-batch/src/test/kotlin/com/imstargg/core/batch/job/statistics/BrawlerBattleResultStatsTaskTest.kt`
- Modify: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerBattleResultStatsJobConfig.kt`

### Reader

- [ ] **Step 2.1: `BrawlerBattleResultStatsReader.kt` 생성**

```kotlin
package com.imstargg.core.batch.job.statistics

import com.imstargg.infrastructure.db.core.statistics.battle.QStatsResultBattleEntity.Companion.statsResultBattleEntity
import com.imstargg.infrastructure.db.core.statistics.battle.StatsResultBattleEntity
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManagerFactory
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class BrawlerBattleResultStatsReader(
    private val entityManagerFactory: EntityManagerFactory,
) {
    fun read(
        date: LocalDate,
        consume: (List<StatsResultBattleEntity>) -> Unit,
    ): Long {
        val entityManager = entityManagerFactory.createEntityManager()

        entityManager.use { em ->
            val queryFactory = JPAQueryFactory(em)
            var lastId = 0L
            var totalRead = 0L

            while (true) {
                em.clear()

                val battles =
                    queryFactory
                        .selectFrom(statsResultBattleEntity)
                        .where(
                            statsResultBattleEntity.battleDate.eq(date),
                            statsResultBattleEntity.id.gt(lastId),
                        ).orderBy(statsResultBattleEntity.id.asc())
                        .limit(BattleStatsConstants.READ_PAGE_SIZE)
                        .fetch()

                if (battles.isEmpty()) break
                totalRead += battles.size
                lastId = battles.last().id!!
                consume(battles)
            }

            return totalRead
        }
    }
}
```

### Writer

- [ ] **Step 2.2: `BrawlerBattleResultStatsWriter.kt` 생성**

```kotlin
package com.imstargg.core.batch.job.statistics

import com.imstargg.core.batch.domain.BrawlerBattleResultStatsAggregator
import com.imstargg.infrastructure.db.core.statistics.QBrawlerBattleResultStatsEntity.Companion.brawlerBattleResultStatsEntity
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Component
class BrawlerBattleResultStatsWriter(
    private val em: EntityManager,
) {
    @Transactional
    fun write(
        date: LocalDate,
        aggregator: BrawlerBattleResultStatsAggregator,
    ): Long {
        val queryFactory = JPAQueryFactory(em)
        queryFactory
            .delete(brawlerBattleResultStatsEntity)
            .where(brawlerBattleResultStatsEntity.battleDate.eq(date))
            .execute()

        var totalEntities = 0L
        aggregator.flushEntityChunks(BattleStatsConstants.BATCH_SIZE) { batch ->
            batch.forEach { em.persist(it) }
            totalEntities += batch.size
            em.flush()
            em.clear()
        }
        return totalEntities
    }
}
```

### Task

- [ ] **Step 2.3: `BrawlerBattleResultStatsTask.kt` 생성**

```kotlin
package com.imstargg.core.batch.job.statistics

import com.imstargg.core.batch.domain.BrawlerBattleResultStatsAggregator
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class BrawlerBattleResultStatsTask(
    private val reader: BrawlerBattleResultStatsReader,
    private val writer: BrawlerBattleResultStatsWriter,
) {
    fun aggregate(date: LocalDate) {
        log.info("[{}] 시작 - date: {}", JOB_NAME, date)
        val aggregator = BrawlerBattleResultStatsAggregator()
        val totalRead =
            reader.read(date) { battles ->
                battles.forEach { aggregator.aggregate(it) }
                log.debug("[{}] 읽기 진행 - date: {}", JOB_NAME, date)
            }
        val totalEntities = writer.write(date, aggregator)
        log.info(
            "[{}] 완료 - date: {}, 전투: {}건, 집계: {}건",
            JOB_NAME,
            date,
            totalRead,
            totalEntities,
        )
    }

    companion object {
        private const val JOB_NAME = "brawlerBattleResultStatsJob"
        private val log = LoggerFactory.getLogger(BrawlerBattleResultStatsTask::class.java)
    }
}
```

### Task 단위 테스트

- [ ] **Step 2.4: `BrawlerBattleResultStatsTaskTest.kt` 생성**

ownership task 테스트 (`BrawlerItemOwnershipStatsTaskTest`) 와 동일 구조. reader/writer mock. 흐름 검증.

```kotlin
package com.imstargg.core.batch.job.statistics

import com.imstargg.core.batch.domain.BrawlerBattleResultStatsAggregator
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BrawlerBattleResultStatsTaskTest {
    private val reader = mockk<BrawlerBattleResultStatsReader>()
    private val writer = mockk<BrawlerBattleResultStatsWriter>()
    private val sut = BrawlerBattleResultStatsTask(reader, writer)

    @Test
    fun `리더가 전달한 배틀을 집계해 writer 에 저장한다`() {
        // given
        val date = LocalDate.of(2026, 5, 1)
        val aggregatorSlot = slot<BrawlerBattleResultStatsAggregator>()
        every { reader.read(eq(date), any()) } returns 0L
        every { writer.write(eq(date), capture(aggregatorSlot)) } returns 0L

        // when
        sut.aggregate(date)

        // then
        verify(exactly = 1) { reader.read(eq(date), any()) }
        verify(exactly = 1) { writer.write(eq(date), any()) }
    }
}
```

### Run task tests

- [ ] **Step 2.5: Task 테스트 실행**

Run:
```bash
./gradlew :core:core-batch:test --tests "com.imstargg.core.batch.job.statistics.BrawlerBattleResultStatsTaskTest"
```
Expected: PASS

### JobConfig 슬림화

- [ ] **Step 2.6: `BrawlerBattleResultStatsJobConfig.kt` 슬림화**

기존 파일을 다음 내용으로 교체:

```kotlin
package com.imstargg.core.batch.job.statistics

import com.imstargg.core.batch.support.parameter.DateJobParameter
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.job.parameters.DefaultJobParametersValidator
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.Step
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.infrastructure.repeat.RepeatStatus
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager

@Configuration
class BrawlerBattleResultStatsJobConfig(
    private val jobRepository: JobRepository,
    private val txManager: PlatformTransactionManager,
    private val task: BrawlerBattleResultStatsTask,
) {
    companion object {
        const val JOB_NAME = "brawlerBattleResultStatsJob"
        const val DATE_PARAMETER_NAME = "brawlerBattleResultStatsJobDateJobParameter"
        private const val STEP_NAME = "brawlerBattleResultStatsStep"
    }

    @Bean(DATE_PARAMETER_NAME)
    @JobScope
    fun dateJobParameter() = DateJobParameter()

    @Bean(JOB_NAME)
    fun brawlerBattleResultStatsJob(): Job =
        JobBuilder(JOB_NAME, jobRepository)
            .validator(
                DefaultJobParametersValidator(
                    arrayOf(DateJobParameter.KEY),
                    arrayOf(),
                ),
            ).start(brawlerBattleResultStatsStep())
            .build()

    @Bean(STEP_NAME)
    @JobScope
    fun brawlerBattleResultStatsStep(): Step =
        StepBuilder(STEP_NAME, jobRepository)
            .tasklet({ _, _ ->
                task.aggregate(dateJobParameter().date!!)
                RepeatStatus.FINISHED
            }, txManager)
            .build()
}
```

### 회귀 확인

- [ ] **Step 2.7: 컨텍스트 테스트 통과 확인**

Run:
```bash
./gradlew :core:core-batch:contextTest
```
Expected: BUILD SUCCESSFUL. `BrawlerBattleResultStatsJob`, `BrawlerBattleResultStatsTask`, Reader, Writer bean 모두 로딩.

- [ ] **Step 2.8: 린트 적용**

Run:
```bash
./gradlew :core:core-batch:ktlintFormat
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 2.9: 커밋**

```bash
git add imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerBattleResultStats{Reader,Writer,Task,JobConfig}.kt \
        imstargg-backend/core/core-batch/src/test/kotlin/com/imstargg/core/batch/job/statistics/BrawlerBattleResultStatsTaskTest.kt
git commit -m "$(cat <<'EOF'
refactor(core-batch): BrawlerBattleResultStatsJob 평탄화

JobConfig 에서 generic 람다 조립을 제거하고 Reader/Writer/Task 로 분리.
read 는 자체 EntityManager 로 트랜잭션 밖에서 수행, write 만 @Transactional.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 3: BrawlerBattleRankStatsJob 평탄화

**Files:**
- Create: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerBattleRankStatsReader.kt`
- Create: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerBattleRankStatsWriter.kt`
- Create: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerBattleRankStatsTask.kt`
- Create: `imstargg-backend/core/core-batch/src/test/kotlin/com/imstargg/core/batch/job/statistics/BrawlerBattleRankStatsTaskTest.kt`
- Modify: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerBattleRankStatsJobConfig.kt`

Task 2 와 동일 패턴. 차이점: `StatsResultBattleEntity` → `StatsRankBattleEntity`, `BrawlerBattleResultStatsAggregator` → `BrawlerBattleRankStatsAggregator`, `BrawlerBattleResultStatsEntity` → `BrawlerBattleRankStatsEntity`, Q-class 이름 차이.

### Reader

- [ ] **Step 3.1: `BrawlerBattleRankStatsReader.kt` 생성**

```kotlin
package com.imstargg.core.batch.job.statistics

import com.imstargg.infrastructure.db.core.statistics.battle.QStatsRankBattleEntity.Companion.statsRankBattleEntity
import com.imstargg.infrastructure.db.core.statistics.battle.StatsRankBattleEntity
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManagerFactory
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class BrawlerBattleRankStatsReader(
    private val entityManagerFactory: EntityManagerFactory,
) {
    fun read(
        date: LocalDate,
        consume: (List<StatsRankBattleEntity>) -> Unit,
    ): Long {
        val entityManager = entityManagerFactory.createEntityManager()

        entityManager.use { em ->
            val queryFactory = JPAQueryFactory(em)
            var lastId = 0L
            var totalRead = 0L

            while (true) {
                em.clear()

                val battles =
                    queryFactory
                        .selectFrom(statsRankBattleEntity)
                        .where(
                            statsRankBattleEntity.battleDate.eq(date),
                            statsRankBattleEntity.id.gt(lastId),
                        ).orderBy(statsRankBattleEntity.id.asc())
                        .limit(BattleStatsConstants.READ_PAGE_SIZE)
                        .fetch()

                if (battles.isEmpty()) break
                totalRead += battles.size
                lastId = battles.last().id!!
                consume(battles)
            }

            return totalRead
        }
    }
}
```

### Writer

- [ ] **Step 3.2: `BrawlerBattleRankStatsWriter.kt` 생성**

```kotlin
package com.imstargg.core.batch.job.statistics

import com.imstargg.core.batch.domain.BrawlerBattleRankStatsAggregator
import com.imstargg.infrastructure.db.core.statistics.QBrawlerBattleRankStatsEntity.Companion.brawlerBattleRankStatsEntity
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Component
class BrawlerBattleRankStatsWriter(
    private val em: EntityManager,
) {
    @Transactional
    fun write(
        date: LocalDate,
        aggregator: BrawlerBattleRankStatsAggregator,
    ): Long {
        val queryFactory = JPAQueryFactory(em)
        queryFactory
            .delete(brawlerBattleRankStatsEntity)
            .where(brawlerBattleRankStatsEntity.battleDate.eq(date))
            .execute()

        var totalEntities = 0L
        aggregator.flushEntityChunks(BattleStatsConstants.BATCH_SIZE) { batch ->
            batch.forEach { em.persist(it) }
            totalEntities += batch.size
            em.flush()
            em.clear()
        }
        return totalEntities
    }
}
```

### Task

- [ ] **Step 3.3: `BrawlerBattleRankStatsTask.kt` 생성**

```kotlin
package com.imstargg.core.batch.job.statistics

import com.imstargg.core.batch.domain.BrawlerBattleRankStatsAggregator
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class BrawlerBattleRankStatsTask(
    private val reader: BrawlerBattleRankStatsReader,
    private val writer: BrawlerBattleRankStatsWriter,
) {
    fun aggregate(date: LocalDate) {
        log.info("[{}] 시작 - date: {}", JOB_NAME, date)
        val aggregator = BrawlerBattleRankStatsAggregator()
        val totalRead =
            reader.read(date) { battles ->
                battles.forEach { aggregator.aggregate(it) }
                log.debug("[{}] 읽기 진행 - date: {}", JOB_NAME, date)
            }
        val totalEntities = writer.write(date, aggregator)
        log.info(
            "[{}] 완료 - date: {}, 전투: {}건, 집계: {}건",
            JOB_NAME,
            date,
            totalRead,
            totalEntities,
        )
    }

    companion object {
        private const val JOB_NAME = "brawlerBattleRankStatsJob"
        private val log = LoggerFactory.getLogger(BrawlerBattleRankStatsTask::class.java)
    }
}
```

### Task 단위 테스트

- [ ] **Step 3.4: `BrawlerBattleRankStatsTaskTest.kt` 생성**

```kotlin
package com.imstargg.core.batch.job.statistics

import com.imstargg.core.batch.domain.BrawlerBattleRankStatsAggregator
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BrawlerBattleRankStatsTaskTest {
    private val reader = mockk<BrawlerBattleRankStatsReader>()
    private val writer = mockk<BrawlerBattleRankStatsWriter>()
    private val sut = BrawlerBattleRankStatsTask(reader, writer)

    @Test
    fun `리더가 전달한 배틀을 집계해 writer 에 저장한다`() {
        // given
        val date = LocalDate.of(2026, 5, 1)
        val aggregatorSlot = slot<BrawlerBattleRankStatsAggregator>()
        every { reader.read(eq(date), any()) } returns 0L
        every { writer.write(eq(date), capture(aggregatorSlot)) } returns 0L

        // when
        sut.aggregate(date)

        // then
        verify(exactly = 1) { reader.read(eq(date), any()) }
        verify(exactly = 1) { writer.write(eq(date), any()) }
    }
}
```

- [ ] **Step 3.5: Task 테스트 실행**

Run:
```bash
./gradlew :core:core-batch:test --tests "com.imstargg.core.batch.job.statistics.BrawlerBattleRankStatsTaskTest"
```
Expected: PASS

### JobConfig 슬림화

- [ ] **Step 3.6: `BrawlerBattleRankStatsJobConfig.kt` 슬림화**

기존 파일을 다음 내용으로 교체:

```kotlin
package com.imstargg.core.batch.job.statistics

import com.imstargg.core.batch.support.parameter.DateJobParameter
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.job.parameters.DefaultJobParametersValidator
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.Step
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.infrastructure.repeat.RepeatStatus
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager

@Configuration
class BrawlerBattleRankStatsJobConfig(
    private val jobRepository: JobRepository,
    private val txManager: PlatformTransactionManager,
    private val task: BrawlerBattleRankStatsTask,
) {
    companion object {
        const val JOB_NAME = "brawlerBattleRankStatsJob"
        const val DATE_PARAMETER_NAME = "brawlerBattleRankStatsJobDateJobParameter"
        private const val STEP_NAME = "brawlerBattleRankStatsStep"
    }

    @Bean(DATE_PARAMETER_NAME)
    @JobScope
    fun dateJobParameter() = DateJobParameter()

    @Bean(JOB_NAME)
    fun brawlerBattleRankStatsJob(): Job =
        JobBuilder(JOB_NAME, jobRepository)
            .validator(
                DefaultJobParametersValidator(
                    arrayOf(DateJobParameter.KEY),
                    arrayOf(),
                ),
            ).start(brawlerBattleRankStatsStep())
            .build()

    @Bean(STEP_NAME)
    @JobScope
    fun brawlerBattleRankStatsStep(): Step =
        StepBuilder(STEP_NAME, jobRepository)
            .tasklet({ _, _ ->
                task.aggregate(dateJobParameter().date!!)
                RepeatStatus.FINISHED
            }, txManager)
            .build()
}
```

- [ ] **Step 3.7: 컨텍스트 테스트 통과 확인**

Run:
```bash
./gradlew :core:core-batch:contextTest
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3.8: 린트 적용 & 커밋**

Run:
```bash
./gradlew :core:core-batch:ktlintFormat
```

```bash
git add imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerBattleRankStats{Reader,Writer,Task,JobConfig}.kt \
        imstargg-backend/core/core-batch/src/test/kotlin/com/imstargg/core/batch/job/statistics/BrawlerBattleRankStatsTaskTest.kt
git commit -m "$(cat <<'EOF'
refactor(core-batch): BrawlerBattleRankStatsJob 평탄화

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 4: BrawlerPairBattleResultStatsJob 평탄화

**Files:**
- Create: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerPairBattleResultStatsReader.kt`
- Create: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerPairBattleResultStatsWriter.kt`
- Create: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerPairBattleResultStatsTask.kt`
- Create: `imstargg-backend/core/core-batch/src/test/kotlin/com/imstargg/core/batch/job/statistics/BrawlerPairBattleResultStatsTaskTest.kt`
- Modify: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerPairBattleResultStatsJobConfig.kt`

Weekly-Event 형. Reader 는 `readDistinctEventIds` + `read(weekRange, eventId, consume)` 두 메서드. Writer 는 `weekRange × eventId` 단위로 delete + write.

### Reader

- [ ] **Step 4.1: `BrawlerPairBattleResultStatsReader.kt` 생성**

```kotlin
package com.imstargg.core.batch.job.statistics

import com.imstargg.core.enums.WeekRange
import com.imstargg.infrastructure.db.core.statistics.battle.QStatsResultBattleEntity.Companion.statsResultBattleEntity
import com.imstargg.infrastructure.db.core.statistics.battle.StatsResultBattleEntity
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManagerFactory
import org.springframework.stereotype.Component

@Component
class BrawlerPairBattleResultStatsReader(
    private val entityManagerFactory: EntityManagerFactory,
) {
    fun readDistinctEventIds(weekRange: WeekRange): List<Long> {
        val entityManager = entityManagerFactory.createEntityManager()
        entityManager.use { em ->
            return JPAQueryFactory(em)
                .select(statsResultBattleEntity.eventBrawlStarsId)
                .distinct()
                .from(statsResultBattleEntity)
                .where(statsResultBattleEntity.battleDate.between(weekRange.start, weekRange.end))
                .fetch()
                .toList()
        }
    }

    fun read(
        weekRange: WeekRange,
        eventBrawlStarsId: Long,
        consume: (List<StatsResultBattleEntity>) -> Unit,
    ): Long {
        val entityManager = entityManagerFactory.createEntityManager()

        entityManager.use { em ->
            val queryFactory = JPAQueryFactory(em)
            var lastId = 0L
            var totalRead = 0L

            while (true) {
                em.clear()

                val battles =
                    queryFactory
                        .selectFrom(statsResultBattleEntity)
                        .where(
                            statsResultBattleEntity.battleDate.between(weekRange.start, weekRange.end),
                            statsResultBattleEntity.eventBrawlStarsId.eq(eventBrawlStarsId),
                            statsResultBattleEntity.id.gt(lastId),
                        ).orderBy(statsResultBattleEntity.id.asc())
                        .limit(BattleStatsConstants.READ_PAGE_SIZE)
                        .fetch()

                if (battles.isEmpty()) break
                totalRead += battles.size
                lastId = battles.last().id!!
                consume(battles)
            }

            return totalRead
        }
    }
}
```

### Writer

- [ ] **Step 4.2: `BrawlerPairBattleResultStatsWriter.kt` 생성**

```kotlin
package com.imstargg.core.batch.job.statistics

import com.imstargg.core.batch.domain.BrawlerPairBattleResultStatsAggregator
import com.imstargg.core.enums.WeekRange
import com.imstargg.infrastructure.db.core.statistics.QBrawlerPairBattleResultStatsEntity.Companion.brawlerPairBattleResultStatsEntity
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class BrawlerPairBattleResultStatsWriter(
    private val em: EntityManager,
) {
    @Transactional
    fun write(
        weekRange: WeekRange,
        eventBrawlStarsId: Long,
        aggregator: BrawlerPairBattleResultStatsAggregator,
    ): Long {
        val queryFactory = JPAQueryFactory(em)
        queryFactory
            .delete(brawlerPairBattleResultStatsEntity)
            .where(
                brawlerPairBattleResultStatsEntity.startBattleDate.eq(weekRange.start),
                brawlerPairBattleResultStatsEntity.endBattleDate.eq(weekRange.end),
                brawlerPairBattleResultStatsEntity.eventBrawlStarsId.eq(eventBrawlStarsId),
            ).execute()

        var totalEntities = 0L
        aggregator.flushEntityChunks(BattleStatsConstants.BATCH_SIZE) { batch ->
            batch.forEach { em.persist(it) }
            totalEntities += batch.size
            em.flush()
            em.clear()
        }
        return totalEntities
    }
}
```

### Task

- [ ] **Step 4.3: `BrawlerPairBattleResultStatsTask.kt` 생성**

```kotlin
package com.imstargg.core.batch.job.statistics

import com.imstargg.core.batch.domain.BrawlerPairBattleResultStatsAggregator
import com.imstargg.core.enums.WeekRange
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class BrawlerPairBattleResultStatsTask(
    private val reader: BrawlerPairBattleResultStatsReader,
    private val writer: BrawlerPairBattleResultStatsWriter,
) {
    fun aggregate(weekRanges: List<WeekRange>) {
        log.info("[{}] 주간 범위 {}개 처리 시작", JOB_NAME, weekRanges.size)

        for (weekRange in weekRanges) {
            processWeekRange(weekRange)
        }
    }

    private fun processWeekRange(weekRange: WeekRange) {
        val eventIds = reader.readDistinctEventIds(weekRange)
        log.info(
            "[{}] 이벤트 {}개 처리 시작 - weekRange: {} ~ {}",
            JOB_NAME,
            eventIds.size,
            weekRange.start,
            weekRange.end,
        )

        var totalRead = 0L
        var totalEntities = 0L
        for (eventId in eventIds) {
            val aggregator =
                BrawlerPairBattleResultStatsAggregator(
                    startBattleDate = weekRange.start,
                    endBattleDate = weekRange.end,
                )
            totalRead +=
                reader.read(weekRange, eventId) { battles ->
                    battles.forEach { aggregator.aggregate(it) }
                }
            totalEntities += writer.write(weekRange, eventId, aggregator)
        }

        log.info(
            "[{}] 주간 범위 완료 - weekRange: {} ~ {}, 전투: {}건, 집계: {}건",
            JOB_NAME,
            weekRange.start,
            weekRange.end,
            totalRead,
            totalEntities,
        )
    }

    companion object {
        private const val JOB_NAME = "brawlerPairBattleResultStatsJob"
        private val log = LoggerFactory.getLogger(BrawlerPairBattleResultStatsTask::class.java)
    }
}
```

### Task 단위 테스트

- [ ] **Step 4.4: `BrawlerPairBattleResultStatsTaskTest.kt` 생성**

```kotlin
package com.imstargg.core.batch.job.statistics

import com.imstargg.core.enums.WeekRange
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BrawlerPairBattleResultStatsTaskTest {
    private val reader = mockk<BrawlerPairBattleResultStatsReader>()
    private val writer = mockk<BrawlerPairBattleResultStatsWriter>()
    private val sut = BrawlerPairBattleResultStatsTask(reader, writer)

    @Test
    fun `주간 범위마다 이벤트를 조회해 이벤트별로 집계하고 저장한다`() {
        // given
        val weekRange1 =
            WeekRange(start = LocalDate.of(2026, 4, 28), end = LocalDate.of(2026, 5, 4))
        val weekRange2 =
            WeekRange(start = LocalDate.of(2026, 5, 5), end = LocalDate.of(2026, 5, 11))
        val weekRanges = listOf(weekRange1, weekRange2)

        every { reader.readDistinctEventIds(weekRange1) } returns listOf(10L, 11L)
        every { reader.readDistinctEventIds(weekRange2) } returns listOf(20L)
        every { reader.read(any(), any(), any()) } returns 0L
        every { writer.write(any(), any(), any()) } returns 0L

        // when
        sut.aggregate(weekRanges)

        // then
        verify(exactly = 1) { reader.readDistinctEventIds(weekRange1) }
        verify(exactly = 1) { reader.readDistinctEventIds(weekRange2) }
        verifyOrder {
            reader.read(weekRange1, 10L, any())
            writer.write(weekRange1, 10L, any())
            reader.read(weekRange1, 11L, any())
            writer.write(weekRange1, 11L, any())
            reader.read(weekRange2, 20L, any())
            writer.write(weekRange2, 20L, any())
        }
    }
}
```

- [ ] **Step 4.5: Task 테스트 실행**

Run:
```bash
./gradlew :core:core-batch:test --tests "com.imstargg.core.batch.job.statistics.BrawlerPairBattleResultStatsTaskTest"
```
Expected: PASS

### JobConfig 슬림화

- [ ] **Step 4.6: `BrawlerPairBattleResultStatsJobConfig.kt` 슬림화**

기존 파일을 다음 내용으로 교체:

```kotlin
package com.imstargg.core.batch.job.statistics

import com.imstargg.core.batch.support.parameter.DateJobParameter
import com.imstargg.core.enums.StatsWeekRange
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.job.parameters.DefaultJobParametersValidator
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.Step
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.infrastructure.repeat.RepeatStatus
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager

@Configuration
class BrawlerPairBattleResultStatsJobConfig(
    private val jobRepository: JobRepository,
    private val txManager: PlatformTransactionManager,
    private val task: BrawlerPairBattleResultStatsTask,
) {
    companion object {
        const val JOB_NAME = "brawlerPairBattleResultStatsJob"
        const val DATE_PARAMETER_NAME = "brawlerPairBattleResultStatsJobDateJobParameter"
        private const val STEP_NAME = "brawlerPairBattleResultStatsStep"
    }

    @Bean(DATE_PARAMETER_NAME)
    @JobScope
    fun dateJobParameter() = DateJobParameter()

    @Bean(JOB_NAME)
    fun brawlerPairBattleResultStatsJob(): Job =
        JobBuilder(JOB_NAME, jobRepository)
            .validator(
                DefaultJobParametersValidator(
                    arrayOf(DateJobParameter.KEY),
                    arrayOf(),
                ),
            ).start(brawlerPairBattleResultStatsStep())
            .build()

    @Bean(STEP_NAME)
    @JobScope
    fun brawlerPairBattleResultStatsStep(): Step =
        StepBuilder(STEP_NAME, jobRepository)
            .tasklet({ _, _ ->
                task.aggregate(StatsWeekRange.currentAndPrevious(dateJobParameter().date!!))
                RepeatStatus.FINISHED
            }, txManager)
            .build()
}
```

- [ ] **Step 4.7: 컨텍스트 테스트 통과 확인**

Run:
```bash
./gradlew :core:core-batch:contextTest
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4.8: 린트 적용 & 커밋**

Run:
```bash
./gradlew :core:core-batch:ktlintFormat
```

```bash
git add imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerPairBattleResultStats{Reader,Writer,Task,JobConfig}.kt \
        imstargg-backend/core/core-batch/src/test/kotlin/com/imstargg/core/batch/job/statistics/BrawlerPairBattleResultStatsTaskTest.kt
git commit -m "$(cat <<'EOF'
refactor(core-batch): BrawlerPairBattleResultStatsJob 평탄화

Weekly-Event 잡을 Reader/Writer/Task 로 분리. 한 (weekRange, eventId)
쌍이 한 write 트랜잭션이 되어 기존 단일 거대 tasklet 트랜잭션 해소.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 5: BrawlerPairBattleRankStatsJob 평탄화

**Files:**
- Create: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerPairBattleRankStatsReader.kt`
- Create: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerPairBattleRankStatsWriter.kt`
- Create: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerPairBattleRankStatsTask.kt`
- Create: `imstargg-backend/core/core-batch/src/test/kotlin/com/imstargg/core/batch/job/statistics/BrawlerPairBattleRankStatsTaskTest.kt`
- Modify: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerPairBattleRankStatsJobConfig.kt`

Task 4 와 동일 구조. 차이점: `StatsResultBattleEntity` → `StatsRankBattleEntity`, `BrawlerPairBattleResultStatsAggregator` → `BrawlerPairBattleRankStatsAggregator`, `BrawlerPairBattleResultStatsEntity` → `BrawlerPairBattleRankStatsEntity`.

### Reader

- [ ] **Step 5.1: `BrawlerPairBattleRankStatsReader.kt` 생성**

```kotlin
package com.imstargg.core.batch.job.statistics

import com.imstargg.core.enums.WeekRange
import com.imstargg.infrastructure.db.core.statistics.battle.QStatsRankBattleEntity.Companion.statsRankBattleEntity
import com.imstargg.infrastructure.db.core.statistics.battle.StatsRankBattleEntity
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManagerFactory
import org.springframework.stereotype.Component

@Component
class BrawlerPairBattleRankStatsReader(
    private val entityManagerFactory: EntityManagerFactory,
) {
    fun readDistinctEventIds(weekRange: WeekRange): List<Long> {
        val entityManager = entityManagerFactory.createEntityManager()
        entityManager.use { em ->
            return JPAQueryFactory(em)
                .select(statsRankBattleEntity.eventBrawlStarsId)
                .distinct()
                .from(statsRankBattleEntity)
                .where(statsRankBattleEntity.battleDate.between(weekRange.start, weekRange.end))
                .fetch()
                .toList()
        }
    }

    fun read(
        weekRange: WeekRange,
        eventBrawlStarsId: Long,
        consume: (List<StatsRankBattleEntity>) -> Unit,
    ): Long {
        val entityManager = entityManagerFactory.createEntityManager()

        entityManager.use { em ->
            val queryFactory = JPAQueryFactory(em)
            var lastId = 0L
            var totalRead = 0L

            while (true) {
                em.clear()

                val battles =
                    queryFactory
                        .selectFrom(statsRankBattleEntity)
                        .where(
                            statsRankBattleEntity.battleDate.between(weekRange.start, weekRange.end),
                            statsRankBattleEntity.eventBrawlStarsId.eq(eventBrawlStarsId),
                            statsRankBattleEntity.id.gt(lastId),
                        ).orderBy(statsRankBattleEntity.id.asc())
                        .limit(BattleStatsConstants.READ_PAGE_SIZE)
                        .fetch()

                if (battles.isEmpty()) break
                totalRead += battles.size
                lastId = battles.last().id!!
                consume(battles)
            }

            return totalRead
        }
    }
}
```

### Writer

- [ ] **Step 5.2: `BrawlerPairBattleRankStatsWriter.kt` 생성**

```kotlin
package com.imstargg.core.batch.job.statistics

import com.imstargg.core.batch.domain.BrawlerPairBattleRankStatsAggregator
import com.imstargg.core.enums.WeekRange
import com.imstargg.infrastructure.db.core.statistics.QBrawlerPairBattleRankStatsEntity.Companion.brawlerPairBattleRankStatsEntity
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class BrawlerPairBattleRankStatsWriter(
    private val em: EntityManager,
) {
    @Transactional
    fun write(
        weekRange: WeekRange,
        eventBrawlStarsId: Long,
        aggregator: BrawlerPairBattleRankStatsAggregator,
    ): Long {
        val queryFactory = JPAQueryFactory(em)
        queryFactory
            .delete(brawlerPairBattleRankStatsEntity)
            .where(
                brawlerPairBattleRankStatsEntity.startBattleDate.eq(weekRange.start),
                brawlerPairBattleRankStatsEntity.endBattleDate.eq(weekRange.end),
                brawlerPairBattleRankStatsEntity.eventBrawlStarsId.eq(eventBrawlStarsId),
            ).execute()

        var totalEntities = 0L
        aggregator.flushEntityChunks(BattleStatsConstants.BATCH_SIZE) { batch ->
            batch.forEach { em.persist(it) }
            totalEntities += batch.size
            em.flush()
            em.clear()
        }
        return totalEntities
    }
}
```

### Task

- [ ] **Step 5.3: `BrawlerPairBattleRankStatsTask.kt` 생성**

```kotlin
package com.imstargg.core.batch.job.statistics

import com.imstargg.core.batch.domain.BrawlerPairBattleRankStatsAggregator
import com.imstargg.core.enums.WeekRange
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class BrawlerPairBattleRankStatsTask(
    private val reader: BrawlerPairBattleRankStatsReader,
    private val writer: BrawlerPairBattleRankStatsWriter,
) {
    fun aggregate(weekRanges: List<WeekRange>) {
        log.info("[{}] 주간 범위 {}개 처리 시작", JOB_NAME, weekRanges.size)

        for (weekRange in weekRanges) {
            processWeekRange(weekRange)
        }
    }

    private fun processWeekRange(weekRange: WeekRange) {
        val eventIds = reader.readDistinctEventIds(weekRange)
        log.info(
            "[{}] 이벤트 {}개 처리 시작 - weekRange: {} ~ {}",
            JOB_NAME,
            eventIds.size,
            weekRange.start,
            weekRange.end,
        )

        var totalRead = 0L
        var totalEntities = 0L
        for (eventId in eventIds) {
            val aggregator =
                BrawlerPairBattleRankStatsAggregator(
                    startBattleDate = weekRange.start,
                    endBattleDate = weekRange.end,
                )
            totalRead +=
                reader.read(weekRange, eventId) { battles ->
                    battles.forEach { aggregator.aggregate(it) }
                }
            totalEntities += writer.write(weekRange, eventId, aggregator)
        }

        log.info(
            "[{}] 주간 범위 완료 - weekRange: {} ~ {}, 전투: {}건, 집계: {}건",
            JOB_NAME,
            weekRange.start,
            weekRange.end,
            totalRead,
            totalEntities,
        )
    }

    companion object {
        private const val JOB_NAME = "brawlerPairBattleRankStatsJob"
        private val log = LoggerFactory.getLogger(BrawlerPairBattleRankStatsTask::class.java)
    }
}
```

### Task 단위 테스트

- [ ] **Step 5.4: `BrawlerPairBattleRankStatsTaskTest.kt` 생성**

```kotlin
package com.imstargg.core.batch.job.statistics

import com.imstargg.core.enums.WeekRange
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BrawlerPairBattleRankStatsTaskTest {
    private val reader = mockk<BrawlerPairBattleRankStatsReader>()
    private val writer = mockk<BrawlerPairBattleRankStatsWriter>()
    private val sut = BrawlerPairBattleRankStatsTask(reader, writer)

    @Test
    fun `주간 범위마다 이벤트를 조회해 이벤트별로 집계하고 저장한다`() {
        // given
        val weekRange =
            WeekRange(start = LocalDate.of(2026, 4, 28), end = LocalDate.of(2026, 5, 4))

        every { reader.readDistinctEventIds(weekRange) } returns listOf(10L, 11L)
        every { reader.read(any(), any(), any()) } returns 0L
        every { writer.write(any(), any(), any()) } returns 0L

        // when
        sut.aggregate(listOf(weekRange))

        // then
        verify(exactly = 1) { reader.readDistinctEventIds(weekRange) }
        verifyOrder {
            reader.read(weekRange, 10L, any())
            writer.write(weekRange, 10L, any())
            reader.read(weekRange, 11L, any())
            writer.write(weekRange, 11L, any())
        }
    }
}
```

- [ ] **Step 5.5: Task 테스트 실행**

Run:
```bash
./gradlew :core:core-batch:test --tests "com.imstargg.core.batch.job.statistics.BrawlerPairBattleRankStatsTaskTest"
```
Expected: PASS

### JobConfig 슬림화

- [ ] **Step 5.6: `BrawlerPairBattleRankStatsJobConfig.kt` 슬림화**

기존 파일을 다음 내용으로 교체:

```kotlin
package com.imstargg.core.batch.job.statistics

import com.imstargg.core.batch.support.parameter.DateJobParameter
import com.imstargg.core.enums.StatsWeekRange
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.job.parameters.DefaultJobParametersValidator
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.Step
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.infrastructure.repeat.RepeatStatus
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager

@Configuration
class BrawlerPairBattleRankStatsJobConfig(
    private val jobRepository: JobRepository,
    private val txManager: PlatformTransactionManager,
    private val task: BrawlerPairBattleRankStatsTask,
) {
    companion object {
        const val JOB_NAME = "brawlerPairBattleRankStatsJob"
        const val DATE_PARAMETER_NAME = "brawlerPairBattleRankStatsJobDateJobParameter"
        private const val STEP_NAME = "brawlerPairBattleRankStatsStep"
    }

    @Bean(DATE_PARAMETER_NAME)
    @JobScope
    fun dateJobParameter() = DateJobParameter()

    @Bean(JOB_NAME)
    fun brawlerPairBattleRankStatsJob(): Job =
        JobBuilder(JOB_NAME, jobRepository)
            .validator(
                DefaultJobParametersValidator(
                    arrayOf(DateJobParameter.KEY),
                    arrayOf(),
                ),
            ).start(brawlerPairBattleRankStatsStep())
            .build()

    @Bean(STEP_NAME)
    @JobScope
    fun brawlerPairBattleRankStatsStep(): Step =
        StepBuilder(STEP_NAME, jobRepository)
            .tasklet({ _, _ ->
                task.aggregate(StatsWeekRange.currentAndPrevious(dateJobParameter().date!!))
                RepeatStatus.FINISHED
            }, txManager)
            .build()
}
```

- [ ] **Step 5.7: 컨텍스트 테스트 통과 확인**

Run:
```bash
./gradlew :core:core-batch:contextTest
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5.8: 린트 적용 & 커밋**

Run:
```bash
./gradlew :core:core-batch:ktlintFormat
```

```bash
git add imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerPairBattleRankStats{Reader,Writer,Task,JobConfig}.kt \
        imstargg-backend/core/core-batch/src/test/kotlin/com/imstargg/core/batch/job/statistics/BrawlerPairBattleRankStatsTaskTest.kt
git commit -m "$(cat <<'EOF'
refactor(core-batch): BrawlerPairBattleRankStatsJob 평탄화

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 6: BrawlerEnemyBattleResultStatsJob 평탄화

**Files:**
- Create: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerEnemyBattleResultStatsReader.kt`
- Create: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerEnemyBattleResultStatsWriter.kt`
- Create: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerEnemyBattleResultStatsTask.kt`
- Create: `imstargg-backend/core/core-batch/src/test/kotlin/com/imstargg/core/batch/job/statistics/BrawlerEnemyBattleResultStatsTaskTest.kt`
- Modify: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerEnemyBattleResultStatsJobConfig.kt`

Task 4 와 동일 구조 (Weekly-Event, `StatsResultBattleEntity` 입력). 차이점: `BrawlerPairBattleResultStatsAggregator` → `BrawlerEnemyBattleResultStatsAggregator`, `BrawlerPairBattleResultStatsEntity` → `BrawlerEnemyBattleResultStatsEntity`.

### Reader

- [ ] **Step 6.1: `BrawlerEnemyBattleResultStatsReader.kt` 생성**

```kotlin
package com.imstargg.core.batch.job.statistics

import com.imstargg.core.enums.WeekRange
import com.imstargg.infrastructure.db.core.statistics.battle.QStatsResultBattleEntity.Companion.statsResultBattleEntity
import com.imstargg.infrastructure.db.core.statistics.battle.StatsResultBattleEntity
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManagerFactory
import org.springframework.stereotype.Component

@Component
class BrawlerEnemyBattleResultStatsReader(
    private val entityManagerFactory: EntityManagerFactory,
) {
    fun readDistinctEventIds(weekRange: WeekRange): List<Long> {
        val entityManager = entityManagerFactory.createEntityManager()
        entityManager.use { em ->
            return JPAQueryFactory(em)
                .select(statsResultBattleEntity.eventBrawlStarsId)
                .distinct()
                .from(statsResultBattleEntity)
                .where(statsResultBattleEntity.battleDate.between(weekRange.start, weekRange.end))
                .fetch()
                .toList()
        }
    }

    fun read(
        weekRange: WeekRange,
        eventBrawlStarsId: Long,
        consume: (List<StatsResultBattleEntity>) -> Unit,
    ): Long {
        val entityManager = entityManagerFactory.createEntityManager()

        entityManager.use { em ->
            val queryFactory = JPAQueryFactory(em)
            var lastId = 0L
            var totalRead = 0L

            while (true) {
                em.clear()

                val battles =
                    queryFactory
                        .selectFrom(statsResultBattleEntity)
                        .where(
                            statsResultBattleEntity.battleDate.between(weekRange.start, weekRange.end),
                            statsResultBattleEntity.eventBrawlStarsId.eq(eventBrawlStarsId),
                            statsResultBattleEntity.id.gt(lastId),
                        ).orderBy(statsResultBattleEntity.id.asc())
                        .limit(BattleStatsConstants.READ_PAGE_SIZE)
                        .fetch()

                if (battles.isEmpty()) break
                totalRead += battles.size
                lastId = battles.last().id!!
                consume(battles)
            }

            return totalRead
        }
    }
}
```

### Writer

- [ ] **Step 6.2: `BrawlerEnemyBattleResultStatsWriter.kt` 생성**

```kotlin
package com.imstargg.core.batch.job.statistics

import com.imstargg.core.batch.domain.BrawlerEnemyBattleResultStatsAggregator
import com.imstargg.core.enums.WeekRange
import com.imstargg.infrastructure.db.core.statistics.QBrawlerEnemyBattleResultStatsEntity.Companion.brawlerEnemyBattleResultStatsEntity
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class BrawlerEnemyBattleResultStatsWriter(
    private val em: EntityManager,
) {
    @Transactional
    fun write(
        weekRange: WeekRange,
        eventBrawlStarsId: Long,
        aggregator: BrawlerEnemyBattleResultStatsAggregator,
    ): Long {
        val queryFactory = JPAQueryFactory(em)
        queryFactory
            .delete(brawlerEnemyBattleResultStatsEntity)
            .where(
                brawlerEnemyBattleResultStatsEntity.startBattleDate.eq(weekRange.start),
                brawlerEnemyBattleResultStatsEntity.endBattleDate.eq(weekRange.end),
                brawlerEnemyBattleResultStatsEntity.eventBrawlStarsId.eq(eventBrawlStarsId),
            ).execute()

        var totalEntities = 0L
        aggregator.flushEntityChunks(BattleStatsConstants.BATCH_SIZE) { batch ->
            batch.forEach { em.persist(it) }
            totalEntities += batch.size
            em.flush()
            em.clear()
        }
        return totalEntities
    }
}
```

### Task

- [ ] **Step 6.3: `BrawlerEnemyBattleResultStatsTask.kt` 생성**

```kotlin
package com.imstargg.core.batch.job.statistics

import com.imstargg.core.batch.domain.BrawlerEnemyBattleResultStatsAggregator
import com.imstargg.core.enums.WeekRange
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class BrawlerEnemyBattleResultStatsTask(
    private val reader: BrawlerEnemyBattleResultStatsReader,
    private val writer: BrawlerEnemyBattleResultStatsWriter,
) {
    fun aggregate(weekRanges: List<WeekRange>) {
        log.info("[{}] 주간 범위 {}개 처리 시작", JOB_NAME, weekRanges.size)

        for (weekRange in weekRanges) {
            processWeekRange(weekRange)
        }
    }

    private fun processWeekRange(weekRange: WeekRange) {
        val eventIds = reader.readDistinctEventIds(weekRange)
        log.info(
            "[{}] 이벤트 {}개 처리 시작 - weekRange: {} ~ {}",
            JOB_NAME,
            eventIds.size,
            weekRange.start,
            weekRange.end,
        )

        var totalRead = 0L
        var totalEntities = 0L
        for (eventId in eventIds) {
            val aggregator =
                BrawlerEnemyBattleResultStatsAggregator(
                    startBattleDate = weekRange.start,
                    endBattleDate = weekRange.end,
                )
            totalRead +=
                reader.read(weekRange, eventId) { battles ->
                    battles.forEach { aggregator.aggregate(it) }
                }
            totalEntities += writer.write(weekRange, eventId, aggregator)
        }

        log.info(
            "[{}] 주간 범위 완료 - weekRange: {} ~ {}, 전투: {}건, 집계: {}건",
            JOB_NAME,
            weekRange.start,
            weekRange.end,
            totalRead,
            totalEntities,
        )
    }

    companion object {
        private const val JOB_NAME = "brawlerEnemyBattleResultStatsJob"
        private val log = LoggerFactory.getLogger(BrawlerEnemyBattleResultStatsTask::class.java)
    }
}
```

### Task 단위 테스트

- [ ] **Step 6.4: `BrawlerEnemyBattleResultStatsTaskTest.kt` 생성**

```kotlin
package com.imstargg.core.batch.job.statistics

import com.imstargg.core.enums.WeekRange
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BrawlerEnemyBattleResultStatsTaskTest {
    private val reader = mockk<BrawlerEnemyBattleResultStatsReader>()
    private val writer = mockk<BrawlerEnemyBattleResultStatsWriter>()
    private val sut = BrawlerEnemyBattleResultStatsTask(reader, writer)

    @Test
    fun `주간 범위마다 이벤트를 조회해 이벤트별로 집계하고 저장한다`() {
        // given
        val weekRange =
            WeekRange(start = LocalDate.of(2026, 4, 28), end = LocalDate.of(2026, 5, 4))

        every { reader.readDistinctEventIds(weekRange) } returns listOf(10L, 11L)
        every { reader.read(any(), any(), any()) } returns 0L
        every { writer.write(any(), any(), any()) } returns 0L

        // when
        sut.aggregate(listOf(weekRange))

        // then
        verify(exactly = 1) { reader.readDistinctEventIds(weekRange) }
        verifyOrder {
            reader.read(weekRange, 10L, any())
            writer.write(weekRange, 10L, any())
            reader.read(weekRange, 11L, any())
            writer.write(weekRange, 11L, any())
        }
    }
}
```

- [ ] **Step 6.5: Task 테스트 실행**

Run:
```bash
./gradlew :core:core-batch:test --tests "com.imstargg.core.batch.job.statistics.BrawlerEnemyBattleResultStatsTaskTest"
```
Expected: PASS

### JobConfig 슬림화

- [ ] **Step 6.6: `BrawlerEnemyBattleResultStatsJobConfig.kt` 슬림화**

기존 파일을 다음 내용으로 교체:

```kotlin
package com.imstargg.core.batch.job.statistics

import com.imstargg.core.batch.support.parameter.DateJobParameter
import com.imstargg.core.enums.StatsWeekRange
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.job.parameters.DefaultJobParametersValidator
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.Step
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.infrastructure.repeat.RepeatStatus
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager

@Configuration
class BrawlerEnemyBattleResultStatsJobConfig(
    private val jobRepository: JobRepository,
    private val txManager: PlatformTransactionManager,
    private val task: BrawlerEnemyBattleResultStatsTask,
) {
    companion object {
        const val JOB_NAME = "brawlerEnemyBattleResultStatsJob"
        const val DATE_PARAMETER_NAME = "brawlerEnemyBattleResultStatsJobDateJobParameter"
        private const val STEP_NAME = "brawlerEnemyBattleResultStatsStep"
    }

    @Bean(DATE_PARAMETER_NAME)
    @JobScope
    fun dateJobParameter() = DateJobParameter()

    @Bean(JOB_NAME)
    fun brawlerEnemyBattleResultStatsJob(): Job =
        JobBuilder(JOB_NAME, jobRepository)
            .validator(
                DefaultJobParametersValidator(
                    arrayOf(DateJobParameter.KEY),
                    arrayOf(),
                ),
            ).start(brawlerEnemyBattleResultStatsStep())
            .build()

    @Bean(STEP_NAME)
    @JobScope
    fun brawlerEnemyBattleResultStatsStep(): Step =
        StepBuilder(STEP_NAME, jobRepository)
            .tasklet({ _, _ ->
                task.aggregate(StatsWeekRange.currentAndPrevious(dateJobParameter().date!!))
                RepeatStatus.FINISHED
            }, txManager)
            .build()
}
```

- [ ] **Step 6.7: 컨텍스트 테스트 통과 확인**

Run:
```bash
./gradlew :core:core-batch:contextTest
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6.8: 린트 적용 & 커밋**

Run:
```bash
./gradlew :core:core-batch:ktlintFormat
```

```bash
git add imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerEnemyBattleResultStats{Reader,Writer,Task,JobConfig}.kt \
        imstargg-backend/core/core-batch/src/test/kotlin/com/imstargg/core/batch/job/statistics/BrawlerEnemyBattleResultStatsTaskTest.kt
git commit -m "$(cat <<'EOF'
refactor(core-batch): BrawlerEnemyBattleResultStatsJob 평탄화

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 7: 공통 집계 task 제거

5개 잡이 모두 새 구조로 전환되었으므로 `DailyBattleStatsAggregationTask`, `WeeklyEventBattleStatsAggregationTask` 와 관련 테스트를 삭제한다.

**Files:**
- Delete: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/task/DailyBattleStatsAggregationTask.kt`
- Delete: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/task/WeeklyEventBattleStatsAggregationTask.kt`
- Delete: `imstargg-backend/core/core-batch/src/test/kotlin/com/imstargg/core/batch/job/statistics/task/DailyBattleStatsAggregationTaskTest.kt`
- Delete: `imstargg-backend/core/core-batch/src/test/kotlin/com/imstargg/core/batch/job/statistics/task/WeeklyEventBattleStatsAggregationTaskTest.kt`

- [ ] **Step 7.1: 참조 확인**

Run:
```bash
grep -rn "DailyBattleStatsAggregationTask\|WeeklyEventBattleStatsAggregationTask" imstargg-backend/core/core-batch/src/
```
Expected: 4개 파일 (위 4개) 외에 다른 참조가 없어야 한다. 만약 추가 참조가 있으면 그 파일도 같이 정리한다.

- [ ] **Step 7.2: 파일 삭제**

Run:
```bash
git rm imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/task/DailyBattleStatsAggregationTask.kt \
       imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/task/WeeklyEventBattleStatsAggregationTask.kt \
       imstargg-backend/core/core-batch/src/test/kotlin/com/imstargg/core/batch/job/statistics/task/DailyBattleStatsAggregationTaskTest.kt \
       imstargg-backend/core/core-batch/src/test/kotlin/com/imstargg/core/batch/job/statistics/task/WeeklyEventBattleStatsAggregationTaskTest.kt
```

- [ ] **Step 7.3: 빈 디렉토리 확인 & 제거**

Run:
```bash
ls imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/task/ 2>/dev/null
ls imstargg-backend/core/core-batch/src/test/kotlin/com/imstargg/core/batch/job/statistics/task/ 2>/dev/null
```

Expected: 디렉토리가 비어있으면 다음 명령으로 제거:
```bash
rmdir imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/task/ 2>/dev/null || true
rmdir imstargg-backend/core/core-batch/src/test/kotlin/com/imstargg/core/batch/job/statistics/task/ 2>/dev/null || true
```

(`task` 디렉토리에 다른 파일이 남아 있다면 무시 — `StatsPlayerSyncFromRankingTask` 가 같은 task 디렉토리에 있는지 확인 필요)

- [ ] **Step 7.4: 전체 빌드 통과 확인**

Run:
```bash
./gradlew :core:core-batch:build -x test
```
Expected: BUILD SUCCESSFUL. 컴파일 오류 없음.

- [ ] **Step 7.5: 전체 테스트 통과 확인**

Run:
```bash
./gradlew :core:core-batch:test
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7.6: 커밋**

```bash
git add -A
git commit -m "$(cat <<'EOF'
refactor(core-batch): 사용하지 않는 공통 집계 task 제거

5개 통계 잡 평탄화 완료로 DailyBattleStatsAggregationTask 와
WeeklyEventBattleStatsAggregationTask 사용처가 사라져 제거.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 8: BrawlerSyncTask 헬퍼 추출

`detectBrawlerChanges` 의 4번 동일 구조 반복을 작은 헬퍼로 정리. 브롤러 item response 타입들(`StarPowerResponse`, `AccessoryResponse`, `HyperChargeResponse`, `GearStatResponse`)이 공통 base 가 없으므로 generic + selector 함수로 추출.

**Files:**
- Modify: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/brawlstars/task/BrawlerSyncTask.kt`

- [ ] **Step 8.1: 기존 테스트 확인**

`BrawlerSyncTask` 에 대한 직접 단위 테스트는 없고 `BrawlerChangeDetector` 테스트만 있다. 이번 리팩터링은 호출부 형태만 바꾸는 것이라 기존 테스트로 회귀가 잡히지 않음. **컨텍스트 테스트와 다음 단계 코드 리뷰로 검증.**

Run:
```bash
find imstargg-backend/core/core-batch/src/test/kotlin -name "BrawlerSyncTask*"
```
Expected: 결과 없음 (단위 테스트 없음)

- [ ] **Step 8.2: `BrawlerSyncTask.kt` 리팩터링**

`detectBrawlerChanges` 메서드를 다음과 같이 헬퍼 기반으로 교체. `jsonMapper` 는 메서드 진입 시 1회 생성 (기존도 같음).

기존 `detectBrawlerChanges` 와 `alertNewBrawlers` 의 `jsonMapper` 생성을 클래스 최상단 companion 또는 메서드 로컬에 유지. 아래는 메서드 로컬 유지 버전:

```kotlin
private fun detectBrawlerChanges(existingBrawlers: List<BrawlerResponse>): List<BrawlerChange> {
    val jsonMapper = JsonMapper.builder().build()
    val changes = mutableListOf<BrawlerChange>()

    for (brawler in existingBrawlers) {
        changes += detectFor(
            brawler = brawler,
            jsonMapper = jsonMapper,
            itemType = BrawlerChangeItemType.STAR_POWER,
            apiItems = brawler.starPowers,
            apiItemId = { it.id },
            dbItemIds = starPowerJpaRepository
                .findAllByBrawlerBrawlStarsId(brawler.id)
                .map { it.brawlStarsId }
                .toSet(),
        )

        changes += detectFor(
            brawler = brawler,
            jsonMapper = jsonMapper,
            itemType = BrawlerChangeItemType.GADGET,
            apiItems = brawler.gadgets,
            apiItemId = { it.id },
            dbItemIds = gadgetJpaRepository
                .findAllByBrawlerBrawlStarsId(brawler.id)
                .map { it.brawlStarsId }
                .toSet(),
        )

        changes += detectFor(
            brawler = brawler,
            jsonMapper = jsonMapper,
            itemType = BrawlerChangeItemType.HYPER_CHARGE,
            apiItems = brawler.hyperCharges,
            apiItemId = { it.id },
            dbItemIds = hyperChargeJpaRepository
                .findAllByBrawlerBrawlStarsId(brawler.id)
                .map { it.brawlStarsId }
                .toSet(),
        )

        changes += detectFor(
            brawler = brawler,
            jsonMapper = jsonMapper,
            itemType = BrawlerChangeItemType.GEAR,
            apiItems = brawler.gears,
            apiItemId = { it.id },
            dbItemIds = brawlerGearJpaRepository
                .findAllByBrawlerBrawlStarsId(brawler.id)
                .map { it.gearBrawlStarsId }
                .toSet(),
        )
    }

    return changes
}

private fun <T> detectFor(
    brawler: BrawlerResponse,
    jsonMapper: JsonMapper,
    itemType: BrawlerChangeItemType,
    apiItems: List<T>,
    apiItemId: (T) -> Long,
    dbItemIds: Set<Long>,
): List<BrawlerChange> =
    changeDetector.detectItemChanges(
        brawlerId = brawler.id,
        brawlerName = brawler.name,
        itemType = itemType,
        apiItemIds = apiItems.map(apiItemId).toSet(),
        dbItemIds = dbItemIds,
        toJson = { id -> jsonMapper.writeValueAsString(apiItems.first { apiItemId(it) == id }) },
    )
```

`alertNewBrawlers`, `alertDeletedBrawlers`, `alertBrawlerChanges` 와 그 외 메서드는 그대로 유지.

- [ ] **Step 8.3: 컴파일 통과 확인**

Run:
```bash
./gradlew :core:core-batch:build -x test
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 8.4: 전체 테스트 통과 확인**

Run:
```bash
./gradlew :core:core-batch:test
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 8.5: 린트 적용 & 커밋**

Run:
```bash
./gradlew :core:core-batch:ktlintFormat
```

```bash
git add imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/brawlstars/task/BrawlerSyncTask.kt
git commit -m "$(cat <<'EOF'
refactor(core-batch): BrawlerSyncTask 의 item 변경 감지 헬퍼 추출

STAR_POWER/GADGET/HYPER_CHARGE/GEAR 4번 반복하던 detectItemChanges
호출을 detectFor 헬퍼로 정리. 각 호출이 어떤 itemType/repository 를
쓰는지는 그대로 호출부에 보인다.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 9: 최종 회귀 검증

모든 변경 통합 후 전체 모듈 회귀.

- [ ] **Step 9.1: 단위 테스트 통과 확인**

Run:
```bash
./gradlew :core:core-batch:unitTest
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 9.2: 컨텍스트 테스트 통과 확인**

Run:
```bash
./gradlew :core:core-batch:contextTest
```
Expected: BUILD SUCCESSFUL. `CoreBatchContextTest` 가 5개 신규 Task, 5개 신규 Reader, 5개 신규 Writer, 5개 슬림 JobConfig 모두 로딩 성공.

- [ ] **Step 9.3: 전체 빌드 통과 확인**

Run:
```bash
./gradlew :core:core-batch:build
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 9.4: 린트 통과 확인**

Run:
```bash
./gradlew :core:core-batch:ktlintCheck
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 9.5: 평탄화 결과 점검 (육안 확인)**

5개 슬림 JobConfig 의 길이를 확인:

```bash
wc -l imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/Brawler{BattleResult,BattleRank,PairBattleResult,PairBattleRank,EnemyBattleResult}StatsJobConfig.kt
```
Expected: 각 50줄 내외. 비즈니스 로직(쿼리/람다) 없음.

`task/` 디렉토리가 깨끗한지 확인:
```bash
ls imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/task/ 2>/dev/null
```
Expected: 디렉토리가 없거나 비어있음. (구현 중 `task/` 디렉토리 안에 다른 잡의 Task 가 들어가 있지 않다는 가정)

- [ ] **Step 9.6: 변경 로그 확인 & 최종 push 준비**

Run:
```bash
git log --oneline origin/main..HEAD
```
Expected: spec 1개 + plan 1개 + Task 2~6 (5개) + Task 7~8 (2개) = 약 9개 커밋.

---

## Self-Review Notes

- [x] 모든 spec 요구사항이 task 에 매핑됨:
  - § 4.1 Daily Result/Rank → Task 2, 3
  - § 4.2 Weekly-Event Pair Result/Pair Rank/Enemy Result → Task 4, 5, 6
  - § 5.1 BrawlerSyncTask 헬퍼 → Task 8
  - 공통 task 제거 → Task 7
  - 최종 회귀 → Task 9
- [x] 모든 step 이 실행 가능한 코드/명령을 포함
- [x] 타입/시그니처 일관성: 5개 Writer 의 `write` 시그니처 (Daily/Weekly-Event 두 모양), 5개 Reader 의 `read` 시그니처, 5개 Task 의 `aggregate` 시그니처 모두 통일된 모양
- [x] 데드코드 보존 항목 (`QuerydslZeroPagingItemReader` 등) 은 plan 에서 건드리지 않음
- [x] BrawlerEnemyBattleResultStatsJob 의 `persistAdditional` 은 실 사용처가 없어 신규 writer 에서 단순 제거 (이 점 spec 의 § 4.2 "persistAdditional 흡수" 보다 더 정확)
