# core-api 통계 도메인 정합성 + 성능 개선 설계서

- 작성일: 2026-04-25
- 상태: 설계 (구현 대기)
- 선행 문서:
  - core-api 검토 보고서 (대화 이력)
  - `docs/superpowers/specs/2026-04-23-core-batch-data-integrity-design.md` (구현 완료)
  - `docs/superpowers/specs/2026-04-23-core-batch-team-stats-pipeline-design.md` (스켈레톤)

## 1. 목적

core-api 검토에서 발견된 다음 결함을 한 번에 정리한다.

- **NEW1 (Critical)**: weekly batch 가 매일 다른 `(start, end)` row 를 만들어 같은 주 데이터가 다중 합산되는 정합성 결함
- **C2**: 팀 통계 `brawlerIdFilters` 의 N 개 분리 서브쿼리 AND
- **C3**: `GROUP BY team_key` 최적화 인덱스 부재
- **C4**: `findBrawlerResultCounts` 의 엔티티 전체 페치 + 메모리 groupBy
- **I1**: 무거운 통계 캐시의 stampede 위험 (`sync = true` 부재)
- **I2**: 팀/적 통계의 count×2 + data 분리 캐시로 인한 시간 단면 어긋남
- **I3**: `brawlerIds` 캐시 키 직렬화의 비정렬·장문화 문제
- **I4**: 컨트롤러 입력 검증 부재 (`brawlerIds` 크기 무제한)

비목표:

- D7 Renewal outbox 패턴 도입
- I5 페이지네이션 클라이언트 노출
- 외부 분석 보고서의 그 외 minor 항목

운영 데이터 정리는 불필요 (대상 테이블에 데이터 없음, 서비스 미운영). 캐시 키 prefix 도 `v1` 유지.

## 2. NEW1 — Weekly batch row 누적 결함

### 2.1 근본 원인

`StatsWeekRange.currentWeek(date)` 가 `end = minOf(naturalSunday, monthEnd, date)` 로 today 에서 절단한다. 그 결과 매일 다른 `(start, end)` 가 만들어지고, weekly batch 의 `deleteExisting` 은 `startBattleDate.eq AND endBattleDate.eq` 정확 매칭 row 만 삭제하므로 이전 일자에 만든 row 가 그대로 남는다.

unique 인덱스 `uk_brawler_team_battle_*_stats__1` 은 `(event, tier, start, end, team_key)` 단위라 `(월,월)` 과 `(월,화)` 가 별도 row 로 공존. 조회 측이 `start.goe(startDate) AND end.loe(endDate)` 로 매칭하므로 같은 주 데이터가 row 수만큼 다중 합산된다.

같은 결함이 result/rank/enemy 3 개 weekly job 모두에 동일 적용된다.

### 2.2 수정 — 의도된 동작

통계 row 는 항상 **자연 주 (`월요일`, `일요일`)** 또는 **월말 경계 (`월요일`, `월말`)** 단위로 저장한다. batch 가 어느 요일에 돌든 같은 row 를 덮어쓴다.

```kotlin
// StatsWeekRange.kt
object StatsWeekRange {
    fun weekRange(date: LocalDate): WeekRange {
        val monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val naturalSunday = monday.plusDays(6)
        val monthStart = date.withDayOfMonth(1)
        val monthEnd = date.with(TemporalAdjusters.lastDayOfMonth())
        val start = maxOf(monday, monthStart)
        val end = minOf(naturalSunday, monthEnd)
        return WeekRange(start, end)
    }

    fun currentAndPrevious(today: LocalDate): List<WeekRange> {
        val current = weekRange(today)
        val previous = weekRange(today.minusDays(1))
        return if (previous == current) listOf(current) else listOf(previous, current)
    }
}
```

`currentWeek` / `finalizedWeek` 두 함수는 `weekRange` 하나로 통합된다. 호출처는 모두 `weekRange` 또는 `currentAndPrevious` 를 사용한다.

### 2.3 호출처 영향

