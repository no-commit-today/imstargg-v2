# 1인 브롤러 티어 점수 재설계(스펙 C2) 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 1인 브롤러 티어 점수를 "수축 승률 + 상한 있는 인기 가산" 산식과 새 등급 경계로 바꾸고, 전투 수가 적은 브롤러를 등급 없이(표본 부족) 보이게 한다.

**Architecture:** core-api `domain/statistics` 에 1인 전용 값 타입 `BrawlerTierScore`(점수와 nullable 등급)를 두고 `BrawlerResultStatistics` 가 이것으로 점수와 항목 순서를 정한다. 1인 응답의 `tier` 는 nullable 이 되고 1인 캐시 키는 `v4` 로 오른다. 프론트는 `tier: StatTier | null` 을 받아 배지, 정렬, 행 강조, 범례를 고친다. 조합의 동작과 응답, core-batch, core-worker, DB 는 그대로다.

**Tech Stack:** Kotlin + Spring Boot 4(core-api), JUnit 5 + AssertJ, Next.js + TypeScript, Vitest + Testing Library.

**Spec:** [docs/superpowers/specs/2026-11-10-brawler-tier-score-design.md](../specs/2026-11-10-brawler-tier-score-design.md). 결정 1 ~ 8, §3 설계, §5 테스트가 근거다. 이 계획이 스펙과 어긋나면 스펙이 이긴다.

## Global Constraints

- `./gradlew` 명령은 `imstargg-backend/`, `pnpm` 명령은 `imstargg-frontend/` 에서 실행한다. **`git` 명령(`git add`, `git commit`, `git grep`)은 저장소 루트에서 실행한다.** 경로를 저장소 루트 기준으로 적었기 때문이다. 백엔드를 고친 Task 는 커밋 전에 `imstargg-backend/` 에서 `./gradlew ktlintFormat` 을 돌린다. `pnpm` 이 "env: node: No such file" 로 죽으면 `/opt/homebrew/bin/pnpm` 을 쓴다.
- **산식**(스펙 §3.1). n = 승 + 패 + 무, wr = 승 ÷ (승 + 패)(분모 0 이면 0), μ = Σ승 ÷ Σ(승 + 패)(분모 0 이면 0), adj = (n × wr + 300 × μ) ÷ (n + 300), m = 입력 브롤러들의 n 의 중앙값(짝수면 가운데 두 값의 평균), 가산 = 4 × tanh(ln(n ÷ m) ÷ ln 3), 점수 = `roundToInt(max(0, 50 + 200 × (adj − μ) + 가산))`.
- **경계 처리**: 입력이 비면 빈 결과. m 이 0 이면 가산은 0. m > 0 이고 n 이 0 이면 가산은 −4.
- **등급**(스펙 §3.2): 전투 수 300 미만이면 등급 없음(`null`). 그 밖은 점수 ≥ 60 S, ≥ 55 A, ≥ 45 B, ≥ 40 C, 나머지 D. `TierGrade` 에 값을 더하지 않는다.
- **상수 이름**: `BAYESIAN_C = 300.0`, `SCALING_K = 200.0`(둘은 `TierScore` 의 이름과 값을 그대로 옮긴다), `PICK_RATE_BONUS_CAP = 4.0`, `PICK_RATE_BONUS_SATURATION = 3.0`, `GRADE_MIN_BATTLE_COUNT = 300L`.
- **응답**: `tierScore` 는 그대로 정수, `tier` 는 `"S"` ~ `"D"` 또는 `null`. 항목 순서는 등급 있는 항목 먼저, 점수 내림차순, 전투 수 내림차순이다.
- **캐시 키**: `'v4:brawlerResultStats:'`. 다른 키는 그대로다.
- **화면 문구**(스펙 §3.5, 글자 그대로): 배지의 기호는 `–`(U+2013), `aria-label` 과 `title` 은 `표본 부족`, 범례는 `– 표본 부족: 전투 수가 적어 티어를 매기지 않습니다.` 화면 문구에 판수(300)를 적지 않는다.
- **바꾸지 않는 곳**: 조합 표 세 개와 카운터 표, 조합 매퍼가 쓰는 `parseTier`, 조합 응답, core-batch, core-worker, core-enum, db-core, 다른 캐시 키, e2e 테스트.
- 프로젝트 규칙: 파일당 클래스 하나, 운영 코드에서 인자가 셋 이상이면 named arguments, 주석·KDoc·테스트 이름은 한국어, 비자명한 결정은 KDoc 에 남긴다. 테스트 서브클래스에 `@Tag("context")` 를 달지 않는다. CI 는 H2 이고 Redis 가 없다.
- Kotlin 을 고치기 전에 `superdomain:conventions` 스킬을 읽는다. 새 이름은 `docs/superglossary/glossary.md` 의 단어로 짓는다(Task 1 이 필요한 단어를 등록한다).
- 커밋은 Conventional Commits 이고 끝에 `Co-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>` 트레일러를 붙인다. `git add` 는 경로를 적어서 한다(`.agents/skills/release-management/` 는 이 작업의 파일이 아니다). 푸시하지 않는다. 커밋 시각(09 ~ 18시 KST 밖)은 컨트롤러가 PR 전에 한꺼번에 옮기므로 구현자는 평소대로 커밋한다.
- 운영 DB 와 `.secrets/` 에 접근하지 않는다.

## 실행 전 확인 (컨트롤러)

