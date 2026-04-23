# core-batch 데이터 정확성 번들 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** rank 전투 통계의 중복 집계(1-A), 수동 EM 트랜잭션(1-B/2-B), 자연키 unique 누락(2-C), 재실행 불가(3-A) 를 한 번에 해소한다.

**Architecture:**
- Rank 전투 `battle_key` 를 "자기 팀 플레이어 태그" 기반으로 재정의하고 `stats_rank_battle` unique 를 `(battle_date, battle_key)` 로 축소 → 같은 팀 여러 수집분이 1 row 로 merge 되어 aggregator 수정 없이 중복 제거.
- `BattleKeyBuilder` 를 `db-core` 로 이전해 `BattleKeyEmbedded` 값 객체로 재구성 (엔티티 컬럼 타입은 `String` 유지).
- 3 개 tasklet 의 수동 `entityManagerFactory.createEntityManager()` + `em.transaction.*` 을 제거하고 Spring-managed `EntityManager` 를 생성자 주입받아 Spring Batch TaskletStep 이 이미 열어둔 트랜잭션에 합류.
- 5 개 통계 테이블에 자연키 unique index 추가.
- 재실행은 운영 문서(runbook) 만 추가. 코드 변경 없음.

**Tech Stack:** Kotlin 2.2.21, Spring Boot 4.0.2, Spring Batch 6, Hibernate + Querydsl 7.1, PostgreSQL, Flyway, AssertJ, JUnit 5.

---

## File Structure

### 신규
- `imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/battle/BattleKeyEmbedded.kt`
- `imstargg-backend/infrastructure/db-core/src/test/kotlin/com/imstargg/infrastructure/db/core/statistics/battle/BattleKeyEmbeddedTest.kt`
- `imstargg-backend/infrastructure/db-core/src/main/resources/db/migration/V260423a__stats_rank_battle_unique_key.sql`
- `imstargg-backend/infrastructure/db-core/src/main/resources/db/migration/V260423b__stats_natural_key_unique.sql`

### 수정
- `imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/battle/StatsRankBattleEntity.kt` (unique index 정의 교체)
- `imstargg-backend/infrastructure/db-core/src/main/resources/schema.sql` (rank unique + 자연키 unique)
- `imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/battle/repository/StatsRankBattleJpaRepositoryCustom.kt` (시그니처 변경)
- `imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/battle/repository/StatsRankBattleJpaRepositoryCustomImpl.kt` (구현 변경)
- `imstargg-backend/core/core-worker/src/main/kotlin/com/imstargg/core/worker/domain/BattleBuilder.kt` (rank/result 분기 key 생성)
- `imstargg-backend/core/core-worker/src/main/kotlin/com/imstargg/core/worker/application/StatsBattleAggregateApplier.kt` (distinctBy + dedup 키 통일)
- `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/DailyBattleStatsTasklet.kt`
- `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/PerEventDailyBattleStatsTasklet.kt`
- `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/PerEventWeeklyBattleStatsTasklet.kt`
- `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerBattleRankStatsJobConfig.kt`
- `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerBattleResultStatsJobConfig.kt`
- `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerTeamBattleRankStatsJobConfig.kt`
- `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerTeamBattleResultStatsJobConfig.kt`
- `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerEnemyBattleResultStatsJobConfig.kt`
- `imstargg-backend/docs/stats-battle-data-strategy.md` (BattleKey 섹션 + 재실행 runbook 추가)

### 삭제
- `imstargg-backend/core/core-worker/src/main/kotlin/com/imstargg/core/worker/domain/BattleKeyBuilder.kt`
- `imstargg-backend/core/core-worker/src/test/kotlin/com/imstargg/core/worker/domain/BattleKeyBuilderTest.kt`
- `imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/battle/repository/BattleKeyPlayerTagProjection.kt`

---

## Task 1: `BattleKeyEmbedded` 값 객체 신설 + 기존 `BattleKeyBuilder` 테스트 이관

**Files:**
- Create: `imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/battle/BattleKeyEmbedded.kt`
- Create: `imstargg-backend/infrastructure/db-core/src/test/kotlin/com/imstargg/infrastructure/db/core/statistics/battle/BattleKeyEmbeddedTest.kt`

### Step 1: `BattleKeyEmbeddedTest` (이관된 시나리오 + rank/result 분기) 작성

- [ ] 다음 내용으로 `BattleKeyEmbeddedTest.kt` 작성. 기존 `BattleKeyBuilderTest` 의 6 개 시나리오를 `forResult` / `forRank` 양쪽으로 이관.

```kotlin
package com.imstargg.infrastructure.db.core.statistics.battle

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class BattleKeyEmbeddedTest {
    private val battleTime =
        ZonedDateTime.of(LocalDateTime.of(2021, 1, 1, 0, 0), ZoneId.systemDefault()).toOffsetDateTime()
    private val anotherTime =
        ZonedDateTime.of(LocalDateTime.of(2021, 1, 2, 0, 0), ZoneId.systemDefault()).toOffsetDateTime()

    @Nested
    inner class forResult {
        @Test
        fun `길이64의 배틀키를 생성한다`() {
            val sut = BattleKeyEmbedded.forResult(battleTime, listOf("player1", "player2", "player3"))

            assertThat(sut.value).hasSize(64)
        }

        @Test
        fun `전투 시간과 플레이어가 같으면 같은 키를 생성한다`() {
            val result1 = BattleKeyEmbedded.forResult(battleTime, listOf("player1", "player2", "player3"))
            val result2 = BattleKeyEmbedded.forResult(battleTime, listOf("player1", "player2", "player3"))

            assertThat(result1).isEqualTo(result2)
        }

        @Test
        fun `플레이어 태그 순서는 달라도 된다`() {
            val result1 = BattleKeyEmbedded.forResult(battleTime, listOf("player1", "player2", "player3"))
            val result2 = BattleKeyEmbedded.forResult(battleTime, listOf("player2", "player1", "player3"))

            assertThat(result1).isEqualTo(result2)
        }

        @Test
        fun `날짜가 다르면 다른 키가 생성된다`() {
            val result1 = BattleKeyEmbedded.forResult(battleTime, listOf("player1", "player2", "player3"))
            val result2 = BattleKeyEmbedded.forResult(anotherTime, listOf("player1", "player2", "player3"))

            assertThat(result1).isNotEqualTo(result2)
        }

        @Test
        fun `플레이어 수가 다르면 다른 키가 생성된다`() {
            val result1 = BattleKeyEmbedded.forResult(battleTime, listOf("player1", "player2", "player3"))
            val result2 = BattleKeyEmbedded.forResult(battleTime, listOf("player1", "player2"))

            assertThat(result1).isNotEqualTo(result2)
        }

        @Test
        fun `플레이어가 다르면 다른 키가 생성된다`() {
            val result1 = BattleKeyEmbedded.forResult(battleTime, listOf("player1", "player2", "player3"))
            val result2 = BattleKeyEmbedded.forResult(battleTime, listOf("player4", "player5", "player6"))

            assertThat(result1).isNotEqualTo(result2)
        }
    }

    @Nested
    inner class forRank {
        @Test
        fun `자기 팀 태그만으로 키를 생성한다`() {
            val myTeam = listOf("A", "B", "C")

            val sut = BattleKeyEmbedded.forRank(battleTime, myTeam)

            assertThat(sut.value).hasSize(64)
        }

        @Test
        fun `같은 팀의 다른 수집 주체는 같은 키를 생성한다`() {
            val myTeam = listOf("A", "B", "C")

            val fromA = BattleKeyEmbedded.forRank(battleTime, myTeam)
            val fromB = BattleKeyEmbedded.forRank(battleTime, myTeam)

            assertThat(fromA).isEqualTo(fromB)
        }

        @Test
        fun `다른 팀은 다른 키를 생성한다`() {
            val ourTeam = listOf("A", "B", "C")
            val enemyTeam = listOf("D", "E", "F")

            val ours = BattleKeyEmbedded.forRank(battleTime, ourTeam)
            val theirs = BattleKeyEmbedded.forRank(battleTime, enemyTeam)

            assertThat(ours).isNotEqualTo(theirs)
        }
    }

    @Nested
    inner class forResult_vs_forRank {
        @Test
        fun `같은 태그 집합이어도 forResult 와 forRank 는 같은 hash 를 반환한다`() {
            val tags = listOf("A", "B", "C")

            val asResult = BattleKeyEmbedded.forResult(battleTime, tags)
            val asRank = BattleKeyEmbedded.forRank(battleTime, tags)

            assertThat(asResult).isEqualTo(asRank)
        }
    }
}
```

