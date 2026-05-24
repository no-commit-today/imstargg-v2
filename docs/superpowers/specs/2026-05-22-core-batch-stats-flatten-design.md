# core-batch 통계 잡 평탄화 & 트랜잭션 정리 설계서

- 작성일: 2026-05-22
- 재정렬: 2026-05-23 (dev 베이스로 재정렬 — `JpaBatchTransactionRunner` 가 이미 머지되어 있는 상태를 전제)
- 대상 모듈: `imstargg-backend/core/core-batch`
- 우선순위: 가독성 (과도한 추상화 제거), 트랜잭션 일관성
- 선행 문서: [2026-05-19-batch-short-transactions-design.md](./2026-05-19-batch-short-transactions-design.md), [2026-04-30-core-batch-refactoring-design.md](./2026-04-30-core-batch-refactoring-design.md)

## 0. 베이스 전제 (dev 현재 상태)

이 설계는 `origin/dev` 의 다음 상태를 전제한다.

- `support/transaction/JpaBatchTransactionRunner` 가 이미 존재 — `PROPAGATION_REQUIRES_NEW` + `EntityManager` 주입 + `JPAQueryFactory` 제공
- `task/DailyBattleStatsAggregationTask`, `task/WeeklyEventBattleStatsAggregationTask` 가 이미 짧은 트랜잭션으로 리팩터링됨
  - read 구간: `entityManagerFactory.createEntityManager()` 로 자체 em (트랜잭션 없음)
  - write 구간: `transactionRunner.run { em, qf -> ... }` 로 짧은 트랜잭션
- `BrawlerItemOwnershipStats*` 가 이미 `Reader / Writer / Task / Accumulator / Result` 로 분리되어 있음
  - Writer 는 `JpaBatchTransactionRunner` 로 5개 테이블 각각 별도 트랜잭션
- JobConfig 들은 `txManager` 를 주입하지 않고, step tasklet 에 tx 를 부여하지 않음 (`.tasklet({...})`)

따라서 본 설계의 주 목적은 **과도한 추상화 제거 (가독성)** 이고, 트랜잭션 정리는 dev 의 기존 패턴을 통계 5개 잡의 새 구조에서도 일관되게 사용하는 부수 효과다.

## 1. 목적

`core-batch` 의 통계 집계 잡 5개를 ownership 잡과 동일한 분리 패턴(`Task` / `Reader` / `Writer`) 으로 평탄화한다.

dev 의 `DailyBattleStatsAggregationTask` / `WeeklyEventBattleStatsAggregationTask` 는 **이미 짧은 트랜잭션** 으로 정착되었으나, 다음 가독성 문제가 남아 있다.

1. **JobConfig 의 람다 트리**: 각 잡의 `JobConfig.step` tasklet 안에서 `aggregationTask.aggregate(...)` 를 호출하면서 `readPage`, `getId`, `aggregator`, `deleteExisting`, `persist`, (Weekly 는 `readDistinctEventIds`, `persistAdditional` 까지) 5~7개의 람다를 직접 정의한다. 잡 의도를 파악하려면 JobConfig 와 공통 task 를 왕복해서 읽어야 한다.
2. **공통 task 의 generic 압박**: `<TBattle, TEntity : Any>` 타입 파라미터 + EM/queryFactory 분리 책임 + 페이지 루프 + 트랜잭션 호출이 한 메서드에 몰려 있다. 단일 책임이 무너져 디버깅이 어렵다.
3. **ownership 잡과의 비일관성**: `BrawlerItemOwnershipStatsJob` 은 이미 Reader/Writer/Task/Accumulator 로 분리되어 잡 흐름이 자기 파일에 그대로 보인다. 통계 5개 잡만 공통 task 추상화에 묶여 있다.

본 리팩터링은 ownership 패턴을 통계 5개 잡으로 일반화한다. 통계 5개 + ownership 1개가 같은 모양을 갖게 되어 일관성이 올라간다. 트랜잭션은 dev 의 `JpaBatchTransactionRunner` 를 재사용해 동일 정책을 유지한다.

사용자 정책 ("과도한 추상화보다는 중복이 더 괜찮음") 과 일치한다.

## 2. 범위

포함한다.

