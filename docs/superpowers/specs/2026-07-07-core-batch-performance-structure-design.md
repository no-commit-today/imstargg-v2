# core-batch 성능·구조 개선 설계서

- 작성일: 2026-07-07
- 대상 모듈: `imstargg-backend/core/core-batch` (+ `infrastructure/db-core` 통계 집계 엔티티/스키마, `core/core-enum` 파일 분리 1건)
- 우선순위: 성능(예방적) > 구조 정리. 운영성(메트릭·알림)은 비범위.
- 선행 문서: [2026-05-22-core-batch-stats-flatten-design.md](./2026-05-22-core-batch-stats-flatten-design.md), [2026-05-19-batch-short-transactions-design.md](./2026-05-19-batch-short-transactions-design.md), [2026-04-30-core-batch-refactoring-design.md](./2026-04-30-core-batch-refactoring-design.md)

## 0. 배경

core-batch 전체(잡 16개, k8s CronJob 실행)를 예방적으로 검토했다. 현재 체감되는 성능 문제는 없으며, 데이터 증가에 대비한 성능 보강과 코드 품질 정리가 목적이다.

통계 잡 6종(일별 battleresult/battlerank, 주간 pair×2/enemy, ownership)은 "키셋 읽기 → 인메모리 집계 → delete+insert 교체" 패턴으로 정착되어 있고, 이 구조는 2026-05-22 평탄화 설계의 의도적 결정이다. 당시 정책 **"과도한 추상화보다는 중복이 더 괜찮음"** 은 이번 설계에서도 유지한다 — 공통화는 잡 의도(쿼리·삭제 조건)를 숨기지 않는 기계적 보일러플레이트로 한정한다.

## 1. 범위

포함한다.

- 성능 3건: 통계 INSERT 배칭 복구(SEQUENCE 전환), ownership 리더 쿼리 안전화, offset→keyset 페이징 전환
- 구조 4건: 기계적 보일러플레이트 공통화, ownership 집계 도메인 위치 정렬, 데드코드 제거, 파티션 보존↔주간 룩백 가드

포함하지 않는다 (§10 향후 제안 참조).

- single-pass 집계 파이프라인 재설계, 통계 잡 프레임워크화 (정책상 부적합)
- 배치 메트릭/알림 확장 (04-30 설계의 "Grafana 등 별도 도구" 방침 유지), `BATCH_*` 메타테이블 정리
- worker 가 쓰는 원본 `stats_rank_battle`/`stats_result_battle` 의 ID 전략 (파티션 함수 `OVERRIDING SYSTEM VALUE` 가 IDENTITY 전제)
- trio 통계 테이블 2종 (휴면 — schema.sql 에 "현재 API/배치 미제공" 주석)
- RankingCollect 3태스크 통합, 집계기(`domain/statistics`) 로직 변경

## 2. 결정 로그

| 질문 | 결정 |
|---|---|
| 우선 영역 | 성능 최적화 + 구조 리팩터링 (운영성 제외) |
| 현재 페인포인트 | 없음 — 예방적 개선 |
| DB 스키마 변경 | 허용 (수동 마이그레이션 포함) |
| 접근법 | A. 타깃 개선 (프레임워크화 B, 파이프라인 재설계 C 기각) |
| 공통화 수준 | 기계적 보일러플레이트만 — Reader/Writer/Task/JobConfig 클래스는 6종 각자 유지 |
| SEQUENCE 전환 대상 | 집계 테이블 10종 (trio 2종 제외) |

## 3. 성능 개선

### 3.1 통계 INSERT 배칭 복구 — IDENTITY → SEQUENCE

**문제**: 통계 집계 엔티티가 전부 `GenerationType.IDENTITY` 다. Hibernate 는 IDENTITY 전략에서 JDBC insert 배칭을 비활성화하므로, `application.yml` 의 `hibernate.jdbc.batch_size: 100` / `order_inserts: true` 설정이 통계 쓰기(delete+insert 교체)에 전혀 작동하지 않는다. `BrawlerBattleResultStatsWriter` 등의 `em.persist` 루프가 건당 1 왕복으로 실행된다.

**변경**:

