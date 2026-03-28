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
| `aggregated_result_battle_key` | ResultBattle 중복 수집 방지 | `battle_date` | 30일 |
| `aggregated_rank_battle_key` | RankBattle 중복 수집 방지 | `battle_date` | 30일 |

### 2.2 Stats Battle 테이블

#### `stats_result_battle`
- **목적**: 승/패/무 결과가 있는 배틀 데이터 저장
- **파티션 키**: `battle_date` (battleAt의 한국시간 날짜)
- **중복 기준**: `battleKey` 단독 (같은 배틀은 한 번만 저장)
- **사용처**: 브롤러별 승률, 맵별 승률 등 결과 기반 통계

#### `stats_rank_battle`
- **목적**: 랭크(순위) 결과가 있는 배틀 데이터 저장
- **파티션 키**: `battle_date` (battleAt의 한국시간 날짜)
- **중복 기준**: `playerTag + battleKey` (같은 배틀의 같은 플레이어는 한 번만)
- **사용처**: 솔로 쇼다운 등 순위 기반 통계

### 2.3 Aggregated Key 테이블

#### `aggregated_result_battle_key`
- **목적**: `stats_result_battle` 중복 수집 방지
- **Unique 키**: `(battle_date, battle_key)`
- **사용 이유**: 같은 배틀이 여러 플레이어 갱신 시 중복 수집되지 않도록 함

#### `aggregated_rank_battle_key`
- **목적**: `stats_rank_battle` 중복 수집 방지
- **Unique 키**: `(battle_date, player_brawlstars_tag, battle_key)`
- **사용 이유**: 랭크 통계는 플레이어별로 저장되므로 플레이어+배틀 키로 중복 체크

## 3. 데이터 흐름

```
BattleEntity (원본 배틀 데이터)
    │
    ├── 조건 1: 최근 30일 이내 전투만 (StatsBattleHelper.BATTLE_MAX_AGE_DAYS)
    ├── 조건 2: Aggregated 테이블에 없는 키만 (중복 방지)
    │
    ├─────────────────────────────────────────────────┐
    │                                                 │
    ▼                                                 ▼
StatsResultBattleEntity                    StatsRankBattleEntity
(배틀 단위 통계)                             (플레이어별 랭크 통계)
    │                                                 │
    ▼                                                 ▼
AggregatedResultBattleKeyEntity            AggregatedRankBattleKeyEntity
(battleKey 중복 방지)                        (playerTag + battleKey 중복 방지)
```

### 수집 프로세스

1. **배틀 데이터 조회**: `BattleEntity`에서 최근 30일 이내 배틀 조회
2. **중복 체크**: Aggregated Key 테이블에서 이미 처리된 키인지 확인
3. **Stats Battle 저장**: 중복이 아닌 경우 Stats 테이블에 저장 (`battleDate`는 `battleAt`에서 자동 계산)
4. **Aggregated Key 저장**: 처리 완료된 키를 Aggregated 테이블에 저장
5. **통계 반영**: 배치에서 Stats 데이터를 집계하여 통계 반영

## 4. 파티셔닝 전략

### 4.1 파티션 키

모든 테이블이 `battle_date` (전투일, 한국 시간 기준)를 파티션 키로 사용합니다.

| 테이블 | 파티션 키 | 이유 |
|--------|----------|------|
| `stats_*_battle` | `battle_date` | 배틀 일자 기준으로 파티션 관리 (통계 집계 시 battleDate 기준 조회) |
| `aggregated_*_battle_key` | `battle_date` | 배틀 일자 기준으로 중복 체크 (30일 이내 배틀만 수집) |

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

### 4.3 Aggregated Key 테이블 파티셔닝

```sql
-- 파티션 구조: battle_date 기준
partition by range (to_days(battle_date)) (
    partition p_default values less than maxvalue
)
```

- **파티션 키**: `battle_date` (배틀 일자)
- **보관 기간**: 30일
- **삭제 시점**: 30일 초과 파티션 삭제 (더 이상 중복 체크 불필요)

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

### 6.1 보관 기간 요약

| 테이블 유형 | 보관 기간 | 삭제 조건 |
|-------------|----------|----------|
| Stats Battle | 7일 | 통계 반영 완료 + 7일 경과 |
| Aggregated Key | 30일 | 배틀 일자 기준 30일 초과 |

### 6.2 보관 기간 설정 근거

#### Stats Battle (7일)
- 수집된 데이터는 최근 3일 롤링 배치로 통계 반영
- 통계 반영 후에는 원본 데이터 불필요
- 여유 기간 포함하여 7일 보관

#### Aggregated Key (30일)
- `StatsBattleHelper.BATTLE_MAX_AGE_DAYS = 30`과 일치
- 30일 이내 배틀만 수집하므로 중복 체크도 30일 필요
- 30일 초과 배틀은 수집 대상이 아니므로 키 삭제 가능

## 7. 파티션 관리 가이드

### 7.1 파티션 추가 (일별)

```sql
-- aggregated_result_battle_key
ALTER TABLE aggregated_result_battle_key ADD PARTITION (
    PARTITION p_20250201 VALUES LESS THAN (to_days('2025-02-02'))
);

-- aggregated_rank_battle_key
ALTER TABLE aggregated_rank_battle_key ADD PARTITION (
    PARTITION p_20250201 VALUES LESS THAN (to_days('2025-02-02'))
);

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

-- Aggregated Key: 30일 초과 파티션 삭제
ALTER TABLE aggregated_result_battle_key DROP PARTITION p_20250101;
ALTER TABLE aggregated_rank_battle_key DROP PARTITION p_20250101;
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
- `AggregatedResultBattleKeyEntity`: Result 중복 방지 키 엔티티
- `AggregatedRankBattleKeyEntity`: Rank 중복 방지 키 엔티티

### Repository

- `AggregatedResultBattleKeyJpaRepository`
- `AggregatedRankBattleKeyJpaRepository`

### 설정

- `StatsBattleHelper.BATTLE_MAX_AGE_DAYS = 30`: 수집 대상 배틀의 최대 경과 일수