- 통계 5개 잡 평탄화
  - Daily 2개: `BrawlerBattleResultStatsJob`, `BrawlerBattleRankStatsJob`
  - Weekly-Event 3개: `BrawlerPairBattleResultStatsJob`, `BrawlerPairBattleRankStatsJob`, `BrawlerEnemyBattleResultStatsJob`
- 평탄화 완료 후 `DailyBattleStatsAggregationTask`, `WeeklyEventBattleStatsAggregationTask` 제거
- `BrawlerSyncTask.detectBrawlerChanges` 의 4번 반복을 작은 헬퍼로 정리

포함하지 않는다.

- `JpaBatchTransactionRunner` 자체 변경 — dev 의 것 그대로 활용
- `BrawlerItemOwnershipStats*` 변경 — 이미 같은 패턴 (Reader/Writer/Task/Accumulator/Result) 으로 정리되어 있음
- `QuerydslZeroPagingItemReader`, `QuerydslEntityCursorItemReader` 삭제 (추후 사용 예정으로 보존)
- `BrawlerItemOwnershipStatsAccumulator/Reader` 의 5번 반복 (의도된 명시성으로 보존)
- `BattleStatsAggregator` 인터페이스 제거 (5개 task 가 동일 contract 로 사용 — 유지)
- `StatsPlayerSyncJobConfig` 의 rank/result reader 중복 (호출 2회 + `@StepScope` bean 이름이 메타데이터 — 추출하면 오히려 obscure)
- 알림 정책, 통계 쿼리 SQL `GROUP BY` 이관, 스키마 변경
- core-worker, core-api 동작 변경

## 3. 통일 패턴 (Ownership 패턴 일반화)

5개 통계 잡 모두 같은 4-컴포넌트 구조로 정렬한다.

```text
{JobName}JobConfig (wiring만, @JobScope/@StepScope 포함, txManager 미주입)
└─ {JobName}Task                  : 잡 흐름 조립 (read → aggregate → write)
   ├─ {JobName}Reader              : 자체 EntityManager (트랜잭션 X), 페이지 단위 read
   ├─ BattleStatsAggregator 구현체  : 기존 도메인 그대로 사용
   └─ {JobName}Writer              : JpaBatchTransactionRunner.run { ... } 으로 짧은 트랜잭션
```

### 3.1 책임 분리 규칙

- **JobConfig**: `JobBuilder`, `StepBuilder`, parameter bean, `Task.aggregate(...)` 호출 람다만. **`txManager` 미주입**, **`.tasklet({...})`** (step tx 없음). 쿼리 / 람다 / `EntityManager` 의존성 금지. (dev 의 ownership/공통 task 호출 패턴과 동일)
- **Reader**: `EntityManagerFactory.createEntityManager()` 로 자체 em → 트랜잭션 밖에서 페이지 read. 안정 정렬(`id.asc()`) 명시. `consume: (List<TBattle>) -> Unit` 콜백 (ownership reader 와 동일).
- **Writer**: `JpaBatchTransactionRunner.run { em, queryFactory -> ... }` 로 단일 트랜잭션 안에서 `deleteExisting` 후 `aggregator.flushEntityChunks { batch -> persist + em.flush + em.clear }`. **`@Transactional` 어노테이션 사용하지 않음** (Runner 로 통일).
- **Task**: `Reader.read { aggregator.aggregate(it) }` → `Writer.write(target, aggregator)`. 진행 로그(누적 read, 집계 entity 수) 책임.

### 3.2 트랜잭션 경계

- **read 구간**: 트랜잭션 없음. 자체 em 으로 페이지 fetch & `em.clear()`.
- **write 구간**: `JpaBatchTransactionRunner.run { em, queryFactory -> ... }` (PROPAGATION_REQUIRES_NEW) 안에서 `deleteExisting` 후 chunked insert. 외부에서 빈 통계가 관측되는 시간 0 (dev 의 ownership 정책과 동일).
- **Weekly 잡**: `weekRange × eventId` 한 쌍당 한 번의 `transactionRunner.run` (dev `WeeklyEventBattleStatsAggregationTask` 와 동일 입자).
- Step tasklet 의 `txManager` 미사용 — spring-batch JobRepository 메타데이터는 spring-batch 가 자체적으로 처리.

### 3.3 `BattleStatsAggregator` 인터페이스

