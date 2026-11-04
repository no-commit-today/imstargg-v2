# 조합 통계 재구성(스펙 B) 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 2인 조합·카운터 통계에서 이벤트 키를 빼고, 3인 조합과 전체 조합 정렬을 더하고, 세 테이블을 월 파티션으로 보존하며, 쇼다운 조합 순위를 없앤다.

**Architecture:** 조합은 "조합 1행 + 정렬 위치 컬럼(brawler1 < brawler2 [< brawler3])", 티어는 조합 멤버 최저 트로피로 판정한다. 카운터는 지금처럼 방향성 행이다. 배치는 주 버킷 단위로 집계기 하나를 돌려 버킷을 지우고 다시 넣는다. API 는 하한·상위 N·`TierScore` 정렬을 도메인에서 적용한 결과만 캐시한다. 프론트는 브롤러 목록에 "2인 조합 / 3인 조합" 탭을, 상세에 3인 표를 더하고 이벤트 상세의 선택 패널을 없앤다.

**Tech Stack:** Spring Boot 4 + Kotlin(core-enum·db-core(JPA/QueryDSL)·core-batch(Spring Batch)·core-api), PostgreSQL 18(월 범위 파티션, plpgsql), H2 MODE=PostgreSQL(테스트), Redis(statistics 캐시), Next.js + TypeScript + Vitest/MSW + Playwright, imstargg-deploy(K8s CronJob values.yaml).

**Spec:** [docs/superpowers/specs/2026-11-04-combo-stats-redesign-design.md](../specs/2026-11-04-combo-stats-redesign-design.md) — 결정 1~12, §3 설계, 부록 B 수치가 근거다. 이 계획이 스펙과 어긋나면 스펙이 이긴다.

## Global Constraints

- **컴파일 결합**: Task 3(db-core 엔티티)부터 Task 9(core-api 조합 API)까지는 모듈 테스트만 녹색이면 된다. Task 3·4 는 `./gradlew :infrastructure:db-core:test`, Task 5~7 은 `./gradlew :core:core-batch:test`(core-api 는 빨간 상태 허용), Task 8 에서 `./gradlew build` 가 다시 녹색이어야 한다. 리뷰어는 이 구간에서 다른 모듈의 컴파일 실패를 결함으로 보지 않는다.
- 백엔드 명령은 `imstargg-backend/`, 프론트 명령은 `imstargg-frontend/` 에서 실행한다. 커밋 전 백엔드는 `./gradlew ktlintFormat`, 프론트는 `pnpm lint` 를 통과한다.
- 테스트 서브클래스에 `@Tag("context")` 를 달지 않는다(`CoreApiContextTest`·`CoreBatchContextTest` 가 이미 가진다). CI 는 H2 이고 Redis 가 없다(`redisCacheManager` 직접 호출 금지). 파티션 함수·이관 SQL 은 H2 에서 돌지 않으므로 docker `postgres:18-alpine` 으로 확인한다.
- 테이블·인덱스(스펙 §3.1): 유니크 `brawler_pair_battle_result_stats (tier_range, start_battle_date, brawler1_brawlstars_id, brawler2_brawlstars_id)`, `brawler_trio_battle_result_stats (… , brawler3_brawlstars_id)`, `brawler_enemy_battle_result_stats (tier_range, brawler_brawlstars_id, start_battle_date, enemy_brawler_brawlstars_id)`. 위치 인덱스 `(tier_range, brawlerN_brawlstars_id, start_battle_date)` 는 2인 2개·3인 3개, 카운터는 없다. `end_battle_date` 는 어느 인덱스에도 넣지 않는다. PK `(id, start_battle_date)`, `start_battle_date` 월 범위 파티션 `{table}_p_YYYYMM`, default 파티션 없음. 시퀀스 `{table}_seq increment by 100`, 엔티티 `allocationSize = 100`.
- 티어 규칙: 조합은 `StatsTierRange.resolveAll(battleType, 멤버 trophies 의 최솟값)`, 카운터는 기준 플레이어 `trophies`.
- 하한·상위 N(`ComboStatsPolicy`): `BRAWLER_MIN_BATTLE_COUNT = 30L`, `BRAWLER_TOP_N = 200`, `RANKING_MIN_BATTLE_COUNT = 100L`, `RANKING_TOP_N = 50`. 분모: 2인 = Σ total_count ÷ 3, 3인 = Σ total_count, 카운터 = X 행 Σ total_count ÷ 3.
- `TierScore` 상수: `BAYESIAN_C = 300.0`, `LOG_ALPHA = 100.0`, `SCALING_K = 200.0`, `MIN_WEIGHT_BETA = 0.1`, 등급 임계 55/52/48/45, 기본값 `TierScore(50)`. 1인 통계 `pickRateWeight = true`, 조합·전체 정렬 `false`.
- 캐시 키(Redis `statistics`, `sync = true`): `'v3:brawlerResultStats:' + #eventId + ':' + #tierRange + ':' + #startDate + ':' + #endDate`(1인 브롤러 통계 — `BrawlerTierScore` → `TierScore` 클래스 이름 변경으로 Redis `@class` 역직렬화가 깨져 올린다), `'v3:brawlerPairResultStats:' + #brawlerId + ':' + #tierRange + ':' + #startDate + ':' + #endDate`, `'v3:brawlerEnemyResultStats:' + …(같은 꼴)`, `'v1:brawlerTrioResultStats:' + …(같은 꼴)`, `'v1:pairResultRanking:' + #tierRange + ':' + #startDate + ':' + #endDate`, `'v1:trioResultRanking:' + …(같은 꼴)`. `v1:brawlerPairRankStats` 는 삭제.
- 배치: 3인 잡 `brawlerTrioBattleResultStatsJob`, CronJob `trio-battle-result-stats` `"0 20 * * *" # 05:00 KST`, args `date=$(date -u -d '15 hours ago' +%Y-%m-%d)`. `pair-battle-rank-stats` CronJob 삭제. 보존 `MONTHS_AHEAD = 1`, 삭제 경계 `date_trunc('month', base_date) - interval '1 month'`.
- 마이그레이션: `V261104a__combo_stats_redesign.sql`(rename `_legacy`, drop pair_rank, 새 테이블·파티션, insert-select), `V261104b__drop_combo_stats_legacy.sql`. 두 번 적용해도 같은 결과.
- 커밋은 Conventional Commits(`feat:`/`refactor:`/`docs:`/`test:` + scope 예 `feat(core-api):`), 끝에 `Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>` 트레일러. 주석·KDoc·테스트 이름은 한국어.
- API 변경은 프론트에 반영한다(AGENTS.md). 프론트 호환 getter `totalBattleCount` 는 2인·카운터 응답 둘 다 지운다(프론트 매퍼는 이미 `battleCount` 만 읽는다).

## Review Focus

1. **X 가 brawler2/brawler3 위치인 조합** — 브롤러 기준 2인·3인 조회에 그 조합이 나오고, `pairBrawler`/`pairBrawlers` 가 X 를 뺀 나머지다. → Task 8 `BrawlerPairResultStatisticsTest` "X 가 brawler2 이면 pairBrawlerId 는 brawler1", Task 9 `BrawlerTrioResultStatisticsTest` "X 가 brawler3 이면 pairBrawlerIds 는 [brawler1, brawler2]", Task 9 저장소 테스트 "X 가 b1·b2·b3 어느 위치든 조회된다".
2. **전체 정렬 분모는 HAVING 전의 합** — 하한 미만 조합도 분모(Σ total_count)에 들어가야 픽률이 맞다. → Task 9 저장소 테스트 "sumTotalCount 는 HAVING 에 걸린 행도 더한다".
3. **빈 상위 티어** — 조합이 없거나 분모가 0 이어도 빈 목록이고 0 으로 나누지 않는다. 화면은 "전투 100회 이상인 조합이 없습니다". → Task 9 `TrioResultRankingTest` "counts 가 비면 items 가 비고 예외가 없다", Task 13 `ComboRankingTable` 빈 상태 테스트.
4. **월 경계 버킷** — 10-26~10-31 과 11-01 버킷이 각자의 달 파티션에 들어가고, 라이터는 `(start, end)` 가 정확히 같은 버킷만 지운다. 이관 범위는 지난달 1일 이후 버킷을 모두 포함한다. → Task 5 `BrawlerPairBattleResultStatsWriterTest` "다른 버킷은 건드리지 않는다", Task 4 docker 확인(표본에 10-26~10-31·11-01 버킷 포함).
5. **`pickRateWeight = false` 경로** — 픽률 가중 없이도 승률 차이가 점수 차이로 나오고, 전체 합이 0 일 때의 "모두 50" 반환이 이 경로에서 잘못 발동하지 않는다. → Task 2 `TierScoreTest` "가중을 끄면 픽률이 달라도 같은 승률은 같은 점수", "가중을 꺼도 n=0 은 50".

---

### Task 1: 쇼다운 조합 순위(pairRank) 제거