- 브랜치는 `feature/brawler-tier-score`(develop 에서 분기)다. 스펙과 이 계획 문서를 먼저 커밋한다: `docs: 1인 브롤러 티어 점수 재설계 스펙과 구현 계획을 추가한다`.
- 저장소 루트에서 `git grep -n "TierScore.create" -- imstargg-backend/core/core-api/src/main` 으로 조합 호출부를 확인한다. 이 계획은 조합 네 곳이 남아 있는 상태(develop `1939b3fe`)를 전제로 썼다. 조합 호출부가 이미 없으면 Task 3 을 "`TierScore.kt` 와 `TierScoreTest.kt` 를 지운다" 로 바꾼다(스펙 §3.4).

## Review Focus

1. **무승부뿐인 맥락**(Σ(승 + 패) = 0): 0 나눗셈 없이 모든 브롤러가 50점이다. → Task 1 "무승부뿐이면 모두 50점이다".
2. **식이 음수가 되는 브롤러**(승률이 아주 낮음): 점수는 0 이고 등급은 D 다. → Task 1 "점수는 0 아래로 내려가지 않는다".
3. **통계가 0건인 기간의 `/brawlers`**(전 브롤러의 통계가 없음): 전 브롤러가 표본 부족 배지와 `neutral` 레일이고 범례가 보인다. → Task 5 "통계가 0건이면".
4. **검색으로 등급 없는 행이 걸러진 표**: 범례가 사라진다. → Task 5 "범례는 보이는 행에".
5. **대소문자·공백이 섞였거나 `tier` 필드가 없는 응답**(새 프론트가 구버전 백엔드를 만남): 알 수 있는 값은 그 등급, 나머지는 표본 부족이다. → Task 4 `parseBrawlerTier` 테스트.

## File Structure

`…api/` 는 `imstargg-backend/core/core-api/src/{main,test}/kotlin/com/imstargg/core/`, `fe/` 는 `imstargg-frontend/` 다.

| 파일 | 책임 |
|---|---|
| main `…api/domain/statistics/BrawlerTierScore.kt` (새로) | 1인 티어 점수의 산식, 상수, 등급 판정, 근거 KDoc |
| main `…api/domain/statistics/BrawlerResultStatistics.kt` | 항목의 점수 타입과 항목 순서 |
| main `…api/api/controller/v1/response/BrawlerResultStatisticsResponse.kt` | `tier` nullable |
| main `…api/application/statistics/BattleResultStatisticsRepositoryWithCache.kt` | 1인 캐시 키 `v4` |
| main `…api/domain/statistics/TierScore.kt` 와 조합 네 파일 | 1인 전용 분기 제거(조합 전용으로 남김) |
| `fe/src/domain/statistics/tier.ts`, `sort.ts` | `parseBrawlerTier`, `compareBrawlerTier` |
| `fe/app/_components/statistics/TierBadge.tsx` | 등급 없는 배지 |
| `fe/app/_components/statistics/UngradedTierDescription.tsx` (새로) | 범례 한 줄 |
| `fe/src/api/types.ts`, `fe/src/domain/statistics/types.ts`, `mappers.ts` | `tier` nullable |
| `fe/app/brawlers/_components/BrawlerStatRow.tsx`, `BrawlerStatTable.tsx`, `fe/app/events/[id]/_components/EventResultStatsTable.tsx` | 배지, 정렬, 레일, 범례 |
| `fe/app/about/page.tsx`, `docs/superdomain/DOMAIN.md`, `docs/superglossary/glossary.md` | 문구와 문서 |

---

### Task 1: 산식과 등급 `BrawlerTierScore`

**Files:**
- Create: `…api/domain/statistics/BrawlerTierScore.kt` (main)
- Create: `…api/domain/statistics/BrawlerTierScoreTest.kt` (test)
- Modify: `docs/superglossary/glossary.md`

**Interfaces:**
- Consumes: `ResultCount`(`victoryCount`, `defeatCount`, `drawCount`, `totalCount`, `winRate`), `StatsRate.winRate(victoryCount, defeatCount)`, `TierGrade`(S, A, B, C, D), `BrawlStarsId`.
- Produces:

```kotlin
data class BrawlerTierScore(val value: Int, val grade: TierGrade?) {
    companion object {
        const val GRADE_MIN_BATTLE_COUNT = 300L
        fun create(counts: Map<BrawlStarsId, ResultCount>): Map<BrawlStarsId, BrawlerTierScore>
        fun gradeOf(value: Int, battleCount: Long): TierGrade?
    }
}
```

- [ ] **Step 1: 용어 사전에 다섯 단어를 더한다**

`docs/superglossary/glossary.md` 의 표에 가나다순 위치로 넣는다(가산은 가젯 앞, 상한은 상태 뒤, 중앙값은 주 뒤, 최소는 최근 뒤, 포화는 포인트 뒤).

```markdown
| 가산 | bonus |  |  | 점수에 더하는 값(티어 점수의 인기 가산) |
| 상한 | cap |  |  | 넘지 못하게 묶는 최댓값 |
| 중앙값 | median |  |  | 크기순으로 가운데 값 |
| 최소 | minimum | min |  | 가장 작은 허용값(최소 전투 수) |
| 포화 | saturation |  |  | 커질수록 상한에 붙어 더 늘지 않음 |
```

- [ ] **Step 2: 실패하는 테스트를 쓴다**