| 파일 | 변경 |
| --- | --- |
| `BrawlerTeamBattleResultStatsJobConfig.kt:58` | `currentAndPrevious` 그대로 (시그니처 동일) |
| `BrawlerTeamBattleRankStatsJobConfig.kt:58` | 동일 |
| `BrawlerEnemyBattleResultStatsJobConfig.kt:55` | 동일 |
| `StatsDateRange.kt:23,28,37` | `StatsWeekRange.currentWeek(today)` → `StatsWeekRange.weekRange(today)` |

worker/admin 등 외부에서는 `StatsWeekRange` 를 직접 호출하지 않는다 (검증 완료).

`StatsDateRange` 변경의 사용자 노출 영향: `THIS_WEEK` / `SINCE_LAST_WEEK` / `SINCE_TWO_WEEKS_AGO` 의 `endDate` 가 today 에서 일요일(또는 월말)로 바뀐다. 응답 데이터 자체는 batch 가 채워둔 시점까지만 누적되어 이전과 동일하지만, 조회 범위 표기가 자연주로 통일된다. 서비스 미운영이므로 호환성 부담은 없다.

### 2.4 weekly batch deleteExisting 견고화

`weekRange` 통합으로 누적 자체가 사라지지만, 방어 차원에서 `deleteExisting` 의 `endBattleDate.eq` 는 그대로 둔다. 향후 누군가 `currentWeek` 의도를 되돌려도 잠재적 row 누적이 한 weekRange 로 한정되도록 한다.

### 2.5 테스트

- `StatsWeekRangeTest`: 같은 주 안의 모든 요일에서 `weekRange(date)` 가 동일 `(start, end)` 반환. 월 경계 (4/30, 5/1, 5/2) 와 월 첫 주 (월요일이 전월 마지막 주에 속하는 케이스) 케이스. `currentAndPrevious` 가 월요일/월초 케이스에서만 2 개 반환.
- 통합 테스트 (신규): `PerEventWeeklyBattleStatsTasklet` 을 같은 주 안의 서로 다른 날짜로 3 회 실행한 뒤 DB row 가 1 개 (각 weekRange 당) 인지 검증.

## 3. C2 — `brawlerIdFilters` EXISTS + COUNT(DISTINCT) 통합

### 3.1 변경

```kotlin
private fun brawlerIdFilters(ids: List<Long>): Predicate? {
    if (ids.isEmpty()) return null
    return brawlerTeamBattleResultStatsEntity.id.`in`(
        JPAExpressions
            .select(brawlerTeamBattleResultStatsBrawlerEntity.stats.id)
            .from(brawlerTeamBattleResultStatsBrawlerEntity)
            .where(brawlerTeamBattleResultStatsBrawlerEntity.brawlerBrawlStarsId.`in`(ids))
            .groupBy(brawlerTeamBattleResultStatsBrawlerEntity.stats.id)
            .having(
                brawlerTeamBattleResultStatsBrawlerEntity.brawlerBrawlStarsId
                    .countDistinct().eq(ids.size.toLong())
            )
    )
}
```

result / rank 두 Custom Impl 에 동일 적용. 기존 `forEach { builder.and(...) }` 패턴 제거.

### 3.2 인덱스 활용

`brawler_team_battle_result_stats_brawler` 의 기존 `(brawler_brawlstars_id, stats_id)` 인덱스로 `IN` + `GROUP BY stats_id` 가 효율적으로 풀린다. 추가 인덱스 불필요.

### 3.3 테스트

- 기존 Custom Impl 통합 테스트가 있다면 brawlerIds 1/2/3 개 케이스에서 결과 동일성 검증. 없다면 신규.

## 4. C3 — Covering index 추가

### 4.1 마이그레이션

`infrastructure/db-core/src/main/resources/db/migration/V260425a__brawler_team_stats_group_by_index.sql` (신규):

```sql
create index if not exists ix_brawler_team_battle_result_stats_2
  on brawler_team_battle_result_stats
  (event_brawlstars_id, tier_range, start_battle_date, end_battle_date, team_size, team_key);

create index if not exists ix_brawler_team_battle_rank_stats_2
  on brawler_team_battle_rank_stats
  (event_brawlstars_id, tier_range, start_battle_date, end_battle_date, team_size, team_key);
```

`schema.sql` 도 동일 인덱스 추가 (`ix_*_1` 다음 줄). 기존 `ix_*_1` 과 `uk_*_1` 은 유지.

