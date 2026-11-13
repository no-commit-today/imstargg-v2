# 패치 기준 통계 2단계(API·화면 전환과 옛 통계 삭제) 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 통계 API 가 기간 대신 패치일을 받고 화면이 패치 선택과 "최근 업데이트"를 보이게 바꾸며, 옛 조회 코드·옛 잡·옛 엔티티·기간 옵션·일별 추이를 지운다.

**Architecture:** core-api 는 체크포인트 줄에서 "선택할 수 있는 패치"를 구해 목록으로 주고, 통계 7개는 패치일을 패치 id 로 바꿔 1단계가 채운 새 테이블에서 키별 한 줄을 그대로 읽는다. 프론트는 서버가 패치 목록과 가장 최근 패치의 통계를 같은 렌더에서 받아 내려 주고, 화면은 그 목록으로 기본 패치를 정한다. 옛 것은 같은 릴리스에서 지우고, `_backup` 테이블을 지우는 SQL 은 파일만 넣어 두었다가 전환 1주 뒤에 적용한다.

**Tech Stack:** Kotlin + Spring Boot 4.1, JPA + QueryDSL, Redis 캐시(`statistics`, 30분), MockMvc, H2. Next.js 15 + React 19, TanStack Query, vitest + MSW, Playwright.

**Spec:** [docs/superpowers/specs/2026-11-13-patch-based-statistics-design.md](../specs/2026-11-13-patch-based-statistics-design.md). 이 계획이 스펙과 어긋나면 스펙이 이긴다. 이 문서는 스펙 10장의 2단계와 3단계다. 1단계는 [2026-11-13-patch-based-statistics-stage1.md](2026-11-13-patch-based-statistics-stage1.md) 다.

## Global Constraints

- `./gradlew` 는 `imstargg-backend/`, `pnpm` 은 `imstargg-frontend/` 에서 실행한다. **`git` 은 저장소 루트에서 실행한다.** 아래 경로는 모두 저장소 루트 기준이다.
- **API**(스펙 6장): `GET /v1/statistics/patches` 는 선택할 수 있는 패치를 최신순으로 최대 3개 준다. 항목은 `patchDate`(`yyyy-MM-dd`)와 `lastBattleAt`(ISO 일시)이다. 통계 7개(`/v1/statistics/brawler/result`, `/brawler/rank`, `/brawler/enemy/result`, `/brawler/pair/result`, `/brawler/trio/result`, `/pair/result`, `/trio/result`)는 `dateRange` 를 받지 않고 `patchDate`(필수)를 받는다. 응답 모양은 그대로다. `/v1/statistics/brawler/result/daily` 는 없앤다. `/v1/statistics/brawler/item/ownership` 은 그대로다.
- **선택할 수 있는 패치**: 다섯 통계 유형 모두의 체크포인트 줄에 최근 전투 일시가 있는 패치다. `lastBattleAt` 은 다섯 값 중 가장 이른 값이다. 통계 7개는 선택할 수 있는 패치면 목록의 3개 밖이어도 받고, 아니면 404, 형식이 틀리거나 값이 없으면 400 이다. 옛 화면을 위한 호환 경로를 두지 않는다.
- **숫자 규칙**: 분모, 표본 하한, 티어 점수, 상위 N 을 바꾸지 않는다(`ComboStatsPolicy`, `StatsRate`, `BrawlerTierScore`). 합산 범위만 기간에서 패치 하나로 바뀐다.
- **캐시**: 이름은 `CacheNames.STATISTICS`(30분) 그대로다. 키의 시작·끝 날짜를 패치 id 로 바꾸고 접두사 버전을 메서드마다 1 올린다(Task 3 의 표). 패치 목록도 같은 캐시에 둔다.
- **화면**(스펙 7장): 주소는 `?patch=2026-11-11`, 가장 최근 패치면 생략한다. 옛 `?date=` 와 목록에 없는 값은 가장 최근 패치로 본다. 선택 항목의 표시는 "11월 11일 패치", 문구는 "최근 업데이트: 55분 전"(플레이어 화면과 같은 `formatRelativeTime`), 준비 안내는 "통계를 준비하고 있습니다". 패치 목록이 비었거나 못 받아도 렌더와 빌드가 실패하지 않는다.
- **프론트 규칙**: `"use client"` 컴포넌트는 `src/api` 를 직접 부르지 않고 `src/hooks/queries/*` 를 거친다. 쿼리 키는 `src/hooks/queries/queryKeys.ts` 에서만 만든다. DTO 는 `src/api/types.ts`, 매퍼는 `src/domain/*/mappers.ts`. 테스트는 네트워크 경계(MSW)만 대신한다. UI 를 고치기 전에 `imstargg-frontend/DESIGN.md` 와 `imstargg-frontend/AGENTS.md` 를 읽는다.
- **백엔드 규칙**: 파일당 클래스 하나, 인자가 셋 이상이면 named arguments, 주석·KDoc·테스트 이름은 한국어. Service 가 다른 Service 를 부르지 않는다(패치일 확인은 `StatisticsQueryComposer` 가 한다). DB 는 H2 실 DB 를 쓰고 리포지토리를 mockk 로 바꾸지 않는다. 컨텍스트 테스트 서브클래스에 `@Tag("context")` 를 다시 달지 않는다. Redis 는 테스트에 없다(`redisCacheManager` 를 직접 부르지 않는다).
- **확인 명령**: 백엔드는 태스크마다 고친 모듈의 `ktlintFormat`, 이어서 따로 `ktlintCheck`. 프론트는 CI 가 `pnpm test:run` 만 돌리므로 태스크마다 `pnpm typecheck && pnpm lint && pnpm test:run` 을, 마지막에 `pnpm build` 를 로컬에서 돌린다. `pnpm` 이 "env: node: No such file" 로 죽으면 `/opt/homebrew/bin/pnpm` 을 직접 부른다.
- Kotlin 을 고치기 전에 `superdomain:conventions` 스킬을 읽는다. 새 이름은 `docs/superglossary/glossary.md` 의 단어로 짓는다. 이 계획에 적힌 이름은 사전과 대조한 것이니 그대로 쓴다.
- 커밋은 Conventional Commits 이고 끝에 `Co-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>` 트레일러를 붙인다. `git add` 는 경로를 적어서 한다. 푸시하지 않는다.
- 운영 DB 와 `.secrets/` 에 접근하지 않는다.

## 실행 전 확인 (컨트롤러)

- **1단계가 운영에 있고 전환 조건을 통과했다**(스펙 10장). 사용자에게 네 가지를 확인받는다. 하나라도 아니면 구현은 할 수 있어도 `develop` 에 병합하지 않는다(`develop` 에 들어가면 다음 운영 배포에 함께 나간다).
  1. 체크포인트 줄이 통계 유형 5 × 줄이 만들어진 패치 수만큼 있고, 적용한 범위가 패치 목록과 같고, 실행마다 체크포인트가 오른다.
  2. 1인 승률·1인 순위: 줄이 만들어진 가장 오래된 패치를 `_backup` 과 견줘 새 값이 옛 값 이상이고 승률이 비슷하다.
  3. 시험 패치를 넣었다 뺀 뒤 숫자가 등록 전과 정확히 같다. 3인 조합 하루치 빼기의 시간을 쟀다.
  4. 상대·조합의 표본 규모와 상위 조합이 비슷하다.
