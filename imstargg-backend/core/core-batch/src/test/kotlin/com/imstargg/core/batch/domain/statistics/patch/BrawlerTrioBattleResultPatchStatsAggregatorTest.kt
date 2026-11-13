package com.imstargg.core.batch.domain.statistics.patch

import com.imstargg.core.batch.domain.statistics.statsPlayer
import com.imstargg.core.batch.domain.statistics.statsResultBattle
import com.imstargg.infrastructure.db.core.statistics.battle.StatsBattleEntityTeamPlayer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime

class BrawlerTrioBattleResultPatchStatsAggregatorTest {
    @Test
    fun `3명 팀은 정렬 키 1줄을 만든다`() {
        val counts = aggregate(listOf(statsPlayer(3), statsPlayer(1), statsPlayer(2)))

        assertThat(counts.single().keys).containsExactly("TROPHY_1000_PLUS", 1L, 2L, 3L)
    }

    @Test
    fun `티어는 세 멤버 trophies 최솟값으로 정한다`() {
        val counts = aggregate(listOf(statsPlayer(1, 3000), statsPlayer(2, 1000), statsPlayer(3, 2500)))

        assertThat(counts.map { it.keys[0] }).containsExactly("TROPHY_1000_PLUS")
    }

    @Test
    fun `적군 팀은 반대 결과로 집계한다`() {
        val sut = BrawlerTrioBattleResultPatchStatsAggregator()
        sut.aggregate(
            statsResultBattle(listOf(listOf(statsPlayer(1)), listOf(statsPlayer(11), statsPlayer(12), statsPlayer(13)))),
        )

        assertThat(sut.toCounts()).containsExactly(
            PatchStatsCount(listOf("TROPHY_1000_PLUS", 11L, 12L, 13L), listOf(0L, 1L, 0L, 1L)),
        )
    }

    @Test
    fun `아군 팀은 전투 결과 그대로 집계한다`() {
        val counts = aggregate(listOf(statsPlayer(1), statsPlayer(2), statsPlayer(3)))

        assertThat(counts.single().counts).containsExactly(1L, 0L, 0L, 1L)
    }

    @Test
    fun `인원이 3이 아닌 팀은 건너뛴다`() {
        assertThat(aggregate(listOf(statsPlayer(1), statsPlayer(2)))).isEmpty()
        assertThat(aggregate((1..4).map { statsPlayer(it) })).isEmpty()
    }

    @Test
    fun `여러 날짜의 전투가 한 키로 합쳐진다`() {
        val teams = listOf(listOf(statsPlayer(1), statsPlayer(2), statsPlayer(3)))
        val sut = BrawlerTrioBattleResultPatchStatsAggregator()
        sut.aggregate(statsResultBattle(teams, battleAt = OffsetDateTime.parse("2026-11-01T12:00:00+09:00").toInstant()))
        sut.aggregate(
            statsResultBattle(teams, battleKey = "other", battleAt = OffsetDateTime.parse("2026-11-09T12:00:00+09:00").toInstant()),
        )

        assertThat(sut.toCounts().single().counts).containsExactly(2L, 0L, 0L, 2L)
    }

    private fun aggregate(players: List<StatsBattleEntityTeamPlayer>): List<PatchStatsCount> =
        BrawlerTrioBattleResultPatchStatsAggregator()
            .also { it.aggregate(statsResultBattle(listOf(players))) }
            .toCounts()
}