> 마지막 테스트는 "팩토리 분리는 호출 의미를 분리하기 위한 것이며, 입력이 같으면 해시는 같다" 는 계약을 명시화한다.

### Step 2: 테스트 실행해 실패 확인

- [ ] Run: `./gradlew :infrastructure:db-core:test --tests "com.imstargg.infrastructure.db.core.statistics.battle.BattleKeyEmbeddedTest"`
- [ ] Expected: `BattleKeyEmbedded` 미존재로 컴파일 실패.

### Step 3: `BattleKeyEmbedded` 구현

- [ ] 다음 내용으로 `BattleKeyEmbedded.kt` 작성.

```kotlin
package com.imstargg.infrastructure.db.core.statistics.battle

import java.nio.ByteBuffer
import java.security.MessageDigest
import java.time.OffsetDateTime

@JvmInline
value class BattleKeyEmbedded(val value: String) {
    companion object {
        fun forResult(
            battleTime: OffsetDateTime,
            playerTags: Collection<String>,
        ): BattleKeyEmbedded = hash(battleTime, playerTags)

        fun forRank(
            battleTime: OffsetDateTime,
            playerTeamTags: Collection<String>,
        ): BattleKeyEmbedded = hash(battleTime, playerTeamTags)

        private fun hash(
            battleTime: OffsetDateTime,
            tags: Collection<String>,
        ): BattleKeyEmbedded {
            val joined =
                tags
                    .sorted()
                    .joinToString("")
                    .toByteArray(Charsets.UTF_8)
            val buffer = ByteBuffer.allocate(Long.SIZE_BYTES + joined.size)
            buffer.putLong(battleTime.toEpochSecond())
            buffer.put(joined)
            val digest = MessageDigest.getInstance("SHA-256").digest(buffer.array())
            return BattleKeyEmbedded(digest.toHex())
        }

        private fun ByteArray.toHex(): String =
            joinToString("") { byte ->
                val hex = Integer.toHexString(0xff and byte.toInt())
                if (hex.length == 1) "0$hex" else hex
            }
    }
}
```

### Step 4: 테스트 통과 확인

- [ ] Run: `./gradlew :infrastructure:db-core:test --tests "com.imstargg.infrastructure.db.core.statistics.battle.BattleKeyEmbeddedTest"`
- [ ] Expected: 모든 테스트 PASS (10 개).

### Step 5: 커밋

- [ ] Run:

```bash
git add imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/battle/BattleKeyEmbedded.kt \
        imstargg-backend/infrastructure/db-core/src/test/kotlin/com/imstargg/infrastructure/db/core/statistics/battle/BattleKeyEmbeddedTest.kt
git commit -m "feat(db-core): BattleKeyEmbedded 값 객체 신설 (forResult / forRank 분기)"
```

---

## Task 2: `BattleBuilder` 가 `BattleKeyEmbedded` 를 사용하도록 전환

**Files:**
- Modify: `imstargg-backend/core/core-worker/src/main/kotlin/com/imstargg/core/worker/domain/BattleBuilder.kt`
- Delete: `imstargg-backend/core/core-worker/src/main/kotlin/com/imstargg/core/worker/domain/BattleKeyBuilder.kt`
- Delete: `imstargg-backend/core/core-worker/src/test/kotlin/com/imstargg/core/worker/domain/BattleKeyBuilderTest.kt`
- Test: `imstargg-backend/core/core-worker/src/test/kotlin/com/imstargg/core/worker/domain/BattleBuilderTest.kt` (존재하면 확장, 없으면 신규)

### Step 1: 기존 `BattleBuilderTest` 존재 여부 확인 및 테스트 초안 작성

- [ ] Run: `ls imstargg-backend/core/core-worker/src/test/kotlin/com/imstargg/core/worker/domain/BattleBuilderTest.kt 2>/dev/null || echo "not found"`
- [ ] 존재하지 않으면 다음 내용으로 신규 생성, 존재하면 아래 `@Nested class 배틀키_생성` 만 추가.

```kotlin
package com.imstargg.core.worker.domain

import com.imstargg.client.brawlstars.response.BattleResponse
import com.imstargg.client.brawlstars.response.BattleResultBrawlerResponse
import com.imstargg.client.brawlstars.response.BattleResultPlayerResponse
import com.imstargg.client.brawlstars.response.EventResponse
import com.imstargg.client.brawlstars.response.BattleResultResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime

class BattleBuilderTest {
    private val battleTime = OffsetDateTime.parse("2026-04-23T10:00:00+09:00")

    private fun player(tag: String) =
        BattleResultPlayerResponse(
            tag = tag,
            name = "name-$tag",
            brawler =
                BattleResultBrawlerResponse(
                    id = 1,
                    name = "SHELLY",
                    power = 11,
                    trophies = 800,
                    trophyChange = 0,
                ),
            brawlers = null,
        )

    @Nested
    inner class `배틀키 생성` {
        @Test
        fun `rank 응답은 자기 팀 태그만으로 키를 생성한다`() {
            // given
            val response =
                BattleResponse(
                    battleTime = battleTime,
                    event = EventResponse(id = 1L, mode = "soloShowdown", modeId = 1, map = "MAP"),
                    battle =
                        BattleResultResponse(
                            mode = "soloShowdown",
                            type = "soloRanked",
                            result = null,
                            duration = 100,
                            rank = 1,
                            trophyChange = 0,
                            starPlayer = null,
                            teams = listOf(listOf(player("A"), player("B"), player("C")), listOf(player("D"), player("E"), player("F"))),
                            players = null,
                        ),
                )
            val sut = BattleBuilder(brawlStarsTag = "A", battleResponse = response)

            // when
            val battle = sut.build()

            // then
            val expected =
                com.imstargg.infrastructure.db.core.statistics.battle.BattleKeyEmbedded
                    .forRank(battleTime, listOf("A", "B", "C"))
                    .value
            assertThat(battle.battleKey).isEqualTo(expected)
        }

        @Test
        fun `result 응답은 전체 플레이어 태그로 키를 생성한다`() {
            // given
            val response =
                BattleResponse(
                    battleTime = battleTime,
                    event = EventResponse(id = 1L, mode = "gemGrab", modeId = 1, map = "MAP"),
                    battle =
                        BattleResultResponse(
                            mode = "gemGrab",
                            type = "ranked",
                            result = "victory",
                            duration = 100,
                            rank = null,
                            trophyChange = 0,
                            starPlayer = null,
                            teams = listOf(listOf(player("A"), player("B"), player("C")), listOf(player("D"), player("E"), player("F"))),
                            players = null,
                        ),
                )
            val sut = BattleBuilder(brawlStarsTag = "A", battleResponse = response)

            // when
            val battle = sut.build()

            // then
            val expected =
                com.imstargg.infrastructure.db.core.statistics.battle.BattleKeyEmbedded
                    .forResult(battleTime, listOf("A", "B", "C", "D", "E", "F"))
                    .value
            assertThat(battle.battleKey).isEqualTo(expected)
        }

        @Test
        fun `rank 응답에서 수집 대상 플레이어가 팀에 없으면 예외`() {
            // given
            val response =
                BattleResponse(
                    battleTime = battleTime,
                    event = EventResponse(id = 1L, mode = "soloShowdown", modeId = 1, map = "MAP"),
                    battle =
                        BattleResultResponse(
                            mode = "soloShowdown",
                            type = "soloRanked",
                            result = null,
                            duration = 100,
                            rank = 3,
                            trophyChange = 0,
                            starPlayer = null,
                            teams = listOf(listOf(player("X")), listOf(player("Y"))),
                            players = null,
                        ),
                )
            val sut = BattleBuilder(brawlStarsTag = "Z", battleResponse = response)

            // expect
            org.assertj.core.api.Assertions
                .assertThatThrownBy { sut.build() }
                .isInstanceOf(IllegalArgumentException::class.java)
        }
    }
}
```