### 4.2 트레이드오프

- write 비용 증가: 통계 테이블은 batch 시점에만 write 되므로 영향 미미.
- `eventId nullable` 조회 (eventId 미지정) 는 leading column 이 null 이라 인덱스 활용이 제한적. 본 스펙에서는 `eventId` 필수화는 별도 결정 사항으로 두고 인덱스만 추가한다.

## 5. C4 — `findBrawlerResultCounts` projection 전환

### 5.1 신규 Projection

`infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/repository/BrawlerResultProjection.kt` (신규):

```kotlin
data class BrawlerResultProjection(
    val brawlerBrawlStarsId: Long,
    val victoryCount: Long,
    val defeatCount: Long,
    val drawCount: Long,
    val starPlayerCount: Long,
)
```

### 5.2 Custom Repository 추가

`BrawlerBattleResultStatsJpaRepositoryCustom` / `*Impl` 신설 (현재 인터페이스가 없다면 함께 신설):

```kotlin
fun findGroupedByBrawler(
    eventBrawlStarsId: Long?,
    tierRange: StatsTierRange,
    startDate: LocalDate,
    endDate: LocalDate,
): List<BrawlerResultProjection>
```

QueryDSL `select(brawlerId, victorySum, defeatSum, drawSum, starPlayerSum).groupBy(brawlerId).fetch()`.

### 5.3 호출 측 변경

`BattleResultStatisticsRepository.findBrawlerResultCounts` 가 새 메서드를 호출해 `BrawlerResultCount` 로 매핑. 메모리 `groupBy`/`reduce` 제거.

`findBrawlerDailyResultCounts` (단일 brawler·날짜별) 는 row 수가 작아 본 스펙 범위 밖.

## 6. I1 + I2 + I3 — 캐시 통합 + 정규화 + sync

### 6.1 통합 응답 DTO

각 통계마다 `items + totalBattleCount + totalElementCount` 를 묶은 응답 객체를 도입한다. `core-api/.../application/statistics/` 패키지에 신규.

```kotlin
data class BrawlerTeamResultCounts(
    val items: List<BrawlerTeamResultCount>,
    val totalBattleCount: Long,
    val totalElementCount: Long,
)

data class BrawlerTeamRankCounts(
    val items: List<BrawlerTeamRankCount>,
    val totalBattleCount: Long,
    val totalElementCount: Long,
)

data class BrawlerEnemyResultCounts(
    val items: List<BrawlerEnemyResultCount>,
    val totalBattleCount: Long,
    val totalElementCount: Long,
)
```

기존 도메인 통계 객체 (`BrawlerTeamResultStatistics` 등) 는 변경하지 않는다 — 이 DTO 는 `*RepositoryWithCache` 의 단일 리턴 타입 용도다.

### 6.2 단일 캐시 메서드

`BattleResultStatisticsRepositoryWithCache`:

```kotlin
@Cacheable(
    cacheNames = [CacheNames.STATISTICS],
    key = "'v1:teamResult:' + #eventId + ':' + T(com.imstargg.core.application.statistics.StatisticsCacheKeys).joinIds(#brawlerIds)" +
        " + ':' + #tierRange + ':' + #startDate + ':' + #endDate + ':' + #teamSize" +
        " + ':' + #sortField + ':' + #sortOrder + ':' + #offsetLimit.offset + ':' + #offsetLimit.limit",
    sync = true,
)
fun findBrawlerTeamResult(...): BrawlerTeamResultCounts
```

기존 `countTeamResultTotalBattleCount` / `countTeamResultBy` / `findBrawlerTeamResultCounts` 3 개 메서드는 제거. enemy / team rank 동일 패턴.

`BattleRankStatisticsRepositoryWithCache` 도 같은 패턴으로 통합.

### 6.3 키 정규화 helper

`core-api/.../application/statistics/StatisticsCacheKeys.kt` (신규):

```kotlin
object StatisticsCacheKeys {
    @JvmStatic
    fun joinIds(ids: List<BrawlStarsId>): String =
        ids.map { it.value }.sorted().joinToString("_")
}
```

