# 통계 대상 모드 정리 (스펙 A) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 통계 수집을 코드 enum 허용 목록(12개 모드)으로 제한하고, `battle_event_mode` 의 `result_type`·`player_counts`·`color` 를 없앤다. API 의 `mode.resultType` 은 enum 에서 얻는 nullable 통계 수집 유형이 된다.

**Architecture:** core-enum 의 `StatsEventMode` 가 지원 모드·결과 유형·팀 구성·결과형 슬롯 수(6)의 단일 출처다. core-worker 는 수집 시점에 이 enum 으로 거르고, core-api·core-admin 은 같은 enum 으로 `resultType` 을 채운다. 컬럼 제거는 사용처를 모두 걷어 낸 뒤 db-core 에서 한 번에 한다. 미지원 모드의 새 맵을 놓치지 않도록 `battleEventSyncJob` 에 로테이션 기반 미등록 감지 단계를 더한다.

**Tech Stack:** Kotlin, Spring Boot 4, Spring Batch, Spring Data JPA·QueryDSL, H2(테스트), JUnit5·AssertJ·MockK, Next.js·TypeScript·vitest·MSW, pnpm

**Spec:** `docs/superpowers/specs/2026-11-03-stats-event-mode-whitelist-design.md`

## Global Constraints

- 지원 모드 12개(modeId, 유형, 팀 수×인원)
  - 결과형 2×3: GEM_GRAB 0, HEIST 2, BOUNTY 3, BRAWL_BALL 5, HOT_ZONE 17, KNOCKOUT 20, BASKET_BRAWL 22, BRAWL_HOCKEY 45, BRAWL_ARENA 48
  - 순위형: SOLO_SHOWDOWN 6 (10×1), DUO_SHOWDOWN 9 (5×2), TRIO_SHOWDOWN 38 (4×3)
- `StatsEventMode.RESULT_SLOTS_PER_GAME = 6`
- enum 이름 `BattleEventModeResultType` 과 API 필드 이름 `resultType` 은 그대로 둔다. 바뀌는 것은 nullable 이 되는 것뿐이다. 정의: "통계 수집 유형. 통계를 모으지 않는 모드는 본래 결과형이어도 null".
- 프론트 공용 별칭 `BattleEventModeResultType = "RESULT" | "RANK"` 은 non-null 그대로 둔다. DTO·도메인 필드만 `| null` 로 넓힌다.
- 미지원 안내 문구: `이 모드는 통계를 제공하지 않습니다`
- 프론트 `MODE_COLORS`: 0 `#8F43EA`, 2 `#C763CD`, 3 `#FFC60A`, 5 `#909FDB`, 17 `#FF8A1F`, 20 `#E8893A`, 22 `#62C1F4`, 45 `#D2D2FB`, 6·9·38 `#95D449`. 48 은 넣지 않는다(기본 회색 `#2A2E38`).
- 마이그레이션: `V261103__drop_battle_event_mode_stats_columns.sql`, `drop column if exists` 로 멱등. `schema.sql` 도 함께 고친다.
- 백엔드 규칙(`imstargg-backend/AGENTS.md`)
  - 인자 3개 이상 호출은 named arguments, 파일당 클래스 1개, 로깅은 kotlin-logging
  - 테스트: 한글 메서드명, 대상 변수 `sut`, `// given // when // then`, AssertJ
  - 컨텍스트 테스트 서브클래스에 `@Tag("context")` 를 다시 달지 않는다
- API 를 바꾸면 대응 프론트를 같은 변경에서 고친다(core-api → imstargg-frontend, core-admin → imstargg-admin-frontend).
- 운영 DB 에 쓰지 않고 배포하지 않는다. 마이그레이션 적용·core-admin 재기동·맵 등록(스펙 §3.10)은 배포 담당자의 작업이다.
- 커밋은 Conventional Commits 를 따르고, 메시지 끝에 `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>` 를 붙인다.
- 명령의 기준 디렉터리: 백엔드 `imstargg-backend/`, 프론트 `imstargg-frontend/`, 관리자 `imstargg-admin-frontend/`.

## Review Focus

1. **젬그랩 modeId 0.** 카드 색 조회가 falsy 검사(`if (!modeId)`)를 쓰면 젬그랩이 회색이 된다. → Task 8 에서 `getModeStyle(0)` 테스트로 고정한다.
2. **로테이션 수집 시각의 KST 날짜 경계.** `collected_at` 은 `Instant` 이고 잡 파라미터 `date` 는 KST 날짜다. UTC 로 자르면 KST 00:00~09:00 수집분이 전날로 간다. → Task 7 에 경계 테스트 2개(전날 23:59 KST 제외, 당일 00:00 KST 포함)를 둔다.
3. **하루에 24번 수집되는 같은 미등록 이벤트.** 로테이션은 매시 저장된다. → Task 7 에서 같은 이벤트는 1건만 알리는지 테스트한다.
4. **`mode` 가 null 인 이벤트 상세.** 지금은 결과형으로 처리하지만, 바뀐 뒤에는 안내만 보이고 통계 요청이 나가지 않아야 한다. → Task 8 의 `EventStatsSection` 테스트에 `mode: null` 사례를 넣는다.
5. **미지원 모드 이벤트의 메타 설명.** `generateMetadata` 가 null 을 "결과형" 으로 떨어뜨려도 tsc 가 잡지 못한다. → Task 8 에서 `generateMetadata` 테스트로 고정한다.

---

## File Structure

| 영역 | 파일 | 책임 |
|---|---|---|
| core-enum | `core/core-enum/src/main/kotlin/com/imstargg/core/enums/StatsEventMode.kt` (생성) | 지원 모드 허용 목록·유형·팀 구성·결과형 슬롯 수 |
| core-worker | `core/core-worker/.../application/stats/StatsBattleAggregateApplier.kt` | 수집 필터 |
| core-api | `core/core-api/.../domain/statistics/StatsRate.kt` | 결과형 게임 수 환산(새 함수) |
| core-api | `.../domain/statistics/BrawlerResultStatistics.kt`, `.../application/statistics/BattleResultStatisticsRepository.kt` | 게임 수 분모 사용처 |
| core-api | `.../domain/brawlstars/event/BattleEventMode.kt`, `.../application/brawlstars/BattleEventRepository.kt`, `.../api/controller/v1/response/BattleEventModeResponse.kt` | 모드 응답 |
| core-admin | `request/BattleEventModeCreateRequest.kt`, `response/BattleEventModeResponse.kt`, `controller/BattleEventModeController.kt`, `application/BattleEventService.kt` | 관리자 모드 API |
| db-core | `brawlstars/event/BattleEventModeEntity.kt`, `schema.sql`, `db/migration/V261103__drop_battle_event_mode_stats_columns.sql`(생성) | 컬럼 제거 |
| db-core | `brawlstars/event/repository/BattleEventRotation(Item)JpaRepository.kt`, `statistics/battle/repository/Stats(Rank|Result)BattleJpaRepository.kt` | 로테이션 감지용 조회 메서드 |
| core-batch | `job/brawlstars/task/UnregisteredRotationEventAlertTask.kt`(생성), `job/brawlstars/BattleEventSyncJobConfig.kt` | 로테이션 기반 미등록 감지 |
| frontend | `src/api/types.ts`, `src/domain/event/*`, `src/domain/battle/*`, `app/events/_components/modeStyles.ts`·`RotationCard.tsx`·`SoloRankCard.tsx`, `app/events/[id]/_components/EventStatsSection.tsx`·`EventHero.tsx`, `app/events/[id]/page.tsx` | 타입·색·이벤트 상세 |
| admin-frontend | `lib/types.ts`, `lib/api.ts`, `app/event-modes/page.tsx`, `app/event-modes/[brawlStarsId]/edit/page.tsx` | 관리자 화면 |