> `BattleResponse` / `BattleResultResponse` / `EventResponse` / `BattleResultPlayerResponse` / `BattleResultBrawlerResponse` 의 실제 생성자 시그니처가 위와 다르면 **기존 호출부(`BattleBuilder.getBattleTeamList` 근처 또는 기존 테스트)에 맞춰 필드를 조정**한다. 핵심 검증은 `battle.battleKey` 값 비교이므로 세부 필드는 도메인 기본값을 맞추기만 하면 된다.

### Step 2: 테스트 실행해 실패 확인

- [ ] Run: `./gradlew :core:core-worker:unitTest --tests "com.imstargg.core.worker.domain.BattleBuilderTest*"`
- [ ] Expected: `BattleKeyEmbedded` import 는 컴파일되지만 `battle.battleKey` 기대값과 실제값이 달라 rank 테스트는 FAIL (현재 BattleBuilder 가 `forResult` 와 동등한 전체 태그 기반으로 계산).

### Step 3: `BattleBuilder` 구현 변경

- [ ] `imstargg-backend/core/core-worker/src/main/kotlin/com/imstargg/core/worker/domain/BattleBuilder.kt` 를 다음과 같이 수정.

```kotlin
package com.imstargg.core.worker.domain

import com.imstargg.client.brawlstars.response.BattleResponse
import com.imstargg.client.brawlstars.response.BattleResultBrawlerResponse
import com.imstargg.client.brawlstars.response.BattleResultPlayerResponse
import com.imstargg.infrastructure.db.core.statistics.battle.BattleKeyEmbedded

class BattleBuilder(
    private val brawlStarsTag: String,
    private val battleResponse: BattleResponse,
) {
    fun build(): Battle {
        val teams = getBattleTeamList()
        val battleKey = createBattleKey(teams)
        return buildBattleResult(battleKey, teams)
    }

    private fun getBattleTeamList(): List<List<BattleResultPlayerResponse>> =
        battleResponse.battle.teams
            ?: battleResponse.battle.players?.map { listOf(it) }
            ?: throw IllegalArgumentException("플레이어 정보가 없습니다. battleResponse=$battleResponse")

    private fun createBattleKey(teams: List<List<BattleResultPlayerResponse>>): String {
        val isRank = battleResponse.battle.rank != null
        return if (isRank) {
            val playerTeam =
                teams.firstOrNull { team -> team.any { it.tag == brawlStarsTag } }
                    ?: throw IllegalArgumentException(
                        "수집 대상 플레이어가 팀에 포함되지 않았습니다. tag=$brawlStarsTag",
                    )
            BattleKeyEmbedded
                .forRank(
                    battleTime = battleResponse.battleTime,
                    playerTeamTags = playerTeam.map { it.tag },
                ).value
        } else {
            BattleKeyEmbedded
                .forResult(
                    battleTime = battleResponse.battleTime,
                    playerTags = teams.flatten().map { it.tag },
                ).value
        }
    }

    private fun buildBattleResult(
        battleKey: String,
        teams: List<List<BattleResultPlayerResponse>>,
    ): Battle {
        val battle = battleResponse.battle
        val event = battleResponse.event
        return Battle(
            battleKey = battleKey,
            battleAt = battleResponse.battleTime,
            event =
                BattleEvent(
                    brawlStarsId = event.id,
                    modeBrawlStarsName = event.mode,
                    modeBrawlStarsId = event.modeId,
                    map = event.map,
                ),
            mode = battle.mode,
            type = battle.type,
            result = battle.result,
            duration = battle.duration,
            starPlayerBrawlStarsTag = battle.starPlayer?.tag,
            playerBrawlStarsTag = brawlStarsTag,
            battleRank = battle.rank,
            trophyChange = battle.trophyChange,
            teams = buildTeams(teams),
        )
    }

    private fun buildTeams(teams: List<List<BattleResultPlayerResponse>>): List<List<BattleTeamPlayer>> =
        teams.map { team ->
            team.flatMap { player ->
                getPlayerBrawlers(player).map { brawler ->
                    BattleTeamPlayer(
                        brawlStarsTag = player.tag,
                        name = player.name,
                        brawler =
                            BattleTeamPlayerBrawler(
                                brawlStarsId = brawler.id,
                                name = brawler.name,
                                power = brawler.power,
                                trophies = brawler.trophies,
                                trophyChange = brawler.trophyChange,
                            ),
                    )
                }
            }
        }

    private fun getPlayerBrawlers(player: BattleResultPlayerResponse): List<BattleResultBrawlerResponse> =
        player.brawler?.let { listOf(it) }
            ?: player.brawlers
            ?: throw IllegalArgumentException("브롤러 정보가 없습니다. player=$player")
}
```

### Step 4: 테스트 통과 확인

- [ ] Run: `./gradlew :core:core-worker:unitTest --tests "com.imstargg.core.worker.domain.BattleBuilderTest*"`
- [ ] Expected: 새 rank/result/예외 테스트 3 개 모두 PASS.

### Step 5: 이전 `BattleKeyBuilder` 파일과 테스트 삭제

- [ ] Run:

```bash
git rm imstargg-backend/core/core-worker/src/main/kotlin/com/imstargg/core/worker/domain/BattleKeyBuilder.kt \
       imstargg-backend/core/core-worker/src/test/kotlin/com/imstargg/core/worker/domain/BattleKeyBuilderTest.kt
```

### Step 6: core-worker 전체 빌드로 사용처 누락 확인

- [ ] Run: `./gradlew :core:core-worker:compileKotlin :core:core-worker:compileTestKotlin`
- [ ] Expected: 성공. 만약 `BattleKeyBuilder` 를 참조하는 다른 파일이 있다면 import 를 `BattleKeyEmbedded` 로 수정.

### Step 7: 커밋

- [ ] Run:

```bash
git add imstargg-backend/core/core-worker/src/main/kotlin/com/imstargg/core/worker/domain/BattleBuilder.kt \
        imstargg-backend/core/core-worker/src/test/kotlin/com/imstargg/core/worker/domain/BattleBuilderTest.kt
git commit -m "refactor(core-worker): BattleBuilder 가 BattleKeyEmbedded 를 사용하도록 전환

rank 응답은 자기 팀 태그만으로, result 응답은 전체 태그로 battle_key 생성.
BattleKeyBuilder 및 그 테스트는 BattleKeyEmbedded / BattleKeyEmbeddedTest 로 이관 완료되어 제거."
```

