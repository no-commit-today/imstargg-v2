package com.imstargg.core.batch.domain.statistics

import com.imstargg.core.enums.BattleResult
import com.imstargg.core.enums.BattleType
import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.battle.StatsBattleEntityTeamPlayer
import com.imstargg.infrastructure.db.core.statistics.battle.StatsBattleEntityTeamPlayerBrawler
import com.imstargg.infrastructure.db.core.statistics.battle.StatsResultBattleEntity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.OffsetDateTime

class BrawlerEnemyBattleResultStatsAggregatorTest {
    @Nested
    inner class Aggregate {
        @Test
        fun `내 팀 (1) vs 적 (2) 승리면 (1,2) 승1, (2,1) 패1 을 만든다`() {
            val aggregator = BrawlerEnemyBattleResultStatsAggregator(START_DATE, END_DATE)
            aggregator.aggregate(
                resultBattle(
                    teams = listOf(listOf(player(1)), listOf(player(2))),
                    result = BattleResult.VICTORY,
                ),
            )

            val entities = aggregator.toEntities()

            assertThat(entities).hasSize(2)
            val forward = entities.single { it.brawlerBrawlStarsId == 1L && it.enemyBrawlerBrawlStarsId == 2L }
            assertThat(forward.resultCount.victoryCount).isEqualTo(1)
            assertThat(forward.resultCount.defeatCount).isZero()
            assertThat(forward.resultCount.drawCount).isZero()
            val backward = entities.single { it.brawlerBrawlStarsId == 2L && it.enemyBrawlerBrawlStarsId == 1L }
            assertThat(backward.resultCount.victoryCount).isZero()
            assertThat(backward.resultCount.defeatCount).isEqualTo(1)
            assertThat(backward.resultCount.drawCount).isZero()
        }

        @Test
        fun `키에 이벤트가 없어 다른 이벤트 전투가 같은 행에 합산된다`() {
            val aggregator = BrawlerEnemyBattleResultStatsAggregator(START_DATE, END_DATE)
            aggregator.aggregate(
                resultBattle(
                    teams = listOf(listOf(player(1)), listOf(player(2))),
                    result = BattleResult.VICTORY,
                    eventBrawlStarsId = 1,
                ),
            )
            aggregator.aggregate(
                resultBattle(
                    teams = listOf(listOf(player(1)), listOf(player(2))),
                    result = BattleResult.VICTORY,
                    eventBrawlStarsId = 2,
                ),
            )

            val entities = aggregator.toEntities()

            assertThat(entities).hasSize(2)
            val forward = entities.single { it.brawlerBrawlStarsId == 1L && it.enemyBrawlerBrawlStarsId == 2L }
            assertThat(forward.resultCount.victoryCount).isEqualTo(2)
        }

        @Test
        fun `티어는 기준 플레이어의 trophies 로 정한다`() {
            val aggregator = BrawlerEnemyBattleResultStatsAggregator(START_DATE, END_DATE)
            aggregator.aggregate(
                resultBattle(
                    teams = listOf(listOf(player(1, trophies = 2500)), listOf(player(2, trophies = 1000))),
                    result = BattleResult.VICTORY,
                ),
            )

            val entities = aggregator.toEntities()

            assertThat(entities.filter { it.brawlerBrawlStarsId == 1L }.map { it.tierRange })
                .containsExactlyInAnyOrder(StatsTierRange.TROPHY_1000_PLUS, StatsTierRange.TROPHY_2000_PLUS)
            assertThat(entities.filter { it.brawlerBrawlStarsId == 2L }.map { it.tierRange })
                .containsExactly(StatsTierRange.TROPHY_1000_PLUS)
        }

        @Test
        fun `엔티티에 주 버킷 날짜를 담는다`() {
            val aggregator = BrawlerEnemyBattleResultStatsAggregator(START_DATE, END_DATE)
            aggregator.aggregate(
                resultBattle(
                    teams = listOf(listOf(player(1)), listOf(player(2))),
                    result = BattleResult.VICTORY,
                ),
            )

            assertThat(aggregator.toEntities()).allSatisfy {
                assertThat(it.startBattleDate).isEqualTo(START_DATE)
                assertThat(it.endBattleDate).isEqualTo(END_DATE)
            }
        }
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

    private fun player(
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