- **브랜치**: `origin/develop`(1단계가 들어간 뒤)에서 `feature/patch-based-statistics-switch` 를 만든다.
- **1단계가 낸 이름을 대조한다.** 아래가 그대로 있어야 한다. 다르면 이 계획의 이름을 실제 이름으로 읽고, 차이를 기록한다.

```bash
git grep -n "enum class StatsType" -- imstargg-backend/core/core-enum
git grep -n "class StatsCheckpointEntity\|fun findAllByStatsType" -- imstargg-backend/infrastructure/db-core
git grep -n "class BrawlerBattleRankStatsEntity\|fun rankCounts" -- imstargg-backend/infrastructure/db-core
git grep -ln "BackupEntity" -- imstargg-backend | wc -l      # 옛 것이 Backup 이름으로 남아 있다
git grep -n "PatchStatsJob\"" -- imstargg-backend/core/core-batch   # 새 잡 다섯 개
```

- **core-domain**: `imstargg-backend/core/core-domain` 모듈이 생겼으면 멈추고 사용자에게 알린다.
- **프론트**: `node_modules` 가 없으면 `pnpm install`. 3000 번 포트에 남은 서버가 있으면 E2E 전에 끈다.
- **커밋 시각**: 09시에서 18시 사이(KST)에 찍힌 커밋은 푸시·PR 전에 그 밖의 시각으로 옮긴다.

## Review Focus

1. **캐시에 넣은 패치 목록의 직렬화**: 값에 `LocalDate` 와 `Instant` 가 든다. Redis 오류는 폴백 핸들러가 삼켜 DB 조회로 넘어가므로, 직렬화가 깨져도 화면은 멀쩡하고 캐시만 조용히 죽는다. → Task 2 "패치 목록이 캐시 직렬화를 왕복한다".
2. **`patchDate` 가 날짜 형식이 아니거나 옛 `dateRange` 만 옴**(`2026-11-1`, `LAST_WEEK`, 값 없음): 500 이 아니라 400 이다. → Task 3 "패치일이 날짜 형식이 아니면 400 이다", "패치일 없이 옛 기간만 보내면 400 이다".
3. **패치 목록은 받았는데 통계 조회가 실패한 서버 렌더**(404·500·시간 초과): 페이지 오류나 빌드 실패가 아니라 준비 안내가 보인다. → Task 8, Task 9 "통계를 못 받으면 준비 안내를 보인다".
4. **주소의 `patch` 가 목록에 없는 값·틀린 형식·옛 `date=LAST_WEEK`**: 가장 최근 패치의 통계를 보이고 그 패치일로 요청한다. → Task 5 "목록에 없는 값은 가장 최근 패치다", Task 8 "주소의 패치가 목록에 없으면 가장 최근 패치로 조회한다".
5. **서버가 굳힌 HTML 속의 상대 시간**: ISR 로 60초 이상 묵은 "55분 전"이 그대로 보이거나 하이드레이션이 어긋나지 않는다. 상대 시간은 브라우저에서 그린다. → Task 6 "서버 렌더 결과에는 상대 시간이 없고 마운트 뒤에 나타난다".

## File Structure

`…db/` 는 `imstargg-backend/infrastructure/db-core/src/{main,test}/kotlin/com/imstargg/infrastructure/db/core/`, `res/` 는 `imstargg-backend/infrastructure/db-core/src/main/resources/`, `…api/` 는 `imstargg-backend/core/core-api/src/{main,test}/kotlin/com/imstargg/core/`, `…batch/` 는 `imstargg-backend/core/core-batch/src/{main,test}/kotlin/com/imstargg/core/batch/`, `…enum/` 은 `imstargg-backend/core/core-enum/src/{main,test}/kotlin/com/imstargg/core/enums/`, `fe/` 는 `imstargg-frontend/` 다.

| 파일 | 책임 |
|---|---|
| `…db/statistics/repository/Brawler*StatsJpaRepository*.kt` | 새 통계 테이블 조회(패치 하나, 합산 없음) |
| `…api/domain/statistics/StatisticsPatch.kt`, `StatisticsPatches.kt`, `StatisticsPatchCheckpoint.kt` | 선택할 수 있는 패치의 규칙 |
| `…api/application/statistics/StatisticsPatchRepository.kt`, `StatisticsPatchRepositoryWithCache.kt`, `StatisticsPatchService.kt` | 패치 목록 조회와 캐시, 패치일 확인 |
| `…api/api/controller/v1/StatisticsController.kt`, `response/StatisticsPatchResponse.kt`, `…api/api/query/StatisticsQueryComposer.kt` | 엔드포인트 |
| `…api/application/statistics/Battle{Result,Rank}Statistics{Service,Repository,RepositoryWithCache}.kt` | 통계 7개가 패치로 조회 |
| `res/db/migration/V{yyMMdd}a__drop_stats_backup.sql` | 전환 1주 뒤에 적용하는 삭제 SQL |
| `fe/src/domain/statistics/patch.ts` | 패치 해석과 표시(순수 함수) |
| `fe/src/api/endpoints/statistics.ts`, `fe/src/api/types.ts`, `fe/src/hooks/queries/queryKeys.ts`, `useBrawlerStats.ts`, `useEventStats.ts` | 클라이언트와 훅 |
| `fe/app/_lib/loadStatsPatches.ts` | 서버에서 패치 목록을 받되 실패하면 빈 목록 |
| `fe/app/_components/statistics/StatRangeSelector.tsx`, `StatsLastUpdated.tsx`, `StatsPreparing.tsx` | 티어·패치 선택, 최근 업데이트, 준비 안내 |
| `fe/app/brawlers/**`, `fe/app/events/[id]/**` | 세 화면 |

---

### Task 1: 새 통계 테이블 조회 (db-core)

**Files:**
- Create: `…db/statistics/repository/` 에 `BrawlerBattleResultStatsJpaRepository.kt`, `…Custom.kt`, `…CustomImpl.kt`, `BrawlerBattleRankStatsJpaRepository.kt`, `BrawlerEnemyBattleResultStatsJpaRepository.kt`(+`Custom`, `CustomImpl`), `BrawlerPairBattleResultStatsJpaRepository.kt`(+2), `BrawlerTrioBattleResultStatsJpaRepository.kt`(+2)
- Test: `…db/statistics/repository/PatchStatsJpaRepositoryTest.kt`

**Interfaces:**
- Consumes: 1단계의 새 엔티티 5개, 지금 있는 `BrawlerEventResultProjection`, `BrawlerEnemyResultProjection`, `BrawlerPairResultProjection`, `BrawlerTrioResultProjection`.
- Produces(모두 `JpaRepository<엔티티, Long>` 를 잇는다):