---

## Task 3: `stats_rank_battle` unique index 축소 (스키마 + 엔티티 + 마이그레이션)

**Files:**
- Modify: `imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/battle/StatsRankBattleEntity.kt`
- Modify: `imstargg-backend/infrastructure/db-core/src/main/resources/schema.sql`
- Create: `imstargg-backend/infrastructure/db-core/src/main/resources/db/migration/V260423a__stats_rank_battle_unique_key.sql`

### Step 1: `StatsRankBattleEntity.kt` 의 `@Index` 정의 교체

- [ ] 기존:

```kotlin
Index(
    name = "uk_stats_rank_battle__battle_date_battle_key_player",
    columnList = "battle_date, battle_key, player_brawlstars_tag",
    unique = true,
),
```

를 아래로 교체:

```kotlin
Index(
    name = "uk_stats_rank_battle__battle_date_battle_key",
    columnList = "battle_date, battle_key",
    unique = true,
),
```

### Step 2: `schema.sql` 에서 `stats_rank_battle` unique 인덱스 정의 교체

- [ ] `imstargg-backend/infrastructure/db-core/src/main/resources/schema.sql` 를 열어 `uk_stats_rank_battle__battle_date_battle_key_player` 가 정의된 부분(대략 295 행 부근) 을 찾는다.

- [ ] 해당 라인을 다음과 같이 교체:

```sql
create unique index if not exists uk_stats_rank_battle__battle_date_battle_key
    on stats_rank_battle (battle_date, battle_key);
```

### Step 3: 마이그레이션 파일 작성

- [ ] 다음 내용으로 `V260423a__stats_rank_battle_unique_key.sql` 생성.

```sql
-- rank 전투 unique key 축소: (battle_date, battle_key, player_brawlstars_tag)
-- → (battle_date, battle_key)
-- 근거: battle_key 를 자기 팀 플레이어 태그 기반으로 생성하여 팀 단위 1 row 로 dedup.
-- 서비스 미운영 시기라 기존 데이터는 없거나 truncate 가능함을 전제.

drop index if exists uk_stats_rank_battle__battle_date_battle_key_player;

create unique index if not exists uk_stats_rank_battle__battle_date_battle_key
    on stats_rank_battle (battle_date, battle_key);
```

### Step 4: 로컬 DB 로 마이그레이션 적용 확인

- [ ] 로컬 인프라가 꺼져 있으면 `docker-compose up -d` 로 기동.
- [ ] 개발 DB 의 `stats_rank_battle` 데이터를 truncate (마이그레이션 전):

```bash
psql -h localhost -U postgres -d imstargg -c "truncate table stats_rank_battle;"
```

> 비밀번호 프롬프트에 `secret` 입력.

- [ ] core-batch(또는 db-core 의존하는 모듈) 를 기동해 Flyway 마이그레이션 자동 적용. 예시:

```bash
./gradlew :core:core-batch:bootRun --args='--job.name=healthCheckJob' 2>&1 | tee /tmp/migrate.log
```

- [ ] 종료 후 인덱스 확인:

```bash
psql -h localhost -U postgres -d imstargg -c "\\d stats_rank_battle"
```

- [ ] Expected: `uk_stats_rank_battle__battle_date_battle_key` UNIQUE btree (battle_date, battle_key) 존재, `..._player` 는 사라짐.

### Step 5: db-core 테스트 재실행해 회귀 없음 확인

- [ ] Run: `./gradlew :infrastructure:db-core:test`
- [ ] Expected: 전부 PASS.

### Step 6: 커밋

- [ ] Run:

```bash
git add imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/battle/StatsRankBattleEntity.kt \
        imstargg-backend/infrastructure/db-core/src/main/resources/schema.sql \
        imstargg-backend/infrastructure/db-core/src/main/resources/db/migration/V260423a__stats_rank_battle_unique_key.sql
git commit -m "refactor(db-core): stats_rank_battle unique 를 (battle_date, battle_key) 로 축소"
```

---

## Task 4: `StatsRankBattleJpaRepositoryCustom` 시그니처 변경 + `BattleKeyPlayerTagProjection` 제거

**Files:**
- Modify: `imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/battle/repository/StatsRankBattleJpaRepositoryCustom.kt`
- Modify: `imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/battle/repository/StatsRankBattleJpaRepositoryCustomImpl.kt`
- Delete: `imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/battle/repository/BattleKeyPlayerTagProjection.kt`

### Step 1: `StatsRankBattleJpaRepositoryCustom` 시그니처 교체

- [ ] 기존 파일 내용:

```kotlin
package com.imstargg.infrastructure.db.core.statistics.battle.repository

import com.imstargg.infrastructure.db.core.statistics.battle.StatsRankBattleEntity

interface StatsRankBattleJpaRepositoryCustom {
    fun findAllBattleKeyAndPlayerTagByStatsRankBattles(
        statsRankBattles: Collection<StatsRankBattleEntity>,
    ): Set<BattleKeyPlayerTagProjection>
}
```

를 아래로 교체:

```kotlin
package com.imstargg.infrastructure.db.core.statistics.battle.repository

import com.imstargg.infrastructure.db.core.statistics.battle.StatsRankBattleEntity

interface StatsRankBattleJpaRepositoryCustom {
    fun findAllBattleKeysByStatsRankBattles(statsRankBattles: Collection<StatsRankBattleEntity>): Set<String>
}
```

### Step 2: 구현 교체

- [ ] `StatsRankBattleJpaRepositoryCustomImpl.kt` 를 아래로 교체:

```kotlin
package com.imstargg.infrastructure.db.core.statistics.battle.repository

import com.imstargg.infrastructure.db.core.statistics.battle.QStatsRankBattleEntity.Companion.statsRankBattleEntity
import com.imstargg.infrastructure.db.core.statistics.battle.StatsRankBattleEntity
import com.imstargg.infrastructure.db.core.toKoreaLocalDate
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager

internal class StatsRankBattleJpaRepositoryCustomImpl(
    entityManager: EntityManager,
) : StatsRankBattleJpaRepositoryCustom {
    private val queryFactory = JPAQueryFactory(entityManager)

    override fun findAllBattleKeysByStatsRankBattles(statsRankBattles: Collection<StatsRankBattleEntity>): Set<String> {
        if (statsRankBattles.isEmpty()) return emptySet()

        val battleKeys = statsRankBattles.map { it.battleKey }
        val battleDates = statsRankBattles.map { it.battleAt.toKoreaLocalDate() }.distinct()

        return queryFactory
            .select(statsRankBattleEntity.battleKey)
            .from(statsRankBattleEntity)
            .where(
                statsRankBattleEntity.battleDate.`in`(battleDates),
                statsRankBattleEntity.battleKey.`in`(battleKeys),
            ).fetch()
            .toSet()
    }
}
```

### Step 3: `BattleKeyPlayerTagProjection` 삭제

- [ ] Run:

```bash
git rm imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/battle/repository/BattleKeyPlayerTagProjection.kt
```

### Step 4: db-core 컴파일 확인

- [ ] Run: `./gradlew :infrastructure:db-core:compileKotlin`
- [ ] Expected: 성공.

### Step 5: 커밋

- [ ] Run:

```bash
git add imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/battle/repository/StatsRankBattleJpaRepositoryCustom.kt \
        imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/battle/repository/StatsRankBattleJpaRepositoryCustomImpl.kt
git commit -m "refactor(db-core): rank dedup 시그니처를 findAllBattleKeys 로 통일

BattleKeyPlayerTagProjection 제거.
rank/result 모두 battle_key 단일로 dedup."
```

