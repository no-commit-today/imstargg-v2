# 봇 전투 제외(스펙 C1) 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 통계 집계에서 매치메이킹 봇이 상대인 3대3 전투와 봇 로비 쇼다운을 빼고, 그 규칙이 깨졌는지를 일별 1인 통계 잡이 감시하게 한다.

**Architecture:** core-batch `domain/statistics` 에 순수 객체 `StatsBotBattlePolicy` 를 두고 집계기 다섯 개가 `aggregate()` 진입부에서 호출해 전투를 건너뛴다. 일별 1인 잡 두 개의 Task 는 같은 전투를 누적기(`StatsBotBattleMonitor`, `StatsBotLobbyMonitor`)에도 넘겨 요약 로그 한 줄과 Discord 경고를 낸다. 워커, 원본 스키마, API, 캐시 키는 그대로다.

**Tech Stack:** Kotlin + Spring Boot 4 + Spring Batch(core-batch), JUnit 5 + mockk + AssertJ, H2 MODE=PostgreSQL(테스트), docker `postgres:18.3`(런북 SQL 확인), Next.js + TypeScript(소개 페이지).

**Spec:** [docs/superpowers/specs/2026-11-06-stats-bot-battle-exclusion-design.md](../specs/2026-11-06-stats-bot-battle-exclusion-design.md) — 결정 1 ~ 10, §3 설계, §5 테스트가 근거다. 이 계획이 스펙과 어긋나면 스펙이 이긴다.

## Global Constraints

- 백엔드 명령은 `imstargg-backend/`, 프론트 명령은 `imstargg-frontend/` 에서 실행한다. 커밋 전 백엔드는 `./gradlew ktlintFormat` 을 돌린다. `pnpm` 이 "env: node: No such file" 로 죽으면 `/opt/homebrew/bin/pnpm` 을 쓴다.
- **바꾸는 곳**: core-batch 의 `domain/statistics`, `job/statistics/battleresult`, `job/statistics/battlerank`(와 그 테스트), `imstargg-frontend/app/about/page.tsx`, `imstargg-backend/docs/stats-battle-data-strategy.md`, 스펙 §3.4 의 한 항목(Task 6).
- **바꾸지 않는 곳**: core-worker, core-api, core-enum, db-core(엔티티·`schema.sql`·마이그레이션), 통계 캐시 키, 조합 Task 세 개(카운터·2인·3인)의 본문, 프론트의 다른 파일. `/brawlers` 브롤러 탭의 0건 표시는 그대로 둔다(결정 10).
- **정책 상수**(스펙 §3.1): `BOT_TROPHY_LIMIT = 2000`, `TEAM_AVERAGE_WINDOW_LOWER = -75`(포함), `TEAM_AVERAGE_WINDOW_UPPER = 25`(제외), `CLUSTER_WIDTH = 75`, `SOLO_SHOWDOWN_CLUSTER_THRESHOLD = 5`, `DUO_SHOWDOWN_CLUSTER_THRESHOLD = 2`, `TRIO_SHOWDOWN_CLUSTER_THRESHOLD = 2`.
- **판정 방식**: 모드는 `StatsEventMode.find(battle.eventModeBrawlStarsId)` 로 정한다(`mode` 문자열은 보지 않는다). 유형은 `EnumCodeType.findByCode<BattleType>(battle.type) == BattleType.RANKED` 로 보고 가장 먼저 본다. 트로피는 팀원의 `brawler.trophies` 다. 이름·태그·파워는 판정에 쓰지 않는다. 정책 함수와 누적기의 `record` 는 예외를 던지지 않고, 조건을 하나라도 만족하지 않으면 `false` 다.
- **3대3 창**은 정수 비교식이다. 수집자 팀 세 명의 트로피 합 S 와 상대의 트로피 t 에 대해 `3·t ≥ S − 225` 이고 `3·t < S + 75`. 평균을 정수 나눗셈으로 내지 않는다.
- **3대3 감시 문턱**(스펙 §3.3): 걸린 전투 승률 90% 미만(걸린 결정전 1,000건 이상), 남은 전투 승률 55% 초과(결정전 5,000건 이상), 2000 이상 구간 승률 75% 초과(결정전 300건 이상). 걸리는 비중과 기본 이름 비율은 로그만 남긴다.
- **쇼다운 감시 문턱**(솔로 / 듀오 / 트리오): 걸린 전투의 1위 비율 50% / 70% / 70% 미만(걸린 전투 1,000 / 500 / 300건 이상), 남은 전투의 1위 비율 12.5% / 24% / 31% 초과(남은 전투 3,000 / 1,000 / 1,000건 이상). 걸리는 비중은 로그만 남긴다.
- **알림**: 제목 `[잡 이름] 봇 전투 규칙 이상 감지`, `AlertCommand.builder().warn()`, 실행당 한 번. 본문은 `date` 한 줄과 걸린 지표만 한 줄씩이다. 잡 이름은 Task 의 기존 `JOB_NAME` 상수다.
- **요약 로그**: 잡마다 한 줄, `[잡 이름] 봇 감시 - date: …, …`. 3대3 의 "걸린 전투 수 ÷ 대상 전투 수" 는 비중으로 찍지 않는다(기준선과 분모가 다르다).
- 누적기는 정책의 판정 함수와 단계 함수만 부른다. 범위·창 산식을 다시 쓰지 않는다.
- 프로젝트 규칙: 파일당 클래스 하나, 인자 셋 이상이면 named arguments, 로깅은 kotlin-logging(`private val log = KotlinLogging.logger {}`), 주석·KDoc·테스트 이름은 한국어, 비자명한 결정은 KDoc 에 남긴다. 테스트 서브클래스에 `@Tag("context")` 를 달지 않는다. CI 는 H2 이고 Redis 가 없다.
- 커밋은 Conventional Commits 이고 끝에 `Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>` 트레일러를 붙인다. `git add` 는 경로를 적어서 한다(`.agents/skills/release-management/` 는 이 작업의 파일이 아니다). 푸시하지 않는다. 커밋 시각(09 ~ 18시 KST 밖)은 컨트롤러가 PR 전에 한꺼번에 옮기므로 구현자는 평소대로 커밋한다.
- 운영 DB 와 `.secrets/` 에 접근하지 않는다.

## Review Focus

1. **전투가 0건이거나 결정전이 0건인 날**(원본이 없는 날짜의 재실행 등) — 요약이 0 나눗셈 없이 찍히고 알림이 없고 Task 가 정상 종료한다. → Task 3 `StatsBotMonitorMetricTest` "total 이 0 이면", 두 누적기의 "아무것도 기록하지 않아도", Task 4 "전투가 없으면 알림을 보내지 않는다".
2. **2000 이상 슬롯이 낀 봇 전투** — 통째로 빠져 `TROPHY_2000_PLUS` 를 포함해 어느 티어 행도 생기지 않는다. → Task 2 `StatsBotBattleExclusionTest` "2000 이상 슬롯이 낀 봇 전투도".
3. **경쟁전 전투의 `trophies`(랭크 티어 값 1 ~ 22)가 창과 2000 조건을 만족** — 제외하지 않는다. → Task 1 "경쟁전 전투는 랭크 티어 값이 창 안이어도", Task 2 "경쟁전 전투는 집계한다".
4. **`result` 가 알 수 없는 코드이거나 무승부** — `record` 가 던지지 않고, 승률 분모에는 넣지 않고, 걸리는 비중 분모에는 넣는다. → Task 3 `StatsBotBattleMonitorTest` 의 해당 두 테스트.
5. **표본 부족 지표와 걸린 지표가 함께 있거나 지표 둘이 함께 걸림** — 알림은 한 번이고 본문에는 걸린 지표만 한 줄씩 있다. → Task 3 "표본이 충분하고 문턱을 넘으면", Task 4 "지표가 둘 걸려도".

## File Structure

경로의 `…batch/` 는 `imstargg-backend/core/core-batch/src/{main,test}/kotlin/com/imstargg/core/batch/` 다.

| 파일 | 책임 |
|---|---|
| main `…batch/domain/statistics/StatsBotBattlePolicy.kt` (새로) | 상수, 3대3·쇼다운 판정 함수, 단계 함수, 근거 KDoc |
| main `…/domain/statistics/StatsBotMonitorMetric.kt` (새로) | 감시 지표 하나(값, 표본, 문턱, 경고 여부, 한 줄 표기) |
| main `…/domain/statistics/StatsBotBattleMonitor.kt` (새로) | 3대3 지표 누적 |
| main `…/domain/statistics/StatsBotLobbyMonitor.kt` (새로) | 쇼다운 지표 누적(모드별) |
| main 집계기 다섯 개, `BattleStatsAggregator.kt` | 진입부 건너뛰기, KDoc |
| main `…/job/statistics/battleresult/BrawlerBattleResultStatsTask.kt`, `…/battlerank/BrawlerBattleRankStatsTask.kt` | 누적기 호출, 요약 로그, 경고 |
| test `…/domain/statistics/StatsBattleFixtures.kt` (새로) | 새 테스트가 함께 쓰는 전투 픽스처(최상위 `internal` 함수) |
| test `StatsBotBattlePolicyTest`, `StatsBotBattleExclusionTest`, `BrawlerBattleResultStatsAggregatorTest`, `BrawlerBattleRankStatsAggregatorTest`, `StatsBotMonitorMetricTest`, `StatsBotBattleMonitorTest`, `StatsBotLobbyMonitorTest` (새로) | 각 단위 테스트 |

---

### Task 1: 판정 규칙 `StatsBotBattlePolicy`

**Files:**
- Create: `…batch/domain/statistics/StatsBotBattlePolicy.kt` (main)
- Create: `…batch/domain/statistics/StatsBattleFixtures.kt`, `…/StatsBotBattlePolicyTest.kt` (test)

**Interfaces:**
- Produces (main):

