package com.imstargg.core.batch.job.statistics.battlerank

import com.imstargg.core.batch.domain.statistics.BrawlerBattleRankStatsAggregator
import com.imstargg.core.batch.domain.statistics.showdown
import com.imstargg.core.batch.support.alert.AlertCommand
import com.imstargg.core.batch.support.alert.AlertManager
import com.imstargg.core.enums.BattleType
import com.imstargg.core.enums.StatsEventMode
import com.imstargg.infrastructure.db.core.statistics.battle.StatsBattleEntityTeamPlayer
import com.imstargg.infrastructure.db.core.statistics.battle.StatsBattleEntityTeamPlayerBrawler
import com.imstargg.infrastructure.db.core.statistics.battle.StatsRankBattleEntity
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.OffsetDateTime

class BrawlerBattleRankStatsTaskTest {
    private val reader = mockk<BrawlerBattleRankStatsReader>()
    private val writer = mockk<BrawlerBattleRankStatsWriter>()
    private val alertManager = mockk<AlertManager>(relaxUnitFun = true)
    private val sut = BrawlerBattleRankStatsTask(reader = reader, writer = writer, alertManager = alertManager)
    private val date = LocalDate.of(2026, 5, 1)

    @Test
    fun `리더가 전달한 배틀을 집계해 writer 에 저장한다`() {
        // given
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
        verify(exactly = 0) { alertManager.alert(any()) }
    }

    @Test
    fun `경고 조건에 걸린 지표가 있으면 알림을 한 번 보낸다`() {
        givenBattles(List(300) { trioBotLobby() })

        sut.aggregate(date)

        val alert = slot<AlertCommand>()
        verify(exactly = 1) { alertManager.alert(capture(alert)) }
        assertThat(alert.captured.title).contains("[brawlerBattleRankStatsJob] 봇 전투 규칙 이상 감지")
        assertThat(alert.captured.content!!.lines()).hasSize(2)
        assertThat(alert.captured.content)
            .startsWith("date: 2026-05-01")
            .contains("트리오 걸린 전투 1위 비율 0.0% (표본 300건, 문턱 70.0% 미만)")
    }

    @Test
    fun `지표가 둘 걸려도 알림은 한 번이고 본문에 한 줄씩 적는다`() {
        givenBattles(List(300) { trioBotLobby() } + List(500) { duoBotLobby() })

        sut.aggregate(date)

        val alert = slot<AlertCommand>()
        verify(exactly = 1) { alertManager.alert(capture(alert)) }
        assertThat(alert.captured.content!!.lines()).hasSize(3)
        assertThat(alert.captured.content).contains("트리오 걸린 전투 1위 비율 0.0%", "듀오 걸린 전투 1위 비율 0.0%")
    }

    @Test
    fun `전투가 없으면 알림을 보내지 않는다`() {
        every { reader.read(eq(date), any()) } returns 0L
        every { writer.write(eq(date), any()) } returns 0L

        sut.aggregate(date)

        verify(exactly = 0) { alertManager.alert(any()) }
    }

    @Test
    fun `알림 전송에서 예외가 나도 정상 종료한다`() {
        givenBattles(List(300) { trioBotLobby() })
        every { alertManager.alert(any()) } throws RuntimeException("boom")

        assertThatCode { sut.aggregate(date) }.doesNotThrowAnyException()
        verify(exactly = 1) { writer.write(eq(date), any()) }
    }

    private fun givenBattles(battles: List<StatsRankBattleEntity>) {
        val consumeSlot = slot<(List<StatsRankBattleEntity>) -> Unit>()
        every { reader.read(eq(date), capture(consumeSlot)) } answers {
            consumeSlot.captured.invoke(battles)
            battles.size.toLong()
        }
        every { writer.write(eq(date), any()) } returns 1L
    }

    private fun trioBotLobby() =
        showdown(
            StatsEventMode.TRIO_SHOWDOWN,
            listOf(listOf(1500, 1500, 1500), listOf(1000, 1010, 1020), listOf(1030, 1040, 1050), listOf(1400, 1410, 1420)),
            battleRank = 2,
        )

    private fun duoBotLobby() =
        showdown(
            StatsEventMode.DUO_SHOWDOWN,
            listOf(listOf(1500, 1500), listOf(1000, 1010), listOf(1020, 1030), listOf(1400, 1410), listOf(1600, 1610)),
            battleRank = 2,
        )

    private fun rankBattle(): StatsRankBattleEntity {
        val playerTag = "#TAG1"
        val teams = listOf(listOf(player(tag = playerTag, index = 1)))
        return StatsRankBattleEntity(
            battleKey = "battle-key",
            battleAt = OffsetDateTime.parse("2026-05-01T09:00:00+09:00").toInstant(),
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