백엔드 경로의 `...` 는 각 모듈의 `src/main/kotlin/com/imstargg/core/<모듈명>` 이다. db-core 는 `infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core` 다.

**태스크 순서**: 1 → 2 → 3 → 4 → 5 → 6 → 7 → 8 → 9 → 10.
- 6(컬럼 제거)은 3·4·5 가 엔티티의 세 필드 사용을 모두 걷어 낸 뒤에야 컴파일된다.
- 8 은 4 의 API 형태를 따른다. 9 는 5 의 API 형태를 따른다.

---

### Task 1: `StatsEventMode` 허용 목록 enum

**Files:**
- Create: `imstargg-backend/core/core-enum/src/main/kotlin/com/imstargg/core/enums/StatsEventMode.kt`
- Test: `imstargg-backend/core/core-enum/src/test/kotlin/com/imstargg/core/enums/StatsEventModeTest.kt`

**Interfaces:**
- Produces:
  - `enum class StatsEventMode(val modeBrawlStarsId: Long, val resultType: BattleEventModeResultType, val teamCount: Int, val teamSize: Int)`
  - 항목: Global Constraints 의 12개. 이름도 그대로다.
  - `companion object { const val RESULT_SLOTS_PER_GAME = 6; fun find(modeBrawlStarsId: Long): StatsEventMode? }`

- [ ] **Step 1: 실패하는 테스트 작성** — 태그 없는 단위 테스트. `StatsTierRangeTest` 처럼 `@Nested inner class Find` 와 테스트 클래스 바로 아래 불변식 테스트로 나눈다.

```kotlin
@Nested
inner class Find {
    @Test
    fun `지원 모드 ID 면 해당 항목을 반환한다`() {
        assertThat(StatsEventMode.find(0L)).isEqualTo(StatsEventMode.GEM_GRAB)
        assertThat(StatsEventMode.find(45L)).isEqualTo(StatsEventMode.BRAWL_HOCKEY)
        assertThat(StatsEventMode.find(38L)).isEqualTo(StatsEventMode.TRIO_SHOWDOWN)
    }

    @Test
    fun `미지원 모드 ID 면 null 을 반환한다`() {
        // 24 태그팀/듀얼, 25 와이프아웃, 31 와이프아웃 5v5, 57 BRAWL HOCKEY(로테이션에 나온 적 없음)
        listOf(24L, 25L, 31L, 57L).forEach { assertThat(StatsEventMode.find(it)).isNull() }
    }
}

@Test
fun `modeBrawlStarsId 는 중복되지 않는다`() {
    val ids = StatsEventMode.entries.map { it.modeBrawlStarsId }
    assertThat(ids).doesNotHaveDuplicates()
}

@Test
fun `결과형 항목의 1게임당 슬롯 수는 RESULT_SLOTS_PER_GAME 과 같다`() {
    val result = StatsEventMode.entries.filter { it.resultType == BattleEventModeResultType.RESULT }
    assertThat(result).hasSize(9)
    assertThat(result).allSatisfy {
        assertThat(it.teamCount * it.teamSize).isEqualTo(StatsEventMode.RESULT_SLOTS_PER_GAME)
    }
}

@Test
fun `순위형 항목은 솔로·듀오·트리오 쇼다운이다`() {
    val rank = StatsEventMode.entries.filter { it.resultType == BattleEventModeResultType.RANK }
    assertThat(rank.map { Triple(it.modeBrawlStarsId, it.teamCount, it.teamSize) })
        .containsExactlyInAnyOrder(Triple(6L, 10, 1), Triple(9L, 5, 2), Triple(38L, 4, 3))
}
```

- [ ] **Step 2: 실패 확인** — `./gradlew :core:core-enum:test --tests "com.imstargg.core.enums.StatsEventModeTest"` → `Unresolved reference: StatsEventMode` 로 컴파일 실패.

- [ ] **Step 3: `StatsEventMode` 구현**
  - 항목은 named arguments 로 쓴다. 예: `GEM_GRAB(modeBrawlStarsId = 0, resultType = BattleEventModeResultType.RESULT, teamCount = 2, teamSize = 3)`.
  - `find` 는 `entries.find { it.modeBrawlStarsId == modeBrawlStarsId }` 다. `SoloRankTier.of` 와 같은 관용구다.
  - 클래스 KDoc 에 스펙 §3.1 의 결정 이력과 모드 추가 절차(①~④)를 적는다.
    - 결정 이력: 핵심 6이 결과형의 91%, 세 모드를 더하면 약 98%. 하키·아레나·농구를 넣은 이유와 와이프아웃·태그팀/듀얼·5v5 를 뺀 이유. mode 문자열 불신(45→`brawlBall`, 48→`siege`). `player_counts` 미관리. 듀얼 펼침.
  - `RESULT_SLOTS_PER_GAME` KDoc 에는 "결과형 항목이 모두 2×3 이라는 전제(StatsEventModeTest 가 고정)" 를 적는다.

- [ ] **Step 4: 통과 확인** — 같은 명령으로 PASS 를 본다. `./gradlew :core:core-enum:ktlintCheck` 도 통과해야 한다.

- [ ] **Step 5: 커밋** — `feat(core-enum): 통계 수집 모드 허용 목록 StatsEventMode 를 추가한다`

---

### Task 2: 수집 필터 (core-worker)

**Files:**
- Modify: `imstargg-backend/core/core-worker/src/main/kotlin/com/imstargg/core/worker/application/stats/StatsBattleAggregateApplier.kt` (L57-86)
- Test: `imstargg-backend/core/core-worker/src/test/kotlin/com/imstargg/core/worker/application/stats/StatsBattleAggregateApplierTest.kt`
- Modify: `imstargg-backend/docs/stats-battle-data-strategy.md` (§3 데이터 흐름·수집 프로세스)

**Interfaces:**
- Consumes: `StatsEventMode.find(modeBrawlStarsId: Long): StatsEventMode?`, `teamCount`, `teamSize`, `resultType` (Task 1)
- Produces: 없음(내부 변경). `canCreateStatsBattle(battle): Boolean` 을 `private fun findStatsEventMode(battle: Battle): StatsEventMode?` 로 바꾼다.

