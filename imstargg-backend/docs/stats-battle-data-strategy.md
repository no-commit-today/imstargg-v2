# Stats Battle 데이터 수집 전략

이 문서는 Stats Battle 테이블들의 설계 의도, 파티셔닝 전략, 데이터 보관 정책을 설명합니다.

## 1. 개요

Stats Battle 시스템은 BrawlStars 게임의 배틀 데이터를 수집하고 통계 분석을 위한 데이터를 저장합니다. 대량의 배틀 데이터를 효율적으로 처리하기 위해 파티셔닝과 중복 방지 전략을 사용합니다.

### 핵심 목표
- **효율적인 데이터 수집**: 중복 없이 배틀 데이터 수집
- **빠른 통계 반영**: 멱등 배치를 통한 통계 집계
- **저장 공간 최적화**: 파티션 기반 데이터 보관/삭제

### 날짜 기준
- 모든 테이블은 **battleDate** (전투일, 한국 시간 기준) 를 기준으로 파티셔닝 및 저장
- `battleDate`는 `battleAt` (배틀 발생 시각, OffsetDateTime)을 한국 시간(Asia/Seoul)으로 변환한 날짜

## 2. 테이블 구조 및 역할

### 2.1 테이블 요약

| 테이블 | 역할 | 파티션 키 | 보관 기간 |
|--------|------|----------|----------|
| `stats_result_battle` | 배틀 결과 통계 데이터 (승/패/무) | `battle_date` | 7일 |
| `stats_rank_battle` | 배틀 랭크 통계 데이터 (순위) | `battle_date` | 7일 |

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
BattleEntity (원본 배틀 데이터)
    │
    ├── 조건 1: 최근 7일 이내 전투만 (StatsBattleHelper.BATTLE_MAX_AGE_DAYS)
    ├── 조건 2: Unique Index 기반 중복 방지
    │
    ├─────────────────────────────────────────────────┐
    │                                                 │
    ▼                                                 ▼
StatsResultBattleEntity                    StatsRankBattleEntity
(배틀 단위 통계)                             (플레이어별 랭크 통계)
```

### 수집 프로세스

1. **배틀 데이터 조회**: `BattleEntity`에서 최근 7일 이내 배틀 조회
2. **중복 체크**: Stats 테이블에서 이미 처리된 키인지 확인
3. **Stats Battle 저장**: 중복이 아닌 경우 Stats 테이블에 저장 (`battleDate`는 `battleAt`에서 자동 계산)
4. **통계 반영**: 배치에서 Stats 데이터를 집계하여 통계 반영

## 4. 파티셔닝 전략

### 4.1 파티션 키

모든 테이블이 `battle_date` (전투일, 한국 시간 기준)를 파티션 키로 사용합니다.

### 4.2 Stats Battle 테이블 파티셔닝

```sql
-- 파티션 구조: battle_date 기준
partition by range (to_days(battle_date)) (
    partition p_default values less than maxvalue
)
```

- **파티션 키**: `battle_date` (배틀 일자, 한국 시간 기준)
- **보관 기간**: 7일
- **삭제 시점**: 통계 반영 완료 후 7일 경과 시 파티션 삭제

## 5. 통계 집계 배치 전략

### 5.1 멱등 배치

통계 집계 배치는 **멱등(idempotent)** 하게 설계합니다.
- 동일한 날짜에 대해 배치를 재실행해도 안전
- 기존 통계를 삭제 후 재집계하는 방식

### 5.2 최근 3일 롤링 배치

배치는 **최근 3일**에 대해 실행합니다.

**이유**: 전투 데이터는 계속 수집되며, 이전 날짜에 대해서도 새로운 데이터가 축적됩니다. 컴퓨팅 자원의 제약으로 최근 3일에 대해서만 배치를 실행하여 늦게 수집된 데이터를 통계에 반영합니다.

### 5.3 배치 실행 흐름

```
배치 실행 (date 파라미터: 집계 대상 battleDate)
    │
    ├── D-0 (오늘): Stats Battle에서 battleDate = D-0 데이터 집계
    ├── D-1 (어제): Stats Battle에서 battleDate = D-1 데이터 집계
    └── D-2 (그저께): Stats Battle에서 battleDate = D-2 데이터 집계
```

- `date` 파라미터로 집계 대상 날짜 지정
- Stats Battle 테이블에서 해당 `battleDate`의 데이터를 조회하여 통계 집계
- 멱등 처리: 해당 날짜의 기존 통계 삭제 후 재집계

## 6. 데이터 보관 정책

| 테이블 유형 | 보관 기간 | 삭제 조건 |
|-------------|----------|----------|
| Stats Battle | 7일 | 통계 반영 완료 + 7일 경과 |

- 수집된 데이터는 최근 3일 롤링 배치로 통계 반영
- 통계 반영 후에는 원본 데이터 불필요
- 여유 기간 포함하여 7일 보관

## 7. 파티션 관리 가이드

### 7.1 파티션 추가 (일별)

```sql
-- stats_result_battle
ALTER TABLE stats_result_battle ADD PARTITION (
    PARTITION p_20250201 VALUES LESS THAN (to_days('2025-02-02'))
);

-- stats_rank_battle
ALTER TABLE stats_rank_battle ADD PARTITION (
    PARTITION p_20250201 VALUES LESS THAN (to_days('2025-02-02'))
);
```

### 7.2 파티션 삭제 (보관 기간 초과)

```sql
-- Stats Battle: 7일 초과 파티션 삭제
ALTER TABLE stats_result_battle DROP PARTITION p_20250101;
ALTER TABLE stats_rank_battle DROP PARTITION p_20250101;
```

### 7.3 운영 권장 사항

1. **자동화**: 파티션 추가/삭제는 배치 또는 스케줄러로 자동화
2. **미리 생성**: 파티션은 최소 7일 전에 미리 생성
3. **모니터링**: 파티션 크기 및 개수 모니터링
4. **백업**: 파티션 삭제 전 필요시 백업 수행

## 8. 관련 코드

### 엔티티

- `StatsResultBattleEntity`: 결과 기반 통계 배틀 엔티티
- `StatsRankBattleEntity`: 랭크 기반 통계 배틀 엔티티

### 설정

- `StatsBattleHelper.BATTLE_MAX_AGE_DAYS = 7`: 수집 대상 배틀의 최대 경과 일수

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
- `BrawlerBattleRankStatsAggregator`, `BrawlerTeamBattleRankStatsAggregator` 는 row 당 `playerTeams()` 를 그대로 순회해도 1 battle → 1 회 집계가 된다 (별도 dedup 장치 불필요).

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
- `run.id` 같은 Spring Batch 예약 키는 피한다. `RunIdIncrementer` 가 쓰는 키라 혼동 소지가 있음. 본 프로젝트는 `RunIdIncrementer` 를 도입하지 않았다 — `JobLauncherApplicationRunner` 는 CLI 파라미터로 JobInstance 를 먼저 조회하고, 완료 상태면 incrementer 를 호출하지 않기 때문에 정기 스케줄 케이스에서 효과가 없다 (Spring Boot issue #22602).
