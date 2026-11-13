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
| `stats_result_battle` | 배틀 결과 통계 데이터 (승/패/무) | `battle_date` | 45일 (`RETENTION_DAYS`) |
| `stats_rank_battle` | 배틀 랭크 통계 데이터 (순위) | `battle_date` | 45일 (`RETENTION_DAYS`) |

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
   | 6 | `stats_event_mode` 테이블의 모드 | `modeBrawlStarsId` 로만 판정한다. 목록 밖 모드(와이프아웃 25, 태그팀/듀얼 24, 5v5 등)는 버린다 |
   | 7 | 팀 구성 일치 | 팀 수가 수집 유형의 `teamCount`, 모든 팀의 인원이 `teamSize` 와 같아야 한다 |
   | 8 | 유형과 경로 일치 | 순위형 테이블은 `battleRank` 가 있고 수집 유형의 `resultType == RANK` 인 모드, 결과형 테이블은 `result` 가 있고 수집 유형의 `resultType == RESULT` 인 모드만 받는다 |

   - 6번은 `battle.mode` 문자열을 쓰지 않는다. battlelog 가 하키(45)를 `brawlBall`, 아레나(48)를 `siege` 로 보내는 등 문자열을 믿을 수 없기 때문이다.
   - 듀얼은 `BattleBuilder` 가 같은 태그로 브롤러 3개씩 2×3 으로 펼치므로 7번은 통과한다. 6번에서 걸려 버려진다.
   - 플레이어 한 명이 브롤러 여러 개를 낸 전투(듀얼 펼침)는 유형과 관계없이 거른다. 관리자가 듀얼에 3대3 유형을 매겨도 6·7번을 통과하는데, 펼침은 전적 표시용이고 통계에서는 한 명의 브롤러들이 팀원·상대로 오해되기 때문이다. `BattleBuilder` 가 `Battle.brawlersExpanded` 로 표시한다.
   - 모드 → 수집 유형은 `stats_event_mode` 테이블, 유형별 팀 구성은 `StatsEventModeType`(core-enum)이 단일 출처다.
   - core-batch `statsPlayerSyncJob`(통계 전투에서 수집 대상 플레이어 발굴)은 모드를 거르지 않고 통계 전투 테이블을 그대로 읽는다. 그래서 허용 목록 적용 뒤 수집된 전투로는 허용 목록 모드에서 본 플레이어만 발굴한다(미지원 모드에서만 보인 플레이어는 이 경로로 들어오지 않는다). 적용 전에 쌓인 행은 파티션 보존 기간 동안 그대로 발굴에 쓰인다.
2. **중복 체크**: Stats 테이블에서 이미 처리된 `battleKey` 인지 확인한다.
3. **Stats Battle 저장**: 중복이 아닌 경우 Stats 테이블에 저장한다 (`battleDate`는 `battleAt`에서 자동 계산).
4. **통계 반영**: 배치에서 Stats 데이터를 집계하여 통계 반영.

### 수집 필터와 집계 제외

전투를 빼는 곳이 두 군데이고 기준이 다르다.

| 구분 | 위치 | 기준 | 빠진 전투의 행방 |
|------|------|------|------------------|
| 수집 필터 | 워커 (`StatsBattleAggregateApplier`) | 정의에 따른 제외. 위 표의 여덟 조건 | 저장하지 않는다 |
| 집계 제외 | 배치 (집계기 다섯 개) | 추정 규칙에 따른 제외. 봇 전투 | 원본에 저장되고 집계에서만 빠진다 |

- 봇 전투는 매치메이킹 봇이 상대인 전투이고, 트로피 분포로 추정하는 규칙(섹션 5.5)으로 판정한다.
- 원본에 남기는 까닭은 둘이다. 규칙을 고친 뒤 원본이 남은 45일 안에서 다시 집계할 수 있다. 같은 전투로 감시 지표를 낼 수 있다.

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
- **보관 기간**: 45일 (`PartitionManageJobConfig.RETENTION_DAYS`). 패치일 등록 기한(30일)보다 길어야 한다. 범위 맞추기가 등록 기한 안의 날짜를 원본에서 다시 읽기 때문이다(가드: `PartitionRetentionLookbackTest`).
- **삭제 시점**: `partitionManageJob` (매일 02:00 KST) 이 `date - RETENTION_DAYS` 보다 이른 일 파티션을 DETACH 후 DROP (`drop_old_partitions`)
- **하한**: 주간 통계 잡의 최대 룩백(`StatsWeekRange.currentAndPrevious` 기준 최대 7일 + 실행일 보정 1일)을 원본이 커버해야 한다. 가드: `PartitionRetentionLookbackTest`