```kotlin
package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class BrawlerTierScoreTest {
    private val average = count(5000, 5000)

    /** 입력 순서대로 브롤러 ID 0, 1, 2 … 를 붙여 점수를 낸다. */
    private fun create(vararg counts: ResultCount): List<BrawlerTierScore> {
        val scores =
            BrawlerTierScore.create(
                counts.withIndex().associate { (index, count) -> BrawlStarsId(index.toLong()) to count },
            )
        return counts.indices.map { scores.getValue(BrawlStarsId(it.toLong())) }
    }

    private fun count(victory: Long, defeat: Long, draw: Long = 0) =
        ResultCount(victoryCount = victory, defeatCount = defeat, drawCount = draw)

    private fun score(value: Int, grade: TierGrade?) = BrawlerTierScore(value = value, grade = grade)

    private val b50 = score(50, TierGrade.B)

    @Nested
    inner class Create {
        @Test
        fun `모두 같으면 50점 B 다`() {
            assertThat(create(count(500, 500), count(500, 500), count(500, 500))).containsExactly(b50, b50, b50)
        }

        @Test
        fun `인기만 높으면 54점이고 B 에 머문다`() {
            val small = count(500, 500)
            assertThat(create(count(50000, 50000), small, small, small, small))
                .containsExactly(score(54, TierGrade.B), b50, b50, b50, b50)
        }

        @Test
        fun `인기만 낮으면 46점이고 B 에 머문다`() {
            assertThat(create(count(150, 150), average, average, average, average))
                .containsExactly(score(46, TierGrade.B), b50, b50, b50, b50)
        }

        @Test
        fun `픽률이 같으면 승률 차이만으로 S 부터 D 까지 갈린다`() {
            assertThat(create(count(5600, 4400), count(5300, 4700), average, count(4700, 5300), count(4400, 5600)))
                .containsExactly(
                    score(62, TierGrade.S),
                    score(56, TierGrade.A),
                    b50,
                    score(44, TierGrade.C),
                    score(38, TierGrade.D),
                )
        }

        @Test
        fun `픽률이 중앙값의 2배면 가산이 더해진다`() {
            // 승률 55% 의 수축 승률은 54.93% 라 가산 없이는 60점이다. 가산 +2.2 로 62점이 된다.
            assertThat(create(count(11000, 9000), average, average, average, count(4000, 6000)))
                .containsExactly(score(62, TierGrade.S), b50, b50, b50, score(31, TierGrade.D))
        }

        @Test
        fun `전투 수가 300 미만이면 점수는 있고 등급은 없다`() {
            assertThat(create(count(180, 119), count(180, 120), average, average, average))
                .containsExactly(score(56, null), score(56, TierGrade.A), b50, b50, b50)
        }

        @Test
        fun `표본이 적으면 승률이 높아도 등급이 없다`() {
            assertThat(create(count(90, 10), average, average, average, average))
                .containsExactly(score(66, null), b50, b50, b50, b50)
        }

        @Test
        fun `브롤러 수가 짝수면 중앙값은 가운데 두 값의 평균이다`() {
            assertThat(create(count(1000, 1000), count(2000, 2000), count(3000, 3000), count(6000, 6000)))
                .containsExactly(
                    score(47, TierGrade.B),
                    score(49, TierGrade.B),
                    score(51, TierGrade.B),
                    score(53, TierGrade.B),
                )
        }

        @Test
        fun `무승부는 전투 수에 들어간다`() {
            assertThat(create(count(400, 400, 200), count(500, 500), count(500, 500))).containsExactly(b50, b50, b50)
        }

        @Test
        fun `전투 수가 0 인 항목의 가산은 -4 다`() {
            assertThat(create(count(0, 0), count(500, 500), count(500, 500)))
                .containsExactly(score(46, null), b50, b50)
        }

        @Test
        fun `중앙값이 0 이면 가산은 0 이다`() {
            assertThat(create(count(0, 0), count(0, 0), count(500, 500)))
                .containsExactly(score(50, null), score(50, null), b50)
        }

        @Test
        fun `브롤러가 하나면 50점 B 다`() {
            assertThat(create(count(5500, 4500))).containsExactly(b50)
        }

        @Test
        fun `빈 입력은 빈 결과다`() {
            assertThat(BrawlerTierScore.create(emptyMap())).isEmpty()
        }

        @Test
        fun `무승부뿐이면 모두 50점이다`() {
            assertThat(create(count(0, 0, 500), count(0, 0, 500))).containsExactly(b50, b50)
        }

        @Test
        fun `점수는 0 아래로 내려가지 않는다`() {
            val s69 = score(69, TierGrade.S)
            assertThat(create(count(100, 9900), average, average, average, average))
                .containsExactly(score(0, TierGrade.D), s69, s69, s69, s69)
        }
    }

    @Nested
    inner class GradeOf {
        @Test
        fun `점수 경계는 60 55 45 40 이다`() {
            val grades =
                listOf(60, 59, 55, 54, 45, 44, 40, 39, 0)
                    .map { BrawlerTierScore.gradeOf(value = it, battleCount = 300) }
            assertThat(grades).containsExactly(
                TierGrade.S, TierGrade.A, TierGrade.A, TierGrade.B, TierGrade.B,
                TierGrade.C, TierGrade.C, TierGrade.D, TierGrade.D,
            )
        }

        @Test
        fun `전투 수가 300 미만이면 등급이 없다`() {
            assertThat(BrawlerTierScore.gradeOf(value = 70, battleCount = 299)).isNull()
            assertThat(BrawlerTierScore.gradeOf(value = 70, battleCount = 300)).isEqualTo(TierGrade.S)
        }
    }
}
```

- [ ] **Step 3: 실패를 확인한다**

Run: `./gradlew :core:core-api:test --tests "com.imstargg.core.domain.statistics.BrawlerTierScoreTest"`
Expected: 컴파일 실패(`Unresolved reference: BrawlerTierScore`).

- [ ] **Step 4: `BrawlerTierScore` 를 구현한다**

`create` 는 맥락 전체에서 μ 와 중앙값을 한 번 내고 브롤러마다 아래 식을 적용한다. `grade` 는 `gradeOf(value, totalCount)` 로 채운다.

