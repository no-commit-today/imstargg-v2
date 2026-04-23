# core-batch 데이터 정확성 번들 설계서

- 작성일: 2026-04-23
- 대상 모듈: `imstargg-backend/core/core-batch`
- 선행 문서: core-batch 코드 리뷰 최종 보고서 (2026-04-23)

## 1. 목적

core-batch 코드 리뷰 결과 Critical·High 로 분류된 **데이터 정확성** 이슈를 한 묶음으로 해결한다. 팀 통계 메모리 스트리밍 등의 구조적 재설계(스펙 B) 이전에, **집계 수치 자체가 틀려 있을 수 있는 문제를 먼저 바로잡고 멱등성/원자성을 확보**하는 것이 이 스펙의 목표다.

## 2. 범위

다음 이슈를 포함한다.

| #   | 심각도      | 이슈                                                                                          |
| --- | ----------- | --------------------------------------------------------------------------------------------- |
| 1-A | 🔴 Critical | `stats_rank_battle` 기반 rank 통계의 팀 크기 편향 중복 집계                                   |
| 1-B | 🔴 Critical | 수동 `EntityManager` 트랜잭션과 Spring Batch step 트랜잭션 분리로 인한 부분 실패 시 깨진 상태 |
| 2-B | 🟠 High     | `deleteExisting` → `persist` 사이 수십 초 공백에 외부 API가 빈 통계 관측                      |
| 2-C | 🟠 High     | 집계 테이블에 자연키 unique 제약 없음                                                         |
| 3-A | 🟡 Medium   | 통계/파티션 잡의 `RunIdIncrementer` 누락으로 동일 `date` 재실행 불가                          |

다음은 **포함하지 않는다** (별도 스펙).

- 팀 통계 메모리 스트리밍, chunk-oriented step 전환 → **스펙 B**
- 브롤스타즈 API 실패 삼킴, `orderBy` 누락, 알림 누락 → **스펙 C**
- `BrawlerItemOwnershipStatsJob`, `BrawlerSyncJob`, 파티션 매니지 개선

## 3. 대상 파일 맵

### 3.1 변경 대상

| 경로                                                                                                   | 변경 성격                      |
| ------------------------------------------------------------------------------------------------------ | ------------------------------ |
| `core/core-batch/src/main/kotlin/com/imstargg/core/batch/domain/BrawlerBattleRankStatsAggregator.kt`   | 집계 규칙 수정 (1-A)           |
| `core/core-batch/src/main/kotlin/com/imstargg/core/batch/domain/BrawlerTeamBattleRankStatsAggregator.kt` | 집계 규칙 수정 (1-A)           |
| `core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/DailyBattleStatsTasklet.kt`    | 트랜잭션 구조 변경 (1-B, 2-B)  |
| `core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/PerEventDailyBattleStatsTasklet.kt` | 트랜잭션 구조 변경 (1-B, 2-B)  |
| `core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/PerEventWeeklyBattleStatsTasklet.kt` | 트랜잭션 구조 변경 (1-B, 2-B)  |
| `core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerBattleRankStatsJobConfig.kt` | `RunIdIncrementer` 추가 (3-A) |
| `core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerBattleResultStatsJobConfig.kt` | `RunIdIncrementer` 추가 (3-A) |
| `core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerTeamBattleRankStatsJobConfig.kt` | `RunIdIncrementer` 추가 (3-A) |
| `core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerTeamBattleResultStatsJobConfig.kt` | `RunIdIncrementer` 추가 (3-A) |
| `core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerEnemyBattleResultStatsJobConfig.kt` | `RunIdIncrementer` 추가 (3-A) |
| `core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerItemOwnershipStatsJobConfig.kt` | `RunIdIncrementer` 추가 (3-A) |
| `core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/PartitionManageJobConfig.kt`              | `RunIdIncrementer` 추가 (3-A) |
| `infrastructure/db-core/src/main/resources/schema.sql`                                                 | 자연키 unique 인덱스 추가 (2-C) |
| `infrastructure/db-core/src/main/resources/db/migration/V260423__stats_natural_key_unique.sql`         | 신규 마이그레이션 (2-C)        |