```kotlin
// BrawlerBattleResultStatsJpaRepositoryCustom
fun findAllByPatch(brawlStarsPatchId: Long, tierRange: StatsTierRange, eventBrawlStarsId: Long?): List<BrawlerEventResultProjection>
// BrawlerBattleRankStatsJpaRepository (파생 쿼리)
fun findAllByBrawlStarsPatchIdAndTierRangeAndEventBrawlStarsId(brawlStarsPatchId: Long, tierRange: StatsTierRange, eventBrawlStarsId: Long): List<BrawlerBattleRankStatsEntity>
// BrawlerEnemyBattleResultStatsJpaRepositoryCustom
fun findAllByBrawler(brawlStarsPatchId: Long, tierRange: StatsTierRange, brawlerBrawlStarsId: Long): List<BrawlerEnemyResultProjection>
// BrawlerPairBattleResultStatsJpaRepositoryCustom (3인도 같은 세 메서드, 반환은 BrawlerTrioResultProjection)
fun findAllByBrawler(brawlStarsPatchId: Long, tierRange: StatsTierRange, brawlerBrawlStarsId: Long): List<BrawlerPairResultProjection>
fun findAllHavingTotalCountAtLeast(brawlStarsPatchId: Long, tierRange: StatsTierRange, minTotalCount: Long): List<BrawlerPairResultProjection>
fun sumTotalCount(brawlStarsPatchId: Long, tierRange: StatsTierRange): Long
```

- [ ] **Step 1: 실패하는 테스트를 쓴다.** `DbCoreContextTest` 를 상속하고 새 엔티티를 직접 저장한다.
  - "패치와 티어 구간이 같은 줄만 준다"(다른 패치, 다른 티어 구간의 줄을 섞는다. 유형마다 한 건)
  - "1인 승률은 이벤트를 주지 않으면 모든 이벤트의 줄을 준다", "이벤트를 주면 그 이벤트의 줄만 준다"
  - "조합은 브롤러가 어느 자리에 있든 찾는다"(2인은 두 자리, 3인은 세 자리)
  - "합이 하한 이상인 조합만 준다"(하한과 같은 값은 들어간다)
  - "조합의 합은 하한과 무관한 전체 합이다", "줄이 없으면 합은 0 이다"
- [ ] **Step 2: 실패를 확인한다.** `./gradlew :infrastructure:db-core:test --tests '*PatchStatsJpaRepositoryTest'` → 컴파일 실패.
- [ ] **Step 3: 리포지토리를 만든다.** QueryDSL 은 옛 `…BackupJpaRepositoryCustomImpl` 과 같은 모양에서 날짜 조건을 `brawlStarsPatchId.eq` 로 바꾸고 `groupBy` 와 `sum` 을 뺀다(키마다 한 줄이다). `sumTotalCount` 만 합을 구한다.
- [ ] **Step 4: 통과를 확인한다.** 위 명령 → 통과. `ktlintFormat`, `ktlintCheck`.
- [ ] **Step 5: 커밋한다.** `feat(backend): 패치별 통계 테이블을 조회하는 리포지토리를 더한다`

### Task 2: 패치 목록 API

**Files:**
- Create: `…api/domain/statistics/StatisticsPatch.kt`, `StatisticsPatches.kt`, `StatisticsPatchCheckpoint.kt`
- Create: `…api/application/statistics/StatisticsPatchRepository.kt`, `StatisticsPatchRepositoryWithCache.kt`, `StatisticsPatchService.kt`
- Create: `…api/api/controller/v1/response/StatisticsPatchResponse.kt`
- Modify: `…api/api/controller/v1/StatisticsController.kt`, `…api/api/query/StatisticsQueryComposer.kt`
- Test: `…api/domain/statistics/StatisticsPatchesTest.kt`, `…api/application/statistics/StatisticsPatchRepositoryTest.kt`, `StatisticsPatchServiceTest.kt`, 그리고 기존 `StatisticsCacheKeyTest.kt`, `…api/config/CacheConfigSerializerTest.kt`, `…api/api/controller/v1/StatisticsControllerTest.kt`

**Interfaces:**
- Consumes: `BrawlStarsPatchJpaRepository.findAllByOrderByPatchDateDesc()`, `StatsCheckpointJpaRepository.findAll()`, `StatsType`, `CoreException(ErrorType.NOT_FOUND, …)`, `CacheNames.STATISTICS`, `ListResponse`.
- Produces:

```kotlin
data class StatisticsPatch(val id: Long, val patchDate: LocalDate, val lastBattleAt: Instant)
data class StatisticsPatchCheckpoint(val patchId: Long, val statsType: StatsType, val lastBattleAt: Instant?)
/** 선택할 수 있는 패치 전부. 최신순이다. */
data class StatisticsPatches(val patches: List<StatisticsPatch>) {
    fun recent(): List<StatisticsPatch>                     // 앞에서 RECENT_COUNT 개
    fun find(patchDate: LocalDate): StatisticsPatch?        // 3개 밖도 찾는다
    companion object {
        const val RECENT_COUNT = 3
        fun create(patchDates: Map<Long, LocalDate>, checkpoints: List<StatisticsPatchCheckpoint>): StatisticsPatches
    }
}
class StatisticsPatchRepository { fun findStatisticsPatches(): StatisticsPatches }           // @Repository @Transactional(readOnly = true)
class StatisticsPatchRepositoryWithCache { fun findStatisticsPatches(): StatisticsPatches }  // @Cacheable(cacheNames = [CacheNames.STATISTICS], key = "'v1:statisticsPatches'", sync = true)
class StatisticsPatchService {
    fun getRecentPatches(): List<StatisticsPatch>
    fun getPatch(patchDate: LocalDate): StatisticsPatch      // 없으면 CoreException(ErrorType.NOT_FOUND, "patchDate=$patchDate")
}
data class StatisticsPatchResponse(val patchDate: LocalDate, val lastBattleAt: Instant)
// StatisticsQueryComposer
fun getPatches(): ListResponse<StatisticsPatchResponse>
fun getPatch(patchDate: LocalDate): StatisticsPatch          // Task 3 이 쓴다
// StatisticsController
@GetMapping("/v1/statistics/patches") fun getPatches(): ResponseEntity<ListResponse<StatisticsPatchResponse>>
```