## 5. 통계 집계 배치 전략

### 5.1 멱등 배치

통계 집계 배치는 **멱등(idempotent)** 하게 설계합니다.
- 동일한 날짜에 대해 배치를 재실행해도 안전(원본이 남아 있는 동안). 단 조합·카운터·3인 잡은 버킷을 통째로 덮어쓰므로, 다시 쓰는 버킷의 시작일이 실행일 − 45 보다 이르면 남은 원본만으로 덮어써 부분 데이터가 된다(섹션 10)
- 기존 통계를 삭제 후 재집계하는 방식

### 5.2 재집계 범위

잡 종류에 따라 재집계 범위가 다릅니다. 모두 `date` 파라미터(KST 전일)를 기준으로 합니다.

| 잡 | 실행 시각 (KST) | 재집계 범위 |
|----|----------------|------------|
| `brawlerBattleRankStatsJob`, `brawlerBattleResultStatsJob` (1인 브롤러 통계) | 02:00, 03:00 | `date` 하루 |
| `brawlerEnemyBattleResultStatsJob`, `brawlerTrioBattleResultStatsJob`, `brawlerPairBattleResultStatsJob` (카운터·3인·2인 조합) | 04:00, 05:00, 07:00 | `StatsWeekRange.currentAndPrevious(date)` 주 버킷 |

- **1인 브롤러 통계**: `date` 하루만 집계합니다. 이미 집계한 날짜에 늦게 들어온 전투는 다시 반영되지 않는 한계가 있습니다(조합 통계 재구성 스펙의 비목표).
- **조합·카운터·3인**: 현재 주 버킷을 통째로 다시 집계하고, `date` 전날이 다른 주에 속하면(월요일, 월 첫날) 직전 주 버킷도 다시 집계합니다. 최대 두 버킷입니다. 주 버킷은 월 경계에서 잘립니다(`StatsWeekRange`).

**이유**: 전투 데이터는 계속 수집되며, 이전 날짜에 대해서도 새로운 데이터가 축적됩니다. 주 버킷 단위로 다시 집계해 늦게 수집된 데이터를 같은 주의 통계에 반영합니다. 이 룩백이 원본 배틀 보관 기간(45일, 섹션 4.2)을 넘지 않아야 합니다.

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

### 5.5 봇 전투 제외

매치메이킹 봇이 상대인 전투는 집계에서 뺀다. 트로피 분포로 추정하는 규칙이다. 근거와 결정 이력은 [봇 전투 제외 설계](../../docs/superpowers/specs/2026-11-06-stats-bot-battle-exclusion-design.md) 의 §3.1 과 부록 C.

**규칙**: `StatsBotBattlePolicy`(core-batch `domain/statistics`)에 상수, 판정 함수 둘(`isBotBattle`, `isBotLobby`), 단계 함수를 둔다. 순수 함수이고 예외를 던지지 않는다. 조건을 하나라도 만족하지 않으면 `false` 다. 모드를 보지 않고 팀 구성(`StatsEventModeType.find`)으로 유형을 정하며 `mode` 문자열도 보지 않는다. 트로피는 팀원의 `brawler.trophies` 다. 이름·태그·파워는 판정에 쓰지 않는다.

| 상수 | 값 | 뜻 |
|------|-----|-----|
| `BOT_TROPHY_LIMIT` | 2000 | 봇 슬롯의 트로피 상한(미만) |
| `TEAM_AVERAGE_WINDOW_LOWER` | −75 | 3대3 창의 하한(포함), 수집자 팀 평균 대비 |
| `TEAM_AVERAGE_WINDOW_UPPER` | +25 | 3대3 창의 상한(제외) |
| `CLUSTER_WIDTH` | 75 | 쇼다운 몰림 창의 폭 |
| `SOLO_SHOWDOWN_CLUSTER_THRESHOLD` | 5 | 솔로 쇼다운에서 몰린 팀 수가 이 값 이상이면 봇 로비 |
| `DUO_SHOWDOWN_CLUSTER_THRESHOLD` | 2 | 듀오 쇼다운의 같은 값 |
| `TRIO_SHOWDOWN_CLUSTER_THRESHOLD` | 2 | 트리오 쇼다운의 같은 값 |

