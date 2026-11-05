# Stats Battle 데이터 수집 전략

이 문서는 Stats Battle 테이블들의 설계 의도, 파티셔닝 전략, 데이터 보관 정책을 설명합니다.

## 1. 개요

Stats Battle 시스템은 BrawlStars 게임의 배틀 데이터를 수집하고 통계 분석을 위한 데이터를 저장합니다. 대량의 배틀 데이터를 효율적으로 처리하기 위해 파티셔닝과 중복 방지 전략을 사용합니다.

### 핵심 목표
- **효율적인 데이터 수집**: 중복 없이 배틀 데이터 수집
- **빠른 통계 반영**: 멱등 배치를 통한 통계 집계
- **저장 공간 최적화**: 파티션 기반 데이터 보관/삭제

### 날짜 기준
- 원본 두 테이블(`stats_result_battle`, `stats_rank_battle`)은 **`battle_date`**(전투일, 한국 시간 기준) 일별 파티션, 조합 통계 세 테이블은 **`start_battle_date`**(주 버킷 시작일) 월별 파티션이다(섹션 4.1)
- `battleDate`는 `battleAt` (배틀 발생 시각, OffsetDateTime)을 한국 시간(Asia/Seoul)으로 변환한 날짜

## 2. 테이블 구조 및 역할

### 2.1 테이블 요약

| 테이블 | 역할 | 파티션 키 | 보관 기간 |
|--------|------|----------|----------|
| `stats_result_battle` | 배틀 결과 통계 데이터 (승/패/무) | `battle_date` | 10일 (`RETENTION_DAYS`) |
| `stats_rank_battle` | 배틀 랭크 통계 데이터 (순위) | `battle_date` | 10일 (`RETENTION_DAYS`) |

조합 통계 집계 테이블 3종(`brawler_pair_battle_result_stats` 등)의 파티션·보관은 섹션 5.4, 6 참조.

### 2.2 Stats Battle 테이블

#### `stats_result_battle`
- **목적**: 승/패/무 결과가 있는 배틀 데이터 저장
- **파티션 키**: `battle_date` (battleAt의 한국시간 날짜)
- **중복 방지**: Unique Index `(battle_date, battle_key)` (같은 배틀은 한 번만 저장)
- **사용처**: 브롤러별 승률, 맵별 승률 등 결과 기반 통계

#### `stats_rank_battle`
- **목적**: 랭크(순위) 결과가 있는 배틀 데이터 저장
- **파티션 키**: `battle_date` (battleAt의 한국시간 날짜)
- **중복 방지**: Unique Index `(battle_date, battle_key)` (같은 배틀 = 같은 팀 관점 = 1 row). 자세한 배틀 키 생성 규칙은 섹션 9 참조.
- **사용처**: 솔로 쇼다운 등 순위 기반 통계

## 3. 데이터 흐름

```
BrawlStars battlelog API 응답
    │
    ▼
BattleBuilder → Battle (core-worker 도메인 모델)
    │
    ▼
StatsBattleAggregateApplier (core-worker)
    │
    ├── 조건 1: 최근 7일 이내 전투만 (StatsBattleAggregateApplier.BATTLE_MAX_AGE_DAYS)
    ├── 조건 2~8: 수집 대상 조건 (아래 표)
    ├── 조건 9: Unique Index 기반 중복 방지
    │
    ├─────────────────────────────────────────────────┐
    │                                                 │
    ▼                                                 ▼
StatsResultBattleEntity                    StatsRankBattleEntity
(배틀 단위 통계)                             (플레이어별 랭크 통계)
```

### 수집 프로세스

원천은 DB 의 전투 테이블이 아니라 battlelog API 응답이다. 갱신 경로(`StatsBattleAggregator.asyncAggregate`)와 축적 스케줄러(`StatsBattleAggregator.aggregate`)가 `Battle` 목록을 `StatsBattleAggregateApplier.apply` 에 넘긴다.