**Files:**
- Delete: `imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/BrawlerPairBattleRankStatsEntity.kt`, `…/statistics/repository/BrawlerPairBattleRankStatsJpaRepository.kt`, `…JpaRepositoryCustom.kt`, `…JpaRepositoryCustomImpl.kt`, `…/repository/BrawlerPairRankProjection.kt`
- Delete: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/pairbattlerank/`(4개), `…/batch/domain/statistics/BrawlerPairBattleRankStatsAggregator.kt`, 테스트 `…/test/…/domain/statistics/BrawlerPairBattleRankStatsAggregatorTest.kt`, `…/test/…/job/statistics/pairbattlerank/BrawlerPairBattleRankStatsTaskTest.kt`
- Delete: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/api/controller/v1/response/BrawlerPairRankStatisticsResponse.kt`, `…/core/domain/statistics/BrawlerPairRankCount.kt`, `…/BrawlerPairRankStatistics.kt`, 테스트 `…/test/…/domain/statistics/BrawlerPairRankStatisticsTest.kt`
- Modify: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/api/controller/v1/StatisticsController.kt`(`getBrawlerPairRankStatistics` 삭제), `…/api/query/StatisticsQueryComposer.kt`, `…/application/statistics/BattleRankStatisticsService.kt`, `…/BattleRankStatisticsRepository.kt`, `…/BattleRankStatisticsRepositoryWithCache.kt`(`…PairRank…` 메서드 삭제)
- Modify: `imstargg-backend/core/core-api/src/test/kotlin/com/imstargg/core/application/statistics/StatisticsCacheKeyTest.kt`, `…/BattleRankStatisticsRepositoryTest.kt`(pair rank 테스트 삭제), `…/api/controller/v1/StatisticsControllerTest.kt`
- Modify: `imstargg-backend/infrastructure/db-core/src/main/resources/schema.sql`(`brawler_pair_battle_rank_stats` 블록 삭제), `imstargg-deploy/apps/prod/core-batch/values.yaml`(`pair-battle-rank-stats` 항목 삭제)
- 남긴다: `RankCountsEmbedded`·`RankCountAccumulator`·`RankCountsEmbeddedExtensions` 는 `brawler_battle_rank_stats` 가 쓴다.

**Interfaces:**
- Produces: `StatisticsCacheKeyTest` 스냅샷에 `findBrawlerPairRankStatistics` 항목이 없다. `GET /v1/statistics/brawler/pair/rank` 는 404.

- [ ] **Step 1: 컨트롤러 테스트를 먼저 바꾼다** — `StatisticsControllerTest` 에서 `pair rank 에 brawlerId 가 없으면 400`·`pair rank 의 legacy sort 파라미터는 무시한다` 를 지우고, 기존 `team result endpoint 는 등록되어 있지 않다` 와 같은 꼴로 추가:

```kotlin
@Test
fun `pair rank endpoint 는 삭제되어 404`() {
    mockMvc
        .get("/v1/statistics/brawler/pair/rank") {
            param("eventId", "60000001"); param("brawlerId", "16000000")
            param("tierRange", "TROPHY_1000_PLUS"); param("dateRange", "THIS_WEEK")
        }.andExpect { status { isNotFound() } }
}
```

- [ ] **Step 2: 실패 확인** — `./gradlew :core:core-api:test --tests "com.imstargg.core.api.controller.v1.StatisticsControllerTest"` → 새 테스트 FAIL(200).
- [ ] **Step 3: Files 의 삭제·수정을 모두 적용한다.** `StatisticsCacheKeyTest` 스냅샷에서 `BattleRankStatisticsRepositoryWithCache.findBrawlerPairRankStatistics` 줄을 지운다. `values.yaml` 에서 `pair-battle-rank-stats` 블록 4줄을 지운다.
- [ ] **Step 4: 확인** — `./gradlew ktlintFormat build` → BUILD SUCCESSFUL. `grep -rn "PairRank\|PairBattleRank" imstargg-backend --include='*.kt' --include='*.sql'` → 결과 없음.
- [ ] **Step 5: Commit** — `refactor: 쇼다운 조합 순위(pairRank) API·배치·테이블 정의를 지운다`

### Task 2: `TierScore`·`TierGrade` 일반화

**Files:**
- Rename: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/domain/statistics/BrawlerTierScore.kt` → `TierScore.kt`, `BrawlerTier.kt` → `TierGrade.kt`, 테스트 `BrawlerTierScoreTest.kt` → `TierScoreTest.kt`
- Modify: `…/domain/statistics/BrawlerResultStatistics.kt`, `…/api/controller/v1/response/BrawlerResultStatisticsResponse.kt`, `…/test/…/response/BrawlerResultStatisticsResponseTest.kt`(이름 참조)

**Interfaces:**
- Produces: `enum class TierGrade(val value: Int) { S(1), A(2), B(3), C(4), D(5) }`; `data class TierScore(val value: Int) { fun grade(): TierGrade }`; `companion fun <K> create(counts: Map<K, ResultCount>, pickRateWeight: Boolean): Map<K, TierScore>`. 응답 필드는 그대로 `tierScore = item.tierScore.value`, `tier = item.tierScore.grade().name`.

- [ ] **Step 1: 테스트** — `TierScoreTest` 로 이름을 바꾸고 기존 케이스를 `TierScore`/`TierGrade`/`grade()`/`create(counts.associate { it.brawlerId to it.resultCount }, pickRateWeight = true)` 로 옮긴다. 추가:

```kotlin
@Test
fun `가중을 끄면 점수는 수축 승률만으로 정해진다`() {
    // avg = 1160/2100 = 0.55238. few: adj = (100·0.6 + 300·avg)/400 = 0.56429 → 50 + 0.01190·200 = 52.38 → 52
    // many: adj = (1000·0.6 + 300·avg)/1300 = 0.58901 → 57.33 → 57. 가중 on 이면 few 의 픽률 가중 0.506 으로 51.
    val counts = mapOf(
        "many" to ResultCount(victoryCount = 600, defeatCount = 400, drawCount = 0),
        "few" to ResultCount(victoryCount = 60, defeatCount = 40, drawCount = 0),
        "avg" to ResultCount(victoryCount = 500, defeatCount = 500, drawCount = 0),
    )
    val off = TierScore.create(counts, pickRateWeight = false)
    val on = TierScore.create(counts, pickRateWeight = true)
    assertThat(off["few"]).isEqualTo(TierScore(52))
    assertThat(off["many"]).isEqualTo(TierScore(57))
    assertThat(on["few"]!!.value).isLessThan(off["few"]!!.value)
}

@Test
fun `가중을 꺼도 n 이 0 인 키는 50 이다`() {
    val result = TierScore.create(mapOf("x" to ResultCount(0, 0, 0)), pickRateWeight = false)
    assertThat(result["x"]).isEqualTo(TierScore(50))
}
```

- [ ] **Step 2: 실패 확인** — `./gradlew :core:core-api:test --tests "com.imstargg.core.domain.statistics.TierScoreTest"` → 컴파일 FAIL.
- [ ] **Step 3: 구현** — `TierScore.create` 는 `pickRateWeight = false` 이면 가중 계수를 1.0 으로 두고 `maxPickRate` 를 계산하지 않는다(전체 합 0 이면 모든 키 50). KDoc 에 "저장하지 않는 이유"(산식이 기간 단위에서 정해짐, 2026.05.3 사전집계→즉석 계산+캐시, 산식 변경은 캐시 키 버전만 올림)와 "조합은 가중 off"(스펙 A 부록 A.5: 픽률 곱은 신뢰도 보정이 아니라 인기 감점) 를 적는다. `BrawlerResultStatistics.create` 는 `TierScore.create(brawlerAggregates.associate { it.brawlerId to it.resultCount }, pickRateWeight = true)`.
- [ ] **Step 4: 확인** — `./gradlew ktlintFormat :core:core-api:test` → PASS. `grep -rn "BrawlerTierScore\|BrawlerTier\b" imstargg-backend --include='*.kt'` → 없음.
- [ ] **Step 5: Commit** — `refactor(core-api): BrawlerTierScore 를 TierScore 로 일반화하고 픽률 가중을 선택지로 둔다`

### Task 3: db-core — 조합·카운터 엔티티·조회·schema.sql

**Files:**
- Modify: `imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/BrawlerPairBattleResultStatsEntity.kt`, `BrawlerEnemyBattleResultStatsEntity.kt`
- Create: `…/statistics/BrawlerTrioBattleResultStatsEntity.kt`, `…/statistics/repository/BrawlerTrioBattleResultStatsJpaRepository.kt`, `BrawlerTrioBattleResultStatsJpaRepositoryCustom.kt`, `BrawlerTrioBattleResultStatsJpaRepositoryCustomImpl.kt`, `BrawlerTrioResultProjection.kt`
- Modify: `…/repository/BrawlerPairBattleResultStatsJpaRepositoryCustom.kt`, `…CustomImpl.kt`, `BrawlerPairResultProjection.kt`, `BrawlerEnemyBattleResultStatsJpaRepositoryCustom.kt`, `…CustomImpl.kt`
- Modify: `imstargg-backend/infrastructure/db-core/src/main/resources/schema.sql`
- Test: `imstargg-backend/infrastructure/db-core/src/test/kotlin/com/imstargg/infrastructure/db/core/statistics/BrawlerPairBattleResultStatsEntityTest.kt`, `BrawlerTrioBattleResultStatsEntityTest.kt`(순수 JUnit), `…/statistics/repository/ComboBattleResultStatsJpaRepositoryCustomImplTest.kt`(`DbCoreContextTest` 상속, `BattleJpaRepositoryCustomImplTest` 와 같은 꼴)

**Interfaces:**
- Produces 엔티티 생성자(모두 `BaseTimeEntity`, `@Embedded resultCount: ResultCountEmbedded`, `tierRange: StatsTierRange`, `startBattleDate/endBattleDate: LocalDate`):
  - `BrawlerPairBattleResultStatsEntity(tierRange, startBattleDate, endBattleDate, brawler1BrawlStarsId: Long, brawler2BrawlStarsId: Long, resultCount)` — `init { require(brawler1BrawlStarsId < brawler2BrawlStarsId) }`
  - `BrawlerTrioBattleResultStatsEntity(tierRange, startBattleDate, endBattleDate, brawler1BrawlStarsId, brawler2BrawlStarsId, brawler3BrawlStarsId, resultCount)` — `require(b1 < b2 && b2 < b3)`; 시퀀스 `brawler_trio_battle_result_stats_seq`, generator `brawlerTrioBattleResultStatsSeq`, id 컬럼 `brawler_trio_battle_result_stats_id`
  - `BrawlerEnemyBattleResultStatsEntity(tierRange, startBattleDate, endBattleDate, brawlerBrawlStarsId, enemyBrawlerBrawlStarsId, resultCount)` — `require(brawler != enemy)`