**3대3 (`isBotBattle`)**: 아래를 모두 만족하면 봇 전투다. 1 ~ 2 가 적용 범위이고 3 ~ 4 가 판정이다.

1. 전투 유형이 트로피전(`BattleType.RANKED`)이다. 경쟁전, 챌린지, 토너먼트, 알 수 없는 코드는 `false`. 유형을 가장 먼저 본다. 경쟁전 행의 `trophies` 에는 트로피가 아니라 랭크 티어 값이 들어 있다.
2. 팀이 2개이고 각 팀이 3명이다(결과형 3대3 구성). 수집자가 여섯 슬롯 중 정확히 한 곳에 있다.
3. 상대 세 명의 트로피가 모두 2000 미만이다.
4. 수집자 팀 세 명의 트로피 합을 S 라 할 때 상대 세 명 각각의 트로피 t 가 `3·t ≥ S − 225` 이고 `3·t < S + 75` 다. t 가 수집자 팀 평균의 −75 이상 +25 미만이라는 뜻이다. 평균을 정수 나눗셈으로 내면 경계에서 측정과 달라지므로 정수 비교식으로 고정한다.

수집자 팀을 기준으로 상대 쪽만 본다. 양 팀에 대칭으로 적용하지 않는다.

**쇼다운 (`isBotLobby`)**: 아래를 모두 만족하면 봇 로비다. 1 ~ 3 이 적용 범위이고 4 ~ 6 이 판정이다.

1. 전투 유형이 트로피전이다.
2. 팀 구성이 순위형 수집 유형(`SOLO_SHOWDOWN` 10×1, `DUO_SHOWDOWN` 5×2, `TRIO_SHOWDOWN` 4×3) 중 하나와 맞는다.
3. 수집자 팀이 정확히 한 팀이고, 수집자가 전 슬롯 중 정확히 한 곳에 있다.
4. **자격 있는 팀** = 상대 팀 중 팀원 전원의 트로피가 2000 미만인 팀.
5. 자격 있는 팀 A 마다 창 `[min(A), min(A) + 75)` 를 놓는다. 자격 있는 팀 T 는 `min(T) ≥ min(A)` 이고 `max(T) < min(A) + 75` 일 때 그 창에 든다. A 자신에게도 같은 조건을 건다. 창마다 드는 팀 수를 세어 가장 큰 값을 **몰린 팀 수**로 한다. 자격 있는 팀이 없으면 0 이다.
6. 몰린 팀 수가 그 유형의 임계값 이상이다.

수집자 팀은 세지 않고, 수집자 본인의 트로피는 조건에 넣지 않는다. 판정은 행 단위라 같은 로비를 다른 팀의 수집자가 수집한 행은 판정이 다를 수 있다(경계에 걸린 로비에서만, 행 기준 0.2 ~ 0.4%). 로비 단위로 맞추지 않는다.

**적용 지점**: 집계기 다섯 개가 `aggregate()` 진입부에서 건너뛴다.

| 집계기 | 판정 |
|--------|------|
| `BrawlerBattleResultStatsAggregator` (1인) | `isBotBattle` |
| `BrawlerEnemyBattleResultStatsAggregator` (카운터) | `isBotBattle` |
| `BrawlerPairBattleResultStatsAggregator` (2인) | `isBotBattle` |
| `BrawlerTrioBattleResultStatsAggregator` (3인) | `isBotBattle` |
| `BrawlerBattleRankStatsAggregator` (순위형) | `isBotLobby` |

- 전투 단위로 뺀다. 봇 전투에 있던 사람 슬롯도 모두 빠진다(2000 이상인 동료 슬롯 포함).
- 티어 범위와 무관하다. 집계기가 슬롯의 티어 범위를 정하기 전에 전투를 건너뛰므로 걸린 전투는 어느 티어 행에도 들지 않는다.
- 리더, Task 의 읽기·쓰기 흐름, Writer 는 바꾸지 않는다. 리더가 넘기는 전투 수(`전투: N건` 로그)는 제외 전 값이다.
- 규칙 함수는 한 곳이고 호출 지점이 다섯 곳이다. 하나가 빠지면 화면 간 수치가 어긋나므로 테스트로 고정한다.

**감시**: 일별 1인 잡 두 개에서만 돈다. 집계하면서 같은 전투로 지표를 세므로 원본을 한 번 더 읽지 않는다. 조합 잡 세 개에는 넣지 않는다(주 묶음 단위라 일별 추세가 나오지 않는다).