- [ ] **Step 1: 실패하는 테스트를 쓴다.**
  - `StatisticsPatchesTest`(순수 단위): "다섯 유형 모두에 최근 전투 일시가 있는 패치만 선택할 수 있다"(네 유형만 있는 패치, 다섯 줄 중 하나가 `null` 인 패치는 빠진다), "최근 업데이트는 다섯 값 중 가장 이른 값이다", "최신순으로 3개까지만 최근 목록에 든다"(패치 4개 → `recent()` 는 최신 3개), "3개 밖의 패치도 패치일로 찾는다", "선택할 수 없는 패치는 찾지 못한다", "체크포인트는 있는데 패치가 지워졌으면 넣지 않는다", "패치가 없으면 목록이 비어 있다".
  - `StatisticsPatchRepositoryTest`(`CoreApiContextTest`, H2): 패치와 체크포인트 줄을 저장하고 `findStatisticsPatches()` 가 위 규칙의 결과를 준다(선택할 수 있는 패치 하나, 없는 패치 하나).
  - `StatisticsPatchServiceTest`(`CoreApiContextTest`): "없는 패치일이면 NOT_FOUND 다", "선택할 수 없는 패치일이면 NOT_FOUND 다", "3개 밖의 패치일은 받는다".
  - `StatisticsCacheKeyTest`: `targets` 에 `StatisticsPatchRepositoryWithCache::class` 를 더하고 스냅샷에 `"StatisticsPatchRepositoryWithCache.findStatisticsPatches" to "'v1:statisticsPatches'"` 를 더한다.
  - `CacheConfigSerializerTest`: **"패치 목록이 캐시 직렬화를 왕복한다"**. `StatisticsPatches(listOf(StatisticsPatch(id = 2, patchDate = LocalDate.of(2026, 11, 11), lastBattleAt = Instant.parse("2026-11-20T00:10:00Z"))))` 를 캐시 직렬화기로 썼다 읽은 값이 같다.
  - `StatisticsControllerTest`: "패치 목록을 준다"(`$.content[0].patchDate == "2026-11-11"`, `$.content[0].lastBattleAt` 이 ISO 일시).
- [ ] **Step 2: 실패를 확인한다.** `./gradlew :core:core-api:test --tests '*StatisticsPatch*' --tests '*StatisticsCacheKeyTest' --tests '*CacheConfigSerializerTest' --tests '*StatisticsControllerTest'` → 컴파일 실패.
- [ ] **Step 3: 구현한다.** `StatisticsPatchRepository` 는 패치 목록과 체크포인트 줄 전부(유형 5 × 패치 수라 적다)를 읽어 `StatisticsPatches.create` 에 넘긴다. 규칙은 도메인에만 둔다.
- [ ] **Step 4: 통과를 확인한다.** 위 명령 → 통과. `ktlintFormat`, `ktlintCheck`.
- [ ] **Step 5: 커밋한다.** `feat(backend): 선택할 수 있는 패치와 최근 업데이트 일시를 주는 API 를 더한다`

### Task 3: 통계 7개가 패치일을 받고 일별 통계를 없앤다

**Files:**
- Modify: `…api/api/controller/v1/StatisticsController.kt`, `…api/api/query/StatisticsQueryComposer.kt`, `…api/application/statistics/BattleResultStatisticsService.kt`, `BattleRankStatisticsService.kt`, `BattleResultStatisticsRepositoryWithCache.kt`, `BattleRankStatisticsRepositoryWithCache.kt`, `BattleResultStatisticsRepository.kt`, `BattleRankStatisticsRepository.kt`
- Delete: `…api/domain/statistics/BrawlerResultDailyCount.kt`, `BrawlerResultDailyStatistics.kt`, `…api/api/controller/v1/response/BrawlerResultDailyStatisticsResponse.kt`, `…api/infrastructure/jpa/RankCountsEmbeddedExtensions.kt`
- Test: `StatisticsControllerTest.kt`, `BattleResultStatisticsRepositoryTest.kt`, `BattleRankStatisticsRepositoryTest.kt`, `StatisticsCacheKeyTest.kt`, `…api/api/query/StatisticsQueryComposerTest.kt`(새로 만든다)

**Interfaces:**
- Consumes: Task 1 의 리포지토리, Task 2 의 `StatisticsQueryComposer.getPatch(patchDate)`.
- Produces:
  - 컨트롤러 7곳: `@RequestParam dateRange: StatsDateRange` → `@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) patchDate: LocalDate`.
  - Composer 7곳: `dateRange` 인자 → `patchDate: LocalDate`. 맨 앞에서 `getPatch(patchDate)` 로 패치를 구하고 Service 에 `patchId = patch.id` 를 넘긴다.
  - Service·WithCache·Repository 8개 메서드: `startDate`, `endDate`(Service 는 `dateRange`) 인자 → `patchId: Long`. 두 Service 에서 `Clock` 의존을 뺀다.
  - 캐시 키(전부):

| 메서드 | 키 |
|---|---|
| `findBrawlerResultStatistics` | `'v5:brawlerResultStats:' + #eventId + ':' + #tierRange + ':' + #patchId` |
| `findBrawlerPairResultStatistics` | `'v4:brawlerPairResultStats:' + #brawlerId + ':' + #tierRange + ':' + #patchId` |
| `findBrawlerTrioResultStatistics` | `'v2:brawlerTrioResultStats:' + #brawlerId + ':' + #tierRange + ':' + #patchId` |
| `findPairResultRanking` | `'v2:pairResultRanking:' + #tierRange + ':' + #patchId` |
| `findTrioResultRanking` | `'v2:trioResultRanking:' + #tierRange + ':' + #patchId` |
| `findBrawlerEnemyResultStatistics` | `'v4:brawlerEnemyResultStats:' + #brawlerId + ':' + #tierRange + ':' + #patchId` |
| `findBrawlerRankStatistics` | `'v2:brawlerRankStats:' + #eventId + ':' + #tierRange + ':' + #patchId` |

- [ ] **Step 1: 실패하는 테스트를 쓴다.**
  - `StatisticsControllerTest`: 모든 요청의 `dateRange=THIS_WEEK` 를 `patchDate=2026-11-11` 로 바꾼다. 더한다: **"패치일이 날짜 형식이 아니면 400 이다"**(`patchDate=2026-11-1`, `patchDate=LAST_WEEK`), **"패치일 없이 옛 기간만 보내면 400 이다"**(`dateRange=THIS_WEEK` 만), "일별 통계 경로는 없다"(`/v1/statistics/brawler/result/daily` → 404), "옛 기간 파라미터가 함께 와도 무시한다". `@MockkBean Clock` 을 뺀다.
  - `StatisticsQueryComposerTest`(`CoreApiContextTest`, H2): "선택할 수 없는 패치일이면 NOT_FOUND 다"(통계 7개 메서드 각각), "선택할 수 있는 패치일이면 그 패치의 통계를 준다"(1인 승률 한 건을 저장하고 응답에 그 브롤러가 있다).
  - `BattleResultStatisticsRepositoryTest`, `BattleRankStatisticsRepositoryTest`: 새 엔티티와 `patchId` 로 다시 쓴다. 지금의 케이스를 유지하되 "여러 날짜·주의 줄을 합산한다"는 케이스는 "다른 패치의 줄은 넣지 않는다"로 바꾼다. 분모 케이스(2인은 합 ÷ 3, 3인은 합 그대로, 하한 미만 조합도 분모에 든다)는 그대로 둔다. 일별 케이스 두 건은 지운다.
  - `StatisticsCacheKeyTest`: 스냅샷을 위 표로 바꾸고 일별 줄을 지운다.