- `@Table` 이름: 유니크 `uk_{table}__1`, 위치 인덱스 `ix_{table}__brawler1|2|3`(컬럼 순서는 Global Constraints).
- 프로젝션: `BrawlerPairResultProjection(brawler1BrawlStarsId, brawler2BrawlStarsId, victoryCount, defeatCount, drawCount)`, `BrawlerTrioResultProjection(brawler1BrawlStarsId, brawler2BrawlStarsId, brawler3BrawlStarsId, victoryCount, defeatCount, drawCount)`, `BrawlerEnemyResultProjection` 그대로.
- Custom(모두 `tierRange: StatsTierRange, startDate: LocalDate, endDate: LocalDate`; 날짜 조건은 `startBattleDate.between(startDate, endDate)` 만):
  - Pair/Trio: `findAllByBrawler(brawlerBrawlStarsId: Long, tierRange, startDate, endDate): List<…Projection>`(WHERE `b1 = X OR b2 = X [OR b3 = X]`, GROUP BY 위치 컬럼, SUM), `findAllHavingTotalCountAtLeast(tierRange, startDate, endDate, minTotalCount: Long): List<…Projection>`(GROUP BY 위치 컬럼 HAVING `SUM(total_count) >= min`), `sumTotalCount(tierRange, startDate, endDate): Long`(없으면 0).
  - Enemy: `findAllByBrawler(brawlerBrawlStarsId, tierRange, startDate, endDate): List<BrawlerEnemyResultProjection>`(WHERE `brawler = X`, GROUP BY brawler, enemy). `countDistinct*` 메서드는 모두 삭제.

- [ ] **Step 1: 엔티티 단위 테스트** — `BrawlerPairBattleResultStatsEntityTest`: `brawler1 >= brawler2 이면 IllegalArgumentException`(같은 값·역순 두 케이스), `brawler1 < brawler2 는 생성된다`. `BrawlerTrioBattleResultStatsEntityTest`: `(1,2,2)`·`(2,1,3)` 예외, `(1,2,3)` 생성.
- [ ] **Step 2: 조회 컨텍스트 테스트** — `ComboBattleResultStatsJpaRepositoryCustomImplTest : DbCoreContextTest()`, 세 저장소 주입. 케이스(모든 날짜는 `start=2026-10-26, end=2026-10-31` 버킷 A 와 `start=2026-11-01, end=2026-11-01` 버킷 B):
  - `pair findAllByBrawler - X 가 brawler1 이든 brawler2 이든 찾고 버킷을 합산한다`: (10,20) A: 승1, (10,20) B: 패1, (5,10) A: 승1 → X=10 결과 2행, (10,20) 은 v=1,d=1.
  - `pair findAllByBrawler - 기간 밖 버킷은 더하지 않는다`: startDate=2026-11-01 로 조회하면 (10,20) v=0,d=1.
  - `pair findAllHavingTotalCountAtLeast - 하한 미만 조합은 빠지고 sumTotalCount 는 모두 더한다`: (10,20) 총 3, (10,30) 총 1 → min 2 결과 1행, `sumTotalCount` = 4.
  - `trio findAllByBrawler - X 가 b1·b2·b3 어느 위치든 찾는다`: (10,20,30), (5,10,30), (1,2,10) 각 1판 → X=10 결과 3행.
  - `enemy findAllByBrawler - X 기준 행만 돌려준다`: (10→20) 승1, (20→10) 패1 → X=10 결과 1행 v=1.
- [ ] **Step 3: 실패 확인** — `./gradlew :infrastructure:db-core:test` → 컴파일 FAIL.
- [ ] **Step 4: 구현** — Interfaces 대로 엔티티·프로젝션·Custom/Impl 을 만들고 고친다. 엔티티 KDoc: "한 행 = (티어, 주 버킷)에서 이 조합이 성립한 팀의 승·패·무. 티어는 멤버 전원 기준(최저 trophies). 이벤트 키는 2026-11 스펙 B 에서 뺐다(맵 효과는 1인 맵 효과로 설명됨)". 카운터 KDoc: "(X, Y) 행은 X 가 Y 를 상대로 만난 결과(X 기준). (Y, X) 행과 승↔패 보완".
- [ ] **Step 5: schema.sql** — 세 테이블 블록을 새 정의로 바꾼다: `create sequence … increment by 100`, `create table … ( {id} bigint not null default nextval('{table}_seq'), … , primary key ({id}, start_battle_date), constraint ck_{table}_ordered check (brawler1_brawlstars_id < brawler2_brawlstars_id [and brawler2 < brawler3]) ) partition by range (start_battle_date)`, 카운터는 `ck_brawler_enemy_battle_result_stats_distinct_brawler check (brawler_brawlstars_id <> enemy_brawler_brawlstars_id)`, 유니크·위치 인덱스, `alter sequence … owned by`. 블록 머리 주석: "월 파티션 `{table}_p_YYYYMM` 은 partition-manage.sql 의 `create_month_partitions(current_date, 1)` 로 만든다. default 파티션 없음. 보존: `drop_old_month_partitions` 가 지난달 이전 달을 지운다". `stats_rank_battle`·`stats_result_battle` 주석의 "보관 기간 7일" 을 "보관 기간 10일(PartitionManageJobConfig.RETENTION_DAYS)" 로 고친다.
- [ ] **Step 6: 확인** — `./gradlew ktlintFormat :infrastructure:db-core:test` → PASS(core-batch·core-api 는 이 시점에 컴파일 실패해도 된다).
- [ ] **Step 7: Commit** — `feat(db-core): 조합 통계를 정렬 위치 컬럼 1행으로 바꾸고 3인 조합 테이블을 더한다`

### Task 4: 마이그레이션 SQL 과 PostgreSQL 검증

**Files:**
- Create: `imstargg-backend/infrastructure/db-core/src/main/resources/db/migration/V261104a__combo_stats_redesign.sql`, `V261104b__drop_combo_stats_legacy.sql`

**Interfaces:**
- Consumes: Task 3 의 테이블·인덱스·CHECK·시퀀스 정의(schema.sql 과 글자 그대로 같아야 한다).
- Produces: 운영 DB 에 새 테이블 3개 + 파티션(지난달·이번 달·다음 달), `_legacy` 2개, `brawler_pair_battle_rank_stats` 없음.

- [ ] **Step 1: V261104a 작성** — 두 트랜잭션(1차 DDL·rename, 2차 이관) + `analyze`. (구현 중 정정: 처음 계획은 한 트랜잭션이었으나 legacy 전체 스캔 동안 ACCESS EXCLUSIVE 를 쥐어 구 core-api 커넥션 풀을 잠식할 수 있어 나눴다. 스펙 §8.) 머리말에 V261103 과 같은 항목(배경=스펙 B 결정 1·3·6·7, 배포 순서=스펙 §3.8 1~7, 멱등 방식, 롤백, **재실행 금지**: "지난달 1일 ~ 배포일-7일 사이 date 로 `brawlerPairBattleResultStatsJob`·`brawlerEnemyBattleResultStatsJob` 을 수동 재실행하지 않는다 — 이관 버킷을 원본 10일치로 덮어쓴다"). 본문 순서:
  1. `DO $$ … IF to_regclass('brawler_pair_battle_result_stats_legacy') IS NULL AND to_regclass('brawler_pair_battle_result_stats') IS NOT NULL AND <event_brawlstars_id 컬럼이 있음> THEN rename table·index(`ix_…_2` → `ix_…_legacy_2`, `uk_…__1` → `uk_…_legacy__1`, PK 인덱스 `…_pkey` → `…_legacy_pkey` — 새 테이블의 자동 PK 이름과 충돌한다)·sequence(`…_seq` → `…_seq_legacy`) END IF` — 카운터도 같이. (컬럼 존재 검사로 "이미 새 테이블이 그 이름" 인 재실행을 건너뛴다.)
  2. `drop table if exists brawler_pair_battle_rank_stats; drop sequence if exists brawler_pair_battle_rank_stats_seq;`
  3. 새 시퀀스 3개(`create sequence if not exists … increment by 100`), 새 테이블 3개(`if not exists`, Task 3 schema.sql 블록과 동일), `DO` 블록으로 `date_trunc('month', current_date) + (-1|0|+1) month` 의 파티션 `{table}_p_YYYYMM` 을 `if not exists` 로 만든다.
  4. 이관(둘 다 `on conflict do nothing` — 유니크에 파티션 키가 들어 있어 파티션 테이블에서도 동작):

```sql
insert into brawler_pair_battle_result_stats
  (tier_range, start_battle_date, end_battle_date, brawler1_brawlstars_id, brawler2_brawlstars_id,
   victory_count, defeat_count, draw_count, total_count)
select distinct on (tier_range, start_battle_date, end_battle_date, b1, b2)
       tier_range, start_battle_date, end_battle_date, b1, b2, v, d, dr, t
from (
  select tier_range, start_battle_date, end_battle_date,
         least(brawler_brawlstars_id, pair_brawler_brawlstars_id) b1,
         greatest(brawler_brawlstars_id, pair_brawler_brawlstars_id) b2,
         (brawler_brawlstars_id < pair_brawler_brawlstars_id) forward,
         sum(victory_count) v, sum(defeat_count) d, sum(draw_count) dr, sum(total_count) t
  from brawler_pair_battle_result_stats_legacy
  where start_battle_date >= date_trunc('month', current_date) - interval '1 month'
  group by 1, 2, 3, 4, 5, 6
) g
order by tier_range, start_battle_date, end_battle_date, b1, b2, t asc, forward desc
on conflict do nothing;
```

  카운터는 `(tier_range, start_battle_date, end_battle_date, brawler_brawlstars_id, enemy_brawler_brawlstars_id)` 로 `group by` 하여 합만 넣는다.
  5. 검증 쿼리(주석): 기본 티어(`TROPHY_1000_PLUS`,`SOLO_RANK_MYTHIC_PLUS`)에서 legacy `brawler < pair` 방향 합산 행 수·Σ total 과 새 테이블 행 수·Σ total 이 같은지; 카운터는 전 티어 Σ total 이 같은지; 파티션 3개 존재.
