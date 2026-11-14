package com.imstargg.core.batch.domain.statistics.patch

import com.imstargg.core.batch.domain.statistics.statsPlayer
import com.imstargg.core.batch.domain.statistics.statsResultBattle
import com.imstargg.core.enums.BattleResult
import com.imstargg.infrastructure.db.core.statistics.battle.StatsBattleEntityTeamPlayer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime

class BrawlerPairBattleResultPatchStatsAggregatorTest {
    @Test
    fun `2명 팀에서 정렬 쌍 1개를 생성한다`() {
        val counts = aggregate(listOf(statsPlayer(2), statsPlayer(1)))

        assertThat(counts).hasSize(1)
        assertThat(counts.single().keys).containsExactly("TROPHY_1000_PLUS", 1L, 2L)
    }

    @Test
    fun `3명 팀은 정렬 쌍 3개를 만든다`() {
        val counts = aggregate(listOf(statsPlayer(3), statsPlayer(1), statsPlayer(2)))

        assertThat(counts.map { it.keys[1] to it.keys[2] }).containsExactlyInAnyOrder(1L to 2L, 1L to 3L, 2L to 3L)
    }

    @Test
    fun `5명 팀에서 정렬 쌍 10개를 생성한다`() {
        val counts = aggregate((1..5).map { statsPlayer(it) })

        assertThat(counts).hasSize(10)
        assertThat(counts.map { it.keys[1] to it.keys[2] }).doesNotHaveDuplicates()
    }

    @Test
    fun `팀원이 1명이면 쌍을 만들지 않는다`() {
        assertThat(aggregate(listOf(statsPlayer(1)))).isEmpty()
    }

    @Test
    fun `티어는 두 멤버 trophies 의 최솟값으로 정한다`() {
        val counts = aggregate(listOf(statsPlayer(1, 3000), statsPlayer(2, 1000), statsPlayer(3, 2500)))

        assertThat(counts).hasSize(4)
        assertThat(counts.tiersOf(1, 2)).containsExactly("TROPHY_1000_PLUS")
        assertThat(counts.tiersOf(2, 3)).containsExactly("TROPHY_1000_PLUS")
        assertThat(counts.tiersOf(1, 3)).containsExactlyInAnyOrder("TROPHY_1000_PLUS", "TROPHY_2000_PLUS")
    }

    @Test
    fun `적군 팀은 반대 결과로 집계한다`() {
        val sut = BrawlerPairBattleResultPatchStatsAggregator()
        sut.aggregate(statsResultBattle(listOf(listOf(statsPlayer(1)), listOf(statsPlayer(11), statsPlayer(12)))))

        assertThat(sut.toCounts()).containsExactly(
            PatchStatsCount(listOf("TROPHY_1000_PLUS", 11L, 12L), listOf(0L, 1L, 0L, 1L)),
        )
    }

    @Test
    fun `아군 팀은 전투 결과 그대로 집계한다`() {
        assertThat(aggregate(listOf(statsPlayer(1), statsPlayer(2))).single().counts).containsExactly(1L, 0L, 0L, 1L)
    }

    @Test
    fun `키에 이벤트가 없어 다른 이벤트 전투가 같은 줄에 합산된다`() {
        val teams = listOf(listOf(statsPlayer(1), statsPlayer(2)))
        val sut = BrawlerPairBattleResultPatchStatsAggregator()
        sut.aggregate(statsResultBattle(teams, eventBrawlStarsId = 1))
        sut.aggregate(statsResultBattle(teams, result = BattleResult.DEFEAT.code, eventBrawlStarsId = 2, battleKey = "other"))

        assertThat(sut.toCounts().single().counts).containsExactly(1L, 1L, 0L, 2L)
    }

    @Test
    fun `여러 날짜의 전투가 한 키로 합쳐진다`() {
        val teams = listOf(listOf(statsPlayer(1), statsPlayer(2)))
        val sut = BrawlerPairBattleResultPatchStatsAggregator()
        sut.aggregate(statsResultBattle(teams, battleAt = OffsetDateTime.parse("2026-11-01T12:00:00+09:00").toInstant()))
        sut.aggregate(
            statsResultBattle(teams, battleKey = "other", battleAt = OffsetDateTime.parse("2026-11-09T12:00:00+09:00").toInstant()),
        )

        assertThat(sut.toCounts().single().counts).containsExactly(2L, 0L, 0L, 2L)
    }

    private fun aggregate(players: List<StatsBattleEntityTeamPlayer>): List<PatchStatsCount> =
        BrawlerPairBattleResultPatchStatsAggregator()
            .also { it.aggregate(statsResultBattle(listOf(players))) }
            .toCounts()

    private fun List<PatchStatsCount>.tiersOf(
        brawler1: Long,
        brawler2: Long,
    ): List<Any> = filter { it.keys[1] == brawler1 && it.keys[2] == brawler2 }.map { it.keys[0] }
}