1. **수집 대상 판정**: 전투마다 아래 조건을 순서대로 검사하고, 하나라도 어기면 그 전투를 버린다.

   | 순서 | 조건 | 비고 |
   |------|------|------|
   | 1 | 7일 이내 | `StatsBattleAggregateApplier.BATTLE_MAX_AGE_DAYS` |
   | 2 | 이벤트·맵·mode·type 이 있어야 한다 | 이벤트 ID, 모드 ID, 맵이 모두 있어야 한다 |
   | 3 | 친선전 제외 | `BattleType.FRIENDLY` |
   | 4 | 브롤러 중복 없음 | 전투 전체에서 같은 브롤러가 두 번 나오면 버린다 |
   | 5 | 최저 트로피 | 일반 전투는 `StatsTierRange.MIN_TROPHY`(1000) 이상, 경쟁전(`soloRanked`)은 `MIN_SOLO_RANK_TIER`(신화 1) 이상 |
   | 6 | `StatsEventMode` 허용 목록 | `modeBrawlStarsId` 로만 판정한다. 목록 밖 모드(와이프아웃 25, 태그팀/듀얼 24, 5v5 등)는 버린다 |
   | 7 | 팀 구성 일치 | 팀 수가 `teamCount`, 모든 팀의 인원이 `teamSize` 와 같아야 한다 |
   | 8 | 유형과 경로 일치 | 순위형 테이블은 `battleRank` 가 있고 `resultType == RANK` 인 모드, 결과형 테이블은 `result` 가 있고 `resultType == RESULT` 인 모드만 받는다 |

   - 6번은 `battle.mode` 문자열을 쓰지 않는다. battlelog 가 하키(45)를 `brawlBall`, 아레나(48)를 `siege` 로 보내는 등 문자열을 믿을 수 없기 때문이다.
   - 듀얼은 `BattleBuilder` 가 같은 태그로 브롤러 3개씩 2×3 으로 펼치므로 7번은 통과한다. 6번에서 걸려 버려진다.
   - 허용 모드와 모드별 팀 구성은 `StatsEventMode`(core-enum)가 단일 출처다.
   - core-batch `statsPlayerSyncJob`(통계 전투에서 수집 대상 플레이어 발굴)은 모드를 거르지 않고 통계 전투 테이블을 그대로 읽는다. 그래서 허용 목록 적용 뒤 수집된 전투로는 허용 목록 모드에서 본 플레이어만 발굴한다(미지원 모드에서만 보인 플레이어는 이 경로로 들어오지 않는다). 적용 전에 쌓인 행은 파티션 보존 기간 동안 그대로 발굴에 쓰인다.
2. **중복 체크**: Stats 테이블에서 이미 처리된 `battleKey` 인지 확인한다.
3. **Stats Battle 저장**: 중복이 아닌 경우 Stats 테이블에 저장한다 (`battleDate`는 `battleAt`에서 자동 계산).
4. **통계 반영**: 배치에서 Stats 데이터를 집계하여 통계 반영.

## 4. 파티셔닝 전략

### 4.1 파티션 키

- 원본 배틀 테이블(`stats_result_battle`, `stats_rank_battle`)은 `battle_date` (전투일, 한국 시간 기준)를 파티션 키로 사용합니다.
- 조합 통계 집계 테이블 3종(`brawler_pair_battle_result_stats`, `brawler_trio_battle_result_stats`, `brawler_enemy_battle_result_stats`)은 `start_battle_date` (주 버킷 시작일) 월 파티션입니다. 섹션 5.4 참조.

### 4.2 Stats Battle 테이블 파티셔닝

```sql
-- 파티션 구조: battle_date 기준 일 범위 파티션 (PostgreSQL)
create table stats_result_battle ( ... ) partition by range (battle_date);
create table stats_result_battle_default partition of stats_result_battle default;
-- 일별 파티션 {table}_p_YYYYMMDD 는 create_partitions() 가 default 에서 떼어 내 만든다
```

- **파티션 키**: `battle_date` (배틀 일자, 한국 시간 기준)
- **보관 기간**: 10일 (`PartitionManageJobConfig.RETENTION_DAYS`)
- **삭제 시점**: `partitionManageJob` (매일 02:00 KST) 이 `date - RETENTION_DAYS` 보다 이른 일 파티션을 DETACH 후 DROP (`drop_old_partitions`)
- **10일인 이유**: 주간 통계 잡의 최대 룩백(`StatsWeekRange.currentAndPrevious` 기준 최대 7일 + 실행일 보정 1일)을 원본이 커버해야 한다. 가드: `PartitionRetentionLookbackTest`

## 5. 통계 집계 배치 전략

### 5.1 멱등 배치

통계 집계 배치는 **멱등(idempotent)** 하게 설계합니다.
- 동일한 날짜에 대해 배치를 재실행해도 안전(원본이 남아 있는 동안). 단 조합·카운터·3인 잡은 버킷을 통째로 덮어쓰므로, 원본 10일 보존보다 오래된 버킷이나 이관된 버킷의 `date` 로 재실행하면 남은 원본만으로 덮어써 부분 데이터가 된다(섹션 10)
- 기존 통계를 삭제 후 재집계하는 방식

