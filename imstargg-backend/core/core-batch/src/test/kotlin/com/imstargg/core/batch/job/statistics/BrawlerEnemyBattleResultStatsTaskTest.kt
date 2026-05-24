package com.imstargg.core.batch.job.statistics

import com.imstargg.core.batch.domain.BrawlerEnemyBattleResultStatsAggregator
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
import java.time.ZoneOffset

class BrawlerEnemyBattleResultStatsTaskTest {
    private val reader = mockk<BrawlerEnemyBattleResultStatsReader>()
    private val writer = mockk<BrawlerEnemyBattleResultStatsWriter>()
    private val sut = BrawlerEnemyBattleResultStatsTask(reader, writer)

    @Test
    fun `주간 범위마다 이벤트를 조회해 이벤트별로 집계하고 저장한다`() {
        // given
        val weekRange = WeekRange(start = LocalDate.of(2026, 4, 28), end = LocalDate.of(2026, 5, 4))
        val eventId = 10L
        val battle = enemyBattle(eventBrawlStarsId = eventId, battleDate = weekRange.start)
        val aggregatorSlot = slot<BrawlerEnemyBattleResultStatsAggregator>()
        val consumeSlot = slot<(List<StatsResultBattleEntity>) -> Unit>()

        every { reader.readDistinctEventIds(weekRange) } returns listOf(eventId)
        every { reader.read(eq(weekRange), eq(eventId), capture(consumeSlot)) } answers {
            consumeSlot.captured.invoke(listOf(battle))
            1L
        }
        every { writer.write(eq(weekRange), eq(eventId), capture(aggregatorSlot)) } returns 1L

        // when
        sut.aggregate(listOf(weekRange))

        // then
        assertThat(aggregatorSlot.captured.toEntities()).isNotEmpty()
        verify(exactly = 1) { reader.readDistinctEventIds(weekRange) }
        verify(exactly = 1) { reader.read(weekRange, eventId, any()) }
        verify(exactly = 1) { writer.write(weekRange, eventId, any()) }
    }

    @Test
    fun `여러 주간 범위가 주어지면 범위마다 독립적으로 집계한다`() {
        // given
        val weekRange1 = WeekRange(start = LocalDate.of(2026, 4, 28), end = LocalDate.of(2026, 5, 4))
        val weekRange2 = WeekRange(start = LocalDate.of(2026, 5, 5), end = LocalDate.of(2026, 5, 11))
        val eventId1 = 10L
        val eventId2 = 20L
        val battle1 = enemyBattle(eventBrawlStarsId = eventId1, battleDate = weekRange1.start)
        val battle2 = enemyBattle(eventBrawlStarsId = eventId2, battleDate = weekRange2.start)
        val consumeSlot1 = slot<(List<StatsResultBattleEntity>) -> Unit>()
        val consumeSlot2 = slot<(List<StatsResultBattleEntity>) -> Unit>()

        every { reader.readDistinctEventIds(weekRange1) } returns listOf(eventId1)
        every { reader.readDistinctEventIds(weekRange2) } returns listOf(eventId2)
        every { reader.read(eq(weekRange1), eq(eventId1), capture(consumeSlot1)) } answers {
            consumeSlot1.captured.invoke(listOf(battle1))
            1L
        }
        every { reader.read(eq(weekRange2), eq(eventId2), capture(consumeSlot2)) } answers {
            consumeSlot2.captured.invoke(listOf(battle2))
            1L
        }
        every { writer.write(any(), any(), any()) } returns 1L

        // when
        sut.aggregate(listOf(weekRange1, weekRange2))

        // then
        verify(exactly = 1) { reader.readDistinctEventIds(weekRange1) }
        verify(exactly = 1) { reader.readDistinctEventIds(weekRange2) }
        verify(exactly = 1) { reader.read(weekRange1, eventId1, any()) }
        verify(exactly = 1) { reader.read(weekRange2, eventId2, any()) }
        verify(exactly = 1) { writer.write(weekRange1, eventId1, any()) }
        verify(exactly = 1) { writer.write(weekRange2, eventId2, any()) }
    }

    private fun enemyBattle(
        eventBrawlStarsId: Long,
        battleDate: LocalDate,
    ): StatsResultBattleEntity {
        // 적군 페어가 만들어지려면 본인 팀 1명 + 다른 팀(적군) 1명 이상 필요
        val teams =
            listOf(
                listOf(player(index = 1)),
                listOf(player(index = 2)),
            )
        return StatsResultBattleEntity(
            battleKey = "battle-key",
            battleAt = OffsetDateTime.of(battleDate.atStartOfDay(), ZoneOffset.ofHours(9)),
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