```kotlin
object StatsBotBattlePolicy {
    const val BOT_TROPHY_LIMIT = 2000
    const val TEAM_AVERAGE_WINDOW_LOWER = -75
    const val TEAM_AVERAGE_WINDOW_UPPER = 25
    const val CLUSTER_WIDTH = 75
    const val SOLO_SHOWDOWN_CLUSTER_THRESHOLD = 5
    const val DUO_SHOWDOWN_CLUSTER_THRESHOLD = 2
    const val TRIO_SHOWDOWN_CLUSTER_THRESHOLD = 2

    fun appliesTo(battle: StatsResultBattleEntity): Boolean                     // 스펙 §3.1 3대3 의 1 ~ 3
    fun opponentsBelowBotLimit(battle: StatsResultBattleEntity): Boolean        // 4. 범위 밖이면 false
    fun opponentsInTeamAverageWindow(battle: StatsResultBattleEntity): Boolean  // 5. 4번과 무관, 범위 밖이면 false
    fun isBotBattle(battle: StatsResultBattleEntity): Boolean                   // 위 셋 모두
    fun appliesTo(battle: StatsRankBattleEntity): Boolean                       // 쇼다운의 1 ~ 3
    fun isBotLobby(battle: StatsRankBattleEntity): Boolean                      // 1 ~ 6
}
```

- Produces (test 픽스처, 패키지 `com.imstargg.core.batch.domain.statistics`, 최상위 `internal` 함수). 뒤 Task 의 테스트도 이것만 쓴다.

```kotlin
/** 태그 "#TAG$index", 브롤러 id = index, 브롤러 이름 "Brawler$index", 파워 11. */
internal fun statsPlayer(index: Int, trophies: Int = 1000, name: String = "Player$index", tag: String = "#TAG$index"): StatsBattleEntityTeamPlayer

/** battleKey "battle-key", battleAt 2026-11-01T12:00+09:00, eventBrawlStarsId 1, eventMap "test-map", mode "gemGrab". */
internal fun statsResultBattle(
    teams: List<List<StatsBattleEntityTeamPlayer>>,
    result: String = BattleResult.VICTORY.code,
    type: String = BattleType.RANKED.code,
    eventModeBrawlStarsId: Long = StatsEventMode.GEM_GRAB.modeBrawlStarsId,
    playerBrawlStarsTag: String = teams.first().first().brawlStarsTag,
    starPlayerBrawlStarsTag: String? = null,
): StatsResultBattleEntity

/** 위와 같은 고정값, mode "soloShowdown". */
internal fun statsRankBattle(
    teams: List<List<StatsBattleEntityTeamPlayer>>,
    battleRank: Int = 1,
    type: String = BattleType.RANKED.code,
    eventModeBrawlStarsId: Long = StatsEventMode.SOLO_SHOWDOWN.modeBrawlStarsId,
    playerBrawlStarsTag: String = teams.first().first().brawlStarsTag,
): StatsRankBattleEntity

/** 2×3 결과형. 수집자 팀은 인덱스 1 ~ 3(수집자는 #TAG1), 상대는 인덱스 4 ~ 6. 리스트는 각 팀원의 트로피다. */
internal fun threeVsThree(
    collectorTeam: List<Int>,
    opponents: List<Int>,
    result: String = BattleResult.VICTORY.code,
    type: String = BattleType.RANKED.code,
    eventModeBrawlStarsId: Long = StatsEventMode.GEM_GRAB.modeBrawlStarsId,
    opponentNames: List<String> = listOf("Player4", "Player5", "Player6"),
): StatsResultBattleEntity

/** 쇼다운. teamTrophies 의 첫 팀이 수집자 팀이고 그 첫 팀원이 수집자(#TAG1)다. 슬롯 인덱스는 1 부터 팀 순서대로. 모드 id 는 mode 의 것. */
internal fun showdown(
    mode: StatsEventMode,
    teamTrophies: List<List<Int>>,
    battleRank: Int = 1,
    type: String = BattleType.RANKED.code,
): StatsRankBattleEntity
```

- [ ] **Step 1: 픽스처 파일을 쓴다** — 위 시그니처대로. 전투 안의 브롤러 id 는 겹치면 안 된다(`BaseStatsBattleEntity` 의 `init` 이 던진다). 인덱스를 그대로 id 로 쓰면 된다.

- [ ] **Step 2: 3대3 실패 테스트를 쓴다** — `StatsBotBattlePolicyTest` 의 `@Nested inner class ThreeVsThree`. `P` 는 `import com.imstargg.core.batch.domain.statistics.StatsBotBattlePolicy as P` 다. 이 계획의 테스트 코드는 한 줄로 적은 곳이 있다. `ktlintFormat` 이 줄을 나누면 그대로 따른다.

```kotlin
private val bot = threeVsThree(listOf(1000, 1100, 1200), listOf(1050, 1080, 1090))

@Test
fun `상대 세 명이 모두 창 안이고 2000 미만이면 봇 전투다`() {
    assertThat(P.appliesTo(bot)).isTrue()
    assertThat(P.isBotBattle(bot)).isTrue()
}

@Test
fun `창의 하한은 3t 가 S - 225 이상이다`() {
    // S = 3300 → 3·1025 = 3075 = S - 225
    assertThat(P.isBotBattle(threeVsThree(listOf(1000, 1100, 1200), listOf(1025, 1080, 1080)))).isTrue()
    // S = 3301(평균이 정수가 아니다) → 3·1025 = 3075 = S - 226
    assertThat(P.isBotBattle(threeVsThree(listOf(1000, 1100, 1201), listOf(1025, 1080, 1080)))).isFalse()
}

@Test
fun `창의 상한은 3t 가 S + 75 미만이다`() {
    // S = 3301 → 3·1125 = 3375 = S + 74
    assertThat(P.isBotBattle(threeVsThree(listOf(1000, 1100, 1201), listOf(1125, 1080, 1080)))).isTrue()
    // S = 3300 → 3·1125 = 3375 = S + 75
    assertThat(P.isBotBattle(threeVsThree(listOf(1000, 1100, 1200), listOf(1125, 1080, 1080)))).isFalse()
}

@Test
fun `상대 한 명이 2000 이면 창 조건만 참이다`() {
    // S = 5970 → 창은 1915 이상 2015 미만
    val below = threeVsThree(listOf(2000, 2000, 1970), listOf(1950, 1960, 1999))
    val atLimit = threeVsThree(listOf(2000, 2000, 1970), listOf(1950, 1960, 2000))
    assertThat(P.isBotBattle(below)).isTrue()
    assertThat(P.opponentsInTeamAverageWindow(atLimit)).isTrue()
    assertThat(P.opponentsBelowBotLimit(atLimit)).isFalse()
    assertThat(P.isBotBattle(atLimit)).isFalse()
}

@Test
fun `상대 쪽만 본다`() {
    // 수집자 팀 세 명은 상대 평균(1040)의 창 안이지만 상대는 수집자 팀 창(925 이상 1025 미만) 밖이다
    assertThat(P.isBotBattle(threeVsThree(listOf(1000, 1000, 1000), listOf(1030, 1040, 1050)))).isFalse()
}

@Test
fun `트로피전이 아니면 네 함수 모두 false 다`() {
    listOf("soloRanked", "challenge", "unknown").forEach { type ->
        val battle = threeVsThree(listOf(1000, 1100, 1200), listOf(1050, 1080, 1090), type = type)
        assertThat(P.appliesTo(battle)).isFalse()
        assertThat(P.opponentsBelowBotLimit(battle)).isFalse()
        assertThat(P.opponentsInTeamAverageWindow(battle)).isFalse()
        assertThat(P.isBotBattle(battle)).isFalse()
    }
}

@Test
fun `경쟁전 전투는 랭크 티어 값이 창 안이어도 봇 전투가 아니다`() {
    assertThat(P.isBotBattle(threeVsThree(listOf(19, 19, 19), listOf(19, 19, 19), type = "soloRanked"))).isFalse()
}

@Test
fun `결과형 허용 모드가 아니면 false 다`() {
    // 6 은 솔로 쇼다운, 999 는 허용 목록에 없는 모드
    listOf(6L, 999L).forEach { modeId ->
        val battle = threeVsThree(listOf(1000, 1100, 1200), listOf(1050, 1080, 1090), eventModeBrawlStarsId = modeId)
        assertThat(P.appliesTo(battle)).isFalse()
        assertThat(P.isBotBattle(battle)).isFalse()
    }
}

@Test
fun `팀이 2×3 이 아니면 false 다`() {
    val twoByTwo = listOf(listOf(statsPlayer(1), statsPlayer(2)), listOf(statsPlayer(4, 1050), statsPlayer(5, 1050)))
    val threeTeams = listOf(1, 4, 7).map { first -> (first..first + 2).map { statsPlayer(it) } }
    val threeVsTwo = listOf((1..3).map { statsPlayer(it) }, listOf(statsPlayer(4), statsPlayer(5)))
    listOf(twoByTwo, threeTeams, threeVsTwo).forEach {
        assertThat(P.appliesTo(statsResultBattle(it))).isFalse()
        assertThat(P.isBotBattle(statsResultBattle(it))).isFalse()
    }
}

@Test
fun `수집자 슬롯이 정확히 하나가 아니면 false 다`() {
    val collectorTeam = (1..3).map { statsPlayer(it) }
    val opponents = (4..6).map { statsPlayer(it) }
    val absent = statsResultBattle(listOf(collectorTeam, opponents), playerBrawlStarsTag = "#NOBODY")
    val twiceInTeam = statsResultBattle(listOf(listOf(statsPlayer(1), statsPlayer(2, tag = "#TAG1"), statsPlayer(3)), opponents))
    val inBothTeams = statsResultBattle(listOf(collectorTeam, listOf(statsPlayer(4, tag = "#TAG1"), statsPlayer(5), statsPlayer(6))))
    listOf(absent, twiceInTeam, inBothTeams).forEach { assertThat(P.isBotBattle(it)).isFalse() }
}
```