### 5.2 재집계 범위

잡 종류에 따라 재집계 범위가 다릅니다. 모두 `date` 파라미터(KST 전일)를 기준으로 합니다.

| 잡 | 실행 시각 (KST) | 재집계 범위 |
|----|----------------|------------|
| `brawlerBattleRankStatsJob`, `brawlerBattleResultStatsJob` (1인 브롤러 통계) | 02:00, 03:00 | `date` 하루 |
| `brawlerEnemyBattleResultStatsJob`, `brawlerTrioBattleResultStatsJob`, `brawlerPairBattleResultStatsJob` (카운터·3인·2인 조합) | 04:00, 05:00, 07:00 | `StatsWeekRange.currentAndPrevious(date)` 주 버킷 |

- **1인 브롤러 통계**: `date` 하루만 집계합니다. 이미 집계한 날짜에 늦게 들어온 전투는 다시 반영되지 않는 한계가 있습니다(조합 통계 재구성 스펙의 비목표).
- **조합·카운터·3인**: 현재 주 버킷을 통째로 다시 집계하고, `date` 전날이 다른 주에 속하면(월요일, 월 첫날) 직전 주 버킷도 다시 집계합니다. 최대 두 버킷입니다. 주 버킷은 월 경계에서 잘립니다(`StatsWeekRange`).

**이유**: 전투 데이터는 계속 수집되며, 이전 날짜에 대해서도 새로운 데이터가 축적됩니다. 주 버킷 단위로 다시 집계해 늦게 수집된 데이터를 같은 주의 통계에 반영합니다. 이 룩백이 원본 배틀 보관 기간(10일, 섹션 4.2)을 넘지 않아야 합니다.

### 5.3 배치 실행 흐름

```
배치 실행 (date 파라미터: 집계 대상 battleDate, KST 전일)
    │
    ├── 1인 브롤러 통계 (rank 02:00 / result 03:00)
    │     └── battleDate = date 하루 집계 → 그 날짜의 기존 통계 삭제 후 재삽입
    │
    └── 조합·카운터·3인 (enemy 04:00 / trio 05:00 / pair 07:00)
          └── StatsWeekRange.currentAndPrevious(date) 의 주 버킷마다
                ├── 리더: battle_date BETWEEN 주 범위, id keyset 으로 5,000건씩 스트리밍
                ├── 집계기: 버킷당 하나 (조합 키별 승·패·무 합산, 이벤트 키 없음)
                └── 라이터: (start_battle_date, end_battle_date) 삭제 → 1,000건 청크로 삽입
                      (버킷당 REQUIRES_NEW 트랜잭션 하나)
```

- `date` 파라미터로 집계 대상 날짜 지정
- Stats Battle 테이블에서 해당 `battleDate` 범위의 데이터를 조회하여 통계 집계
- 멱등 처리: 해당 날짜(1인) 또는 해당 버킷(조합·카운터·3인)의 기존 통계 삭제 후 재집계

### 5.4 조합 통계 (2인·3인·카운터)

대상 테이블은 `brawler_pair_battle_result_stats`(같은 팀 두 브롤러), `brawler_trio_battle_result_stats`(같은 팀 세 브롤러), `brawler_enemy_battle_result_stats`(내 브롤러 대 상대 브롤러)입니다. 쇼다운 조합 순위(`brawler_pair_battle_rank_stats`)는 지웠습니다.