### 3.2 테스트 추가/확장 대상

| 경로                                                                                                        | 내용                                    |
| ----------------------------------------------------------------------------------------------------------- | --------------------------------------- |
| `core/core-batch/src/test/kotlin/com/imstargg/core/batch/domain/BrawlerBattleRankStatsAggregatorTest.kt`    | 신규: 1-A 재현 및 수정 검증             |
| `core/core-batch/src/test/kotlin/com/imstargg/core/batch/domain/BrawlerTeamBattleRankStatsAggregatorTest.kt` | 신규: 1-A 재현 및 수정 검증             |
| `core/core-batch/src/test/kotlin/com/imstargg/core/batch/job/statistics/*StatsJobConfigContextTest.kt`      | 신규 또는 확장: 트랜잭션 원자성 통합 테스트 |

## 4. 이슈별 설계

### 4.1 (1-A) Rank 통계 중복 집계 수정

#### 문제 요약

`stats_rank_battle`는 unique key `(battle_date, battle_key, player_brawlstars_tag)`이다. 한 랭크 전투가 **같은 팀에 속한 플레이어 수만큼 row**로 저장된다(트리오 랭크면 1 battle → 최대 3 rows). `BaseStatsBattleEntity.playerTeams()`는 "본인 플레이어의 팀"만 필터링해 반환하지만, 이 "본인 팀" 자체가 3개 row에 동일하게 실려 있어 aggregator가 순회할 때 **같은 팀이 3회 집계**된다.

#### 수정 방침

- **개인 브롤러 rank 통계 (`BrawlerBattleRankStatsAggregator`)**  
  row 당 "본인 플레이어 한 명"만 집계한다. 즉 `playerBrawlStarsTag`에 해당하는 플레이어를 `teams`에서 찾아 그 한 명의 브롤러/트로피 기준으로만 키를 만든다. 이 방식은 row마다 자연스럽게 1명씩 집계되므로 HashSet dedup이 불필요하고 메모리에도 유리하다.

- **팀 rank 통계 (`BrawlerTeamBattleRankStatsAggregator`)**  
  팀 조합(2·3조합)은 전체 팀을 봐야 하므로 row 단위로 "처음 본 `battleKey`일 때만" 집계한다. aggregator 내부에 `HashSet<String> processedBattleKeys`를 두고 첫 row에서만 집계.  
  - HashSet 크기 한도: "주간 × 단일 이벤트" 범위의 고유 `battle_key` 수. 리뷰 보고서의 실측 SQL로 운영 수치 확인 후 스펙 B의 스트리밍 리팩토링 단계에서 구조적으로 제거. 본 스펙에서는 정확성을 우선하고 메모리 risk는 수용.
  - `processedBattleKeys`는 `aggregatorFactory(weekRange)`가 이벤트별로 새 인스턴스를 만들어주므로 이벤트 간에는 공유되지 않는다. 현재 tasklet 구조 유지.

#### 코드 스케치

`BrawlerBattleRankStatsAggregator`:

```kotlin
override fun aggregate(battle: StatsRankBattleEntity) {
    val battleType = EnumCodeType.findByCode<BattleType>(battle.type) ?: return

    val me = battle.playerTeams()
        .asSequence()
        .flatten()
        .firstOrNull { it.brawlStarsTag == battle.playerBrawlStarsTag }
        ?: return

    val tierRanges = StatsTierRange.resolveAll(battleType, me.brawler.trophies)
    for (tierRange in tierRanges) {
        val key = BrawlerBattleStatsKey(
            brawlerBrawlStarsId = me.brawler.brawlStarsId,
            eventBrawlStarsId = battle.eventBrawlStarsId,
            tierRange = tierRange,
            battleDate = battle.battleDate,
        )
        rankCounts.add(key, battle.battleRank)
    }
}
```

`BrawlerTeamBattleRankStatsAggregator`:

```kotlin
private val processedBattleKeys = HashSet<String>()

override fun aggregate(battle: StatsRankBattleEntity) {
    if (!processedBattleKeys.add(battle.battleKey)) return
    // 기존 로직 유지
}
```

#### 검증