- [ ] **Step 2: V261104b 작성** — 머리말 "V261104a 배포 뒤 §3.8 6번 확인이 끝난 다음 적용. 이후 롤백 불가". `drop table if exists brawler_pair_battle_result_stats_legacy; drop table if exists brawler_enemy_battle_result_stats_legacy;`(시퀀스는 owned by 로 함께 떨어진다).
- [ ] **Step 3: docker 검증** — `docker run -d --rm --name spec-b-pg -e POSTGRES_PASSWORD=pg -p 55432:5432 postgres:18-alpine`. 순서: (a) `git show origin/develop:imstargg-backend/infrastructure/db-core/src/main/resources/schema.sql` 에서 pair·enemy·pair_rank 세 블록을 뽑아 적용(legacy 모양); (b) 표본 insert — 기본 티어 (10,20)·(20,10) 두 방향 같은 값 2이벤트, 상위 티어 `TROPHY_2000_PLUS` 는 (10,20) t=5 와 (20,10) t=3 → 3 이 선택돼야 함, 한 방향만 있는 (30,40), 버킷 `2026-10-26~10-31` 과 `2026-11-01` 둘 다, 지난달 1일 이전 버킷 1행(이관 제외), 카운터 (10→20) 2이벤트; (c) `psql -v ON_ERROR_STOP=1 -f V261104a` **두 번**; (d) 확인: 새 pair 행 수, 2000+ (10,20) total = 3, 이전 버킷 없음, 카운터 합 일치, `\d+ brawler_pair_battle_result_stats` 에 파티션 3개·유니크·위치 인덱스·CHECK, `brawler_pair_battle_rank_stats` 없음, `_legacy` 2개; (e) `partition-manage.sql` 은 Task 7 에서 본다; (f) `V261104b` 적용 후 `_legacy` 없음; `docker stop spec-b-pg`. 결과(행 수·선택된 total)를 보고서에 적는다.
- [ ] **Step 4: Commit** — `feat(db-core): 조합 통계 재구성 마이그레이션 V261104a·V261104b 를 더한다`

### Task 5: core-batch — 2인·카운터 집계를 버킷 단위로 바꾼다

**Files:**
- Modify: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/domain/statistics/BrawlerPairBattleStatsKey.kt`, `BrawlerEnemyBattleStatsKey.kt`, `BrawlerPairBattleResultStatsAggregator.kt`, `BrawlerEnemyBattleResultStatsAggregator.kt`, `BattleStatsAggregator.kt`(KDoc "집계 키 차원" 에서 이벤트 제거)
- Modify: `…/batch/job/statistics/pairbattleresult/BrawlerPairBattleResultStatsReader.kt`, `…Writer.kt`, `…Task.kt`; `…/enemybattleresult/BrawlerEnemyBattleResultStatsReader.kt`, `…Writer.kt`, `…Task.kt`
- Test: `…/test/…/domain/statistics/BrawlerPairBattleResultStatsAggregatorTest.kt`(재작성), Create `BrawlerEnemyBattleResultStatsAggregatorTest.kt`, Modify `…/job/statistics/pairbattleresult/BrawlerPairBattleResultStatsTaskTest.kt`, `…/enemybattleresult/BrawlerEnemyBattleResultStatsTaskTest.kt`, Create `…/job/statistics/pairbattleresult/BrawlerPairBattleResultStatsWriterTest.kt`(`CoreBatchContextTest` 상속)

**Interfaces:**
- Consumes: Task 3 엔티티 생성자.
- Produces: `data class BrawlerPairBattleStatsKey(brawler1BrawlStarsId: Long, brawler2BrawlStarsId: Long, tierRange: StatsTierRange)`, `BrawlerEnemyBattleStatsKey(brawlerBrawlStarsId, enemyBrawlerBrawlStarsId, tierRange)`. Reader `fun read(weekRange: WeekRange, consume: (List<StatsResultBattleEntity>) -> Unit): Long`(`readDistinctEventIds` 삭제, WHERE `battleDate between` + `id > lastId`, 5000건 keyset). Writer `fun write(weekRange: WeekRange, aggregator: …Aggregator): Long`(DELETE `startBattleDate = start AND endBattleDate = end`, `flushEntityChunks(BATCH_SIZE)`). Task `fun aggregate(weekRanges: List<WeekRange>)` 는 버킷마다 집계기 하나 → `reader.read` → `writer.write`.
- 2인 집계기 규칙: 팀의 모든 `i < j` 쌍, 키는 id 오름차순, `require(id_i != id_j)`, `tierRanges = StatsTierRange.resolveAll(battleType, minOf(trophies_i, trophies_j))`, 아군 팀 결과 그대로·적 팀 `opposite()`, 팀원 2명 미만은 건너뜀. 카운터 집계기는 이벤트 필드만 빼고 그대로.

- [ ] **Step 1: 집계기 테스트** — `BrawlerPairBattleResultStatsAggregatorTest` 재작성(기존 헬퍼 `players()`·`resultBattle()` 유지):
  - `3명 팀은 정렬 쌍 3개를 만든다`: 브롤러 (3,1,2) → `(1,2),(1,3),(2,3)` 각 1행, 방향 중복 없음.
  - `티어는 두 멤버 trophies 의 최솟값으로 정한다`: (1: 3000, 2: 1000, 3: 2500) → (1,2)·(2,3) 은 `TROPHY_1000_PLUS` 만, (1,3) 은 1000·2000 두 티어.
  - `적군 팀은 반대 결과로 집계한다`(기존 유지, 필드 이름만 변경).
  - 팀 안 브롤러 중복은 `StatsResultBattleEntity`(`BaseStatsBattleEntity.init`)가 이미 막으므로 집계기 테스트로는 닿지 않는다. 집계기의 `require` 는 방어선으로만 두고 테스트하지 않는다.
  `BrawlerEnemyBattleResultStatsAggregatorTest` 신규: `내 팀 (1) vs 적 (2): 승리 → (1,2) 승1, (2,1) 패1`, `키에 이벤트가 없어 다른 이벤트 전투가 같은 행에 합산된다`(eventBrawlStarsId 1·2 두 전투 → (1,2) v=2).
- [ ] **Step 2: Task·Writer 테스트** — Task 테스트 두 개를 `reader.read(eq(weekRange), capture(consumeSlot))`·`writer.write(eq(weekRange), capture(aggregatorSlot))` 로 바꾸고 `readDistinctEventIds` 검증을 지운다. `BrawlerPairBattleResultStatsWriterTest : CoreBatchContextTest()`(주입: `sut: BrawlerPairBattleResultStatsWriter`, `repository: BrawlerPairBattleResultStatsJpaRepository`, `cleanUp: CleanUp`; `@AfterEach cleanUp.all()`):
  - `버킷의 기존 행을 지우고 집계 결과를 넣되 다른 버킷은 건드리지 않는다`: 버킷 A(10-26~10-31) 에 (10,20) 1행, 버킷 B(11-01~11-01) 에 (10,20) 1행 저장 → 집계기(A, 전투 1건: (1,2,3) vs (4,5,6)) 로 `write(A, agg)` → A 는 6행(두 팀 각 3쌍)이고 (10,20) A 행은 없으며 B 행은 그대로 1행.
- [ ] **Step 3: 실패 확인** — `./gradlew :core:core-batch:test` → 컴파일 FAIL.
- [ ] **Step 4: 구현** — Interfaces 대로. Task 로그는 "주간 범위 완료 - weekRange, 전투 N건, 집계 M건" 꼴 유지. 집계기 KDoc 에 티어 규칙(멤버 최저)과 이벤트 키 제거 이유를 한 줄 적는다.
- [ ] **Step 5: 확인** — `./gradlew ktlintFormat :core:core-batch:test` → PASS(core-api 컴파일 실패는 허용).
- [ ] **Step 6: Commit** — `feat(core-batch): 2인 조합·카운터 집계를 이벤트 없는 주 버킷 단위로 바꾼다`

### Task 6: core-batch — 3인 조합 집계 잡과 CronJob

**Files:**
- Create: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/domain/statistics/BrawlerTrioBattleStatsKey.kt`, `BrawlerTrioBattleResultStatsAggregator.kt`
- Create: `…/batch/job/statistics/triobattleresult/BrawlerTrioBattleResultStatsJobConfig.kt`, `…Reader.kt`, `…Writer.kt`, `…Task.kt`
- Modify: `imstargg-deploy/apps/prod/core-batch/values.yaml`
- Test: `…/test/…/domain/statistics/BrawlerTrioBattleResultStatsAggregatorTest.kt`, `…/test/…/job/statistics/triobattleresult/BrawlerTrioBattleResultStatsTaskTest.kt`

**Interfaces:**
- Consumes: Task 3 `BrawlerTrioBattleResultStatsEntity`, Task 5 의 Reader/Writer/Task 모양.
- Produces: `data class BrawlerTrioBattleStatsKey(brawler1BrawlStarsId, brawler2BrawlStarsId, brawler3BrawlStarsId, tierRange)`; `class BrawlerTrioBattleResultStatsAggregator(startBattleDate, endBattleDate) : BattleStatsAggregator<StatsResultBattleEntity, BrawlerTrioBattleResultStatsEntity>` — 팀 인원이 3이 아니면 그 팀은 건너뜀(허용 목록 이전에 수집된 행 대비), `require` 로 팀 안 브롤러 중복 금지, 키는 id 오름차순, 티어는 세 멤버 trophies 최솟값, 아군 그대로·적 `opposite()`. JobConfig 상수 `JOB_NAME = "brawlerTrioBattleResultStatsJob"`, `DATE_PARAMETER_NAME = "brawlerTrioBattleResultStatsJobDateJobParameter"`, step `brawlerTrioBattleResultStatsStep`; 나머지는 pair JobConfig 와 같은 꼴(`StatsWeekRange.currentAndPrevious(date)`).