- [ ] **Step 3: 실패 확인** — `./gradlew :core:core-batch:test --tests "com.imstargg.core.batch.domain.statistics.StatsBotBattlePolicyTest"` → 컴파일 실패(`StatsBotBattlePolicy` 없음).

- [ ] **Step 4: 3대3 을 구현한다** — 스펙 §3.1 의 1 ~ 5 그대로. 창은 상수에서 끌어낸 식 `3 * t >= s + 3 * TEAM_AVERAGE_WINDOW_LOWER && 3 * t < s + 3 * TEAM_AVERAGE_WINDOW_UPPER` 로 쓴다(225, 75 를 따로 적지 않는다). 적용 범위는 `private` 함수 하나로 "수집자 팀과 상대 팀" 을 돌려주게 하면 단계 함수 셋이 그것을 함께 쓴다.

- [ ] **Step 5: 3대3 통과 확인** — 같은 명령 → `ThreeVsThree` 전부 PASS.

- [ ] **Step 6: 쇼다운 실패 테스트를 쓴다** — `@Nested inner class Showdown`. 수집자 팀은 항상 1500 이다.

```kotlin
private fun solo(vararg others: Int) = showdown(StatsEventMode.SOLO_SHOWDOWN, listOf(listOf(1500)) + others.map { listOf(it) })
private fun duo(vararg others: List<Int>) = showdown(StatsEventMode.DUO_SHOWDOWN, listOf(listOf(1500, 1500)) + others)
private fun trio(vararg others: List<Int>) = showdown(StatsEventMode.TRIO_SHOWDOWN, listOf(listOf(1500, 1500, 1500)) + others)
private val farDuos = arrayOf(listOf(1700, 1710), listOf(1900, 1910))

@Test
fun `솔로는 몰린 팀이 5 이상이면 봇 로비다`() {
    assertThat(P.isBotLobby(solo(1000, 1010, 1020, 1030, 1200, 1400, 1600, 1800, 1990))).isFalse()
    assertThat(P.isBotLobby(solo(1000, 1010, 1020, 1030, 1040, 1200, 1400, 1600, 1800))).isTrue()
}

@Test
fun `듀오는 몰린 팀이 2 이상이면 봇 로비다`() {
    assertThat(P.isBotLobby(duo(listOf(1000, 1010), listOf(1200, 1210), listOf(1400, 1410), listOf(1600, 1610)))).isFalse()
    assertThat(P.isBotLobby(duo(listOf(1000, 1010), listOf(1020, 1030), listOf(1400, 1410), listOf(1600, 1610)))).isTrue()
}

@Test
fun `트리오는 몰린 팀이 2 이상이면 봇 로비다`() {
    assertThat(P.isBotLobby(trio(listOf(1000, 1010, 1020), listOf(1200, 1210, 1220), listOf(1400, 1410, 1420)))).isFalse()
    assertThat(P.isBotLobby(trio(listOf(1000, 1010, 1020), listOf(1030, 1040, 1050), listOf(1400, 1410, 1420)))).isTrue()
}

@Test
fun `팀의 최댓값이 창 시작 + 75 미만이어야 그 창에 든다`() {
    assertThat(P.isBotLobby(duo(listOf(1000, 1010), listOf(1020, 1074), *farDuos))).isTrue()
    assertThat(P.isBotLobby(duo(listOf(1000, 1010), listOf(1020, 1075), *farDuos))).isFalse()
}

@Test
fun `팀원 한 명만 창 안이면 그 팀은 세지 않는다`() {
    assertThat(P.isBotLobby(duo(listOf(1000, 1010), listOf(1050, 1300), *farDuos))).isFalse()
}

@Test
fun `팀원 트로피 폭이 75 이상인 팀은 자기 창에서도 세지 않는다`() {
    // 스펙 §5 의 예: 몰린 팀 수 1
    assertThat(P.isBotLobby(duo(listOf(1000, 1100), listOf(1010, 1050), listOf(1500, 1700), listOf(2100, 2200)))).isFalse()
}

@Test
fun `팀원 한 명이 2000 인 팀은 자격이 없다`() {
    assertThat(P.isBotLobby(duo(listOf(1990, 1995), listOf(1992, 1999), listOf(1000, 1010), listOf(1300, 1310)))).isTrue()
    assertThat(P.isBotLobby(duo(listOf(1990, 1995), listOf(1992, 2000), listOf(1000, 1010), listOf(1300, 1310)))).isFalse()
}

@Test
fun `자격 있는 팀이 없으면 범위 안이어도 false 다`() {
    val lobby = duo(listOf(2000, 2010), listOf(2020, 2030), listOf(2040, 2050), listOf(2060, 2070))
    assertThat(P.appliesTo(lobby)).isTrue()
    assertThat(P.isBotLobby(lobby)).isFalse()
}

@Test
fun `수집자 팀은 세지 않는다`() {
    val lobby = showdown(StatsEventMode.DUO_SHOWDOWN, listOf(listOf(1000, 1010), listOf(1020, 1030), listOf(1300, 1310), listOf(1600, 1610), listOf(1900, 1910)))
    assertThat(P.isBotLobby(lobby)).isFalse()
}

@Test
fun `트로피전 쇼다운의 정해진 팀 구성이 아니면 false 다`() {
    val clustered = arrayOf(listOf(1000, 1010), listOf(1020, 1030), listOf(1400, 1410), listOf(1600, 1610))
    val notRanked = showdown(StatsEventMode.DUO_SHOWDOWN, listOf(listOf(1500, 1500)) + clustered, type = "soloRanked")
    val notShowdown = statsRankBattle(duo(*clustered).teams, eventModeBrawlStarsId = StatsEventMode.GEM_GRAB.modeBrawlStarsId)
    val nineSolos = showdown(StatsEventMode.SOLO_SHOWDOWN, listOf(listOf(1500)) + (0..7).map { listOf(1000 + it) })
    val shortTeam = showdown(StatsEventMode.DUO_SHOWDOWN, listOf(listOf(1500, 1500), listOf(1000, 1010), listOf(1020, 1030), listOf(1400, 1410), listOf(1600)))
    listOf(notRanked, notShowdown, nineSolos, shortTeam).forEach {
        assertThat(P.appliesTo(it)).isFalse()
        assertThat(P.isBotLobby(it)).isFalse()
    }
}

@Test
fun `수집자 태그가 두 팀에 있으면 false 다`() {
    val teams =
        listOf(
            listOf(statsPlayer(1, 1500), statsPlayer(2, 1500)),
            listOf(statsPlayer(3, 1000, tag = "#TAG1"), statsPlayer(4, 1010)),
            listOf(statsPlayer(5, 1020), statsPlayer(6, 1030)),
            listOf(statsPlayer(7, 1040), statsPlayer(8, 1050)),
            listOf(statsPlayer(9, 1700), statsPlayer(10, 1710)),
        )
    val lobby = statsRankBattle(teams, eventModeBrawlStarsId = StatsEventMode.DUO_SHOWDOWN.modeBrawlStarsId)
    assertThat(P.isBotLobby(lobby)).isFalse()
}
```

- [ ] **Step 7: 실패 확인** — 같은 명령 → `Showdown` 의 `isTrue()` 단언들이 FAIL 하거나 컴파일 실패.

- [ ] **Step 8: 쇼다운을 구현한다** — 스펙 §3.1 의 1 ~ 6 그대로. 임계값은 `when (mode)` 으로 `SOLO_SHOWDOWN`·`DUO_SHOWDOWN`·`TRIO_SHOWDOWN` 만 값을 주고 나머지는 범위 밖으로 본다. 몰린 팀 수는 자격 있는 팀 A 마다 `min(T) >= min(A) && max(T) < min(A) + CLUSTER_WIDTH` 인 자격 있는 팀 T(A 포함)를 세어 낸 최댓값이다.

- [ ] **Step 9: KDoc 을 쓴다** — `ComboStatsPolicy`(core-api `domain/statistics`)의 꼴을 따른다. 객체 KDoc 에 (1) 두 규칙의 한 줄 정의, (2) `## 근거`(스펙 §1 과 부록 C.1 ~ C.3 의 핵심 수치: 걸린 전투 승률 93 ~ 95%, 남은 전투 승률 52%, 창 변형 비교에서 [−75, +25) 를 고른 까닭, 쇼다운 임계값을 고른 까닭), (3) `## 결정 이력`(상대 쪽만 본다, 이름·태그·파워를 쓰지 않는다, 2000 조건을 상대 세 명에 건다, 집계 단계에서만 뺀다, 로비 단위로 맞추지 않는다), (4) 스펙 경로를 적는다. 상수마다 한 줄 KDoc 을 단다.

- [ ] **Step 10: 확인** — `./gradlew ktlintFormat :core:core-batch:test --tests "com.imstargg.core.batch.domain.statistics.StatsBotBattlePolicyTest"` → 전부 PASS.

- [ ] **Step 11: Commit** — `git add` 로 세 파일만 올리고 `feat(core-batch): 봇 전투 판정 규칙(StatsBotBattlePolicy)을 더한다`.

### Task 2: 집계기 다섯 개에 적용

**Files:**
- Modify (main `…batch/domain/statistics/`): `BrawlerBattleResultStatsAggregator.kt`, `BrawlerEnemyBattleResultStatsAggregator.kt`, `BrawlerPairBattleResultStatsAggregator.kt`, `BrawlerTrioBattleResultStatsAggregator.kt`, `BrawlerBattleRankStatsAggregator.kt`, `BattleStatsAggregator.kt`(KDoc 만)
- Create (test `…batch/domain/statistics/`): `StatsBotBattleExclusionTest.kt`, `BrawlerBattleResultStatsAggregatorTest.kt`, `BrawlerBattleRankStatsAggregatorTest.kt`
- Modify (test): `…batch/job/statistics/triobattleresult/BrawlerTrioBattleResultStatsTaskTest.kt`, `…/pairbattleresult/BrawlerPairBattleResultStatsWriterTest.kt`