- 유닛 테스트: 같은 `battleKey`를 가진 row를 2·3개 공급하고, 기대 집계 결과가 "1 battle 단위"임을 검증.
- 기존 테스트 회귀 없음 확인.
- 수정 전/후 비교 쿼리 (운영 재집계 시):
  ```sql
  -- 수정 후 재집계 실행 → 이 값이 수정 전 수치보다 작아야 정상
  SELECT sum(rank_1_count + rank_2_count + ...) FROM brawler_team_battle_rank_stats WHERE ...;
  ```

#### 운영 반영

- 본 변경 배포 후 **과거 rank 통계 재집계 필요**. 재집계 범위: `StatsWeekRange.currentAndPrevious`가 커버하는 최근 2주. 3-A의 `RunIdIncrementer` 적용 후 과거 `date`로 재실행.

---

### 4.2 (1-B, 2-B) 트랜잭션 원자화

#### 문제 요약

세 tasklet(`DailyBattleStatsTasklet`, `PerEventDailyBattleStatsTasklet`, `PerEventWeeklyBattleStatsTasklet`)은 Spring Batch가 `txManager`로 트랜잭션을 열어주지만, 내부에서 `entityManagerFactory.createEntityManager()`로 별도 application-managed EM을 만들고 수동 `em.transaction.begin()/commit()`으로 DML을 커밋한다. 결과:

1. `deleteExisting` 커밋 이후 `persist` chunk가 N번째에서 실패하면 롤백되지 않음 → **빈/부분 통계 상태 잔존**.
2. `deleteExisting`와 첫 `persist` 커밋 사이에 수십 초~수 분 간격 발생 → **외부 조회 API가 빈 통계 관측**.
3. 수동 EM과 Spring EM이 동시에 커넥션을 점유 → HikariCP 풀 고갈 가능.

#### 수정 방침

tasklet을 "chunk-oriented step"으로 전환하는 것은 스펙 B에서 수행하는 큰 리팩토링이다. 본 스펙에서는 **현 tasklet 구조를 유지하되, "이벤트 1개(혹은 날짜 1개)의 delete + persist 전체"를 하나의 Spring-managed 트랜잭션으로 묶는다**.

구체적으로:

- `entityManagerFactory.createEntityManager()` 및 수동 `em.transaction.*` 호출을 제거한다.
- tasklet에서 실제 DML을 수행하는 부분을 별도의 `@Transactional` 메서드가 있는 Spring bean(예: `PerEventWeeklyStatsWriter`)으로 추출한다. 또는 `TransactionTemplate`를 주입받아 이벤트 단위로 `execute { ... }` 블록으로 감싼다.
- 권장: `TransactionTemplate` 방식(추가 빈 정의 최소화).

#### 트랜잭션 경계

- `DailyBattleStatsTasklet`: `deleteExisting(date)` + 전체 `persist` 을 **하나의 트랜잭션**. (범위: 날짜 1건)
- `PerEventDailyBattleStatsTasklet`: `deleteExisting(date, eventId)` + 해당 이벤트의 전체 `persist` 을 **이벤트별 트랜잭션**. (범위: 날짜 1개 × 이벤트 1개)
- `PerEventWeeklyBattleStatsTasklet`: `deleteExisting(weekRange, eventId)` + 해당 (주간 범위, 이벤트) 의 전체 `persist` 을 **(주간 범위, 이벤트) 조합별 트랜잭션**. (범위: 주간 범위 1개 × 이벤트 1개)

각 트랜잭션 내에서 대용량 데이터의 1차 캐시 누적을 막기 위해 기존 `em.clear()` 호출은 유지한다.

#### 읽기/쓰기 EM 분리

- **읽기 EM**: 트랜잭션 밖에서 배틀을 페이지 단위로 읽는 용도. application-managed `EntityManager` (`entityManagerFactory.createEntityManager()`) 를 사용하되 수동 트랜잭션은 열지 않는다. 읽기 중 `em.clear()` 로 1차 캐시 누적을 막는 기존 패턴 유지.
- **쓰기 EM**: 실제 DML 을 수행하는 용도. **반드시 Spring-managed** 이어야 한다. 이는 협력자 빈의 `@Transactional` 메서드 안에서 `@PersistenceContext` 로 주입받은 EM 을 사용하거나, `TransactionTemplate.execute { ... }` 안에서 `SharedEntityManagerCreator` 를 통해 얻은 EM 을 사용해 달성한다. `entityManagerFactory.createEntityManager()` 를 트랜잭션 내부에서 직접 호출하면 application-managed 로 남아 Spring 트랜잭션에 묶이지 않으므로 **금지**.