- 대상 10종: `brawler_battle_result_stats`, `brawler_battle_rank_stats`, `brawler_pair_battle_result_stats`, `brawler_pair_battle_rank_stats`, `brawler_enemy_battle_result_stats`, ownership 5종(`brawler_gear/gadget/star_power/hyper_charge/buffy_ownership_stats`)
- 엔티티: `@GeneratedValue(strategy = SEQUENCE, generator = ...)` + `@SequenceGenerator(sequenceName = "{table}_seq", allocationSize = 100)` (pooled optimizer — 시퀀스 왕복도 100건당 1회)
- DDL 마이그레이션 `V260707__stats_identity_to_sequence.sql` (멱등):
  1. `create sequence if not exists {table}_seq increment by 100`
  2. `alter table ... alter column ... drop identity if exists`
  3. `set default nextval('{table}_seq')` (구버전 앱 호환용 안전망)
  4. `setval` 로 현재 max(id) 이상으로 초기화
- `schema.sql` 스냅샷 동기 갱신 (두 파일 모두 반영 — DB 컨벤션)

**호환성/배포 순서**: `default nextval` 상태에서는 IDENTITY 를 기대하는 구버전 앱의 insert(getGeneratedKeys) 도 정상 동작한다. 운영 DDL 선적용 → 앱 배포 순서로 진행하며, 롤백 시에도 앱만 되돌리면 된다.

**효과**: 교체 쓰기 구간 INSERT 왕복 약 1/100. 집계 테이블 ID 는 무의미한 대리키라 정합성 영향 없음.

### 3.2 ownership 리더 쿼리 안전화

**문제**: `BrawlerItemOwnershipStatsReader` 는 stats_player 1,000명 페이지당 player_brawler 를 로드한 뒤, 그 ID 목록(플레이어당 브롤러 ~수십 개 → 페이지당 수만~10만 개)을 아이템 5개 테이블 조회의 `IN` 절에 그대로 넣는다. 거대 IN 절은 파싱/플래닝 비용과 실행 계획 불안정을 유발한다.

**변경**:

- 아이템 5테이블 조회를 `playerBrawler.id IN (거대 목록)` → **`playerBrawler.player.brawlStarsTag IN (페이지 태그 1,000개)` join 조건**으로 전환. IN 크기가 페이지 크기로 고정된다.
- 엔티티 전체 로드 대신 QueryDSL **프로젝션**으로 필요한 컬럼만 조회: player_brawler 는 `(id, brawlerBrawlStarsId, trophies)`, 아이템 테이블은 `(playerBrawlerId, itemBrawlStarsId)` 튜플.
- 아이템 5종의 조회 5회 반복은 유지한다 (05-22 설계의 "의도된 명시성" 결정 준수). 스냅샷 조립·`BrawlerItemOwnershipStatsAccumulator` 집계 로직 불변.

### 3.3 offset → keyset 페이징 전환

**문제**: `battleEventSyncJob`/`statsPlayerSyncJob` 의 chunk 스텝이 `QuerydslPagingItemReader`(offset 기반)를 사용한다. offset 페이징은 뒤 페이지로 갈수록 스킵 비용이 누적돼 하루 데이터가 커지면 O(n²) 로 열화된다. 통계 잡 6종은 이미 키셋(`id > lastId`) 루프를 쓰고 있어 모듈 내 패턴도 불일치한다.

**변경**:

- `support/querydsl/KeysetItemReader<T>` 신설: Spring Batch `ItemStreamReader` 구현. `(lastId, JPAQueryFactory) -> JPAQuery<T>` 빌더를 받아 키셋으로 페이지를 읽는다. 기존 `QuerydslEntityCursorItemReader` 의 키셋 접근을 계승하되(05-22 "추후 사용 예정 보존" 의도의 실현), 다음을 바로잡는다:
  - offset 리더 상속 + `UnsupportedOperationException` 해킹 제거 (독립 구현)
  - `lastId` 를 ExecutionContext 에 저장해 **스텝 재시작 시 이어 읽기** 지원 (기존 offset 리더가 제공하던 재시작성 유지)
- `BattleEventSyncJobConfig`, `StatsPlayerSyncJobConfig` 의 리더 빈 4개를 교체. 스텝 이름·chunk 크기·잡 파라미터 불변.

## 4. 구조 정리

### 4.1 기계적 보일러플레이트 공통화

Reader/Writer/Task/JobConfig 클래스는 6종 각자 유지한다. 추출하는 것은 잡 의도가 없는 순수 기계 부분 2가지뿐이다.