- [ ] **Step 1: 테스트** — 집계기: `3명 팀은 정렬 키 1행을 만든다`((3,1,2) → (1,2,3)), `티어는 세 멤버 trophies 최솟값`((3000,1000,2500) → 1000 티어만), `적군 팀은 반대 결과`, `인원이 3이 아닌 팀은 건너뛴다`(2명 팀 → 0행). 중복 브롤러는 엔티티 `init` 이 막으므로(Task 5 와 같은 이유) 테스트하지 않는다. Task 테스트는 Task 5 의 pair Task 테스트와 같은 꼴(주간 범위 2개 → read·write 각 2회).
- [ ] **Step 2: 실패 확인** — `./gradlew :core:core-batch:test --tests "*Trio*"` → 컴파일 FAIL.
- [ ] **Step 3: 구현** — Interfaces 대로. `values.yaml` 의 `enemy-battle-result-stats` 뒤에 추가:

```yaml
  - name: trio-battle-result-stats
    schedule: "0 20 * * *" # 05:00 KST
    args:
      - "java -jar /app/application.jar --job.name=brawlerTrioBattleResultStatsJob date=$(date -u -d '15 hours ago' +%Y-%m-%d)"
```

- [ ] **Step 4: 확인** — `./gradlew ktlintFormat :core:core-batch:test` → PASS.
- [ ] **Step 5: Commit** — `feat(core-batch): 3인 조합 결과 통계 잡과 CronJob 을 더한다`

### Task 7: `partitionManageJob` 월 파티션 스텝

**Files:**
- Modify: `imstargg-backend/infrastructure/db-core/src/main/resources/partition-manage.sql`, `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/PartitionManageJobConfig.kt`
- Test: `imstargg-backend/core/core-batch/src/test/kotlin/com/imstargg/core/batch/job/MonthPartitionRetentionTest.kt`

**Interfaces:**
- Produces SQL: `create_month_partitions(base_date date, months_ahead int)`(세 조합 테이블에 `date_trunc('month', base_date)` 부터 `months_ahead` 달 뒤까지 `{table}_p_YYYYMM` 을 `IF NOT EXISTS` 로 생성, default 파티션 처리 없음), `drop_old_month_partitions(base_date date)`(부모가 세 테이블이고 이름이 `_p_\d{6}$` 이며 `to_date(…, 'YYYYMM') < date_trunc('month', base_date) - interval '1 month'` 인 파티션을 DETACH 후 DROP). 머리말 사용법에 두 함수를 더한다.
- Produces Kotlin: `PartitionManageJobConfig` 에 `const val MONTHS_AHEAD = 1`, `fun monthRetentionCutoff(date: LocalDate): LocalDate = date.withDayOfMonth(1).minusMonths(1)`(SQL 과 같은 경계를 Kotlin 에서 한 번 더 적어 테스트·로그에 쓴다), 스텝 `monthPartitionCreateStep`(`SELECT create_month_partitions(?, ?)`)·`monthPartitionDropStep`(`SELECT drop_old_month_partitions(?)`), 잡 흐름 `partitionCreateStep → partitionDropStep → monthPartitionCreateStep → monthPartitionDropStep`.

- [ ] **Step 1: 테스트** — `MonthPartitionRetentionTest`(`PartitionRetentionLookbackTest` 와 같은 순수 테스트):

```kotlin
@Test
fun `11월 1일 실행의 경계는 10월 1일이다 - 9월 파티션은 지우고 10월은 남긴다`() {
    assertThat(PartitionManageJobConfig.monthRetentionCutoff(LocalDate.of(2026, 11, 1)))
        .isEqualTo(LocalDate.of(2026, 10, 1))
}

@Test
fun `보존 경계는 API 최대 조회 범위(SINCE_LAST_MONTH)의 시작일을 넘지 않는다`() {
    val start = LocalDate.of(2026, 1, 1)
    generateSequence(start) { it.plusDays(1) }.takeWhile { it <= start.plusYears(2) }.forEach { date ->
        val clock = Clock.fixed(date.atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant(), ZoneId.of("Asia/Seoul"))
        val (apiStart, _) = StatsDateRange.SINCE_LAST_MONTH.dateRange(clock)
        assertThat(PartitionManageJobConfig.monthRetentionCutoff(date)).isBeforeOrEqualTo(apiStart)
    }
}
```

- [ ] **Step 2: 실패 확인** — `./gradlew :core:core-batch:test --tests "*MonthPartitionRetentionTest"` → 컴파일 FAIL.
- [ ] **Step 3: 구현** — Interfaces 대로. `RETENTION_DAYS` KDoc 옆에 월 파티션 보존 KDoc("조합 통계 3종. API `SINCE_LAST_MONTH` 와 같은 경계 — 가드: MonthPartitionRetentionTest").
- [ ] **Step 4: docker 확인** — Task 4 의 컨테이너를 다시 띄워 `V261104a` 적용 후 `partition-manage.sql` 적용, `select create_month_partitions('2026-11-01', 1)` 두 번(멱등), `select drop_old_month_partitions('2026-11-01')` → `_p_202609` 는 없고 `_p_202610`·`_p_202611`·`_p_202612` 가 있다. 2026-09 파티션은 먼저 수동으로 만들어 두고 본다. 원본 함수 `create_partitions`·`drop_old_partitions` 도 여전히 동작한다. 결과를 보고서에 적는다.
- [ ] **Step 5: 확인** — `./gradlew ktlintFormat :core:core-batch:test` → PASS.
- [ ] **Step 6: Commit** — `feat(core-batch): 조합 통계 월 파티션 생성·삭제 스텝을 partitionManageJob 에 더한다`

### Task 8: core-api — 2인·카운터 API 재구성