- **이벤트 키 없음**: 세 테이블 어디에도 이벤트(맵) 키가 없습니다. 맵에 따른 조합 승률 차이는 거의 다 개인 브롤러의 맵 효과이고 이벤트별 칸은 표본이 얇아 예측에 보탬이 없어 뺐습니다. 근거와 결정 이력은 [조합 통계 재구성 설계](../../docs/superpowers/specs/2026-11-04-combo-stats-redesign-design.md) 결정 1·부록 B.1.
- **키 (2인·3인)**: 조합당 1행이고 브롤러 id 오름차순으로 정렬해 저장합니다(`brawler1 < brawler2 [< brawler3]`, CHECK 제약). 방향 중복이 없습니다. 유니크 `(tier_range, start_battle_date, brawler1, brawler2[, brawler3])`, 브롤러 위치별 조회 인덱스 `(tier_range, brawlerN, start_battle_date)`.
- **키 (카운터)**: 방향성 행 `(brawler, enemy)`입니다. (X,Y)와 (Y,X) 두 행이 승↔패로 보완됩니다. 유니크 `(tier_range, brawler, start_battle_date, enemy)`. `end_battle_date` 는 어느 인덱스에도 넣지 않습니다.
- **티어**: 2인·3인은 조합 멤버 전원이 그 티어 이상이어야 하므로 멤버 중 가장 낮은 trophies 로 `StatsTierRange.resolveAll(battleType, trophies)` 를 호출합니다. 카운터는 기준 플레이어의 trophies 입니다.
- **한 행의 뜻**: "(티어, 주 버킷)에서 이 조합이 성립한 팀의 승·패·무"입니다. 한 버킷은 한 월 파티션에 들어갑니다(섹션 6).
- **API 분모**: 조합 픽률의 분모는 조합 테이블 자체의 `total_count` 합에서 구하며 단일 브롤러 통계 테이블에 의존하지 않습니다.

## 6. 데이터 보관 정책

| 테이블 유형 | 파티션 | 보관 기간 | 삭제 조건 |
|-------------|--------|----------|----------|
| Stats Battle (`stats_result_battle`, `stats_rank_battle`) | 일 파티션 `{table}_p_YYYYMMDD` (`battle_date`) | 10일 (`RETENTION_DAYS`) | `date - 10일` 보다 이른 파티션 DROP (`drop_old_partitions`) |
| 조합 통계 (`brawler_pair_battle_result_stats`, `brawler_trio_battle_result_stats`, `brawler_enemy_battle_result_stats`) | 월 파티션 `{table}_p_YYYYMM` (`start_battle_date`), default 파티션 없음 | 지난달 1일부터 | 월 파티션 `{table}_p_YYYYMM`, 지난달 1일 이전 월 DROP (`drop_old_month_partitions`) |

- 원본 배틀은 주 버킷 재집계(섹션 5.2)로 통계에 반영하며, 반영 후에는 필요 없다. 최대 룩백을 덮고 여유를 두어 10일 보관한다.
- 조합 통계의 삭제 경계는 API 가 읽는 가장 오래된 날짜(`StatsDateRange.SINCE_LAST_MONTH`, 지난달 1일)와 같다. `date` 가 속한 달의 1일에서 한 달을 뺀 날짜가 속한 달보다 이른 파티션이 지워진다. 예) 2026-11-01 실행이면 2026-09 까지 삭제하고 2026-10 은 남는다. 월 단위 DROP 은 블로트를 남기지 않는다.
- 매일 다시 쓰는 버킷의 삭제 흔적은 그 달 파티션 안에서 autovacuum 이 정리한다.

## 7. 파티션 관리 가이드

파티션 생성·삭제는 PostgreSQL 함수(`infrastructure/db-core/src/main/resources/partition-manage.sql`)가 맡고, `partitionManageJob` (02:00 KST, `date` = KST 오늘)이 매일 호출한다.

### 7.1 파티션 추가 (일별)

```sql
-- stats_result_battle, stats_rank_battle: 2025-01-24 부터 7일 뒤까지 일별 파티션 생성
-- (DAYS_AHEAD = 7. 이미 있는 파티션은 건너뛴다)
SELECT create_partitions('2025-01-24', 7);
```

### 7.2 파티션 삭제 (보관 기간 초과)

```sql
-- Stats Battle: 2025-01-24 기준 10일(RETENTION_DAYS) 이전, 즉 2025-01-14 보다 이른 파티션 삭제
SELECT drop_old_partitions('2025-01-24', 10);
```

### 7.3 월 파티션 (조합 통계)

```sql
-- 조합 통계 3종: 2026-11 부터 1달 뒤(2026-12)까지 월 파티션 생성 (MONTHS_AHEAD = 1)
SELECT create_month_partitions('2026-11-05', 1);

-- 2026-11-05 기준 지난달 1일(2026-10-01) 보다 이른 월 파티션(2026-09 이하) 삭제
SELECT drop_old_month_partitions('2026-11-05');
```

