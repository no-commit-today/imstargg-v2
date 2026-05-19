# core-batch statistics 짧은 트랜잭션 설계서

- 작성일: 2026-05-19
- 대상 모듈: `imstargg-backend/core/core-batch`
- 관련 모듈: `imstargg-backend/infrastructure/db-core`
- 우선순위: DB 안정성, 코드 품질, 기존 구조 보존

## 1. 목적

`core-batch` statistics 배치에서 step 시작부터 종료까지 유지되는 긴 DB 트랜잭션을 제거한다. 현재 daily/weekly battle statistics job은 `StepBuilder(...).tasklet(..., txManager)` 형태라 읽기, 집계, 기존 집계 삭제, 새 집계 저장이 하나의 tasklet step transaction에 묶인다. `EntityManager.flush()`와 `clear()`를 호출해도 commit은 step 종료 시점까지 지연된다.

이번 변경의 목표는 다음과 같다.

- DB lock 보유 시간과 MVCC vacuum 지연 가능성을 줄인다.
- 대량 delete/insert rollback 비용을 줄인다.
- 실패 시 이미 완료된 작은 단위는 유지하고, 재실행으로 최종 상태를 복구한다.
- Spring Batch는 job metadata, parameter, 실행 이력, 실패 상태 관리를 위한 실행 프레임으로 유지한다.
- statistics 집계 step은 DB transaction manager를 전달하지 않는 tasklet step으로 구성한다.
- 현재 aggregator와 query lambda 구조를 가능한 한 유지한다.
- 트랜잭션 경계 관리가 각 job config에 흩어지지 않게 공통 컴포넌트로 표현한다.

## 2. 범위

포함한다.

- `DailyBattleStatsAggregationTask`를 사용하는 daily battle statistics job
  - `BrawlerBattleResultStatsJobConfig`
  - `BrawlerBattleRankStatsJobConfig`
- `WeeklyEventBattleStatsAggregationTask`를 사용하는 weekly/event battle statistics job
  - `BrawlerPairBattleResultStatsJobConfig`
  - `BrawlerPairBattleRankStatsJobConfig`
  - `BrawlerEnemyBattleResultStatsJobConfig`
- `BrawlerItemOwnershipStatsJobConfig`
- `BrawlerItemOwnershipStatsTask`
- `BrawlerItemOwnershipStatsWriter`
- 짧은 transaction 실행과 집계 테이블 교체를 위한 `core-batch` support component
- 관련 단위 테스트와 context wiring 테스트

포함하지 않는다.

- 통계 테이블 schema/versioning 재설계
- API 조회 조건 변경
- DB `GROUP BY` 집계 이관
- chunk-oriented step 전면 전환
- 신규 통계 지표 추가
- core-api, core-worker의 동작 변경

## 3. 결정 사항

### 3.1 부분 갱신 허용

배치 실행 중 API가 부분 갱신된 통계 결과를 보는 것을 허용한다. 예를 들어 weekly job에서 특정 `(weekRange, eventId)`는 새 결과이고 다른 event는 이전 결과인 상태가 잠깐 존재할 수 있다.

이 결정을 통해 staging/version 테이블이나 job 단위 원자적 전환은 도입하지 않는다. 대신 DB 부담을 줄이고 구현 복잡도를 낮춘다.

### 3.2 실패와 재실행

실패하면 Spring Batch job은 실패 상태로 남긴다. 이미 commit된 처리 단위는 롤백하지 않는다. 운영자는 같은 job parameter로 재실행한다.

재실행 시 각 처리 단위는 기존 집계를 다시 삭제하고 새 집계를 삽입한다. 집계 테이블에는 자연키 unique index가 있으므로 같은 단위의 반복 실행은 최종 결과 관점에서 멱등적이어야 한다.

### 3.3 Spring Batch 유지 여부

Spring Batch는 유지한다. 이 프로젝트의 statistics job은 Kubernetes CronJob 전제로 실행되더라도 다음 Batch 기능을 계속 활용한다.