```kotlin
val averageWinRate = StatsRate.winRate(totalVictoryCount, totalDefeatCount)
val median = median(counts.values.map { it.totalCount })   // 새로 쓰는 private 함수. Double 을 돌려주고 짝수면 가운데 두 값의 평균
// 브롤러마다
val n = resultCount.totalCount.toDouble()
val adjustedWinRate = (n * resultCount.winRate + BAYESIAN_C * averageWinRate) / (n + BAYESIAN_C)
val bonus =
    when {
        median == 0.0 -> 0.0
        n == 0.0 -> -PICK_RATE_BONUS_CAP
        else -> PICK_RATE_BONUS_CAP * tanh(ln(n / median) / ln(PICK_RATE_BONUS_SATURATION))
    }
val value = max(0.0, 50.0 + SCALING_K * (adjustedWinRate - averageWinRate) + bonus).roundToInt()
```

KDoc 에 적을 것(`ComboStatsPolicy` 의 방식):
- 산식 한 줄과 각 상수의 뜻. 가산은 픽률 ÷ 픽률 중앙값의 로그를 tanh 로 ±4점에 묶은 값이고, 전투 수 ÷ 전투 수 중앙값과 같다.
- 상한 4 는 A 경계까지의 거리(5)보다 작아 인기만으로 등급이 바뀌지 않는다. 표본 하한 300 은 수축 승률이 자기 승률과 평균의 중간이 되는 판수다.
- μ 와 중앙값이 조회 맥락(이벤트, 티어 구간, 기간)에서 나오므로 맥락이 바뀌면 같은 브롤러의 점수도 바뀐다.
- 저장하지 않고 조회 때 계산한다. 결과는 Redis `statistics` 캐시에 30분 보관되므로 산식이나 타입을 바꾸면 캐시 키 버전을 올린다.
- 결정 이력: 2026-11 스펙 C2 가 픽률을 곱하던 가중을 버리고 가산으로 바꿨고, 등급 경계를 55/52/48/45 에서 60/55/45/40 으로 넓혔고, 표본 하한을 새로 뒀다. 근거 수치는 스펙 부록 C2.

- [ ] **Step 5: 통과를 확인한다**

Run: `./gradlew :core:core-api:test --tests "com.imstargg.core.domain.statistics.BrawlerTierScoreTest"`
Expected: PASS (17 tests).

- [ ] **Step 6: 커밋한다**

`imstargg-backend/` 에서 `./gradlew ktlintFormat` 을 돌린 뒤 저장소 루트에서:

```bash
git add imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/domain/statistics/BrawlerTierScore.kt imstargg-backend/core/core-api/src/test/kotlin/com/imstargg/core/domain/statistics/BrawlerTierScoreTest.kt docs/superglossary/glossary.md
git commit -m "feat(core-api): 1인 브롤러 티어 점수 산식과 등급 규칙을 추가한다"
```

---

### Task 2: 1인 통계, 응답, 캐시 키에 적용

**Files:**
- Modify: `…api/domain/statistics/BrawlerResultStatistics.kt`
- Modify: `…api/api/controller/v1/response/BrawlerResultStatisticsResponse.kt`
- Modify: `…api/application/statistics/BattleResultStatisticsRepositoryWithCache.kt:27`
- Test: `…api/domain/statistics/BrawlerResultStatisticsTest.kt`, `…api/api/controller/v1/response/BrawlerResultStatisticsResponseTest.kt`, `…api/application/statistics/StatisticsCacheKeyTest.kt`, `…api/config/CacheConfigSerializerTest.kt`

**Interfaces:**
- Consumes: Task 1 의 `BrawlerTierScore.create`, `BrawlerTierScore(value, grade)`.
- Produces: `BrawlerResultStatisticsItem.tierScore: BrawlerTierScore`, `BrawlerResultStatisticsItemResponse.tier: String?`.

- [ ] **Step 1: 실패하는 테스트를 쓴다**

`BrawlerResultStatisticsTest` 의 `Create` 에 더한다.

```kotlin
@Test
fun `등급 있는 항목이 먼저 오고 그 안에서 점수와 전투 수 내림차순이다`() {
    // given: a 58/A, b 46/B(1만 판), c 46/B(9500판), d 65/없음(100판), e 44/없음(200판). 입력 순서는 섞는다
    val event = BrawlStarsId(1L)
    val (a, b, c, d, e) = (10L..14L).map { BrawlStarsId(it) }
    val rows =
        listOf(
            BrawlerEventResultCount(event, c, ResultCount(4750, 4750, 0), 0),
            BrawlerEventResultCount(event, e, ResultCount(100, 100, 0), 0),
            BrawlerEventResultCount(event, a, ResultCount(5600, 4400, 0), 0),
            BrawlerEventResultCount(event, d, ResultCount(90, 10, 0), 0),
            BrawlerEventResultCount(event, b, ResultCount(5000, 5000, 0), 0),
        )

    // when
    val sut = BrawlerResultStatistics.create(rows)

    // then
    assertThat(sut.items.map { it.brawlerId }).containsExactly(a, b, c, d, e)
    assertThat(sut.items.map { it.tierScore }).containsExactly(
        BrawlerTierScore(value = 58, grade = TierGrade.A),
        BrawlerTierScore(value = 46, grade = TierGrade.B),
        BrawlerTierScore(value = 46, grade = TierGrade.B),
        BrawlerTierScore(value = 65, grade = null),
        BrawlerTierScore(value = 44, grade = null),
    )
}
```

`BrawlerResultStatisticsResponseTest` 에 더한다(import: `BrawlStarsId`, `Brawlers`, `BrawlerResultStatistics`, `BrawlerResultStatisticsItem`, `BrawlerTierScore`).