제거하지 않는다. `aggregate`, `flushEntityChunks`, `toEntities` contract 가 5개 task 모두에서 동일한 모양으로 쓰인다. `forEachEntity` / `flushEntityChunks` 의 기본 구현도 유지.

## 4. 잡별 상세 설계

### 4.1 Daily 통계 잡 (2개)

**대상**

- `BrawlerBattleResultStatsJob` (집계 키: 이벤트 × 티어 × 날짜 × 브롤러 × 결과)
- `BrawlerBattleRankStatsJob` (집계 키: 이벤트 × 티어 × 날짜 × 브롤러 × 순위)

**각 잡당 신설 컴포넌트 (3개)**

```text
job/statistics/
├── BrawlerBattleResultStatsJobConfig   (이미 존재, 슬림화)
├── BrawlerBattleResultStatsTask         (신규)
├── BrawlerBattleResultStatsReader       (신규)
└── BrawlerBattleResultStatsWriter       (신규)

├── BrawlerBattleRankStatsJobConfig     (이미 존재, 슬림화)
├── BrawlerBattleRankStatsTask           (신규)
├── BrawlerBattleRankStatsReader         (신규)
└── BrawlerBattleRankStatsWriter         (신규)
```

> 기존 `job/statistics/` 평면 구조를 유지. ownership 만 별도 `ownership/` 서브패키지 유지 (이미 그러함).

**Task 시그니처**

```kotlin
@Component
class BrawlerBattleResultStatsTask(
    private val reader: BrawlerBattleResultStatsReader,
    private val writer: BrawlerBattleResultStatsWriter,
) {
    fun aggregate(date: LocalDate) {
        log.info("[{}] 시작 - date: {}", JOB_NAME, date)
        val aggregator = BrawlerBattleResultStatsAggregator()
        val totalRead = reader.read(date) { battles ->
            battles.forEach { aggregator.aggregate(it) }
        }
        val totalEntities = writer.write(date, aggregator)
        log.info("[{}] 완료 - date: {}, 전투: {}건, 집계: {}건", JOB_NAME, date, totalRead, totalEntities)
    }
}
```

**Reader 시그니처**

```kotlin
@Component
class BrawlerBattleResultStatsReader(
    private val entityManagerFactory: EntityManagerFactory,
) {
    fun read(date: LocalDate, consume: (List<StatsResultBattleEntity>) -> Unit): Long {
        // 자체 em → 트랜잭션 밖
        // 안정 정렬: statsResultBattleEntity.id.asc()
        // 페이지 끝마다 em.clear()
    }
}
```

**Writer 시그니처 (dev `JpaBatchTransactionRunner` 활용)**

```kotlin
@Component
class BrawlerBattleResultStatsWriter(
    private val transactionRunner: JpaBatchTransactionRunner,
) {
    fun write(date: LocalDate, aggregator: BrawlerBattleResultStatsAggregator): Long {
        var totalEntities = 0L
        transactionRunner.run { em, queryFactory ->
            queryFactory
                .delete(brawlerBattleResultStatsEntity)
                .where(brawlerBattleResultStatsEntity.battleDate.eq(date))
                .execute()

            aggregator.flushEntityChunks(BattleStatsConstants.BATCH_SIZE) { batch ->
                batch.forEach { em.persist(it) }
                totalEntities += batch.size
                em.flush()
                em.clear()
            }
        }
        return totalEntities
    }
}
```

`@Transactional` 어노테이션 / 주입된 `EntityManager` 를 직접 쓰지 않는다 — dev 의 통일 패턴(`JpaBatchTransactionRunner`) 으로만 트랜잭션을 만든다.

### 4.2 Weekly-Event 통계 잡 (3개)

**대상**

- `BrawlerPairBattleResultStatsJob`
- `BrawlerPairBattleRankStatsJob`
- `BrawlerEnemyBattleResultStatsJob`

**각 잡당 신설 컴포넌트**

`*StatsTask` / `*StatsReader` / `*StatsWriter`.

**Task 시그니처 (Weekly 의 특수성)**

```kotlin
fun aggregate(weekRanges: List<WeekRange>) {
    for (weekRange in weekRanges) {
        val eventIds = reader.readDistinctEventIds(weekRange)
        for (eventId in eventIds) {
            val aggregator = BrawlerPairBattleResultStatsAggregator(
                startBattleDate = weekRange.start,
                endBattleDate = weekRange.end,
            )
            reader.read(weekRange, eventId) { battles ->
                battles.forEach { aggregator.aggregate(it) }
            }
            writer.write(weekRange, eventId, aggregator)
        }
    }
}
```