플랜 단계에서 두 옵션(`TransactionTemplate` vs 협력자 빈의 `@Transactional`) 중 하나를 선택한다. 기본 추천은 **협력자 빈의 `@Transactional` 메서드** — 쓰기 EM 을 `@PersistenceContext` 로 자연스럽게 얻을 수 있기 때문.

#### 읽기 경로 분리

- 읽기(page-by-page)는 `JPAQueryFactory`로 그대로 수행하되, **트랜잭션 안에서 수행할 필요가 없다**. 트랜잭션은 write(delete + persist) 구간만 감싼다.
- 읽기 단계는 Spring-managed EM의 `@Transactional(readOnly = true)` 또는 아예 트랜잭션 없이(`JpaTransactionManager` 기본 동작 하에 auto-commit read) 수행. 현재는 수동 EM 기반이므로 읽기용 EM도 Spring `EntityManagerFactory` 주입 후 `createEntityManager()` 허용 — 단 수동 트랜잭션은 열지 않는다.

#### 실패 시 동작

- 트랜잭션 내에서 예외 발생 시 delete + persist 모두 롤백 → 해당 이벤트 통계는 **이전 상태 그대로 유지**.
- step 전체가 실패로 Spring Batch에 보고되고, 재실행 시 `RunIdIncrementer`(3-A) 덕분에 같은 `date` 로 재시도 가능.

#### 코드 스케치

쓰기 전용 협력자 빈을 도입해 `@Transactional` 로 원자화한다.

```kotlin
@Component
class BrawlerTeamBattleStatsWriter(
    @PersistenceContext private val em: EntityManager,
) {
    @Transactional
    fun replace(
        deleteExisting: (JPAQueryFactory) -> Unit,
        entities: List<BrawlerTeamBattleRankStatsEntity>,
        persistAdditional: (EntityManager, BrawlerTeamBattleRankStatsEntity) -> Unit,
    ) {
        val writeQueryFactory = JPAQueryFactory(em)
        deleteExisting(writeQueryFactory)
        entities.chunked(BattleStatsConstants.BATCH_SIZE).forEach { batch ->
            batch.forEach {
                em.persist(it)
                persistAdditional(em, it)
            }
            em.flush()
            em.clear()
        }
    }
}
```

tasklet 은 읽기 EM 만 직접 열고, 쓰기는 writer 빈에 위임한다.

```kotlin
class PerEventWeeklyBattleStatsTasklet<TBattle, TEntity>(
    private val entityManagerFactory: EntityManagerFactory, // 읽기 전용
    private val writer: BrawlerTeamBattleStatsWriter,       // 쓰기 전용 (Spring-managed tx)
    // ... 기존 함수형 의존성
) : Tasklet {
    override fun execute(
        contribution: StepContribution,
        chunkContext: ChunkContext,
    ): RepeatStatus {
        val weekRanges = weekRangesSupplier()
        entityManagerFactory.createEntityManager().use { readEm ->
            val readQueryFactory = JPAQueryFactory(readEm)
            for (weekRange in weekRanges) {
                val eventIds = readDistinctEventIds(readQueryFactory, weekRange)
                for (eventId in eventIds) {
                    val aggregator = aggregatorFactory(weekRange)
                    readBattlesIntoAggregator(readEm, readQueryFactory, weekRange, eventId, aggregator)
                    val entities = aggregator.toEntities()
                    writer.replace(
                        deleteExisting = { qf -> deleteExisting(qf, weekRange, eventId) },
                        entities = entities,
                        persistAdditional = persistAdditional,
                    )
                }
            }
        }
        return RepeatStatus.FINISHED
    }
}
```

> writer 는 통계 타입마다 동일 패턴이므로 제네릭화 가능(예: `StatsReplaceWriter<TEntity>`). 플랜 단계에서 실제 형태를 확정한다.