- `support/querydsl/KeysetScanner`: 자체 EntityManager 생성/정리 + `while { em.clear(); fetch(lastId); consume }` 루프. 통계 Reader 6종(배틀 5 + ownership)의 동일한 키셋 루프가 이걸 호출하는 3~5줄로 줄어든다. 각 Reader 파일에는 **자기 쿼리(where 조건)가 그대로 남는다**.
- `job/statistics/support/StatsReplaceWriter`: `JpaBatchTransactionRunner` 위에서 `delete(조건) → aggregator.flushEntityChunks(persist+flush+clear)` 를 실행하는 헬퍼. 배틀 통계 Writer 5종은 자기 삭제 조건을 정의하고 이 헬퍼에 위임한다. ownership Writer 는 이미 자체 `replaceTable` 헬퍼(repository 기반 전체 교체)를 갖고 있어 대상에서 제외한다.

`KeysetItemReader`(§3.3)와 `KeysetScanner` 는 같은 키셋 규약을 공유하되, Spring Batch 스텝용/tasklet 콜백용으로 소비 형태가 다르다.

### 4.2 ownership 집계 도메인 위치 정렬

집계 규칙은 `domain/`, IO·잡 배선은 `job/` 이라는 경계를 ownership 에도 적용한다. 로직 변경 없는 패키지 이동:

- `job/statistics/ownership/` → `domain/statistics/ownership/`: `BrawlerItemOwnershipStatsAccumulator`, `BrawlerItemOwnershipStatsResult`, `PlayerBrawlerOwnershipSnapshot`, `BrawlerTrophyKey`, `BrawlerTrophyItemKey`, `BrawlerTrophyBuffyKey`
- Reader/Writer/Task/JobConfig 는 `job/statistics/ownership/` 에 잔류

### 4.3 데드코드 제거·파일 정리

| 대상 | 처리 | 근거 |
|---|---|---|
| `PlayerCombinator` (+테스트) | 삭제 | 사용처 0. trio 통계(휴면) 잔재 — 부활 시 git 에서 복원 |
| `QuerydslEntityCursorItemReader` | 삭제 (`KeysetItemReader` 로 계승) | §3.3 — 05-22 의 "추후 사용" 의도를 정비된 형태로 실현 |
| `QuerydslZeroPagingItemReader` | 삭제 | 사용처 0. read-and-delete 용도인데 해당 패턴의 잡이 없음 |
| `QuerydslPagingItemReader` | 삭제 | §3.3 전환 후 사용처 0 (offset 페이징 근절) |
| `StatsWeekRange.kt` (core-enum) | `WeekRange` data class 를 별도 파일로 분리 | 1파일 1클래스 컨벤션 |

### 4.4 파티션 보존 ↔ 주간 룩백 가드

`PartitionManageJobConfig.RETENTION_DAYS = 10` 과 주간 잡의 최대 룩백(`StatsWeekRange.currentAndPrevious` 기준 8일)은 암묵적 결합이다. 보존 기간을 줄이거나 룩백을 늘리면 주간 재집계가 드롭된 파티션을 읽어 **통계가 조용히 불완전해진다**.

- core-batch 단위 테스트 추가: 임의 기준일들에 대해 `currentAndPrevious` 최대 룩백을 계산하고 `룩백 + 1일 여유 ≤ RETENTION_DAYS` 를 검증
- `RETENTION_DAYS` 를 테스트에서 참조 가능하게 공개(companion `const val` 유지)하고, 양쪽에 상호 참조 KDoc 명시

## 5. 파일 변경 맵

**신규**
- `core-batch/support/querydsl/KeysetScanner.kt`, `KeysetItemReader.kt`
- `core-batch/job/statistics/support/StatsReplaceWriter.kt`
- `core-enum/.../WeekRange.kt` (분리)
- `db-core/.../db/migration/V260707__stats_identity_to_sequence.sql`
- 테스트: `KeysetScannerTest`, `KeysetItemReaderTest`(재시작 포함), `StatsReplaceWriterTest`, 룩백 가드 테스트, ownership 리더 컨텍스트 테스트

**수정**
- 배틀 통계 Reader 5종 → `KeysetScanner` 사용 (쿼리 정의는 잔류)
- 배틀 통계 Writer 5종 → `StatsReplaceWriter` 사용 (삭제 조건은 잔류)
- `BrawlerItemOwnershipStatsReader` → `KeysetScanner` + tag join + 프로젝션 (§3.2)
- `BattleEventSyncJobConfig`, `StatsPlayerSyncJobConfig` → `KeysetItemReader` (§3.3)
- 통계 엔티티 10종 → `@SequenceGenerator` (§3.1), `db-core/schema.sql` 갱신
- ownership 도메인 클래스 6종 패키지 이동 (§4.2) 및 참조 갱신
- `PartitionManageJobConfig`, `StatsWeekRange` KDoc (§4.4)