**Writer 트랜잭션 입자 (Weekly 의 핵심)**

- `weekRange × eventId` 한 쌍이 한 번의 `transactionRunner.run` (dev 의 `WeeklyEventBattleStatsAggregationTask` 와 동일 입자).
- 한 이벤트의 delete + insert 가 단일 트랜잭션이라 외부에서 빈 통계가 보이는 시간이 없다.
- `BrawlerEnemyBattleResultStatsJob` 의 `persistAdditional` (적 통계 추가 저장) 은 Writer 의 chunk persist 안에서 함께 처리한다.

### 4.3 JobConfig 최종 모습 (예시) — dev 패턴 유지

```kotlin
@Configuration
class BrawlerBattleResultStatsJobConfig(
    private val jobRepository: JobRepository,
    private val task: BrawlerBattleResultStatsTask,
) {
    companion object {
        const val JOB_NAME = "brawlerBattleResultStatsJob"
        const val DATE_PARAMETER_NAME = "brawlerBattleResultStatsJobDateJobParameter"
        private const val STEP_NAME = "brawlerBattleResultStatsStep"
    }

    @Bean(DATE_PARAMETER_NAME) @JobScope
    fun dateJobParameter() = DateJobParameter()

    @Bean(JOB_NAME)
    fun job(): Job =
        JobBuilder(JOB_NAME, jobRepository)
            .validator(DefaultJobParametersValidator(arrayOf(DateJobParameter.KEY), arrayOf()))
            .start(step())
            .build()

    @Bean(STEP_NAME) @JobScope
    fun step(): Step =
        StepBuilder(STEP_NAME, jobRepository)
            .tasklet({ _, _ ->
                task.aggregate(dateJobParameter().date!!)
                RepeatStatus.FINISHED
            })
            .build()
}
```

- `txManager` 미주입.
- `.tasklet({...})` — step 트랜잭션 부여 안 함.
- JobConfig 가 wiring 만 담당. `EntityManager` / 쿼리 / 람다 모두 사라진다.

(dev 의 현재 `BrawlerItemOwnershipStatsJobConfig` 와 동일한 모양)

## 5. 주변 가독성 정리

### 5.1 `BrawlerSyncTask.detectBrawlerChanges` 헬퍼 추출

`STAR_POWER` / `GADGET` / `HYPER_CHARGE` / `GEAR` 4번 동일 구조 반복을 작은 헬퍼로 정리한다. (dev `BrawlerSyncTask` 가 base — main 과 동일한 코드 형태)

```kotlin
private fun <T> detectFor(
    brawler: BrawlerResponse,
    jsonMapper: JsonMapper,
    itemType: BrawlerChangeItemType,
    apiItems: List<T>,
    apiItemId: (T) -> Long,
    dbItemIds: Set<Long>,
): List<BrawlerChange> = changeDetector.detectItemChanges(
    brawlerId = brawler.id,
    brawlerName = brawler.name,
    itemType = itemType,
    apiItemIds = apiItems.map(apiItemId).toSet(),
    dbItemIds = dbItemIds,
    toJson = { id -> jsonMapper.writeValueAsString(apiItems.first { apiItemId(it) == id }) },
)
```

호출부가 4줄짜리 4개로 축약된다. itemType, repository 선택 의도는 호출부에 그대로 보인다.

> Gear 의 ID 추출만 `gearBrawlStarsId` 로 다르므로 `apiItemId` 람다로 분기 (Star Power / Gadget / Hyper Charge 는 `brawlStarsId`).

### 5.2 보존 항목

- `JpaBatchTransactionRunner`: dev 의 것 그대로 활용 (변경 금지)
- `BrawlerItemOwnershipStats*`: dev 의 분리 패턴 그대로 (변경 금지)
- `QuerydslZeroPagingItemReader`, `QuerydslEntityCursorItemReader`: 사용자 결정에 따라 보존
- `BrawlerItemOwnershipStatsAccumulator/Reader` 의 5번 반복: 의도된 명시성
- `StatsPlayerSyncJobConfig` rank/result 중복: 호출 2회이고 `@StepScope` bean 이름이 메타데이터 — 추출하면 오히려 obscure
- `BattleStatsAggregator` 인터페이스: 5개 task 가 동일 contract 로 사용