**Files:**
- Create: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/domain/statistics/ComboStatsPolicy.kt`
- Modify: `…/domain/statistics/StatsRate.kt`, `BrawlerPairResultCount.kt`, `BrawlerPairResultStatistics.kt`, `BrawlerEnemyResultStatistics.kt`
- Modify: `…/application/statistics/BattleResultStatisticsRepository.kt`, `BattleResultStatisticsRepositoryWithCache.kt`, `BattleResultStatisticsService.kt`, `…/api/query/StatisticsQueryComposer.kt`, `…/api/controller/v1/StatisticsController.kt`, `…/controller/v1/response/BrawlerPairResultStatisticsItemResponse.kt`, `BrawlerEnemyResultStatisticsItemResponse.kt`
- Modify(db-core): `imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/repository/BrawlerBattleResultStatsJpaRepositoryCustom.kt`, `…CustomImpl.kt`(`findEventTotalSlotSums` 삭제); Delete: `…/repository/EventSlotSumProjection.kt`
- Test: `…/test/…/domain/statistics/BrawlerPairResultStatisticsTest.kt`, `BrawlerEnemyResultStatisticsTest.kt`, `StatsRateTest.kt`, `…/application/statistics/BattleResultStatisticsRepositoryTest.kt`, `StatisticsCacheKeyTest.kt`, `…/api/controller/v1/StatisticsControllerTest.kt`, `…/config/CacheConfigSerializerTest.kt`

**Interfaces:**
- Consumes: Task 2 `TierScore`, Task 3 Custom 메서드.
- Produces:
  - `object ComboStatsPolicy { const val BRAWLER_MIN_BATTLE_COUNT = 30L; const val BRAWLER_TOP_N = 200; const val RANKING_MIN_BATTLE_COUNT = 100L; const val RANKING_TOP_N = 50 }` — KDoc 에 근거(스펙 부록 B.1: 상위 10 중 30판 미만 56~86%, 3인 응답 상한 없으면 Redis 항목 ~1.3MB).
  - `StatsRate`: `const val PAIRS_PER_TEAM = 3`(3명 팀의 쌍 수), `const val ENEMY_SLOTS_PER_GAME = 3`(한 게임에서 X 가 만나는 적 수), `fun pairTeamCount(pairSlotSum: Long): Long = pairSlotSum / PAIRS_PER_TEAM`, `fun enemyGameCount(enemySlotSum: Long): Long = enemySlotSum / ENEMY_SLOTS_PER_GAME`. `pickRate` KDoc 을 "조합 픽률 분모 = 조합 테이블 자체의 합(2026-11 스펙 B 결정 12) — 1인 테이블 의존을 끊었다. 카운터 픽률 = X 의 게임 중 Y 를 만난 비율" 로 갱신.
  - `data class BrawlerPairResultCount(brawler1Id: BrawlStarsId, brawler2Id: BrawlStarsId, resultCount: ResultCount)`.
  - `data class BrawlerPairResultStatisticsItem(brawlerId, pairBrawlerId, battleCount: Long, pickRate: Double, winRate: Double, tierScore: TierScore)`; `data class BrawlerPairResultStatistics(val items: List<…Item>) { companion fun create(brawlerId: BrawlStarsId, totalTeamCount: Long, counts: List<BrawlerPairResultCount>) }` — `pairBrawlerId` 는 X 가 아닌 쪽, `pickRate = StatsRate.pickRate(count.totalCount, totalTeamCount)`, `TierScore.create(counts.associate { (it.brawler1Id to it.brawler2Id) to it.resultCount }, pickRateWeight = false)`, `battleCount >= BRAWLER_MIN_BATTLE_COUNT` 만, `tierScore.value` 내림차순, `take(BRAWLER_TOP_N)`.
  - `data class BrawlerEnemyResultStatisticsItem(brawlerId, enemyBrawlerId, battleCount, pickRate, winRate)`(KDoc: "winRate 는 X 가 Y 를 만났을 때 X 의 승률"); `BrawlerEnemyResultStatistics(val items) { create(counts: List<BrawlerEnemyResultCount>) }` — 분모 `StatsRate.enemyGameCount(counts.sumOf { it.resultCount.totalCount })`, 하한 30, `winRate` 오름차순, 상위 200.
  - 저장소: `findBrawlerPairResultStatistics(brawlerId, tierRange, startDate, endDate)`(`findAllByBrawler` + `StatsRate.pairTeamCount(sumTotalCount(...))`), `findBrawlerEnemyResultStatistics(brawlerId, tierRange, startDate, endDate)`. 1인 테이블 분모 경로는 통째로 지운다: `BattleResultStatisticsRepository.computeTotalGameCount`, db-core `BrawlerBattleResultStatsJpaRepositoryCustom.findEventTotalSlotSums`(+Impl), `EventSlotSumProjection.kt` — 호출처가 조합·카운터뿐이다. `StatsRate.resultGameCount` KDoc 의 "Pair/Enemy 통계의 분모도 Result 슬롯 합 기준" 문장을 "2026-11 스펙 B 부터 조합·카운터 분모는 각 조합 테이블의 합(결정 12)" 로 바꾼다.
  - 캐시 메서드 시그니처·키: Global Constraints 의 `v3:` 두 개. 서비스·컴포저·컨트롤러에서 `eventId`·`pairBrawlerId` 를 뺀다(`GET /v1/statistics/brawler/pair/result?brawlerId&tierRange&dateRange`, `enemy/result` 같음).
  - 응답: `BrawlerPairResultStatisticsItemResponse(brawler, pairBrawler, battleCount, pickRate, winRate, tierScore: Int, tier: String)`, `BrawlerEnemyResultStatisticsItemResponse(brawler, enemyBrawler, battleCount, pickRate, winRate)`. `totalBattleCount` getter 둘 다 삭제.

- [ ] **Step 1: 도메인 테스트** — `BrawlerPairResultStatisticsTest` 재작성:
  - `X 가 brawler2 이면 pairBrawlerId 는 brawler1 이다`: counts (5,10) → X=10 → `pairBrawlerId == 5`.
  - `픽률 분모는 팀 수다`: `create(X, totalTeamCount = 100, (10,20) 승30 패10)` → battleCount 40, pickRate 0.4.
  - `battleCount 30 미만은 빠진다`: 총 29 → items 비어 있음.
  - `tierScore 내림차순이고 상위 200 개만 남는다`: 201개 조합(각 총 30, 승률 서로 다름) → 200개, 첫 항목이 가장 높은 승률.
  - `totalTeamCount 0 이면 pickRate 0`.
  `BrawlerEnemyResultStatisticsTest` 재작성: `픽률 분모는 X 의 게임 수(Σ total ÷ 3)` — 상대 20·30·40·50·60 각 총 30 → Σ 150 → X 게임 50 → 모든 행 pickRate 0.6; `winRate 오름차순`; `30 미만 제외`.
  `StatsRateTest` 에 `pairTeamCount(10) == 3`, `enemyGameCount(8) == 2`.
- [ ] **Step 2: 저장소·컨트롤러·캐시 키 테스트** — `BattleResultStatisticsRepositoryTest` 의 pair/enemy 테스트를 새 엔티티로 다시 쓴다: `findBrawlerPairResultStatistics - 분모는 조합 테이블 전체 합 ÷ 3` ((10,20)·(30,40)·(50,60)·(70,80) 각 총 30 → Σ 120 → 팀 40 → X=10 의 (10,20) pickRate 0.75; X 행만 분모로 쓰면 1.0 이 되어 구별된다), `X 가 brawler2 인 조합도 돌려준다`. `StatisticsControllerTest`: `pair result 에 brawlerId 가 없으면 400` 유지, legacy sort 테스트 유지(eventId 도 이제 무시된다는 테스트로 `param("eventId", "1")` 를 더해 200 확인). `StatisticsCacheKeyTest` 스냅샷을 `v3:` 두 키로 갱신. `CacheConfigSerializerTest` 에 기존 라운드트립 케이스와 같은 꼴로 `BrawlerPairResultStatistics 직렬화 라운드트립`(items 1개, `tierScore = TierScore(52)`)·`BrawlerEnemyResultStatistics 직렬화 라운드트립` 을 더한다 — 캐시에 들어가는 도메인 모양이 바뀌었다.
- [ ] **Step 3: 실패 확인** — `./gradlew :core:core-api:test` → 컴파일 FAIL.
- [ ] **Step 4: 구현** — Interfaces 대로.
- [ ] **Step 5: 확인** — `./gradlew ktlintFormat build` → BUILD SUCCESSFUL(여기서 전체가 다시 녹색).
- [ ] **Step 6: Commit** — `feat(core-api): 2인 조합·카운터 API 에서 이벤트 키를 빼고 하한·티어 스코어·새 분모를 적용한다`

### Task 9: core-api — 3인 조합과 전체 조합 정렬 API

**Files:**
- Create: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/domain/statistics/BrawlerTrioResultCount.kt`, `BrawlerTrioResultStatistics.kt`, `ComboResultRankingItem.kt`, `PairResultRanking.kt`, `TrioResultRanking.kt`
- Create: `…/api/controller/v1/response/BrawlerTrioResultStatisticsItemResponse.kt`, `ComboResultRankingItemResponse.kt`
- Modify: `…/application/statistics/BattleResultStatisticsRepository.kt`(`BrawlerTrioBattleResultStatsJpaRepository` 주입), `BattleResultStatisticsRepositoryWithCache.kt`, `BattleResultStatisticsService.kt`, `…/api/query/StatisticsQueryComposer.kt`, `…/api/controller/v1/StatisticsController.kt`
- Test: Create `…/test/…/domain/statistics/BrawlerTrioResultStatisticsTest.kt`, `PairResultRankingTest.kt`, `TrioResultRankingTest.kt`; Modify `BattleResultStatisticsRepositoryTest.kt`, `StatisticsCacheKeyTest.kt`, `StatisticsControllerTest.kt`, `…/config/CacheConfigSerializerTest.kt`

**Interfaces:**
- Consumes: Task 3 trio Custom·`findAllHavingTotalCountAtLeast`·`sumTotalCount`, Task 8 `ComboStatsPolicy`·`StatsRate.pairTeamCount`.
- Produces:
  - `data class BrawlerTrioResultCount(brawler1Id, brawler2Id, brawler3Id, resultCount)`.
  - `data class BrawlerTrioResultStatisticsItem(brawlerId, pairBrawlerIds: List<BrawlStarsId>, battleCount, pickRate, winRate, tierScore)`; `BrawlerTrioResultStatistics(val items) { create(brawlerId, totalTeamCount, counts: List<BrawlerTrioResultCount>) }` — 규칙은 2인과 같되 분모는 `totalTeamCount` 그대로, `pairBrawlerIds` 는 X 를 뺀 두 id(오름차순).
  - `data class ComboResultRankingItem(brawlerIds: List<BrawlStarsId>, battleCount, pickRate, winRate, tierScore)`; `PairResultRanking(val items) { create(totalTeamCount, counts: List<BrawlerPairResultCount>) }`, `TrioResultRanking(val items) { create(totalTeamCount, counts: List<BrawlerTrioResultCount>) }` — 하한 `RANKING_MIN_BATTLE_COUNT`, `tierScore` 내림차순, `take(RANKING_TOP_N)`, `pickRateWeight = false`.
  - 저장소: `findBrawlerTrioResultStatistics(brawlerId, tierRange, startDate, endDate)`(`trio.findAllByBrawler` + `trio.sumTotalCount`), `findPairResultRanking(tierRange, startDate, endDate)`(`pair.findAllHavingTotalCountAtLeast(…, ComboStatsPolicy.RANKING_MIN_BATTLE_COUNT)` + `StatsRate.pairTeamCount(pair.sumTotalCount(...))`), `findTrioResultRanking(tierRange, startDate, endDate)`.
  - 캐시 메서드 3개와 키: Global Constraints 의 `v1:` 세 개. 서비스 `getBrawlerTrioResultStatistics(brawlerId, tierRange, dateRange)`, `getPairResultRanking(tierRange, dateRange)`, `getTrioResultRanking(tierRange, dateRange)`; 컴포저 같은 이름으로 `List<…Response>`.
  - 컨트롤러: `GET /v1/statistics/brawler/trio/result (brawlerId, tierRange, dateRange)`, `GET /v1/statistics/pair/result (tierRange, dateRange)`, `GET /v1/statistics/trio/result (tierRange, dateRange)` — 모두 `ListResponse`.
  - 응답: `BrawlerTrioResultStatisticsItemResponse(brawler: BrawlerResponse?, pairBrawlers: List<BrawlerResponse?>, battleCount, pickRate, winRate, tierScore: Int, tier: String)`, `ComboResultRankingItemResponse(brawlers: List<BrawlerResponse?>, battleCount, pickRate, winRate, tierScore, tier)`.

- [ ] **Step 1: 도메인 테스트** — `BrawlerTrioResultStatisticsTest`: `X 가 brawler3 이면 pairBrawlerIds 는 [brawler1, brawler2]`, `픽률 분모는 팀 수 그대로`(총 40, totalTeamCount 100 → 0.4), `30 미만 제외`. `PairResultRankingTest`: `100 미만 제외`, `tierScore 내림차순 상위 50`(51개 → 50), `brawlerIds 는 [b1, b2]`. `TrioResultRankingTest`: `counts 가 비면 items 가 비고 예외가 없다`(`create(0, emptyList())`), `brawlerIds 는 [b1, b2, b3]`.
- [ ] **Step 2: 저장소·컨트롤러·캐시 키 테스트** — `BattleResultStatisticsRepositoryTest` 추가: `findBrawlerTrioResultStatistics - X 가 b1·b2·b3 어느 위치든 조회된다`(세 조합 각 총 30 → 3 items), `findPairResultRanking - 하한 미만 조합은 빠지지만 분모에는 들어간다`((10,20) 100, (30,40) 3, (50,60) 97, (70,80) 100 → Σ 300 → 팀 100 → items 는 (10,20)·(70,80) 두 개, (10,20) pickRate 1.0; HAVING 뒤 합으로 나누면 Σ 200 → 팀 66 → 1.5 가 되어 구별된다), `findTrioResultRanking - 데이터가 없으면 빈 목록`. `StatisticsControllerTest`: 세 엔드포인트 `tierRange` 없으면 400, 있으면 200. `StatisticsCacheKeyTest` 스냅샷에 세 키 추가. `CacheConfigSerializerTest` 에 `PairResultRanking 직렬화 라운드트립`·`BrawlerTrioResultStatistics 직렬화 라운드트립` 추가 — `List<BrawlStarsId>`(value class 목록) 필드가 Redis 직렬화를 처음 지나므로 `deserialize(serialize(x)) == x` 를 고정한다.
- [ ] **Step 3: 실패 확인** — `./gradlew :core:core-api:test` → 컴파일 FAIL.
- [ ] **Step 4: 구현** — Interfaces 대로. 전체 정렬 KDoc: "정렬·상위 N 은 DB 가 아니라 도메인에서 한다 — 티어 스코어가 기간 전체 평균 승률을 필요로 한다(스펙 §3.5)".
- [ ] **Step 5: 확인** — `./gradlew ktlintFormat build` → BUILD SUCCESSFUL.
- [ ] **Step 6: Commit** — `feat(core-api): 3인 조합 통계와 전체 2인·3인 조합 정렬 API 를 더한다`