#### 한계 (의도된 미해결)

- 단일 이벤트의 집계 결과 List가 매우 크면 한 트랜잭션이 길어진다. PostgreSQL WAL·LOCK·트랜잭션 ID wrap-around 관점에서 **한 이벤트의 entity 수가 수백만을 넘으면 부담**이 된다.
  - 현재 관측치 기준(수천~수만 추정, 리뷰 보고서 섹션 2-A)은 감당 가능.
  - 본 스펙에서는 이 tradeoff를 수용하고, 스펙 B의 스트리밍+upsert 전환으로 구조적으로 해결한다.

---

### 4.3 (2-C) 자연키 unique 인덱스 추가

#### 문제 요약

현재 통계 테이블은 application-level "매번 delete + insert" 방어만 존재하며 DB 레벨 unique 제약이 없다. 운영자가 SQL 로 수동 재적재하거나 잡을 두 번 돌리면 DB 가 막아주지 못한다.

#### 추가 인덱스

| 테이블                               | 자연키                                                                                                                                         |
| ------------------------------------ | ---------------------------------------------------------------------------------------------------------------------------------------------- |
| `brawler_battle_result_stats`        | `(event_brawlstars_id, tier_range, battle_date, brawler_brawlstars_id)`                                                                        |
| `brawler_battle_rank_stats`          | `(event_brawlstars_id, tier_range, battle_date, brawler_brawlstars_id)`                                                                        |
| `brawler_team_battle_result_stats`   | `(event_brawlstars_id, tier_range, start_battle_date, end_battle_date, team_key)`                                                              |
| `brawler_team_battle_rank_stats`     | `(event_brawlstars_id, tier_range, start_battle_date, end_battle_date, team_key)`                                                              |
| `brawler_enemy_battle_result_stats`  | `(event_brawlstars_id, tier_range, start_battle_date, end_battle_date, brawler_brawlstars_id, enemy_brawler_brawlstars_id)`                    |

- `team_key` 는 `BrawlerTeamEmbedded.teamKey` 로 이미 정의되어 있는 `varchar(100)` 컬럼이다 (정렬된 브롤러 ID 문자열). 별도 컬럼 신설 없이 바로 unique 인덱스에 사용 가능.

#### 마이그레이션 파일

`infrastructure/db-core/src/main/resources/db/migration/V260423__stats_natural_key_unique.sql`

```sql
create unique index if not exists uk_brawler_battle_result_stats__1
  on brawler_battle_result_stats (event_brawlstars_id, tier_range, battle_date, brawler_brawlstars_id);

create unique index if not exists uk_brawler_battle_rank_stats__1
  on brawler_battle_rank_stats (event_brawlstars_id, tier_range, battle_date, brawler_brawlstars_id);

create unique index if not exists uk_brawler_team_battle_result_stats__1
  on brawler_team_battle_result_stats (event_brawlstars_id, tier_range, start_battle_date, end_battle_date, team_key);

create unique index if not exists uk_brawler_team_battle_rank_stats__1
  on brawler_team_battle_rank_stats (event_brawlstars_id, tier_range, start_battle_date, end_battle_date, team_key);
```

- 프로젝트 컨벤션에 따라 `schema.sql` 에도 동일 `create unique index if not exists` 문을 추가한다.
- `team_key` 칼럼 확인 후 존재하지 않으면 이번 스펙에서는 팀 테이블은 **보류하고 플랜 단계에서 확인**. 칼럼이 없으면 스펙 B 의 upsert 전환 시 함께 반영.

#### 주의

- **먼저 1-A 버그 수정 + 재집계를 수행한 뒤** unique 인덱스를 추가해야 한다. 현재 DB 에 이미 중복이 존재할 수 있기 때문에 먼저 인덱스를 생성하면 실패한다.
- 플랜 단계에서 "기존 중복 존재 여부 진단 쿼리" + "재집계 수순" 을 명시한다.

---

### 4.4 (3-A) `RunIdIncrementer` 적용

#### 대상 잡