- [ ] **Step 2: 실패를 확인한다.** `./gradlew :core:core-api:test --tests '*Statistics*'` → 컴파일 실패.
- [ ] **Step 3: 구현한다.** 1인 순위는 `entity.rankCounts()`(0번이 1위)로 `RankCount` 를 만든다. 옛 `RankCountsEmbedded.toMap()` 이 만들던 것과 같은 맵이어야 한다(먼저 그 함수를 읽고 0 인 순위를 같은 방식으로 다룬다). 일별 통계의 메서드와 import 를 컨트롤러·Composer·Service·WithCache·Repository 에서 지운다.
- [ ] **Step 4: 통과를 확인한다.** `./gradlew :core:core-api:test` 전체 통과. `git grep -n "dateRange\|StatsDateRange\|Daily" -- imstargg-backend/core/core-api/src/main` → 0건. `ktlintFormat`, `ktlintCheck`.
- [ ] **Step 5: 커밋한다.** `feat(backend)!: 통계 API 가 기간 대신 패치일을 받고 일별 통계를 없앤다`

### Task 4: 옛 통계를 지운다 (백엔드)

**Files:**
- Delete(core-batch): `…batch/job/statistics/{battleresult,battlerank,enemybattleresult,pairbattleresult,triobattleresult}/` 전부와 그 테스트, `…batch/domain/statistics/` 의 옛 집계기 5개와 `BattleStatsAggregator.kt`, `BrawlerBattleStatsKey.kt`, `RankCountAccumulator.kt`, 그 테스트(`Brawler*StatsAggregatorTest` 5개, `BattleStatsAggregatorChunkFlushTest`, `StatsBotBattleExclusionTest`), `…batch/domain/statistics/patch/PatchStatsAggregatorParityTest.kt`
- Delete(core-enum): `StatsDateRange.kt`, `StatsWeekRange.kt`, `WeekRange.kt` 와 그 테스트
- Delete(db-core): `…db/statistics/Brawler*StatsBackupEntity.kt` 5개, `repository/Brawler*StatsBackupJpaRepository*.kt`, `RankCountsEmbedded.kt`, 그 테스트(`…BackupEntityTest` 2개, `ComboBattleResultStatsBackupJpaRepositoryCustomImplTest`, `RankCountsEmbeddedTest`, `StatsBackupEntityNameTest`)
- Modify: `…batch/job/PartitionRetentionLookbackTest.kt`(주간 룩백 테스트를 지우고 보관과 기한의 테스트만 남긴다), `…batch/job/PartitionManageJobConfig.kt`(`RETENTION_DAYS` KDoc 에서 주간 통계 이야기를 뺀다), `res/schema.sql`(`_backup` 다섯 테이블의 DDL 삭제), `res/partition-manage.sql`(`create_month_partitions`, `drop_old_month_partitions` 삭제)
- Create: `res/db/migration/V{yyMMdd}a__drop_stats_backup.sql`(`{yyMMdd}` 는 구현하는 날)

**Interfaces:**
- Produces: 저장소에 `Backup`, `StatsDateRange`, `StatsWeekRange` 가 남지 않는다. 삭제 SQL 파일의 이름은 배포 절과 PR 본문에 적는다.

- [ ] **Step 1: 삭제 SQL 을 쓴다.** 머리 주석 첫 줄에 **"전환(2단계 배포) 1주 뒤에 적용한다. 다른 마이그레이션과 함께 적용하지 않는다"** 를 적고, 조건 둘(새 잡 5종이 1주 동안 실패 없이 돌았다, 화면 숫자에 이상이 없다)과 "적용하면 되돌릴 수 없다"를 적는다. 본문은 `drop table if exists {t}_backup cascade` 다섯 개(월 파티션이 함께 지워진다), `drop sequence if exists {t}_backup_seq` 다섯 개, `drop function if exists create_month_partitions(date, integer)`, `drop function if exists drop_old_month_partitions(date)` 다. 함수의 인자 타입은 `res/partition-manage.sql` 에서 확인하고 지운다.
- [ ] **Step 2: 로컬 PostgreSQL 에서 확인한다.** 1단계 계획 Task 4 와 같은 방법으로 컨테이너를 띄워 `origin/main`(1단계가 나간 상태)의 `schema.sql` 과 `partition-manage.sql` 을 넣고, 삭제 SQL 을 두 번 적용한다. 기대: 둘 다 오류 없이 끝나고, `select count(*) from pg_class where relname ~ '_backup'` 이 0 이고, 새 다섯 테이블과 `stats_checkpoint` 는 남아 있다. 도커가 없으면 "확인하지 못함"으로 적는다.
- [ ] **Step 3: 코드를 지운다.** 지운 뒤 남은 참조를 고친다: `StatsBotBattleMonitor`·`StatsBotLobbyMonitor` 와 그 테스트는 남긴다(새 잡이 쓴다). `DateJobParameter` 는 남긴다(다른 잡이 쓴다). KDoc 과 주석에 남은 옛 클래스 이름(`StatsWeekRange` 등)을 지운다.
- [ ] **Step 4: 확인한다.** `./gradlew test ktlintCheck` 전체 통과. `git grep -nE "Backup(Entity|JpaRepository)|StatsDateRange|StatsWeekRange|WeekRange|RankCountsEmbedded|_backup|month_partitions" -- imstargg-backend ':!imstargg-backend/infrastructure/db-core/src/main/resources/db/migration'` → 0건.
- [ ] **Step 5: 커밋한다.** `refactor(backend)!: 옛 통계 잡과 엔티티, 기간 옵션을 지운다`

### Task 5: 프론트의 패치 기반 (타입, 클라이언트, 순수 함수)

**Files:**
- Create: `fe/src/domain/statistics/patch.ts`, `fe/app/_lib/loadStatsPatches.ts`
- Modify: `fe/src/api/types.ts`, `fe/src/api/endpoints/statistics.ts`, `fe/src/hooks/queries/queryKeys.ts`, `fe/src/domain/statistics/enums.ts`, `fe/src/domain/statistics/mappers.ts`, `fe/src/domain/statistics/types.ts`
- Test: `fe/tests/unit/domain/statistics/patch.test.ts`, `fe/tests/unit/app/loadStatsPatches.test.ts`, 기존 `fe/tests/unit/domain/statistics/enums.test.ts`, `mappers.test.ts`

**Interfaces:**
- Produces:

```ts
// src/api/types.ts
export interface StatisticsPatchResponse { patchDate: string; lastBattleAt: string }
// src/domain/statistics/patch.ts
export interface StatsPatch { patchDate: string; lastBattleAt: string }   // patchDate 는 yyyy-MM-dd
/** 주소의 값이 목록에 있으면 그 패치일, 없거나 값이 없으면 목록의 첫 항목, 목록이 비었으면 null. */
export function resolvePatchDate(patches: StatsPatch[], value: string | null | undefined): string | null
export function isRecentPatchDate(patches: StatsPatch[], patchDate: string): boolean   // 목록의 첫 항목인가(주소에서 생략하는 기준)
export function formatPatchLabel(patchDate: string): string                            // "2026-11-11" → "11월 11일 패치"
export function toStatsPatch(dto: StatisticsPatchResponse): StatsPatch
// src/api/endpoints/statistics.ts
statisticsApi.patches(opts?: FetchOpts): Promise<ListResponse<StatisticsPatchResponse>>   // GET /v1/statistics/patches
// 파라미터 타입 7개: dateRange: StatsDateRangeDto → patchDate: string. daily 와 DailyParams 는 지운다.
// src/hooks/queries/queryKeys.ts
queryKeys.statistics.patches()      // daily 는 지운다
// app/_lib/loadStatsPatches.ts (서버 전용)
export async function loadStatsPatches(): Promise<StatsPatch[]>   // 실패하면 빈 배열. revalidate 는 CACHE_SECONDS.SHORT
```

  지우는 것: `enums.ts` 의 `STATS_DATE_RANGES`, `StatsDateRange`, `DATE_RANGE_LABELS`, `DEFAULT_DATE_RANGE`, `parseDateRange`. `types.ts` 의 `StatsDateRangeDto`, `BrawlerResultDailyStatisticsResponse`, 도메인의 `DailyResultStat`, 매퍼 `toDailyResultStat`. 이 태스크가 끝나면 그것을 쓰던 화면 파일들이 타입 오류가 난다. Task 6–9 가 차례로 고치고, 그 사이 커밋은 `pnpm test:run` 대상 테스트만 통과하면 된다(타입 검사는 Task 9 끝에서 전체가 통과한다).

- [ ] **Step 1: 실패하는 테스트를 쓴다.** `patch.test.ts`. `patches = [{ patchDate: "2026-11-11", … }, { patchDate: "2026-11-03", … }]`.
  - "목록에 있는 값은 그 패치다"(`"2026-11-03"` → `"2026-11-03"`)
  - **"목록에 없는 값은 가장 최근 패치다"**(`"2026-10-01"`, `"abc"`, `"LAST_WEEK"` → `"2026-11-11"`)
  - "값이 없으면 가장 최근 패치다"(`undefined`, `null`, `""`)
  - "목록이 비었으면 패치가 없다"(`null`)
  - "가장 최근 패치인지 안다", "패치일을 'n월 n일 패치'로 표시한다"(`"2026-11-11"` → `"11월 11일 패치"`, `"2026-01-05"` → `"1월 5일 패치"`. 시간대에 따라 하루가 밀리지 않는다)
  - `loadStatsPatches.test.ts`(MSW): "패치 목록을 받는다", "응답이 500 이면 빈 목록이다", "네트워크 오류면 빈 목록이다"
  - `enums.test.ts` 에서 기간 옵션의 케이스를, `mappers.test.ts` 에서 `toDailyResultStat` 의 케이스를 지운다.
- [ ] **Step 2: 실패를 확인한다.** `pnpm test:run tests/unit/domain/statistics tests/unit/app/loadStatsPatches.test.ts` → 모듈 없음으로 실패.
- [ ] **Step 3: 구현한다.** `formatPatchLabel` 은 문자열을 `-` 로 쪼개 만든다(`Date` 를 거치지 않는다). Task 6 의 `StatsPreparing` 에 쓸 단어를 용어 사전(`docs/superglossary/glossary.md`, 한글 열 코드포인트 순)에 등록한다: `| 준비 | prepare |  |  | 통계를 아직 보일 수 없을 때의 안내 |`.
- [ ] **Step 4: 통과를 확인한다.** 위 명령 → 통과. `pnpm lint`.
- [ ] **Step 5: 커밋한다.** `feat(frontend): 패치 목록의 타입과 패치 해석 함수를 더하고 기간 옵션을 지운다`

### Task 6: 선택 UI 와 최근 업데이트

**Files:**
- Modify: `fe/app/_components/statistics/StatRangeSelector.tsx`
- Create: `fe/app/_components/statistics/StatsLastUpdated.tsx`, `fe/app/_components/statistics/StatsPreparing.tsx`
- Test: `fe/tests/unit/components/statistics/StatRangeSelector.test.tsx`, `StatsLastUpdated.test.tsx`

**Interfaces:**
- Consumes: Task 5 의 `StatsPatch`, `formatPatchLabel`, `isRecentPatchDate`. 지금 있는 `formatRelativeTime`(`fe/src/lib/relativeTime.ts`).
- Produces:

```tsx
type ViewProps = {
  tier: StatsTierRange;
  patchDate: string;
  patches: StatsPatch[];
  onSelect: (next: { tier: StatsTierRange; patchDate: string }) => void;
};
export function StatRangeSelectorView(props: ViewProps)       // 티어 선택, 패치 선택(aria-label="패치 선택"), 그 옆에 StatsLastUpdated
export function StatRangeSelector(props: { tier: StatsTierRange; patchDate: string; patches: StatsPatch[] })   // 목록 페이지용. router.replace
export function StatsLastUpdated(props: { lastBattleAt: string })   // "최근 업데이트: 55분 전". 마운트 뒤에만 그린다
export function StatsPreparing()                                    // "통계를 준비하고 있습니다"
```

  `StatRangeSelector` 는 주소를 쓸 때 가장 최근 패치면 `patch` 를 지우고, 옛 `date` 파라미터는 늘 지운다.

- [ ] **Step 1: 실패하는 테스트를 쓴다.**
  - `StatRangeSelector.test.tsx`: 기간의 케이스를 패치로 바꾼다. "지금 패치를 'n월 n일 패치'로 보인다", "패치를 고르면 그 패치일을 넘긴다", "티어를 고르면 패치는 그대로 넘긴다", "가장 최근 패치를 고르면 주소에서 patch 가 빠진다", "다른 패치를 고르면 주소에 patch 가 들어간다", "주소에 남은 옛 date 는 지운다", "고른 패치의 최근 업데이트를 보인다".
  - `StatsLastUpdated.test.tsx`: **"서버 렌더 결과에는 상대 시간이 없고 마운트 뒤에 나타난다"**(`renderToString` 결과에 "전"이 없고, `render` 뒤에는 `최근 업데이트: 55분 전` 이 있다. 시각은 `vi.setSystemTime` 으로 `lastBattleAt` 의 55분 뒤로 고정한다).
- [ ] **Step 2: 실패를 확인한다.** `pnpm test:run tests/unit/components/statistics` → 실패.
- [ ] **Step 3: 구현한다.** 패치 선택은 기간 선택이 쓰던 shadcn `Select` 그대로다(너비는 표시 길이에 맞춘다). 문구의 색은 `RenewalStatus` 와 같은 보조 텍스트 색이다.
- [ ] **Step 4: 통과를 확인한다.** 위 명령 → 통과. `pnpm lint`.
- [ ] **Step 5: 커밋한다.** `feat(frontend): 기간 선택을 패치 선택으로 바꾸고 최근 업데이트를 보인다`