```kotlin
@Test
fun `등급이 없으면 tier 는 null 이고 tierScore 는 값이 있다`() {
    // given
    val statistics =
        BrawlerResultStatistics(
            totalBattleCount = 10,
            items =
                listOf(
                    BrawlerResultStatisticsItem(
                        brawlerId = BrawlStarsId(1L),
                        battleCount = 100,
                        pickRate = 0.1,
                        winRate = 0.9,
                        starPlayerRate = 0.0,
                        tierScore = BrawlerTierScore(value = 66, grade = null),
                    ),
                ),
        )

    // when
    val sut = BrawlerResultStatisticsResponse.of(statistics, Brawlers(emptyList()))
    val json = ObjectMapper().readTree(ObjectMapper().writeValueAsString(sut))
    val item = json.get("items").get(0)

    // then
    assertThat(item.has("tier")).isTrue()
    assertThat(item.get("tier").isNull).isTrue()
    assertThat(item.get("tierScore").asInt()).isEqualTo(66)
}
```

`StatisticsCacheKeyTest` 의 스냅샷에서 `findBrawlerResultStatistics` 의 기대값을 `'v4:brawlerResultStats:' + …` 로 바꾼다(나머지 문자열은 그대로).

`CacheConfigSerializerTest` 의 `BrawlerResultStatistics 직렬화 라운드트립 - tierScore 포함` 을 `… - 등급이 있는 항목과 없는 항목` 으로 바꾸고, 항목을 둘로 한다. 첫 항목은 `tierScore = BrawlerTierScore(value = 56, grade = TierGrade.A)`, 둘째 항목은 다른 `brawlerId` 에 `tierScore = BrawlerTierScore(value = 66, grade = null)` 이다. 단언(`isEqualTo(original)`)은 그대로다.

- [ ] **Step 2: 실패를 확인한다**

Run: `./gradlew :core:core-api:test --tests "*BrawlerResultStatisticsTest" --tests "*BrawlerResultStatisticsResponseTest" --tests "*StatisticsCacheKeyTest" --tests "*CacheConfigSerializerTest"`
Expected: 컴파일 실패(`tierScore` 의 타입 불일치).

- [ ] **Step 3: 구현한다**

- `BrawlerResultStatisticsItem.tierScore` 의 타입을 `BrawlerTierScore` 로 바꾼다.
- `BrawlerResultStatistics.create` 는 `BrawlerTierScore.create(brawlerAggregates.associate { it.brawlerId to it.resultCount })` 로 점수를 내고, 항목을 "등급 있는 항목 먼저(`tierScore.grade != null`), 점수 내림차순, `battleCount` 내림차순" 으로 정렬한다.
- `BrawlerResultStatisticsItemResponse.tier` 를 `String?` 로 바꾸고 `item.tierScore.grade?.name` 을 넣는다.
- `findBrawlerResultStatistics` 의 캐시 키 앞머리를 `'v4:brawlerResultStats:'` 로 올린다.

- [ ] **Step 4: 통과를 확인한다**

Run: `./gradlew :core:core-api:test`
Expected: PASS. 직렬화 왕복이 등급 `null` 에서 실패하면 고치지 말고 BLOCKED 로 보고한다(캐시 직렬화 설정을 바꾸는 일은 이 계획의 범위 밖이다).

- [ ] **Step 5: 커밋한다**

`imstargg-backend/` 에서 `./gradlew ktlintFormat` 을 돌린 뒤 저장소 루트에서:

```bash
git add imstargg-backend/core/core-api/src
git commit -m "feat(core-api): 1인 통계가 새 티어 점수를 쓰고 표본 부족이면 tier 를 비운다"
```

---

### Task 3: `TierScore` 에서 1인 전용 분기 제거

**Files:**
- Modify: `…api/domain/statistics/TierScore.kt`
- Modify: `…api/domain/statistics/BrawlerPairResultStatistics.kt:40`, `BrawlerTrioResultStatistics.kt:41`, `PairResultRanking.kt:26`, `TrioResultRanking.kt:27`
- Test: `…api/domain/statistics/TierScoreTest.kt`

**Interfaces:**
- Produces: `fun <K> TierScore.create(counts: Map<K, ResultCount>, baselineWinRate: Double? = null): Map<K, TierScore>`. `TierScore.grade()` 와 등급 경계(55/52/48/45)는 그대로다.

- [ ] **Step 1: 테스트를 새 시그니처에 맞춘다**

`TierScoreTest` 에서:
- 모든 `TierScore.create(…)` 호출에서 `pickRateWeight = …` 인자를 뺀다.
- `높은 승률이라도 매우 낮은 픽률이면 점수가 억제된다` 를 지운다(곱 가중에만 해당한다).
- `가중을 끄면 점수는 수축 승률만으로 정해진다` 를 `점수는 수축 승률만으로 정해진다` 로 바꾸고 `on` 변수와 그 단언을 지운다. 주석의 "가중 on 이면 …" 문장도 지운다.
- `가중을 꺼도 n 이 0 인 키는 50 이다` 를 `n 이 0 인 키는 50 이다` 로 바꾼다.

- [ ] **Step 2: 실패를 확인한다**

Run: `./gradlew :core:core-api:test --tests "com.imstargg.core.domain.statistics.TierScoreTest"`
Expected: 컴파일 실패(`pickRateWeight` 인자가 필요함).

- [ ] **Step 3: 구현한다**