- `brawlerBattleResultStatsJob`
- `brawlerBattleRankStatsJob`
- `brawlerTeamBattleResultStatsJob`
- `brawlerTeamBattleRankStatsJob`
- `brawlerEnemyBattleResultStatsJob`
- `brawlerItemOwnershipStatsJob`
- `partitionManageJob`

#### 수정 내용

각 `JobBuilder(...)` 에 `.incrementer(RunIdIncrementer())` 추가. 기존 `DateJobParameter` 는 identifying 파라미터로 유지하여 "같은 date, 다른 run.id" 로 재실행 가능.

```kotlin
JobBuilder(JOB_NAME, jobRepository)
    .incrementer(RunIdIncrementer())
    .validator(DefaultJobParametersValidator(arrayOf(DateJobParameter.KEY), arrayOf()))
    .start(step)
    .build()
```

#### 운영 영향

- 재실행성이 확보되어 1-A 수정 후 과거 `date` 재집계가 간단해진다.
- `spring-boot` 기본 자동 실행(`spring.batch.job.enabled=true` 환경) 에서 중복 실행 방지 동작이 바뀌지 않도록, 스케줄러/진입점이 명시적으로 잡을 트리거하는 환경임을 플랜 단계에서 재확인한다.

---

## 5. 컴포넌트와 경계

- **Aggregator** (`domain/*Aggregator.kt`): 순수 in-memory 집계. Spring / JPA 의존성 없음. 입력은 `StatsRankBattleEntity` / `StatsResultBattleEntity`, 출력은 엔티티 List.
- **Tasklet** (`job/statistics/*Tasklet.kt`): Spring Batch 계약을 만족하며 read → aggregate → write 를 조율한다. 수동 트랜잭션 제거, `TransactionTemplate` 혹은 협력자 빈을 통해 Spring-managed 트랜잭션 경계만 유지.
- **JobConfig** (`job/statistics/*JobConfig.kt`): Job/Step bean 조립. `RunIdIncrementer` 적용 외에 tasklet 에 `TransactionTemplate` 주입 추가.
- **Schema** (`infrastructure/db-core/.../schema.sql`, `V260423__*.sql`): 자연키 unique 인덱스.

단위 테스트는 aggregator 에 집중(순수 Kotlin). 트랜잭션/원자성 검증은 `@SpringBatchTest` + `@Transactional`(롤백) 기반 context 테스트.

## 6. 실행 순서와 롤아웃

| 순서 | 단계                                                                                    | 비고                                                                            |
| ---- | --------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------- |
| 1    | (코드) 1-A Aggregator 수정 + 유닛 테스트                                                | 선 배포 가능                                                                    |
| 2    | (코드) `RunIdIncrementer` 적용 (3-A)                                                    | 재집계 도구                                                                     |
| 3    | (코드) 1-B, 2-B 트랜잭션 원자화                                                         | 위 두 개와 같이 배포 권장                                                       |
| 4    | (운영) 1-A 수정 반영된 빌드 배포 후 최근 2주 rank 통계 재집계 (`StatsWeekRange`)        | `date` 를 과거로 주며 job 재실행                                                |
| 5    | (운영) 재집계 완료 확인 + 중복 잔존 여부 진단 쿼리                                      | 진단 쿼리는 플랜 단계에 포함                                                    |
| 6    | (스키마) 2-C unique 인덱스 마이그레이션 적용                                            | 중복 완전 제거 이후                                                             |
| 7    | (관측) 배치 실행 시간과 조회 API 캐시 미스 구간 확인                                    | 2-B 트랜잭션 확대로 단일 트랜잭션 길이 증가 가능성 모니터링 — 스펙 B 로 이어짐 |

## 7. 위험과 완화