- [ ] **Step 1: 기존 픽스처를 지원 모드로 바꾸고 헬퍼를 넓힌다** (테스트 파일)
  - `createRankBattle`
    - L137 의 `modeBrawlStarsId = 2L` 을 `6L` 로 바꾼다.
    - 파라미터를 추가한다(모두 기본값): `modeBrawlStarsId: Long = 6L`, `teams = createSoloTeams()`, `battleRank: Int? = 1`, `result: String? = null`.
  - `createResultBattle`
    - L163 의 `modeBrawlStarsId = 1L` 을 `20L` 로 바꾼다.
    - 파라미터를 추가한다: `modeBrawlStarsId: Long = 20L`, `mode: String = "knockout"`, `battleRank: Int? = null`, `result: String? = "victory"`.
    - `Battle.mode` 와 `BattleEvent.modeBrawlStarsName` 에 같은 `mode` 를 넣는다.
  - 새 헬퍼 `createTeams(teamCount: Int, teamSize: Int): List<List<BattleTeamPlayer>>`
    - 플레이어마다 태그와 브롤러 ID 가 고유해야 한다. 브롤러 ID 는 전투 안에서 고유해야 중복 검사에 먼저 걸리지 않는다.
    - `trophies = 2000` 으로 둔다.
  - 새 헬퍼 `createDuelTeams()`
    - 2팀이고, 팀마다 같은 태그로 브롤러 3개(브롤러 ID 는 6개 모두 고유)를 둔다. `BattleBuilder` 가 듀얼을 펼친 모양이다.
  - 기존 테스트 3개가 그대로 통과하는지 확인한다: `./gradlew :core:core-worker:test --tests "*StatsBattleAggregateApplierTest"`

- [ ] **Step 2: 실패하는 테스트 작성** — `@Nested inner class \`apply 수집 모드\``. battleKey 는 테스트마다 고유하게, battleAt 은 `clock` 기준 7일 이내로 둔다. 단언은 저장소의 `count()` 로 한다.

```kotlin
@Test
fun `허용 목록 밖 모드의 결과형 전투는 저장하지 않는다`() {
    sut.apply(listOf(createResultBattle(battleKey = "k1", battleAt = recent, playerBrawlStarsTag = "#A1", modeBrawlStarsId = 25L, mode = "wipeout")))
    assertThat(statsResultBattleRepository.count()).isZero()
}

@Test
fun `듀얼은 2x3 으로 펼쳐져도 저장하지 않는다`() {
    sut.apply(listOf(createResultBattle(..., modeBrawlStarsId = 24L, mode = "duels", teams = createDuelTeams())))
    assertThat(statsResultBattleRepository.count()).isZero()
}

@Test
fun `mode 문자열이 아니라 modeId 로 판정한다`() {
    sut.apply(listOf(
        createResultBattle(battleKey = "hockey", ..., modeBrawlStarsId = 45L, mode = "brawlBall"),
        createResultBattle(battleKey = "wipeout", ..., modeBrawlStarsId = 25L, mode = "gemGrab"),
    ))
    assertThat(statsResultBattleRepository.findAll().map { it.eventModeBrawlStarsId }).containsExactly(45L)
}

@Test
fun `핵심 모드라도 팀 구성이 다르면 저장하지 않는다`() {
    sut.apply(listOf(createResultBattle(..., modeBrawlStarsId = 20L, teams = createTeams(teamCount = 2, teamSize = 2))))
    assertThat(statsResultBattleRepository.count()).isZero()
}

@Test
fun `결과형 모드의 순위 전투는 순위형으로 저장하지 않는다`() {
    sut.apply(listOf(createRankBattle(..., modeBrawlStarsId = 20L, teams = createTeams(teamCount = 2, teamSize = 3), battleRank = 1)))
    assertThat(statsRankBattleRepository.count()).isZero()
}

@Test
fun `쇼다운의 결과 전투는 결과형으로 저장하지 않는다`() {
    sut.apply(listOf(createRankBattle(..., modeBrawlStarsId = 6L, battleRank = null, result = "victory")))
    assertThat(statsResultBattleRepository.count()).isZero()
}

@Test
fun `솔로·듀오·트리오 쇼다운은 순위형으로 저장된다`() {
    sut.apply(listOf(
        createRankBattle(battleKey = "solo", ..., modeBrawlStarsId = 6L, teams = createTeams(teamCount = 10, teamSize = 1)),
        createRankBattle(battleKey = "duo", ..., modeBrawlStarsId = 9L, teams = createTeams(teamCount = 5, teamSize = 2)),
        createRankBattle(battleKey = "trio", ..., modeBrawlStarsId = 38L, teams = createTeams(teamCount = 4, teamSize = 3)),
    ))
    assertThat(statsRankBattleRepository.findAll().map { it.eventModeBrawlStarsId }).containsExactlyInAnyOrder(6L, 9L, 38L)
}
```

`...` 자리는 기존 헬퍼의 필수 인자(battleKey·battleAt·playerBrawlStarsTag)다. `recent` 는 `clock.instant().minus(Duration.ofHours(1))` 같은 7일 이내 시각이다.

- [ ] **Step 3: 실패 확인** — 같은 명령. 와이프아웃·듀얼·2×2·유형 불일치 사례가 저장되어 FAIL 이 난다.

- [ ] **Step 4: 필터 구현**
  - `private fun findStatsEventMode(battle: Battle): StatsEventMode?`
    - 기존 `canCreateStatsBattle` 의 조건을 그대로 거친다. 실패하면 null 이다.
    - 이어서 `StatsEventMode.find(event.modeBrawlStarsId)` 를 조회한다. L59 의 null 검사 뒤라 `Long` 으로 스마트 캐스트된다.
    - 마지막으로 `battle.teams.size == teamCount && battle.teams.all { it.size == teamSize }` 를 검사하고, 통과한 항목을 돌려준다.
  - `canCreateRankBattle`: `battle.battleRank != null && findStatsEventMode(battle)?.resultType == BattleEventModeResultType.RANK`
  - `canCreateResultBattle`: `battle.result != null && findStatsEventMode(battle)?.resultType == BattleEventModeResultType.RESULT`
  - `battle.mode == null` 검사(L60)는 남긴다. 저장 필드가 non-null 이기 때문이다. 판정에는 쓰지 않는다.

- [ ] **Step 5: 통과 확인** — 같은 명령으로 PASS(기존 3 + 신규 7). `./gradlew :core:core-worker:ktlintCheck` 도 통과.

- [ ] **Step 6: 문서 갱신** — `imstargg-backend/docs/stats-battle-data-strategy.md` §3 을 고친다.
  - 데이터 흐름과 수집 프로세스의 조건 목록을 실제 순서로 다시 쓴다.
    1. 7일 이내(`StatsBattleAggregateApplier.BATTLE_MAX_AGE_DAYS`)
    2. 이벤트·맵·mode·type 이 있어야 한다
    3. 친선전 제외
    4. 브롤러 중복 없음
    5. 최저 트로피(1000) 또는 경쟁전 신화 이상
    6. `StatsEventMode` 허용 목록(modeId)
    7. 팀 구성 일치
    8. 유형과 경로 일치
  - 없는 `StatsBattleHelper` 언급과 `BattleEntity` 원천 표기를 바로잡는다.

- [ ] **Step 7: 커밋** — `feat(core-worker): 통계 전투를 허용 모드와 팀 구성이 맞을 때만 수집한다`

---

### Task 3: 결과형 게임 수를 상수로 계산 (core-api)