**삭제**
- `PlayerCombinator.kt` + `PlayerCombinatorTest.kt`
- `QuerydslPagingItemReader.kt`, `QuerydslEntityCursorItemReader.kt`, `QuerydslZeroPagingItemReader.kt`

**불변 (호환성 보장)**
- 잡 이름·스텝 이름·잡 파라미터(`date`, `window`)·검증기 — k8s CronJob `--job.name` 및 `BATCH_*` 메타데이터 호환
- `domain/statistics` 집계기 6종과 그 테스트
- 집계 결과 값 (동작 보존 리팩터링)

## 6. 에러 처리·멱등성

기존 모델을 유지한다. delete+insert 는 잡당 단일 `REQUIRES_NEW` 트랜잭션(주간 잡은 weekRange×event 단위)이라 실패 시 원자적으로 롤백되고, 재실행은 전체 교체로 멱등하다. SEQUENCE 전환·keyset 전환은 이 특성에 영향을 주지 않는다. `KeysetItemReader` 의 ExecutionContext 재시작은 chunk 스텝(battleEventSync/statsPlayerSync)에만 적용된다.

## 7. 테스트 전략

1. **동작 보존 증명**: 기존 Task 테스트 6종 + 컨텍스트 테스트가 무수정 통과
2. **신규 컴포넌트**: §5 신규 테스트 목록. `KeysetItemReader` 는 ExecutionContext 저장/복원 케이스 포함
3. **ownership 리더**: 실 DB 컨텍스트 테스트로 join·프로젝션 결과가 기존과 동일함을 검증
4. **완료 기준**: `./gradlew ktlintCheck unitTest contextTest` 통과. SEQUENCE 배칭은 로컬에서 Hibernate 로그(`generate_statistics`)로 배치 실행 확인

## 8. 단계 분리 (위험도 오름차순 PR 3개)

| PR | 내용 | 위험 |
|---|---|---|
| PR1 | 데드코드 제거(PlayerCombinator, QueryDSL 리더는 PR2 로 연기) + WeekRange 분리 + 룩백 가드 + ownership 도메인 이동 | 없음 (동작 무관) |
| PR2 | KeysetScanner/StatsReplaceWriter/KeysetItemReader + Reader·Writer 정리 + ownership 쿼리 개선 + offset 리더 3종 삭제 | 낮음 (코드만, 테스트로 보존 증명) |
| PR3 | SEQUENCE 전환 (DDL + 엔티티 10종 + schema.sql) | 중간 (운영 DDL 선적용 절차 필요) |

## 9. 리스크와 완화

| 리스크 | 완화 |
|---|---|
| SEQUENCE 전환 후 ID 충돌 | `setval` 을 max(id) 이상으로 초기화, `default nextval` 안전망, DDL 선적용 순서 |
| keyset 전환으로 읽기 결과 차이 | 두 방식 모두 `order by id` 전제 — 스냅샷 격리 하 동일 결과. 컨텍스트 테스트로 검증 |
| 공통화가 잡 의도를 흐림 | 쿼리·삭제 조건을 각 파일에 잔류시키는 경계(§4.1)로 차단. 과거 정책 준수 |
| 재시작 동작 회귀 | `KeysetItemReader` 재시작 테스트로 offset 리더의 재시작성 등가 보장 |

## 10. 향후 제안 (이번 비범위, 기록만)

- **single-pass 일별 집계**: 일별 원본 스캔을 잡 4~5개 → 1회로 축소. 실측 병목이 생기면 검토 (중간 집계 테이블·실패 영향 확대 트레이드오프)
- **배치 관측성**: 04-30 방침대로 Grafana 계열로 해결 — 필요 시 push-gateway 기반 잡 duration/read-write count 메트릭
- **`BATCH_*` 메타테이블 정리 잡**: 실행 이력 무한 증가 대비
- **RankingCollect 3태스크의 country 루프·스냅샷 보존 로직 공통화**: 이득이 작아 보류
- **worker 원본 테이블 saveAll 배칭**: 동일 IDENTITY 제약 — 파티션 함수와 함께 별도 검토