`brawlerIds` 정렬은 캐시 키 단계뿐 아니라 **Service 진입 시에도 정렬**한다 (`brawlerIds.sortedBy { it.value }`). 하위 쿼리도 정렬된 입력으로 일관성 유지.

### 6.4 sync = true 적용 범위

- 통합된 무거운 메서드: `findBrawlerTeamResult`, `findBrawlerEnemyResult`, `findBrawlerTeamRank`
- 기존 무거운 메서드: `findBrawlerResultCounts`, `findBrawlerDailyResultCounts`

count 단독·brawler list 캐시는 가벼우므로 미적용.

### 6.5 Service 변경

`BattleResultStatisticsService.getBrawlerTeamResultStatistics` / `BattleRankStatisticsService.getBrawlerTeamRankStatistics` / enemy 메서드가 단일 호출로 변경. 호출 횟수 3 → 1. 응답 DTO 에서 `items`, `totalBattleCount`, `totalElementCount` 분해해 도메인 통계 객체로 매핑.

## 7. I4 — 컨트롤러 입력 검증

### 7.1 Bean Validation

`StatisticsController` 클래스에 `@Validated` 추가, `brawlerIds` 파라미터에 `@Size(max = 3)`:

```kotlin
@RestController
@Validated
class StatisticsController(...) {

    @GetMapping("/v1/statistics/brawler/team/result")
    fun getBrawlerTeamResultStatistics(
        @RequestParam eventId: Long?,
        @RequestParam(required = false, defaultValue = "")
        @Size(max = 3) brawlerIds: List<Long>,
        @RequestParam teamSize: StatsTeamSize,
        ...
    )
}
```

`team/rank` 엔드포인트에도 동일 적용.

### 7.2 Service-level 검증

Service 진입 시 `require(brawlerIds.size <= teamSize.value) { ... }` 명시. `IllegalArgumentException` 의 HTTP 매핑은 기존 GlobalExceptionHandler 정책에 따른다 (없다면 별도 작업, 본 스펙 범위 밖).

### 7.3 ConstraintViolationException 핸들링

기존 GlobalExceptionHandler 가 `ConstraintViolationException` → 400 매핑이 없다면 추가. 있다면 그대로 활용.

## 8. 변경 파일 요약

### 코드

| 파일 | 변경 |
| --- | --- |
| `core/core-enum/.../enums/StatsWeekRange.kt` | `weekRange` 단일 함수 + `currentAndPrevious` 단순화. `currentWeek`/`finalizedWeek` 제거 |
| `core/core-enum/.../enums/StatsDateRange.kt` | `currentWeek` 호출 → `weekRange` |
| `core/core-enum/.../enums/StatsWeekRangeTest.kt` | 케이스 갱신 + 같은 주 동일성 케이스 추가 |
| `core/core-api/.../application/statistics/StatisticsCacheKeys.kt` | 신규 — `joinIds(...)` |
| `core/core-api/.../application/statistics/BrawlerTeamResultCounts.kt` | 신규 DTO |
| `core/core-api/.../application/statistics/BrawlerTeamRankCounts.kt` | 신규 DTO |
| `core/core-api/.../application/statistics/BrawlerEnemyResultCounts.kt` | 신규 DTO |
| `core/core-api/.../application/statistics/BattleResultStatisticsRepositoryWithCache.kt` | 팀/적 통계 단일 메서드, `sync = true`, key helper |
| `core/core-api/.../application/statistics/BattleRankStatisticsRepositoryWithCache.kt` | 팀 rank 통계 단일 메서드 |
| `core/core-api/.../application/statistics/BattleResultStatisticsRepository.kt` | `findBrawlerResultCounts` projection 호출. team / enemy 의 기존 3 분리 메서드는 유지하되 `*RepositoryWithCache` 가 내부에서 합산해 단일 캐시로 묶음 |
| `core/core-api/.../application/statistics/BattleRankStatisticsRepository.kt` | team rank 의 기존 3 분리 메서드 유지. `*RepositoryWithCache` 에서 합산. brawler rank (단일) 는 변경 없음 |
| `core/core-api/.../application/statistics/BattleResultStatisticsService.kt` | brawlerIds 정렬, 단일 호출 |
| `core/core-api/.../application/statistics/BattleRankStatisticsService.kt` | 동일 |
| `core/core-api/.../api/controller/v1/StatisticsController.kt` | `@Validated`, `@Size(max = 3)` |
| `infrastructure/db-core/.../statistics/repository/BrawlerTeamBattleResultStatsJpaRepositoryCustomImpl.kt` | `brawlerIdFilters` EXISTS + HAVING |
| `infrastructure/db-core/.../statistics/repository/BrawlerTeamBattleRankStatsJpaRepositoryCustomImpl.kt` | 동일 |
| `infrastructure/db-core/.../statistics/repository/BrawlerBattleResultStatsJpaRepositoryCustom.kt` | 신규 (없다면) — `findGroupedByBrawler` |
| `infrastructure/db-core/.../statistics/repository/BrawlerBattleResultStatsJpaRepositoryCustomImpl.kt` | 신규 — projection 쿼리 |
| `infrastructure/db-core/.../statistics/repository/BrawlerResultProjection.kt` | 신규 |