### Task 10: frontend — 이벤트 상세의 선택 패널·pairRank 제거

**Files:**
- Delete: `imstargg-frontend/app/events/[id]/_components/EventSelectedBrawlerPanel.tsx`, `PairRankTable.tsx`
- Modify: `imstargg-frontend/app/events/[id]/_components/EventStatsPanel.tsx`, `EventResultStatsTable.tsx`, `EventRankStatsTable.tsx`, `imstargg-frontend/src/hooks/queries/useEventStats.ts`(`useEventSelectedStats` 삭제), `src/hooks/queries/queryKeys.ts`(`pairRank` 삭제), `src/api/endpoints/statistics.ts`(`PairRankParams`·`pairRank` 삭제), `src/api/types.ts`(`BrawlerPairRankStatisticsResponse` 삭제), `src/domain/statistics/types.ts`(`PairRankStat` 삭제), `src/domain/statistics/mappers.ts`(`toPairRankStat` 삭제), `src/domain/statistics/sort.ts`(`RANK_SORT_BYS`·`RankStatsSortBy`·`RANK_SORT_LABELS`·`parseRankSortBy`·`sortRankStats` 삭제 — PairRankTable 만 썼다)
- Test: `tests/components/EventStatsPanel.test.tsx`, `tests/unit/domain/statistics/mappers.test.ts`(toPairRankStat 블록 삭제), `tests/unit/domain/statistics/sort.test.ts`(parseRankSortBy·sortRankStats 블록 삭제), `tests/e2e/statistics.spec.ts`(`/events/[id] 통계 행 클릭 시 brawlerId URL + 선택 패널` 삭제)

**Interfaces:**
- Produces: `EventResultStatsTable({ items })`·`EventRankStatsTable({ items })` — `selectedBrawlerId`/`onToggleBrawler` prop 삭제, 행은 `<Link href={`/brawlers/${id}`}>`(`BrawlerStatRow` 와 같은 꼴, `aria-pressed` 없음), 브롤러가 null 인 행은 `<div>` 로 그대로 표시. `EventStatsPanel` 은 `brawlerId` URL 파라미터를 읽지도 쓰지도 않는다.

- [ ] **Step 1: 테스트** — `EventStatsPanel.test.tsx` 의 "브롤러 행 클릭 → 선택 패널" 테스트를 지우고 추가: `통계 행은 브롤러 상세 링크다` — `screen.findByRole("link", { name: /데미안/ })` 의 `href` 가 `/brawlers/16000104`, `screen.queryByRole("button", { pressed: false })` 가 없음. 매퍼·정렬 테스트에서 pairRank 블록을 지운다. e2e 테스트 하나를 지운다.
- [ ] **Step 2: 실패 확인** — `pnpm test:run tests/components/EventStatsPanel.test.tsx` → FAIL(링크 없음).
- [ ] **Step 3: 구현** — Files 대로 삭제·수정.
- [ ] **Step 4: 확인** — `pnpm typecheck && pnpm lint && pnpm test:run` → PASS. `grep -rn "pairRank\|PairRank\|useEventSelectedStats\|EventSelectedBrawlerPanel" app src tests` → 없음.
- [ ] **Step 5: Commit** — `refactor(frontend): 이벤트 상세의 선택 브롤러 패널과 쇼다운 조합 순위를 지운다`

### Task 11: frontend — API 클라이언트·도메인·훅(3인·전체 정렬·티어 필드)

**Files:**
- Modify: `imstargg-frontend/src/api/types.ts`, `src/api/endpoints/statistics.ts`, `src/domain/statistics/types.ts`, `src/domain/statistics/mappers.ts`, `src/domain/statistics/sort.ts`, `src/domain/statistics/enums.ts`, `src/hooks/queries/queryKeys.ts`, `src/hooks/queries/useBrawlerStats.ts`, `app/brawlers/[id]/page.tsx`(`statisticsApi.trioResult(statsParams, opts)` 를 `Promise.all` 에 더해 `initial.trio` 로 넘긴다 — `trio` 가 필수 필드라 같은 작업에서 고쳐야 typecheck 가 통과한다)
- Test: `tests/unit/domain/statistics/mappers.test.ts`, `sort.test.ts`, `enums.test.ts`, `tests/components/BrawlerStatsPanel.test.tsx`(fixture 에 `tierScore`·`tier`·`trio: []` 추가, 필터 변경 테스트의 `server.use` 에 `*/v1/statistics/brawler/trio/result → { content: [] }` 핸들러 추가 — `vitest.setup.ts` 가 `onUnhandledRequest: "error"` 다)

**Interfaces:**
- Produces (api/types.ts): `BrawlerPairResultStatisticsItemResponse` 에 `tierScore: number; tier: string`; `BrawlerTrioResultStatisticsItemResponse { brawler: BrawlerResponse | null; pairBrawlers: (BrawlerResponse | null)[]; battleCount; pickRate; winRate; tierScore; tier }`; `ComboResultRankingItemResponse { brawlers: (BrawlerResponse | null)[]; battleCount; pickRate; winRate; tierScore; tier }`.
- Produces (statistics.ts): `PairResultParams`·`EnemyResultParams` 에서 `eventId` 삭제; `TrioResultParams { brawlerId; tierRange; dateRange }`; `ComboRankingParams { tierRange; dateRange }`; `statisticsApi.trioResult → /v1/statistics/brawler/trio/result`, `pairRanking → /v1/statistics/pair/result`, `trioRanking → /v1/statistics/trio/result`(모두 `ListResponse<…>`).
- Produces (domain): `PairResultStat` 에 `tierScore: number; tier: StatTier`; `TrioResultStat { brawler: Brawler | null; pairBrawlers: Brawler[]; battleCount; pickRate; winRate; tierScore; tier }`; `ComboRankingStat { brawlers: Brawler[]; battleCount; pickRate; winRate; tierScore; tier }`. 매퍼 `toTrioResultStat`, `toComboRankingStat` — 배열의 null 은 버린다(표는 길이가 2/3 인 행만 그린다). `sort.ts`: `COMBO_SORT_BYS = ["TIER_SCORE", "WIN_RATE", "PICK_RATE", "BATTLE_COUNT"] as const`, `ComboStatsSortBy`, `COMBO_SORT_LABELS`(티어/승률/픽률/전투수), `sortComboStats<T extends { tierScore; winRate; pickRate; battleCount }>(items, sortBy, order)`. `enums.ts`: `BRAWLER_META_VIEWS = ["brawler", "pair", "trio"] as const`, `BrawlerMetaView`, `DEFAULT_BRAWLER_META_VIEW = "brawler"`, `BRAWLER_META_VIEW_LABELS = { brawler: "브롤러", pair: "2인 조합", trio: "3인 조합" }`, `parseBrawlerMetaView(value: unknown)`.
- Produces (hooks): `queryKeys.statistics.trioResult|pairRanking|trioRanking(params)`; `BrawlerStatsInitial { daily; pair; trio: TrioResultStat[]; enemy }`; `useBrawlerStats` 가 `{ daily, pair, trio, enemy }` 를 돌려준다.

- [ ] **Step 1: 테스트** — `mappers.test.ts`: `toPairResultStat 는 tierScore·tier 를 옮긴다`(`tier: "S"` → `"S"`), `toTrioResultStat 는 pairBrawlers 의 null 을 버린다`([fake(2), null] → 길이 1), `toComboRankingStat 는 brawlers 를 모두 매핑한다`. `sort.test.ts`: `sortComboStats TIER_SCORE DESC`, `BATTLE_COUNT ASC`. `enums.test.ts`: `parseBrawlerMetaView("TRIO") → "trio"`, `("x") → "brawler"`.
- [ ] **Step 2: 실패 확인** — `pnpm test:run tests/unit/domain/statistics` → FAIL.
- [ ] **Step 3: 구현** — Interfaces 대로. `BrawlerStatsPanel.test.tsx` fixture 를 새 타입에 맞춘다(`tierScore: 55, tier: "S"`, `trio: []`).
- [ ] **Step 4: 확인** — `pnpm typecheck && pnpm lint && pnpm test:run` → PASS.
- [ ] **Step 5: Commit** — `feat(frontend): 3인 조합·전체 조합 정렬 API 클라이언트와 도메인 타입을 더한다`

### Task 12: frontend — 브롤러 상세(3인 표·조합 티어 배지·카운터 표기)

**Files:**
- Create: `imstargg-frontend/app/brawlers/[id]/_components/TrioStatsTable.tsx`
- Modify: `app/brawlers/[id]/_components/PairStatsTable.tsx`, `EnemyStatsTable.tsx`, `BrawlerStatsPanel.tsx`
- Test: Create `tests/unit/components/statistics/PairStatsTable.test.tsx`, `TrioStatsTable.test.tsx`, `EnemyStatsTable.test.tsx`; Modify `tests/components/BrawlerStatsPanel.test.tsx`, `tests/e2e/statistics.spec.ts`