### Task 7: 브롤러 목록 화면 (`/brawlers`)

**Files:**
- Modify: `fe/app/brawlers/page.tsx`, `fe/app/brawlers/_lib/loadBrawlerMetaView.ts`
- Test: `fe/tests/unit/app/brawlers/loadBrawlerMetaView.test.ts`, `fe/tests/unit/app/brawlers/BrawlersPage.test.tsx`(새로 만든다)

**Interfaces:**
- Consumes: Task 5 의 `loadStatsPatches`, `resolvePatchDate`. Task 6 의 `StatRangeSelector`, `StatsPreparing`.
- Produces: `loadBrawlerMetaView(view: BrawlerMetaView, tierRange: StatsTierRange, patchDate: string): Promise<BrawlerMetaViewData | null>`. 통계 조회가 실패하면 `null` 이다.

  화면의 흐름: `loadStatsPatches()` → `resolvePatchDate(patches, sp.patch)` → `null` 이면 선택 UI 없이 `StatsPreparing` → 아니면 `loadBrawlerMetaView` → `null` 이면 선택 UI 와 `StatsPreparing`.

- [ ] **Step 1: 실패하는 테스트를 쓴다.** MSW 로 `/v1/statistics/patches` 와 통계 엔드포인트를 대신한다. 페이지는 `await BrawlersPage({ searchParams: Promise.resolve({…}) })` 로 직접 부른다.
  - `loadBrawlerMetaView.test.ts`: 요청의 `dateRange` 단언을 `patchDate` 로 바꾼다. "통계 조회가 실패하면 null 이다"를 더한다.
  - `BrawlersPage.test.tsx`: "주소에 패치가 없으면 가장 최근 패치로 조회한다"(요청 주소에 `patchDate=2026-11-11`), "주소의 패치로 조회한다", "주소의 패치가 목록에 없으면 가장 최근 패치로 조회한다", "옛 date 파라미터는 무시한다"(`date=LAST_WEEK`), "패치 목록이 비었으면 준비 안내를 보이고 통계를 요청하지 않는다", "패치 목록을 못 받으면 준비 안내를 보인다", "통계를 못 받으면 준비 안내를 보인다".
- [ ] **Step 2: 실패를 확인한다.** `pnpm test:run tests/unit/app/brawlers` → 실패.
- [ ] **Step 3: 구현한다.**
- [ ] **Step 4: 통과를 확인한다.** 위 명령 → 통과. `pnpm lint`.
- [ ] **Step 5: 커밋한다.** `feat(frontend): 브롤러 목록의 통계를 패치로 조회한다`

### Task 8: 브롤러 상세 화면과 일별 추이 삭제 (`/brawlers/[id]`)

**Files:**
- Modify: `fe/app/brawlers/[id]/page.tsx`, `fe/app/brawlers/[id]/_components/BrawlerStatsPanel.tsx`, `fe/src/hooks/queries/useBrawlerStats.ts`, `fe/package.json`, `fe/pnpm-lock.yaml`
- Delete: `fe/app/brawlers/[id]/_components/DailyTrendChart.tsx`, `fe/src/components/ui/chart.tsx`(쓰는 곳이 없어진다. knip 이 `src/components/ui/**` 를 보지 않으므로 손으로 지운다)
- Test: `fe/tests/components/BrawlerStatsPanel.test.tsx`

**Interfaces:**
- Consumes: Task 5, Task 6.
- Produces:

```ts
// src/hooks/queries/useBrawlerStats.ts
export interface StatsFilter { tier: StatsTierRange; patchDate: string }
/** 서버가 미리 받아 둔 통계와 같은 필터인가: 기본 티어이고 목록의 첫 패치다. */
export function isDefaultStatsFilter(filter: StatsFilter, patches: StatsPatch[]): boolean
export interface BrawlerStatsInitial { pair: PairResultStat[]; trio: TrioResultStat[]; enemy: EnemyResultStat[] }
export function useBrawlerStats(brawlerId: number, filter: StatsFilter, patches: StatsPatch[], initial?: BrawlerStatsInitial)
// BrawlerStatsPanel
props: { brawlerId: number; patches: StatsPatch[]; initial: BrawlerStatsInitial | null }
```

  `DEFAULT_STATS_FILTER` 는 지운다(기본 패치가 목록에서 나온다). 페이지는 `loadStatsPatches()` 로 목록을 받고, 목록이 있으면 첫 패치와 기본 티어로 2인·3인·상대 통계를 받는다. 통계 조회는 `try/catch` 로 감싸 실패하면 `initial = null` 로 내린다. 아이템 보유 통계의 조회는 지금 그대로 둔다. 패널은 목록이 비었거나 `initial` 이 `null` 이고 조회도 실패하면 `StatsPreparing` 을 보인다. 주소는 `?patch=` 를 지금의 `?date=` 와 같은 방식(마운트 뒤에 읽기, `popstate`, `applySearchShallow`)으로 다루고, 가장 최근 패치면 `patch: null` 로 지운다.

- [ ] **Step 1: 실패하는 테스트를 쓴다.** `BrawlerStatsPanel.test.tsx`. 일별 핸들러, `initial.daily`, `ResizeObserver` 대역을 지운다.
  - "미리 받은 통계를 가장 최근 패치로 보인다"(마운트 직후 통계 요청이 없다)
  - "패치를 바꾸면 그 패치일로 다시 조회하고 주소에 patch 가 들어간다"
  - "가장 최근 패치로 돌아오면 주소에서 patch 가 빠진다"
  - **"주소의 패치가 목록에 없으면 가장 최근 패치로 조회한다"**(`?patch=2026-10-01`, `?date=LAST_WEEK` 각각. 다시 조회하지 않고 미리 받은 통계를 보인다)
  - "주소의 패치가 목록의 다른 패치면 그 패치일로 조회한다"
  - "티어를 바꾸면 패치는 그대로 두고 다시 조회한다"
  - "패치 목록이 비었으면 준비 안내를 보이고 통계를 요청하지 않는다"
  - **"통계를 못 받으면 준비 안내를 보인다"**(`initial = null` 이고 조회가 500)
  - "일별 트렌드가 없다"(그 제목이 화면에 없다)
- [ ] **Step 2: 실패를 확인한다.** `pnpm test:run tests/components/BrawlerStatsPanel.test.tsx` → 실패.
- [ ] **Step 3: 구현하고 차트를 지운다.** `pnpm remove recharts`. `git grep -n "recharts\|DailyTrend\|ui/chart" -- imstargg-frontend ':!imstargg-frontend/docs' ':!imstargg-frontend/pnpm-lock.yaml'` → 0건.
- [ ] **Step 4: 통과를 확인한다.** 위 명령 → 통과. `pnpm lint`.
- [ ] **Step 5: 커밋한다.** `feat(frontend): 브롤러 상세의 통계를 패치로 조회하고 일별 추이를 없앤다`

