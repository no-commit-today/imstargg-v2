# 조합 통계 재구성 설계 (스펙 B)

- 작성일: 2026-11-04
- 대상: `infrastructure/db-core`(조합·카운터 통계 테이블, 파티션 함수), `core/core-batch`(집계 잡, 보존), `core/core-api`(조합·카운터 API, 티어 스코어), `imstargg-frontend`(브롤러 목록·상세, 이벤트 상세), `imstargg-deploy`(CronJob)
- 상태: 구현 완료 (구현 중 설계에서 달라진 점은 [§8 구현 중 정정](#8-구현-중-정정-2026-11-05))
- 관련: 스펙 A([2026-11-03-stats-event-mode-whitelist-design.md](2026-11-03-stats-event-mode-whitelist-design.md)) 부록 A.2~A.4 가 이 스펙의 1차 입력이다. 티어 스코어 산식 자체의 재검토는 스펙 C 에서 한다. 이 스펙은 산식을 담는 자리(`TierScore`)만 바꾼다.

## 1. 배경

조합 통계는 2인 조합(같은 팀 두 브롤러), 카운터(내 브롤러 대 적 브롤러), 쇼다운 조합 순위 세 가지다. 셋 모두 이벤트(맵) 키로 나눠 저장한다. 질문은 세 개였다. 이벤트 키가 정말 필요한가, 3인 조합을 더할 수 있는가, 용량은 어떻게 되는가. 운영 DB(읽기 전용)로 확인한 사실이다. 수치의 출처와 방법은 [부록 B](#부록-b-분석-근거-2026-11-04)에 있다.

- **맵에 따른 2인 조합 승률 차이는 거의 다 개인 브롤러의 맵 효과다.** 2인 칸의 맵 편차 중 두 브롤러의 1인 맵 편차로 설명되는 비율은 경쟁전 조합 87~94%, 경쟁전 카운터 89~94%, 트로피전 카운터 79~94% 다. 남는 차이는 1.0~1.7pp 이고 반분(수집 플레이어 해시) 사이에서 재현되지 않는다(상관 ≤0.18). 트로피전 조합만 남는 차이가 3.3~4.2pp 로 보였는데, 이 차이는 수집 플레이어의 **상대 팀** 칸에서만 나타나고 수집 플레이어 팀 칸에서는 1.0~1.4pp 였으며 두 진영을 교차하면 상관이 0 이었다. 조합×맵의 성질이 아니라 수집 구조에서 생긴 효과다.
- **이벤트별 조합 값은 예측에 보탬이 없다.** 이벤트 없는 전체 조합 값에 두 브롤러의 1인 맵 편차를 더한 예측기가 이벤트별 조합 원값보다 모든 맥락에서 낫고, 이벤트별 값을 그 위에 수축해 더해도 이득이 없거나(경쟁전 −7~−13%) 작다.
- **지금 이벤트 패널은 표본이 얇다.** 승률순 상위 10개 중 30판 미만이 판수 가중 56~86%(10판 미만 35~57%)이고, 상위 5개가 두 반분에서 겹치는 비율(Jaccard)은 0.16~0.30 이다. 원값을 그대로 보이는 지금 방식은 이벤트 없는 전체 값보다 예측이 나쁘다(MSE −44~−85pp²).
- **쇼다운 조합 순위는 더 얇다.** 10일치에서 이벤트별 칸 중 30판 이상은 듀오 4.5%, 트리오 2.4% 이고, 이벤트 패널 상위 10개는 사실상 전부 30판 미만이다. 이벤트 없는 전체로 모아도 브롤러 절반이 30판 이상 짝을 하나도 갖지 못한다.
- **용량.** `brawler_pair_battle_result_stats` 4.7GB(13.2M 행), `brawler_enemy_battle_result_stats` 5.9GB(17.0M 행). 보존 정책이 없고 주 약 1.1GB 씩 늘어난다. API 가 읽는 가장 오래된 날짜는 지난달 1일(`SINCE_LAST_MONTH`)이다.
- **3인 조합.** 허용 목록 결과형 9모드(2×3)에서 정렬 1행(b1<b2<b3) 기준 주 버킷 약 42만 행이다. 상위 티어는 적다(5일 누적 트로피 3000+ 20행, 마스터+ 5천 행).
- **카운터 표기 방향.** 백엔드는 "내 브롤러가 그 적을 만났을 때 **내** 승률"을 집계하는데, 프론트는 "적 픽일 때의 **적** 승률"로 표기하고 55% 이상을 위협(빨강)으로 칠한다. 방향이 뒤집혀 있다.

## 2. 결정 사항 (사용자 합의)

| # | 결정 | 근거 |
|---|---|---|
| 1 | 2인 조합·카운터에서 **이벤트 키를 뺀다**. 이벤트 상세의 "선택한 브롤러 분석" 패널은 내용이 모두 사라지므로 패널 자체(행 선택, `brawlerId` URL)를 없앤다. 조합·카운터는 브롤러 상세에서만 본다. | §1 첫 세 항목. 맵 차이는 이미 이벤트 상세의 1인 통계가 보여 준다 |
| 2 | **쇼다운 조합 순위(pairRank)** 기능을 없앤다(API·배치·테이블). | 표본이 얇아 보여 줄 값이 없다. 수집량이 늘면 다시 넣는다 |
| 3 | **3인 조합**을 전 티어로 더한다. | 사용자 요구. 상위 티어 행이 적어 전 티어를 쌓아도 용량은 기본 티어가 거의 다 차지한다 |
| 4 | 2인·3인 조합은 **조합 1행 + 정렬 위치 컬럼**(`brawler1 < brawler2 [< brawler3]`)으로 저장한다. 티어는 **조합 멤버 전원**이 그 티어 이상일 때(멤버 최저 트로피로 `resolveAll`). 카운터는 지금처럼 **방향성 행**(기준 플레이어 트로피)으로 둔다. | 브롤러 X 조회와 전체 조합 정렬이 모두 돼야 한다(결정 5). 조합마다 숫자가 하나여야 정렬이 된다. 브롤러 기준 문서형과 조합↔멤버 일대다는 [부록 B.4](#b4-테이블-설계-비교) |
| 5 | **전체 조합 정렬**을 더하고 브롤러 목록 페이지에 "2인 조합·3인 조합" 탭으로 보인다. | 사용자 요구 |
| 6 | **보존 정책**: 지난달 1일 이전 버킷을 지운다. 세 테이블을 `start_battle_date` 월 파티션으로 만들고 `partitionManageJob` 이 DROP 한다. | API 최대 범위 `SINCE_LAST_MONTH` 와 같은 경계. 원본 테이블과 같은 방식 |
| 7 | 기존 2인·카운터 행은 **전 티어 이관**한다. 기본 티어는 정확하고 상위 티어는 근사다. | 사용자 선택. 수집 단계가 전투의 전원 ≥1000(경쟁전 신화+)을 요구하므로 기본 티어에서는 전원 기준과 플레이어 기준이 같다. 상위 티어는 두 방향 중 합계가 작은 쪽을 택해 새 정의의 가장 좁은 상한으로 둔다. 보존 2개월 안에 소멸한다 |
| 8 | 인덱스는 **유니크 `(tier, start, b1, b2[, b3])` + 위치 인덱스 `(tier, bN, start)`**, 카운터는 유니크 `(tier, brawler, start, enemy)` 하나. `end_battle_date` 는 어느 인덱스에도 넣지 않는다. PK `(id, start_battle_date)` 는 관례대로 둔다. | [부록 B.5](#b5-인덱스-실측) 실측. 위치 인덱스는 7B/행이고 없으면 조회가 파티션 전체 스캔으로 떨어진다. PK 는 기능상 쓰이지 않지만 관례와 원본 테이블 선례를 따른다 |
| 9 | 최소 표본·상위 N: 브롤러 기준 목록은 `battleCount ≥ 30` 에 상위 200, 전체 정렬은 `≥ 100` 에 상위 50. | §1 세 번째 항목. 3인 응답은 상한이 없으면 Redis 항목 하나가 최대 약 1.3MB 다 |
| 10 | 티어 스코어는 **즉석 계산**을 유지하고, `BrawlerTierScore` 를 **`TierScore`** 로 일반화해 1인·2인·3인·전체 정렬에 쓴다. 조합에는 **픽률 가중을 끈다**(수축 승률만). | 산식은 기간 단위에서 정해지고 스펙 C 가 바꿀 예정이라 저장하지 않는다. 픽률 곱 가중은 신뢰도 보정이 아니라 인기 감점이다(스펙 A 부록 A.5). 반분 검증에서 가장 잘 맞은 예측기는 수축 승률이다 |
| 11 | 카운터 표기를 바로잡는다. 부제 "이 상대를 만났을 때 내 승률", 55% 이상 초록(유리)·45% 이하 빨강(불리), 기본 정렬 승률 **오름차순**. | §1 마지막 항목 |
| 12 | 조합 픽률 분모는 **조합 테이블 자체의 합**으로 구한다. 카운터 픽률은 **X 의 게임 중 Y 를 만난 비율**로 바꾼다. | 단일 브롤러 테이블 의존을 끊어 이벤트 없는 분모 쿼리(인덱스를 못 탐)를 없앤다. 전원 기준 티어와 맞는 분모다 |

## 3. 설계

### 3.1 데이터 모델 (db-core)

새 테이블 3개가 기존 3개(`brawler_pair_battle_result_stats`, `brawler_enemy_battle_result_stats`, `brawler_pair_battle_rank_stats`)를 대체한다. 세 테이블은 `start_battle_date` 기준 **월 범위 파티션**(`{table}_p_YYYYMM`)이다. 주 버킷은 월 경계에서 잘리므로(`StatsWeekRange`) 한 버킷은 항상 한 파티션에 들어간다.

| 테이블 | 키 컬럼 | 유니크 | 조회 인덱스 | CHECK |
|---|---|---|---|---|
| `brawler_pair_battle_result_stats` | `tier_range`, `start_battle_date`, `end_battle_date`, `brawler1_brawlstars_id`, `brawler2_brawlstars_id` | `(tier_range, start_battle_date, brawler1, brawler2)` | `(tier_range, brawler1, start_battle_date)`, `(tier_range, brawler2, start_battle_date)` | `brawler1 < brawler2` |
| `brawler_trio_battle_result_stats` | 위 + `brawler3_brawlstars_id` | `(tier_range, start_battle_date, brawler1, brawler2, brawler3)` | 위치별 3개 `(tier_range, brawlerN, start_battle_date)` | `brawler1 < brawler2 and brawler2 < brawler3` |
| `brawler_enemy_battle_result_stats` | `tier_range`, `start_battle_date`, `end_battle_date`, `brawler_brawlstars_id`, `enemy_brawler_brawlstars_id` | `(tier_range, brawler_brawlstars_id, start_battle_date, enemy_brawler_brawlstars_id)` | 없음(유니크가 조회를 겸함) | `brawler <> enemy` |

- 공통 컬럼: `{table}_id bigint`(시퀀스, allocationSize 100 — 기존 통계 테이블과 같음), `victory_count`, `defeat_count`, `draw_count`, `total_count bigint`, `created_at`, `updated_at`. PK 는 `(id, start_battle_date)` 다(파티션 키 포함, 원본 `stats_*_battle` 과 같은 꼴).
- 유니크가 `(tier_range, start_battle_date, …)` 로 시작하므로 전체 정렬(티어·기간 범위 스캔)과 버킷 삭제가 이 인덱스를 쓴다. 위치 인덱스는 "브롤러 X 가 어느 위치에 있든" 조회를 맡는다. `end_battle_date` 는 컬럼만 두고 인덱스에 넣지 않는다(start 가 버킷을 유일하게 정한다).
- 한 행의 뜻: "(티어, 주 버킷)에서 이 조합이 성립한 팀의 승·패·무". 2인·3인은 조합당 1행이고 방향 중복이 없다. 카운터는 "(티어, 주 버킷)에서 X 가 Y 를 상대로 만난 결과(X 기준)"이고 (X,Y)·(Y,X) 두 행이 승↔패로 보완된다.
- 엔티티: `BrawlerPairBattleResultStatsEntity`(필드 `brawler1BrawlStarsId`, `brawler2BrawlStarsId`), 신규 `BrawlerTrioBattleResultStatsEntity`, `BrawlerEnemyBattleResultStatsEntity`(이벤트 필드 제거). 모두 `BaseTimeEntity` 상속, `@Table` 에 유니크·인덱스 선언, `init` 에서 정렬 순서와 `brawler <> enemy` 를 `require`. `BrawlerPairBattleRankStatsEntity` 와 `RankCountsEmbedded` 사용처는 지운다.
- 기존 테이블은 이관 소스로 `_legacy` 로 이름을 바꿔 두고(§3.4) 배포 뒤 확인 후 지운다. `brawler_pair_battle_rank_stats` 는 바로 지운다.
- `schema.sql` 을 함께 고친다. 주석의 "보관 기간 7일" 같은 오래된 서술도 실제 값(원본 10일, 집계 월 파티션)으로 바꾼다.

### 3.2 집계 — core-batch

- **집계 단위**: 주 버킷마다 집계기 하나. 이벤트 루프(`readDistinctEventIds`, 이벤트별 집계기·트랜잭션)를 없앤다. 리더는 `battle_date BETWEEN 주 범위` 를 id keyset(5,000건)으로 읽고, 라이터는 버킷 `(start_battle_date, end_battle_date)` 를 지운 뒤 청크(`BATCH_SIZE` 1,000, JDBC batch 100)로 넣는다. 버킷당 트랜잭션 하나(`REQUIRES_NEW`). 매일 `StatsWeekRange.currentAndPrevious(date)` 버킷(현재 주, 월요일·월 첫날에는 직전 주까지 최대 둘)을 다시 집계하는 방식은 그대로다.
- **메모리**: 정렬 1행이라 키가 작다. 5일 누적 실측으로 버킷당 키 수는 2인 약 2.6만, 카운터 약 5.5만, 3인 약 36만(전 티어 합)이다. 3인도 수십 MB 라 티어 분할은 두지 않는다.
- **키**: `BrawlerPairBattleStatsKey(brawler1, brawler2, tierRange)`, 신규 `BrawlerTrioBattleStatsKey(brawler1, brawler2, brawler3, tierRange)`, `BrawlerEnemyBattleStatsKey(brawler, enemy, tierRange)`. 모두 이벤트 필드가 없다.
- **티어 판정**: 조합은 `StatsTierRange.resolveAll(battleType, 멤버 최저 trophies)`. 경쟁전은 `trophies` 필드가 랭크 티어 값이고 수집 단계(`StatsBattleAggregateApplier`)가 같은 비교를 쓴다. 카운터는 기준 플레이어의 `trophies`(변경 없음).
- **2인 집계기**: 팀마다 멤버의 순서 없는 쌍 `(i<j)` 을 브롤러 id 오름차순 키로 만든다. 아군 팀은 결과 그대로, 적 팀은 `opposite()` 로 양 팀을 모두 집계한다(지금과 같음). 같은 팀의 중복 브롤러는 수집 단계가 걸러 주지만 집계기에서도 `require`.
- **3인 집계기**(신규 `BrawlerTrioBattleResultStatsAggregator`): 결과형 전투는 모두 2×3 이므로 팀마다 정렬 키 하나. 티어·결과 처리는 2인과 같다. `ResultCountAccumulator` 를 그대로 쓴다.
- **카운터 집계기**: 이벤트 필드만 뺀다.
- **잡**: `brawlerPairBattleResultStatsJob`(07:00 KST)·`brawlerEnemyBattleResultStatsJob`(04:00) 유지, 신규 `brawlerTrioBattleResultStatsJob`(05:00 KST, 다른 통계 잡과 같은 tasklet 구조, `date` 파라미터 필수). `brawlerPairBattleRankStatsJob` 과 그 리더·라이터·집계기·테스트는 지운다. `imstargg-deploy` core-batch `values.yaml` 에 CronJob `trio-battle-result-stats`(`0 20 * * *` = 05:00 KST, `date=$(date -u -d '15 hours ago' +%Y-%m-%d)`)를 더하고 `pair-battle-rank-stats` 를 지운다.
- **분모용 저장은 없다.** API 가 조합 테이블 합으로 계산한다(§3.5).

### 3.3 보존 — `partitionManageJob`

- `partition-manage.sql` 에 월 파티션용 함수 둘을 더한다. 기존 일별 함수와 같은 방식(대상 테이블 배열 하드코딩, DETACH 뒤 DROP, `if not exists`).
  - `create_month_partitions(base_date date, months_ahead int)`: 세 테이블에 `base_date` 의 달부터 `months_ahead` 달 뒤까지 `{table}_p_YYYYMM` 을 만든다. 운영 값은 1(다음 달까지).
  - `drop_old_month_partitions(base_date date)`: 파티션 월이 `date_trunc('month', base_date) - interval '1 month'` 보다 이전이면 DETACH 후 DROP. 즉 매월 1일 실행에서 두 달 전 파티션이 떨어진다. API 의 `SINCE_LAST_MONTH`(지난달 1일~)와 같은 경계다.
- `PartitionManageJobConfig`(02:00 KST, `date`=KST 오늘)에 스텝 둘(월 파티션 생성, 월 파티션 삭제)을 더한다. 상수 `MONTHS_AHEAD = 1`.
- `partition-manage.sql` 은 앱이 적용하지 않는다. 운영자가 새 core-batch 이미지가 돌기 전에 DB 에 적용한다(§3.8 ①, §8 3번).
- 매일 다시 쓰는 두 버킷의 삭제 흔적은 그 달 파티션 안에서 autovacuum 이 정리한다. 월 단위 DROP 은 블로트를 남기지 않는다.

### 3.4 이관 — 마이그레이션 SQL

`V261104a__combo_stats_redesign.sql` 한 파일, **두 트랜잭션**(1차: DDL·이름 변경, 2차: 이관) + `analyze`. 머리말에 배경·배포 순서(§3.8)·멱등 방식·재실행 금지 기간·적용 중 영향·사전 측정·중단 방법을 적는다. 단일 트랜잭션으로 묶지 않은 이유는 §8 첫 항목.

**1차 트랜잭션** — 이름 변경·삭제·새 테이블. ACCESS EXCLUSIVE 를 몇 초만 잡는다.

1. `alter table brawler_pair_battle_result_stats rename to brawler_pair_battle_result_stats_legacy`, 카운터도 같이. 인덱스·시퀀스 이름도 `_legacy` 로 바꾼다.
2. `drop table if exists brawler_pair_battle_rank_stats`.
3. 새 테이블 3개 생성(§3.1) + 파티션: 지난달·이번 달·다음 달.

**2차 트랜잭션** — 이관. `_legacy` 에는 ACCESS SHARE, 새 테이블에는 ROW EXCLUSIVE 만 잡아 기다리는 쪽이 없다. 커밋 뒤 두 이관 테이블을 `analyze` 한다(새 core-api 의 첫 조회가 통계 없는 파티션을 만나지 않게).

4. 이관(`insert … select`). 범위는 `start_battle_date >= date_trunc('month', current_date) - interval '1 month'` 다. 그 이전 legacy 행은 옮기지 않는다(보존 정책과 같음).
   - **2인**: legacy 를 `(tier_range, start_battle_date, end_battle_date, least(brawler, pair), greatest(brawler, pair), brawler < pair)` 로 묶어 이벤트를 합산한 뒤, 방향 둘 중 `total_count` 합이 작은 쪽을 택한다(같으면 `brawler < pair` 쪽). 기본 티어(트로피 1000+, 신화+)는 두 방향 값이 같아 정확하다. 상위 티어는 "기준 플레이어만 티어 이상" 행이므로 새 정의("전원 티어 이상")의 상한이고, 둘 중 작은 쪽이 가장 좁은 상한이다.
   - **카운터**: `(tier_range, start_battle_date, end_battle_date, brawler, enemy)` 로 이벤트만 합산한다. 의미가 바뀌지 않으므로 정확하다.
5. 검증 쿼리(주석으로 함께 둔다): 기본 티어에서 legacy 의 `brawler < pair` 방향 합산 행 수·합계와 새 테이블이 같은지, 카운터는 전 티어에서 합계가 같은지.
6. `_legacy` 두 테이블의 DROP 은 `V261104b__drop_combo_stats_legacy.sql` 로 배포 뒤 확인하고 적용한다.

- 3인은 이관할 데이터가 없다. 첫 실행이 원본(보존 10일)에서 현재·직전 주를 집계하므로 최대 약 10일분부터 시작한다.
- 이관한 버킷의 날짜로 잡을 수동 재실행하면 원본 10일치로 덮어쓴다. 마이그레이션 머리말과 릴리즈 노트에 "지난달 1일부터 배포 전 7일 전까지의 date 로 pair·enemy 잡을 재실행하지 않는다"고 적는다.
- 과거 행에 섞인 허용 목록 밖 모드(스펙 A 결정 9)는 합산 뒤 분리할 수 없고 2개월 안에 소멸한다.
- 파티션 함수와 이관 SQL 은 H2 에서 돌지 않으므로 docker `postgres:18` 에서 두 번 적용해 멱등을 확인한다(스펙 A 와 같은 방식). 이관 SQL 은 legacy 모양의 표본 데이터로 방향 선택과 합산을 확인한다.

### 3.5 API — core-api

**엔드포인트**

| 경로 | 변화 | 쿼리 파라미터 |
|---|---|---|
| `GET /v1/statistics/brawler/pair/result` | 유지. `eventId`·`pairBrawlerId` 제거 | `brawlerId`, `tierRange`, `dateRange` |
| `GET /v1/statistics/brawler/enemy/result` | 유지. `eventId` 제거 | 같음 |
| `GET /v1/statistics/brawler/trio/result` | 신규. X 가 포함된 3인 조합 | 같음 |
| `GET /v1/statistics/pair/result` | 신규. 전체 2인 조합 상위 목록 | `tierRange`, `dateRange` |
| `GET /v1/statistics/trio/result` | 신규. 전체 3인 조합 상위 목록 | `tierRange`, `dateRange` |
| `GET /v1/statistics/brawler/pair/rank` | 삭제 | |

경로 규칙은 `/v1/statistics/brawler/result`(브롤러 전체 목록)와 같다. `statistics/brawler/…` 는 브롤러 기준, `statistics/pair|trio/…` 는 조합 전체다. 호출 체인(Controller → `StatisticsQueryComposer` → Service → `*RepositoryWithCache` → Repository → QueryDSL CustomImpl)은 지금 구조를 따른다.

**응답** — 모두 `ListResponse<content>`.

| 항목 | 필드 |
|---|---|
| 브롤러 기준 2인 | `brawler`, `pairBrawler`, `battleCount`, `pickRate`, `winRate`, `tierScore`, `tier` |
| 브롤러 기준 카운터 | `brawler`, `enemyBrawler`, `battleCount`, `pickRate`, `winRate` |
| 브롤러 기준 3인 | `brawler`, `pairBrawlers`(2개), `battleCount`, `pickRate`, `winRate`, `tierScore`, `tier` |
| 전체 2인·3인 | `brawlers`(2개 또는 3개), `battleCount`, `pickRate`, `winRate`, `tierScore`, `tier` |

`battleCount` 는 조합이 성립한 팀 수(카운터는 X 가 Y 를 만난 게임 수)다. 2인 응답의 구버전 호환용 `totalBattleCount` getter 는 지운다. 브롤러 참조는 지금처럼 `BrawlerResponse`(이름·이미지 포함)다.

**분모·픽률** — `StatsRate` 의 KDoc(정본)을 갱신한다.

- 조합 픽률 = 조합 팀 수 ÷ 그 (티어, 기간)의 팀 수. 팀 수는 같은 조회 조건의 `Σ total_count` 에서 구한다. 2인은 `÷ 3`(팀 하나가 쌍 셋), 3인은 그대로. SUM 쿼리 한 번이고 유니크 인덱스 범위 스캔이다. 단일 브롤러 테이블(`brawler_battle_result_stats`)에 의존하던 `computeTotalGameCount`·`findEventTotalSlotSums` 경로는 조합·카운터에서 쓰지 않는다.
- 카운터 픽률 = `count(X, Y) ÷ (X 행의 Σ total_count ÷ 3)`. 뜻은 "X 가 치른 게임 중 상대에 Y 가 있던 비율".
- 승률은 `V ÷ (V + D)` 그대로. 1인 브롤러 통계(`/v1/statistics/brawler/result`)는 바꾸지 않는다.

**최소 표본·상위 N** — 도메인 상수 `ComboStatsPolicy` 에 두고 KDoc 에 근거(부록 B.1)를 적는다.

| | 하한 | 상위 N | 정렬 |
|---|---|---|---|
| 브롤러 기준 2인·3인 | `battleCount ≥ 30` | 200 | `tierScore` 내림차순 |
| 브롤러 기준 카운터 | `battleCount ≥ 30` | 200 | `winRate` 오름차순 |
| 전체 2인·3인 | `battleCount ≥ 100` | 50 | `tierScore` 내림차순 |

2인·카운터는 짝이 107개 이하라 상한에 걸리지 않는다. 상위 티어는 하한을 넘는 조합이 적어 빈 목록이 날 수 있다.

**쿼리**

- 브롤러 조회: `tier_range = T AND start_battle_date BETWEEN 기간 AND (brawler1 = X OR brawler2 = X [OR brawler3 = X])` → 조합별 `SUM`. 위치 인덱스 BitmapOr. 실측 23ms(인기 브롤러, 3버킷).
- 전체 정렬: `tier_range = T AND start_battle_date BETWEEN 기간` → 조합별 `SUM` → `HAVING SUM(total_count) >= 100`. 정렬·상위 N 은 DB 가 아니라 도메인에서 한다(티어 스코어가 하한(HAVING ≥ 100)을 통과한 조합의 평균 승률을 필요로 하므로). 하한은 DB 가 적용하므로 도메인은 하한 미만 조합을 보지 못하고, 수축의 기준 평균은 하한을 통과한 조합에서 계산한다. 결과형 2×3 전투에서 모든 조합의 평균 승률은 정확히 0.5 이므로 이 평균과의 차이는 하한을 통과한 집합의 인기 편향(많이 뽑히는 조합일수록 승률이 높은 쪽)이다. 산식 재검토(스펙 C)의 입력이다. 하한을 넘는 조합은 월 단위 수천~2만 행이다. 유니크 인덱스 범위 스캔으로 실측 0.3~0.6초(94만 행).
- 조회 조건은 `start_battle_date` 만 쓴다(`end_battle_date <= 종료일` 조건은 뺀다). 모든 `StatsDateRange` 가 버킷 경계와 맞고 start 가 버킷을 유일하게 정한다.
- 응답에 쓰이지 않는 `countDistinct*`·`countTotalBattleCount` 쿼리와 `totalElementCount` 는 지운다.

**캐시** — Redis `statistics`(TTL 30분, `sync = true`). 키 버전을 올리고 `StatisticsCacheKeyTest` 스냅샷을 갱신한다. 캐시에는 하한·상위 N 을 적용한 결과만 저장한다.

| 메서드 | 키 |
|---|---|
| 1인 브롤러 통계 | `v3:brawlerResultStats:{eventId}:{tierRange}:{startDate}:{endDate}` |
| 브롤러 기준 2인 | `v3:brawlerPairResultStats:{brawlerId}:{tierRange}:{startDate}:{endDate}` |
| 브롤러 기준 카운터 | `v3:brawlerEnemyResultStats:{brawlerId}:{tierRange}:{startDate}:{endDate}` |
| 브롤러 기준 3인 | `v1:brawlerTrioResultStats:{brawlerId}:{tierRange}:{startDate}:{endDate}` |
| 전체 2인 | `v1:pairResultRanking:{tierRange}:{startDate}:{endDate}` |
| 전체 3인 | `v1:trioResultRanking:{tierRange}:{startDate}:{endDate}` |
| 쇼다운 조합 순위 | 삭제 (`v1:brawlerPairRankStats`) |

1인 브롤러 통계 키는 `BrawlerTierScore` 가 `TierScore` 로 바뀌며 Redis 에 캐시된 값의 `@class`(클래스 이름) 역직렬화가 깨져 버전을 올렸다. 응답 형태는 같다.

**도메인** — `BrawlerPairResultStatistics`·`BrawlerEnemyResultStatistics` 는 분모 규칙·하한·정렬만 바꾼다. 신규 `BrawlerTrioResultStatistics`, `PairResultRanking`, `TrioResultRanking`. Pair/Enemy 를 통합하지 않는 원칙(`StatsRate` KDoc)은 그대로다. 카운터 `winRate` 의 KDoc 은 "X 가 Y 를 만났을 때 X 의 승률"로 적는다. 순위형 도메인(`BrawlerPairRankStatistics` 등)은 지운다.

### 3.6 티어 스코어 — `TierScore` (core-api)

- `BrawlerTierScore` 를 `TierScore` 로 바꾼다. `data class TierScore(val value: Int) { fun grade(): TierGrade }`. 등급 enum `BrawlerTier`(S~D)는 `TierGrade` 로 바꾼다. API 필드 이름 `tierScore`·`tier` 와 값(`S`/`A`/`B`/`C`/`D`)은 그대로다.
- `fun <K> create(…, pickRateWeight: Boolean): Map<K, TierScore>`. 입력은 키 `K`(브롤러 id, 2인 쌍, 3인 셋)마다 기간을 합산한 승·패·무이고, 정확한 입력 타입은 구현 계획에서 정한다(지금의 `BrawlerResultCount` 를 키 제네릭으로 넓히는 꼴).
- 산식은 지금 그대로다. 평균 승률로 수축(`BAYESIAN_C = 300`, 전체 정렬의 기준 평균은 하한 통과 조합의 평균 — §8 5번), `pickRateWeight = true` 이면 `ln(1 + 100·픽률) / ln(1 + 100·최대 픽률)` 가중을 곱하고(`MIN_WEIGHT_BETA = 0.1`), `SCALING_K = 200`, 등급 임계값 55/52/48/45. 상수는 이 클래스에만 있다.
- 1인 브롤러 통계는 `pickRateWeight = true`(지금과 같은 값), 2인·3인(브롤러 기준·전체)은 `false`. 산식과 가중 정책의 재검토는 스펙 C 다.
- 티어 스코어를 저장하지 않는 이유를 KDoc 에 남긴다: 산식이 기간 단위에서 정해지고(수축 기준 평균·최대 픽률), 2026.05.3 에 사전집계를 즉석 계산 + 캐시로 바꿨고, 산식이 바뀌면 캐시 키 버전만 올리면 된다.

### 3.7 프론트 — imstargg-frontend

- **브롤러 목록(`/brawlers`)**: 탭 3개 "브롤러 / 2인 조합 / 3인 조합"(`components/ui/tabs.tsx`). URL `?view=pair|trio`, 기본은 브롤러. 탭은 헤더 아래 전폭 라인바다(부제 옆이 아니다). 티어·기간 선택기(`StatRangeSelector`)는 공유한다. 조합 탭 표(`ComboRankingTable`)는 순위 / 조합 브롤러(아이콘 2~3개, 각각 상세 링크) / 티어 배지 / 승률 / 픽률 / 전투수. 서버가 하한·상위 N·스코어 정렬을 이미 적용했으므로 클라이언트는 `SortableHeader` 로 재정렬만 한다. SSR 은 선택된 탭의 데이터만 받는다(`revalidate` SHORT). 빈 목록 문구는 "전투 100회 이상인 조합이 없습니다". 이 표에는 툴바 안내가 없어 하한(100)이 빈 상태 문구에서만 보인다 — 알려진 공백이다(상세의 세 표에는 "전투 30회 이상 … 보입니다" 안내가 있다).
- **브롤러 상세(`/brawlers/[id]`)**: `PairStatsTable`·`EnemyStatsTable` 유지(응답 의미만 바뀜). 신규 `TrioStatsTable`("3인 조합": 순위 / 조합 브롤러 2명 / 티어 배지 / 픽률 / 승률 / 전투수, 기본 정렬 `tierScore`). `PairStatsTable` 도 기본 정렬을 `tierScore` 로 바꾸고 티어 배지를 더한다. `useBrawlerStats` 와 SSR `initialData` 에 3인 쿼리를 더한다. 세 표에 "전투 30회 이상 조합만 보입니다" 안내 한 줄.
- **카운터 표기**: 부제 "이 상대를 만났을 때 내 승률", `enemyRail` 은 55% 이상 `win`(초록), 45% 이하 `loss`(빨강), 기본 정렬 `WIN_RATE` 오름차순.
- **이벤트 상세(`/events/[id]`)**: `EventSelectedBrawlerPanel`·`PairRankTable` 삭제. `EventStatsPanel` 의 행 선택(aria-pressed 토글), `brawlerId` URL 파라미터, `useEventStats` 의 조합·카운터·순위 조합 쿼리를 없앤다. 통계 행의 브롤러 이름은 브롤러 상세 링크로 바꾼다. 스펙 A 의 미지원 모드 안내는 그대로다.
- **API 클라이언트·도메인**: `statistics.ts` 에서 `pairRank` 제거, `pairResult`·`enemyResult` 파라미터에서 `eventId`·`pairBrawlerId` 제거, `trioResult`·`pairRanking`·`trioRanking` 추가. `queryKeys`, 도메인 타입·매퍼(3인, 전체 정렬, `tierScore`/`tier` 필드) 갱신.
- **테스트**: 도메인 매퍼 단위 테스트, 목록 탭(URL·SSR 호출 파라미터·빈 상태), `ComboRankingTable`·`TrioStatsTable`, `EnemyStatsTable`(부제·색·기본 정렬), `EventStatsPanel`(행 선택·패널 제거, 링크), MSW 핸들러 갱신. e2e `tests/e2e/statistics.spec.ts` 의 "/events/[id] 통계 행 클릭 시 brawlerId URL + 선택 패널" 테스트는 지운다.

### 3.8 배포 순서·롤백

1. **DDL 1차** `V261104a`(§3.4)를 **파일 전체**(2차 트랜잭션 커밋과 `analyze` 까지) 적용한다. `partition-manage.sql` 도 함께 적용한다(앱이 적용하지 않는다 — 미적용 시 다음 날 02:00 `partitionManageJob` 의 월 스텝이 `function does not exist` 로 실패한다). **적용 시간대**는 트래픽이 가장 적은 때이고 통계 배치 시간대(02:00~09:00 KST) 밖이다. 1차 트랜잭션 커밋부터 ② 배포 전까지 구버전이 영향을 받는다.
   - 구 core-api 의 `pair/result`·`enemy/result`·`pair/rank` 는 캐시 미스에서 500 을 준다(새 테이블에는 event 컬럼이 없고 pair rank 테이블은 지워졌다). 바로 실패하며 기다리지 않는다.
   - 구 core-batch 의 CronJob `enemy-battle-result-stats`(04:00)·`pair-battle-result-stats`(07:00)가 그 사이에 돌면 실패하고, `pair-battle-rank-stats`(06:00)는 ③ 에서 삭제하기 전까지 돌 때마다 실패한다.
   - 다른 엔드포인트와 페이지는 영향이 없다.
2. **core-api·core-batch 배포**(Deploy 워크플로)를 ① **V261104a 파일 전체가 끝난 뒤** 바로 이어서 한다. 2차 트랜잭션 커밋 전에 새 core-api 가 뜨면 아직 빈 새 테이블을 읽어 빈 응답을 캐시에 쌓는다. 1차 커밋 ~ ② 사이가 조합·카운터·쇼다운 조합 순위만 끊기는 구간이다(2차 트랜잭션 시간 + 배포 시간).
3. **imstargg-deploy**: CronJob `trio-battle-result-stats` 추가, `pair-battle-rank-stats` 삭제.
4. **프론트 배포**(Amplify). core-api 뒤에 올린다. 그 사이 구버전 프론트는 이벤트 상세의 선택 패널에서 지워진 `pair/rank` 를 불러 그 패널만 오류를 보인다.
5. **3인 잡 1회 수동 실행**(`date` = KST 전일). 첫 데이터는 원본 10일분이다.
6. **배포 후 확인**: 새 테이블 버킷·행 수, 기본 티어에서 `_legacy` 방향 합계와 새 행 수 일치, 3인 잡 성공 로그, `/v1/statistics/trio/result` 와 `/v1/statistics/brawler/trio/result` 응답 샘플, 브롤러 목록 탭·상세 3인 표·카운터 색 반전 화면, Redis 에 새 키 버전만 생기는지, 다음 날 02:00 `partitionManageJob` 의 월 파티션 스텝 성공.
7. **DDL 2차** `V261104b`(며칠 뒤): `_legacy` 두 테이블 DROP.

롤백: `_legacy` 를 지우기 전까지 가능하다. 새 테이블을 `_v2` 로 치우고 `_legacy` 를 원래 이름으로 되돌린 뒤 구 이미지를 배포한다. 전환 기간 데이터는 `_legacy` 에 없지만 구버전 일일 잡이 현재·직전 주를 다시 집계해 하루 안에 최대 2주치가 메워진다. 3인 조합·전체 정렬·3인 CronJob 은 사라지고, 캐시는 키 버전이 달라 섞이지 않는다. 2차 DDL 뒤에는 되돌릴 수 없다.

## 4. 바꾸지 않는 것 (비목표)

- 1인 브롤러 통계(`brawler_battle_result_stats`·`/v1/statistics/brawler/result`)의 키·분모·산식. 티어 스코어 산식과 가중 정책의 재검토는 스펙 C.
- 트로피전에서 수집 플레이어 팀 승률이 60.9%(경쟁전 49.4%)인 수집 편향. 통계 전반의 문제라 스펙 C 또는 별도 건.
- 1인 브롤러 일일 잡이 늦게 들어온 전투를 놓치는 문제(하루만 재집계).
- 합산 맥락의 하키·아레나·농구 비중(스펙 A §3.3 메모).
- 운영자용 SQL 뷰, 전체 정렬의 배치 사전 계산, 4인 이상 조합, 쇼다운 조합 순위의 재도입.

## 5. 테스트·검증

- **db-core**: 엔티티 `init` 의 정렬·중복 `require`, `@Table` 과 `schema.sql` 일치(기존 패턴).
- **core-batch**: 집계기 단위 테스트 — 2인(정렬 키, 전원 티어 규칙, 양 팀 집계, 중복 브롤러 `require`), 3인(정렬 키, 전원 티어 규칙), 카운터(이벤트 없음). Task·Writer 컨텍스트 테스트(H2) — 버킷 단위 삭제·삽입, 현재·직전 주 두 버킷, 다른 버킷은 건드리지 않음. `PartitionManageJobConfig` 월 파티션 스텝의 날짜 경계 테스트(`PartitionRetentionLookbackTest` 패턴): 11-01 실행이 9월 파티션을 지우고 10월은 남긴다.
- **core-api**: 레포지토리(H2) — 양방향 위치 조회(X 가 b1/b2/b3 어느 위치든), 분모 SUM, `HAVING`, 기간 경계. 도메인 — 하한·상위 N·픽률(2인 ÷3, 3인, 카운터 X 게임), `TierScore` 가중 on/off 와 등급, 정렬. 컨트롤러(MockMvc) — 다섯 엔드포인트 응답 형태, 삭제된 `pair/rank` 404. `StatisticsCacheKeyTest` 스냅샷.
- **frontend**: §3.7 테스트 목록. `pnpm test:run`, `typecheck`, `lint`.
- **PostgreSQL 전용**: `V261104a`·`V261104b`·파티션 함수를 docker `postgres:18` 에서 두 번 적용해 멱등 확인, legacy 표본으로 이관 방향 선택 검증. CI 는 H2 라 여기서 못 본다.
- **배포 후**: §3.8 6번.

## 6. 문서·버전

- `imstargg-backend/docs/stats-battle-data-strategy.md`: 조합 통계 절을 새 키·티어 규칙·보존 정책으로 고치고, "7일 보존"·"최근 3일 롤링" 같은 오래된 서술을 실제 값으로 바꾼다.
- `schema.sql` 주석, 새 엔티티·`TierScore`·`ComboStatsPolicy`·`StatsRate` KDoc(결정 이력: 이벤트 키를 뺀 이유, 전원 티어 규칙, 저장하지 않는 티어 스코어, 조합 픽률 가중 off).
- 버전: core-api·core-batch·frontend 는 스펙 A 에서 이미 minor SNAPSHOT(0.6.0 / 0.5.0 / 0.8.0)이다. PR 단계에서 `/release` 로 확인한다. core-worker 는 변경이 없다.
- 릴리즈 노트에 적을 것: 카운터 의미 변화(픽률 분모·표기·색·정렬), 상위 티어 빈 목록, 이관 근사, 이관 버킷 재실행 금지, DDL 2단계와 롤백 한계, `pair-battle-rank-stats` CronJob 은 배포 도구가 삭제하지 않으면 수동 삭제, V261104a 적용 중 영향·사전 측정·중단 방법은 파일 머리말, `partition-manage.sql` 수동 적용.

## 7. 위험과 대응

| 위험 | 대응 |
|---|---|
| 상위 티어(트로피 3000+, 마스터+)는 전원 기준이라 3인 조합이 거의 없다 | 프론트 빈 상태 문구. 하한은 `ComboStatsPolicy` 상수라 조정 가능 |
| 이관 근사: 상위 티어 2인 값이 한 달간 새 정의보다 조금 크다 | 두 방향 중 작은 쪽을 택해 상한을 좁힌다. 보존 정책으로 소멸. 릴리즈 노트에 적는다 |
| 카운터 숫자·색·정렬이 바뀌어 사용자가 보는 값이 달라진다 | 의미가 맞아지는 변경이다. 릴리즈 노트에 적는다 |
| 전체 정렬 캐시 미스 0.3~0.6초 | 키 48개라 영향이 작다. 문제가 되면 배치 사전 계산으로 바꿀 자리다 |
| 3인 응답 크기 | 하한 30·상위 200 으로 묶고 캐시에는 적용 후 결과만 저장 |
| H2 가 파티션 함수·이관 SQL 을 못 돌린다 | docker `postgres:18` 두 번 적용으로 확인 |
| 이관 버킷에 잡을 재실행하면 원본 10일치로 덮어쓴다 | 마이그레이션 머리말·릴리즈 노트에 금지 기간 명시 |
| 배치 집계기가 버킷 전체 키를 메모리에 올린다 | 실측 3인 36만 키. 수십 MB. 늘어나면 티어 분할 |
| 과거에 같은 모양(조합 1행)의 3인 테이블을 만들었다가 쓰이지 않아 지운 이력(V260426a→V260707a) | 이번에는 테이블·배치·API·화면을 한 PR 에서 함께 넣는다 |

## 8. 구현 중 정정 (2026-11-05)

구현하며 설계 문장 몇 개가 사실과 달라져 본문을 고쳤다. 달라진 점과 이유를 모아 둔다.

1. **마이그레이션은 두 트랜잭션이다(§3.4).** 1차(이름 변경·pair rank 삭제·새 시퀀스·테이블·인덱스·파티션)와 2차(이관 insert 두 개)로 나누고 커밋 뒤 `analyze` 한다. 단일 트랜잭션은 legacy 전체 스캔(start_battle_date 로 시작하는 인덱스가 없어 2인 약 4.7GB·카운터 약 6.0GB, 2026-11-05 운영 기준) 내내 이름을 바꾼 테이블의 ACCESS EXCLUSIVE 를 쥔다. 그동안 구 core-api 조회가 락을 기다리며 커넥션 풀을 잠식할 수 있다. 두 트랜잭션이면 1차 커밋 뒤 구 조회가 기다리지 않고 바로 실패해 §3.8 의 "조합·카운터만 끊김" 의도대로 된다.
2. **배포 ① 의 영향 서술(§3.8).** "다른 통계와 페이지는 영향이 없다" 앞에 영향 범위를 정확히 적었다: 구 core-api 의 `pair/result`·`enemy/result`·`pair/rank` 는 캐시 미스에서 500, 구 CronJob `enemy-battle-result-stats`·`pair-battle-result-stats` 와 `pair-battle-rank-stats`(③ 전까지)는 돌면 실패. 적용 시간대는 02:00~09:00 KST 밖, ② 는 V261104a 파일 전체가 끝난 뒤.
3. **`partition-manage.sql` 은 앱이 적용하지 않는다(§3.3, §3.8 ①).** 새 core-batch 이미지가 돌기 전에 운영자가 DB 에 함께 적용해야 한다. 미적용 시 다음 날 02:00 `partitionManageJob` 의 월 스텝이 `function does not exist` 로 실패한다.
4. **캐시 키(§3.5).** 1인 브롤러 통계 `v3:brawlerResultStats` 도 올렸다. `BrawlerTierScore` → `TierScore` 로 클래스 이름이 바뀌어 Redis `@class` 역직렬화가 깨지기 때문이다.
5. **티어 스코어의 기준 평균(§3.5, §3.6).** 전체 정렬의 수축 기준 평균은 "기간 전체 평균 승률"이 아니라 하한(HAVING ≥ 100)을 통과한 조합의 평균이다(DB 가 HAVING 을 적용하므로 도메인은 전체를 보지 못한다). 전체 조합의 평균은 2×3 결과 게임에서 정확히 0.5 라, 차이는 하한을 통과한 집합의 인기 편향이다. 스펙 C 입력.
6. **프론트(§3.7).** 탭은 부제 옆이 아니라 헤더 아래 전폭 라인바다. `ComboRankingTable` 에는 툴바 안내(하한 100)가 없어 빈 상태 문구에서만 보인다 — 알려진 공백.
7. **재집계 범위의 실제 동작.** `StatsWeekRange.currentAndPrevious(date)` 는 현재 주 버킷과 `date` 전날이 속한 주를 돌려준다. 전날이 같은 주면 버킷은 하나이고 월요일·월 첫날에만 둘이다(최대 둘). 1인 브롤러 일일 잡은 `date` 하루만 집계한다(비목표 참조).

## 부록 B: 분석 근거 (2026-11-04)

스크립트는 세션 임시 폴더에만 있었으므로 방법과 수치를 여기에 옮긴다. 운영 DB 읽기 전용 조회와 운영과 같은 PostgreSQL 18.3 컨테이너 실측이다.

### B.1 이벤트 키 — 1인 맵 효과 대비 2인 조합·카운터

- 데이터: 10-27~11-01(10-31 제외) 결과형 전투, 핵심 6모드 3v3. 수집 플레이어 태그 해시로 반분. 2인 칸은 같은 팀 쌍(b<p, 그 팀 승리), 카운터 칸은 맞상대 쌍(b<p, b 승리). 1인 승률은 2인 행에서 유도(팀-게임당 브롤러 2회 → ÷2).
- 예측기: `G` 이벤트 없는 조합 승률, `P1` = G 에 두 브롤러의 그 맵 1인 편차(logit, C=300 수축, 칸 자신 제외, 조합의 이벤트 분포로 중심화)를 더함, `P2` 이벤트별 조합 원값, `P3` 이벤트별 값을 P1 로 수축.
- 2인 칸 맵 편차 중 1인 효과로 설명되는 비율(반분 공분산 기준, n≥30/100/300): 경쟁전 조합 87~94%, 경쟁전 카운터 89~94%, 트로피 카운터 79~94%, 트로피 조합 55~68%.
- 1인 효과를 뺀 잔차의 진짜 SD(pp)와 반분 상관: 경쟁전 조합 1.38/1.11/1.19(0.07/0.09/0.17), 경쟁전 카운터 1.45/1.23/1.14(0.09/0.11/0.18), 트로피 카운터 2.87/1.71/1.15(0.31/0.28/0.27; 경험적 베이즈 C 로는 1.69/1.32/0.95), 트로피 조합 4.20/3.73/3.30(0.48/0.62/0.71).
- 트로피 조합 잔차는 상대 팀 칸에만 있다: 상대 팀 5.81/4.92/4.30(0.56~0.75), 수집자 팀 1.41/1.32/0.96(0.07~0.17), 두 진영 교차 SD 0(상관 −0.07~−0.11). 트로피 수집자 팀 승률 60.9%(경쟁전 49.4%), 수집자 우위의 맵별 SD 3.73pp.
- 시험 절반 MSE 개선(G 대비, pp²): 트로피 조합 P1 +26.5 / P3(C300) +30.2 / P2 −69.6; 트로피 카운터 +33.6 / +34.1 / −44.4; 경쟁전 조합 +11.9 / +11.8(C1000) / −84.8; 경쟁전 카운터 +12.1 / +12.0 / −56.9. 모드 단위 집계는 +5.8~+8.8 로 P1 에 밀린다. 트로피 조합을 수집자 팀 칸으로만 시험하면 P1 +2.8, P3 +0.7 이고 날짜 분할에서는 −8.2~−11.7 이다.
- 현재 UI: 승률순 상위 10 중 30판 미만 비율(판수 가중) 트로피 조합 62%, 트로피 카운터 56%, 경쟁전 조합 82%, 경쟁전 카운터 86%(10판 미만 35~57%). 상위 5 반분 Jaccard: 원값 0.16~0.30, 30판 하한 0.22~0.41, 이벤트 없는 전체 0.40~0.68.
- 검증: 코드 감사·독립 재구현·해석 비판 세 갈래로 확인했다. 결론을 뒤집는 결함은 없었다. 재구현은 pp 척도와 경험적 베이즈 C 로 트로피 조합 잔차를 3.56/3.17/2.69 로 냈다(방향 같음, 15~20% 작음).

### B.2 쇼다운 조합 순위

- 데이터: 10-25~11-03 `stats_rank_battle`, 듀오(9)·트리오(38), 수집 플레이어 팀만. 10일 팀-판 듀오 48,665, 트리오 17,835, 모드별 이벤트 11개.
- 이벤트별 칸 중 30판 이상: 듀오 49칸(판 4.5%), 트리오 32칸(2.4%). 이벤트 패널 상위 10 중 30판 미만 100%.
- 평균 순위 예측(G 대비 순위²): 듀오 P1 +0.005, 원값 −0.532; 트리오 +0.005, −0.374. 이벤트 칸의 이득은 없다.
- 이벤트 없는 전체 10일: 30판 이상 조합 듀오 381(판 45%), 트리오 402(37%). 브롤러별 30판 이상 짝 중앙 13개, 0개인 브롤러 55~57/108.

### B.3 3인 조합 용량

- 데이터: 10-30~11-03 `stats_result_battle` 일별 파티션, 허용 목록 결과형 9모드 2×3. 정렬 1행(팀 최저 트로피 티어)과 방향성 3행(플레이어별 티어) 두 가지 키.
- 누적 고유 행(전 티어): 1일 18.9만/65.9만, 5일 36.2만/125만. 증가 지수 약 0.40 → 7일 버킷 외삽 42.2만/146만.
- 5일 정렬 1행 티어별 행 / 30판 이상 조합 / 브롤러별 30판 이상 조합 중앙값(0개 브롤러): 트로피 1000+ 14.7만 / 15,566 / 220(2명), 2000+ 3.7만 / 502 / 1(53명), 3000+ 20 / 0 / 0(전원), 신화+ 11.6만 / 21,603 / 380(1명), 전설+ 5.7만 / 5,330 / 53(11명), 마스터+ 5,233 / 10 / 0(98명).
- 2인 조합·카운터에서 이벤트 키를 빼면 10-26~31 버킷이 1,071,187 → 52,378 / 1,220,635 → 54,627 행이다(이론 상한 약 6만/종류).

### B.4 테이블 설계 비교

- 접근 패턴: 읽기는 "(브롤러 X, 티어, 기간) → 짝 목록" 과 "(티어, 기간) → 전체 조합 정렬". 쓰기는 배치가 현재·직전 주 버킷을 통째로 지우고 넣는다.
- 후보: (R) 조합 1행 + 위치 컬럼, (R-dir) 기준 브롤러별 방향성 행(3인은 3행), (D) 브롤러 기준 문서(티어·버킷·브롤러당 1행, 짝 목록 JSON), (일대다) 조합 테이블 + 멤버 자식 테이블.
- D 는 브롤러 조회에는 가장 작고 빠르지만(상시 0.1~0.2GB, 문서 수 636/버킷, 최대 문서 jsonb 45.7KB) 같은 조합이 세 문서에 들어가고 티어 의미가 보는 사람 기준이라 **전체 정렬이 안 된다**. 결정 5 로 제외.
- R-dir 은 상시 5.85GB, 하루 약 290만 행 재작성(WAL 약 950MB)이고 역시 조합당 숫자가 셋이다. 제외.
- 일대다는 이 저장소가 2026-04-20(V260420a, `team_key` + 멤버 자식 테이블)에 만들었다가 6일 뒤(V260426a)에 방향성 행으로 바꾼 구조다. 조합 크기가 2·3 으로 고정이라 위치 컬럼이 조인 없이 같은 성질을 얻는다. 멤버 행(3인 주 127만)이 더 든다. 배열 + GIN 은 매핑 선례가 없고 H2 와 맞지 않는다.
- R 은 과거(V260420a)에 `team_key` 문자열 키로 만들었을 때 `GROUP BY team_key` 인덱스 부재가 문제였다. 이번에는 정렬 위치 컬럼과 유니크 인덱스가 그 역할을 한다.

### B.5 인덱스 실측

- 환경: `postgres:18.3-alpine`, 실제 3인 키(5일 누적)를 10개 주 버킷으로 늘려 427만 행, 2인 30.2만 행. 2026-10 파티션(168만 행) 기준.
- 크기: heap 224MB(136B/행), 유니크 `(tier, b1, start, b2, b3)` 124MB(75B), `end` 포함 유니크 124MB(차이 없음), 위치 인덱스 `(tier, bN, start)` 12MB 씩(7B — 중복 키 압축), `(tier, start)` 11MB, PK `(id, start)` 50MB(30B). 측정한 유니크는 `(tier, b1, start, b2, b3)` 순서였고, §3.1 이 제안하는 `(tier, start, b1, b2, b3)` 순서가 전체 정렬·버킷 삭제에 주는 효과는 `(tier, start)` 인덱스로 대신 재었다. 컬럼이 같아 크기는 같다.
- 브롤러 조회(인기 브롤러, 3버킷, 19.8k 행): 유니크만 31.6ms / 20.7k 페이지(한 파티션은 33만 행 걸러낸 전체 스캔, 디스크 읽기 4,309) → 위치 인덱스 22.7ms / 5.6k 페이지(읽기 33). 희귀 브롤러 8.5ms.
- 전체 정렬(지난달부터, 94만 행): `(tier, start)` 로 시작하는 인덱스 없이 586ms / 40k 페이지(파티션 2개 전체 스캔) → 있으면 303ms / 17k 페이지. 이번 주 108 → 81ms.
- 버킷 삭제(31만 행): 144 → 112ms.
- 2인(2026-10, 12.6만 행): heap 16MB, 유니크 8MB, 위치 인덱스 1MB. 조회 1.9 → 0.4ms, 전체 정렬 9ms.
- 상시 용량(보존 2개월 ≈ 10.5버킷, 비대 전): 3인 약 1.2GB(263B/행), 2인 약 75MB, 카운터 약 125MB. 합 약 1.4GB. 지금은 10.6GB 이고 주 1.1GB 씩 는다.