- `TierScore.create` 에서 `pickRateWeight` 인자, 최대 픽률 계산, 가중 분기, 상수 `LOG_ALPHA`·`MIN_WEIGHT_BETA`, 쓰이지 않게 된 import 를 지운다. 점수는 `50 + (수축 승률 − 기준 평균 승률) × SCALING_K` 다.
- 조합 네 곳의 호출에서 `pickRateWeight = false,` 줄만 지운다.
- `TierScore` 의 KDoc 을 고친다: 조합(2인·3인) 전용이고 1인 브롤러는 `BrawlerTierScore` 를 쓴다는 것, 산식에서 가중 설명을 뺀 것, 조합 등급은 "조합 통계 배포 전 정정" 스펙이 지울 예정이라는 것.

- [ ] **Step 4: 통과를 확인한다**

Run(`imstargg-backend/`): `./gradlew :core:core-api:test`
Expected: PASS. 조합 테스트(`BrawlerPairResultStatisticsTest` 등)는 고치지 않고 통과해야 한다.

Run(저장소 루트): `git grep -n "pickRateWeight" -- imstargg-backend`
Expected: 출력 없음(`git grep` 은 찾은 것이 없으면 종료 코드 1 이다. 실패가 아니다).

- [ ] **Step 5: 커밋한다**

`imstargg-backend/` 에서 `./gradlew ktlintFormat` 을 돌린 뒤 저장소 루트에서:

```bash
git add imstargg-backend/core/core-api/src
git commit -m "refactor(core-api): TierScore 에서 1인 전용 픽률 가중을 지운다"
```

---

### Task 4: 프론트의 등급 없는 상태 부품

**Files:**
- Modify: `fe/src/domain/statistics/tier.ts`, `fe/src/domain/statistics/sort.ts`
- Modify: `fe/app/_components/statistics/TierBadge.tsx`
- Create: `fe/app/_components/statistics/UngradedTierDescription.tsx`
- Test: `fe/tests/unit/domain/statistics/tier.test.ts`, `sort.test.ts`, `fe/tests/unit/components/statistics/TierBadge.test.tsx`
- Create: `fe/tests/unit/components/statistics/UngradedTierDescription.test.tsx`

**Interfaces:**
- Produces:

```ts
// tier.ts
export function parseBrawlerTier(value: unknown): StatTier | null;
// sort.ts
export function compareBrawlerTier(
  a: { tier: StatTier | null; tierScore: number } | null,
  b: { tier: StatTier | null; tierScore: number } | null,
  order: SortOrder,
): number;
// TierBadge.tsx
export function TierBadge({ tier }: { tier: StatTier | null });
// UngradedTierDescription.tsx
export function UngradedTierDescription();
```

- [ ] **Step 1: 실패하는 테스트를 쓴다**

`tier.test.ts` 에 더한다.

```ts
describe("parseBrawlerTier", () => {
  it("S~D 는 그 등급이다", () => {
    expect(parseBrawlerTier("S")).toBe("S");
    expect(parseBrawlerTier("D")).toBe("D");
  });
  it("소문자와 공백을 정규화한다", () => {
    expect(parseBrawlerTier(" a ")).toBe("A");
  });
  it("null·누락·모르는 문자열은 null 이다", () => {
    expect(parseBrawlerTier(null)).toBeNull();
    expect(parseBrawlerTier(undefined)).toBeNull();
    expect(parseBrawlerTier("F")).toBeNull();
    expect(parseBrawlerTier("")).toBeNull();
  });
});
```

`sort.test.ts` 에 더한다.

```ts
describe("compareBrawlerTier", () => {
  const s = { id: "s", tier: "S" as const, tierScore: 62 };
  const b = { id: "b", tier: "B" as const, tierScore: 50 };
  const high = { id: "high", tier: null, tierScore: 66 };
  const low = { id: "low", tier: null, tierScore: 46 };
  const sortedIds = (order: SortOrder) =>
    [low, b, null, high, s]
      .sort((x, y) => compareBrawlerTier(x, y, order))
      .map((x) => x?.id ?? "none");

  it("내림차순이면 등급 있는 행이 점수순으로 먼저 오고 등급 없는 행이 뒤에 온다", () => {
    expect(sortedIds("DESC")).toEqual(["s", "b", "high", "low", "none"]);
  });
  it("오름차순에서도 등급 없는 행은 아래다", () => {
    expect(sortedIds("ASC")).toEqual(["b", "s", "none", "low", "high"]);
  });
});
```

`TierBadge.test.tsx` 에 더한다.

```tsx
it("등급이 없으면 – 와 표본 부족 라벨을 그린다", () => {
  render(<TierBadge tier={null} />);
  const badge = screen.getByLabelText("표본 부족");
  expect(badge).toHaveTextContent("–");
  expect(badge).toHaveAttribute("title", "표본 부족");
});
```

`UngradedTierDescription.test.tsx` 를 만든다.

```tsx
it("범례 문구를 그린다", () => {
  render(<UngradedTierDescription />);
  expect(screen.getByText("– 표본 부족: 전투 수가 적어 티어를 매기지 않습니다.")).toBeInTheDocument();
});
```

각 테스트 파일에 필요한 import(`parseBrawlerTier`, `compareBrawlerTier`, `SortOrder` 타입, `UngradedTierDescription`, `render`·`screen`)를 더한다.

- [ ] **Step 2: 실패를 확인한다**

Run: `pnpm test:run tests/unit/domain/statistics tests/unit/components/statistics`
Expected: FAIL(`parseBrawlerTier`, `compareBrawlerTier`, `UngradedTierDescription` 이 없음, `TierBadge` 가 `null` 을 그리지 못함).

- [ ] **Step 3: 구현한다**

