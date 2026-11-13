# 패치 기준 통계 1단계(새 테이블·체크포인트·집계 잡) 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 옛 통계를 `_backup` 테이블에서 그대로 돌리면서, 패치마다 키별 한 줄에 누적하는 새 통계 테이블 5개와 체크포인트, 3시간마다 도는 집계 잡 5종을 만든다. API 와 화면은 이 단계에서 바뀌지 않는다.

**Architecture:** 새 집계 잡은 엔진 하나(`PatchStatsTask`)에 통계 유형별 규칙 다섯(`PatchStatsAggregation`)을 꽂는 구조다. 체크포인트, 범위 맞추기, 쓰기 트랜잭션은 엔진과 쓰기 클래스 한 곳에만 있고, 유형마다 다른 것은 원본 테이블, 집계기, 컬럼 목록뿐이다. 쓰기는 JDBC 로 직접 하며 H2 와 PostgreSQL 에서 같은 SQL 이 돈다. 옛 통계 엔티티와 리포지토리는 이름에 `Backup` 을 붙여 `_backup` 테이블을 가리키게 하고, 새 엔티티가 원래 이름을 쓴다. 옛 잡과 옛 집계기의 코드는 타입 이름 말고는 건드리지 않는다.

**Tech Stack:** Kotlin + Spring Boot 4.1, Spring Batch 6, JPA + QueryDSL(읽기), `JdbcTemplate` + `TransactionTemplate`(쓰기), H2 2.4.240(테스트, `MODE=PostgreSQL`), PostgreSQL 18(운영, 로컬 확인), MockK.

**Spec:** [docs/superpowers/specs/2026-11-13-patch-based-statistics-design.md](../specs/2026-11-13-patch-based-statistics-design.md). 이 계획이 스펙과 어긋나면 스펙이 이긴다. 이 문서는 스펙 10장의 1단계까지다. 2단계(API·화면 전환, 옛 코드 삭제)와 3단계(옛 테이블 삭제 SQL)는 [2026-11-13-patch-based-statistics-stage2.md](2026-11-13-patch-based-statistics-stage2.md) 에 있다.

## Global Constraints

- `./gradlew` 는 `imstargg-backend/` 에서, **`git` 은 저장소 루트에서** 실행한다. 아래 경로는 모두 저장소 루트 기준이다.
- **패치의 범위**(스펙 5.2): 패치일 다음 날부터 다음 패치일 전날까지. 다음 패치가 없으면 끝이 없다. 패치 당일과 가장 이른 패치 이전의 전투는 어느 패치에도 넣지 않는다. 날짜는 모두 KST 다(`KOREA_ZONE`, `Instant.toKoreaLocalDate()`).
- **지키는 규칙**(스펙 4.2): 한 패치의 통계는 "전투 날짜가 적용한 범위 안이고 id 가 체크포인트 이하"인 원본 전투를 집계한 값과 같다. 모든 쓰기 트랜잭션이 통계 줄과 체크포인트 줄을 함께 바꾼다.
- **쓰기 트랜잭션**(스펙 5.6): 첫 문장은 체크포인트 줄의 조건부 갱신(또는 삭제)이다. 영향받은 줄이 1이 아니면 `IllegalStateException` 을 던지고 통계 줄을 건드리지 않는다. JPA 의 `@Version` 이나 더티 체킹으로 대신하지 않는다(갱신이 커밋 때로 밀린다).
- **SQL**: `insert … on conflict`, `merge` 를 쓰지 않는다. 키로 `update` 하고 갱신된 줄이 없는 키만 `insert` 한다. 시각은 `current_timestamp` 로 넣는다. 한 번에 보내는 묶음은 `BattleStatsConstants.BATCH_SIZE`(1,000) 줄이다(운영 배치의 소켓 읽기 제한은 600초다).
- **상수**: 원본 읽기 한 쪽 5,000건(`BattleStatsConstants.READ_PAGE_SIZE`), 저장 단위 전투 100,000건, 상한 id 의 여유 10분, 상한 id 를 찾는 전투 날짜 최근 8일, 원본 보관 45일 > 등록 기한 30일.
- **예외**(스펙 8장): 잡아서 넘기지 않는다. 잡이 그 자리에서 실패하고 다음 실행이 체크포인트에서 잇는다. 예외는 봇 감시 하나뿐이다(Task 12).
- **옛 코드**: 옛 잡 5종의 잡 이름, 클래스 이름, 동작을 바꾸지 않는다. 바뀌는 것은 타입 이름(`…BackupEntity`, `…BackupJpaRepository`)과 Q클래스 참조뿐이다.
- **테스트**: DB 는 H2 실 DB 를 쓰고 리포지토리를 mockk 로 바꾸지 않는다. 시각은 고정한 `Clock` 을 생성자로 넘긴다. 원본 전투의 `created_at` 은 Hibernate 가 실제 시각으로 채우므로, 테스트는 저장한 뒤 `JdbcTemplate` 으로 `created_at` 을 고쳐 쓴다(Task 8 의 `PatchStatsTestSupport`). 컨텍스트 테스트 서브클래스에 `@Tag("context")` 를 다시 달지 않는다.
- **프로젝트 규칙**: 파일당 클래스 하나, 운영 코드에서 인자가 셋 이상이면 named arguments, 주석·KDoc·테스트 이름은 한국어, 로깅은 kotlin-logging. Q클래스는 static import 한 인스턴스를 그대로 쓴다. 스키마는 `schema.sql` 과 마이그레이션 파일 양쪽에 멱등하게 쓰고, 인덱스와 유니크는 엔티티 `@Table` 에도 적는다. 외래 키 제약과 파티션은 새 테이블에 두지 않는다.
- Kotlin 을 고치기 전에 `superdomain:conventions` 스킬을 읽는다. 새 이름은 `docs/superglossary/glossary.md` 의 단어로 짓는다(Task 1 이 필요한 단어를 등록한다). 이 계획에 적힌 이름은 사전과 대조한 것이니 그대로 쓴다.
- **ktlint**: 태스크마다 커밋 전에 고친 모듈의 `ktlintFormat` 을 돌리고, 이어서 따로 `ktlintCheck` 를 돌린다. 두 태스크를 한 명령에 묶지 않는다. 특수 공백 같은 보이지 않는 문자는 소스에 그대로 쓰지 않고 `\uXXXX` 로 쓴다.
- 커밋은 Conventional Commits 이고 끝에 `Co-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>` 트레일러를 붙인다. `git add` 는 경로를 적어서 한다(`.agents/skills/release-management/` 는 이 작업의 파일이 아니다). 푸시하지 않는다.
- 운영 DB 와 `.secrets/` 에 접근하지 않는다.

## 이름 표

마이그레이션(Task 4), 옛 엔티티(Task 2), 새 엔티티(Task 3)가 모두 이 표를 따른다. H2 는 엔티티에서 스키마를 만들기 때문에 SQL 과 애너테이션이 어긋나도 테스트가 실패하지 않는다. 어긋나면 운영에서 옛 잡의 `nextval` 이 실패한다.

`{t}` 는 테이블 다섯 개다: `brawler_battle_result_stats`, `brawler_battle_rank_stats`, `brawler_enemy_battle_result_stats`, `brawler_pair_battle_result_stats`, `brawler_trio_battle_result_stats`.

**옛 것의 이름을 바꾼다**

| 객체 | 옛 이름 | 바꾼 이름 |
|---|---|---|
| 테이블 | `{t}` | `{t}_backup` |
| PK 인덱스 | `{t}_pkey` | `{t}_backup_pkey` |
| id 시퀀스 | `{t}_seq` | `{t}_backup_seq` |
| 유니크 | `uk_{t}__1` | `uk_{t}_backup__1` |
| 인덱스(1인 승률) | `ix_brawler_battle_result_stats_1`, `…_2` | `ix_brawler_battle_result_stats_backup_1`, `…_backup_2` |
| 인덱스(1인 순위) | `ix_brawler_battle_rank_stats` | `ix_brawler_battle_rank_stats_backup` |
| 인덱스(2인) | `ix_brawler_pair_battle_result_stats__brawler1`, `…__brawler2` | `ix_brawler_pair_battle_result_stats_backup__brawler1`, `…_backup__brawler2` |
| 인덱스(3인) | `ix_brawler_trio_battle_result_stats__brawler1`, `…2`, `…3` | `ix_brawler_trio_battle_result_stats_backup__brawler1`, `…2`, `…3` |
| 인덱스(상대, 운영에만 있을 수 있음) | `ix_brawler_enemy_battle_result_stats_1` | `ix_brawler_enemy_battle_result_stats_backup_1` (`alter index if exists`) |
| CHECK | `ck_brawler_pair_battle_result_stats_ordered`, `ck_brawler_trio_battle_result_stats_ordered`, `ck_brawler_enemy_battle_result_stats_distinct_brawler` | 테이블 이름 자리에 `_backup` 을 넣는다(`ck_brawler_pair_battle_result_stats_backup_ordered` 등) |
| 월 파티션 `{t}_p_YYYYMM` 과 그 인덱스 | 그대로 | 그대로(겹치는 이름이 없다) |

옛 엔티티의 `@Table(name)`, `Index(name)`, `UniqueConstraint(name)`, `@SequenceGenerator` 의 `sequenceName` 이 오른쪽 열과 같아야 한다. 생성기의 `name`(JPA 안에서만 쓰는 이름)은 지금의 낙타 표기를 따라 `…StatsBackupSeq` 로 한다(예: `brawlerPairBattleResultStatsBackupSeq`).

**새 것이 원래 이름을 쓴다**

| 통계 유형(`StatsType`) | 테이블 | 키 컬럼(유니크 `uk_{t}__1` 의 순서, 맨 앞은 `brawlstars_patch_id`) | 값 컬럼 | 더하는 인덱스 |
|---|---|---|---|---|
| `BRAWLER_BATTLE_RESULT` | `brawler_battle_result_stats` | `tier_range`, `event_brawlstars_id`, `brawler_brawlstars_id` | `victory_count`, `defeat_count`, `draw_count`, `total_count`, `star_player_count` | 없음 |
| `BRAWLER_BATTLE_RANK` | `brawler_battle_rank_stats` | `tier_range`, `event_brawlstars_id`, `brawler_brawlstars_id` | `rank1_count` 부터 `rank10_count` 까지, `total_count` | 없음 |
| `BRAWLER_ENEMY_BATTLE_RESULT` | `brawler_enemy_battle_result_stats` | `tier_range`, `brawler_brawlstars_id`, `enemy_brawler_brawlstars_id` | `victory_count`, `defeat_count`, `draw_count`, `total_count` | 없음 |
| `BRAWLER_PAIR_BATTLE_RESULT` | `brawler_pair_battle_result_stats` | `tier_range`, `brawler1_brawlstars_id`, `brawler2_brawlstars_id` | 위와 같은 4개 | `ix_brawler_pair_battle_result_stats__brawler2 (brawlstars_patch_id, tier_range, brawler2_brawlstars_id)` |
| `BRAWLER_TRIO_BATTLE_RESULT` | `brawler_trio_battle_result_stats` | `tier_range`, `brawler1_brawlstars_id`, `brawler2_brawlstars_id`, `brawler3_brawlstars_id` | 위와 같은 4개 | `ix_brawler_trio_battle_result_stats__brawler2 (brawlstars_patch_id, tier_range, brawler2_brawlstars_id)`, `…__brawler3 (…, brawler3_brawlstars_id)` |