**Interfaces:**
- Consumes: Task 1 의 `StatsBotBattlePolicy.isBotBattle`, `isBotLobby`, 픽스처 함수.
- Produces: 다섯 집계기의 `aggregate()` 가 봇 전투·봇 로비에서 아무것도 누적하지 않는다. 시그니처는 그대로다.

- [ ] **Step 1: 실패 테스트를 쓴다** — `StatsBotBattleExclusionTest`. 버킷 날짜는 `LocalDate.parse("2026-10-26")`, `LocalDate.parse("2026-10-31")`.

```kotlin
private fun entityCounts(battle: StatsResultBattleEntity): List<Int> =
    listOf(
        BrawlerBattleResultStatsAggregator(),
        BrawlerEnemyBattleResultStatsAggregator(START, END),
        BrawlerPairBattleResultStatsAggregator(START, END),
        BrawlerTrioBattleResultStatsAggregator(START, END),
    ).map {
        it.aggregate(battle)
        it.toEntities().size
    }

@Test
fun `결과형 집계기 네 개가 같은 봇 전투를 모두 건너뛴다`() {
    assertThat(entityCounts(threeVsThree(listOf(1000, 1100, 1200), listOf(1050, 1080, 1090)))).containsExactly(0, 0, 0, 0)
}

@Test
fun `상대 트로피를 창 밖으로 옮기면 네 개 모두 집계한다`() {
    // 1인 6행, 카운터 3×3×2 = 18행, 2인 3 + 3 = 6행, 3인 2행
    assertThat(entityCounts(threeVsThree(listOf(1000, 1100, 1200), listOf(1150, 1180, 1190)))).containsExactly(6, 18, 6, 2)
}

@Test
fun `2000 이상 슬롯이 낀 봇 전투도 통째로 건너뛴다`() {
    // S = 4500 → 창은 1425 이상 1525 미만
    assertThat(entityCounts(threeVsThree(listOf(1000, 1000, 2500), listOf(1450, 1460, 1470)))).containsExactly(0, 0, 0, 0)

    val aggregator = BrawlerBattleResultStatsAggregator()
    aggregator.aggregate(threeVsThree(listOf(1000, 1000, 2500), listOf(1550, 1560, 1570)))
    assertThat(aggregator.toEntities().map { it.tierRange }).contains(StatsTierRange.TROPHY_2000_PLUS)
}

@Test
fun `경쟁전 전투는 집계한다`() {
    assertThat(entityCounts(threeVsThree(listOf(19, 19, 19), listOf(19, 19, 19), type = "soloRanked"))).allMatch { it > 0 }
}
```

`BrawlerBattleRankStatsAggregatorTest`:

```kotlin
private fun aggregate(battle: StatsRankBattleEntity) = BrawlerBattleRankStatsAggregator().also { it.aggregate(battle) }.toEntities()
private fun duo(vararg others: List<Int>, battleRank: Int = 1) =
    showdown(StatsEventMode.DUO_SHOWDOWN, listOf(listOf(1500, 2100)) + others, battleRank = battleRank)

@Test
fun `수집자 팀만 본인 트로피의 티어로 순위를 집계한다`() {
    val entities = aggregate(duo(listOf(1000, 1010), listOf(1200, 1210), listOf(1400, 1410), listOf(1600, 1610), battleRank = 2))

    assertThat(entities.map { it.brawlerBrawlStarsId to it.tierRange }).containsExactlyInAnyOrder(
        1L to StatsTierRange.TROPHY_1000_PLUS,
        2L to StatsTierRange.TROPHY_1000_PLUS,
        2L to StatsTierRange.TROPHY_2000_PLUS,
    )
    assertThat(entities).allSatisfy {
        assertThat(it.rankCounts.countOf(2)).isEqualTo(1L)
        assertThat(it.rankCounts.totalCount).isEqualTo(1L)
    }
}

@Test
fun `봇 로비는 건너뛰고 임계값 아래 로비는 집계한다`() {
    assertThat(aggregate(duo(listOf(1000, 1010), listOf(1020, 1030), listOf(1400, 1410), listOf(1600, 1610)))).isEmpty()
    assertThat(aggregate(duo(listOf(1000, 1010), listOf(1200, 1210), listOf(1400, 1410), listOf(1600, 1610)))).isNotEmpty()
}
```

- [ ] **Step 2: 실패 확인** — `./gradlew :core:core-batch:test --tests "*StatsBotBattleExclusionTest" --tests "*BrawlerBattleRankStatsAggregatorTest"` → 봇 전투·봇 로비 단언이 FAIL(행이 생긴다).

- [ ] **Step 3: 집계기에 건너뛰기를 넣는다** — 결과형 네 개는 `aggregate()` 의 첫 줄에 `if (StatsBotBattlePolicy.isBotBattle(battle)) return`, 순위형은 `if (StatsBotBattlePolicy.isBotLobby(battle)) return`. 각 집계기 KDoc 의 집계 규칙에 "봇 전투([StatsBotBattlePolicy])는 전투 단위로 건너뛴다. 티어 구간을 정하기 전이라 어느 티어 행에도 들지 않는다" 를 한 줄 더한다. `BattleStatsAggregator` KDoc 의 "공통 집계 규칙" 에도 같은 항목을 더하고, 호출 지점이 다섯 곳이며 `StatsBotBattleExclusionTest` 가 고정한다고 적는다.

- [ ] **Step 4: 깨지는 기존 테스트 둘의 픽스처를 고친다** — `./gradlew :core:core-batch:test` 를 돌리면 `BrawlerTrioBattleResultStatsTaskTest`(둘 다)와 `BrawlerPairBattleResultStatsWriterTest` 가 FAIL 한다. 여섯 명이 모두 1000 인 `ranked` 2×3 전투라 새 규칙에 걸린다. 단언은 그대로 두고 두 파일의 `player` 에 `trophies: Int = 1000` 인자를 더해 상대 세 명(인덱스 4 ~ 6)만 `1100` 으로 만든다. 수집자 팀 합 3000 의 창은 925 이상 1025 미만이라 1100 은 창 밖이고 티어는 여전히 `TROPHY_1000_PLUS` 하나다.

- [ ] **Step 5: 1인 결과 집계기의 전용 테스트를 쓴다** — `BrawlerBattleResultStatsAggregatorTest`(지금까지 전용 테스트가 없었다). 지금 동작을 고정하는 테스트라 바로 통과한다.

```kotlin
private fun aggregate(battle: StatsResultBattleEntity) = BrawlerBattleResultStatsAggregator().also { it.aggregate(battle) }.toEntities()

@Test
fun `수집자 팀은 결과 그대로, 상대 팀은 반대로 집계한다`() {
    val entities = aggregate(statsResultBattle(listOf(listOf(statsPlayer(1)), listOf(statsPlayer(2)))))

    assertThat(entities.single { it.brawlerBrawlStarsId == 1L }.resultCount.victoryCount).isEqualTo(1L)
    assertThat(entities.single { it.brawlerBrawlStarsId == 2L }.resultCount.defeatCount).isEqualTo(1L)
}

@Test
fun `티어는 플레이어 본인의 trophies 로 정한다`() {
    val entities = aggregate(statsResultBattle(listOf(listOf(statsPlayer(1, 2500)), listOf(statsPlayer(2, 1000)))))

    assertThat(entities.filter { it.brawlerBrawlStarsId == 1L }.map { it.tierRange })
        .containsExactlyInAnyOrder(StatsTierRange.TROPHY_1000_PLUS, StatsTierRange.TROPHY_2000_PLUS)
    assertThat(entities.filter { it.brawlerBrawlStarsId == 2L }.map { it.tierRange }).containsExactly(StatsTierRange.TROPHY_1000_PLUS)
}

@Test
fun `스타 플레이어의 행만 starPlayerCount 를 센다`() {
    val entities = aggregate(statsResultBattle(listOf(listOf(statsPlayer(1)), listOf(statsPlayer(2))), starPlayerBrawlStarsTag = "#TAG2"))

    assertThat(entities.single { it.brawlerBrawlStarsId == 1L }.starPlayerCount).isZero()
    assertThat(entities.single { it.brawlerBrawlStarsId == 2L }.starPlayerCount).isEqualTo(1L)
}

@Test
fun `유형이나 결과가 알 수 없는 코드면 건너뛴다`() {
    val teams = listOf(listOf(statsPlayer(1)), listOf(statsPlayer(2)))
    assertThat(aggregate(statsResultBattle(teams, type = "unknown"))).isEmpty()
    assertThat(aggregate(statsResultBattle(teams, result = "unknown"))).isEmpty()
}
```

- [ ] **Step 6: 확인** — `./gradlew ktlintFormat :core:core-batch:test` → BUILD SUCCESSFUL(컨텍스트 테스트 포함).

- [ ] **Step 7: Commit** — `feat(core-batch): 통계 집계기 다섯 개가 봇 전투를 건너뛴다`.

### Task 3: 감시 누적기

**Files:**
- Create (main `…batch/domain/statistics/`): `StatsBotMonitorMetric.kt`, `StatsBotBattleMonitor.kt`, `StatsBotLobbyMonitor.kt`
- Create (test, 같은 패키지): `StatsBotMonitorMetricTest.kt`, `StatsBotBattleMonitorTest.kt`, `StatsBotLobbyMonitorTest.kt`

**Interfaces:**
- Consumes: Task 1 의 정책 함수 여섯 개와 픽스처.
- Produces:

```kotlin
/** 감시 지표 하나. 값은 count / total 이고 total 이 표본 수다. warnBelow·warnAbove 가 모두 null 이면 로그만 남기는 지표다. */
data class StatsBotMonitorMetric(
    val name: String,
    val count: Long,
    val total: Long,
    val warnBelow: Double? = null,   // rate 가 이 값 미만이면 경고
    val warnAbove: Double? = null,   // rate 가 이 값 초과면 경고
    val minSample: Long = 0,         // total 이 이 값 미만이면 경고를 판정하지 않는다
) {
    val rate: Double?                   // total 이 0 이면 null
    val isSampleInsufficient: Boolean   // 문턱이 있고 total < minSample
    val isWarning: Boolean              // 문턱이 있고 표본이 충분하고 문턱을 넘었다
    fun describe(): String              // 아래 형식
}

class StatsBotBattleMonitor {
    val targetCount: Long     // 3대3 적용 범위 안의 전투 수
    val flaggedCount: Long    // 그중 isBotBattle 인 전투 수
    fun record(battle: StatsResultBattleEntity)
    fun metrics(): List<StatsBotMonitorMetric>   // 아래 표의 순서로 다섯 개
    fun summary(): String
}

class StatsBotLobbyMonitor {
    fun record(battle: StatsRankBattleEntity)
    fun metrics(): List<StatsBotMonitorMetric>   // 솔로·듀오·트리오 순으로 모드마다 세 개, 모두 아홉 개
    fun summary(): String
}
```

`describe()` 의 형식. 백분율은 `String.format(Locale.ROOT, "%.1f%%", rate * 100)`, `rate` 가 null 이면 `-` 다.

| 경우 | 출력 |
|---|---|
| 문턱 없음 | `걸리는 비중 28.4% (표본 5000건)` |
| `warnBelow` | `걸린 전투 승률 88.2% (표본 1234건, 문턱 90.0% 미만)` |
| `warnAbove` | `남은 전투 승률 56.0% (표본 6000건, 문턱 55.0% 초과)` |
| 표본 부족 | `걸린 전투 승률 85.0% (표본 20건, 문턱 90.0% 미만, 표본 부족)` |
| 표본 0 | `걸린 전투 승률 - (표본 0건, 문턱 90.0% 미만, 표본 부족)` |

`StatsBotBattleMonitor.metrics()`. 대상은 `appliesTo(battle)` 인 전투뿐이다. "결정전" 은 `battle.result` 가 `victory` 나 `defeat` 인 전투다. "본인" 은 수집자 팀에서 `brawlStarsTag == playerBrawlStarsTag` 인 팀원의 `brawler.trophies` 이고 1000 이상 2000 미만일 때 "본인 1000~1999" 다.

| 순서 | `name` | `count` | `total` | 문턱 |
|---|---|---|---|---|
| 1 | `걸린 전투 승률` | `isBotBattle` 인 전투의 승 | 그 결정전 수 | `warnBelow = 0.90`, `minSample = 1000` |
| 2 | `남은 전투 승률` | 본인 1000~1999 이고 `isBotBattle` 이 아닌 전투의 승 | 그 결정전 수 | `warnAbove = 0.55`, `minSample = 5000` |
| 3 | `2000 이상 구간 승률` | `opponentsInTeamAverageWindow` 이고 `opponentsBelowBotLimit` 이 아닌 전투의 승 | 그 결정전 수 | `warnAbove = 0.75`, `minSample = 300` |
| 4 | `걸리는 비중` | 본인 1000~1999 전투 중 `isBotBattle` 인 수 | 본인 1000~1999 전투 수(무승부·알 수 없는 결과 포함) | 없음 |
| 5 | `기본 이름 비율` | `isBotBattle` 인 전투의 상대 슬롯 중 팀원 `name` 이 `Brawler` 인 수 | 그 상대 슬롯 수 | 없음 |

`summary()` 는 `대상 {targetCount}건, 걸린 전투 {flaggedCount}건, 본인 1000~1999 {4번의 total}건 중 걸린 {4번의 count}건, ` 뒤에 `metrics()` 의 `describe()` 를 `, ` 로 이어 붙인다.

`StatsBotLobbyMonitor.metrics()`. 대상은 `appliesTo(battle)` 인 전투뿐이고 모드별로 센다. 모드 이름은 `솔로`, `듀오`, `트리오` 다.

| 모드 안 순서 | `name` | `count` | `total` | 문턱 (솔로 / 듀오 / 트리오) |
|---|---|---|---|---|
| 1 | `{모드} 걸린 전투 1위 비율` | `isBotLobby` 이고 `battleRank == 1` | `isBotLobby` 인 전투 수 | `warnBelow` 0.50 / 0.70 / 0.70, `minSample` 1000 / 500 / 300 |
| 2 | `{모드} 남은 전투 1위 비율` | `isBotLobby` 가 아니고 `battleRank == 1` | `isBotLobby` 가 아닌 전투 수 | `warnAbove` 0.125 / 0.24 / 0.31, `minSample` 3000 / 1000 / 1000 |
| 3 | `{모드} 걸리는 비중` | `isBotLobby` 인 전투 수 | 대상 전투 수 | 없음 |

`summary()` 는 `metrics()` 의 `describe()` 를 `, ` 로 이어 붙인 것이다(모드별 대상 수와 걸린 수는 3번 지표의 표본과 값에 들어 있다).

- [ ] **Step 1: `StatsBotMonitorMetricTest` 를 쓴다**

```kotlin
@Test
fun `total 이 0 이면 rate 는 null 이고 경고가 아니다`() {
    val metric = StatsBotMonitorMetric(name = "걸린 전투 승률", count = 0, total = 0, warnBelow = 0.90, minSample = 0)
    assertThat(metric.rate).isNull()
    assertThat(metric.isWarning).isFalse()
}

@Test
fun `warnBelow 는 미만일 때만 경고다`() {
    assertThat(StatsBotMonitorMetric(name = "m", count = 899, total = 1000, warnBelow = 0.90, minSample = 1000).isWarning).isTrue()
    assertThat(StatsBotMonitorMetric(name = "m", count = 900, total = 1000, warnBelow = 0.90, minSample = 1000).isWarning).isFalse()
}

@Test
fun `warnAbove 는 초과일 때만 경고다`() {
    assertThat(StatsBotMonitorMetric(name = "m", count = 2751, total = 5000, warnAbove = 0.55, minSample = 5000).isWarning).isTrue()
    assertThat(StatsBotMonitorMetric(name = "m", count = 2750, total = 5000, warnAbove = 0.55, minSample = 5000).isWarning).isFalse()
}

@Test
fun `최소 표본 미만이면 경고가 아니고 표본 부족이다`() {
    val metric = StatsBotMonitorMetric(name = "m", count = 0, total = 999, warnBelow = 0.90, minSample = 1000)
    assertThat(metric.isWarning).isFalse()
    assertThat(metric.isSampleInsufficient).isTrue()
}

@Test
fun `문턱이 없으면 경고도 표본 부족도 아니다`() {
    val metric = StatsBotMonitorMetric(name = "m", count = 1, total = 1)
    assertThat(metric.isWarning).isFalse()
    assertThat(metric.isSampleInsufficient).isFalse()
}

@Test
fun `describe 는 값과 표본과 문턱을 한 줄로 적는다`() {
    assertThat(StatsBotMonitorMetric(name = "걸리는 비중", count = 1420, total = 5000).describe())
        .isEqualTo("걸리는 비중 28.4% (표본 5000건)")
    assertThat(StatsBotMonitorMetric(name = "걸린 전투 승률", count = 1088, total = 1234, warnBelow = 0.90, minSample = 1000).describe())
        .isEqualTo("걸린 전투 승률 88.2% (표본 1234건, 문턱 90.0% 미만)")
    assertThat(StatsBotMonitorMetric(name = "남은 전투 승률", count = 3360, total = 6000, warnAbove = 0.55, minSample = 5000).describe())
        .isEqualTo("남은 전투 승률 56.0% (표본 6000건, 문턱 55.0% 초과)")
    assertThat(StatsBotMonitorMetric(name = "걸린 전투 승률", count = 17, total = 20, warnBelow = 0.90, minSample = 1000).describe())
        .isEqualTo("걸린 전투 승률 85.0% (표본 20건, 문턱 90.0% 미만, 표본 부족)")
    assertThat(StatsBotMonitorMetric(name = "걸린 전투 승률", count = 0, total = 0, warnBelow = 0.90, minSample = 1000).describe())
        .isEqualTo("걸린 전투 승률 - (표본 0건, 문턱 90.0% 미만, 표본 부족)")
}
```

- [ ] **Step 2: 실패 확인 뒤 `StatsBotMonitorMetric` 을 구현한다** — `./gradlew :core:core-batch:test --tests "*StatsBotMonitorMetricTest"` 가 컴파일 실패 → 구현 → PASS.

- [ ] **Step 3: `StatsBotBattleMonitorTest` 를 쓴다** — `metric(name)` 은 `monitor.metrics().single { it.name == name }` 이다.