**Files:**
- Modify: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/domain/statistics/StatsRate.kt`
- Modify: `.../domain/statistics/BrawlerResultStatistics.kt` (`create`, L23-70)
- Modify: `.../application/statistics/BattleResultStatisticsRepository.kt` (생성자 L25-30, L57-58, L181-198)
- Delete: `.../domain/statistics/GameSlot.kt`, `.../domain/statistics/GameSlots.kt`, `.../application/statistics/GameSlotRepository.kt`
- Delete tests: `core/core-api/src/test/kotlin/com/imstargg/core/domain/statistics/GameSlotTest.kt`, `GameSlotsTest.kt`, `core/application/statistics/GameSlotRepositoryTest.kt`
- Create test: `core/core-api/src/test/kotlin/com/imstargg/core/domain/statistics/StatsRateTest.kt`
- Modify tests: `core/domain/statistics/BrawlerResultStatisticsTest.kt`, `core/application/statistics/BattleResultStatisticsRepositoryTest.kt`
- Modify KDoc: `infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/BrawlerBattleResultStatsEntity.kt` (L29-35), `BrawlerBattleRankStatsEntity.kt` (L29-31)

**Interfaces:**
- Consumes: `StatsEventMode.RESULT_SLOTS_PER_GAME` (Task 1)
- Produces:
  - `StatsRate.resultGameCount(resultSlotSum: Long): Long` (= `resultSlotSum / RESULT_SLOTS_PER_GAME`, 정수 나눗셈)
  - `BrawlerResultStatistics.create(rows: List<BrawlerEventResultCount>): BrawlerResultStatistics`

- [ ] **Step 1: 실패하는 테스트 작성**
  - `StatsRateTest`(태그 없음) 에 `@Nested inner class ResultGameCount` 를 둔다.
    - `결과형 슬롯 합을 6 으로 나눈 값이다`: `assertThat(StatsRate.resultGameCount(900)).isEqualTo(150)`
    - `나머지는 버린다`: `resultGameCount(905)` → 150
    - `0 이면 0 이다`
  - `BrawlerResultStatisticsTest.Create`
    - `다중 이벤트 mix 시 게임수 분모는 이벤트별 환산 합` 과 `GameSlots 에 누락된 이벤트는 default 로 폴백` 은 지운다.
    - `단일 3v3 이벤트의 픽률은 게임 수 분모로 계산된다` 와 `빈 입력은 totalBattleCount 0` 은 `create(rows)` 로 부르게 고친다. 기대값(150, pickRate 2.0)은 그대로다.
    - 새 테스트 `여러 이벤트의 슬롯 합을 6 으로 나눈 값이 게임 수다`: 이벤트 A 600 슬롯 + 이벤트 B 300 슬롯이면 `assertThat(sut.totalBattleCount).isEqualTo(150)` 이다.

- [ ] **Step 2: 실패 확인** — `./gradlew :core:core-api:test --tests "com.imstargg.core.domain.statistics.StatsRateTest" --tests "com.imstargg.core.domain.statistics.BrawlerResultStatisticsTest"` → `resultGameCount` 와 `create(rows)` 미정의로 컴파일 실패.

- [ ] **Step 3: 구현**
  - `StatsRate.resultGameCount` 를 추가한다. KDoc 에 세 가지를 적는다.
    - `GameSlots.totalGameCount` 의 결정 이력: "Pair/Enemy 분모도 Result 슬롯 합 기준, 같은 게임 수가 보장되므로 Result 테이블이 단일 출처"
    - "2026-11 스펙 A 로 이벤트별 내림 후 합산에서 합산 후 한 번 나누기로 바꿨다(지원 결과형이 모두 2×3)"
    - `RESULT_SLOTS_PER_GAME` 링크
  - `pickRate` KDoc 의 "[GameSlots.totalGameCount] 참조" 를 `[resultGameCount]` 로 고친다.
  - `BrawlerResultStatistics.create(rows)`: `totalBattleCount = StatsRate.resultGameCount(rows.sumOf { it.resultCount.totalCount })`
  - `BattleResultStatisticsRepository`
    - `gameSlotRepository` 생성자 의존성을 지운다.
    - L57-58 은 `BrawlerResultStatistics.create(rows = rows)` 가 된다.
    - `computeTotalGameCount` 는 `findEventTotalSlotSums(...)` 결과의 `totalSlotCount` 합을 `StatsRate.resultGameCount` 에 넘긴다.
    - L181 KDoc 은 `[StatsRate.resultGameCount]` 를 가리키게 고친다.
  - `GameSlot.kt`·`GameSlots.kt`·`GameSlotRepository.kt` 와 그 테스트 3개를 지운다.
  - `BattleResultStatisticsRepositoryTest`
    - `save3v3Event` 헬퍼와 `battleEventJpaRepository`·`battleEventModeJpaRepository` 주입을 지운다. sut 가 이제 통계 테이블만 읽기 때문이다.
    - 호출부(L57·78·118·162·201·227)는 `BrawlStarsId(eventId)` 를 바로 쓴다. 기대값은 바뀌지 않는다(모두 3v3).
  - db-core 두 엔티티 KDoc 의 `BattleEventMode.playerCounts`·`GameSlot(...)` 언급을 "결과형 지원 모드는 모두 2×3 이라 게임 수 = 슬롯 합 ÷ `StatsEventMode.RESULT_SLOTS_PER_GAME`(순위형은 본인 팀만 집계해 도출 불가)" 로 고친다.

- [ ] **Step 4: 통과 확인**
  - `./gradlew :core:core-api:test --tests "com.imstargg.core.domain.statistics.*" --tests "com.imstargg.core.application.statistics.*"`
  - `StatisticsCacheKeyTest` 포함 PASS 를 본다. 이어서 `./gradlew :core:core-api:ktlintCheck`.

- [ ] **Step 5: 커밋** — `refactor(core-api): 결과형 게임 수를 슬롯 합 ÷ 6 으로 계산하고 GameSlot 을 없앤다`

---

### Task 4: 이벤트 모드 응답 (core-api)

**Files:**
- Modify: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/domain/brawlstars/event/BattleEventMode.kt`
- Modify: `.../application/brawlstars/BattleEventRepository.kt` (L84-91, L109-116)
- Modify: `.../api/controller/v1/response/BattleEventModeResponse.kt`
- Test: `core/core-api/src/test/kotlin/com/imstargg/core/application/brawlstars/BattleEventServiceTest.kt`
- Create test: `core/core-api/src/test/kotlin/com/imstargg/core/api/controller/v1/BattleEventControllerTest.kt`

**Interfaces:**
- Consumes: `StatsEventMode.find`, `resultType` (Task 1)
- Produces:
  - 도메인 `data class BattleEventMode(val id: BrawlStarsId, val name: Messages, val iconImageUrl: String?, val resultType: BattleEventModeResultType?)`
  - 응답 `data class BattleEventModeResponse(val id: Long, val name: MessagesResponse, val iconImageUrl: String?, val resultType: BattleEventModeResultType?)` (`color` 제거)
  - JSON: `"resultType": "RESULT" | "RANK" | null`, `color` 키 없음. Task 8 이 이 형태를 쓴다.