- job parameter 검증과 실행 식별
- job/step 실행 이력과 실패 상태 기록
- 실패한 job의 운영 재실행 모델
- 기존 `core-batch` job wiring, context test, 모듈 경계

다만 Spring Batch를 "모든 데이터 처리 transaction을 맡기는 도구"로 쓰지는 않는다. statistics aggregation은 Spring Batch가 제공하는 실행 프레임 안에서 애플리케이션 task가 명시적으로 transaction 단위를 관리한다.

### 3.4 Tasklet step 유지 여부

statistics aggregation은 tasklet step으로 유지한다.

이 배치는 `item -> process -> write` 흐름이 아니라 다음 흐름을 가진다.

```text
many battles/pages -> in-memory aggregate -> replace aggregate rows
```

Spring Batch의 chunk-oriented step은 item을 읽고 commit interval마다 chunk를 writer에 전달하고 commit하는 모델이다. 현재 statistics 알고리즘은 전체 또는 event 단위 입력을 집계한 뒤 결과 row set을 교체하므로 chunk-oriented step에 억지로 맞추면 aggregator lifecycle, event grouping, 최종 flush 시점이 오히려 복잡해진다.

따라서 tasklet step이 더 자연스럽다. 단, DB transaction manager를 전달한 tasklet step은 부적합하다. Spring Batch 6.0.2에는 `StepBuilder.tasklet(tasklet)` 오버로드가 있고, transaction manager를 명시하지 않으면 `TaskletStep`은 resourceless transaction manager를 기본 사용한다. statistics tasklet은 이 형태를 사용해 step 자체가 DB transaction을 열지 않게 한다.

```kotlin
StepBuilder(STEP_NAME, jobRepository)
    .tasklet { _, _ ->
        task.aggregate(...)
        RepeatStatus.FINISHED
    }
    .build()
```

### 3.5 선택한 접근

tasklet 구조는 유지하되, tasklet step에는 DB transaction manager를 전달하지 않는다. 실제 DB 변경은 내부 replace 처리 단위를 `TransactionTemplate`으로 짧게 commit한다.

선택하지 않은 접근:

- chunk-oriented step 전면 전환: Spring Batch 구조에는 잘 맞지만 현재 in-memory aggregator 생명주기와 weekly event 단위 flush 흐름이 복잡해진다.
- staging/version 도입: API 원자성은 좋아지지만 이번 요구보다 과하고 schema/API 조회 변경으로 범위가 커진다.

## 4. 현재 문제

### 4.1 DB transaction manager를 전달한 TaskletStep

daily/weekly statistics job은 다음 형태로 구성되어 있다.

```kotlin
StepBuilder(STEP_NAME, jobRepository)
    .tasklet({ _, _ ->
        aggregationTask.aggregate(...)
        RepeatStatus.FINISHED
    }, txManager)
    .build()
```

Spring Batch `TaskletStep`은 tasklet 호출을 transaction으로 감싼다. 위처럼 DB `txManager`를 전달하면 `aggregate()` 내부의 모든 읽기와 쓰기가 하나의 DB transaction에 참여한다. `DailyBattleStatsAggregationTask`와 `WeeklyEventBattleStatsAggregationTask`는 페이지 단위로 읽고 `em.clear()`를 호출하지만, transaction 자체는 step 종료까지 유지된다.

문제의 핵심은 tasklet step 자체가 아니라 **tasklet step에 DB transaction manager를 전달한 것**이다.

### 4.2 Flush/clear와 commit의 혼동

현재 aggregation task는 저장 중 `em.flush()`와 `em.clear()`를 호출한다. 이는 SQL 전송과 persistence context 메모리 관리에는 도움이 되지만, DB lock 해제나 MVCC snapshot 종료를 의미하지 않는다. commit이 늦으면 delete/insert lock과 undo/rollback 비용은 계속 누적된다.

### 4.3 Ownership writer의 중첩 transaction 표현