```kotlin
private val monitor = StatsBotBattleMonitor()
private fun bot(result: String = "victory", names: List<String> = listOf("Brawler", "Brawler", "Kim")) =
    threeVsThree(listOf(1000, 1100, 1200), listOf(1050, 1080, 1090), result = result, opponentNames = names)
private fun real(result: String = "victory") = threeVsThree(listOf(1000, 1100, 1200), listOf(1150, 1180, 1190), result = result)
private fun aboveLimit(result: String = "victory") = threeVsThree(listOf(2000, 2000, 1970), listOf(1950, 1960, 2000), result = result)
private fun record(vararg battles: StatsResultBattleEntity) = battles.forEach { monitor.record(it) }
private fun metric(name: String) = monitor.metrics().single { it.name == name }

@Test
fun `걸린 전투 승률은 무승부를 빼고 센다`() {
    record(bot("victory"), bot("victory"), bot("defeat"), bot("draw"))

    assertThat(monitor.targetCount).isEqualTo(4)
    assertThat(monitor.flaggedCount).isEqualTo(4)
    assertThat(metric("걸린 전투 승률")).extracting("count", "total").containsExactly(2L, 3L)
}

@Test
fun `남은 전투 승률은 본인 1000~1999 이고 걸리지 않은 전투만 센다`() {
    // 마지막 전투는 본인이 2100 이라 빠진다 (S = 4100 → 창 밖, 걸리지 않음)
    record(real("victory"), real("defeat"), real("draw"), bot("victory"), threeVsThree(listOf(2100, 1000, 1000), listOf(1800, 1810, 1820)))

    assertThat(metric("남은 전투 승률")).extracting("count", "total").containsExactly(1L, 2L)
}

@Test
fun `2000 이상 구간 승률은 창을 만족하고 상대에 2000 이상이 있는 전투만 센다`() {
    record(aboveLimit("victory"), aboveLimit("defeat"), aboveLimit("draw"), bot("victory"), real("victory"))

    assertThat(metric("2000 이상 구간 승률")).extracting("count", "total").containsExactly(1L, 2L)
}

@Test
fun `걸리는 비중은 본인 1000~1999 전투를 무승부까지 분모로 센다`() {
    // 마지막 전투는 봇 전투지만 본인이 2500 이라 분자·분모 모두에서 빠진다
    record(bot("victory"), bot("draw"), real("draw"), real("defeat"), threeVsThree(listOf(2500, 1000, 1000), listOf(1450, 1460, 1470)))

    assertThat(monitor.flaggedCount).isEqualTo(3)
    assertThat(metric("걸리는 비중")).extracting("count", "total").containsExactly(2L, 4L)
}

@Test
fun `기본 이름 비율은 걸린 전투의 상대 슬롯으로 센다`() {
    record(bot(names = listOf("Brawler", "Brawler", "Kim")), real())

    assertThat(metric("기본 이름 비율")).extracting("count", "total").containsExactly(2L, 3L)
}

@Test
fun `result 가 알 수 없는 코드여도 던지지 않고 승률에는 넣지 않는다`() {
    record(bot("unknown"))

    assertThat(monitor.flaggedCount).isEqualTo(1)
    assertThat(metric("걸린 전투 승률").total).isZero()
    assertThat(metric("걸리는 비중")).extracting("count", "total").containsExactly(1L, 1L)
}

@Test
fun `적용 범위 밖 전투는 어느 지표에도 세지 않는다`() {
    record(
        threeVsThree(listOf(19, 19, 19), listOf(19, 19, 19), type = "soloRanked"),
        threeVsThree(listOf(1000, 1100, 1200), listOf(1050, 1080, 1090), eventModeBrawlStarsId = 6),
        statsResultBattle(listOf(listOf(statsPlayer(1)), listOf(statsPlayer(2)))),
    )

    assertThat(monitor.targetCount).isZero()
    assertThat(monitor.metrics()).allMatch { it.total == 0L }
}

@Test
fun `문턱과 최소 표본은 스펙 값이다`() {
    assertThat(monitor.metrics().map { it.name })
        .containsExactly("걸린 전투 승률", "남은 전투 승률", "2000 이상 구간 승률", "걸리는 비중", "기본 이름 비율")
    assertThat(metric("걸린 전투 승률")).extracting("warnBelow", "warnAbove", "minSample").containsExactly(0.90, null, 1000L)
    assertThat(metric("남은 전투 승률")).extracting("warnBelow", "warnAbove", "minSample").containsExactly(null, 0.55, 5000L)
    assertThat(metric("2000 이상 구간 승률")).extracting("warnBelow", "warnAbove", "minSample").containsExactly(null, 0.75, 300L)
    assertThat(metric("걸리는 비중")).extracting("warnBelow", "warnAbove").containsExactly(null, null)
    assertThat(metric("기본 이름 비율")).extracting("warnBelow", "warnAbove").containsExactly(null, null)
}

@Test
fun `아무것도 기록하지 않아도 summary 와 metrics 가 예외 없이 나온다`() {
    assertThat(monitor.metrics()).noneMatch { it.isWarning }
    assertThat(monitor.summary()).startsWith("대상 0건, 걸린 전투 0건, 본인 1000~1999 0건 중 걸린 0건, 걸린 전투 승률 - (")
}

@Test
fun `표본이 충분하고 문턱을 넘으면 그 지표만 경고다`() {
    repeat(300) { record(aboveLimit("victory")) }
    record(bot("defeat"))

    assertThat(monitor.metrics().filter { it.isWarning }.map { it.name }).containsExactly("2000 이상 구간 승률")
    assertThat(metric("걸린 전투 승률").isSampleInsufficient).isTrue()
}
```

- [ ] **Step 4: 실패 확인 뒤 `StatsBotBattleMonitor` 를 구현한다** — 문턱과 최소 표본은 `companion object` 의 `private const` 로 두고 각 상수 KDoc 에 스펙 §3.3 의 기준선(92.7 ~ 95.0%, 51.8 ~ 52.5%, 56 ~ 61%)을 적는다. 클래스 KDoc 에 지표의 뜻(실제 전투를 지우고 있다, 봇을 놓치고 있다, 2000 위에 봇이 생겼다)과 "정책의 판정·단계 함수만 부른다" 를 적는다.

- [ ] **Step 5: `StatsBotLobbyMonitorTest` 를 쓴다**

```kotlin
private val monitor = StatsBotLobbyMonitor()
private fun botDuo(rank: Int) =
    showdown(StatsEventMode.DUO_SHOWDOWN, listOf(listOf(1500, 1500), listOf(1000, 1010), listOf(1020, 1030), listOf(1400, 1410), listOf(1600, 1610)), battleRank = rank)
private fun realDuo(rank: Int) =
    showdown(StatsEventMode.DUO_SHOWDOWN, listOf(listOf(1500, 1500), listOf(1000, 1010), listOf(1200, 1210), listOf(1400, 1410), listOf(1600, 1610)), battleRank = rank)
private fun botSolo(rank: Int) =
    showdown(StatsEventMode.SOLO_SHOWDOWN, listOf(listOf(1500)) + listOf(1000, 1010, 1020, 1030, 1040, 1200, 1400, 1600, 1800).map { listOf(it) }, battleRank = rank)
private fun metric(name: String) = monitor.metrics().single { it.name == name }

@Test
fun `걸린 전투와 남은 전투의 1위 비율과 걸리는 비중을 모드별로 센다`() {
    listOf(botDuo(1), botDuo(2), realDuo(1), realDuo(3), realDuo(4), botSolo(1)).forEach { monitor.record(it) }

    assertThat(metric("듀오 걸린 전투 1위 비율")).extracting("count", "total").containsExactly(1L, 2L)
    assertThat(metric("듀오 남은 전투 1위 비율")).extracting("count", "total").containsExactly(1L, 3L)
    assertThat(metric("듀오 걸리는 비중")).extracting("count", "total").containsExactly(2L, 5L)
    assertThat(metric("솔로 걸린 전투 1위 비율")).extracting("count", "total").containsExactly(1L, 1L)
    assertThat(metric("트리오 걸리는 비중").total).isZero()
}

@Test
fun `적용 범위 밖 전투는 세지 않는다`() {
    monitor.record(showdown(StatsEventMode.DUO_SHOWDOWN, botDuo(1).teams.map { team -> team.map { it.brawler.trophies } }, type = "soloRanked"))
    monitor.record(showdown(StatsEventMode.SOLO_SHOWDOWN, listOf(listOf(1500)) + (0..7).map { listOf(1000 + it) }))

    assertThat(monitor.metrics()).allMatch { it.total == 0L }
}

@Test
fun `지표는 솔로 듀오 트리오 순으로 모드마다 세 개이고 문턱은 스펙 값이다`() {
    assertThat(monitor.metrics().map { it.name }).containsExactly(
        "솔로 걸린 전투 1위 비율", "솔로 남은 전투 1위 비율", "솔로 걸리는 비중",
        "듀오 걸린 전투 1위 비율", "듀오 남은 전투 1위 비율", "듀오 걸리는 비중",
        "트리오 걸린 전투 1위 비율", "트리오 남은 전투 1위 비율", "트리오 걸리는 비중",
    )
    assertThat(metric("솔로 걸린 전투 1위 비율")).extracting("warnBelow", "minSample").containsExactly(0.50, 1000L)
    assertThat(metric("듀오 걸린 전투 1위 비율")).extracting("warnBelow", "minSample").containsExactly(0.70, 500L)
    assertThat(metric("트리오 걸린 전투 1위 비율")).extracting("warnBelow", "minSample").containsExactly(0.70, 300L)
    assertThat(metric("솔로 남은 전투 1위 비율")).extracting("warnAbove", "minSample").containsExactly(0.125, 3000L)
    assertThat(metric("듀오 남은 전투 1위 비율")).extracting("warnAbove", "minSample").containsExactly(0.24, 1000L)
    assertThat(metric("트리오 남은 전투 1위 비율")).extracting("warnAbove", "minSample").containsExactly(0.31, 1000L)
    assertThat(metric("솔로 걸리는 비중")).extracting("warnBelow", "warnAbove").containsExactly(null, null)
}

@Test
fun `아무것도 기록하지 않아도 summary 와 metrics 가 예외 없이 나온다`() {
    assertThat(monitor.metrics()).noneMatch { it.isWarning }
    assertThat(monitor.summary()).startsWith("솔로 걸린 전투 1위 비율 - (")
}
```

- [ ] **Step 6: 실패 확인 뒤 `StatsBotLobbyMonitor` 를 구현한다** — 상수와 KDoc 은 Step 4 와 같은 방식(기준선은 스펙 §3.3 쇼다운 표). KDoc 에 "트리오의 남은 전투는 전투가 적은 날 최소 표본에 못 미쳐 판정을 건너뛴다" 를 적는다.

