# core-batch 잡 구조 리팩터링 설계서

- 작성일: 2026-04-30
- 대상 모듈: `imstargg-backend/core/core-batch`
- 우선순위: 가독성/유지보수성, 성능/트랜잭션 안정성

## 1. 목적

`core-batch`의 잡 구조를 전체적으로 정리한다. 목표는 JobConfig를 읽기 쉬운 배치 wiring 계층으로 축소하고, 실제 실행 로직을 이름 있는 컴포넌트로 분리하는 것이다. 동시에 통계 잡의 메모리 피크, 긴 트랜잭션, 안정 정렬 없는 paging reader, 수동 `EntityManager`/수동 트랜잭션 사용을 줄인다.

이번 리팩터링은 알림 체계 확장을 목표로 하지 않는다. 운영 알림과 관측성은 Grafana 등 별도 도구를 사용할 예정이므로 기존 알림 호출은 기능 보존 범위에서만 다룬다.

## 2. 범위

포함한다.

- `job/brawlstars` API 수집/동기화 잡의 JobConfig 슬림화
- `job/statistics` 통계 집계 잡의 실행 로직 분리
- `BrawlerItemOwnershipStatsJobConfig`의 읽기, 집계, 쓰기 책임 분리
- `QuerydslPagingItemReader` 사용처의 안정 정렬 명시
- 수동 `EntityManager`/수동 트랜잭션 제거 또는 Spring Batch 트랜잭션 경계 합류
- 통계 집계 결과 저장 시 사용하기 단순한 chunk flush 방식 도입
- `BrawlStarsApiTasklet`의 의도된 예외 처리 정책 문서화와 테스트 보강

포함하지 않는다.

- 신규 알림 체계, Discord/Grafana 연동 확장
- 통계 쿼리의 완전한 SQL `GROUP BY` 이관
- DB 스키마 재설계 또는 새로운 통계 지표 추가
- core-worker, core-api 동작 변경

## 3. 현재 구조에서 확인한 문제

### 3.1 JobConfig가 실행 로직까지 보유

`PlayerRankingCollectJobConfig`, `ClubRankingCollectJobConfig`, `BrawlerRankingCollectJobConfig`, `BattleEventRotationSyncJobConfig`, `BattleEventModeSyncJobConfig`, `BrawlerSyncJobConfig`는 Job/Step wiring과 실제 API 호출, 비교, 저장, 정리 로직이 한 파일 안에 섞여 있다. 특히 `BrawlerSyncJobConfig`는 변경 감지와 alert payload 생성까지 포함해 파일이 커진다.

### 3.2 통계 Tasklet이 제네릭 람다 조립에 의존

`DailyBattleStatsTasklet`과 `PerEventWeeklyBattleStatsTasklet`은 재사용성을 얻었지만, 각 JobConfig가 많은 람다를 넘겨야 한다. 결과적으로 "어떤 데이터를 읽고, 어떤 단위로 집계하고, 어떤 테이블을 교체하는지"를 이해하려면 config와 tasklet을 왕복해서 읽어야 한다.

### 3.3 Ownership 잡의 수동 트랜잭션

`BrawlerItemOwnershipStatsJobConfig`는 `entityManagerFactory.createEntityManager()`와 `em.transaction.begin/commit()`을 직접 사용한다. Step에 `txManager`가 지정되어 있어도 실제 delete/insert 경계가 별도 수동 트랜잭션으로 관리되어 Batch 트랜잭션 모델과 어긋난다.

### 3.4 Paging reader 안정 정렬 누락

`QuerydslPagingItemReader` 사용처 중 `BattleEventSyncJobConfig`, `StatsPlayerSyncJobConfig`는 `where`만 지정하고 `orderBy`가 없다. page 기반 reader에서 정렬이 없으면 DB 실행 계획이나 동시 변경에 따라 skip/duplicate가 발생할 수 있다.

### 3.5 집계 결과 materialization

통계 집계는 `aggregator.toEntities()`로 전체 결과 리스트를 만든 뒤 chunking하여 persist한다. 데이터가 커질수록 aggregator map과 entity list가 동시에 살아 있어 메모리 피크가 커진다.

