package com.imstargg.core.batch.job.statistics

import com.imstargg.core.batch.domain.BrawlerBattleResultStatsAggregator
import com.imstargg.core.enums.BattleResult
import com.imstargg.core.enums.BattleType
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

class BrawlerBattleResultStatsTaskTest {
    private val reader = mockk<BrawlerBattleResultStatsReader>()
    private val writer = mockk<BrawlerBattleResultStatsWriter>()
    private val sut = BrawlerBattleResultStatsTask(reader, writer)

    @Test
    fun `리더가 전달한 배틀을 집계해 writer 에 저장한다`() {
        // given
        val date = LocalDate.of(2026, 5, 1)
        val sampleBattles = listOf(resultBattle())
        val consumeSlot = slot<(List<StatsResultBattleEntity>) -> Unit>()
        val aggregatorSlot = slot<BrawlerBattleResultStatsAggregator>()

        every { reader.read(eq(date), capture(consumeSlot)) } answers {
            consumeSlot.captured.invoke(sampleBattles)
            sampleBattles.size.toLong()
        }
        every { writer.write(eq(date), capture(aggregatorSlot)) } returns 0L

        // when
        sut.aggregate(date)

        // then
        verify(exactly = 1) { reader.read(eq(date), any()) }
        verify(exactly = 1) { writer.write(eq(date), any()) }
        // 리더가 넘긴 배틀이 writer 로 전달된 aggregator 에 집계되어 있어야 한다
        val aggregatedEntities = aggregatorSlot.captured.toEntities()
        assertThat(aggregatedEntities).isNotEmpty
        assertThat(aggregatedEntities.map { it.brawlerBrawlStarsId })
            .containsOnly(1L, 2L)
        assertThat(aggregatedEntities).allSatisfy { entity ->
            assertThat(entity.eventBrawlStarsId).isEqualTo(1L)
        }
    }

    @Test
    fun `리더가 빈 배틀 리스트를 넘겨도 writer 가 호출된다`() {
        // given
        val date = LocalDate.of(2026, 5, 1)
        val consumeSlot = slot<(List<StatsResultBattleEntity>) -> Unit>()
        val aggregatorSlot = slot<BrawlerBattleResultStatsAggregator>()

        every { reader.read(eq(date), capture(consumeSlot)) } answers {
            consumeSlot.captured.invoke(emptyList())
            0L
        }
        every { writer.write(eq(date), capture(aggregatorSlot)) } returns 0L

        // when
        sut.aggregate(date)

        // then
        verify(exactly = 1) { reader.read(eq(date), any()) }
        verify(exactly = 1) { writer.write(eq(date), any()) }
        assertThat(aggregatorSlot.captured.toEntities()).isEmpty()
    }

    private fun resultBattle(): StatsResultBattleEntity =
        StatsResultBattleEntity(
            battleKey = "battle-test",
            battleAt = OffsetDateTime.parse("2026-05-01T00:00:00+09:00"),
            eventBrawlStarsId = 1,
            eventModeBrawlStarsId = 2,
            eventMap = "test-map",
            mode = "gemGrab",
            type = BattleType.RANKED.code,
            starPlayerBrawlStarsTag = null,
            playerBrawlStarsTag = "#TAG1",
            teams =
                listOf(
                    listOf(player(brawlerBrawlStarsId = 1)),
                    listOf(player(brawlerBrawlStarsId = 2)),
                ),
            result = BattleResult.VICTORY.code,
        )

    private fun player(brawlerBrawlStarsId: Long): StatsBattleEntityTeamPlayer =
        StatsBattleEntityTeamPlayer(
            brawlStarsTag = "#TAG$brawlerBrawlStarsId",
            name = "Player$brawlerBrawlStarsId",
            brawler =
                StatsBattleEntityTeamPlayerBrawler(
                    brawlStarsId = brawlerBrawlStarsId,
                    name = "Brawler$brawlerBrawlStarsId",
                    power = 11,
                    trophies = 1000,
                ),
        )
}