- [ ] **Step 1: 실패하는 테스트 작성**
  - `BattleEventServiceTest`
    - 기존 `@BeforeEach`(cleanUp·BATTLE_EVENT 캐시 비우기)를 그대로 쓴다.
    - 모드는 `aBattleEventModeEntity().withBrawlStarsId(...).build()` 로 저장하고, 이벤트는 `aBattleEventEntity().withMode(mode)` 로 저장한다.
    - `get 은 지원 모드 이벤트의 resultType 을 통계 수집 유형으로 채운다`: 모드 0 → `sut.get(BrawlStarsId(eventId)).mode?.resultType` 이 `RESULT`, 모드 6 → `RANK`
    - `get 은 미지원 모드 이벤트의 resultType 을 null 로 채운다`: 모드 25 → `isNull()`
    - `search 도 같은 규칙으로 resultType 을 채운다`: `sut.search(맵 이름)` 결과에서 모드 45 → `RESULT`, 모드 24 → null. `toBattleEvent` 경로를 덮는다.
  - `BattleEventControllerTest`
    - `StatisticsControllerTest` 와 같은 패턴(`CoreApiContextTest` + `@AutoConfigureMockMvc` + `@MockkBean(relaxed = true) BattleEventService`)을 쓴다.
    - `미지원 모드 이벤트는 resultType null 을 내보내고 color 를 내보내지 않는다`: `every { battleEventService.get(any()) }` 로 `resultType = null` 인 모드의 `BattleEvent` 를 돌려주고 `mockMvc.get("/v1/events/{id}", ...)` 를 부른다.
    - 단언: `content { string(containsString("\"resultType\":null")) }` 와 `string(not(containsString("\"color\"")))`. 운영 웹 mapper(Jackson 3)를 거치는지 보는 테스트다. Jackson 2 `ObjectMapper()` 로 직렬화하지 않는다.

- [ ] **Step 2: 실패 확인** — `./gradlew :core:core-api:test --tests "com.imstargg.core.application.brawlstars.BattleEventServiceTest" --tests "com.imstargg.core.api.controller.v1.BattleEventControllerTest"` → 모드 25 가 `RESULT` 로 나오고 `color` 가 직렬화되어 FAIL.

- [ ] **Step 3: 구현**
  - 도메인 `BattleEventMode` 에서 `playerCounts`·`color`·`DEFAULT_PLAYER_COUNTS` 를 지운다. `resultType` 은 nullable 로 바꾸고 KDoc 에 Global Constraints 의 정의를 적는다.
  - `BattleEventRepository` 의 두 생성 지점에서 세 줄을 `resultType = StatsEventMode.find(modeEntity.brawlStarsId)?.resultType` 한 줄로 바꾼다.
  - `BattleEventModeResponse` 에서 `color` 를 지우고 `resultType` 을 nullable 로 바꾼다. KDoc 에 같은 정의를 적는다.

- [ ] **Step 4: 통과 확인** — 같은 명령으로 PASS. 이어서 `./gradlew :core:core-api:test`(전체)와 `./gradlew :core:core-api:ktlintCheck`.

- [ ] **Step 5: 커밋** — `feat(core-api): 이벤트 모드 resultType 을 통계 수집 유형으로 바꾸고 color 를 응답에서 뺀다`

---

### Task 5: 관리자 모드 API (core-admin)

**Files:**
- Modify: `imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/request/BattleEventModeCreateRequest.kt`
- Delete: `.../request/BattleEventModeUpdateRequest.kt`, `.../request/BattleEventModeColorUpdateRequest.kt`
- Modify: `.../response/BattleEventModeResponse.kt`, `.../controller/BattleEventModeController.kt` (L54-70 제거), `.../application/BattleEventService.kt` (L71-73, L84-86, L92-113)
- Test: `imstargg-backend/core/core-admin/src/test/kotlin/com/imstargg/core/admin/application/BattleEventServiceTest.kt`

**Interfaces:**
- Consumes: `StatsEventMode.find` (Task 1)
- Produces:
  - `data class BattleEventModeCreateRequest(val brawlStarsId: Long, val brawlStarsName: String, val names: Map<Language, String> = emptyMap())`
  - 관리자 응답 `BattleEventModeResponse(brawlStarsId, brawlStarsName, names, iconImageUrl, resultType: BattleEventModeResultType?)`
  - 남는 엔드포인트: `GET/POST /event-modes`, `PUT /event-modes/{id}/messages`, `PUT /event-modes/{id}/image`. Task 9 가 이 형태를 쓴다.

- [ ] **Step 1: 실패하는 테스트 작성**
  - 새 테스트 `모드 목록의 resultType 은 통계 수집 유형이다`
    - 모드 0·6·24 를 `battleEventModeJpaRepository.save(...)` 로 저장한다.
    - 단언: `assertThat(sut.findAllModes().associate { it.brawlStarsId to it.resultType }).containsExactlyInAnyOrderEntriesOf(mapOf(0L to RESULT, 6L to RANK, 24L to null))`
  - `이벤트 모드 등록 시 이름_인원수_색상이 함께 저장된다` 는 `이벤트 모드 등록 시 이름이 함께 저장된다` 로 바꾼다. 요청에서 세 인자를 빼고, 인원·색 단언(L78-79)을 지운다.
  - `이벤트 모드 등록 시 잘못된 색상이면 예외가 발생한다` 는 지운다.

- [ ] **Step 2: 실패 확인** — `./gradlew :core:core-admin:test --tests "com.imstargg.core.admin.application.BattleEventServiceTest"` → 생성 요청에 없는 인자로 컴파일 실패, 또는 모드 24 가 RESULT 라 FAIL.

- [ ] **Step 3: 구현**
  - `findAllModes` 매핑의 세 줄을 `resultType = StatsEventMode.find(mode.brawlStarsId)?.resultType` 로 바꾼다.
  - `createMode` 는 엔티티 생성에서 세 인자를 뺀다. 엔티티 기본값으로 들어간다.
  - `updateMode`·`updateModeColor` 와 컨트롤러의 PATCH·color PUT, 두 요청 DTO 파일을 지운다.
  - 생성 요청의 "엔티티 기본값과 일치" 주석을 지운다.
  - 쓰이지 않게 된 import(`PatchMapping`, 지운 DTO 들)를 정리한다.

- [ ] **Step 4: 통과 확인** — 같은 명령과 `./gradlew :core:core-admin:test :core:core-admin:ktlintCheck`.

- [ ] **Step 5: 커밋** — `feat(core-admin): 모드 유형·인원·색 편집 API 를 없애고 resultType 을 허용 목록에서 채운다`

---

### Task 6: `battle_event_mode` 컬럼 제거 (db-core)

**Files:**
- Modify: `imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/brawlstars/event/BattleEventModeEntity.kt`
- Modify: `infrastructure/db-core/src/testFixtures/kotlin/com/imstargg/infrastructure/db/core/test/fixture/BattleEventModeEntityFixture.kt`
- Delete: `infrastructure/db-core/src/test/kotlin/com/imstargg/infrastructure/db/core/brawlstars/event/BattleEventModeEntityTest.kt`
- Modify: `infrastructure/db-core/src/main/resources/schema.sql` (L364-366)
- Create: `infrastructure/db-core/src/main/resources/db/migration/V261103__drop_battle_event_mode_stats_columns.sql`

**Interfaces:**
- Consumes: Task 3·4·5 가 엔티티의 `resultType`·`playerCounts`·`color` 사용을 모두 걷어 낸 상태
- Produces: `class BattleEventModeEntity(val brawlStarsId: Long, val brawlStarsName: String)`. 픽스처는 `withBrawlStarsId`·`withBrawlStarsName` 만 남는다.

- [ ] **Step 1: 엔티티·픽스처 정리**
  - 엔티티에서 지울 것: 세 필드(L38-68), `init {}`(L70-73), `update*` 3개, `validate*` 2개, `DEFAULT_PLAYER_COUNTS`·`DEFAULT_COLOR`·`COLOR_REGEX`. 이어서 쓰이지 않는 import(`BattleEventModeResultType`·`EnumType`·`Enumerated`·`JdbcTypeCode`·`SqlTypes`)를 지운다.
  - `UNKNOWN_NAME` 은 남긴다.
  - 픽스처의 `withResultType`·`withPlayerCounts`·`withColor` 와 `build()` 의 세 인자를 지운다.
  - `BattleEventModeEntityTest` 를 지운다(6개 테스트 모두 지워진 멤버만 검사한다).