| 잡 | Task | 누적기 |
|----|------|--------|
| `brawlerBattleResultStatsJob` | `BrawlerBattleResultStatsTask` | `StatsBotBattleMonitor` (3대3) |
| `brawlerBattleRankStatsJob` | `BrawlerBattleRankStatsTask` | `StatsBotLobbyMonitor` (쇼다운) |

- 누적기는 `domain/statistics` 의 순수 클래스다. Task 가 전투마다 `monitor.record(battle)` 과 `aggregator.aggregate(battle)` 을 부른다. 누적기는 정책의 판정 함수와 단계 함수만 부르고 범위·창 산식을 다시 쓰지 않는다. 적용 범위를 만족하지 않는 전투는 어느 지표에도 세지 않는다.
- 문턱과 최소 표본은 누적기의 상수이고 KDoc 에 기준선을 적는다. 기준선은 설계 문서 부록 C.4 의 아홉 날짜를 2026-11-06 에 파티션 전체로 잰 값이다.
- 최소 표본에 못 미친 지표는 그날 경고를 판정하지 않고, 요약 로그에 값과 표본 수를 "표본 부족" 으로 남긴다.
- 일별 잡은 다음 날 새벽에 그 시점까지 적재된 전투만 본다. 먼저 수집된 전투의 값은 날짜 전체와 조금 다르다. 걸리는 비중은 낮게 나오고, 3대3 남은 전투 승률과 쇼다운 남은 전투 1위 비율은 높게 나온다. 경고 문턱은 이 폭 밖에 있다.

3대3 지표. 대상은 `isBotBattle` 의 1 ~ 2 를 만족하는 전투다. 승률은 수집자 기준 `승 / (승 + 패)` 이고 무승부는 뺀다. 결정전은 승패가 난 전투다.

| 지표 | 정의 | 기준선 | 경고 조건 | 뜻 |
|------|------|--------|-----------|-----|
| 걸린 전투 승률 | `isBotBattle` 인 전투의 수집자 팀 승률 | 92.7 ~ 95.0% | 90% 미만 (걸린 결정전 1,000건 이상일 때) | 실제 전투를 지우고 있다 |
| 남은 전투 승률 | 수집자 본인 트로피 1000~1999 이고 `isBotBattle` 이 아닌 전투의 수집자 팀 승률 | 51.8 ~ 52.5% (날짜 전체. 새벽 정기 실행 시점에는 52.2 ~ 53.3%) | 55% 초과 (결정전 5,000건 이상일 때) | 봇을 놓치고 있다 |
| 2000 이상 구간 승률 | 4번(창)은 만족하지만 3번을 만족하지 않는(상대 중 2000 이상이 있는) 전투의 수집자 팀 승률 | 56 ~ 61% | 75% 초과 (결정전 300건 이상일 때) | 2000 위에 봇이 생겼다 |
| 걸리는 비중 | 수집자 본인 1000~1999 인 대상 전투(무승부 포함) 중 `isBotBattle` 인 비율 | 26.9 ~ 34.2% (청크별 25 ~ 43%) | 없음 (로그만) | 수집 시점에 따라 크게 움직인다 |
| 기본 이름 비율 | 걸린 전투의 상대 슬롯 중 팀원 `name`(플레이어 이름. `brawler.name` 이 아니다)이 `Brawler` 인 비율 | 약 72% (71.6 ~ 71.9%) | 없음 (로그만) | 떨어지면 봇 표기가 바뀐 것이다 |

쇼다운 지표. 대상은 `isBotLobby` 의 1 ~ 3 을 만족하는 전투다. 모드별로 센다. 1위 비율은 `battleRank` 가 1 인 전투의 비율이다.

| 지표 | 기준선 (솔로 / 듀오 / 트리오) | 경고 조건 | 뜻 |
|------|------------------------------|-----------|-----|
| 걸린 전투의 1위 비율 | 56.5 ~ 61.8% / 76.3 ~ 81.2% / 76.0 ~ 85.8% | 50% / 70% / 70% 미만 (걸린 전투 1,000 / 500 / 300건 이상일 때) | 실제 로비를 지우고 있다 |
| 남은 전투의 1위 비율 | 9.4 ~ 10.8% / 18.7 ~ 21.6% / 23.5 ~ 28.0% | 12.5% / 24% / 31% 초과 (남은 전투 3,000 / 1,000 / 1,000건 이상일 때) | 봇을 놓치고 있다 |
| 걸리는 비중 | 50.5 ~ 68.7% / 42.1 ~ 62.7% / 38.3 ~ 60.2% | 없음 (로그만) | 날짜에 따라 솔로 18%p, 듀오 21%p, 트리오 22%p 까지 움직인다 |

