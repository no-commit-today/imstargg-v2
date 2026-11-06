package com.imstargg.core.batch.job.statistics.battleresult

import com.imstargg.core.batch.domain.statistics.BrawlerBattleResultStatsAggregator
import com.imstargg.core.batch.domain.statistics.threeVsThree
import com.imstargg.core.batch.support.alert.AlertCommand
import com.imstargg.core.batch.support.alert.AlertManager
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
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.OffsetDateTime

class BrawlerBattleResultStatsTaskTest {
    private val reader = mockk<BrawlerBattleResultStatsReader>()
    private val writer = mockk<BrawlerBattleResultStatsWriter>()
    private val alertManager = mockk<AlertManager>(relaxUnitFun = true)
    private val sut = BrawlerBattleResultStatsTask(reader = reader, writer = writer, alertManager = alertManager)
    private val date = LocalDate.of(2026, 5, 1)

    @Test
    fun `리더가 전달한 배틀을 집계해 writer 에 저장한다`() {
        // given
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
        verify(exactly = 0) { alertManager.alert(any()) }
    }

    @Test
    fun `경고 조건에 걸린 지표가 있으면 알림을 한 번 보낸다`() {
        givenBattles(List(300) { aboveLimit() })

        sut.aggregate(date)

        val alert = slot<AlertCommand>()
        verify(exactly = 1) { alertManager.alert(capture(alert)) }
        assertThat(alert.captured.title).contains("[brawlerBattleResultStatsJob] 봇 전투 규칙 이상 감지")
        assertThat(alert.captured.content!!.lines()).hasSize(2)
        assertThat(alert.captured.content)
            .startsWith("date: 2026-05-01")
            .contains("2000 이상 구간 승률 100.0% (표본 300건, 문턱 75.0% 초과)")
    }

    @Test
    fun `지표가 둘 걸려도 알림은 한 번이고 본문에 한 줄씩 적는다`() {
        givenBattles(List(300) { aboveLimit() } + List(1000) { bot("defeat") })

        sut.aggregate(date)

        val alert = slot<AlertCommand>()
        verify(exactly = 1) { alertManager.alert(capture(alert)) }
        assertThat(alert.captured.content!!.lines()).hasSize(3)
        assertThat(alert.captured.content).contains("걸린 전투 승률 0.0%", "2000 이상 구간 승률 100.0%")
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
        givenBattles(List(300) { aboveLimit() })
        every { alertManager.alert(any()) } throws RuntimeException("boom")

        assertThatCode { sut.aggregate(date) }.doesNotThrowAnyException()
        verify(exactly = 1) { writer.write(eq(date), any()) }
    }

    private fun givenBattles(battles: List<StatsResultBattleEntity>) {
        val consumeSlot = slot<(List<StatsResultBattleEntity>) -> Unit>()
        every { reader.read(eq(date), capture(consumeSlot)) } answers {
            consumeSlot.captured.invoke(battles)
            battles.size.toLong()
        }
        every { writer.write(eq(date), any()) } returns 1L
    }

    private fun aboveLimit(result: String = "victory") = threeVsThree(listOf(2000, 2000, 1970), listOf(1950, 1960, 2000), result = result)

    private fun bot(result: String) = threeVsThree(listOf(1000, 1100, 1200), listOf(1050, 1080, 1090), result = result)

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
