package com.imstargg.core.batch.domain.statistics

import com.imstargg.core.enums.BattleResult
import com.imstargg.core.enums.BattleType
import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.BrawlerTrioBattleResultStatsEntity
import com.imstargg.infrastructure.db.core.statistics.battle.StatsBattleEntityTeamPlayer
import com.imstargg.infrastructure.db.core.statistics.battle.StatsBattleEntityTeamPlayerBrawler
import com.imstargg.infrastructure.db.core.statistics.battle.StatsResultBattleEntity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.OffsetDateTime

class BrawlerTrioBattleResultStatsAggregatorTest {
    @Nested
    inner class Aggregate {
        @Test
        fun `3명 팀은 정렬 키 1행을 만든다`() {
            val entities = aggregate(listOf(players(3), players(1), players(2)))

            assertThat(entities).hasSize(1)
            val entity = entities.single()
            assertThat(entity.brawler1BrawlStarsId).isEqualTo(1L)
            assertThat(entity.brawler2BrawlStarsId).isEqualTo(2L)
            assertThat(entity.brawler3BrawlStarsId).isEqualTo(3L)
        }

        @Test
        fun `티어는 세 멤버 trophies 최솟값으로 정한다`() {
            val entities =
                aggregate(
                    listOf(
                        players(1, trophies = 3000),
                        players(2, trophies = 1000),
                        players(3, trophies = 2500),
                    ),
                )

            assertThat(entities.map { it.tierRange }).containsExactly(StatsTierRange.TROPHY_1000_PLUS)
        }

        @Test
        fun `적군 팀은 반대 결과로 집계한다`() {
            val playerTeam = listOf(players(1))
            val enemyTeam = listOf(players(11), players(12), players(13))
            val entities = aggregate(teams = listOf(playerTeam, enemyTeam), result = BattleResult.VICTORY)

            assertThat(entities).hasSize(1)
            val entity = entities.single()
            assertThat(entity.brawler1BrawlStarsId).isEqualTo(11L)
            assertThat(entity.brawler2BrawlStarsId).isEqualTo(12L)
            assertThat(entity.brawler3BrawlStarsId).isEqualTo(13L)
            assertThat(entity.resultCount.victoryCount).isZero()
            assertThat(entity.resultCount.defeatCount).isEqualTo(1)
            assertThat(entity.resultCount.drawCount).isZero()
        }

        @Test
        fun `아군 팀은 전투 결과 그대로 집계한다`() {
            val entities = aggregate(listOf(players(1), players(2), players(3)))

            val entity = entities.single()
            assertThat(entity.resultCount.victoryCount).isEqualTo(1)
            assertThat(entity.resultCount.defeatCount).isZero()
            assertThat(entity.resultCount.drawCount).isZero()
        }

        @Test
        fun `인원이 3이 아닌 팀은 건너뛴다`() {
            assertThat(aggregate(listOf(players(1), players(2)))).isEmpty()
            assertThat(aggregate((1..4).map { players(it) })).isEmpty()
        }

        @Test
        fun `엔티티에 주 버킷 날짜를 담는다`() {
            val entity = aggregate(listOf(players(1), players(2), players(3))).single()

            assertThat(entity.startBattleDate).isEqualTo(START_DATE)
            assertThat(entity.endBattleDate).isEqualTo(END_DATE)
        }
    }

    private fun aggregate(players: List<StatsBattleEntityTeamPlayer>): List<BrawlerTrioBattleResultStatsEntity> =
        aggregate(teams = listOf(players), result = BattleResult.VICTORY)

    private fun aggregate(
        teams: List<List<StatsBattleEntityTeamPlayer>>,
        result: BattleResult,
    ): List<BrawlerTrioBattleResultStatsEntity> {
        val aggregator = BrawlerTrioBattleResultStatsAggregator(START_DATE, END_DATE)
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

    companion object {
        private val START_DATE: LocalDate = LocalDate.parse("2026-04-20")
        private val END_DATE: LocalDate = LocalDate.parse("2026-04-26")
    }
}