## 4. 설계 원칙

### 4.1 JobConfig는 wiring만 담당

JobConfig는 다음 책임만 가진다.

- `Job`, `Step`, parameter bean 선언
- `DefaultJobParametersValidator`, `RunIdIncrementer` 등 Batch 설정
- reader, processor, writer, task component 연결
- chunk size, step 이름, job 이름 같은 배치 메타데이터 보유

API 호출, 비교, 집계, 저장, 정리 로직은 별도 컴포넌트로 옮긴다. 상속형 tasklet 계층이나 과한 Batch DSL은 도입하지 않는다. 공통화는 좁은 helper 또는 policy wrapper 수준으로 제한한다.

### 4.2 실행 로직은 이름 있는 컴포넌트로 분리

실행 컴포넌트 이름은 잡의 의도를 드러내는 동사형 메서드를 가진다.

예시:

```text
PlayerRankingCollectJobConfig
└── PlayerRankingCollectTask.collect()

BrawlerSyncJobConfig
└── BrawlerSyncTask.sync()
    ├── BrawlerChangeDetector
    └── BrawlerChangeReporter

BrawlerBattleResultStatsJobConfig
└── BrawlerBattleResultStatsTask.aggregate(date)

BrawlerItemOwnershipStatsJobConfig
└── BrawlerItemOwnershipStatsTask.rebuild()
    ├── BrawlerItemOwnershipStatsReader
    ├── BrawlerItemOwnershipStatsAccumulator
    └── BrawlerItemOwnershipStatsWriter
```

`Task`는 Spring Batch의 `Tasklet` 상속체가 아니라 애플리케이션 실행 컴포넌트다. Step에서는 람다 tasklet으로 `task.collect()` 또는 `task.aggregate(date)`를 호출하거나, 정책이 필요한 경우 얇은 tasklet wrapper를 사용한다.

### 4.3 BrawlStarsApiTasklet 정책 유지

`BrawlStarsApiTasklet`은 유지한다. `BrawlStarsApiServerException.ServiceUnavailable`은 BrawlStars API 점검 상태를 의미하며, 이 tasklet을 사용하는 잡에서는 실패로 처리하지 않는다.

정책:

- `ServiceUnavailable` 발생 시 warn 로그를 남긴다.
- 예외를 다시 던지지 않는다.
- `RepeatStatus.FINISHED`로 정상 종료한다.
- 이 정책을 KDoc 또는 주석으로 남긴다.
- 테스트로 "ServiceUnavailable은 삼켜지고 action 이후 정상 종료"를 고정한다.

이 정책은 사용자의 명시적 의도이며, 리팩터링 중 바꾸지 않는다.

### 4.4 Reader는 안정 정렬을 명시

`QuerydslPagingItemReader`를 쓰는 모든 reader는 명시적인 `orderBy`를 가진다. 기본 규칙은 엔티티 primary key 기준 오름차순이다.

예시:

```kotlin
queryFactory
    .selectFrom(rankBattle)
    .where(rankBattle.battleDate.eq(date))
    .orderBy(rankBattle.id.asc())
```

`QuerydslPagingItemReader`에는 page 기반 reader가 안정 정렬을 요구한다는 KDoc을 추가한다. 런타임에서 QueryDSL query의 order metadata를 강제 검사하는 방식은 구현 복잡도가 커질 수 있으므로 도입하지 않는다. 대신 사용처에 정렬을 명시하고, 변경된 reader bean이 정상 생성되는 context 테스트로 회귀를 막는다.

### 4.5 수동 트랜잭션 제거

delete/insert/write 경계는 Spring Batch step transaction 또는 `TransactionTemplate`이 관리한다. 수동 `EntityManager.transaction.begin/commit`은 제거한다.

`BrawlerItemOwnershipStatsJob`는 다음 흐름으로 바꾼다.

```text
read pages -> accumulate counts -> replace stats in managed transaction
```

읽기 중 persistence context가 커지는 문제는 page 단위 clear/detach 또는 reader component 내부의 짧은 생명주기 `EntityManager`로 제어하되, 쓰기 트랜잭션과 충돌하지 않게 분리한다. delete/insert는 한 transaction 안에서 처리해 외부에서 빈 통계가 관측되는 시간을 만들지 않는다.