- id 컬럼은 `{t}_id bigint generated always as identity primary key`, 엔티티는 `GenerationType.IDENTITY` 다. 시퀀스는 만들지 않는다.
- 공통 컬럼: `brawlstars_patch_id bigint not null`, `tier_range varchar(45) not null`, 값 컬럼은 모두 `bigint not null`, `created_at`·`updated_at` 은 `timestamptz not null default now()`.
- CHECK 는 원래 이름으로 새 테이블에도 둔다(2인 `brawler1 < brawler2`, 3인 `brawler1 < brawler2 < brawler3`, 상대 `brawler <> enemy`).
- 체크포인트: 테이블 `stats_checkpoint`, 컬럼은 스펙 4.2 의 표 그대로(`stats_type varchar(45)`), 유니크 `uk_stats_checkpoint__stats_type_brawlstars_patch_id (stats_type, brawlstars_patch_id)`.

**잡 이름**

| 통계 유형 | 잡 | 스텝 | CronJob | 스케줄(UTC) |
|---|---|---|---|---|
| `BRAWLER_BATTLE_RESULT` | `brawlerBattleResultPatchStatsJob` | `brawlerBattleResultPatchStatsStep` | `battle-result-patch-stats` | `0 */3 * * *` |
| `BRAWLER_BATTLE_RANK` | `brawlerBattleRankPatchStatsJob` | `brawlerBattleRankPatchStatsStep` | `battle-rank-patch-stats` | `10 */3 * * *` |
| `BRAWLER_ENEMY_BATTLE_RESULT` | `brawlerEnemyBattleResultPatchStatsJob` | `brawlerEnemyBattleResultPatchStatsStep` | `enemy-battle-result-patch-stats` | `20 */3 * * *` |
| `BRAWLER_PAIR_BATTLE_RESULT` | `brawlerPairBattleResultPatchStatsJob` | `brawlerPairBattleResultPatchStatsStep` | `pair-battle-result-patch-stats` | `30 */3 * * *` |
| `BRAWLER_TRIO_BATTLE_RESULT` | `brawlerTrioBattleResultPatchStatsJob` | `brawlerTrioBattleResultPatchStatsStep` | `trio-battle-result-patch-stats` | `40 */3 * * *` |

## 실행 전 확인 (컨트롤러)

