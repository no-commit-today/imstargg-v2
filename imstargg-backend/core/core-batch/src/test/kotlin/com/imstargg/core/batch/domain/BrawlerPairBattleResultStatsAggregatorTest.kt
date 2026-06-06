package com.imstargg.core.batch.domain

import com.imstargg.core.enums.BattleResult
import com.imstargg.core.enums.BattleType
import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.BrawlerPairBattleResultStatsEntity
import com.imstargg.infrastructure.db.core.statistics.battle.StatsBattleEntityTeamPlayer
import com.imstargg.infrastructure.db.core.statistics.battle.StatsBattleEntityTeamPlayerBrawler
import com.imstargg.infrastructure.db.core.statistics.battle.StatsResultBattleEntity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.OffsetDateTime

class BrawlerPairBattleResultStatsAggregatorTest {
    @Nested
    inner class Aggregate {
        @Test
        fun `2명 팀에서 ordered pair 2개를 생성한다`() {
            val entities = aggregate(listOf(players(1), players(2)))

            assertThat(entities).hasSize(2)
            assertThat(entities.map { it.brawlerBrawlStarsId to it.pairBrawlerBrawlStarsId })
                .containsExactlyInAnyOrder(1L to 2L, 2L to 1L)
        }

        @Test
        fun `3명 팀에서 ordered pair 6개를 생성한다`() {
            val entities = aggregate(listOf(players(1), players(2), players(3)))

            assertThat(entities).hasSize(6)
            assertThat(entities.map { it.brawlerBrawlStarsId to it.pairBrawlerBrawlStarsId })
                .containsExactlyInAnyOrder(
                    1L to 2L,
                    1L to 3L,
                    2L to 1L,
                    2L to 3L,
                    3L to 1L,
                    3L to 2L,
                )
        }

        @Test
        fun `5명 팀에서 ordered pair 20개를 생성한다`() {
            val entities = aggregate((1..5).map { players(it) })

            assertThat(entities).hasSize(20)
            assertThat(entities.map { it.brawlerBrawlStarsId to it.pairBrawlerBrawlStarsId }).doesNotHaveDuplicates()
        }

        @Test
        fun `티어 구간은 기준 브롤러 trophy 기준으로 계산한다`() {
            val entities =
                aggregate(
                    listOf(
                        players(1, trophies = 3000),
                        players(2, trophies = 1000),
                    ),
                )

            assertThat(entities).hasSize(4)
            assertThat(entities.tierRangesOf(1, 2))
                .containsExactlyInAnyOrder(
                    StatsTierRange.TROPHY_1000_PLUS,
                    StatsTierRange.TROPHY_2000_PLUS,
                    StatsTierRange.TROPHY_3000_PLUS,
                )
            assertThat(entities.tierRangesOf(2, 1)).containsExactly(StatsTierRange.TROPHY_1000_PLUS)
        }

        @Test
        fun `적군 팀은 반대 결과로 집계한다`() {
            val playerTeam = listOf(players(1))
            val enemyTeam = listOf(players(11), players(12))
            val entities = aggregate(teams = listOf(playerTeam, enemyTeam), result = BattleResult.VICTORY)

            assertThat(entities).hasSize(2)
            assertThat(entities).allSatisfy { entity ->
                assertThat(entity.brawlerBrawlStarsId).isIn(11L, 12L)
                assertThat(entity.resultCount.victoryCount).isZero()
                assertThat(entity.resultCount.defeatCount).isEqualTo(1)
                assertThat(entity.resultCount.drawCount).isZero()
            }
        }
    }

    private fun aggregate(players: List<StatsBattleEntityTeamPlayer>): List<BrawlerPairBattleResultStatsEntity> =
        aggregate(teams = listOf(players), result = BattleResult.VICTORY)

    private fun aggregate(
        teams: List<List<StatsBattleEntityTeamPlayer>>,
        result: BattleResult,
    ): List<BrawlerPairBattleResultStatsEntity> {
        val aggregator = BrawlerPairBattleResultStatsAggregator(START_DATE, END_DATE)
        aggregator.aggregate(resultBattle(teams = teams, result = result))
        return aggregator.toEntities()
    }

    private fun resultBattle(
        teams: List<List<StatsBattleEntityTeamPlayer>>,
        result: BattleResult,
    ): StatsResultBattleEntity =
        StatsResultBattleEntity(
            battleKey = "battle-${teams.flatten().joinToString("-") { it.brawlStarsTag }}",
            battleAt = OffsetDateTime.parse("2026-04-26T00:00:00+09:00").toInstant(),
            eventBrawlStarsId = 1,
            eventModeBrawlStarsId = 2,
            eventMap = "test-map",
            mode = "gemGrab",
            type = BattleType.RANKED.code,
            starPlayerBrawlStarsTag = null,
            playerBrawlStarsTag = teams.first().first().brawlStarsTag,
            teams = teams,
            result = result.code,
        )

    private fun players(
        index: Int,
        trophies: Int = 1000,
    ): StatsBattleEntityTeamPlayer =
        StatsBattleEntityTeamPlayer(
            brawlStarsTag = "#TAG$index",
            name = "Player$index",
            brawler =
                StatsBattleEntityTeamPlayerBrawler(
                    brawlStarsId = index.toLong(),
                    name = "Brawler$index",
                    power = 11,
                    trophies = trophies,
                ),
        )

    private fun List<BrawlerPairBattleResultStatsEntity>.tierRangesOf(
        brawlerBrawlStarsId: Long,
        pairBrawlerBrawlStarsId: Long,
    ): List<StatsTierRange> =
        filter {
            it.brawlerBrawlStarsId == brawlerBrawlStarsId &&
                it.pairBrawlerBrawlStarsId == pairBrawlerBrawlStarsId
        }.map { it.tierRange }

    companion object {
        private val START_DATE: LocalDate = LocalDate.parse("2026-04-20")
        private val END_DATE: LocalDate = LocalDate.parse("2026-04-26")
    }
}
