package com.imstargg.core.batch.domain.statistics

import com.imstargg.core.enums.BattleType
import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.BrawlerPairBattleRankStatsEntity
import com.imstargg.infrastructure.db.core.statistics.battle.StatsBattleEntityTeamPlayer
import com.imstargg.infrastructure.db.core.statistics.battle.StatsBattleEntityTeamPlayerBrawler
import com.imstargg.infrastructure.db.core.statistics.battle.StatsRankBattleEntity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.OffsetDateTime

class BrawlerPairBattleRankStatsAggregatorTest {
    @Nested
    inner class Aggregate {
        @Test
        fun `3명 팀에서 player team 기준 ordered pair 6개를 생성한다`() {
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
        fun `weighted rank sum 과 total count 를 계산한다`() {
            val entities = aggregate(listOf(players(1), players(2)), battleRank = 3)

            assertThat(entities).hasSize(2)
            assertThat(entities).allSatisfy { entity ->
                assertThat(entity.rankCounts.totalCount).isEqualTo(1)
                assertThat(entity.rankCounts.countOf(3)).isEqualTo(1)
                assertThat(entity.weightedRankSum).isEqualTo(3)
            }
        }
    }

    private fun aggregate(
        players: List<StatsBattleEntityTeamPlayer>,
        battleRank: Int = 1,
    ): List<BrawlerPairBattleRankStatsEntity> {
        val aggregator = BrawlerPairBattleRankStatsAggregator(START_DATE, END_DATE)
        aggregator.aggregate(rankBattle(players = players, battleRank = battleRank))
        return aggregator.toEntities()
    }

    private fun rankBattle(
        players: List<StatsBattleEntityTeamPlayer>,
        battleRank: Int,
    ): StatsRankBattleEntity =
        StatsRankBattleEntity(
            battleKey = "rank-battle-${players.joinToString("-") { it.brawlStarsTag }}",
            battleAt = OffsetDateTime.parse("2026-04-26T00:00:00+09:00").toInstant(),
            eventBrawlStarsId = 1,
            eventModeBrawlStarsId = 2,
            eventMap = "test-map",
            mode = "soloShowdown",
            type = BattleType.RANKED.code,
            playerBrawlStarsTag = players.first().brawlStarsTag,
            teams = listOf(players),
            battleRank = battleRank,
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

    private fun List<BrawlerPairBattleRankStatsEntity>.tierRangesOf(
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