### 4.6 집계 결과는 chunk flush로 저장

`Sequence` 기반 API는 사용하지 않는다. 호출부가 단순한 chunk flush API를 우선한다.

권장 형태:

```kotlin
aggregator.flushEntityChunks(chunkSize = BATCH_SIZE) { entities ->
    writer.write(entities)
}
```

또는 aggregator 변경 범위가 클 경우, task component가 `EntityChunkBuffer` 같은 작은 helper를 사용해 entity 생성과 flush를 함께 처리한다.

원칙:

- 호출부에서 `read -> aggregate -> flush chunks` 흐름이 보여야 한다.
- `toEntities()` 전체 리스트 생성을 성능 리스크가 큰 통계부터 줄인다.
- chunk flush는 여러 chunk로 나눠 저장해도 기존 `toEntities()` 저장 결과와 같아야 한다.
- 사용 편의성을 우선하며, lazy `Sequence`나 복잡한 iterator 계약은 도입하지 않는다.

## 5. 대상별 설계

### 5.1 API 수집/동기화 잡

대상:

- `PlayerRankingCollectJobConfig`
- `ClubRankingCollectJobConfig`
- `BrawlerRankingCollectJobConfig`
- `BattleEventRotationSyncJobConfig`
- `BattleEventModeSyncJobConfig`
- `BrawlerSyncJobConfig`

변경 방향:

- JobConfig는 `BrawlStarsApiTasklet(JOB_NAME) { task.run() }` 형태로 축소한다.
- 수집 로직은 `*Task`로 옮긴다.
- ranking snapshot retention 정리 로직은 task 내부 private 메서드 또는 별도 `RankingSnapshotRetentionPolicy`로 분리한다.
- `BrawlerSync`의 변경 감지는 `BrawlerChangeDetector`로 분리해 단위 테스트 가능하게 한다.
- 기존 alert 호출은 기능 보존 범위에서 유지한다. 새 알림 정책은 추가하지 않는다.

### 5.2 Battle event sync 잡

대상:

- `BattleEventSyncJobConfig`
- 내부 `BattleEventSyncItemWriter`
- 내부 `SoloRankSeasonCache`

변경 방향:

- reader에 `orderBy(id.asc())`를 추가한다.
- 내부 클래스를 별도 파일로 분리한다.
- writer는 `BattleEventSyncItemWriter`로 유지하되, event mismatch detection과 solo rank event creation을 private 메서드 또는 작은 협력 객체로 나눈다.
- writer의 chunk-local cache 정책은 유지한다.

### 5.3 일 단위/주간 통계 잡

대상:

- `BrawlerBattleResultStatsJobConfig`
- `BrawlerBattleRankStatsJobConfig`
- `BrawlerPairBattleResultStatsJobConfig`
- `BrawlerPairBattleRankStatsJobConfig`
- `BrawlerEnemyBattleResultStatsJobConfig`
- `DailyBattleStatsTasklet`
- `PerEventWeeklyBattleStatsTasklet`
- `PerEventDailyBattleStatsTasklet`

변경 방향:

- `PerEventDailyBattleStatsTasklet`은 현재 사용처가 없으므로 삭제한다.
- `DailyBattleStatsTasklet`과 `PerEventWeeklyBattleStatsTasklet`의 제네릭 람다 조립을 줄이고, 각 통계별 `*StatsTask`로 의도를 드러낸다.
- 공통 로직은 `DailyStatsAggregationRunner`, `WeeklyEventStatsAggregationRunner` 같은 composition helper로 둘 수 있으나, 상속은 사용하지 않는다.
- delete/insert는 현재 정확성 보존을 우선한다. upsert 전환은 별도 단계로 검토한다.
- 집계 결과 저장은 chunk flush API를 우선 적용한다.

### 5.4 Brawler item ownership 잡

대상:

- `BrawlerItemOwnershipStatsJobConfig`

변경 방향:

- 가장 먼저 리팩터링할 고위험 후보로 둔다.
- JobConfig에서 300줄 이상의 실행 로직을 제거한다.
- reader, accumulator, writer를 분리한다.
- 수동 `EntityManager` 트랜잭션을 제거한다.
- delete all + insert all은 한 managed transaction 안에서 수행한다.
- count map 구조와 결과 값은 유지한다.
- 기존 nested private key data class는 accumulator 쪽으로 이동한다.

### 5.5 Stats player sync 잡

대상:

- `StatsPlayerSyncJobConfig`
- `StatsPlayerSyncJobItemWriter`

변경 방향:

- rank/result battle reader에 `orderBy(id.asc())`를 추가한다.
- ranking 기반 stats player sync tasklet 로직은 `StatsPlayerSyncFromRankingTask`로 분리한다.
- `StatsPlayerSyncJobItemWriter`는 `writer` 패키지로 이동하거나 이름을 유지하되 JobConfig에서 분리된 책임이 보이게 한다.

## 6. 구현 순서

1. 안전한 구조 기반 정리
   - `BrawlStarsApiTasklet` KDoc/테스트 추가
   - reader `orderBy(id.asc())` 누락 보완
   - 내부 클래스 파일 분리처럼 동작 변화가 거의 없는 작업 수행

2. API 수집 잡 슬림화
   - ranking/rotation/mode sync task 분리
   - `BrawlerSyncTask`, `BrawlerChangeDetector` 분리
   - 기존 기능과 로그를 보존

3. Ownership 잡 리팩터링
   - reader/accumulator/writer 분리
   - 수동 transaction 제거
   - 동일 결과 검증 테스트 추가

4. 통계 집계 잡 리팩터링
   - `DailyBattleStatsTasklet` 사용처를 `*StatsTask` 또는 runner composition으로 대체
   - `PerEventWeeklyBattleStatsTasklet` 사용처를 `*StatsTask` 또는 runner composition으로 대체
   - chunk flush 저장 API 적용

5. 마무리 정리
   - 더 이상 사용하지 않는 tasklet 삭제
   - 패키지 정리
   - 문서와 테스트 갱신

## 7. 테스트 전략

### 7.1 단위 테스트

- `BrawlStarsApiTaskletTest`
  - 일반 action은 한 번 실행된다.
  - `ServiceUnavailable`은 warn 로그 후 정상 종료된다.
  - 그 외 예외는 기존 정책대로 전파된다.

- `BrawlerChangeDetectorTest`
  - star power, gadget, hyper charge, gear 추가/삭제 감지
  - JSON payload 대상 항목 선택

- `BrawlerItemOwnershipStatsAccumulatorTest`
  - trophy range별 total count
  - gear/gadget/star power/hyper charge/buffy owner count
  - chunk가 여러 page로 나뉘어도 결과 동일

- chunk flush 테스트
  - `chunkSize`보다 결과가 많을 때 writer가 여러 번 호출된다.
  - 전체 저장 결과는 기존 `toEntities()` 기준 결과와 같다.

### 7.2 통합/Context 테스트

- `CoreBatchContextTest`는 모든 Job/Step bean 로딩을 유지한다.
- `QuerydslPagingItemReader` KDoc에 안정 정렬 요구사항을 남기고, 정렬이 추가된 reader bean은 context test로 생성 회귀를 확인한다.
- ownership writer는 delete/insert가 managed transaction 안에서 수행되는지 검증한다.

### 7.3 회귀 확인

- `./gradlew :core:core-batch:test`
- 필요 시 `./gradlew :core:core-batch:contextTest`
- 대규모 변경 후 `./gradlew unitTest`

## 8. 성공 기준

- 주요 JobConfig에서 비즈니스 로직 루프와 비교/집계 로직이 제거된다.
- API 점검 시 `BrawlStarsApiTasklet` 사용 잡은 기존처럼 정상 종료된다.
- `QuerydslPagingItemReader` 사용처는 안정 정렬을 가진다.
- `BrawlerItemOwnershipStatsJob`는 수동 `EntityManager` 트랜잭션을 사용하지 않는다.
- 통계 집계 저장은 성능 리스크가 큰 곳부터 chunk flush 방식으로 전환된다.
- 기존 도메인/집계 결과가 테스트로 보존된다.
