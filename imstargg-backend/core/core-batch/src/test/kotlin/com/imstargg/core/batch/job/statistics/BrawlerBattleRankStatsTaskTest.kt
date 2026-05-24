package com.imstargg.core.batch.job.statistics

import com.imstargg.core.batch.domain.BrawlerBattleRankStatsAggregator
import com.imstargg.core.enums.BattleType
import com.imstargg.infrastructure.db.core.statistics.battle.StatsBattleEntityTeamPlayer
import com.imstargg.infrastructure.db.core.statistics.battle.StatsBattleEntityTeamPlayerBrawler
import com.imstargg.infrastructure.db.core.statistics.battle.StatsRankBattleEntity
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.OffsetDateTime

class BrawlerBattleRankStatsTaskTest {
    private val reader = mockk<BrawlerBattleRankStatsReader>()
    private val writer = mockk<BrawlerBattleRankStatsWriter>()
    private val sut = BrawlerBattleRankStatsTask(reader, writer)

    @Test
    fun `리더가 전달한 배틀을 집계해 writer 에 저장한다`() {
        // given
        val date = LocalDate.of(2026, 5, 1)
        val battle = rankBattle()
        val aggregatorSlot = slot<BrawlerBattleRankStatsAggregator>()
        val consumeSlot = slot<(List<StatsRankBattleEntity>) -> Unit>()
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

    private fun rankBattle(): StatsRankBattleEntity {
        val playerTag = "#TAG1"
        val teams = listOf(listOf(player(tag = playerTag, index = 1)))
        return StatsRankBattleEntity(
            battleKey = "battle-key",
            battleAt = OffsetDateTime.parse("2026-05-01T09:00:00+09:00"),
            eventBrawlStarsId = 1,
            eventModeBrawlStarsId = 2,
            eventMap = "test-map",
            mode = "soloShowdown",
            type = BattleType.RANKED.code,
            playerBrawlStarsTag = playerTag,
            teams = teams,
            battleRank = 1,
        )
    }

    private fun player(
        tag: String,
        index: Int,
    ): StatsBattleEntityTeamPlayer =
        StatsBattleEntityTeamPlayer(
            brawlStarsTag = tag,
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