- `parseBrawlerTier`: `parseTier` 와 같은 정규화(trim, 대문자)를 하고 S ~ D 가 아니면 `null` 이다. `parseTier` 는 고치지 않는다.
- `compareBrawlerTier`: 등급이 있는 쪽(`tier` 가 `null` 이 아님)이 `order` 와 관계없이 먼저다. 둘 다 있거나 둘 다 없으면 `tierScore` 차이에 `order` 의 부호를 곱한다. 인자가 `null` 이면 등급 없음, 점수 0 이다.
- `TierBadge`: `tier` 가 `null` 이면 같은 크기(`h-7 w-7`, 원형)의 채우지 않은 배지를 그린다. 글자는 `–`, `aria-label` 과 `title` 은 `표본 부족`, 테두리와 글자색은 `#5C6271`, 배경과 그림자는 없다. 등급이 있을 때의 모양은 그대로다.
- `UngradedTierDescription`: `<p className="px-4 py-3 text-xs text-[#5C6271]">` 안에 범례 문구를 그린다.

- [ ] **Step 4: 통과를 확인한다**

Run: `pnpm test:run tests/unit/domain/statistics tests/unit/components/statistics && pnpm typecheck && pnpm lint`
Expected: 모두 PASS.

- [ ] **Step 5: 커밋한다**

저장소 루트에서:

```bash
git add imstargg-frontend/src/domain/statistics imstargg-frontend/app/_components/statistics imstargg-frontend/tests/unit
git commit -m "feat(frontend): 등급 없는 티어의 배지, 범례, 정렬 비교를 추가한다"
```

---

### Task 5: 프론트 타입과 두 표

**Files:**
- Modify: `fe/src/api/types.ts`(`BrawlerResultStatisticsItemResponse.tier`), `fe/src/domain/statistics/types.ts`(`BrawlerResultStat.tier`), `fe/src/domain/statistics/mappers.ts`(`toBrawlerResultStat`)
- Modify: `fe/app/brawlers/_components/BrawlerStatRow.tsx`, `BrawlerStatTable.tsx`
- Modify: `fe/app/events/[id]/_components/EventResultStatsTable.tsx`
- Test: `fe/tests/unit/domain/statistics/mappers.test.ts`, `fe/tests/components/EventStatsTables.test.tsx`
- Create: `fe/tests/components/BrawlerStatTable.test.tsx`

**Interfaces:**
- Consumes: Task 4 의 `parseBrawlerTier`, `compareBrawlerTier`, `TierBadge`(`tier: StatTier | null`), `UngradedTierDescription`.
- Produces: `BrawlerResultStatisticsItemResponse.tier: string | null`, `BrawlerResultStat.tier: StatTier | null`.

- [ ] **Step 1: 실패하는 테스트를 쓴다**

`mappers.test.ts` 의 `toBrawlerResultStatistics` 에 더한다: 항목의 `tier` 가 `null` 이면 결과의 `tier` 가 `null` 이고, `"F"` 여도 `null` 이다(`toBeNull()`).

`BrawlerStatTable.test.tsx` 를 만든다. 픽스처는 브롤러 셋이다: "가가"(`tier: "S"`, `tierScore: 62`, `winRate: 0.56`), "나나"(`tier: "B"`, `tierScore: 50`, `winRate: 0.5`), "다다"(`tier: null`, `tierScore: 66`, `winRate: 0.9`, `battleCount: 100`). 행은 `screen.getAllByRole("link")` 의 순서로 읽고, 범례는 `/표본 부족: 전투 수가 적어 티어를 매기지 않습니다/` 로 찾는다.

| 테스트 이름 | 준비 | 단언 |
|---|---|---|
| 티어 정렬에서 등급 없는 행은 내림차순·오름차순 모두 아래다 | 행을 다다, 나나, 가가 순으로 준다 | 처음 순서는 가가, 나나, 다다. `티어` 머리글 버튼을 누르면 나나, 가가, 다다 |
| 등급 없는 행은 승률 1위여도 neutral 레일이다 | `initialSort="winRate"` | 행의 `data-rail` 이 차례로 `neutral`, `silver`, `bronze` |
| 통계가 없는 브롤러는 표본 부족 배지다 | `stat: null` 인 행 하나 | `getByLabelText("표본 부족")` 이 있고 `queryByLabelText(/D 티어/)` 는 없다 |
| 통계가 0건이면 전 브롤러가 표본 부족 배지와 neutral 레일이고 범례가 보인다 | `stat: null` 인 행 셋 | 표본 부족 라벨 3개, 모든 행의 `data-rail` 이 `neutral`, 범례가 있다 |
| 범례는 보이는 행에 등급 없는 행이 있을 때만 그린다 | (1) 가가, 나나 (2) 가가, 다다 | (1) 범례 없음 (2) 범례 있음. (2)에서 검색 입력(`placeholder` "브롤러 이름 검색")에 "가가" 를 넣으면 범례가 사라진다 |

`EventStatsTables.test.tsx` 에 더한다(기존 `brawler`, `resultItem` 픽스처를 쓴다).

| 테스트 이름 | 준비 | 단언 |
|---|---|---|
| 등급 없는 결과형 행은 표본 부족 배지와 neutral 레일이고 범례가 보인다 | `{ brawler, ...resultItem, tier: null }` 하나 | 행의 `data-rail` 이 `neutral`(1위여도), `getByLabelText("표본 부족")`, 범례가 있다 |
| 등급이 모두 있으면 범례가 없다 | `{ brawler, ...resultItem }` 하나 | 범례 없음 |
| 결과형 표의 티어 정렬에서 등급 없는 행은 아래다 | 등급 없는 항목(`tierScore: 66`)과 등급 B 항목(`tierScore: 50`, 다른 브롤러)을 이 순서로 준다 | 첫 행이 등급 B 항목이다 |

- [ ] **Step 2: 실패를 확인한다**