---

## Task 5: `StatsBattleAggregateApplier` dedup 키 통일 + same-apply distinctBy

**Files:**
- Modify: `imstargg-backend/core/core-worker/src/main/kotlin/com/imstargg/core/worker/application/StatsBattleAggregateApplier.kt`
- Test: `imstargg-backend/core/core-worker/src/test/kotlin/com/imstargg/core/worker/application/StatsBattleAggregateApplierTest.kt` (존재하면 확장, 없으면 신규)

### Step 1: 기존 테스트 존재 확인

- [ ] Run:

```bash
ls imstargg-backend/core/core-worker/src/test/kotlin/com/imstargg/core/worker/application/StatsBattleAggregateApplierTest.kt 2>/dev/null \
  || echo "not found"
```

### Step 2: "same-apply 같은 팀 중복 수집분은 1 row" 시나리오 작성

- [ ] 파일이 존재하면 기존 베이스 클래스(`CoreWorkerContextTest` 류) 와 픽스처를 그대로 쓰고, 아래 `@Nested class` 만 추가. 파일이 없으면 새로 생성하되 프로젝트의 `CoreWorkerContextTest` 또는 `@DataJpaTest` 기반 베이스를 확장한다.

- [ ] 다음 시나리오를 추가한다 (세부 픽스처는 프로젝트 `BattleFixture` 와 이미 존재하는 테스트를 참조해 맞춘다):

```kotlin
@Nested
inner class `apply 중복 수집` {
    @Test
    fun `rank 배틀을 같은 팀 A B C 가 동시에 수집해 넘겨도 1 row 만 저장된다`() {
        // given
        val battleTime = OffsetDateTime.parse("2026-04-23T10:00:00+09:00")
        val teams = listOf(listOf("A", "B", "C"), listOf("D", "E", "F"))
        val fromA = aRankBattle().withBattleTime(battleTime).withCollector("A").withTeams(teams).build()
        val fromB = aRankBattle().withBattleTime(battleTime).withCollector("B").withTeams(teams).build()
        val fromC = aRankBattle().withBattleTime(battleTime).withCollector("C").withTeams(teams).build()

        // when
        sut.apply(listOf(fromA, fromB, fromC))

        // then
        assertThat(statsRankBattleRepository.findAll()).hasSize(1)
    }

    @Test
    fun `result 배틀도 같은 battle_key 인 중복 수집은 1 row 만 저장된다`() {
        // given
        val battleTime = OffsetDateTime.parse("2026-04-23T10:00:00+09:00")
        val teams = listOf(listOf("A", "B", "C"), listOf("D", "E", "F"))
        val fromA = aResultBattle().withBattleTime(battleTime).withCollector("A").withTeams(teams).build()
        val fromB = aResultBattle().withBattleTime(battleTime).withCollector("B").withTeams(teams).build()

        // when
        sut.apply(listOf(fromA, fromB))

        // then
        assertThat(statsResultBattleRepository.findAll()).hasSize(1)
    }
}
```

> `aRankBattle()` / `aResultBattle()` / `.withCollector(...)` / `.withTeams(...)` 는 프로젝트의 기존 픽스처 명명 규칙을 따른다. 없으면 `BattleFixture` 를 스펙의 Battle 구조에 맞춰 신설(`withBattleTime`, `withCollector`(=brawlStarsTag), `withTeams`, `.build()` 로 `Battle` 반환).

### Step 3: 테스트 실행해 실패 확인

- [ ] Run: `./gradlew :core:core-worker:test --tests "com.imstargg.core.worker.application.StatsBattleAggregateApplierTest*"`
- [ ] Expected:
  - rank 시나리오: 현재는 `BattleKeyPlayerTagProjection` 이 지워져 컴파일도 안 되므로 먼저 Step 4 의 구현 수정이 필요. 하지만 TDD 원칙상 테스트를 먼저 깃발로 세우고, Step 4 구현 후에 재실행해 PASS 를 본다.

### Step 4: `StatsBattleAggregateApplier` 구현 교체

- [ ] `imstargg-backend/core/core-worker/src/main/kotlin/com/imstargg/core/worker/application/StatsBattleAggregateApplier.kt` 의 `apply(battles)` 메서드를 다음과 같이 수정.

```kotlin
@Transactional
fun apply(battles: List<Battle>) {
    val aggregatableBattles = battles.filter { canAggregate(it) }

    val statsRankBattles =
        aggregatableBattles
            .filter { canCreateRankBattle(it) }
            .map { toStatsRankBattle(it) }
            .distinctBy { it.battleKey }

    val statsResultBattles =
        aggregatableBattles
            .filter { canCreateResultBattle(it) }
            .map { toStatsResultBattle(it) }
            .distinctBy { it.battleKey }

    val existingResultBattleKeys =
        statsResultBattleRepository.findAllBattleKeysByStatsResultBattles(statsResultBattles)
    val existingRankBattleKeys =
        statsRankBattleRepository.findAllBattleKeysByStatsRankBattles(statsRankBattles)

    val newRankBattles = statsRankBattles.filter { it.battleKey !in existingRankBattleKeys }
    val newResultBattles = statsResultBattles.filter { it.battleKey !in existingResultBattleKeys }

    statsRankBattleRepository.saveAll(newRankBattles)
    statsResultBattleRepository.saveAll(newResultBattles)
}
```

- [ ] 파일 상단 import 에서 `BattleKeyPlayerTagProjection` 사용 줄을 제거.

### Step 5: 테스트 통과 확인

- [ ] Run: `./gradlew :core:core-worker:test --tests "com.imstargg.core.worker.application.StatsBattleAggregateApplierTest*"`
- [ ] Expected: 새 중복 수집 시나리오 2 건 포함 전부 PASS.

### Step 6: 커밋

- [ ] Run:

```bash
git add imstargg-backend/core/core-worker/src/main/kotlin/com/imstargg/core/worker/application/StatsBattleAggregateApplier.kt \
        imstargg-backend/core/core-worker/src/test/kotlin/com/imstargg/core/worker/application/StatsBattleAggregateApplierTest.kt
git rm -f imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/battle/repository/BattleKeyPlayerTagProjection.kt 2>/dev/null || true
git commit -m "refactor(core-worker): rank/result dedup 키 통일 + same-apply distinctBy

같은 apply 호출 내 동일 팀 중복 수집이 unique 충돌을 일으키지 않도록
distinctBy(battleKey) 를 DB 대조 전에 적용. rank 전용
BattleKeyPlayerTagProjection 경로 제거."
```

---

## Task 6: `DailyBattleStatsTasklet` 의 수동 트랜잭션 제거

**Files:**
- Modify: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/DailyBattleStatsTasklet.kt`
- Modify: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerBattleResultStatsJobConfig.kt`
- Modify: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerBattleRankStatsJobConfig.kt`

### Step 1: `DailyBattleStatsTasklet` 시그니처/본문 교체

- [ ] 전체 파일을 다음으로 교체:

```kotlin
package com.imstargg.core.batch.job.statistics

import com.imstargg.core.batch.domain.BattleStatsAggregator
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import org.slf4j.LoggerFactory
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.StepContribution
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.infrastructure.repeat.RepeatStatus
import java.time.LocalDate