## 6. 구현 순서

리스크가 낮은 순서로 단계화한다. 각 단계 끝에서 `./gradlew :core:core-batch:test` 통과를 확인한다.

1. **Daily 잡 평탄화 (2개)**
   - `BrawlerBattleResultStatsReader/Writer/Task` 신설 (Writer 는 `JpaBatchTransactionRunner` 활용)
   - `BrawlerBattleResultStatsJobConfig` 슬림화 — `aggregationTask` 의존성 제거, `txManager` 도 미주입
   - 동일 작업을 `BrawlerBattleRankStats~` 에 반복
   - 단위 테스트 + Spring context 테스트

2. **Weekly-Event 잡 평탄화 (3개)**
   - `BrawlerPairBattleResultStatsReader/Writer/Task` 신설 (`weekRange × eventId` 단위 `transactionRunner.run`)
   - `BrawlerPairBattleRankStats~`, `BrawlerEnemyBattleResultStats~` 에 반복
   - `BrawlerEnemyBattleResultStats~` 의 `persistAdditional` 흡수

3. **공통 task 제거**
   - `DailyBattleStatsAggregationTask` 삭제
   - `WeeklyEventBattleStatsAggregationTask` 삭제
   - 관련 단위/컨텍스트 테스트 정리

4. **주변 가독성 (§ 5.1)**
   - `BrawlerSyncTask.detectBrawlerChanges` 헬퍼 추출
   - 기존 `BrawlerSyncTask` 테스트로 회귀 확인

5. **마무리**
   - `./gradlew :core:core-batch:contextTest` 로 전체 Job/Step bean 회귀 검증
   - `./gradlew :core:core-batch:ktlintFormat` 적용
   - 패키지 정리

## 7. 테스트 전략

### 7.1 단위 테스트 (각 잡당)

- `*StatsReaderTest`: 페이지 안정 정렬, 빈 결과 종료, consume 콜백 호출 횟수
- `*StatsWriterTest`: `DbCoreContextTest` 활용 — `JpaBatchTransactionRunner` 가 실제 `EntityManager` 로 동작하므로 delete 후 chunked insert 결과 검증, `aggregator.flushEntityChunks` 가 `BATCH_SIZE` 보다 큰 결과에서 여러 번 호출됨을 확인
- `*StatsTaskTest`: reader/writer mock 으로 흐름 검증 (read → aggregate → write 순서)

### 7.2 통합/Context 테스트

- `CoreBatchContextTest` 는 그대로 두고 신규 컴포넌트들도 bean 로딩됨을 자동 검증
- 기존 Job 실행 결과 회귀: 평탄화 전후 동일 데이터에 같은 결과 entity 가 생성됨을 검증 (가능하면 기존 test 데이터 픽스처 재사용)

### 7.3 회귀 명령

```bash
./gradlew :core:core-batch:unitTest
./gradlew :core:core-batch:contextTest
./gradlew :core:core-batch:ktlintCheck
```

## 8. 성공 기준

- 5개 통계 JobConfig 모두 비즈니스 로직(쿼리 / 람다)이 사라지고 wiring 만 보유한다.
- 5개 JobConfig 모두 `txManager` 를 주입하지 않고 step tasklet 에 tx 를 부여하지 않는다 (dev 패턴 일관).
- 5개 Writer 가 `JpaBatchTransactionRunner.run` 으로 통일된다 — `@Transactional` 어노테이션 / 주입된 `EntityManager` 사용 금지.
- read 구간이 트랜잭션 밖에서 동작한다 (자체 em).
- write 구간이 단일 `transactionRunner.run` 안에서 delete + chunked insert 로 수행되어 외부 빈 통계 관측 시간이 0 이 된다.
- `DailyBattleStatsAggregationTask` / `WeeklyEventBattleStatsAggregationTask` 가 제거된다.
- generic + 다수 람다 패턴이 사라지고 각 잡이 자기 의도를 자기 파일로 직접 표현한다.
- 기존 통계 entity 산출 결과가 테스트로 보존된다.
- `:core:core-batch:test` / `:contextTest` / `:ktlintCheck` 모두 통과한다.