Run: `pnpm test:run tests/components/BrawlerStatTable.test.tsx tests/components/EventStatsTables.test.tsx tests/unit/domain/statistics/mappers.test.ts`
Expected: FAIL(매퍼가 `null` 을 D 로 바꿈, 통계 없는 행이 D 배지, 범례 없음).

- [ ] **Step 3: 구현한다**

- 타입 두 곳의 `tier` 를 nullable 로 바꾼다. `src/api/types.ts` 의 주석은 `// "S" | "A" | "B" | "C" | "D", 표본 부족이면 null` 로 한다. 조합 타입 세 개(`PairResultStat`, `TrioResultStat`, `ComboRankingStat`)는 그대로다.
- `toBrawlerResultStat` 는 `parseBrawlerTier(dto.tier)` 를 쓴다. 조합 매퍼는 `parseTier` 그대로다.
- `BrawlerStatRow`: `railFor` 의 `tier` 인자를 `StatTier | null` 로 바꾸고, `null` 이면 순위와 관계없이 `"neutral"` 을 돌려준다(순위 검사보다 먼저). 배지에는 `stat?.tier ?? null` 을 넘긴다.
- `BrawlerStatTable`: 티어 정렬은 `compareBrawlerTier(a.stat, b.stat, order)` 를 쓴다. 표의 행 뒤에, 지금 보이는 행(`sorted`) 중 `stat?.tier` 가 없는 행이 하나라도 있으면 `<UngradedTierDescription />` 을 그린다.
- `EventResultStatsTable`: `rowRail` 의 `tier` 인자를 `StatTier | null` 로 바꾸고 `null` 이면 `"neutral"` 이다. 티어 정렬은 `compareBrawlerTier(a, b, order)` 를 쓴다. 범례 조건은 위와 같다(`it.tier === null`).
- 두 표의 레일 함수는 합치지 않는다. 각 표의 테스트가 고정한다.

- [ ] **Step 4: 통과를 확인한다**

Run: `pnpm typecheck && pnpm lint && pnpm test:run`
Expected: 모두 PASS. D 배지를 기대하던 기존 테스트가 있으면 새 규칙(통계 없는 행은 표본 부족)에 맞춘다.

- [ ] **Step 5: 커밋한다**

저장소 루트에서:

```bash
git add imstargg-frontend/src imstargg-frontend/app/brawlers imstargg-frontend/app/events imstargg-frontend/tests
git commit -m "feat(frontend): 표본이 적은 브롤러를 등급 대신 표본 부족으로 보인다"
```

---

### Task 6: 소개 페이지와 도메인 정의

**Files:**
- Modify: `fe/app/about/page.tsx`
- Modify: `docs/superdomain/DOMAIN.md`

- [ ] **Step 1: 소개 페이지에 문단을 더한다**

`<LegalSection title="데이터 출처와 갱신">` 바로 앞에 `<LegalSection title="티어 산정 방식">` 을 넣고 `<p>` 하나에 아래 문장을 그대로 적는다. 큰따옴표는 같은 파일의 기존 표기처럼 `&ldquo;`, `&rdquo;` 로 쓴다.

> 브롤러 티어(S~D)는 승률과 픽률을 함께 반영한 점수로 매깁니다. 승률은 선택한 조건(티어 구간·기간·맵)의 평균과 비교하며, 전투 수가 적은 브롤러의 승률은 평균 쪽으로 보정합니다. 픽률이 높으면 점수를 조금 더하고 낮으면 조금 빼지만, 그 폭에는 상한이 있어 승률이 평균 수준인 브롤러가 픽률만으로 높은 티어를 받지는 않습니다. 전투 수가 기준보다 적은 브롤러는 티어를 매기지 않고 "–"(표본 부족)로 표시합니다. 브롤러 메타 페이지의 통계는 젬 그랩·브롤 볼 같은 3대3 모드의 전투를 합친 값이며, 쇼다운은 포함하지 않습니다.

- [ ] **Step 2: 도메인 정의의 미정 항목을 지운다**

`docs/superdomain/DOMAIN.md` 의 statistics "미정" 에서 `- '전체 이벤트' 합산 범위를 핵심 6모드로 좁힐지.` 한 줄을 지운다. 다른 미정 항목은 그대로 둔다.

- [ ] **Step 3: 확인하고 커밋한다**

Run: `pnpm typecheck && pnpm lint`
Expected: PASS. 이 문단을 검증하는 테스트는 만들지 않는다(기존 문단과 같다). 저장소 루트에서 커밋한다.

```bash
git add imstargg-frontend/app/about/page.tsx docs/superdomain/DOMAIN.md
git commit -m "docs: 소개 페이지에 티어 산정 방식을 적고 6모드 미정 항목을 닫는다"
```

---

### Task 7: 전체 확인

- [ ] **Step 1: 백엔드 전체**

Run: `./gradlew test ktlintCheck`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 2: 프론트 전체**

Run: `pnpm typecheck && pnpm lint && pnpm test:run`
Expected: 모두 PASS.

- [ ] **Step 3: 남은 흔적 확인**

Run(저장소 루트): `git grep -n -E "pickRateWeight|v3:brawlerResultStats" -- imstargg-backend imstargg-frontend`
Expected: 출력 없음(종료 코드 1).

- [ ] **Step 4: 컨트롤러가 마무리한다**

- `/superglossary:check` 로 새 이름이 용어 사전을 따르는지 본다.
- 사용자에게 `/superdomain:adr` 로 산식 결정을 기록할지 제안한다(스펙 §3.6).
- 버전 올림(`/release bump`)과 PR 은 사용자가 요청할 때 한다. 릴리즈 노트에 적을 것은 스펙 §6 에 있다.