class DailyBattleStatsTasklet<TBattle, TEntity : Any>(
    private val em: EntityManager,
    private val jobName: String,
    private val dateSupplier: () -> LocalDate,
    private val readPage: (JPAQueryFactory, LocalDate, Long) -> List<TBattle>,
    private val getId: (TBattle) -> Long,
    private val aggregatorFactory: () -> BattleStatsAggregator<TBattle, TEntity>,
    private val deleteExisting: (JPAQueryFactory, LocalDate) -> Unit,
) : Tasklet {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun execute(
        contribution: StepContribution,
        chunkContext: ChunkContext,
    ): RepeatStatus {
        val date = dateSupplier()
        val queryFactory = JPAQueryFactory(em)
        val aggregator = aggregatorFactory()

        var lastId = 0L
        var totalRead = 0L
        while (true) {
            val battles = readPage(queryFactory, date, lastId)
            if (battles.isEmpty()) break
            totalRead += battles.size
            battles.forEach { aggregator.aggregate(it) }
            lastId = getId(battles.last())
            em.clear()
            log.debug("[{}] 읽기 진행 - date: {}, 누적: {}건", jobName, date, totalRead)
        }

        deleteExisting(queryFactory, date)

        val entities = aggregator.toEntities()
        entities.chunked(BattleStatsConstants.BATCH_SIZE).forEach { batch ->
            batch.forEach { em.persist(it) }
            em.flush()
            em.clear()
        }

        log.info(
            "[{}] 완료 - date: {}, 전투: {}건, 집계: {}건",
            jobName,
            date,
            totalRead,
            entities.size,
        )
        return RepeatStatus.FINISHED
    }
}
```

### Step 2: `BrawlerBattleResultStatsJobConfig` 의 주입을 `EntityManager` 로 변경

- [ ] 생성자 선언 중 `private val entityManagerFactory: EntityManagerFactory,` 를 `private val em: EntityManager,` 로 교체.
- [ ] import 교체: `import jakarta.persistence.EntityManagerFactory` → `import jakarta.persistence.EntityManager`.
- [ ] tasklet 생성부의 `entityManagerFactory = entityManagerFactory,` 를 `em = em,` 로 교체.
- [ ] 최종 관련 블록 예:

```kotlin
@Configuration
class BrawlerBattleResultStatsJobConfig(
    private val jobRepository: JobRepository,
    private val txManager: PlatformTransactionManager,
    private val em: EntityManager,
) {
    // ...
    @Bean(STEP_NAME)
    @JobScope
    fun brawlerBattleResultStatsStep(): Step =
        StepBuilder(STEP_NAME, jobRepository)
            .tasklet(
                DailyBattleStatsTasklet(
                    em = em,
                    jobName = JOB_NAME,
                    dateSupplier = { dateJobParameter().date!! },
                    // ... 기존 함수형 인자 동일 ...
                ),
                txManager,
            ).build()
}
```

### Step 3: `BrawlerBattleRankStatsJobConfig` 에 동일 변경 적용

- [ ] Step 2 와 동일하게 `entityManagerFactory` → `em` 으로 주입 및 tasklet 생성 인자 변경.

### Step 4: 컴파일 확인

- [ ] Run: `./gradlew :core:core-batch:compileKotlin :core:core-batch:compileTestKotlin`
- [ ] Expected: 성공.

### Step 5: 기존 테스트 회귀 확인

- [ ] Run: `./gradlew :core:core-batch:test`
- [ ] Expected: 전부 PASS. 실패 시 `entityManagerFactory` 를 직접 참조하던 테스트가 있는지 확인 후 `@PersistenceContext`/주입 빈 교체.

### Step 6: 커밋

- [ ] Run:

```bash
git add imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/DailyBattleStatsTasklet.kt \
        imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerBattleResultStatsJobConfig.kt \
        imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerBattleRankStatsJobConfig.kt
git commit -m "refactor(core-batch): DailyBattleStatsTasklet 수동 트랜잭션 제거

EntityManagerFactory 로 application-managed EM 을 만들고 수동
em.transaction.begin/commit 하던 경로를 제거. Spring-managed EM 을
생성자로 주입받아 TaskletStep 이 이미 연 트랜잭션에 합류."
```

---

## Task 7: `PerEventDailyBattleStatsTasklet` 수동 트랜잭션 제거

**Files:**
- Modify: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/PerEventDailyBattleStatsTasklet.kt`
- Modify: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerEnemyBattleResultStatsJobConfig.kt`

### Step 1: `PerEventDailyBattleStatsTasklet` 교체

- [ ] 전체 파일을 다음으로 교체:

```kotlin
package com.imstargg.core.batch.job.statistics

import com.imstargg.core.batch.domain.BattleStatsAggregator
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import org.slf4j.LoggerFactory
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.StepContribution
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.infrastructure.repeat.RepeatStatus
import java.time.LocalDate

class PerEventDailyBattleStatsTasklet<TBattle, TEntity : Any>(
    private val em: EntityManager,
    private val jobName: String,
    private val dateSupplier: () -> LocalDate,
    private val readDistinctEventIds: (JPAQueryFactory, LocalDate) -> List<Long>,
    private val readPage: (JPAQueryFactory, LocalDate, Long, Long) -> List<TBattle>,
    private val getId: (TBattle) -> Long,
    private val aggregatorFactory: () -> BattleStatsAggregator<TBattle, TEntity>,
    private val deleteExisting: (JPAQueryFactory, LocalDate, Long) -> Unit,
    private val persistAdditional: (EntityManager, TEntity) -> Unit = { _, _ -> },
) : Tasklet {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun execute(
        contribution: StepContribution,
        chunkContext: ChunkContext,
    ): RepeatStatus {
        val date = dateSupplier()
        val queryFactory = JPAQueryFactory(em)
        val eventIds = readDistinctEventIds(queryFactory, date)
        log.info("[{}] 이벤트 {}개 처리 시작 - date: {}", jobName, eventIds.size, date)

        var totalRead = 0L
        var totalEntities = 0L

        for (eventId in eventIds) {
            val aggregator = aggregatorFactory()

            var lastId = 0L
            while (true) {
                val battles = readPage(queryFactory, date, lastId, eventId)
                if (battles.isEmpty()) break
                totalRead += battles.size
                battles.forEach { aggregator.aggregate(it) }
                lastId = getId(battles.last())
                em.clear()
            }

            deleteExisting(queryFactory, date, eventId)

            val entities = aggregator.toEntities()
            totalEntities += entities.size
            entities.chunked(BattleStatsConstants.BATCH_SIZE).forEach { batch ->
                batch.forEach {
                    em.persist(it)
                    persistAdditional(em, it)
                }
                em.flush()
                em.clear()
            }
        }

        log.info(
            "[{}] 완료 - date: {}, 전투: {}건, 집계: {}건",
            jobName,
            date,
            totalRead,
            totalEntities,
        )
        return RepeatStatus.FINISHED
    }
}
```

### Step 2: `BrawlerEnemyBattleResultStatsJobConfig` 주입 변경

- [ ] Task 6 Step 2 와 동일: `entityManagerFactory: EntityManagerFactory` → `em: EntityManager`, tasklet 생성 시 `em = em,` 로 교체.

### Step 3: 컴파일 + 테스트 확인

- [ ] Run: `./gradlew :core:core-batch:compileKotlin :core:core-batch:compileTestKotlin :core:core-batch:test`
- [ ] Expected: 성공.

### Step 4: 커밋

- [ ] Run:

```bash
git add imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/PerEventDailyBattleStatsTasklet.kt \
        imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerEnemyBattleResultStatsJobConfig.kt
git commit -m "refactor(core-batch): PerEventDailyBattleStatsTasklet 수동 트랜잭션 제거"
```

---

## Task 8: `PerEventWeeklyBattleStatsTasklet` 수동 트랜잭션 제거

**Files:**
- Modify: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/PerEventWeeklyBattleStatsTasklet.kt`
- Modify: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerTeamBattleResultStatsJobConfig.kt`
- Modify: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerTeamBattleRankStatsJobConfig.kt`

