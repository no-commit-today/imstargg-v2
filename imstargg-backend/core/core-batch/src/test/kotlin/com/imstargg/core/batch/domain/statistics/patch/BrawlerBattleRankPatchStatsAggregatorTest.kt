package com.imstargg.core.batch.domain.statistics.patch

import com.imstargg.core.batch.domain.statistics.showdown
import com.imstargg.core.batch.domain.statistics.statsPlayer
import com.imstargg.core.batch.domain.statistics.statsRankBattle
import com.imstargg.core.enums.StatsEventModeType
import com.imstargg.infrastructure.db.core.statistics.battle.StatsRankBattleEntity
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime

class BrawlerBattleRankPatchStatsAggregatorTest {
    @Test
    fun `수집자 팀만 본인 트로피의 티어로 순위를 집계한다`() {
        val counts =
            aggregate(duo(listOf(1000, 1010), listOf(1200, 1210), listOf(1400, 1410), listOf(1600, 1610), battleRank = 2))

        assertThat(counts.map { Triple(it.keys[0], it.keys[1], it.keys[2]) }).containsExactlyInAnyOrder(
            Triple("TROPHY_1000_PLUS", 1L, 1L),
            Triple("TROPHY_1000_PLUS", 1L, 2L),
            Triple("TROPHY_2000_PLUS", 1L, 2L),
        )
        assertThat(counts).allSatisfy {
            assertThat(it.counts).containsExactly(0L, 1L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 1L)
        }
    }

    @Test
    fun `봇 로비는 건너뛰고 임계값 아래 로비는 집계한다`() {
        assertThat(aggregate(duo(listOf(1000, 1010), listOf(1020, 1030), listOf(1400, 1410), listOf(1600, 1610)))).isEmpty()
        assertThat(aggregate(duo(listOf(1000, 1010), listOf(1200, 1210), listOf(1400, 1410), listOf(1600, 1610)))).isNotEmpty()
    }

    @Test
    fun `20인 솔로 쇼다운(모드 75)의 순위 19 전투는 건너뛰고 같은 날짜의 솔로 쇼다운 전투는 집계한다`() {
        val sut = BrawlerBattleRankPatchStatsAggregator()

        sut.aggregate(statsRankBattle(teams = (101..120).map { listOf(statsPlayer(it)) }, battleRank = 19, eventModeBrawlStarsId = 75))
        sut.aggregate(showdown(StatsEventModeType.SOLO_SHOWDOWN, listOf(listOf(1500)) + List(9) { listOf(2100) }))

        assertThat(sut.toCounts().map { it.keys[2] }).containsOnly(1L)
    }

    @Test
    fun `10팀 듀오 쇼다운(모드 86) 전투는 집계하지 않는다`() {
        val tenDuos = (0 until 10).map { t -> listOf(statsPlayer(2 * t + 1), statsPlayer(2 * t + 2)) }

        assertThat(aggregate(statsRankBattle(teams = tenDuos, battleRank = 3, eventModeBrawlStarsId = 86))).isEmpty()
    }

    @Test
    fun `쇼다운 구성이면 모드 ID 와 관계없이 집계한다`() {
        val teams = showdown(StatsEventModeType.SOLO_SHOWDOWN, listOf(listOf(1500)) + List(9) { listOf(2100) }).teams

        assertThat(aggregate(statsRankBattle(teams = teams, eventModeBrawlStarsId = 999))).isNotEmpty()
    }

    @Test
    fun `여러 날짜의 전투가 한 키로 합쳐진다`() {
        val sut = BrawlerBattleRankPatchStatsAggregator()
        val teams = listOf(listOf(1500)) + List(9) { listOf(2100) }
        sut.aggregate(
            showdown(
                StatsEventModeType.SOLO_SHOWDOWN,
                teams,
                battleRank = 1,
                battleAt = OffsetDateTime.parse("2026-11-01T12:00:00+09:00").toInstant(),
            ),
        )
        sut.aggregate(
            showdown(
                StatsEventModeType.SOLO_SHOWDOWN,
                teams,
                battleRank = 10,
                battleKey = "other",
                battleAt = OffsetDateTime.parse("2026-11-09T12:00:00+09:00").toInstant(),
            ),
        )

        assertThat(sut.toCounts().single { it.keys[2] == 1L }.counts)
            .containsExactly(1L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 1L, 2L)
    }

    @Test
    fun `순위가 10 을 넘으면 실패한다`() {
        val teams = listOf(listOf(1500)) + List(9) { listOf(2100) }

        assertThatThrownBy { aggregate(showdown(StatsEventModeType.SOLO_SHOWDOWN, teams, battleRank = 11)) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `순위가 1 미만인 전투는 옛 집계처럼 세지 않는다`() {
        val teams = listOf(listOf(1500)) + List(9) { listOf(2100) }

        assertThat(aggregate(showdown(StatsEventModeType.SOLO_SHOWDOWN, teams, battleRank = 0))).isEmpty()

        val sut = BrawlerBattleRankPatchStatsAggregator()
        sut.aggregate(showdown(StatsEventModeType.SOLO_SHOWDOWN, teams, battleRank = 0))
        sut.aggregate(showdown(StatsEventModeType.SOLO_SHOWDOWN, teams, battleRank = 2, battleKey = "other"))
        assertThat(sut.toCounts().single { it.keys[2] == 1L }.counts)
            .containsExactly(0L, 1L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 1L)
    }

    private fun aggregate(battle: StatsRankBattleEntity) = BrawlerBattleRankPatchStatsAggregator().also { it.aggregate(battle) }.toCounts()

    private fun duo(
        vararg others: List<Int>,
        battleRank: Int = 1,
    ) = showdown(StatsEventModeType.DUO_SHOWDOWN, listOf(listOf(1500, 2100)) + others, battleRank = battleRank)
}