`BrawlerItemOwnershipStatsJobConfig`도 `tasklet(..., txManager)`를 사용하고, `BrawlerItemOwnershipStatsWriter.write()`는 다시 `@Transactional`을 가진다. 실제로는 outer step transaction에 참여하므로 writer의 chunk flush가 commit 경계를 만들지 못한다.

## 5. 설계

### 5.1 Transaction boundary

읽기와 집계는 긴 transaction 없이 수행한다. DB 변경은 작고 명확한 replace 단위에서만 transaction을 연다.

처리 단위:

- daily battle statistics: `date` 단위
- weekly battle statistics: `(weekRange, eventId)` 단위
- ownership statistics: ownership table 단위 또는 writer가 명시한 작은 replace 단위

각 replace transaction은 다음 순서를 가진다.

```text
begin transaction
delete existing rows for unit
persist new rows in BATCH_SIZE chunks
flush and clear after each chunk
commit transaction
```

### 5.2 `BatchTransactionExecutor`

`core-batch` support package에 짧은 transaction 실행 컴포넌트를 둔다.

책임:

- `PlatformTransactionManager`와 `EntityManagerFactory`를 사용해 transaction을 시작한다.
- transaction 안에서 사용할 새 `EntityManager`와 `JPAQueryFactory`를 제공한다.
- 정상 완료 시 commit, 예외 발생 시 rollback한다.
- transaction 종료 후 `EntityManager`를 닫는다.

예상 형태:

```kotlin
class BatchTransactionExecutor(
    private val txManager: PlatformTransactionManager,
    private val entityManagerFactory: EntityManagerFactory,
) {
    fun <T> execute(
        action: (EntityManager, JPAQueryFactory) -> T,
    ): T
}
```

구현은 Spring의 `TransactionTemplate`을 사용한다. task나 writer가 `@Transactional`에 의존하지 않고, 어느 블록이 commit 단위인지 코드에서 직접 보이게 한다.

전파 정책은 짧은 독립 transaction이어야 한다. 정상 설계에서는 statistics tasklet step이 DB transaction을 열지 않으므로 outer DB transaction이 없다. 그래도 `BatchTransactionExecutor`는 `PROPAGATION_REQUIRES_NEW`를 명시해 replace 단위가 항상 독립 commit이라는 정책을 코드로 고정한다. 이는 tasklet step의 긴 transaction을 우회하기 위한 장치가 아니라, 나중에 다른 DB transaction 안에서 실수로 호출되더라도 replace가 합류하지 않게 하는 방어선이다.

### 5.3 `StatsAggregationReplacer`

집계 테이블의 `delete + insert chunks` 패턴을 공통화한다.

책임:

- `BatchTransactionExecutor` 안에서 replace를 수행한다.
- 호출자가 제공한 `deleteExisting` callback을 먼저 실행한다.
- `BattleStatsAggregator.flushEntityChunks(BATCH_SIZE)`를 사용해 결과 entity를 chunk 단위로 저장한다.
- chunk마다 `persist`, `persistAdditional`, `flush`, `clear`를 실행한다.

예상 형태:

```kotlin
class StatsAggregationReplacer(
    private val transactionExecutor: BatchTransactionExecutor,
) {
    fun <TEntity : Any> replace(
        deleteExisting: (JPAQueryFactory) -> Unit,
        entities: ((List<TEntity>) -> Unit) -> Unit,
        persist: (EntityManager, TEntity) -> Unit,
        persistAdditional: (EntityManager, TEntity) -> Unit = { _, _ -> },
    )
}
```

실제 구현에서는 호출부 가독성을 우선해 generic signature를 조정할 수 있다. 중요한 것은 delete와 insert가 같은 짧은 transaction 안에 있고, 이 helper 밖으로 긴 transaction이 새지 않는 것이다.

### 5.4 Daily battle statistics flow

`DailyBattleStatsAggregationTask`는 `EntityManager`를 외부에서 받지 않는다. task 내부에서 read용 `EntityManager`를 짧은 생명주기로 열고 닫는다.

흐름:

1. read용 `EntityManager`와 `JPAQueryFactory` 생성
2. `date`, `lastId` 기반으로 battle page 조회
3. page를 aggregator에 누적
4. read용 persistence context clear
5. 모든 page 처리 후 read용 `EntityManager` close
6. `StatsAggregationReplacer`로 `date` 단위 replace transaction 실행
7. 완료 로그 기록

job config는 `EntityManager` 주입을 제거하고, read query lambda와 delete lambda만 전달한다.

### 5.5 Weekly event battle statistics flow

`WeeklyEventBattleStatsAggregationTask`도 외부 `EntityManager`를 받지 않는다.

흐름:

1. read용 `EntityManager`와 `JPAQueryFactory` 생성
2. weekRange별 distinct event ids 조회
3. eventId별 aggregator 생성
4. 해당 `(weekRange, eventId)` battle pages 조회와 집계
5. event 하나의 집계가 끝나면 `StatsAggregationReplacer`로 `(weekRange, eventId)` 단위 replace transaction 실행
6. 다음 event 진행
7. read용 `EntityManager` close

이 구조에서는 한 event의 replace가 commit된 뒤 다음 event를 집계한다. 실패 시 이전 event 결과는 남고, 실패 event는 rollback된다.

### 5.6 Ownership statistics flow

`BrawlerItemOwnershipStatsTask`의 read/accumulate 흐름은 유지한다. `BrawlerItemOwnershipStatsReader`는 이미 자체 `EntityManager`를 만들어 page 단위로 읽으므로 긴 write transaction을 만들지 않는다.

`BrawlerItemOwnershipStatsWriter`는 `@Transactional`을 제거하고 `BatchTransactionExecutor` 또는 전용 replace helper를 사용한다.

권장 처리 단위:

- gear ownership stats table
- gadget ownership stats table
- star power ownership stats table
- hyper charge ownership stats table
- buffy ownership stats table

각 table은 다음 흐름으로 교체한다.

```text
begin transaction
delete all rows in this ownership stats table
save rows in BATCH_SIZE chunks
flush and clear after each chunk
commit
```

table 단위 부분 갱신도 허용한다. API는 실행 중 일부 item type만 새 통계를 볼 수 있다.

### 5.7 Step transaction configuration

statistics tasklet step은 더 이상 step 전체 transaction에 의존하지 않는다. 구현 우선순위는 다음과 같다.

- battle statistics job config에서 `EntityManager` field를 제거한다.
- tasklet body는 task 호출만 수행한다.
- Spring Batch 6.0.2의 `StepBuilder.tasklet(tasklet)` 오버로드를 사용하고, statistics tasklet step에는 DB `txManager`를 전달하지 않는다.
- tasklet step은 Spring Batch의 기본 `ResourcelessTransactionManager`를 사용하게 둔다.
- read/write용 `EntityManager`는 task/helper가 명시적으로 생성하고 닫는다.
- write helper는 `PROPAGATION_REQUIRES_NEW`로 replace 단위 독립 commit을 보장한다.

금지하는 형태:

```kotlin
StepBuilder(STEP_NAME, jobRepository)
    .tasklet(tasklet, txManager)
    .build()
```

사용할 형태:

```kotlin
StepBuilder(STEP_NAME, jobRepository)
    .tasklet(tasklet)
    .build()
```

## 6. Error handling

- read/aggregate 중 예외가 발생하면 replace transaction은 시작하지 않고 job은 실패한다.
- replace transaction 중 예외가 발생하면 해당 단위만 rollback되고 job은 실패한다.
- 이미 commit된 이전 단위는 유지한다.
- 재실행 시 각 단위는 `delete + insert`를 다시 수행한다.
- alert 정책은 이번 변경에서 새로 추가하지 않는다.

## 7. Testing

### 7.1 Unit tests

`DailyBattleStatsAggregationTaskTest`:

- read page가 기존 순서대로 호출되는지 확인한다.
- read 중에는 replace callback이 실행되지 않는지 확인한다.
- 모든 read가 끝난 뒤 `delete -> persist chunks -> flush/clear`가 replace transaction 안에서 실행되는지 확인한다.

`WeeklyEventBattleStatsAggregationTaskTest`:

- weekRange별 event ids를 읽는지 확인한다.
- event별 aggregator가 분리되는지 확인한다.
- `(weekRange, eventId)`마다 replace가 독립적으로 실행되는지 확인한다.
- `persistAdditional`이 기존처럼 호출되는지 확인한다.

`BrawlerItemOwnershipStatsWriterTest`:

- table별 delete와 chunk save 순서를 확인한다.
- chunk마다 clear가 호출되는지 확인한다.
- transaction executor가 table 단위로 호출되는지 확인한다.

`BatchTransactionExecutorTest` 또는 helper 단위 테스트:

- 정상 실행 시 action result를 반환한다.
- 예외 발생 시 예외가 전파된다.
- action에 `EntityManager`와 `JPAQueryFactory`가 전달된다.
- `PROPAGATION_REQUIRES_NEW`가 설정되어 replace transaction이 독립 commit 정책을 가진다는 점을 확인한다.

### 7.2 Context tests

`CoreBatchContextTest` 계열에서 statistics job bean wiring을 확인한다.

확인 대상:

- daily battle statistics job config
- weekly battle statistics job config
- ownership statistics job config
- 새 support component bean
- statistics tasklet step이 DB transaction manager를 전달하지 않는 형태로 생성되는지 확인한다.

### 7.3 Verification commands

구현 후 최소 검증:

```bash
./gradlew :core:core-batch:test --tests "com.imstargg.core.batch.job.statistics.*"
./gradlew :core:core-batch:test --tests "com.imstargg.core.batch.job.statistics.ownership.*"
./gradlew :core:core-batch:contextTest
```

필요 시 전체 backend test 또는 `ktlintCheck`를 추가로 실행한다.

## 8. Migration and compatibility

DB schema migration은 필요하지 않다. 기존 통계 테이블과 자연키 unique index를 그대로 사용한다.

외부 동작 변화:

- 배치 실행 중 API가 부분 갱신 결과를 볼 수 있다.
- 실패 후 재실행 전까지 이미 commit된 단위와 이전 단위가 섞여 있을 수 있다.
- 최종 완료 후 결과는 기존 배치와 같아야 한다.

내부 동작 변화:

- statistics tasklet step에서 DB transaction manager 의존을 제거한다.
- write commit 단위가 명시적으로 작아진다.
- `EntityManager` 주입 범위가 줄어든다.
- `@Transactional` 대신 batch-local transaction helper를 사용한다.
- write helper는 accidental outer DB transaction 합류를 막기 위해 `PROPAGATION_REQUIRES_NEW`를 사용한다.

## 9. Acceptance criteria

- statistics battle aggregation job에서 read/aggregate 전체를 감싸는 장기 transaction이 사라진다.
- statistics tasklet step은 DB `txManager`를 전달하지 않는 `tasklet(tasklet)` 형태로 구성된다.
- daily replace는 `date` 단위로 commit된다.
- weekly replace는 `(weekRange, eventId)` 단위로 commit된다.
- ownership stats replace는 table 단위 또는 더 작은 명시 단위로 commit된다.
- replace transaction은 step transaction에 합류하지 않고 독립적으로 commit된다.
- 기존 aggregation 결과의 의미가 바뀌지 않는다.
- 실패 시 job은 실패하고, 같은 parameter 재실행으로 최종 상태를 복구할 수 있다.
- 테스트가 짧은 transaction helper와 replace 순서를 검증한다.

## 10. References

- Spring Batch 6.0.2 local source: `TaskletStep` wraps each tasklet call in a transaction and defaults to `ResourcelessTransactionManager` when no transaction manager is set.
- Spring Batch 6.0.2 local source: `StepBuilder.tasklet(Tasklet)` exists and does not require a `PlatformTransactionManager`.
- Spring Batch reference: chunk-oriented processing is the common item/chunk/write model and commits after each chunk.
- Spring Batch reference: `TaskletStep` is intended for custom step work such as stored procedure calls, scripts, SQL updates, or non-item-oriented tasks.