### Step 1: `PerEventWeeklyBattleStatsTasklet` 교체

- [ ] 전체 파일을 다음으로 교체:

```kotlin
package com.imstargg.core.batch.job.statistics

import com.imstargg.core.batch.domain.BattleStatsAggregator
import com.imstargg.core.enums.WeekRange
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import org.slf4j.LoggerFactory
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.StepContribution
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.infrastructure.repeat.RepeatStatus

class PerEventWeeklyBattleStatsTasklet<TBattle, TEntity : Any>(
    private val em: EntityManager,
    private val jobName: String,
    private val weekRangesSupplier: () -> List<WeekRange>,
    private val readDistinctEventIds: (JPAQueryFactory, WeekRange) -> List<Long>,
    private val readPage: (JPAQueryFactory, WeekRange, Long, Long) -> List<TBattle>,
    private val getId: (TBattle) -> Long,
    private val aggregatorFactory: (WeekRange) -> BattleStatsAggregator<TBattle, TEntity>,
    private val deleteExisting: (JPAQueryFactory, WeekRange, Long) -> Unit,
    private val persistAdditional: (EntityManager, TEntity) -> Unit = { _, _ -> },
) : Tasklet {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun execute(
        contribution: StepContribution,
        chunkContext: ChunkContext,
    ): RepeatStatus {
        val weekRanges = weekRangesSupplier()
        log.info("[{}] 주간 범위 {}개 처리 시작", jobName, weekRanges.size)
        val queryFactory = JPAQueryFactory(em)

        for (weekRange in weekRanges) {
            processWeekRange(queryFactory, weekRange)
        }
        return RepeatStatus.FINISHED
    }

    private fun processWeekRange(
        queryFactory: JPAQueryFactory,
        weekRange: WeekRange,
    ) {
        val eventIds = readDistinctEventIds(queryFactory, weekRange)
        log.info(
            "[{}] 이벤트 {}개 처리 시작 - weekRange: {} ~ {}",
            jobName,
            eventIds.size,
            weekRange.start,
            weekRange.end,
        )

        var totalRead = 0L
        var totalEntities = 0L

        for (eventId in eventIds) {
            val aggregator = aggregatorFactory(weekRange)

            var lastId = 0L
            while (true) {
                val battles = readPage(queryFactory, weekRange, lastId, eventId)
                if (battles.isEmpty()) break
                totalRead += battles.size
                battles.forEach { aggregator.aggregate(it) }
                lastId = getId(battles.last())
                em.clear()
            }

            deleteExisting(queryFactory, weekRange, eventId)

            val entities = aggregator.toEntities()
            totalEntities += entities.size
            entities.chunked(BattleStatsConstants.BATCH_SIZE).forEach { batch ->
                batch.forEach {
                    em.persist(it)
                    persistAdditional(em, it)
                }
                em.flush()
                em.clear()
            }
        }

        log.info(
            "[{}] 주간 범위 완료 - weekRange: {} ~ {}, 전투: {}건, 집계: {}건",
            jobName,
            weekRange.start,
            weekRange.end,
            totalRead,
            totalEntities,
        )
    }
}
```

### Step 2: `BrawlerTeamBattleResultStatsJobConfig` 주입 변경

- [ ] `entityManagerFactory` → `em: EntityManager` 로 교체 (Task 6 Step 2 와 동일 패턴).

### Step 3: `BrawlerTeamBattleRankStatsJobConfig` 주입 변경

- [ ] 동일하게 교체.

### Step 4: 컴파일 + 테스트

- [ ] Run: `./gradlew :core:core-batch:compileKotlin :core:core-batch:compileTestKotlin :core:core-batch:test`
- [ ] Expected: 성공.

### Step 5: 커밋

- [ ] Run:

```bash
git add imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/PerEventWeeklyBattleStatsTasklet.kt \
        imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerTeamBattleResultStatsJobConfig.kt \
        imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerTeamBattleRankStatsJobConfig.kt
git commit -m "refactor(core-batch): PerEventWeeklyBattleStatsTasklet 수동 트랜잭션 제거"
```

---

## Task 9: 집계 테이블 자연키 unique 인덱스 추가

**Files:**
- Modify: `imstargg-backend/infrastructure/db-core/src/main/resources/schema.sql`
- Create: `imstargg-backend/infrastructure/db-core/src/main/resources/db/migration/V260423b__stats_natural_key_unique.sql`

### Step 1: 마이그레이션 파일 생성

- [ ] 다음 내용으로 `V260423b__stats_natural_key_unique.sql` 작성:

```sql
-- 집계 테이블 자연키 unique 인덱스 추가 (멱등 재적재 대비 + 중복 방어선)

create unique index if not exists uk_brawler_battle_result_stats__1
    on brawler_battle_result_stats (event_brawlstars_id, tier_range, battle_date, brawler_brawlstars_id);

create unique index if not exists uk_brawler_battle_rank_stats__1
    on brawler_battle_rank_stats (event_brawlstars_id, tier_range, battle_date, brawler_brawlstars_id);

create unique index if not exists uk_brawler_team_battle_result_stats__1
    on brawler_team_battle_result_stats (event_brawlstars_id, tier_range, start_battle_date, end_battle_date, team_key);

create unique index if not exists uk_brawler_team_battle_rank_stats__1
    on brawler_team_battle_rank_stats (event_brawlstars_id, tier_range, start_battle_date, end_battle_date, team_key);

create unique index if not exists uk_brawler_enemy_battle_result_stats__1
    on brawler_enemy_battle_result_stats (
        event_brawlstars_id, tier_range, start_battle_date, end_battle_date,
        brawler_brawlstars_id, enemy_brawler_brawlstars_id
    );
```

### Step 2: `schema.sql` 에도 동일한 인덱스 선언 추가

- [ ] `schema.sql` 에서 각 테이블 정의 아래 (기존 `ix_...` 인덱스 선언 옆) 에 다음을 추가.

```sql
-- brawler_battle_result_stats 아래
create unique index if not exists uk_brawler_battle_result_stats__1
    on brawler_battle_result_stats (event_brawlstars_id, tier_range, battle_date, brawler_brawlstars_id);

-- brawler_battle_rank_stats 아래
create unique index if not exists uk_brawler_battle_rank_stats__1
    on brawler_battle_rank_stats (event_brawlstars_id, tier_range, battle_date, brawler_brawlstars_id);

-- brawler_team_battle_result_stats 아래
create unique index if not exists uk_brawler_team_battle_result_stats__1
    on brawler_team_battle_result_stats (event_brawlstars_id, tier_range, start_battle_date, end_battle_date, team_key);

-- brawler_team_battle_rank_stats 아래
create unique index if not exists uk_brawler_team_battle_rank_stats__1
    on brawler_team_battle_rank_stats (event_brawlstars_id, tier_range, start_battle_date, end_battle_date, team_key);

-- brawler_enemy_battle_result_stats 아래
create unique index if not exists uk_brawler_enemy_battle_result_stats__1
    on brawler_enemy_battle_result_stats (
        event_brawlstars_id, tier_range, start_battle_date, end_battle_date,
        brawler_brawlstars_id, enemy_brawler_brawlstars_id
    );
```

### Step 3: 로컬 DB 에 마이그레이션 적용

- [ ] 통계 테이블 truncate (이미 Task 3 에서 rank_battle 은 비움; 이번엔 통계 집계 테이블):