- [ ] **Step 7: 확인** — `./gradlew ktlintFormat :core:core-batch:test --tests "*StatsBotMonitorMetricTest" --tests "*StatsBotBattleMonitorTest" --tests "*StatsBotLobbyMonitorTest"` → 전부 PASS.

- [ ] **Step 8: Commit** — `feat(core-batch): 봇 전투 감시 누적기를 더한다`.

### Task 4: 일별 1인 잡의 감시 요약과 경고

**Files:**
- Modify (main): `…batch/job/statistics/battleresult/BrawlerBattleResultStatsTask.kt`, `…/battlerank/BrawlerBattleRankStatsTask.kt`
- Modify (test): `…batch/job/statistics/battleresult/BrawlerBattleResultStatsTaskTest.kt`, `…/battlerank/BrawlerBattleRankStatsTaskTest.kt`

**Interfaces:**
- Consumes: Task 3 의 `StatsBotBattleMonitor`, `StatsBotLobbyMonitor`(인자 없는 생성자, `record`, `metrics()`, `summary()`), `StatsBotMonitorMetric.isWarning`, `describe()`. `AlertManager.alert(AlertCommand)`, `AlertCommand.builder().warn().title(…).content(…).build()`(`…batch/support/alert`).
- Produces: 두 Task 의 생성자가 `(reader, writer, alertManager: AlertManager)` 가 된다. `aggregate(date: LocalDate)` 의 시그니처와 기존 로그는 그대로다.

- [ ] **Step 1: 실패 테스트를 쓴다** — `BrawlerBattleResultStatsTaskTest`. `alertManager = mockk<AlertManager>(relaxUnitFun = true)` 를 더해 `sut = BrawlerBattleResultStatsTask(reader, writer, alertManager)` 로 만든다. 기존 테스트에는 `verify(exactly = 0) { alertManager.alert(any()) }` 를 더한다. `givenBattles(battles)` 는 기존 테스트처럼 `reader.read` 가 `battles` 를 콜백으로 넘기고 `writer.write` 가 `1L` 을 돌려주게 하는 스텁이다. `date` 는 `LocalDate.of(2026, 5, 1)` 이다.

```kotlin
private fun aboveLimit(result: String = "victory") = threeVsThree(listOf(2000, 2000, 1970), listOf(1950, 1960, 2000), result = result)
private fun bot(result: String) = threeVsThree(listOf(1000, 1100, 1200), listOf(1050, 1080, 1090), result = result)

@Test
fun `경고 조건에 걸린 지표가 있으면 알림을 한 번 보낸다`() {
    givenBattles(List(300) { aboveLimit() })

    sut.aggregate(date)

    val alert = slot<AlertCommand>()
    verify(exactly = 1) { alertManager.alert(capture(alert)) }
    assertThat(alert.captured.title).contains("[brawlerBattleResultStatsJob] 봇 전투 규칙 이상 감지")
    assertThat(alert.captured.content!!.lines()).hasSize(2)
    assertThat(alert.captured.content).startsWith("date: 2026-05-01").contains("2000 이상 구간 승률 100.0% (표본 300건, 문턱 75.0% 초과)")
}

@Test
fun `지표가 둘 걸려도 알림은 한 번이고 본문에 한 줄씩 적는다`() {
    givenBattles(List(300) { aboveLimit() } + List(1000) { bot("defeat") })

    sut.aggregate(date)

    val alert = slot<AlertCommand>()
    verify(exactly = 1) { alertManager.alert(capture(alert)) }
    assertThat(alert.captured.content!!.lines()).hasSize(3)
    assertThat(alert.captured.content).contains("걸린 전투 승률 0.0%", "2000 이상 구간 승률 100.0%")
}

@Test
fun `전투가 없으면 알림을 보내지 않는다`() {
    every { reader.read(eq(date), any()) } returns 0L
    every { writer.write(eq(date), any()) } returns 0L

    sut.aggregate(date)

    verify(exactly = 0) { alertManager.alert(any()) }
}

@Test
fun `알림 전송에서 예외가 나도 정상 종료한다`() {
    givenBattles(List(300) { aboveLimit() })
    every { alertManager.alert(any()) } throws RuntimeException("boom")

    assertThatCode { sut.aggregate(date) }.doesNotThrowAnyException()
    verify(exactly = 1) { writer.write(eq(date), any()) }
}
```

`BrawlerBattleRankStatsTaskTest` 에도 같은 네 테스트를 쓴다. 걸리는 전투는 `showdown(StatsEventMode.TRIO_SHOWDOWN, listOf(listOf(1500, 1500, 1500), listOf(1000, 1010, 1020), listOf(1030, 1040, 1050), listOf(1400, 1410, 1420)), battleRank = 2)` 300건이고 기대 줄은 `트리오 걸린 전투 1위 비율 0.0% (표본 300건, 문턱 70.0% 미만)`, 제목은 `[brawlerBattleRankStatsJob] 봇 전투 규칙 이상 감지` 다. "지표가 둘" 은 여기에 같은 구성의 듀오 봇 로비(`listOf(1500, 1500), listOf(1000, 1010), listOf(1020, 1030), listOf(1400, 1410), listOf(1600, 1610)`, `battleRank = 2`) 500건을 더해 `듀오 걸린 전투 1위 비율 0.0%` 까지 두 줄을 확인한다.

- [ ] **Step 2: 실패 확인** — `./gradlew :core:core-batch:test --tests "*BrawlerBattleResultStatsTaskTest" --tests "*BrawlerBattleRankStatsTaskTest"` → 컴파일 실패(생성자 인자).

- [ ] **Step 3: 두 Task 를 고친다** — 두 파일에 같은 꼴로 쓴다(합치지 않는다).
  - 생성자에 `private val alertManager: AlertManager` 를 더한다.
  - 실행마다 집계기와 함께 누적기를 하나 만들고, 리더 콜백에서 전투마다 `monitor.record(it)` 다음에 `aggregator.aggregate(it)` 을 부른다.
  - `writer.write` 와 기존 "완료" 로그 뒤에 `private fun reportBotMonitor(date, monitor)` 를 부른다. 본문 전체를 `try { … } catch (ex: Exception) { log.error(ex) { "[$JOB_NAME] 봇 감시 요약 실패 - date: $date" } }` 로 감싼다.
  - `try` 안: `log.info { "[$JOB_NAME] 봇 감시 - date: $date, ${monitor.summary()}" }`, 이어서 `monitor.metrics().filter { it.isWarning }` 가 비어 있지 않으면 알림 한 번. 제목 `"[$JOB_NAME] 봇 전투 규칙 이상 감지"`, 본문 `"date: $date\n" + warnings.joinToString("\n") { it.describe() }`.
  - Task KDoc 에 "감시 구간의 예외는 로그로만 남기고 집계 잡을 실패시키지 않는다. `DiscordAlertManager` 는 전송 실패를 삼키지만 알림을 만드는 중의 예외는 스텝을 실패시키기 때문이다" 를 적는다.

- [ ] **Step 4: 확인** — `./gradlew ktlintFormat :core:core-batch:test` → BUILD SUCCESSFUL. `CoreBatchApplicationTest` 가 통과하면 새 생성자 인자의 빈 주입도 확인된 것이다.

- [ ] **Step 5: Commit** — `feat(core-batch): 일별 1인 통계 잡이 봇 감시 요약과 경고를 낸다`.

### Task 5: 소개 페이지 문장과 전략 문서

**Files:**
- Modify: `imstargg-frontend/app/about/page.tsx`("데이터 출처와 갱신" 의 `<p>`)
- Modify: `imstargg-backend/docs/stats-battle-data-strategy.md`(3장, 5장, 8장, 10장)

- [ ] **Step 1: 소개 페이지에 한 문장을 더한다** — "…공개 API에서 수집한 데이터를 가공한 것입니다." 바로 뒤, "전적과 랭킹은 …" 앞에 아래 문장을 넣는다. 다른 문장과 다른 파일은 건드리지 않는다.

> 브롤러 메타 통계는 매치메이킹 봇이 상대인 전투를 제외하고 집계합니다.

- [ ] **Step 2: 프론트 확인** — `pnpm typecheck && pnpm lint && pnpm test:run` → 모두 통과. `grep -c '매치메이킹' app/about/page.tsx` → 1 이상.

- [ ] **Step 3: Commit** — `feat(frontend): 소개 페이지에 봇 전투 제외 문장을 더한다`.

