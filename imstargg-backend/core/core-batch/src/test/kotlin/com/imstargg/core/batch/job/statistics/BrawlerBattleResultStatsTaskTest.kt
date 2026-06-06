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
        val battle = resultBattle()
        val aggregatorSlot = slot<BrawlerBattleResultStatsAggregator>()
        val consumeSlot = slot<(List<StatsResultBattleEntity>) -> Unit>()
        every { reader.read(eq(date), capture(consumeSlot)) } answers {
            consumeSlot.captured.invoke(listOf(battle))
            1L
        }
        every { writer.write(eq(date), capture(aggregatorSlot)) } returns 1L

        // when
        sut.aggregate(date)

        // then
        assertThat(aggregatorSlot.captured.toEntities()).isNotEmpty()
        verify(exactly = 1) { reader.read(eq(date), any()) }
        verify(exactly = 1) { writer.write(eq(date), any()) }
    }

    private fun resultBattle(): StatsResultBattleEntity {
        val teams =
            listOf(
                listOf(player(index = 1)),
                listOf(player(index = 2)),
            )
        return StatsResultBattleEntity(
            battleKey = "battle-key",
            battleAt = OffsetDateTime.parse("2026-05-01T09:00:00+09:00").toInstant(),
            eventBrawlStarsId = 1,
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