**로그와 알림**

- 요약 로그는 잡마다 한 줄이다: `[잡 이름] 봇 감시 - date: …, …`. 잡 이름은 Task 의 `JOB_NAME` 상수다. 3대3 의 "걸린 전투 수 ÷ 대상 전투 수" 는 기준선과 분모가 달라 비중으로 찍지 않는다.
- 경고 조건에 걸린 지표가 있으면 실행당 한 번 warn 알림을 보낸다. 제목은 `[잡 이름] 봇 전투 규칙 이상 감지` 이고, 본문은 `date` 한 줄과 걸린 지표만 한 줄씩(지표, 값, 문턱, 표본 수)이다.
- 요약과 알림을 만드는 구간에서 난 예외는 로그로만 남기고 집계 잡을 실패시키지 않는다.
- 수동 재집계(섹션 10)에서도 같은 감시가 돈다.

## 6. 데이터 보관 정책

| 테이블 유형 | 파티션 | 보관 기간 | 삭제 조건 |
|-------------|--------|----------|----------|
| Stats Battle (`stats_result_battle`, `stats_rank_battle`) | 일 파티션 `{table}_p_YYYYMMDD` (`battle_date`) | 45일 (`RETENTION_DAYS`) | `date - 45일` 보다 이른 파티션 DROP (`drop_old_partitions`) |
| 조합 통계 (`brawler_pair_battle_result_stats`, `brawler_trio_battle_result_stats`, `brawler_enemy_battle_result_stats`) | 월 파티션 `{table}_p_YYYYMM` (`start_battle_date`), default 파티션 없음 | 지난달 1일부터 | 월 파티션 `{table}_p_YYYYMM`, 지난달 1일 이전 월 DROP (`drop_old_month_partitions`) |