- [ ] **Step 4: 전략 문서를 고친다**
  - **3장** "수집 프로세스" 끝에 `### 수집 필터와 집계 제외` 를 더한다. 수집 필터(워커, 정의에 따른 제외: 위 표의 여덟 조건)와 집계 제외(배치, 추정 규칙에 따른 제외: 봇 전투)를 구분하고, 봇 전투는 원본에 저장되고 집계에서만 빠진다는 것과 그 까닭(규칙을 고친 뒤 다시 집계할 수 있고 감시 지표를 낼 수 있다)을 적는다.
  - **5장** 에 `### 5.5 봇 전투 제외` 를 더한다. 두 규칙의 정의(스펙 §3.1 의 조건 목록), `StatsBotBattlePolicy` 의 위치, 적용 지점(집계기 다섯 개의 `aggregate()` 진입부, 전투 단위, 티어 구간과 무관), 감시(누적기 두 개, 일별 1인 잡 두 개에서만 돈다, 스펙 §3.3 의 지표·기준선·경고 조건 표 둘, 요약 로그와 알림 제목)를 적고 스펙을 링크한다. **5.1** 의 "이관된 버킷의 `date` 로 재실행하면" 은 아래 10장과 같은 기준으로 맞춘다.
  - **8장** "설정" 에 `StatsBotBattlePolicy`(상수 일곱 개), `StatsBotBattleMonitor`·`StatsBotLobbyMonitor`(문턱과 최소 표본은 클래스 상수) 항목을 더한다.
  - **10장** 의 "덮어쓰기 주의" 항목을 아래 문장으로 바꾼다.

    > - **덮어쓰기 주의**: 조합·카운터·3인 잡은 `currentAndPrevious(date)` 버킷을 통째로 지우고 다시 쓴다. 다시 쓰는 버킷의 시작일이 실행일 − 10 보다 이르면 원본이 모자라 남은 원본만으로 덮어써 부분 데이터가 된다. `date` 가 버킷의 첫날(월요일 또는 1일)이면 직전 버킷도 다시 쓰므로 직전 버킷의 시작일도 같은 기준으로 본다. 1인 잡은 원본이 없는 날짜로 실행하면 그 날짜의 집계가 전 티어에서 지워진다.

  - **10장** 끝에 `### 봇 판정 규칙을 고친 뒤의 재집계` 를 더한다. 스펙 §3.4 의 전제와 2번·3번에서 일반 절차만 옮긴다: 실행일 T 에 다시 집계할 수 있는 날짜는 T − 10 ~ T − 1 이고 T + 1 의 02:00 전에 끝낸다, 통계 배치 시간대(02:00 ~ 09:00 KST) 밖에서 한다, 1인 잡은 가장 오래된 날짜부터, 조합 잡의 `date` 고르는 법(묶음 하나는 그 묶음의 마지막 날, 하루짜리 묶음은 다음 묶음의 첫날, 첫날을 넘기면 직전 묶음까지 다시 쓴다), 수동 재집계에서도 감시가 돈다, 화면은 캐시 TTL 30분 뒤에 본다. 이번 전환에만 쓰는 백업 후 삭제(4번)는 옮기지 않고 스펙을 가리킨다.
  - **10장** 끝에 `### 봇 감시 경고를 받았을 때` 를 더한다. 순서는 (1) 그날과 직전 며칠의 `봇 감시 -` 요약 로그에서 걸린 지표, 값, 표본 수를 본다, (2) 지표의 뜻으로 방향을 정한다(걸린 전투의 승률·1위 비율이 내려감 = 실제 전투를 지우고 있다, 남은 전투의 승률·1위 비율이 올라감 = 봇을 놓치고 있다, 2000 이상 구간 승률이 올라감 = 봇 매치 상한이 바뀌었다, 기본 이름 비율이 내려감 = 표기만 바뀌었다), (3) 공식 릴리스 노트의 매치메이킹 변경과 시즌 전환·트로피 초기화 직후인지를 확인한다, (4) 규칙을 고쳐야 하면 `StatsBotBattlePolicy` 의 상수나 판정을 테스트와 함께 고쳐 배포하고 위 절차로 원본이 남은 10일 안에서 다시 집계한다, (5) 일시적인 현상이면 조치 없이 요약 로그를 며칠 더 본다.

- [ ] **Step 5: 문서 확인** — `grep -n '5.5 봇 전투 제외\|수집 필터와 집계 제외\|봇 판정 규칙을 고친 뒤의 재집계\|봇 감시 경고를 받았을 때\|실행일 − 10' docs/stats-battle-data-strategy.md` → 다섯 표현이 모두 나온다. `grep -n '배포일 − 7일' docs/stats-battle-data-strategy.md` → 결과 없음.

- [ ] **Step 6: Commit** — `docs: 통계 데이터 전략 문서에 봇 전투 제외와 재집계 절차를 더한다`.

### Task 6: 런북 SQL 확인 (로컬 PostgreSQL 18.3)

**Files:**
- Modify: `docs/superpowers/specs/2026-11-06-stats-bot-battle-exclusion-design.md`(§3.4 의 4번 마지막 항목 "이 SQL 은 운영에서 실행해 보지 않은 초안이다…")
- 작업 파일(git 에 넣지 않는다): `.superpowers/analysis/2026-11-06-bot-exclusion/runbook-verify/` 아래의 SQL 과 출력

스펙 §3.4 의 4번 SQL 을 일반 테이블 둘과 파티션 테이블 셋에서 실행해 본다. 운영 DB 는 쓰지 않는다. docker 를 쓸 수 없으면 건너뛰지 말고 BLOCKED 로 보고한다.

- [ ] **Step 1: DB 를 띄운다** — `docker run -d --rm --name bot-c1-pg -e POSTGRES_PASSWORD=pg -p 55433:5432 postgres:18.3`(운영, `docker-compose.yml` 과 같은 버전). `docker exec -i bot-c1-pg psql -U postgres -v ON_ERROR_STOP=1` 로 `imstargg-backend/infrastructure/db-core/src/main/resources/schema.sql` 과 `partition-manage.sql` 을 차례로 적용하고 `select create_month_partitions('2026-10-01', 1);` 로 2026-10, 2026-11 월 파티션을 만든다.

- [ ] **Step 2: 표본을 넣는다** — 기준은 `<D0>` = `2026-10-27`, `<W0>` = `2026-11-01`, `<yymmdd>` = `261106` 이다.

  | 테이블 | 옮겨져야 하는 행 | 남아야 하는 행 |
  |---|---|---|
  | `brawler_battle_result_stats`, `brawler_battle_rank_stats` | `TROPHY_1000_PLUS` 10-20, `TROPHY_2000_PLUS` 10-20, `TROPHY_3000_PLUS` 10-26 | `SOLO_RANK_MYTHIC_PLUS` 10-20, `TROPHY_1000_PLUS` 10-27, `TROPHY_2000_PLUS` 11-01 |
  | 조합 세 테이블 | 묶음 [10-26 ~ 10-31] 의 `TROPHY_1000_PLUS`, `TROPHY_2000_PLUS`, `TROPHY_3000_PLUS` | 묶음 [10-26 ~ 10-31] 의 `SOLO_RANK_MYTHIC_PLUS`, 묶음 [11-01 ~ 11-01] 과 [11-02 ~ 11-08] 의 `TROPHY_1000_PLUS` |

- [ ] **Step 3: 스펙의 SQL 을 그대로 실행하고 기대와 견준다** — 테이블마다 사전 확인, 옮기기, 사후 확인 순서로.

  | 확인 | 기대 |
  |---|---|
  | 사전 확인 0 | `trio_exists = t`, `combo_migrated = t`. `alter table brawler_pair_battle_result_stats add column event_brawlstars_id bigint` 뒤에는 `combo_migrated = f`(확인 뒤 컬럼을 다시 지운다) |
  | 사전 확인(행 수) | 다섯 테이블 모두 3 |
  | 옮기기 | `INSERT 0 3`, 백업 테이블 3행, 원 테이블에는 "남아야 하는 행" 만 |
  | 커밋 뒤 사전 확인 | 0 |
  | 같은 옮기기를 다시 실행 | `create table` 이 "already exists" 로 실패한다. `rollback` 한 뒤 원 테이블과 백업 테이블의 행이 그대로다 |
  | 백업 테이블의 종류 | 조합 테이블의 백업도 일반 테이블이다(`select relkind from pg_class where relname = '…_backup_261106'` → `r`) |
  | `set lock_timeout = '3s'`, `explain` | 오류 없이 실행된다(계획은 출력만 남긴다) |
  | 복원(1인 두 테이블) | `insert into <원> select * from <백업> on conflict do nothing` → `INSERT 0 3`, 한 번 더 실행하면 `INSERT 0 0` |
  | 복원(조합, 달 파티션이 있을 때) | `where start_battle_date >= date '2026-10-01'` 을 붙여 `INSERT 0 3` |
  | 복원(조합, `…_p_202610` 을 `drop` 한 뒤) | `where` 없이 실행하면 "no partition of relation … found for row" 로 문장 전체가 실패한다. `where start_battle_date >= date '2026-11-01'` 을 붙이면 `INSERT 0 0` 으로 성공한다 |

- [ ] **Step 4: 정리** — `docker rm -f bot-c1-pg`.

- [ ] **Step 5: 스펙에 결과를 적는다** — §3.4 4번의 마지막 항목을 "이 SQL 은 운영에서 실행해 보지 않았다. 로컬 PostgreSQL 18.3(docker)에서 일반 테이블 둘과 파티션 테이블 셋으로 확인했다(날짜): 건수 대조, 재실행 실패, 복원, 지워진 달 파티션에서의 복원 실패와 `where` 조건." 으로 바꾼다. 기대와 다른 결과가 하나라도 있으면 스펙의 SQL 이나 설명을 고치고, 무엇이 달랐는지를 그 항목에 적고, 보고서에도 적는다.

- [ ] **Step 6: Commit** — `docs: 봇 전투 제외 런북 SQL 을 로컬 PostgreSQL 에서 확인한 결과를 적는다`.

### Task 7: 전체 확인

- [ ] **Step 1: 백엔드** — `imstargg-backend` 에서 `./gradlew ktlintCheck test` → BUILD SUCCESSFUL(컨텍스트 테스트 포함, 전 모듈).
- [ ] **Step 2: 프론트** — `imstargg-frontend` 에서 `pnpm typecheck && pnpm lint && pnpm test:run` → 모두 통과.
- [ ] **Step 3: 범위 확인** — 저장소 루트에서 `git diff --stat origin/develop...HEAD` 에 core-worker, core-api, core-enum, db-core 의 파일과 `imstargg-frontend` 의 `app/about/page.tsx` 밖 파일이 없다. `grep -rl "if (StatsBotBattlePolicy.isBot" imstargg-backend/core/core-batch/src/main | wc -l` → 5(집계기 다섯 개).
- [ ] **Step 4: 버전** — core-batch 와 frontend 는 이미 minor SNAPSHOT 이다. PR 단계에서 `/release` 로 확인한다(이 계획에서는 바꾸지 않는다).
