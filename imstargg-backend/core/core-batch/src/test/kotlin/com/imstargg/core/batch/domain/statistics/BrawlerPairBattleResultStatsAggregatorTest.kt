package com.imstargg.core.batch.domain.statistics

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
        fun `2명 팀에서 정렬 쌍 1개를 생성한다`() {
            val entities = aggregate(listOf(players(2), players(1)))

            assertThat(entities).hasSize(1)
            assertThat(entities.single().brawler1BrawlStarsId).isEqualTo(1L)
            assertThat(entities.single().brawler2BrawlStarsId).isEqualTo(2L)
        }

        @Test
        fun `3명 팀은 정렬 쌍 3개를 만든다`() {
            val entities = aggregate(listOf(players(3), players(1), players(2)))

            assertThat(entities).hasSize(3)
            assertThat(entities.map { it.brawler1BrawlStarsId to it.brawler2BrawlStarsId })
                .containsExactlyInAnyOrder(1L to 2L, 1L to 3L, 2L to 3L)
        }

        @Test
        fun `5명 팀에서 정렬 쌍 10개를 생성한다`() {
            val entities = aggregate((1..5).map { players(it) })

            assertThat(entities).hasSize(10)
            assertThat(entities.map { it.brawler1BrawlStarsId to it.brawler2BrawlStarsId }).doesNotHaveDuplicates()
        }

        @Test
        fun `팀원이 1명이면 쌍을 만들지 않는다`() {
            val entities = aggregate(listOf(players(1)))

            assertThat(entities).isEmpty()
        }

        @Test
        fun `티어는 두 멤버 trophies 의 최솟값으로 정한다`() {
            val entities =
                aggregate(
                    listOf(
                        players(1, trophies = 3000),
                        players(2, trophies = 1000),
                        players(3, trophies = 2500),
                    ),
                )

            assertThat(entities).hasSize(4)
            assertThat(entities.tierRangesOf(1, 2)).containsExactly(StatsTierRange.TROPHY_1000_PLUS)
            assertThat(entities.tierRangesOf(2, 3)).containsExactly(StatsTierRange.TROPHY_1000_PLUS)
            assertThat(entities.tierRangesOf(1, 3))
                .containsExactlyInAnyOrder(StatsTierRange.TROPHY_1000_PLUS, StatsTierRange.TROPHY_2000_PLUS)
        }

        @Test
        fun `적군 팀은 반대 결과로 집계한다`() {
            val playerTeam = listOf(players(1))
            val enemyTeam = listOf(players(11), players(12))
            val entities = aggregate(teams = listOf(playerTeam, enemyTeam), result = BattleResult.VICTORY)

            assertThat(entities).hasSize(1)
            val entity = entities.single()
            assertThat(entity.brawler1BrawlStarsId).isEqualTo(11L)
            assertThat(entity.brawler2BrawlStarsId).isEqualTo(12L)
            assertThat(entity.resultCount.victoryCount).isZero()
            assertThat(entity.resultCount.defeatCount).isEqualTo(1)
            assertThat(entity.resultCount.drawCount).isZero()
        }

        @Test
        fun `아군 팀은 전투 결과 그대로 집계한다`() {
            val entities = aggregate(listOf(players(1), players(2)))

            val entity = entities.single()
            assertThat(entity.resultCount.victoryCount).isEqualTo(1)
            assertThat(entity.resultCount.defeatCount).isZero()
            assertThat(entity.resultCount.drawCount).isZero()
        }

        @Test
        fun `키에 이벤트가 없어 다른 이벤트 전투가 같은 행에 합산된다`() {
            val aggregator = BrawlerPairBattleResultStatsAggregator(START_DATE, END_DATE)
            aggregator.aggregate(
                resultBattle(
                    teams = listOf(listOf(players(1), players(2))),
                    result = BattleResult.VICTORY,
                    eventBrawlStarsId = 1,
                ),
            )
            aggregator.aggregate(
                resultBattle(
                    teams = listOf(listOf(players(1), players(2))),
                    result = BattleResult.DEFEAT,
                    eventBrawlStarsId = 2,
                ),
            )

            val entity = aggregator.toEntities().single()
            assertThat(entity.resultCount.victoryCount).isEqualTo(1)
            assertThat(entity.resultCount.defeatCount).isEqualTo(1)
        }

        @Test
        fun `엔티티에 주 버킷 날짜를 담는다`() {
            val entity = aggregate(listOf(players(1), players(2))).single()

            assertThat(entity.startBattleDate).isEqualTo(START_DATE)
            assertThat(entity.endBattleDate).isEqualTo(END_DATE)
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
        eventBrawlStarsId: Long = 1,
    ): StatsResultBattleEntity =
        StatsResultBattleEntity(
            battleKey = "battle-${teams.flatten().joinToString("-") { it.brawlStarsTag }}-$eventBrawlStarsId",
            battleAt = OffsetDateTime.parse("2026-04-26T00:00:00+09:00").toInstant(),
            eventBrawlStarsId = eventBrawlStarsId,
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
        brawler1BrawlStarsId: Long,
        brawler2BrawlStarsId: Long,
    ): List<StatsTierRange> =
        filter {
            it.brawler1BrawlStarsId == brawler1BrawlStarsId &&
                it.brawler2BrawlStarsId == brawler2BrawlStarsId
        }.map { it.tierRange }

    companion object {
        private val START_DATE: LocalDate = LocalDate.parse("2026-04-20")
        private val END_DATE: LocalDate = LocalDate.parse("2026-04-26")
    }
}