- `partitionManageJob` 은 일별 스텝 둘(`create_partitions`, `drop_old_partitions`) 뒤에 월 스텝 둘(`create_month_partitions`, `drop_old_month_partitions`)을 실행한다.
- 월 파티션 이름은 `{table}_p_YYYYMM`(6자리), 일 파티션은 `_p_YYYYMMDD`(8자리)라 두 삭제 함수의 정규식이 겹치지 않는다.
- default 파티션이 없으므로 파티션이 없는 달의 행은 삽입에 실패한다. 다음 달 파티션을 `MONTHS_AHEAD` 로 미리 만들어 두는 이유다.
- **`partition-manage.sql` 은 앱이 적용하지 않는다.** 함수를 바꾸는 배포는 새 core-batch 이미지가 돌기 전에 운영자가 DB 에 직접 적용해야 한다(파일은 `DROP FUNCTION IF EXISTS` + `CREATE OR REPLACE` 라 여러 번 적용해도 같은 결과). 적용하지 않으면 다음 날 02:00 `partitionManageJob` 의 월 스텝이 `function does not exist` 로 실패한다.

### 7.4 운영 권장 사항

1. **자동화**: 파티션 추가/삭제는 `partitionManageJob` 으로 자동화
2. **미리 생성**: 일 파티션은 7일 앞(`DAYS_AHEAD`), 월 파티션은 다음 달(`MONTHS_AHEAD`)까지 미리 생성
3. **모니터링**: 파티션 크기 및 개수 모니터링
4. **백업**: 파티션 삭제 전 필요시 백업 수행

## 8. 관련 코드

### 엔티티

- `StatsResultBattleEntity`: 결과 기반 통계 배틀 엔티티
- `StatsRankBattleEntity`: 랭크 기반 통계 배틀 엔티티
- `BrawlerPairBattleResultStatsEntity`: 2인 조합 통계 (`brawler1BrawlStarsId < brawler2BrawlStarsId`)
- `BrawlerTrioBattleResultStatsEntity`: 3인 조합 통계 (`brawler1 < brawler2 < brawler3`)
- `BrawlerEnemyBattleResultStatsEntity`: 카운터 통계 (방향성 `(brawler, enemy)`)

### 설정

- `StatsBattleAggregateApplier.BATTLE_MAX_AGE_DAYS = 7`: 수집 대상 배틀의 최대 경과 일수
- `StatsEventMode`: 통계를 수집하는 모드의 허용 목록과 모드별 팀 구성
- `PartitionManageJobConfig`: `DAYS_AHEAD = 7`, `RETENTION_DAYS = 10`, `MONTHS_AHEAD = 1`. 일 스텝(`partitionCreateStep`, `partitionDropStep`) 뒤에 월 스텝(`monthPartitionCreateStep`, `monthPartitionDropStep`)이 `create_month_partitions`, `drop_old_month_partitions` 를 호출한다. 삭제 경계 `monthRetentionCutoff` 는 SQL 과 같은 경계다
- `partition-manage.sql`: 일·월 파티션 함수. 앱이 적용하지 않는다(섹션 7.3)
- `StatsWeekRange.currentAndPrevious`: 주간 통계 잡의 재집계 범위. `StatsTierRange.resolveAll`: 조합 통계의 티어 판정
- `BattleStatsConstants`: 리더 페이지 크기 `READ_PAGE_SIZE = 5000`, 라이터 청크 `BATCH_SIZE = 1000`

### 잡 (core-batch)

- `brawlerBattleRankStatsJob` (02:00 KST), `brawlerBattleResultStatsJob` (03:00): 1인 브롤러 통계, `date` 하루
- `brawlerEnemyBattleResultStatsJob` (04:00), `brawlerTrioBattleResultStatsJob` (05:00, CronJob `trio-battle-result-stats`), `brawlerPairBattleResultStatsJob` (07:00): 조합·카운터·3인, 주 버킷 재집계
- `partitionManageJob` (02:00): 파티션 생성·삭제
- `brawlerPairBattleRankStatsJob` 과 CronJob `pair-battle-rank-stats` 는 쇼다운 조합 순위 기능과 함께 지웠다

## 9. BattleKey 정의

### 생성 규칙
- `SHA-256(battleAt.toEpochSecond() || sorted(player tags).join(""))` 의 hex.
- **Result 전투**: 전체 플레이어 태그(모든 팀) 로 key 생성. 같은 배틀이면 어느 수집 주체가 들어와도 같은 key → DB unique `(battle_date, battle_key)` 로 1 row 로 수렴.
- **Rank 전투**: 수집 주체 플레이어의 **자기 팀 태그만** 으로 key 생성. 같은 팀의 여러 플레이어가 수집 대상이어도 모두 같은 key → 팀 단위로 1 row 로 수렴.