**Interfaces:**
- Consumes: Task 11 타입·`sortComboStats`·`useBrawlerStats().trio`.
- Produces: `PairStatsTable({ items: PairResultStat[]; initialSort?: ComboStatsSortBy = "TIER_SCORE"; initialOrder?: SortOrder = "DESC" })` — 열 순위 / 조합 브롤러 / 티어(`TierBadge`) / 픽률 / 승률 / 전투수, 툴바 설명 "함께 픽됐을 때의 승률 · 전투 30회 이상 조합만 보입니다". `TrioStatsTable({ items: TrioResultStat[]; … })` — 제목 "3인 조합", 조합 브롤러 칸에 아바타 2개(각각 `/brawlers/{id}` 링크), 열은 2인과 같음, `pairBrawlers.length !== 2` 인 행은 건너뜀. `EnemyStatsTable` — 설명 "이 상대를 만났을 때 내 승률 · 전투 30회 이상 상대만 보입니다", `enemyRail`: `>= 0.55 → "win"`, `<= 0.45 → "loss"`, 그 외 `"neutral"`, 기본 정렬 `WIN_RATE` `ASC`. `BrawlerStatsPanel` 은 기존 2열 그리드(`grid-cols-1 lg:grid-cols-2`)에 조합 → 카운터 → 3인 조합 순으로 세 표를 두고 `useBrawlerStats().trio.data` 를 `TrioStatsTable` 에 넘긴다(SSR `initial.trio` 는 Task 11 에서 이미 들어온다).

- [ ] **Step 1: 테스트** — `EnemyStatsTable.test.tsx`: `부제는 '이 상대를 만났을 때 내 승률' 이다`, `승률 0.6 행은 data-rail=win, 0.4 는 loss`, `기본 정렬은 승률 오름차순`(0.6·0.4 두 행 → 첫 행이 0.4 브롤러). `PairStatsTable.test.tsx`: `티어 배지를 그린다`(`aria-label "S 티어"`), `기본 정렬은 tierScore 내림차순`. `TrioStatsTable.test.tsx`: `조합 브롤러 2명이 각자 상세 링크다`, `pairBrawlers 가 2명이 아닌 행은 그리지 않는다`. `BrawlerStatsPanel.test.tsx`: "3인 조합" 제목이 보인다.
- [ ] **Step 2: 실패 확인** — `pnpm test:run tests/unit/components/statistics tests/components/BrawlerStatsPanel.test.tsx` → FAIL.
- [ ] **Step 3: 구현** — Interfaces 대로.
- [ ] **Step 4: e2e 갱신** — `/brawlers/[id] 상세 통계가 모두 렌더된다` 의 `heading { name: "조합" }` 을 `{ name: "조합", exact: true }` 로 바꾸고 `"3인 조합"` 헤딩 확인을 더한다. `조합 표 승률 헤더 클릭 시 정렬 방향이 토글된다`: 표를 고르는 `filter({ has: getByRole("heading", { name: "조합" }) })` 에도 `exact: true`(아니면 "3인 조합" 표까지 잡혀 승률 버튼이 둘이 된다), 초기 정렬이 티어라 첫 클릭 뒤 `▼`, 두 번째 클릭 뒤 `▲` 로 기대값을 바꾼다(주석도 함께).
- [ ] **Step 5: 확인** — `pnpm typecheck && pnpm lint && pnpm test:run` → PASS.
- [ ] **Step 6: Commit** — `feat(frontend): 브롤러 상세에 3인 조합 표를 더하고 카운터 표기를 바로잡는다`

### Task 13: frontend — 브롤러 목록 탭과 조합 정렬 표

**Files:**
- Create: `imstargg-frontend/app/brawlers/_components/ComboRankingTable.tsx`, `app/brawlers/_components/BrawlerMetaViewTabs.tsx`, `app/brawlers/_lib/loadBrawlerMetaView.ts`
- Modify: `app/brawlers/page.tsx`
- Test: Create `tests/unit/components/statistics/ComboRankingTable.test.tsx`, `tests/unit/components/statistics/BrawlerMetaViewTabs.test.tsx`, `tests/unit/app/brawlers/loadBrawlerMetaView.test.ts`

**Interfaces:**
- Consumes: Task 11 `ComboRankingStat`·`statisticsApi.pairRanking|trioRanking`·`parseBrawlerMetaView`·`BRAWLER_META_VIEW_LABELS`.
- Produces: `loadBrawlerMetaView(view: BrawlerMetaView, tierRange, dateRange): Promise<BrawlerMetaViewData>` with `type BrawlerMetaViewData = { view: "brawler"; stats: BrawlerResultStatistics; brawlers: Brawler[] } | { view: "pair" | "trio"; items: ComboRankingStat[] }` — 선택된 뷰의 엔드포인트만 부른다(`revalidate: CACHE_SECONDS.SHORT`). `BrawlerMetaViewTabs({ view })`(client): `Tabs value={view} onValueChange` → `router.replace` 로 `?view=` 를 쓰되(기본 `brawler` 면 지움) `tier`·`date` 는 보존(`StatRangeSelector` 와 같은 방식), 라벨은 `BRAWLER_META_VIEW_LABELS`. `ComboRankingTable({ items: ComboRankingStat[]; initialSort?: ComboStatsSortBy = "TIER_SCORE"; initialOrder? = "DESC" })` — 열 순위 / 조합 브롤러(아바타 2~3개, 각각 상세 링크) / 티어 / 승률 / 픽률 / 전투수, `SortableHeader`, 빈 목록 "전투 100회 이상인 조합이 없습니다". `page.tsx`: `view = parseBrawlerMetaView(sp.view)`, `loadBrawlerMetaView` 결과로 `BrawlerStatTable` 또는 `ComboRankingTable` 을 그린다. 헤더 부제 옆에 `BrawlerMetaViewTabs`.

- [ ] **Step 1: 테스트** — `loadBrawlerMetaView.test.ts`(MSW `server.use`): `view=pair 면 /v1/statistics/pair/result 만 호출하고 brawler/result 는 부르지 않는다`(호출 URL 기록), `view=brawler 면 brawler/result 와 brawler 목록을 부른다`. `BrawlerMetaViewTabs.test.tsx`(`vi.mock("next/navigation")` 은 `StatRangeSelector.test.tsx` 와 같은 꼴, `useSearchParams → "tier=TROPHY_2000_PLUS"`): `'2인 조합' 탭을 누르면 ?tier=TROPHY_2000_PLUS&view=pair 로 replace 한다`, `'브롤러' 탭은 view 를 지운다`. `ComboRankingTable.test.tsx`: `3인 조합 행은 아바타 3개와 상세 링크 3개`, `빈 목록 문구`, `승률 헤더 클릭 시 승률 내림차순`.
- [ ] **Step 2: 실패 확인** — `pnpm test:run tests/unit/components/statistics/ComboRankingTable.test.tsx tests/unit/components/statistics/BrawlerMetaViewTabs.test.tsx tests/unit/app` → FAIL.
- [ ] **Step 3: 구현** — Interfaces 대로.
- [ ] **Step 4: 확인** — `pnpm typecheck && pnpm lint && pnpm test:run` → PASS.
- [ ] **Step 5: Commit** — `feat(frontend): 브롤러 목록에 2인·3인 조합 정렬 탭을 더한다`

### Task 14: 문서·버전 확인·전체 검증

**Files:**
- Modify: `imstargg-backend/docs/stats-battle-data-strategy.md`(§4.2 보관 7일→10일, §5.2 "최근 3일 롤링" → 1인 통계는 `date` 하루·조합/카운터는 `StatsWeekRange.currentAndPrevious` 두 버킷 재집계, §5.3 흐름 그림 갱신, §6 표에 조합 통계 3종 "월 파티션, 지난달 1일 이전 DROP" 추가, §8 관련 코드에 새 엔티티·`PartitionManageJobConfig` 월 스텝, 조합 통계 절에 키·티어 규칙·이벤트 키 제거 결정 링크)
- Verify only: `imstargg-backend/gradle.properties`(core-api 0.6.0-SNAPSHOT, core-batch 0.5.0-SNAPSHOT), `imstargg-frontend/package.json`(0.8.0-SNAPSHOT)

- [ ] **Step 1: 문서 수정** — 위 항목. 문장은 실제 코드 값(RETENTION_DAYS=10, MONTHS_AHEAD=1, 잡 시각 03/04/05/07시 KST)으로 쓴다.
- [ ] **Step 2: 버전 확인** — `python3 .claude/skills/release-management/scripts/detect_changed_apps.py --base origin/develop` 와 `python3 .claude/skills/release-management/scripts/bump_version.py core-api minor --dry-run`(core-batch·frontend 도) → 모두 `changed: false`(이미 minor SNAPSHOT). 달라지면 적용하고 보고서에 적는다.
- [ ] **Step 3: 전체 검증** — `cd imstargg-backend && ./gradlew ktlintCheck build` → BUILD SUCCESSFUL; `cd imstargg-frontend && pnpm typecheck && pnpm lint && pnpm test:run` → 모두 PASS(`pnpm build` 는 운영 API 네트워크가 필요해 CI 와 같이 생략한다). `grep -rn "eventBrawlStarsId" imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/domain/statistics/Brawler{Pair,Trio,Enemy}*` → 없음.
- [ ] **Step 4: Commit** — `docs: 조합 통계 재구성에 맞춰 통계 데이터 전략 문서를 고친다`
- [ ] **Step 5: PR 본문 메모** — finishing 단계의 PR 본문에 스펙 §6 의 릴리즈 노트 항목(카운터 의미 변화, 상위 티어 빈 목록, 이관 근사, 이관 버킷 재실행 금지, DDL 2단계와 롤백 한계)과 §3.8 배포 순서 7단계를 그대로 적는다. 릴리즈 노트 파일은 `/release prepare` 때 만든다.