- 원본 배틀은 주 버킷 재집계(섹션 5.2)로 통계에 반영하며, 반영 후에는 필요 없다. 보존 기간 안에서는 원본이 재집계(봇 판정 규칙을 고친 뒤 포함)의 원천이다. 주간 통계 최대 룩백과 패치일 등록 기한(30일)을 모두 덮도록 45일 보관한다.
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
-- Stats Battle: 2025-01-24 기준 45일(RETENTION_DAYS) 이전, 즉 2024-12-10 보다 이른 파티션 삭제
SELECT drop_old_partitions('2025-01-24', 45);
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
- `stats_event_mode` 테이블: 통계를 수집하는 모드의 허용 목록과 모드별 수집 유형. `StatsEventModeType`: 유형별 팀 구성
- `StatsBotBattlePolicy`: 봇 전투 판정 규칙(섹션 5.5). 상수 일곱 개 `BOT_TROPHY_LIMIT = 2000`, `TEAM_AVERAGE_WINDOW_LOWER = -75`, `TEAM_AVERAGE_WINDOW_UPPER = 25`, `CLUSTER_WIDTH = 75`, `SOLO_SHOWDOWN_CLUSTER_THRESHOLD = 5`, `DUO_SHOWDOWN_CLUSTER_THRESHOLD = 2`, `TRIO_SHOWDOWN_CLUSTER_THRESHOLD = 2`
- `StatsBotBattleMonitor`(3대3), `StatsBotLobbyMonitor`(쇼다운): 봇 감시 누적기. 경고 문턱과 최소 표본은 클래스 상수
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
- **덮어쓰기 주의**: 조합·카운터·3인 잡은 `currentAndPrevious(date)` 버킷을 통째로 지우고 다시 쓴다. 다시 쓰는 버킷의 시작일이 실행일 − 45 보다 이르면 원본이 모자라 남은 원본만으로 덮어써 부분 데이터가 된다. `date` 가 버킷의 첫날(월요일 또는 1일)이면 직전 버킷도 다시 쓰므로 직전 버킷의 시작일도 같은 기준으로 본다. 1인 잡은 원본이 없는 날짜로 실행하면 그 날짜의 집계가 전 티어에서 지워진다.
- `run.id` 같은 Spring Batch 예약 키는 피한다. `RunIdIncrementer` 가 쓰는 키라 혼동 소지가 있음. 본 프로젝트는 `RunIdIncrementer` 를 도입하지 않았다 — `JobLauncherApplicationRunner` 는 CLI 파라미터로 JobInstance 를 먼저 조회하고, 완료 상태면 incrementer 를 호출하지 않기 때문에 정기 스케줄 케이스에서 효과가 없다 (Spring Boot issue #22602).

### 봇 판정 규칙을 고친 뒤의 재집계

규칙(섹션 5.5)을 고치면 원본이 남은 날짜만 새 규칙으로 다시 집계할 수 있다. 일반 절차만 적는다. 이번 전환에만 쓰는 백업 후 삭제와 순서는 [설계 문서](../../docs/superpowers/specs/2026-11-06-stats-bot-battle-exclusion-design.md) §3.4 를 따른다.

- **다시 집계할 수 있는 날짜**: 실행일을 T 라 하면 T − 45 ~ T − 1 이다. T 당일은 수집 중이다. `partitionManageJob` 이 T 의 02:00 에 `battle_date < T − 45` 인 파티션을 지우고 T − 45 파티션은 T + 1 의 02:00 에 지워지므로, 재집계는 T + 1 의 02:00 전에 끝낸다.
- **시간대**: 통계 배치 시간대(02:00 ~ 09:00 KST) 밖에서 한다. 같은 날짜·묶음을 두 프로세스가 쓰지 않게 한다.
- **1인 잡**(`brawlerBattleResultStatsJob`, `brawlerBattleRankStatsJob`): `date` 하루를 전 티어 통째로 지우고 다시 쓴다. 가장 오래된 날짜부터 실행한다. 범위 밖 날짜를 넘기지 않는다.
- **조합 잡**(카운터·2인·3인): 다시 쓸 수 있는 묶음은 시작일이 T − 45 이상인 묶음뿐이다. `date` 는 이렇게 고른다.
  - 묶음 하나만 다시 쓰려면 `date` 로 그 묶음의 마지막 날을 넘긴다.
  - 하루짜리 묶음(달 경계)은 다음 묶음의 첫날을 넘겨 두 묶음을 함께 다시 쓴다.
  - 묶음의 첫날을 넘기면 직전 묶음까지 다시 쓰므로, 직전 묶음의 시작일이 T − 45 보다 이르면 넘기지 않는다.
  - 다시 쓰는 범위는 시작일이 T − 45 이상인 묶음부터 T − 1 이 속한 묶음까지 전부다.
  - 배포가 T 의 새벽 정기 실행(카운터 04:00, 3인 05:00, 2인 07:00 KST)보다 늦었으면 T − 1 이 속한 묶음은 구 이미지가 쓴 값이라 세 잡을 `date` = T − 1 로도 실행한다. 예는 설계 문서 §3.4 의 3번.
- **감시**: 수동 재집계에서도 감시가 돈다. 요약 로그와 경고를 같은 기준으로 본다.
- **화면**: 통계 캐시 TTL 30분이 지난 뒤에 본다.

### 봇 감시 경고를 받았을 때

`[잡 이름] 봇 전투 규칙 이상 감지` 알림을 받으면 아래 순서로 본다.

1. 그날과 직전 며칠의 `봇 감시 -` 요약 로그에서 걸린 지표, 값, 표본 수를 본다.
2. 지표의 뜻으로 방향을 정한다.
   - 걸린 전투의 승률·1위 비율이 내려갔다: 실제 전투를 지우고 있다.
   - 남은 전투의 승률·1위 비율이 올라갔다: 봇을 놓치고 있다.
   - 2000 이상 구간 승률이 올라갔다: 봇 매치 상한이 바뀌었다.
   - 기본 이름 비율이 내려갔다: 봇 표기가 바뀌었다. 이 지표만 내려갔으면 규칙은 이름을 쓰지 않으므로 그대로 두고, 걸린 전투의 승률과 함께 내려갔으면 실제 전투를 지우고 있는 쪽을 먼저 본다.
3. 공식 릴리스 노트의 매치메이킹 변경과 시즌 전환·트로피 초기화 직후인지를 확인한다.
4. 규칙을 고쳐야 하면 `StatsBotBattlePolicy` 의 상수나 판정을 테스트와 함께 고쳐 배포하고, 위 절차로 원본이 남은 45일 안에서 다시 집계한다.
5. 일시적인 현상이면 조치 없이 요약 로그를 며칠 더 본다.