- [ ] **Step 2: 전 모듈 컴파일 확인** — `./gradlew compileTestKotlin` → BUILD SUCCESSFUL. 남은 사용처가 있으면 여기서 드러난다.

- [ ] **Step 3: 스키마·마이그레이션**
  - `schema.sql` 의 `battle_event_mode` 정의에서 세 컬럼 줄을 지운다.
  - 마이그레이션 파일은 `V261102b__add_status_to_stats_player.sql` 의 머리말 형식을 따른다.
    - 대상: `battle_event_mode`
    - 배경: 스펙 A 결정 5
    - 배포 순서: 앱 배포 → DDL. 구버전 앱은 DDL 뒤 조회가 실패한다(결정 10). core-admin 은 Deploy 워크플로 밖이라 DDL 뒤 새 코드로 다시 띄운다.
    - 적용: `lock_timeout 3s`
    - 멱등: `drop column if exists`
  - 본문은 아래와 같다.

```sql
set lock_timeout = '3s';

alter table battle_event_mode
    drop column if exists result_type,
    drop column if exists player_counts,
    drop column if exists color;
```

- [ ] **Step 4: 마이그레이션 멱등 확인** (테스트 DB 가 H2 라 테스트로는 검증되지 않는다)
  - `docker run -d --rm --name spec-a-pg -e POSTGRES_PASSWORD=pg postgres:17-alpine` 로 띄운다.
  - 변경 전 정의로 테이블을 만든다. 아직 커밋 전이라 `HEAD` 가 변경 전 상태다. `git show HEAD:imstargg-backend/infrastructure/db-core/src/main/resources/schema.sql` 에서 `battle_event_mode` 정의를 추출한다.
  - 마이그레이션을 `psql` 로 **두 번** 적용한다. 두 번 모두 오류가 없어야 한다.
  - `\d battle_event_mode` 에 세 컬럼이 없어야 한다. 확인이 끝나면 `docker stop spec-a-pg`.

- [ ] **Step 5: 테스트 확인** — `./gradlew :infrastructure:db-core:test :core:core-batch:test :core:core-admin:test` 와 `./gradlew ktlintCheck`.

- [ ] **Step 6: 커밋** — `refactor(db-core): battle_event_mode 의 result_type·player_counts·color 컬럼을 없앤다`

---

### Task 7: 로테이션 기반 미등록 이벤트 감지 (core-batch)

**Files:**
- Create: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/brawlstars/task/UnregisteredRotationEventAlertTask.kt`
- Modify: `.../job/brawlstars/BattleEventSyncJobConfig.kt` (스텝 추가)
- Modify: `.../job/brawlstars/BattleEventSyncItemWriter.kt` (`createSoloRankEvents` KDoc)
- Modify (db-core repository):
  - `brawlstars/event/repository/BattleEventRotationJpaRepository.kt`
  - `brawlstars/event/repository/BattleEventRotationItemJpaRepository.kt`
  - `statistics/battle/repository/StatsRankBattleJpaRepository.kt`
  - `statistics/battle/repository/StatsResultBattleJpaRepository.kt`
- Test: `imstargg-backend/core/core-batch/src/test/kotlin/com/imstargg/core/batch/job/brawlstars/task/UnregisteredRotationEventAlertTaskTest.kt`

**Interfaces:**
- Produces:
  - `BattleEventRotationJpaRepository.findAllByCollectedAtGreaterThanEqualAndCollectedAtLessThan(start: Instant, end: Instant): List<BattleEventRotationEntity>`
  - `BattleEventRotationItemJpaRepository.findAllByBattleEventRotationIn(rotations: Collection<BattleEventRotationEntity>): List<BattleEventRotationItemEntity>`
  - `StatsRankBattleJpaRepository.existsByBattleDateAndEventBrawlStarsId(battleDate: LocalDate, eventBrawlStarsId: Long): Boolean`. `StatsResultBattleJpaRepository` 도 같은 시그니처다. 인덱스 `(battle_date, event_brawlstars_id)` 를 탄다.
  - `@Component class UnregisteredRotationEventAlertTask(alertManager: AlertManager, battleEventJpaRepository: BattleEventJpaRepository, battleEventRotationJpaRepository: BattleEventRotationJpaRepository, battleEventRotationItemJpaRepository: BattleEventRotationItemJpaRepository, statsRankBattleJpaRepository: StatsRankBattleJpaRepository, statsResultBattleJpaRepository: StatsResultBattleJpaRepository)` 와 `fun alert(date: LocalDate)`
  - `BattleEventSyncJobConfig.STEP_3_NAME = "battleEventSyncFromRotationStep"`

- [ ] **Step 1: 실패하는 테스트 작성**
  - 베이스는 `CoreBatchContextTest`, 정리는 `@AfterEach cleanUp.all()` 이다.
  - `BattleEventSyncItemWriterTest` 처럼 `alertManager = mockk<AlertManager>(relaxUnitFun = true)` 를 두고, 주입받은 저장소로 sut 를 직접 만든다.
  - 데이터: 로테이션은 `aBattleEventRotationEntity().withCollectedAt(...)`, 항목은 `aBattleEventRotationItemEntity().withBattleEventRotation(...).withEventBrawlStarsId(...).withEventModeBrawlStarsId(...).withEventMapName(...)` 로 저장한다. 시각은 `OffsetDateTime.parse("2026-11-02T12:00+09:00").toInstant()` 형식이다. `date = LocalDate.of(2026, 11, 2)`.

```kotlin
@Test
fun `그날 로테이션에 나온 미등록 이벤트를 알린다`() {
    // given: 11-02 12:00 KST 수집, 이벤트 15009999 (모드 25)
    // when
    sut.alert(date)
    // then
    val alert = slot<AlertCommand>()
    verify(exactly = 1) { alertManager.alert(capture(alert)) }
    assertThat(alert.captured.title).contains("미등록 배틀 이벤트")
    assertThat(alert.captured.content).contains("15009999")
}

@Test
fun `등록된 이벤트는 알리지 않는다`()          // battle_event 에 15009999 저장 → verify(exactly = 0)

@Test
fun `같은 날 통계 전투에 나온 이벤트는 전투 단계가 알리므로 알리지 않는다`()
// StatsRankBattleEntity(battleDate 11-02, eventBrawlStarsId 15009999)를
// BattleEventSyncItemWriterTest 의 soloRankBattle 헬퍼와 같은 방식으로 저장 → verify(exactly = 0)

@Test
fun `하루에 여러 번 수집된 같은 이벤트는 한 번만 알린다`()  // 같은 날 로테이션 3개에 같은 이벤트 → exactly = 1

@Test
fun `KST 전날 23시 59분에 수집된 로테이션은 보지 않는다`()   // 2026-11-01T23:59+09:00 → exactly = 0