| 위험                                                                              | 완화                                                                                                                                  |
| --------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------- |
| 팀 rank aggregator 의 `processedBattleKeys` HashSet 메모리                         | 이벤트 1개 범위로 국한. 스펙 B 에서 구조적 제거. 실측 SQL 결과가 수십만 건 이상이면 스펙 B 우선순위를 올린다.                         |
| 단일 이벤트 트랜잭션이 길어짐                                                      | 현 데이터 규모에서는 감당 가능. 측정 후 임계 초과 시 스펙 B 의 chunk-oriented step 전환으로 해결.                                    |
| unique 인덱스 생성 시 기존 중복 충돌                                               | 반드시 1-A 재집계 이후 적용. 사전 진단 쿼리로 중복 0 확인 후 마이그레이션 실행.                                                       |
| `RunIdIncrementer` 적용이 외부 스케줄러 동작에 미치는 영향                         | 진입점에서 job 을 트리거할 때 `run.id` 를 자동 증가시키는 것이 기본 동작. 기존 스크립트에서 `run.id` 를 명시하고 있는지 플랜에서 확인. |
| 1-A 수정 후 과거 값 대비 통계 수치가 변화해 사용자에게 이질감                       | 수정 직후 공지 또는 관리자 알림 채널에 변경 공지.                                                                                     |

## 8. 검증 계획

### 8.1 단위 테스트

- `BrawlerBattleRankStatsAggregatorTest`
  - 트리오 랭크 1 battle 을 3 row 로 공급 → 한 브롤러에 대해 rank 는 1회만 집계된다.
  - 듀오 랭크 1 battle 을 2 row 로 공급 → 각 플레이어가 자기 row 에서만 집계된다.
  - `StatsTierRange.resolveAll` 이 여러 구간 반환 시 각 구간 별로 1회씩 집계 (기존 동작 유지).
- `BrawlerTeamBattleRankStatsAggregatorTest`
  - 트리오 랭크 1 battle 을 3 row 로 공급 → 팀 조합 키는 `battleKey` dedup 으로 1회만 집계.
  - 서로 다른 `battleKey` 를 가진 동일 팀 조합은 각각 집계.

### 8.2 Context 테스트 (선택)

- tasklet 의 트랜잭션 원자성: 강제 예외 주입 시 delete 가 롤백되어 이전 통계가 유지되는지 확인. `CoreBatchContextTest` 기반.

### 8.3 운영 검증 쿼리

```sql
-- 1-A 수정 전 대비 합계 감소 확인 (재집계 후)
SELECT event_brawlstars_id, tier_range,
       sum(rank_1_count + rank_2_count + rank_3_count
           + rank_4_count + rank_5_count) AS total
FROM brawler_team_battle_rank_stats
WHERE start_battle_date = :start AND end_battle_date = :end
GROUP BY 1, 2 ORDER BY 3 DESC LIMIT 20;

-- 2-C unique 인덱스 생성 전 중복 잔존 여부
SELECT event_brawlstars_id, tier_range, battle_date, brawler_brawlstars_id, count(*)
FROM brawler_battle_rank_stats
GROUP BY 1,2,3,4 HAVING count(*) > 1;
```

## 9. 비목표

- 팀 통계 aggregator 의 메모리 사용량 절감 (스펙 B).
- tasklet → chunk-oriented step 전환 (스펙 B).
- `orderBy` 누락 reader, API 실패 삼킴, 알림 재시도 (스펙 C).
- `BrawlerItemOwnershipStatsJob`, `BrawlerSyncJob` 성능 개선 (별도 스펙).

## 10. 오픈 이슈 (플랜 단계에서 확정)

- writer 빈의 구체 형태(제네릭 단일 빈 `StatsReplaceWriter<T>` vs 통계 종류별 별도 빈). 본 스펙 기본 추천은 제네릭 단일 빈.
- Spring Batch job 의 외부 트리거(스케줄러) 가 `run.id` 를 명시적으로 넘기고 있는지 확인 — 만약 스케줄러가 `run.id` 를 고정값으로 넣고 있다면 `RunIdIncrementer` 효과가 상쇄되므로 트리거 쪽도 같이 조정.
- 1-A 수정 배포 후 재집계 범위 — `StatsWeekRange.currentAndPrevious` 가 커버하는 최근 2주로 고정할지, 운영자 판단으로 과거 몇 주까지 재실행할지.
- `brawler_enemy_battle_result_stats` 의 `team_size` 가 없으므로 자연키 조합에 듀오/트리오 구분자가 필요한지 — 엔티티 집계 규칙(같은 이벤트에서 같은 (brawler, enemy) 쌍은 한 row 로 합쳐짐) 을 플랜 단계에서 재확인.