### 구현 위치
- `infrastructure/db-core` 의 `BattleKeyEmbedded`
  - `BattleKeyEmbedded.create(battleTime, playerTags)` — 호출 측이 집계 의도에 맞는 태그 범위를 넘긴다. 결과형은 전체 플레이어 태그, 순위형은 자기 팀 플레이어 태그.
- 호출부: `core-worker` 의 `BattleBuilder.createBattleKey()` (`battle.rank != null` 여부로 분기).

### Rank 전투 row 정책
- 같은 팀 관점의 중복 수집은 merge 된다 (팀 단위 1 row).
- **서로 다른 팀의 관측은 서로 다른 key** 를 가지므로 별도 row 로 저장될 수 있다. 추적 대상 분포상 흔치 않아 용인한다.
- `playerBrawlStarsTag` 컬럼은 "그 팀의 대표 1명"(먼저 적재되어 unique 에 살아남은 주체) 을 의미한다.

### 집계 시 함의
- `BrawlerBattleRankStatsAggregator` 는 row 당 `playerTeams()` 를 그대로 순회해도 1 battle → 1 회 집계가 된다 (별도 dedup 장치 불필요). 쇼다운 조합 순위 집계기(앞서 이 자리에 적혀 있던 `BrawlerTeamBattleRankStatsAggregator` 의 후속 `BrawlerPairBattleRankStatsAggregator`)는 기능과 함께 지웠다.

### Unique index 변경 이력 (2026-04-23)
- `stats_rank_battle`: `uk_...__battle_date_battle_key_player` (3 컬럼) → `uk_...__battle_date_battle_key` (2 컬럼). 마이그레이션 `V260423a__stats_rank_battle_unique_key.sql`.
- 5 개 집계 테이블: 자연키 unique 인덱스 신설. 마이그레이션 `V260423b__stats_natural_key_unique.sql`.

## 10. Job 재실행 Runbook

### 정기 스케줄
- `date=YYYY-MM-DD` non-option argument 1 개만 전달.
- 예: `java -jar core-batch.jar --job.name=brawlerBattleRankStatsJob date=2026-04-23`.

### 재실행이 필요한 경우
Spring Batch 는 `(jobName, identifying parameters)` 조합이 같으면 동일 JobInstance 로 보고 완료 상태이면 `JobInstanceAlreadyCompleteException` 을 던진다. 운영자가 수동으로 다시 돌려야 할 때는 **identifying non-option argument 를 하나 더 추가** 해 새 JobInstance 를 만든다.

예:

```bash
java -jar core-batch.jar \
    --job.name=brawlerBattleRankStatsJob \
    date=2026-04-23 \
    rerun=1
```

- Job 의 `DefaultJobParametersValidator` 는 `date` 만 required 로 강제하고 optional 리스트는 비워둔다. Spring Batch 6 의 `DefaultJobParametersValidator.validate()` 는 optional 리스트가 비어있으면 required 외 키에 대한 검사를 아예 건너뛰므로, 운영자는 임의 이름의 재실행 구분 키를 자유롭게 붙일 수 있다 (`rerun=1`, `manualRun=1` 등). `date` 만 누락되지 않으면 검증을 통과한다.
- 반드시 **non-option argument** (`key=value`) 형식이어야 한다. `-Drerun=...` 같은 JVM 시스템 프로퍼티는 Spring 환경에만 반영되고 `JobParameters` 에는 들어가지 않으므로 JobInstance 구분에 영향을 주지 못한다.
- **덮어쓰기 주의**: 조합·카운터·3인 잡은 `currentAndPrevious(date)` 버킷을 통째로 지우고 다시 쓴다. 원본 10일 보존보다 오래된 버킷이나 이관된 버킷(지난달 1일 ~ 배포일 − 7일, `V261104a` 머리말)의 `date` 로 재실행하면 남은 원본만으로 덮어써 부분 데이터가 된다.
- `run.id` 같은 Spring Batch 예약 키는 피한다. `RunIdIncrementer` 가 쓰는 키라 혼동 소지가 있음. 본 프로젝트는 `RunIdIncrementer` 를 도입하지 않았다 — `JobLauncherApplicationRunner` 는 CLI 파라미터로 JobInstance 를 먼저 조회하고, 완료 상태면 incrementer 를 호출하지 않기 때문에 정기 스케줄 케이스에서 효과가 없다 (Spring Boot issue #22602).