@Test
fun `KST 당일 0시에 수집된 로테이션은 본다`()               // 2026-11-02T00:00+09:00 → exactly = 1
```

- [ ] **Step 2: 실패 확인** — `./gradlew :core:core-batch:test --tests "com.imstargg.core.batch.job.brawlstars.task.UnregisteredRotationEventAlertTaskTest"` → Task·저장소 메서드 미정의로 컴파일 실패.

- [ ] **Step 3: 구현**
  - 저장소 메서드 4개를 추가한다(Spring Data 파생 쿼리, JPQL 없음).
  - `alert(date)`
    1. 범위는 `start = date.atStartOfDay(KOREA_ZONE).toInstant()`, `end = date.plusDays(1).atStartOfDay(KOREA_ZONE).toInstant()` 다. `KOREA_ZONE` 은 db-core `KoreaDateTimeExtensions.kt` 의 것을 쓴다.
    2. 범위 안의 로테이션을 찾고, 그 항목을 `eventBrawlStarsId` 로 묶어 마지막 항목 하나만 남긴다.
    3. `battleEventJpaRepository.findAllByBrawlStarsIdIn` 결과에 있는 ID 는 뺀다.
    4. 그날 순위형·결과형 통계 전투에 같은 이벤트가 있으면(`existsBy...`) 뺀다.
    5. 남은 이벤트마다 알린다. title 은 `"[${BattleEventSyncJobConfig.JOB_NAME}] 미등록 배틀 이벤트 감지"`, content 는 `eventBrawlStarsId=…`, `modeBrawlStarsId=…`, `map=…` 세 줄이다(nullable 값은 그대로 `null` 로 찍는다). `log.warn` 도 함께 남긴다.
  - 항목의 LAZY `battleEventRotation` 은 따라가지 않는다. 이 스텝은 트랜잭션 없이 돈다.
  - 4번 단계를 두는 이유: 라이터의 `checkedEventIds` 는 `@StepScope` 라이터마다 따로 있어 다른 스텝과 공유되지 않는다. 대신 "그날 통계 전투에 나온 미등록 이벤트는 전투 단계가 이미 알린다" 는 사실로 중복을 피한다. 두 단계를 합쳐 이벤트당 1건이 된다.
  - `BattleEventSyncJobConfig`
    - `STEP_3_NAME` tasklet 스텝을 추가한다. `BrawlerBattleRankStatsJobConfig` 처럼 `StepBuilder(...).tasklet({ _, _ -> task.alert(dateJobParameter().date!!); RepeatStatus.FINISHED })` 로 만든다.
    - 잡에 `.next(step3)` 를 붙인다.
  - 클래스 KDoc 에 스펙 §3.9 의 이유를 적는다.
    - 통계 전투만 보면 미지원 모드의 새 맵을 놓친다.
    - 로테이션 잡(매시)에 두지 않는 이유는 미등록 맵마다 하루 24건이 오기 때문이다.
    - 전투 단계와 중복을 피하는 방식.
  - `BattleEventSyncItemWriter.createSoloRankEvents` KDoc 에 "경쟁전 맵 목록은 통계 전투에서만 만든다 — 경쟁전에 허용 목록 밖 모드가 들어오면 그 맵이 빠진다" 를 적는다.

- [ ] **Step 4: 통과 확인** — 같은 명령으로 PASS(6개). 이어서 `./gradlew :core:core-batch:test :core:core-batch:ktlintCheck :infrastructure:db-core:ktlintCheck`.

- [ ] **Step 5: 커밋** — `feat(core-batch): 로테이션에 나온 미등록 이벤트도 battleEventSyncJob 이 알린다`

---

### Task 8: 사용자 프론트 (imstargg-frontend)

**Files:**
- Modify: `imstargg-frontend/src/api/types.ts` (L165-171), `src/domain/event/types.ts` (L3-10), `src/domain/event/mappers.ts` (L18-19), `src/domain/battle/types.ts` (L24), `src/domain/battle/mappers.ts` (L39)
- Modify: `app/events/_components/modeStyles.ts`, `RotationCard.tsx` (L13), `SoloRankCard.tsx` (L11)
- Modify: `app/events/[id]/_components/EventStatsSection.tsx` (L15), `EventHero.tsx` (L40-44), `app/events/[id]/page.tsx` (L34-37)
- Modify tests: `tests/unit/events/modeStyles.test.ts`, `tests/unit/domain/event/mappers.test.ts`, `tests/domain/battle/mappers.test.ts`, `tests/e2e/statistics.spec.ts` (L32 근처)
- Create tests: `tests/components/EventStatsSection.test.tsx`, `tests/unit/events/eventMetadata.test.ts`

**Interfaces:**
- Consumes: Task 4 의 JSON — `mode.resultType: "RESULT" | "RANK" | null`, `color` 없음
- Produces:
  - `export function getModeStyle(modeId: number | null | undefined): ModeStyle`
  - `BattleEventModeResponse.resultType: BattleEventModeResultType | null`
  - 도메인 `BattleEventMode.resultType: BattleEventModeResultType | null`
  - 패널·훅 prop 타입은 바꾸지 않는다.

- [ ] **Step 1: 실패하는 테스트 작성**
  - `modeStyles.test.ts` 를 modeId 기준으로 다시 쓴다.

```ts
it("젬그랩(modeId 0)은 보라 배경이다", () => {
  expect(getModeStyle(0)).toEqual({ bg: "#8F43EA", fg: "#FFFFFF" });
});
it("바운티(3)처럼 밝은 배경이면 어두운 글자색이다", () => {
  expect(getModeStyle(3)).toEqual({ bg: "#FFC60A", fg: "#14161B" });
});
it("목록에 없는 모드(48 아레나, 25)와 null/undefined 는 기본 회색이다", () => {
  for (const id of [48, 25, null, undefined]) {
    expect(getModeStyle(id)).toEqual({ bg: "#2A2E38", fg: "#FFFFFF" });
  }
});
```

  - `tests/unit/domain/event/mappers.test.ts`
    - `dtoMode` 의 `color` 를 지운다.
    - `toEqual` 기대값(L37)에서 `color` 를 지운다.
    - `resultType: null` 인 DTO 가 `resultType: null` 로 매핑되는 테스트를 추가한다.
  - `tests/domain/battle/mappers.test.ts`
    - `baseDto` 의 `color` 를 지운다.
    - `modeResultType` 단언(L47, L62)을 지운다.
    - L55 테스트 이름에서 `modeResultType` 을 뺀다.
  - `tests/components/EventStatsSection.test.tsx`
    - 서버 컴포넌트를 `const ui = await EventStatsSection({ event }); renderWithProviders(ui);` 로 렌더한다.
    - `server.use(http.get("*/v1/statistics/brawler/result", ...), http.get("*/v1/statistics/brawler/rank", ...))` 로 핸들러를 두고, 호출되면 플래그를 세운다.
    - `resultType 이 null 이면 안내를 보이고 통계를 요청하지 않는다`: `screen.getByText("이 모드는 통계를 제공하지 않습니다")` 가 있고, 플래그는 false 다.
    - `mode 가 null 이어도 안내를 보이고 통계를 요청하지 않는다`
    - `resultType 이 RESULT 면 결과형 통계를 요청한다`: result 핸들러가 빈 통계를 돌려주면 플래그가 true 다.
  - `tests/unit/events/eventMetadata.test.ts`
    - MSW 로 `*/v1/events/:id` 가 `mode.resultType: null` 인 이벤트를 돌려주게 한다.
    - `generateMetadata({ params: Promise.resolve({ id: "15000001" }) })` 의 `description` 이 `"결과 타입"` 을 포함하지 않는다.
    - `resultType: "RANK"` 이면 `"결과 타입: 순위형"` 을 포함한다.

- [ ] **Step 2: 실패 확인** — `pnpm test:run tests/unit/events tests/unit/domain/event/mappers.test.ts tests/domain/battle/mappers.test.ts tests/components/EventStatsSection.test.tsx` → `getModeStyle(0)`, 안내 문구, 메타 설명이 FAIL.

- [ ] **Step 3: 구현**
  - 타입·매퍼: Interfaces 대로 바꾼다. 주석에 Global Constraints 의 `resultType` 정의를 적는다. `BattleEvent.modeResultType` 필드와 매핑을 지운다. `Battle.resultType`(전투 기록 결과형/순위형)은 별개라 남긴다.
  - `modeStyles.ts`
    - `MODE_COLORS` 상수를 Global Constraints 의 값으로 둔다. 주석에 "백엔드 `StatsEventMode` 에 모드를 추가할 때 함께 추가" 를 적는다.
    - `getModeStyle(modeId)` 는 `modeId == null` 이거나 `MODE_COLORS[modeId]` 가 없으면 `DEFAULT_STYLE` 이다.
    - "색 단일 출처는 백엔드" 주석과 `DEFAULT_STYLE` 주석의 백엔드 `DEFAULT_COLOR` 언급을 고친다.
  - `RotationCard`·`SoloRankCard` 는 `getModeStyle(event.mode?.id)` 를 부른다.
  - `EventStatsSection`
    - `const resultType = event.mode?.resultType ?? null;`
    - null 이면 fetch 없이 안내를 렌더한다. 마크업은 기존 빈 상태(`EventResultStatsTable.tsx:83` 의 `px-4 py-12 text-center text-sm text-[#5C6271]`)를 따른다.
    - 나머지 분기는 지금과 같다.
  - `EventHero`: `event.mode?.resultType` 이 있을 때만 배지를 렌더한다.
  - `page.tsx` `generateMetadata`: `resultType` 이 있으면 `모드: ${modeName} · 결과 타입: ${resultLabel}`, 없으면 `모드: ${modeName}`.
  - `tests/e2e/statistics.spec.ts` 의 "/events/[id] 통계 행 클릭 …" 은 첫 로테이션 카드에 고정돼 있다. 상세에 안내 문구가 보이면 다음 카드로 넘어가도록 고친다(최대 5장). e2e 는 운영 API 를 거쳐서 배포 뒤에만 의미가 있고, CI 에서는 돌지 않는다.

- [ ] **Step 4: 통과 확인** — `pnpm test:run`(전체) PASS, `pnpm typecheck`, `pnpm lint`.

- [ ] **Step 5: 커밋** — `feat(frontend): 통계 미지원 모드 이벤트에 안내를 보이고 모드 색을 프론트 상수로 옮긴다`

---

### Task 9: 관리자 프론트 (imstargg-admin-frontend)

**Files:**
- Modify: `imstargg-admin-frontend/lib/types.ts` (L79-91), `lib/api.ts` (L9, L223-257)
- Modify: `app/event-modes/page.tsx`, `app/event-modes/[brawlStarsId]/edit/page.tsx`

**Interfaces:**
- Consumes: Task 5 의 API
  - `GET /event-modes` → `resultType: "RESULT" | "RANK" | null`, `playerCounts`·`color` 없음
  - `POST /event-modes` 는 `{ brawlStarsId, brawlStarsName, names }`
  - PATCH 와 color PUT 은 없음

- [ ] **Step 1: 타입·API 정리**
  - `BattleEventMode` 에서 `playerCounts`·`color` 를 지우고 `resultType: BattleEventModeResultType | null` 로 바꾼다.
  - `BATTLE_EVENT_MODE_RESULT_TYPES` 를 지운다.
  - `createEventMode` 의 인라인 타입에서 세 필드를 지운다.
  - `updateEventMode`·`updateEventModeColor` 와 쓰이지 않게 된 import 를 지운다.

- [ ] **Step 2: 화면 정리**
  - `page.tsx`
    - 지울 것: 결과 타입 `Select` import, `BATTLE_EVENT_MODE_RESULT_TYPES` import, 로컬 `DEFAULT_COLOR`, form state·초기값·`resetForm` 의 세 필드, 인원 검증, 요청 인자, 토스트의 색 형식 안내, 결과 타입·인원·색 입력 UI
    - 목록의 "결과 타입" 셀은 `resultType` 이 `RESULT` 면 `결과형`, `RANK` 면 `순위형`, null 이면 `미지원` 으로 읽기 전용 표시한다. 헤더는 `통계 유형` 으로 바꾼다.
  - `edit/page.tsx`
    - 지울 것: 결과 타입·플레이어 인원수·색상 카드(L145-207), 관련 state·setter·`parsePlayerCounts`·핸들러 3개, 쓰이지 않는 import
    - 이름·아이콘 카드는 남긴다.

- [ ] **Step 3: 검증** — `pnpm lint && pnpm exec tsc --noEmit && pnpm build` 가 오류 없이 끝나야 한다. CI 테스트가 없으므로 로컬 core-admin 에 붙여 화면을 직접 확인한다.
  - 모드 목록에 `통계 유형` 열이 결과형/순위형/미지원으로 보인다.
  - 생성 폼에 이름 입력만 남는다.
  - 편집 페이지에 이름·아이콘만 남는다.

- [ ] **Step 4: 커밋** — `feat(admin-frontend): 모드 유형·인원·색 편집을 없애고 통계 유형을 읽기 전용으로 보인다`

---

### Task 10: 전체 검증

**Files:** 없음(검증만). 실패하면 원인 태스크로 돌아가 고친다.

- [ ] **Step 1: 백엔드 전체** — `./gradlew ktlintCheck test` → BUILD SUCCESSFUL.
- [ ] **Step 2: CI 재현(Redis 없음)** — `docker stop imstargg-valkey` 후 `./gradlew :core:core-api:test` 가 PASS 여야 한다. 확인하면 `docker start imstargg-valkey`. 로컬 valkey 가 없으면 이 단계는 생략하고 그렇게 기록한다.
- [ ] **Step 3: 프론트** — `imstargg-frontend` 에서 `pnpm test:run && pnpm typecheck && pnpm lint`. `imstargg-admin-frontend` 에서 `pnpm lint && pnpm exec tsc --noEmit && pnpm build`.
- [ ] **Step 4: 남은 참조 검색** — 저장소 루트에서 아래 검색 결과가 0건이어야 한다. 검색 대상은 코드 참조(import·호출·KDoc 링크)다. KDoc 으로 옮긴 결정 이력 서술(`GameSlots` 라는 단어)은 대상이 아니다.
  - `grep -rnE "import .*GameSlot|GameSlots?\(|\[GameSlots?|playerCounts|updateModeColor|BATTLE_EVENT_MODE_RESULT_TYPES|modeResultType" --include=*.kt --include=*.ts --include=*.tsx imstargg-backend imstargg-frontend imstargg-admin-frontend | grep -v node_modules`
- [ ] **Step 5: 배포 메모 확인** — 스펙 §3.4·§3.10·§5 "배포 후 확인" 이 PR 설명에 옮겨질 수 있게 정리한다.
  - 코드 배포 뒤 DDL 적용
  - core-admin 재기동
  - 하키·아레나 맵 21개 등록
  - `event_mode_brawlstars_id` 12개 값 확인
  - 버전 bump 는 PR 단계에서 `/release` 스킬로 한다(core-api·core-worker·core-batch·frontend).