### 마이그레이션

| 파일 | 내용 |
| --- | --- |
| `infrastructure/db-core/src/main/resources/db/migration/V260425a__brawler_team_stats_group_by_index.sql` | covering index 2 개 |
| `infrastructure/db-core/src/main/resources/schema.sql` | 동일 인덱스 추가 |

### 테스트

| 파일 | 변경 |
| --- | --- |
| `core-enum/.../StatsWeekRangeTest.kt` | 갱신 |
| `db-core/.../BrawlerTeamBattleResultStatsJpaRepositoryTest` (신규/기존) | brawlerIdFilters EXISTS 정확도 (1/2/3 개 케이스) |
| `db-core/.../BrawlerBattleResultStatsJpaRepositoryTest` (신규/기존) | `findGroupedByBrawler` projection 검증 |
| `core-api/.../BattleResultStatisticsServiceTest` | 단일 캐시 메서드 동작 + brawlerIds 정렬 검증 |
| `core-api/.../StatisticsControllerTest` | `@Size(max = 3)` 위반 시 400 |
| `core-batch/.../PerEventWeeklyBattleStatsTaskletTest` (신규) | 같은 주 안 다른 날짜 3 회 실행 후 row 1 개 검증 |

## 9. 구현 순서

1. **NEW1 핵심**: `StatsWeekRange` 통합 + `StatsDateRange` 호출 갱신 + 테스트. (다른 항목과 독립, 가장 시급)
2. **C3**: 인덱스 마이그레이션 + schema.sql.
3. **C2**: `brawlerIdFilters` EXISTS 통합 (result / rank).
4. **C4**: projection 신규 + `findBrawlerResultCounts` 호출 변경.
5. **I2 + I3 + I1**: 통합 DTO 신규 → `*RepositoryWithCache` 단일 메서드 + key helper + `sync = true` → Service 호출 변경 (한 묶음으로 진행, 부분 적용 시 정합성 더 깨짐).
6. **I4**: Validation 추가.
7. 단계별 `./gradlew ktlintCheck unitTest contextTest` 그린 확인 후 다음으로.

## 10. 위험 / 트레이드오프

- **`StatsWeekRange` 함수명 변경**: 외부 모듈에서 호출하지 않음을 grep 으로 확인했으나, KSP 생성 코드/리플렉션 호출은 별도 점검 필요.
- **`StatsDateRange` 의 `endDate` 의미 변경**: 운영 미배포라 호환성 부담은 없으나 프론트와의 응답 표기 일관성 점검 필요. 본 스펙은 백엔드 단독 변경으로 한정.
- **`@Size(max = 3)` 도입**: ConstraintViolation → HTTP 400 매핑이 글로벌 핸들러에 없다면 별건 작업 필요. 본 스펙에서는 매핑 추가까지만 책임진다 (있으면 활용, 없으면 최소 핸들러 추가).
- **인덱스 추가 비용**: 통계 테이블은 batch write 한정이라 영향 미미.

## 11. 비목표

- D7 Renewal outbox
- I5 페이지네이션 노출
- 외부 분석 보고서 그 외 minor 항목
- core-api 의 다른 도메인 (Battle, Player, Renewal 등) 개선