```bash
psql -h localhost -U postgres -d imstargg -c "
truncate table
    brawler_battle_result_stats,
    brawler_battle_rank_stats,
    brawler_team_battle_result_stats,
    brawler_team_battle_result_stats_brawler,
    brawler_team_battle_rank_stats,
    brawler_team_battle_rank_stats_brawler,
    brawler_enemy_battle_result_stats
;"
```

- [ ] core-batch 기동으로 Flyway 적용: `./gradlew :core:core-batch:bootRun --args='--job.name=healthCheckJob'` (자동 종료되지 않으면 `Ctrl+C`).
- [ ] 확인:

```bash
psql -h localhost -U postgres -d imstargg -c "
\\d brawler_team_battle_rank_stats
"
```

- [ ] Expected: `uk_brawler_team_battle_rank_stats__1` UNIQUE btree 존재.

### Step 4: 커밋

- [ ] Run:

```bash
git add imstargg-backend/infrastructure/db-core/src/main/resources/schema.sql \
        imstargg-backend/infrastructure/db-core/src/main/resources/db/migration/V260423b__stats_natural_key_unique.sql
git commit -m "feat(db-core): 집계 테이블 자연키 unique 인덱스 추가"
```

---

## Task 10: `stats-battle-data-strategy.md` 문서 확장 (BattleKey 정의 + 재실행 runbook)

**Files:**
- Modify: `imstargg-backend/docs/stats-battle-data-strategy.md`

### Step 1: BattleKey 섹션 + 재실행 runbook 섹션 추가

- [ ] 파일 끝(섹션 8 "관련 코드" 다음) 에 다음 내용을 추가한다.

````markdown
## 9. BattleKey 정의

### 생성 규칙
- `SHA-256(battleAt.toEpochSecond() || sorted(player tags).join(""))` 의 hex.
- **Result 전투**: 전체 플레이어 태그(모든 팀) 로 key 생성. 같은 배틀이면 어느 수집 주체가 들어와도 같은 key → DB unique `(battle_date, battle_key)` 로 1 row 로 수렴.
- **Rank 전투**: 수집 주체 플레이어의 **자기 팀 태그만** 으로 key 생성. 같은 팀의 여러 플레이어가 수집 대상이어도 모두 같은 key → 팀 단위로 1 row 로 수렴.

### 구현 위치
- `infrastructure/db-core` 의 `BattleKeyEmbedded`
  - `BattleKeyEmbedded.forResult(battleTime, playerTags)`
  - `BattleKeyEmbedded.forRank(battleTime, playerTeamTags)`
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
    timestamp=$(date +%s%3N)
```

- `timestamp` 키 이름은 운영 재량 (`manualRunAt`, `retry` 등 무엇이든).
- 반드시 **non-option argument** (`key=value`) 형식이어야 한다. `-Dtimestamp=...` 같은 JVM 시스템 프로퍼티는 Spring 환경에만 반영되고 `JobParameters` 에는 들어가지 않으므로 JobInstance 구분에 영향을 주지 못한다.
- `RunIdIncrementer` 는 도입하지 않았다. `JobLauncherApplicationRunner` 는 CLI 파라미터로 JobInstance 를 먼저 조회하고, 완료 상태면 incrementer 를 호출하지 않기 때문에 정기 스케줄 케이스에서 효과가 없다 (Spring Boot issue #22602).
````

### Step 2: 커밋

- [ ] Run:

```bash
git add imstargg-backend/docs/stats-battle-data-strategy.md
git commit -m "docs(backend): BattleKey 정의와 재실행 runbook 추가"
```

---

## Task 11: End-to-end 검증

**Files:** (검증 전용, 코드 변경 없음)

### Step 1: 전체 백엔드 빌드 + 린트

- [ ] Run: `./gradlew build`
- [ ] Expected: 성공. 실패하면 앞선 Task 에서 놓친 import/시그니처 문제를 수정.

### Step 2: 개발 DB 초기화 후 end-to-end 시나리오

- [ ] 모든 통계 테이블 + stats_rank_battle 를 다시 truncate:

```bash
psql -h localhost -U postgres -d imstargg -c "
truncate table
    stats_rank_battle,
    stats_result_battle,
    brawler_battle_result_stats,
    brawler_battle_rank_stats,
    brawler_team_battle_result_stats,
    brawler_team_battle_result_stats_brawler,
    brawler_team_battle_rank_stats,
    brawler_team_battle_rank_stats_brawler,
    brawler_enemy_battle_result_stats
;"
```

- [ ] 로컬 개발용 core-worker 기동으로 배틀 수집(테스트 데이터가 있다면):

```bash
./gradlew :core:core-worker:bootRun
```

> 실제 브롤스타즈 API 호출 없이 검증하려면 `developTest` 경로의 수집 시나리오 테스트를 사용한다. 로컬 API 키가 없으면 이 단계는 건너뛰고 Step 3 으로 진행.

- [ ] rank 배틀이 적재된 뒤 다음 쿼리로 "같은 battle_key 의 row 가 1 건" 인지 확인:

```bash
psql -h localhost -U postgres -d imstargg -c "
select battle_date, battle_key, count(*)
from stats_rank_battle
group by 1, 2
having count(*) > 1
limit 10;
"
```

- [ ] Expected: 0 행 (=중복 없음).

### Step 3: 배치 잡 재실행 시나리오 확인

- [ ] 임의 `date` 로 한 번 실행:

```bash
./gradlew :core:core-batch:bootRun --args='--job.name=brawlerBattleRankStatsJob date=2026-04-23'
```

- [ ] Expected: 성공 종료.

- [ ] 같은 파라미터로 다시 실행:

```bash
./gradlew :core:core-batch:bootRun --args='--job.name=brawlerBattleRankStatsJob date=2026-04-23'
```

- [ ] Expected: `JobInstanceAlreadyCompleteException` 관련 실패 메시지.

- [ ] `timestamp` 파라미터 추가해 재실행:

```bash
./gradlew :core:core-batch:bootRun --args="--job.name=brawlerBattleRankStatsJob date=2026-04-23 timestamp=$(date +%s%3N)"
```

- [ ] Expected: 성공 종료. runbook 동작 확인 완료.

### Step 4: 최종 전체 테스트

- [ ] Run: `./gradlew test`
- [ ] Expected: 전부 PASS.

### Step 5: 정리 커밋 (필요 시)

- [ ] 이 Task 는 코드 변경이 없어 커밋이 없을 수 있다. 만약 Step 1-4 진행 중 수정 사항이 생기면 개별 커밋으로 분리.

---

## 실행 중 예상 이슈 체크리스트

- Querydsl KSP 빌드: 엔티티 인덱스 속성 변경이 Q-Class 에 영향을 주지 않으므로 재생성 문제 없음. 빌드 실패 시 `./gradlew clean build`.
- `DefaultJobParametersValidator` 가 `timestamp` 같은 비지정 파라미터를 거부하면, 해당 잡 config 의 validator 를 다음 중 하나로 완화:
  - `DefaultJobParametersValidator(arrayOf(DateJobParameter.KEY), arrayOf("timestamp", "manualRunAt", "retry"))` 로 optional 에 대표 키 열거.
  - 또는 `JobParametersValidator` 커스텀 구현으로 "`date` 만 required, 나머지는 무시" 정책으로 교체.
  - 실제 필요 여부는 Task 11 Step 3 의 재실행 시나리오에서 판가름.
- Flyway `V260423a` 적용 시 파티셔닝된 `stats_rank_battle` 에서 `drop index` 가 파티션 레벨 인덱스까지 자동 제거하는지 PostgreSQL 버전별 확인. 실패 시 마이그레이션을 각 파티션 `drop index if exists ... on <partition>` 로 분해.