### Task 9: 이벤트 상세 화면 (`/events/[id]`)

**Files:**
- Modify: `fe/app/events/[id]/_components/EventStatsSection.tsx`, `EventStatsPanel.tsx`, `fe/src/hooks/queries/useEventStats.ts`
- Test: `fe/tests/components/EventStatsSection.test.tsx`, `EventStatsPanel.test.tsx`

**Interfaces:**
- Consumes: Task 5, Task 6, Task 8 의 `StatsFilter`, `isDefaultStatsFilter`.
- Produces: `useEventResultStats(eventId, filter, patches, initial?, enabled?)`, `useEventRankStats(…)` 가 `patches` 를 받는다. `EventStatsPanel` 의 props 에 `patches: StatsPatch[]` 를 더하고 `initialResult`, `initialRank` 는 `null` 일 수 있다.

  `EventStatsSection` 의 흐름: 모드가 통계를 제공하지 않으면 지금의 안내 그대로(패치 목록도 요청하지 않는다) → `loadStatsPatches()` → 비었으면 `StatsPreparing` → 첫 패치와 기본 티어로 통계 조회(`try/catch`, 실패하면 `null`) → `EventStatsPanel`.

- [ ] **Step 1: 실패하는 테스트를 쓴다.**
  - `EventStatsSection.test.tsx`: 지금의 케이스를 유지하고 요청 단언을 `patchDate` 로 바꾼다. 더한다: "통계를 제공하지 않는 모드는 패치 목록도 요청하지 않는다", "패치 목록이 비었으면 준비 안내를 보이고 통계를 요청하지 않는다", **"통계를 못 받으면 준비 안내를 보인다"**(통계 엔드포인트가 404 여도 렌더가 던지지 않는다).
  - `EventStatsPanel.test.tsx`: "패치를 바꾸면 그 패치일로 다시 조회한다", "주소의 패치가 목록에 없으면 가장 최근 패치로 조회한다".
- [ ] **Step 2: 실패를 확인한다.** `pnpm test:run tests/components/EventStatsSection.test.tsx tests/components/EventStatsPanel.test.tsx` → 실패.
- [ ] **Step 3: 구현한다.**
- [ ] **Step 4: 전체를 확인한다.** `pnpm typecheck && pnpm lint && pnpm test:run && pnpm build` 모두 통과. `git grep -n "dateRange\|DateRange\|DATE_RANGE\|daily\|Daily" -- imstargg-frontend/src imstargg-frontend/app` → 0건(플레이어 통계 등 무관한 `daily` 가 나오면 그것만 남긴다).
- [ ] **Step 5: 커밋한다.** `feat(frontend): 이벤트 상세의 통계를 패치로 조회한다`

### Task 10: E2E, 문서, 버전 (컨트롤러)

E2E 는 운영 API(`api.imstargg.com`)를 프록시한다. 운영의 core-api 가 2단계로 바뀌기 전에는 새 E2E 가 통과할 수 없다. 테스트는 여기서 고쳐 두고, 실행은 배포 뒤 확인에서 한다.

- [ ] **Step 1: E2E 를 고친다.** `fe/tests/e2e/statistics.spec.ts`: "기간 선택" 케이스를 "패치 선택 상자에서 다른 패치를 고르면 주소에 `patch=` 가 들어간다"로 바꾼다(패치가 하나뿐이면 건너뛴다). "최근 업데이트:" 문구가 보인다. 브롤러 상세에서 "일별 트렌드" 제목을 기다리던 단언을 "없다"로 바꾼다. 스펙 9장의 "패치 목록을 못 받았을 때의 안내"는 서버 쪽 요청을 E2E 에서 실패시킬 수 없어 Task 7–9 의 컴포넌트 테스트가 맡는다. 이 차이를 PR 본문에 적는다.
- [ ] **Step 2: 문서.** `imstargg-backend/docs/stats-battle-data-strategy.md` 에서 옛 집계(일별·주별 재집계, 월 파티션)의 설명을 지우고 패치 기준 집계만 남긴다. `/superdomain:domain` 으로 `docs/superdomain/DOMAIN.md` 의 statistics 절을 고친다(기능: 패치 기준 누적 집계, 통계 조회의 "일별 추이" 삭제, 규칙: 패치의 범위와 다시 읽어 맞추기). `imstargg-frontend/` 문서에 기간 선택이나 일별 추이를 말하는 곳이 있으면 고친다.
- [ ] **Step 3: 용어 검사.** `/superglossary:check` 를 이 브랜치가 더한 코드에 돌린다.
- [ ] **Step 4: 전체 확인.** `./gradlew test ktlintCheck --rerun-tasks`, `pnpm typecheck && pnpm lint && pnpm test:run && pnpm build`. 건수를 PR 본문에 적는다.
- [ ] **Step 5: 버전.** `/release bump`. 통계 API 의 파라미터가 바뀌므로 호환되지 않는 변경이라는 것을 알려 준다(core-api, core-batch, frontend).
- [ ] **Step 6: 결정 기록을 제안한다.** 1단계에서 남기지 않았으면 여기서 다시 묻는다.

## 배포 (2단계와 3단계, 이 PR 밖)

**2단계**(스펙 10장). SQL 은 없다.

1. `imstargg-deploy/apps/prod/core-batch/values.yaml` 에서 옛 통계 CronJob 다섯 개(`battle-rank-stats`, `battle-result-stats`, `enemy-battle-result-stats`, `trio-battle-result-stats`, `pair-battle-result-stats`)를 뺀다. 새 CronJob 다섯 개와 `partition-manage` 는 그대로 둔다.
2. 프론트를 올린다. core-api 가 바뀌기 전까지 통계 영역이 "통계를 준비하고 있습니다"로 보인다.
3. core-api 와 core-batch 를 배포한다.
4. 배포 뒤 확인: `/brawlers`, `/brawlers/[id]`, `/events/[id]` 에서 패치 선택과 최근 업데이트가 보인다. `pnpm test:e2e` 를 돌린다(운영 API 를 본다. 갱신 테스트가 운영에 실제 갱신을 요청한다는 점은 지금과 같다).

프론트를 나중에 올리면 그 사이 옛 화면의 통계 요청이 400 을 받아 오류로 보인다. 순서를 지킨다.

**3단계**: 전환 1주 뒤, 조건 둘(새 잡 5종이 1주 동안 실패 없이 돌았다, 화면 숫자에 이상이 없다)을 확인하고 Task 4 의 삭제 SQL 을 적용한다. 적용하면 되돌릴 수 없다.

**되돌리기**(3단계 전까지): 세 앱을 1단계 릴리스의 버전으로 다시 올리고 옛 CronJob 다섯 개를 다시 넣는다. 그 버전은 `_backup` 테이블을 읽는다. 비는 날짜마다 옛 잡 5종을 날짜를 주어 손으로 돌린다.
