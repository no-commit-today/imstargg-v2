package com.imstargg.core.batch.job.statistics.pairbattleresult

import com.imstargg.core.batch.domain.statistics.BrawlerPairBattleResultStatsAggregator
import com.imstargg.core.enums.BattleResult
import com.imstargg.core.enums.BattleType
import com.imstargg.core.enums.WeekRange
import com.imstargg.infrastructure.db.core.statistics.battle.StatsBattleEntityTeamPlayer
import com.imstargg.infrastructure.db.core.statistics.battle.StatsBattleEntityTeamPlayerBrawler
import com.imstargg.infrastructure.db.core.statistics.battle.StatsResultBattleEntity
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.OffsetDateTime

class BrawlerPairBattleResultStatsTaskTest {
    private val reader = mockk<BrawlerPairBattleResultStatsReader>()
    private val writer = mockk<BrawlerPairBattleResultStatsWriter>()
    private val sut = BrawlerPairBattleResultStatsTask(reader, writer)

    @Test
    fun `주간 범위 하나를 집계기 하나로 읽고 한 번에 저장한다`() {
        // given
        val weekRange = WeekRange(start = LocalDate.of(2026, 4, 28), end = LocalDate.of(2026, 5, 4))
        val battles =
            listOf(
                pairBattle(eventBrawlStarsId = 10L, battleDate = weekRange.start, battleKey = "battle-key-1"),
                pairBattle(eventBrawlStarsId = 20L, battleDate = weekRange.start, battleKey = "battle-key-2"),
            )
        val aggregatorSlot = slot<BrawlerPairBattleResultStatsAggregator>()
        val consumeSlot = slot<(List<StatsResultBattleEntity>) -> Unit>()

        every { reader.read(eq(weekRange), capture(consumeSlot)) } answers {
            consumeSlot.captured.invoke(battles)
            battles.size.toLong()
        }
        every { writer.write(eq(weekRange), capture(aggregatorSlot)) } returns 1L

        // when
        sut.aggregate(listOf(weekRange))

        // then
        val entities = aggregatorSlot.captured.toEntities()
        assertThat(entities).hasSize(2)
        assertThat(entities.map { it.resultCount.totalCount }).containsOnly(2L)
        verify(exactly = 1) { reader.read(weekRange, any()) }
        verify(exactly = 1) { writer.write(weekRange, any()) }
    }

    @Test
    fun `여러 주간 범위가 주어지면 범위마다 독립적으로 집계한다`() {
        // given
        val weekRange1 = WeekRange(start = LocalDate.of(2026, 4, 28), end = LocalDate.of(2026, 5, 4))
        val weekRange2 = WeekRange(start = LocalDate.of(2026, 5, 5), end = LocalDate.of(2026, 5, 11))
        val battle1 = pairBattle(eventBrawlStarsId = 10L, battleDate = weekRange1.start)
        val battle2 = pairBattle(eventBrawlStarsId = 20L, battleDate = weekRange2.start)
        val consumeSlot1 = slot<(List<StatsResultBattleEntity>) -> Unit>()
        val consumeSlot2 = slot<(List<StatsResultBattleEntity>) -> Unit>()
        val aggregators = mutableListOf<BrawlerPairBattleResultStatsAggregator>()

        every { reader.read(eq(weekRange1), capture(consumeSlot1)) } answers {
            consumeSlot1.captured.invoke(listOf(battle1))
            1L
        }
        every { reader.read(eq(weekRange2), capture(consumeSlot2)) } answers {
            consumeSlot2.captured.invoke(listOf(battle2))
            1L
        }
        every { writer.write(any(), capture(aggregators)) } returns 1L

        // when
        sut.aggregate(listOf(weekRange1, weekRange2))

        // then
        verify(exactly = 1) { reader.read(weekRange1, any()) }
        verify(exactly = 1) { reader.read(weekRange2, any()) }
        verify(exactly = 1) { writer.write(weekRange1, any()) }
        verify(exactly = 1) { writer.write(weekRange2, any()) }
        assertThat(aggregators).hasSize(2)
        assertThat(aggregators[0]).isNotSameAs(aggregators[1])
        assertThat(aggregators[0].toEntities().map { it.startBattleDate to it.endBattleDate }.distinct())
            .containsExactly(weekRange1.start to weekRange1.end)
        assertThat(aggregators[1].toEntities().map { it.startBattleDate to it.endBattleDate }.distinct())
            .containsExactly(weekRange2.start to weekRange2.end)
    }

    private fun pairBattle(
        eventBrawlStarsId: Long,
        battleDate: LocalDate,
        battleKey: String = "battle-key",
    ): StatsResultBattleEntity {
        val teams =
            listOf(
                listOf(player(index = 1), player(index = 2)),
                listOf(player(index = 3), player(index = 4)),
            )
        return StatsResultBattleEntity(
            battleKey = battleKey,
            battleAt = OffsetDateTime.of(battleDate.atStartOfDay(), java.time.ZoneOffset.ofHours(9)).toInstant(),
            eventBrawlStarsId = eventBrawlStarsId,
            eventModeBrawlStarsId = 2,
            eventMap = "test-map",
            mode = "gemGrab",
            type = BattleType.RANKED.code,
            starPlayerBrawlStarsTag = null,
            playerBrawlStarsTag = teams.first().first().brawlStarsTag,
            teams = teams,
            result = BattleResult.VICTORY.code,
        )
    }

    private fun player(index: Int): StatsBattleEntityTeamPlayer =
        StatsBattleEntityTeamPlayer(
            brawlStarsTag = "#TAG$index",
            name = "Player$index",
            brawler =
                StatsBattleEntityTeamPlayerBrawler(
                    brawlStarsId = index.toLong(),
                    name = "Brawler$index",
                    power = 11,
                    trophies = 1000,
                ),
        )
}