- **브랜치**: `feature/patch-based-statistics`(`origin/develop` `c9c2e60f` 에서 분기). 지금은 문서 커밋만 있다.
- **`main` 역병합**: 원본 보관 45일은 hotfix(PR #85)로 `main` 에만 있다. 구현을 시작하기 전에 `git merge origin/main` 을 이 브랜치의 첫 코드 커밋으로 넣는다. 충돌은 네 파일이고 이렇게 푼다.
  - `imstargg-backend/gradle.properties`: **`develop` 쪽을 그대로 둔다.** `main` 의 `coreBatchVersion=0.4.5` 를 가져오지 않는다. 병합 뒤 `git diff origin/develop -- imstargg-backend/gradle.properties` 가 비어 있어야 한다(`main` 쪽은 Kotlin 2.2, Boot 4.0.2 라 잘못 고르면 조용히 되돌아간다).
  - `…/job/PartitionManageJobConfig.kt`: `develop` 의 구조를 두고 `RETENTION_DAYS = 45` 와 `main` 이 더한 KDoc 세 줄만 받는다. "임시값" 문구는 Task 1 이 고친다.
  - `imstargg-backend/infrastructure/db-core/src/main/resources/schema.sql`: `develop` 본문을 두고 `stats_rank_battle`, `stats_result_battle` 머리 주석의 보관 기간만 `main` 의 문구로 한다.
  - `imstargg-backend/docs/stats-battle-data-strategy.md`: `develop` 본문을 두고 보관 기간을 말하는 줄만 `main` 의 문구로 한다.
  - 자동 병합되는 것(`imstargg-deploy/apps/prod/*/values.yaml` 의 이미지 태그와 core-worker 환경변수, `docs/release/`)은 그대로 받는다.
  - 병합 뒤 `./gradlew test ktlintCheck` 가 통과해야 한다.
- **운영의 보관 일수**: `origin/main` 의 `imstargg-deploy/apps/prod/core-batch/values.yaml` 이미지 태그가 `0.4.5` 이상인지 본다. 아니면 운영은 아직 10일 보관이다. 구현은 그대로 진행하고 사용자에게 한 줄로 알린다.
- **core-domain**: `imstargg-backend/core/core-domain` 모듈이 생겼으면 멈추고 사용자에게 알린다. 순수 규칙(`PatchDateRange`, 집계기)의 자리가 그 모듈의 규칙으로 바뀐다.
- **H2**: 테스트 클래스패스의 H2 는 2.4.240 이다(2026-11-13 확인). 스펙이 두 단계 쓰기를 확인한 버전과 같다.
- **도커**: Task 4 와 Task 13 은 로컬 PostgreSQL 컨테이너를 쓴다. `docker info` 가 실패하면 두 태스크의 확인을 "하지 못함"으로 기록하고 사용자에게 알린다. 건너뛴 것을 통과로 적지 않는다.
- **커밋 시각**: 09시에서 18시 사이(KST)에 찍힌 커밋은 푸시·PR 전에 그 밖의 시각으로 옮긴다.

## Review Focus

1. **JDBC 드라이버가 배치 갱신의 줄별 결과를 알려 주지 않음**(`Statement.SUCCESS_NO_INFO`, 또는 한 키에 2줄 이상 갱신): 없던 키로 잘못 보고 넣거나 조용히 넘어가지 않고 실패한다. → Task 9 "줄별 결과가 0 이나 1 이 아니면 실패한다".
2. **실행이 자정(KST)을 넘기거나 오래 걸림**: 오늘과 상한 id 는 실행을 시작할 때 한 번 정하고 끝까지 같은 값을 쓴다. 도중에 다시 구하면 패치마다 다른 기준으로 읽는다. → Task 10 "실행 도중에 시각이 흘러도 시작할 때의 오늘과 상한으로 판단한다".
3. **상한 id 가 없거나 체크포인트보다 작음**(최근 8일에 저장된 지 10분 넘은 원본이 없음): 읽지 않고, 체크포인트를 내리지 않는다. → Task 10 "상한 id 가 없으면 읽지 않고 체크포인트를 그대로 둔다".
4. **같은 유형의 두 실행이 같은 패치의 체크포인트 줄을 동시에 만듦**: 한쪽이 실패하고 줄은 하나만 남는다. → Task 9 "같은 유형과 패치의 체크포인트 줄을 두 번 만들면 실패한다".
5. **패치의 범위가 통째로 비게 되는 등록**(패치일 바로 다음 날에 패치를 끼워 넣음): 그 패치의 통계 줄과 체크포인트 줄이 모두 사라지고, 끼운 패치를 지우면 처음부터 다시 채운다. → Task 11 "범위가 통째로 비면 줄이 모두 사라지고 범위가 다시 생기면 처음부터 채운다".

## File Structure

`…enum/` 은 `imstargg-backend/core/core-enum/src/{main,test}/kotlin/com/imstargg/core/enums/`, `…db/` 는 `imstargg-backend/infrastructure/db-core/src/{main,test}/kotlin/com/imstargg/infrastructure/db/core/`, `res/` 는 `imstargg-backend/infrastructure/db-core/src/main/resources/`, `…batch/` 는 `imstargg-backend/core/core-batch/src/{main,test}/kotlin/com/imstargg/core/batch/`, `…api/` 는 `imstargg-backend/core/core-api/src/{main,test}/kotlin/com/imstargg/core/` 다.

| 파일 | 책임 |
|---|---|
| `…enum/StatsType.kt` | 통계 유형 5종 |
| `…db/statistics/Brawler*StatsBackupEntity.kt` 5개, `…db/statistics/repository/Brawler*StatsBackupJpaRepository*.kt` | 옛 통계. 이름만 바꾼 것 |
| `…db/statistics/Brawler*StatsEntity.kt` 5개 | 패치별 통계 줄(새 것) |
| `…db/statistics/StatsCheckpointEntity.kt`, `repository/StatsCheckpointJpaRepository.kt` | 체크포인트 줄과 조회 |
| `res/schema.sql`, `res/db/migration/V{yyMMdd}a__rename_stats_to_backup_and_create_patch_stats.sql` | DDL |
| `…batch/domain/statistics/patch/PatchDateRange.kt` | 패치의 범위 |
| `…batch/domain/statistics/patch/PatchStatsCount.kt`, `PatchStatsAggregator.kt`, `Brawler*PatchStatsAggregator.kt` 5개, `BrawlerEventBattleStatsKey.kt` | 전투를 키별 값으로 모으는 규칙 |
| `…batch/domain/statistics/StatsBotMonitor.kt` | 두 봇 감시의 공통 모양 |
| `…batch/job/statistics/patch/StatsBattleReader.kt`, `StatsResultBattleReader.kt`, `StatsRankBattleReader.kt` | 원본 읽기(상한 id, 가장 이른 날짜, 키셋) |
| `…batch/job/statistics/patch/PatchStatsTable.kt`, `StatsCheckpointUpdate.kt`, `BatchUpdateCounts.kt`, `PatchStatsWriter.kt` | 쓰기 트랜잭션 |
| `…batch/job/statistics/patch/PatchStatsAggregation.kt`, `PatchStatsAggregations.kt` | 통계 유형 하나를 엔진에 꽂는 묶음과 다섯 개의 정의 |
| `…batch/job/statistics/patch/PatchStatsConstants.kt`, `PatchStatsTask.kt`, `PatchStatsRangeAligner.kt` | 한 번의 실행, 범위 맞추기 |
| `…batch/job/statistics/patch/PatchStatsJobConfig.kt` | 잡 5개와 빈 |
| `…batch/job/PartitionManageJobConfig.kt` | 월 파티션 스텝 제거 |

---

### Task 1: 용어, 통계 유형, 보관과 기한의 관계

**Files:**
- Modify: `docs/superglossary/glossary.md`
- Create: `…enum/StatsType.kt`
- Modify: `…batch/job/PartitionManageJobConfig.kt`(KDoc), `res/schema.sql`(주석 두 줄), `imstargg-backend/docs/stats-battle-data-strategy.md`(보관 문구)
- Test: `…batch/job/PartitionRetentionLookbackTest.kt`

**Interfaces:**
- Produces: `enum class StatsType { BRAWLER_BATTLE_RESULT, BRAWLER_BATTLE_RANK, BRAWLER_ENEMY_BATTLE_RESULT, BRAWLER_PAIR_BATTLE_RESULT, BRAWLER_TRIO_BATTLE_RESULT }`. 뒤 태스크가 쓰는 단어 `backup`, `subtract`, `align`, `margin`.

- [ ] **Step 1: 용어 사전에 네 줄을 더한다.** 표는 한글 열의 코드포인트 순이다. 자리를 맞춰 넣는다.

```
| 맞춤 | align |  |  | 적용한 날짜 범위를 지금의 패치 범위에 맞춤 |
| 백업 | backup |  |  | 전환 동안 남겨 두는 옛 통계(`_backup` 테이블) |
| 빼기 | subtract |  |  | 범위에서 빠진 날의 전투를 통계에서 뺌 |
| 여유 | margin |  |  | 저장 중인 트랜잭션이 끝나기를 기다려 두는 시간 |
```

- [ ] **Step 2: 실패하는 테스트를 쓴다.** `PartitionRetentionLookbackTest` 에 더한다.

```kotlin
@Test
fun `원본 보관 일수가 패치일 등록 기한보다 길다`() {
    assertThat(PartitionManageJobConfig.RETENTION_DAYS.toLong())
        .isGreaterThan(BrawlStarsPatchRegistrationPolicy.REGISTRATION_DEADLINE_DAYS)
}
```

- [ ] **Step 3: 실패를 확인한다.** 역병합으로 값이 이미 45 라 바로 통과한다. `RETENTION_DAYS` 를 잠시 30 으로 바꿔 `./gradlew :core:core-batch:test --tests '*PartitionRetentionLookbackTest'` 가 이 테스트에서 실패하는지 보고 45 로 되돌린다.

- [ ] **Step 4: `StatsType` 을 만들고 문구를 고친다.** `RETENTION_DAYS` 의 KDoc 에서 "임시값", "다시 줄인다"를 지우고 이유를 적는다: 범위 맞추기가 등록 기한 30일 안의 날짜를 원본에서 다시 읽으므로 보관이 그보다 길어야 한다(가드: `PartitionRetentionLookbackTest`). `schema.sql` 의 두 주석과 전략 문서의 "임시"도 같은 뜻으로 고친다.

- [ ] **Step 5: 통과를 확인한다.** `./gradlew :core:core-batch:test --tests '*PartitionRetentionLookbackTest'` → 2건 통과. `./gradlew :core:core-enum:ktlintFormat :core:core-batch:ktlintFormat`, 이어서 `./gradlew :core:core-enum:ktlintCheck :core:core-batch:ktlintCheck`.

- [ ] **Step 6: 커밋한다.** `feat(backend): 통계 유형을 더하고 원본 보관과 패치일 등록 기한의 관계를 고정한다`

### Task 2: 옛 통계 엔티티와 리포지토리에 Backup 을 붙인다

동작은 바꾸지 않는다. 이름과 애너테이션의 문자열만 바꾼다.

**Files:**
- Rename + Modify: `…db/statistics/Brawler{BattleResult,BattleRank,EnemyBattleResult,PairBattleResult,TrioBattleResult}StatsEntity.kt` → `…StatsBackupEntity.kt`
- Rename + Modify: `…db/statistics/repository/` 의 `Brawler*StatsJpaRepository.kt` 5개, `…JpaRepositoryCustom.kt` 4개, `…JpaRepositoryCustomImpl.kt` 4개 → `…StatsBackupJpaRepository…`
- Rename: db-core 테스트 `BrawlerPairBattleResultStatsEntityTest`, `BrawlerTrioBattleResultStatsEntityTest` → `…BackupEntityTest`, `repository/ComboBattleResultStatsJpaRepositoryCustomImplTest` → `ComboBattleResultStatsBackupJpaRepositoryCustomImplTest`
- Modify(타입 이름과 Q클래스 참조만): `…api/application/statistics/BattleResultStatisticsRepository.kt`, `BattleRankStatisticsRepository.kt` 와 두 테스트. `…batch/domain/statistics/Brawler*StatsAggregator.kt` 5개, `…batch/job/statistics/*/Brawler*StatsWriter.kt` 5개, 테스트 `BrawlerPairBattleResultStatsAggregatorTest`, `BrawlerTrioBattleResultStatsAggregatorTest`, `BrawlerPairBattleResultStatsWriterTest`
- Test: `…db/statistics/StatsBackupEntityNameTest.kt`(새로 만든다)

**Interfaces:**
- Produces: `BrawlerBattleResultStatsBackupEntity` 등 엔티티 5개, `…StatsBackupJpaRepository`(+`Custom`, `CustomImpl`). 원래 이름 다섯 개가 비어 Task 3 이 쓴다. `ResultCountEmbedded`, `RankCountsEmbedded`, `Brawler*ResultProjection` 4개는 이름을 그대로 둔다.

- [ ] **Step 1: 실패하는 테스트를 쓴다.** `StatsBackupEntityNameTest` 는 리플렉션으로 애너테이션을 이름 표와 견준다. 엔티티 다섯 개 각각에 대해 단언한다.

```kotlin
// 2인 조합의 예. 나머지 넷도 이름 표의 값으로 같은 단언을 쓴다.
val table = BrawlerPairBattleResultStatsBackupEntity::class.java.getAnnotation(Table::class.java)
assertThat(table.name).isEqualTo("brawler_pair_battle_result_stats_backup")
assertThat(table.uniqueConstraints.map { it.name }).containsExactly("uk_brawler_pair_battle_result_stats_backup__1")
assertThat(table.indexes.map { it.name }).containsExactlyInAnyOrder(
    "ix_brawler_pair_battle_result_stats_backup__brawler1",
    "ix_brawler_pair_battle_result_stats_backup__brawler2",
)
// id 필드의 @SequenceGenerator
assertThat(sequenceGenerator.sequenceName).isEqualTo("brawler_pair_battle_result_stats_backup_seq")
assertThat(sequenceGenerator.name).isEqualTo("brawlerPairBattleResultStatsBackupSeq")
```

- [ ] **Step 2: 실패를 확인한다.** `./gradlew :infrastructure:db-core:test --tests '*StatsBackupEntityNameTest'` → 컴파일 실패(`…BackupEntity` 없음).

- [ ] **Step 3: 이름을 바꾼다.** 파일은 `git mv` 로 옮긴다. 엔티티의 애너테이션 문자열을 이름 표의 "바꾼 이름"으로 고치고, KDoc 맨 위에 "패치 기준 통계로 전환하는 동안 남겨 두는 옛 통계다. 2단계에서 지운다"를 적는다. 참조하는 곳은 타입 이름과 Q클래스(`brawlerPairBattleResultStatsBackupEntity` 등)만 바꾼다. 옛 잡과 옛 집계기의 클래스 이름은 바꾸지 않는다.

- [ ] **Step 4: 통과를 확인한다.** `./gradlew test` 전체 통과(이름만 바꿨으므로 기존 테스트가 모두 그대로 통과해야 한다). `git grep -nE 'Brawler(Battle(Result|Rank)|EnemyBattleResult|PairBattleResult|TrioBattleResult)Stats(Entity|JpaRepository)' -- imstargg-backend` → 0건. 고친 세 모듈(db-core, core-api, core-batch)의 `ktlintFormat`, 이어서 `ktlintCheck`.

- [ ] **Step 5: 커밋한다.** `refactor(backend): 옛 통계 엔티티와 리포지토리에 Backup 을 붙인다`

### Task 3: 새 통계 엔티티와 체크포인트 엔티티

**Files:**
- Create: `…db/statistics/BrawlerBattleResultStatsEntity.kt`, `BrawlerBattleRankStatsEntity.kt`, `BrawlerEnemyBattleResultStatsEntity.kt`, `BrawlerPairBattleResultStatsEntity.kt`, `BrawlerTrioBattleResultStatsEntity.kt`, `StatsCheckpointEntity.kt`
- Create: `…db/statistics/repository/StatsCheckpointJpaRepository.kt`
- Test: `…db/statistics/PatchStatsEntityTest.kt`, `…db/statistics/repository/StatsCheckpointJpaRepositoryTest.kt`

**Interfaces:**
- Consumes: `StatsType`(Task 1), `ResultCountEmbedded`, `StatsTierRange`, `BaseTimeEntity`.
- Produces:

```kotlin
class BrawlerBattleResultStatsEntity(brawlStarsPatchId: Long, tierRange: StatsTierRange, eventBrawlStarsId: Long,
    brawlerBrawlStarsId: Long, resultCount: ResultCountEmbedded, starPlayerCount: Long) : BaseTimeEntity()
class BrawlerBattleRankStatsEntity(brawlStarsPatchId: Long, tierRange: StatsTierRange, eventBrawlStarsId: Long,
    brawlerBrawlStarsId: Long, rankCounts: List<Long>) : BaseTimeEntity() {
    fun rankCounts(): List<Long>                 // 0번이 1위. 길이 MAX_RANK
    companion object { const val MAX_RANK = 10 }
}
class BrawlerEnemyBattleResultStatsEntity(brawlStarsPatchId: Long, tierRange: StatsTierRange, brawlerBrawlStarsId: Long,
    enemyBrawlerBrawlStarsId: Long, resultCount: ResultCountEmbedded) : BaseTimeEntity()
class BrawlerPairBattleResultStatsEntity(brawlStarsPatchId: Long, tierRange: StatsTierRange, brawler1BrawlStarsId: Long,
    brawler2BrawlStarsId: Long, resultCount: ResultCountEmbedded) : BaseTimeEntity()
class BrawlerTrioBattleResultStatsEntity(brawlStarsPatchId: Long, tierRange: StatsTierRange, brawler1BrawlStarsId: Long,
    brawler2BrawlStarsId: Long, brawler3BrawlStarsId: Long, resultCount: ResultCountEmbedded) : BaseTimeEntity()
class StatsCheckpointEntity(statsType: StatsType, brawlStarsPatchId: Long, lastReadBattleId: Long,
    appliedStartDate: LocalDate, appliedEndDate: LocalDate?, lastBattleAt: Instant?, version: Long) : BaseTimeEntity()
interface StatsCheckpointJpaRepository : JpaRepository<StatsCheckpointEntity, Long> {
    fun findAllByStatsType(statsType: StatsType): List<StatsCheckpointEntity>
}
```

- [ ] **Step 1: 실패하는 테스트를 쓴다.** 둘 다 `DbCoreContextTest` 를 상속한다.
  - `PatchStatsEntityTest`: "2인 조합은 브롤러 id 가 오름차순이 아니면 만들 수 없다", "3인 조합은 브롤러 id 가 오름차순이 아니면 만들 수 없다", "상대는 자기 자신일 수 없다"(모두 `IllegalArgumentException`), "1인 순위는 순위별 횟수가 10개가 아니면 만들 수 없다", "1인 순위의 합은 순위별 횟수의 합이다"(`rankCounts = listOf(3, 2, 0, 0, 0, 0, 0, 0, 0, 1)` 이면 `totalCount == 6`, 저장 뒤 다시 읽은 `rankCounts()` 가 같다), "같은 키의 줄을 두 번 넣으면 실패한다"(1인 승률 엔티티를 `EntityManager.persist` + `flush` 로 두 번, 유니크 위반 예외).
  - `StatsCheckpointJpaRepositoryTest`: "통계 유형으로 찾는다"(두 유형을 저장하고 한 유형의 줄만 나온다), "끝 날짜와 최근 전투 일시가 없어도 저장한다", "같은 유형과 패치의 줄을 두 번 넣으면 실패한다"(`DataIntegrityViolationException`).

- [ ] **Step 2: 실패를 확인한다.** `./gradlew :infrastructure:db-core:test --tests '*PatchStatsEntityTest' --tests '*StatsCheckpointJpaRepositoryTest'` → 컴파일 실패.

- [ ] **Step 3: 엔티티 여섯 개와 리포지토리를 만든다.** 테이블·컬럼·유니크·인덱스 이름은 이름 표의 "새 것"을 따른다. id 는 `@GeneratedValue(strategy = GenerationType.IDENTITY)`, 컬럼 이름은 `{t}_id`, 체크포인트는 `stats_checkpoint_id`. 값 컬럼은 `updatable = false` 를 달지 않는다. 1인 순위는 `rank1Count` 부터 `rank10Count` 까지 필드 10개와 `totalCount` 를 둔다. `StatsCheckpointEntity.version` 은 평범한 컬럼이다(`@Version` 을 달지 않는다. 고치는 것은 잡의 SQL 이다). KDoc 에는 한 줄의 키, 셈 규칙(옛 엔티티의 KDoc 에서 날짜·주·파티션 이야기를 뺀 것), "잡이 SQL 로 직접 쓴다"를 적는다.

- [ ] **Step 4: 통과를 확인한다.** 위 명령 → 통과. `./gradlew :infrastructure:db-core:test` 전체 통과(`CleanUp` 이 새 테이블을 비울 수 있어야 한다). `ktlintFormat`, `ktlintCheck`.

- [ ] **Step 5: 커밋한다.** `feat(backend): 패치별 통계 엔티티와 체크포인트 엔티티를 더한다`

### Task 4: 스키마와 마이그레이션 SQL

**Files:**
- Modify: `res/schema.sql`
- Create: `res/db/migration/V{yyMMdd}a__rename_stats_to_backup_and_create_patch_stats.sql`. `{yyMMdd}` 는 이 태스크를 구현하는 날(KST)이다. 같은 날짜의 파일이 이미 있으면 접미사를 다음 글자로 한다.

**Interfaces:**
- Consumes: 이름 표.
- Produces: 운영에 적용할 SQL 파일 하나. 파일 이름은 Task 14 의 배포 절과 PR 본문에 적는다.

- [ ] **Step 1: 확인용 PostgreSQL 을 띄우고 옛 스키마를 넣는다.**

```bash
docker run -d --name patch-stats-pg -e POSTGRES_HOST_AUTH_METHOD=trust -e POSTGRES_DB=imstargg -p 55432:5432 postgres:18.3
PSQL="docker exec -i patch-stats-pg psql -v ON_ERROR_STOP=1 -U postgres -d imstargg"
git show origin/develop:imstargg-backend/infrastructure/db-core/src/main/resources/schema.sql | $PSQL
git show origin/develop:imstargg-backend/infrastructure/db-core/src/main/resources/partition-manage.sql | $PSQL
$PSQL -c "select create_month_partitions(current_date, 1)"
```

옛 테이블 다섯 개에 한 줄씩 넣어 둔다(조합 세 개는 이번 달 안의 `start_battle_date` 로). 운영에만 있는 인덱스를 흉내 내려고 `create index ix_brawler_enemy_battle_result_stats_1 on brawler_enemy_battle_result_stats (tier_range, brawler_brawlstars_id, start_battle_date)` 도 만든다.

- [ ] **Step 2: 마이그레이션 SQL 을 쓴다.** 본보기는 `V261104a__combo_stats_redesign.sql` 의 이름 바꾸기 절이다.
  - 머리 주석: 대상, 배경, 배포 순서(이 SQL → core-api·core-batch 배포 → 새 CronJob 추가), 끊기는 구간(스펙 10장), 적용 방법(`psql -v ON_ERROR_STOP=1 -f`), 멱등, 되돌리는 SQL(새 테이블 다섯 개와 `stats_checkpoint` 를 지우고 이름 표의 왼쪽 이름으로 되돌린다. 주석으로 전문을 적는다).
  - 본문은 `set lock_timeout = '3s'; begin; … commit;` 한 트랜잭션이다. 테이블마다 `do $$ … $$` 안에서 "`{t}_backup` 이 없고, `{t}` 가 있고, `{t}` 에 옛 날짜 컬럼(1인은 `battle_date`, 조합은 `start_battle_date`)이 있을 때"만 이름을 바꾼다. 바꾸는 객체는 이름 표의 여덟 줄(월 파티션 줄 제외)이다.
  - 이어서 같은 트랜잭션에서 새 테이블 다섯 개와 `stats_checkpoint` 를 `create table if not exists`, 인덱스를 `create [unique] index if not exists` 로 만든다.

- [ ] **Step 3: 두 번 적용하고 이름을 확인한다.** `$PSQL < <마이그레이션 파일>` 을 두 번 실행한다. 둘 다 오류 없이 끝나야 한다.

```sql
-- 기대: 다섯 테이블 모두 backup 에 1, 새 테이블에 0
select (select count(*) from brawler_pair_battle_result_stats_backup) as backup, (select count(*) from brawler_pair_battle_result_stats) as patch;
-- 기대: 0 줄(옛 이름으로 남은 시퀀스가 없다)
select relname from pg_class where relkind = 'S' and relname ~ '^brawler_.*_stats_seq$';
-- 기대: 값이 나온다(옛 잡의 nextval 이 산다)
select nextval('brawler_pair_battle_result_stats_backup_seq');
-- 기대: 이름 표의 "새 것" 인덱스와 유니크가 모두 있다
select indexname from pg_indexes where tablename in ('brawler_battle_result_stats','brawler_battle_rank_stats','brawler_enemy_battle_result_stats','brawler_pair_battle_result_stats','brawler_trio_battle_result_stats','stats_checkpoint') order by 1;
```

이름 표의 "바꾼 이름" 전부를 `pg_class`, `pg_constraint` 에서 찾아 하나도 빠지지 않았는지 본다. 이어서 머리 주석의 되돌리는 SQL 을 실행해 옛 이름으로 돌아가는지 보고, 마이그레이션을 다시 적용한다.

- [ ] **Step 4: `schema.sql` 을 고친다.** 옛 다섯 테이블의 DDL 을 이름 표의 "바꾼 이름"으로 바꾸고(구조는 그대로), 새 다섯 테이블과 `stats_checkpoint` 의 DDL 을 더한다. 새 컨테이너에 고친 `schema.sql` 만 적용했을 때와 Step 3 의 결과가 같은 객체 이름을 갖는지 `pg_class` 목록을 견줘 확인한다(월 파티션 제외).

- [ ] **Step 5: 커밋한다.** 컨테이너는 Task 13 이 다시 쓰므로 지우지 않는다. `feat(backend): 옛 통계 테이블을 _backup 으로 옮기고 패치별 통계 테이블을 만드는 SQL 을 더한다`

### Task 5: 조합 통계의 월 파티션 관리를 뺀다

이름을 바꾼 뒤에는 월 파티션 함수가 원래 이름(파티션이 아닌 새 테이블)을 찾아 실패한다(스펙 10장).

**Files:**
- Modify: `…batch/job/PartitionManageJobConfig.kt`
- Delete: `…batch/job/MonthPartitionRetentionTest.kt`
- Test: `…batch/job/PartitionManageJobConfigTest.kt`(새로 만든다)
- Modify: `imstargg-backend/docs/stats-battle-data-strategy.md`(7.3 절에 "1단계부터 부르지 않는다. 옛 조합 테이블에는 이번 달과 다음 달 파티션까지만 있다" 한 줄)

**Interfaces:**
- Produces: `partitionManageJob` 의 스텝은 `partitionCreateStep`, `partitionDropStep` 둘뿐이다. `MONTHS_AHEAD`, `monthRetentionCutoff` 는 없다. `res/partition-manage.sql` 의 월 파티션 함수는 2단계에서 지운다.

- [ ] **Step 1: 실패하는 테스트를 쓴다.** `CoreBatchContextTest` 를 상속하고 `@Qualifier(PartitionManageJobConfig.JOB_NAME) Job` 을 주입받는다.

```kotlin
@Test
fun `원본 전투의 일 파티션만 관리한다`() {
    assertThat((job as SimpleJob).stepNames).containsExactly("partitionCreateStep", "partitionDropStep")
}
```

- [ ] **Step 2: 실패를 확인한다.** `./gradlew :core:core-batch:test --tests '*PartitionManageJobConfigTest'` → 스텝이 넷이라 실패.
- [ ] **Step 3: 월 스텝 두 개와 관련 상수·함수·KDoc 을 지운다.** `MonthPartitionRetentionTest` 를 지운다. `PartitionRetentionLookbackTest` 와 `StatsDateRange` 는 그대로 둔다(옛 잡과 옛 API 가 아직 쓴다).
- [ ] **Step 4: 통과를 확인한다.** 위 명령 → 통과. `./gradlew :core:core-batch:test` 전체 통과. `ktlintFormat`, `ktlintCheck`.
- [ ] **Step 5: 커밋한다.** `refactor(backend): 조합 통계의 월 파티션 관리를 파티션 잡에서 뺀다`

### Task 6: 패치의 범위

**Files:**
- Create: `…batch/domain/statistics/patch/PatchDateRange.kt`
- Test: `…batch/domain/statistics/patch/PatchDateRangeTest.kt`

**Interfaces:**
- Produces:

```kotlin
data class PatchDateRange(val startDate: LocalDate, val endDate: LocalDate?) {   // endDate 가 null 이면 끝이 없다
    val isEmpty: Boolean                       // 끝이 있고 시작보다 이르다
    fun contains(date: LocalDate): Boolean
    companion object {
        /** 패치 id → 패치일을 받아 패치 id → 범위를 준다. 범위가 빈 패치도 들어 있다. */
        fun resolveAll(patchDates: Map<Long, LocalDate>): Map<Long, PatchDateRange>
    }
}
```

- [ ] **Step 1: 실패하는 테스트를 쓴다.** 순수 단위 테스트다. `d(4)` 는 `LocalDate.of(2026, 11, 4)` 다.
  - "패치일 다음 날부터 다음 패치일 전날까지다": `{1: d(3), 2: d(11)}` → `1 = PatchDateRange(d(4), d(10))`
  - "마지막 패치는 끝이 없다": 위에서 `2 = PatchDateRange(d(12), null)`
  - "패치일이 연이으면 앞 패치의 범위는 비어 있다": `{1: d(10), 2: d(11)}` → `1 = PatchDateRange(d(11), d(10))`, `isEmpty == true`
  - "패치 당일은 어느 범위에도 없다": 첫 예에서 모든 범위가 `contains(d(11)) == false`, `contains(d(3)) == false`
  - "가장 이른 패치보다 앞선 날짜는 어느 범위에도 없다": `contains(d(2)) == false`
  - "끝이 없는 범위는 시작 뒤의 모든 날짜를 담는다", "패치가 없으면 범위도 없다"(빈 맵), "입력 순서와 무관하다"(맵 순서를 뒤집어도 같다)
- [ ] **Step 2: 실패를 확인한다.** `./gradlew :core:core-batch:test --tests '*PatchDateRangeTest'` → 컴파일 실패.
- [ ] **Step 3: `PatchDateRange` 를 만든다.**
- [ ] **Step 4: 통과를 확인한다.** 위 명령 → 통과. `ktlintFormat`, `ktlintCheck`.
- [ ] **Step 5: 커밋한다.** `feat(backend): 패치일 목록에서 패치의 날짜 범위를 구한다`

### Task 7: 새 집계기 다섯 개

옛 집계기를 고치지 않고 규칙을 옮겨 적는다. 그래서 옛 집계 테스트도 함께 옮기고, 옛 것과 새 것이 같은 숫자를 내는지 대조하는 테스트를 둔다. 전환 조건 2번(옛 통계와 비교)이 이 동등성에 기댄다.

**Files:**
- Create: `…batch/domain/statistics/patch/PatchStatsCount.kt`, `PatchStatsAggregator.kt`, `BrawlerEventBattleStatsKey.kt`, `BrawlerBattleResultPatchStatsAggregator.kt`, `BrawlerBattleRankPatchStatsAggregator.kt`, `BrawlerEnemyBattleResultPatchStatsAggregator.kt`, `BrawlerPairBattleResultPatchStatsAggregator.kt`, `BrawlerTrioBattleResultPatchStatsAggregator.kt`
- Modify: `…batch/domain/statistics/StatsBattleFixtures.kt`(테스트. `statsResultBattle`, `statsRankBattle`, `threeVsThree`, `showdown` 에 `battleKey: String = "battle-key"`, `battleAt: Instant = <지금의 고정값>` 인자를 더한다. 기본값은 지금과 같다)
- Test: `…batch/domain/statistics/patch/` 에 `Brawler*PatchStatsAggregatorTest.kt` 5개, `PatchStatsBotBattleExclusionTest.kt`, `PatchStatsAggregatorParityTest.kt`

**Interfaces:**
- Consumes: `ResultCountAccumulator`, `StarPlayerCountAccumulator`, `BrawlerEnemyBattleStatsKey`, `BrawlerPairBattleStatsKey`, `BrawlerTrioBattleStatsKey`, `StatsBotBattlePolicy`, `StatsTierRange.resolveAll`, `StatsEventModeType.find`, `BattleResult.opposite()`(모두 지금 있는 것).
- Produces:

```kotlin
/** 통계 줄 하나의 키와 더하거나 뺄 값. keys 와 counts 의 순서는 이름 표의 키 컬럼·값 컬럼 순서다(패치는 들어 있지 않다). */
data class PatchStatsCount(val keys: List<Any>, val counts: List<Long>)
/** 묶음(저장 단위 하나, 또는 하루) 하나마다 새로 만든다. */
interface PatchStatsAggregator<TBattle> {
    fun aggregate(battle: TBattle)
    fun toCounts(): List<PatchStatsCount>
}
data class BrawlerEventBattleStatsKey(val tierRange: StatsTierRange, val eventBrawlStarsId: Long, val brawlerBrawlStarsId: Long)
class BrawlerBattleResultPatchStatsAggregator : PatchStatsAggregator<StatsResultBattleEntity>
class BrawlerBattleRankPatchStatsAggregator : PatchStatsAggregator<StatsRankBattleEntity>
class BrawlerEnemyBattleResultPatchStatsAggregator : PatchStatsAggregator<StatsResultBattleEntity>
class BrawlerPairBattleResultPatchStatsAggregator : PatchStatsAggregator<StatsResultBattleEntity>
class BrawlerTrioBattleResultPatchStatsAggregator : PatchStatsAggregator<StatsResultBattleEntity>
```

`keys` 의 값 타입: `tier_range` 는 `StatsTierRange.name`(String), id 는 `Long`. 예: 1인 승률 `PatchStatsCount(keys = listOf("TROPHY_1000_PLUS", 1L, 16000000L), counts = listOf(victory, defeat, draw, total, starPlayer))`.

- [ ] **Step 1: 실패하는 테스트를 쓴다.**
  - 유형별 테스트 5개: 같은 이름의 옛 테스트(`…batch/domain/statistics/Brawler*StatsAggregatorTest`)의 케이스를 모두 옮기고, 엔티티 대신 `toCounts()` 를 단언한다. 날짜·주 범위를 다루던 케이스는 "여러 날짜의 전투가 한 키로 합쳐진다"로 바꾼다.
  - `PatchStatsBotBattleExclusionTest`: `StatsBotBattleExclusionTest` 의 케이스를 새 결과형 집계기 넷에, 순위형의 봇 로비 케이스를 새 순위 집계기에 옮긴다.
  - `PatchStatsAggregatorParityTest`: 유형마다 하나. 날짜가 다른 전투를 섞은 묶음(승·패·무, 티어 구간이 다른 트로피, 스타플레이어, 봇 전투 하나, 쇼다운 순위 여러 개)을 옛 집계기와 새 집계기에 똑같이 넣는다. 옛 엔티티를 날짜·주를 버리고 키별로 합친 값이 새 `toCounts()` 와 같아야 한다. 테스트 이름: "옛 1인 승률 집계와 숫자가 같다" 등.
- [ ] **Step 2: 실패를 확인한다.** `./gradlew :core:core-batch:test --tests '*PatchStats*'` → 컴파일 실패.
- [ ] **Step 3: 집계기 다섯 개를 만든다.** `aggregate` 의 규칙은 같은 이름의 옛 집계기에서 그대로 옮긴다(건너뛰는 조건의 순서까지). 키에서 날짜가 빠진다. 1인 순위는 `RankCountAccumulator` 를 쓰지 않고 키마다 `LongArray(10)` 에 직접 센다(`RankCountsEmbedded` 는 2단계에서 지운다). 순위가 1부터 10 사이가 아니면 지금처럼 `require` 로 실패시킨다. `total_count` 는 값의 합이다.
- [ ] **Step 4: 통과를 확인한다.** `./gradlew :core:core-batch:test --tests '*PatchStats*' --tests '*StatsAggregatorTest' --tests '*StatsBotBattleExclusionTest'` → 통과(옛 테스트도 그대로 통과). `ktlintFormat`, `ktlintCheck`.
- [ ] **Step 5: 커밋한다.** `feat(backend): 전투를 패치 통계의 키별 값으로 모으는 집계기를 더한다`

### Task 8: 원본 읽기

**Files:**
- Create: `…batch/job/statistics/patch/StatsBattleReader.kt`, `StatsResultBattleReader.kt`, `StatsRankBattleReader.kt`
- Create(테스트): `…batch/job/statistics/patch/PatchStatsTestSupport.kt`
- Test: `…batch/job/statistics/patch/StatsBattleReaderTest.kt`

**Interfaces:**
- Consumes: `PatchDateRange`(Task 6), `StatsResultBattleEntity`, `StatsRankBattleEntity`, Q클래스.
- Produces:

```kotlin
interface StatsBattleReader<TBattle : BaseStatsBattleEntity> {
    /** startBattleDate 이후 날짜의 원본 중 created_at 이 createdAtCap 이하인 것의 가장 큰 id. 없으면 null. */
    fun findBattleIdCap(createdAtCap: Instant, startBattleDate: LocalDate): Long?
    /** 원본에 남아 있는 가장 이른 전투 날짜. 원본이 없으면 null. */
    fun findEarliestBattleDate(): LocalDate?
    /** 전투 날짜가 dateRange 안이고 afterBattleId < id <= battleIdCap 인 원본을 id 순으로, 한 쪽씩 넘긴다. */
    fun read(dateRange: PatchDateRange, afterBattleId: Long, battleIdCap: Long, consume: (List<TBattle>) -> Unit)
}
class StatsResultBattleReader(entityManagerFactory: EntityManagerFactory, pageSize: Long) : StatsBattleReader<StatsResultBattleEntity>
class StatsRankBattleReader(entityManagerFactory: EntityManagerFactory, pageSize: Long) : StatsBattleReader<StatsRankBattleEntity>
```

  두 클래스는 `@Component` 가 아니다. Task 12 의 설정이 `BattleStatsConstants.READ_PAGE_SIZE` 로 빈을 만들고, 테스트는 작은 쪽 크기로 직접 만든다.
- 테스트 지원(뒤 태스크가 모두 쓴다):

```kotlin
/** 컨텍스트 테스트에서 직접 만들어 쓴다. */
class PatchStatsTestSupport(jdbcTemplate: JdbcTemplate, resultBattleJpaRepository: StatsResultBattleJpaRepository,
    rankBattleJpaRepository: StatsRankBattleJpaRepository) {
    /** 저장한 뒤 created_at 을 createdAt 으로 고친다. 같은 날짜의 전투는 픽스처의 battleKey 인자로 키를 다르게 준다. */
    fun saveResultBattle(battle: StatsResultBattleEntity, createdAt: Instant): StatsResultBattleEntity
    fun saveRankBattle(battle: StatsRankBattleEntity, createdAt: Instant): StatsRankBattleEntity
    /** 통계 테이블에서 한 패치의 줄을 키(문자열로 바꾼 값) → 값으로 읽는다. 컬럼 순서는 이름 표를 따른다. */
    fun statsOf(tableName: String, keyColumns: List<String>, countColumns: List<String>, brawlStarsPatchId: Long): Map<List<String>, List<Long>>
    /** 전투 묶음을 새 집계기로 처음부터 집계한 값. statsOf 와 같은 모양이다. */
    fun <TBattle> expectedOf(aggregator: PatchStatsAggregator<TBattle>, battles: List<TBattle>): Map<List<String>, List<Long>>
}
```

- [ ] **Step 1: 실패하는 테스트를 쓴다.** `CoreBatchContextTest` 를 상속하고 `@AfterEach` 에서 `cleanUp.all()`. 결과형 읽기로 아래를 쓰고, 순위형은 "순위형 원본도 같은 규칙으로 읽는다" 한 건으로 확인한다. 쪽 크기는 2 로 만든다.
  - "저장된 지 오래된 전투 중 가장 큰 id 가 상한이다": `created_at` 이 상한 시각 이하인 것 둘, 뒤인 것 하나 → 앞의 둘 중 큰 id
  - "기준 날짜보다 앞선 전투만 있으면 상한이 없다" → `null`
  - "남아 있는 가장 이른 전투 날짜를 준다", "원본이 없으면 가장 이른 날짜가 없다"
  - "날짜 범위 안에서 체크포인트 뒤부터 상한까지 id 순으로 읽는다": 범위 앞·안·뒤 날짜, id 가 `afterBattleId` 이하·사이·`battleIdCap` 초과인 전투를 섞어 저장하고 넘어온 id 목록을 단언한다
  - "끝이 없는 범위는 시작 날짜 뒤를 모두 읽는다"
  - "쪽 경계를 넘겨도 빠지거나 겹치지 않는다": 5건, 쪽 크기 2 → 3번에 나뉘어 5건이 한 번씩
- [ ] **Step 2: 실패를 확인한다.** `./gradlew :core:core-batch:test --tests '*StatsBattleReaderTest'` → 컴파일 실패.
- [ ] **Step 3: 읽기 클래스 둘과 테스트 지원을 만든다.** 지금의 `BrawlerBattleResultStatsReader` 와 같은 키셋 방식이다(자체 `EntityManager`, 쪽마다 `em.clear()`, 트랜잭션 밖). 상한 id 는 `select max(id) … where battle_date >= ? and created_at <= ?` 다.
- [ ] **Step 4: 통과를 확인한다.** 위 명령 → 통과. `ktlintFormat`, `ktlintCheck`.
- [ ] **Step 5: 커밋한다.** `feat(backend): 패치 통계가 원본 전투를 범위와 id 구간으로 읽는다`

### Task 9: 쓰기 트랜잭션

**Files:**
- Create: `…batch/job/statistics/patch/PatchStatsTable.kt`, `StatsCheckpointUpdate.kt`, `BatchUpdateCounts.kt`, `PatchStatsWriter.kt`
- Test: `…batch/job/statistics/patch/BatchUpdateCountsTest.kt`, `PatchStatsWriterTest.kt`

**Interfaces:**
- Consumes: `PatchStatsCount`(Task 7), `PatchDateRange`(Task 6), `StatsType`, `StatsCheckpointJpaRepository`(테스트에서 읽기), `PatchStatsTestSupport`(Task 8).
- Produces:

```kotlin
/** 통계 테이블 하나의 모양. keyColumns 에 brawlstars_patch_id 는 넣지 않는다. */
class PatchStatsTable(val name: String, val keyColumns: List<String>, val countColumns: List<String>)
/** 체크포인트 줄을 고칠 값. version 은 줄을 읽었을 때의 수정 번호다. */
data class StatsCheckpointUpdate(val checkpointId: Long, val version: Long, val lastReadBattleId: Long,
    val appliedStartDate: LocalDate, val appliedEndDate: LocalDate?, val lastBattleAt: Instant?)
/** 배치 갱신이 돌려준 줄별 결과. */
class BatchUpdateCounts(counts: IntArray) {
    fun zeroIndexes(): List<Int>     // 갱신된 줄이 없는 자리. 0 이나 1 이 아닌 값이 있으면 IllegalStateException
    fun requireAllOne()              // 1 이 아닌 값이 있으면 IllegalStateException
}
@Component
class PatchStatsWriter(jdbcTemplate: JdbcTemplate, txManager: PlatformTransactionManager) {
    fun createCheckpoint(statsType: StatsType, brawlStarsPatchId: Long, dateRange: PatchDateRange)
    fun add(table: PatchStatsTable, brawlStarsPatchId: Long, checkpoint: StatsCheckpointUpdate, counts: List<PatchStatsCount>)
    fun subtract(table: PatchStatsTable, brawlStarsPatchId: Long, checkpoint: StatsCheckpointUpdate, counts: List<PatchStatsCount>)
    /** 범위의 마지막 날을 뺀다. 체크포인트 줄을 지운다. */
    fun subtractLast(table: PatchStatsTable, brawlStarsPatchId: Long, checkpointId: Long, version: Long, counts: List<PatchStatsCount>)
    /** 지워진 패치의 통계 줄과 체크포인트 줄을 지운다. */
    fun deletePatch(table: PatchStatsTable, brawlStarsPatchId: Long, checkpointId: Long)
}
```

각 메서드가 실행하는 문장(순서대로). `add`, `subtract`, `subtractLast`, `createCheckpoint` 는 호출 하나가 `PROPAGATION_REQUIRES_NEW` 트랜잭션 하나다.

| 메서드 | 문장 |
|---|---|
| `createCheckpoint` | `insert into stats_checkpoint (stats_type, brawlstars_patch_id, last_read_battle_id, applied_start_date, applied_end_date, last_battle_at, version, created_at, updated_at) values (?, ?, 0, ?, ?, null, 0, current_timestamp, current_timestamp)` |
| `add` | ① `update stats_checkpoint set last_read_battle_id = ?, applied_start_date = ?, applied_end_date = ?, last_battle_at = ?, version = version + 1, updated_at = current_timestamp where stats_checkpoint_id = ? and version = ?` → 1줄이 아니면 실패 ② 키마다 `update {t} set c = c + ? …, updated_at = current_timestamp where brawlstars_patch_id = ? and k = ? …` 를 묶음으로 → `zeroIndexes()` ③ 그 자리의 키만 `insert into {t} (brawlstars_patch_id, k…, c…, created_at, updated_at) values (…, current_timestamp, current_timestamp)` |
| `subtract` | ① `add` 의 ①과 같다 ② 키마다 `update {t} set c = c - ? … where brawlstars_patch_id = ? and k = ? … and c >= ? …`(값 컬럼마다 조건) → `requireAllOne()` ③ `delete from {t} where brawlstars_patch_id = ? and total_count = 0` |
| `subtractLast` | ① `delete from stats_checkpoint where stats_checkpoint_id = ? and version = ?` → 1줄이 아니면 실패 ② ③ 은 `subtract` 와 같다 |
| `deletePatch` | `StatsTierRange.entries` 마다 트랜잭션 하나씩 `delete from {t} where brawlstars_patch_id = ? and tier_range = ?`. 끝으로 트랜잭션 하나에서 `delete from stats_checkpoint where stats_checkpoint_id = ?` |

- [ ] **Step 1: 실패하는 테스트를 쓴다.**
  - `BatchUpdateCountsTest`(순수 단위): "갱신된 줄이 없는 자리를 준다"(`[1, 0, 1, 0]` → `[1, 3]`), **"줄별 결과가 0 이나 1 이 아니면 실패한다"**(`[1, Statement.SUCCESS_NO_INFO]`, `[2]`, `[Statement.EXECUTE_FAILED]` 각각 `IllegalStateException`), "모두 1 이 아니면 실패한다"(`[1, 0]`).
  - `PatchStatsWriterTest`(`CoreBatchContextTest`). 테이블은 이름 표대로 1인 승률과 1인 순위의 `PatchStatsTable` 을 테스트에서 직접 만든다.
    - "없던 키는 넣고 있던 키는 더한다": 키 A 를 `[3, 1, 0, 4, 1]` 로 더한 뒤, A `[1, 0, 0, 1, 0]` 과 새 키 B `[0, 2, 0, 2, 0]` 를 함께 더한다 → A `[4, 1, 0, 5, 1]`, B `[0, 2, 0, 2, 0]`. 체크포인트 줄의 `last_read_battle_id`, 범위, `last_battle_at` 이 넘긴 값이고 `version` 이 2 다.
    - "1인 순위는 순위별 컬럼에 더한다"(값 11개)
    - "수정 번호가 다르면 실패하고 통계 줄은 그대로다": 낡은 `version` 으로 `add` → `IllegalStateException`, 통계 줄과 체크포인트 줄이 호출 전과 같다(JDBC 쓰기가 같은 트랜잭션에 묶였는지를 이 테스트가 고정한다)
    - "빼면 값이 줄고 합이 0 이 된 줄은 지워진다"
    - "뺄 줄이 없으면 실패하고 아무것도 바뀌지 않는다", "빼면 음수가 되는 값이 있으면 실패하고 아무것도 바뀌지 않는다"(체크포인트 줄도 그대로)
    - "마지막 날을 빼면 체크포인트 줄도 지워진다", "마지막 날을 뺄 때 수정 번호가 다르면 실패한다"
    - **"같은 유형과 패치의 체크포인트 줄을 두 번 만들면 실패한다"**(줄은 하나)
    - "끝 날짜와 최근 전투 일시가 없어도 고친다"(둘 다 `null`)
    - "패치를 지우면 그 패치의 통계 줄과 체크포인트 줄만 사라진다"(다른 패치의 줄은 남는다)
    - "한 묶음보다 많은 키도 모두 쓴다"(키 1,001개)
- [ ] **Step 2: 실패를 확인한다.** `./gradlew :core:core-batch:test --tests '*BatchUpdateCountsTest' --tests '*PatchStatsWriterTest'` → 컴파일 실패.
- [ ] **Step 3: 네 클래스를 만든다.** `TransactionTemplate(txManager)` 에 `PROPAGATION_REQUIRES_NEW` 를 건다. SQL 문자열은 `PatchStatsTable` 의 컬럼 목록으로 조립한다(값은 모두 바인딩한다). `null` 일 수 있는 날짜와 일시는 SQL 타입을 명시해 넘긴다(`Types.DATE`, `Types.TIMESTAMP_WITH_TIMEZONE`). `Instant` 는 `OffsetDateTime`(UTC)으로 바꿔 넘긴다(PostgreSQL 드라이버가 `Instant` 를 직접 받지 않는다. H2 테스트로는 드러나지 않고 Task 13 이 확인한다). 묶음은 `BattleStatsConstants.BATCH_SIZE` 줄씩 `batchUpdate` 한다.
- [ ] **Step 4: 통과를 확인한다.** 위 명령 → 통과. "없던 키는 넣고 있던 키는 더한다"가 통과하면 스펙 12장의 첫 항목(H2 에서 두 단계 쓰기)이 닫힌다. `ktlintFormat`, `ktlintCheck`.
- [ ] **Step 5: 커밋한다.** `feat(backend): 패치 통계 줄과 체크포인트 줄을 한 트랜잭션으로 쓴다`

### Task 10: 한 번의 실행 — 줄 만들기와 평소 읽기

**Files:**
- Create: `…batch/job/statistics/patch/PatchStatsConstants.kt`, `PatchStatsAggregation.kt`, `PatchStatsAggregations.kt`, `PatchStatsTask.kt`
- Test: `…batch/job/statistics/patch/PatchStatsTaskTest.kt`

**Interfaces:**
- Consumes: Task 6–9 의 것 전부, `BrawlStarsPatchJpaRepository`, `StatsCheckpointJpaRepository`, `KOREA_ZONE`, `toKoreaLocalDate()`.
- Produces:

```kotlin
object PatchStatsConstants {
    const val CHECKPOINT_BATTLE_COUNT = 100_000L          // 전투를 이만큼 읽을 때마다 저장한다
    val BATTLE_ID_CAP_MARGIN: Duration = Duration.ofMinutes(10)
    const val BATTLE_ID_CAP_BATTLE_DAYS = 8L              // 적재가 받는 가장 오래된 전투(7일 전)에 하루를 더했다
}
/** 통계 유형 하나를 집계 잡에 꽂는 묶음. */
class PatchStatsAggregation<TBattle : BaseStatsBattleEntity>(
    val statsType: StatsType, val table: PatchStatsTable, val reader: StatsBattleReader<TBattle>,
    val createAggregator: () -> PatchStatsAggregator<TBattle>)
/** 다섯 유형의 정의. 테이블 이름과 컬럼 목록은 이름 표 그대로다. */
class PatchStatsAggregations(resultBattleReader: StatsBattleReader<StatsResultBattleEntity>, rankBattleReader: StatsBattleReader<StatsRankBattleEntity>) {
    val brawlerBattleResult: PatchStatsAggregation<StatsResultBattleEntity>
    val brawlerBattleRank: PatchStatsAggregation<StatsRankBattleEntity>
    val brawlerEnemyBattleResult: PatchStatsAggregation<StatsResultBattleEntity>
    val brawlerPairBattleResult: PatchStatsAggregation<StatsResultBattleEntity>
    val brawlerTrioBattleResult: PatchStatsAggregation<StatsResultBattleEntity>
}
class PatchStatsTask(patchJpaRepository: BrawlStarsPatchJpaRepository, checkpointJpaRepository: StatsCheckpointJpaRepository,
    writer: PatchStatsWriter, clock: Clock, checkpointBattleCount: Long) {
    fun <TBattle : BaseStatsBattleEntity> execute(aggregation: PatchStatsAggregation<TBattle>)
}
```

  `PatchStatsAggregations` 와 `PatchStatsTask` 는 `@Component` 가 아니다(Task 12 의 설정이 빈으로 만든다).

**`execute` 가 하는 일**(스펙 5.3 의 1, 2, 5번. 3, 4번은 Task 11 이 사이에 넣는다)

1. `now = clock.instant()`, `today = now.toKoreaLocalDate()` 를 한 번 구해 끝까지 쓴다. 상한 id 는 `reader.findBattleIdCap(createdAtCap = now - BATTLE_ID_CAP_MARGIN, startBattleDate = today - BATTLE_ID_CAP_BATTLE_DAYS)` 다.
2. 패치 목록으로 `PatchDateRange.resolveAll` 을 구한다.
3. 패치일이 이른 패치부터 차례로 한다.
   - **줄이 없는 패치**: 범위가 비었거나, `startDate > today` 이거나, 원본의 가장 이른 날짜가 없거나 `startDate` 보다 늦으면 건너뛴다. 아니면 `writer.createCheckpoint` 로 줄을 만든다.
   - **줄의 적용한 범위가 지금의 범위와 다른 패치**: 읽지 않는다(Task 11 이 그 앞에서 맞춘다).
   - **읽기**: 상한 id 가 없거나 체크포인트 이하이면 아무것도 하지 않는다. 아니면 `reader.read(범위, afterBattleId = 체크포인트, battleIdCap = 상한)` 으로 읽으며 집계기에 모은다. 한 쪽을 받은 뒤 마지막 저장 뒤로 읽은 전투가 `checkpointBattleCount` 이상이면 `writer.add` 로 저장한다(체크포인트 = 그 쪽의 마지막 id, 새 집계기로 바꾼다). 다 읽으면 남은 것을 `writer.add` 로 저장하며 체크포인트를 상한 id 로 올린다. 읽은 전투가 없어도 이 마지막 저장은 한다.
   - **최근 전투 일시**: 저장할 때마다 "지금 값과 그 묶음에서 가장 늦은 `battleAt` 중 큰 것"으로 한다. 묶음에 전투가 없으면 그대로다.
   - 저장할 때마다 수정 번호가 1 오른다. 다음 저장은 오른 번호를 넘긴다.

- [ ] **Step 1: 실패하는 테스트를 쓴다.** `CoreBatchContextTest` 를 상속한다. 읽기는 쪽 크기 2, `checkpointBattleCount = 4` 로 만든다. 고정 시각은 `2026-11-20T10:00+09:00`, 패치는 `2026-11-03` 과 `2026-11-11`, 전투의 `created_at` 은 기본으로 고정 시각의 한 시간 전이다. 통계 값의 단언은 모두 `support.statsOf(…)` 가 `support.expectedOf(…)`(그 패치의 범위 안이고 id 가 체크포인트 이하인 전투를 처음부터 집계한 값)와 같은지로 한다. 1인 승률로 쓴다.
  - "체크포인트 뒤의 전투만 더한다": 한 번 돌리고, 전투를 더 넣고 다시 돌린다
  - "두 번 돌려도 숫자가 같다"
  - "저장 단위에서 끊겨도 다음 실행이 이어서 한다": 전투 10건. 세 번째 쪽에서 예외를 던지는 `StatsBattleReader` 대역(진짜 읽기를 감싼 테스트 클래스)으로 한 번 돌려 실패시키고, 체크포인트가 4번째 전투의 id 인지 본 뒤, 진짜 읽기로 다시 돌린다
  - "새 전투가 없어도 체크포인트가 상한 id 로 오른다": 범위 밖 날짜의 전투만 새로 넣는다
  - "패치 당일의 전투는 어느 패치에도 들어가지 않는다"(11-11 의 전투)
  - "늦게 들어온 전투는 지난 패치에 들어간다"(두 번째 실행 전에 넣은 11-10 의 전투가 11-03 패치로)
  - "가장 이른 패치보다 앞선 날짜의 전투는 집계하지 않는다"(11-02)
  - "범위의 첫 날이 원본에 없으면 줄을 만들지 않는다": 원본이 11-06 부터 있으면 11-03 패치의 줄은 없고 11-11 패치의 줄만 생긴다
  - "범위에 오늘까지의 날짜가 없으면 줄을 만들지 않는다": 패치일이 오늘(11-20)인 패치
  - "저장된 지 10분이 안 된 전투는 다음 실행으로 미룬다": `created_at` 이 고정 시각 5분 전인 전투는 이번에 빠지고, 시각을 20분 뒤로 옮긴 다음 실행에서 들어간다
  - **"상한 id 가 없으면 읽지 않고 체크포인트를 그대로 둔다"**: 줄이 있는 상태에서 고정 시각을 9일 뒤로 옮기고 돌린다. 체크포인트와 수정 번호가 그대로다
  - **"실행 도중에 시각이 흘러도 시작할 때의 오늘과 상한으로 판단한다"**: 첫 호출에는 `2026-11-20T23:59+09:00`, 그 뒤로는 `2026-11-21T00:21+09:00` 을 주는 `Clock` 대역으로 돌린다. 패치일이 11-20 인 패치의 줄이 생기지 않고, `created_at` 이 `23:55` 인 전투가 어느 패치에도 들어가지 않는다
  - "더한 전투의 가장 늦은 일시가 최근 전투 일시가 된다", "새 전투가 없으면 최근 전투 일시는 그대로다"
  - "다섯 유형이 각각 자기 테이블에 쓴다": `PatchStatsAggregations` 의 다섯 정의로 한 번씩 돌린다. 결과형과 순위형 전투를 섞어 넣고, 다섯 테이블이 각각 `expectedOf` 와 같다
- [ ] **Step 2: 실패를 확인한다.** `./gradlew :core:core-batch:test --tests '*PatchStatsTaskTest'` → 컴파일 실패.
- [ ] **Step 3: 네 클래스를 만든다.**
- [ ] **Step 4: 통과를 확인한다.** 위 명령 → 통과. `ktlintFormat`, `ktlintCheck`.
- [ ] **Step 5: 커밋한다.** `feat(backend): 체크포인트 뒤의 전투를 패치 통계에 더하는 실행 흐름을 더한다`

### Task 11: 지워진 패치 정리와 범위 맞추기

**Files:**
- Create: `…batch/job/statistics/patch/PatchStatsRangeAligner.kt`
- Modify: `…batch/job/statistics/patch/PatchStatsTask.kt`(생성자에 `rangeAligner: PatchStatsRangeAligner` 를 더하고, 평소 읽기 앞에 두 단계를 넣는다), `PatchStatsTaskTest.kt`(생성자 인자)
- Test: `…batch/job/statistics/patch/PatchStatsRangeAlignTest.kt`

**Interfaces:**
- Consumes: Task 9 의 `subtract`, `subtractLast`, `add`, `deletePatch`. Task 10 의 `PatchStatsTask`.
- Produces:

```kotlin
@Component
class PatchStatsRangeAligner(writer: PatchStatsWriter) {
    /** 체크포인트 줄의 적용한 범위를 dateRange 에 하루씩 맞춘다. 범위가 다 비면 줄이 지워진다. */
    fun <TBattle : BaseStatsBattleEntity> align(aggregation: PatchStatsAggregation<TBattle>, checkpoint: StatsCheckpointEntity,
        dateRange: PatchDateRange, today: LocalDate)
}
```

**`execute` 에 넣는 두 단계**(패치의 범위를 구한 뒤, 줄 만들기와 읽기 앞)

- **지워진 패치 정리**: 이 유형의 체크포인트 줄 가운데 패치 목록에 없는 패치의 줄마다 `writer.deletePatch` 를 부른다.
- **범위 맞추기**: 남은 줄 가운데 적용한 범위가 지금의 범위와 다른 줄마다 `rangeAligner.align` 을 부른다. 끝나면 체크포인트 줄을 다시 읽고 Task 10 의 3번으로 간다.

**`align` 의 순서**(스펙 5.4). 적용한 범위를 A, 지금의 범위를 R 이라 한다. A 의 끝이 없으면 A 의 마지막 날은 `today` 다. 한 날이 트랜잭션 하나다. 날마다 `reader.read(PatchDateRange(그날, 그날), afterBattleId = 0, battleIdCap = 체크포인트)` 로 읽어 새 집계기에 모은다. 체크포인트(`last_read_battle_id`)는 바꾸지 않는다.

1. **끝 줄이기**: A 가 남아 있고 A 의 마지막 날이 R 에 없는 동안, 그날을 `subtract` 하고 적용한 끝을 그 전날로 한다.
2. **앞 줄이기**: A 가 남아 있고 A 의 첫 날이 R 에 없는 동안, 그날을 `subtract` 하고 적용한 시작을 그 다음 날로 한다.
3. 1, 2 에서 빼려는 날이 A 에 남은 마지막 하루이면 `subtract` 대신 `subtractLast` 로 빼고 끝낸다.
4. **앞 늘리기**: 적용한 시작이 R 의 시작보다 늦은 동안, 시작 전날을 `add` 하고 적용한 시작을 그날로 한다.
5. **끝 늘리기**: R 의 끝이 있으면, 적용한 끝이 그보다 이른 동안 끝 다음 날을 `add` 하고 적용한 끝을 그날로 한다. R 의 끝이 없으면, 끝 다음 날부터 `today` 까지 하루씩 `add` 하고, `today` 를 더하는 트랜잭션에서 적용한 끝을 `null` 로 한다(적용한 끝이 이미 `today` 이상이면 더하는 것 없이 `null` 로만 고친다).
6. **최근 전투 일시**: 끝을 줄일 때는 "지금 값과 새 끝 날짜의 다음 날 0시(KST) 중 작은 것"으로 한다. 더할 때는 "지금 값과 더한 전투의 가장 늦은 `battleAt` 중 큰 것"으로 한다. 값이 없으면 줄일 때는 그대로 없다.

- [ ] **Step 1: 실패하는 테스트를 쓴다.** `CoreBatchContextTest`. Task 10 의 테스트와 같은 고정 시각과 지원 클래스를 쓴다. 장면마다 "패치 목록을 바꾼다 → 잡을 돌린다 → 모든 패치의 통계가 `expectedOf`(바뀐 범위 안이고 id 가 체크포인트 이하인 전투를 처음부터 집계한 값)와 같고, 체크포인트 줄의 적용한 범위가 지금의 범위와 같다"를 단언한다. 원본은 11-04 부터 11-19 까지 날마다 두 건 이상 넣는다.
  - "패치를 등록하면 앞 패치에서 그날부터의 전투가 빠지고 새 패치가 채워진다": 11-03 만 있는 채로 돌린 뒤 11-11 을 등록한다. 11-03 패치는 4일부터 10일까지, 11-11 패치는 12일부터. 11일의 전투는 어디에도 없다
  - "패치를 삭제하면 앞 패치가 끝 없음으로 넓어지고 지운 패치의 줄이 사라진다"
  - "패치일을 뒤로 고치면 앞 패치는 넓어지고 그 패치는 줄어든다"(11-11 을 11-12 로. `BrawlStarsPatchEntity.update`)
  - "패치를 사이에 끼워 넣으면 앞 패치가 줄고 끼운 패치가 처음부터 채워진다"(11-07)
  - "맞추는 도중에 실패해도 다시 돌리면 결과가 같다": 세 번째 날의 읽기에서 예외를 던지는 대역으로 한 번, 진짜 읽기로 한 번
  - "끝을 줄이면 최근 전투 일시가 새 끝 날짜의 다음 날 0시를 넘지 않는다": 첫 장면 뒤 11-03 패치의 `last_battle_at <= 2026-11-11T00:00+09:00`
  - "더한 날의 전투가 더 늦으면 최근 전투 일시가 오른다"
  - **"범위가 통째로 비면 줄이 모두 사라지고 범위가 다시 생기면 처음부터 채운다"**: 11-04 를 등록하면 11-03 패치의 통계 줄과 체크포인트 줄이 0건이 된다. 11-04 를 지우고 돌리면 11-03 패치가 다시 `expectedOf` 와 같다
  - "끝이 없던 줄은 오늘부터 거슬러 뺀다": 첫 장면에서 11-03 패치의 줄이 11-20, 11-19 … 11-11 순서로 줄어든다(읽기 대역이 받은 날짜 순서를 단언한다)
  - "어드민에서 지운 패치의 통계 줄과 체크포인트 줄을 지운다"(다른 패치의 줄은 남는다)
  - "범위가 같은 패치는 다시 읽어 빼거나 더하지 않는다": 패치 목록을 바꾸지 않고 두 번 돌린 뒤, 수정 번호가 평소 읽기의 한 번씩만 올랐다
- [ ] **Step 2: 실패를 확인한다.** `./gradlew :core:core-batch:test --tests '*PatchStatsRangeAlignTest'` → 컴파일 실패.
- [ ] **Step 3: `PatchStatsRangeAligner` 를 만들고 `PatchStatsTask` 에 두 단계를 넣는다.**
- [ ] **Step 4: 통과를 확인한다.** `./gradlew :core:core-batch:test --tests '*PatchStatsRangeAlignTest' --tests '*PatchStatsTaskTest'` → 통과. `ktlintFormat`, `ktlintCheck`.
- [ ] **Step 5: 커밋한다.** `feat(backend): 패치 목록이 바뀌면 통계의 날짜 범위를 하루씩 맞춘다`

### Task 12: 잡 다섯 개와 봇 감시

**Files:**
- Create: `…batch/domain/statistics/StatsBotMonitor.kt`, `…batch/job/statistics/patch/PatchStatsJobConfig.kt`
- Modify: `…batch/domain/statistics/StatsBotBattleMonitor.kt`, `StatsBotLobbyMonitor.kt`(인터페이스를 구현한다고 적는다. 동작은 그대로다), `PatchStatsAggregation.kt`(감시를 만드는 자리), `PatchStatsAggregations.kt`, `PatchStatsTask.kt`
- Test: `…batch/job/statistics/patch/PatchStatsJobConfigTest.kt`, `PatchStatsBotMonitorTest.kt`

**Interfaces:**
- Consumes: `AlertManager`, `AlertCommand`, `StatsBotMonitorMetric`(지금 있는 것), 잡 이름 표.
- Produces:

```kotlin
interface StatsBotMonitor<TBattle> {
    fun record(battle: TBattle)
    fun metrics(): List<StatsBotMonitorMetric>
    fun summary(): String
}
// PatchStatsAggregation 에 마지막 인자를 더한다. 1인 승률은 ::StatsBotBattleMonitor, 1인 순위는 ::StatsBotLobbyMonitor, 나머지는 null.
val createBotMonitor: (() -> StatsBotMonitor<TBattle>)? = null
// PatchStatsTask 생성자에 alertManager: AlertManager 를 더한다.
@Configuration
class PatchStatsJobConfig   // 잡 이름 표의 잡 5개와 스텝 5개, 읽기 2개·PatchStatsAggregations·PatchStatsTask 빈
```

**봇 감시**(스펙 5.1): 감시를 만드는 자리가 있는 유형은 실행마다 감시 하나를 만들어 평소 읽기에서 읽은 전투를 `record` 한다. 범위 맞추기로 다시 읽은 전투는 넣지 않는다. 실행 끝에 한 번, 넣은 전투가 있으면 요약을 로그로 남기고 `isWarning` 인 지표가 있으면 경고 알림을 한 번 보낸다(제목 `[<잡 이름>] 봇 전투 규칙 이상 감지`, 내용은 지표의 `describe()` 를 줄마다). 이 구간의 예외는 로그만 남기고 잡을 실패시키지 않는다(옛 1인 잡과 같다).

- [ ] **Step 1: 실패하는 테스트를 쓴다.**
  - `PatchStatsJobConfigTest`(`CoreBatchContextTest`): "패치 통계 잡 다섯 개가 이름표의 이름으로 있다"(잡 빈 이름 5개), "파라미터 없이 시작할 수 있고 실행마다 새 인스턴스가 된다"(잡마다 `jobParametersValidator` 가 빈 `JobParameters()` 를 통과시키고 `jobParametersIncrementer` 가 `RunIdIncrementer` 다). 기존 `JobParametersIncrementerGuardTest` 는 고치지 않고 통과해야 한다.
  - `PatchStatsBotMonitorTest`(`CoreBatchContextTest`, `AlertManager` 는 `mockk(relaxUnitFun = true)`): 지표를 정해 주는 `StatsBotMonitor` 대역을 꽂은 `PatchStatsAggregation` 으로 돌린다. "평소 읽기에서 읽은 전투만 감시에 넣는다"(패치를 등록해 범위 맞추기가 도는 실행에서 `record` 횟수가 평소 읽기의 전투 수와 같다), "경고 지표가 있으면 실행당 한 번 알린다", "경고 지표가 없으면 알리지 않는다", "읽은 전투가 없으면 요약도 알림도 없다", "알림이 실패해도 잡은 실패하지 않는다"(`alert` 가 예외를 던져도 `execute` 가 끝나고 통계가 쓰였다).
- [ ] **Step 2: 실패를 확인한다.** `./gradlew :core:core-batch:test --tests '*PatchStatsJobConfigTest' --tests '*PatchStatsBotMonitorTest'` → 컴파일 실패.
- [ ] **Step 3: 인터페이스, 감시 연결, 설정 클래스를 만든다.** 잡은 `BrawlStarsNewsCollectJobConfig` 와 같은 모양이다: `RunIdIncrementer`, 람다 태스클릿, 스텝에 트랜잭션 매니저를 넘기지 않는다. 읽기 빈은 `BattleStatsConstants.READ_PAGE_SIZE`, 태스크 빈은 `PatchStatsConstants.CHECKPOINT_BATTLE_COUNT` 로 만든다. KDoc 에 "파라미터가 없어 `RunIdIncrementer` 가 없으면 두 번째 실행이 거부된다", "실패 알림은 `JobFailureAlertListener` 가 보낸다"를 적는다.
- [ ] **Step 4: 통과를 확인한다.** `./gradlew :core:core-batch:test` 전체 통과. `ktlintFormat`, `ktlintCheck`.
- [ ] **Step 5: 커밋한다.** `feat(backend): 패치 통계 집계 잡 다섯 개를 더하고 봇 감시를 잇는다`

### Task 13: 로컬 PostgreSQL 에서 확인 (컨트롤러, 자동 테스트 아님)

H2 테스트가 보지 못하는 것을 본다. 결과(본 것과 보지 못한 것)를 PR 본문에 그대로 적는다.

**Files:** 없음(저장소에 남기는 파일이 없다).

**Interfaces:**
- Consumes: Task 4 의 컨테이너(`patch-stats-pg`, 55432), 잡 이름 표.

- [ ] **Step 1: 원본과 패치를 넣는다.** 패치일 둘(어제 기준 10일 전, 3일 전)과, 그 범위 안팎의 결과형·순위형 원본을 몇 건 SQL 로 넣는다. `teams` 의 JSON 모양은 `StatsBattleEntityTeamPlayer` 의 필드 그대로다. `created_at` 은 한 시간 전으로 한다.
- [ ] **Step 2: 새 잡을 두 번 돌린다.** `./gradlew :core:core-batch:bootRun --args='--job.name=brawlerBattleResultPatchStatsJob --storage.datasource.core.jdbc-url=jdbc:postgresql://localhost:55432/imstargg --spring.batch.jdbc.initialize-schema=always'`(접속 계정은 로컬 프로필의 값을 쓴다. 맞지 않으면 같은 방식의 인자로 덮는다. 앱이 뜨는 데 다른 로컬 인프라가 필요하면 `imstargg-backend/` 에서 `docker-compose up -d` 로 띄운다). 두 번째 실행 전에 있던 키와 없던 키에 걸친 원본을 더 넣는다. 기대: 두 번 다 `COMPLETED`, 통계 값이 손으로 센 값과 같다. 이것으로 운영 설정(`reWriteBatchedInserts=true`)에서 배치 갱신이 줄별 결과를 주는지를 본다(주지 않으면 `BatchUpdateCounts` 가 실패시킨다). 다섯 잡을 모두 한 번씩 돌린다.
- [ ] **Step 3: 패치를 끼워 넣고 지운다.** 범위 안에 패치를 `insert` 하고 잡을 돌린 뒤, 그 패치를 지우고 다시 돌린다. 기대: 마지막 통계 값이 끼워 넣기 전과 정확히 같다(끝 날짜 `null` 을 넘기는 갱신이 PostgreSQL 에서 도는지도 여기서 본다).
- [ ] **Step 4: 옛 잡을 한 번 돌린다.** `--job.name=brawlerPairBattleResultStatsJob date=<어제>` 와 `--job.name=brawlerBattleResultStatsJob date=<어제>`. 기대: `COMPLETED`, `_backup` 테이블에 줄이 생긴다(시퀀스 이름이 맞다는 뜻이다). `--job.name=partitionManageJob date=<오늘>` 도 `COMPLETED` 다.
- [ ] **Step 5: 3인 조합 하루치 빼기의 시간을 어림한다.** 새 3인 조합 테이블에 `generate_series` 로 30만 줄을 넣고, 그 30만 줄을 한 트랜잭션에서 한 줄씩 조건부 `update` 하는 SQL 파일을 `\timing` 으로 돌린다. 수치는 참고용이다. 운영 수치는 전환 조건 3번에서 잰다.
- [ ] **Step 6: 정리한다.** `docker rm -f patch-stats-pg`. 하지 못한 단계가 있으면 "확인하지 못한 것"으로 적는다.

### Task 14: 문서, 용어 검사, 버전 (컨트롤러)

- [ ] **Step 1: 전략 문서.** `imstargg-backend/docs/stats-battle-data-strategy.md` 에 "패치 기준 통계(1단계)" 절을 더한다: 새 테이블과 체크포인트, 잡 다섯 개와 스케줄, 지키는 규칙 한 줄, 다시 채우는 방법(스펙 8장), 옛 집계와 함께 도는 기간이라는 것. 옛 집계의 설명은 2단계에서 지운다.
- [ ] **Step 2: 도메인 정의.** `/superdomain:domain` 으로 `docs/superdomain/DOMAIN.md` 의 statistics 절을 고친다: 통계 전투 보관 10일 → 45일(패치일 등록 기한보다 길어야 한다), 기능에 "패치 기준 누적 집계(전환 중: 옛 일별·주별 집계와 함께 돈다)". 클래스 이름은 넣지 않는다.
- [ ] **Step 3: 용어 검사.** `/superglossary:check` 를 이 브랜치가 `origin/develop` 뒤로 더한 코드에 돌린다. 위반이 나오면 고친다.
- [ ] **Step 4: 전체 확인.** `./gradlew test ktlintCheck --rerun-tasks` 전체 통과. 건수를 PR 본문에 적는다.
- [ ] **Step 5: 버전.** `/release bump`(변경 감지의 base 는 `origin/develop`). core-api 와 core-batch 가 대상이다. 이미 SNAPSHOT 이면 바뀌지 않는다.
- [ ] **Step 6: 결정 기록을 제안한다.** 사용자에게 `/superdomain:adr` 로 남길지 묻는다: "통계를 패치마다 한 줄에 누적하고 원본을 다시 읽어 범위를 맞춘다", "새 통계 테이블이 옛 이름을 쓰고 옛 것은 `_backup` 으로 옮긴다".

## 배포 (1단계, 이 PR 밖)

순서는 스펙 10장이다. 1번과 2번은 붙여서 한다. 그 사이에는 캐시에 없는 통계 요청이 500 이 되고 브롤러 목록·브롤러 상세·이벤트 상세 페이지가 오류로 보인다. 트래픽이 적고 02:00–09:00 KST 를 벗어난 때에 한다.

0. **먼저 확인**: 운영 core-batch 가 0.4.5 이상이다(원본 보관 45일). 첫 번째 스펙(패치 테이블)이 운영에 있다. 로컬 PostgreSQL 에서 마이그레이션을 두 번 적용해 봤다(Task 4).
1. 운영 DB 에 Task 4 의 SQL 을 적용한다(`psql -v ON_ERROR_STOP=1 -f`).
2. core-api 와 core-batch 를 배포한다. 옛 통계 화면과 옛 잡 5종, `partitionManageJob` 이 정상인지 본다.
3. 그 뒤에 `imstargg-deploy/apps/prod/core-batch/values.yaml` 에 CronJob 다섯 개를 더한다. 배포보다 먼저 더하면 옛 이미지가 모르는 잡 이름으로 뜬다.

```yaml
  - name: battle-result-patch-stats
    schedule: "0 */3 * * *" # 매 3시간 00분
    args:
      - "java -jar /app/application.jar --job.name=brawlerBattleResultPatchStatsJob"
  - name: battle-rank-patch-stats
    schedule: "10 */3 * * *"
    args:
      - "java -jar /app/application.jar --job.name=brawlerBattleRankPatchStatsJob"
  - name: enemy-battle-result-patch-stats
    schedule: "20 */3 * * *"
    args:
      - "java -jar /app/application.jar --job.name=brawlerEnemyBattleResultPatchStatsJob"
  - name: pair-battle-result-patch-stats
    schedule: "30 */3 * * *"
    args:
      - "java -jar /app/application.jar --job.name=brawlerPairBattleResultPatchStatsJob"
  - name: trio-battle-result-patch-stats
    schedule: "40 */3 * * *"
    args:
      - "java -jar /app/application.jar --job.name=brawlerTrioBattleResultPatchStatsJob"
```

**CronJob 을 더하기 전에 볼 것**(스펙 12장): 차트의 시간 제한(`activeDeadlineSeconds`)과 재시도(`backoffLimit`) 기본값. 첫 실행은 유형당 2시간을 넘길 수 있다(스펙 5.7). 시간 제한이 그보다 짧아도 10만 건마다 저장하므로 다음 실행이 잇지만, 실패 알림이 그만큼 온다. 재시도가 있으면 실패한 실행마다 알림이 곱해진다.

**가동 뒤에 재는 것**(스펙 12장, 1단계에서는 화면이 새 테이블을 읽지 않는다): 상대·2인·3인의 처리 속도, 다섯 유형이 함께 처음 채울 때의 DB 부하(크면 CronJob 을 멈췄다가 하나씩 켠다), 파티션이 45개로 늘었을 때 읽기 쿼리의 계획 시간, 원본 저장 트랜잭션의 길이(10분 여유의 근거).

**2단계로 넘어가는 조건**은 스펙 10장의 네 가지다(체크포인트 줄, 옛 통계와 비교, 시험 패치를 넣었다 빼기, 상대·조합의 눈 확인). 2단계 계획의 "실행 전 확인"이 다시 적는다.

**되돌리기**: 새 CronJob 을 빼고, 마이그레이션 파일 머리의 되돌리는 SQL 을 적용한 뒤, core-api 와 core-batch 를 이전 버전으로 올린다.
