package com.imstargg.core.batch.domain.statistics.patch

import com.imstargg.core.batch.domain.statistics.statsPlayer
import com.imstargg.core.batch.domain.statistics.statsResultBattle
import com.imstargg.core.enums.BattleResult
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime

class BrawlerEnemyBattleResultPatchStatsAggregatorTest {
    @Test
    fun `내 팀 (1) vs 적 (2) 승리면 (1,2) 승1, (2,1) 패1 을 만든다`() {
        val sut = BrawlerEnemyBattleResultPatchStatsAggregator()
        sut.aggregate(statsResultBattle(listOf(listOf(statsPlayer(1)), listOf(statsPlayer(2)))))

        assertThat(sut.toCounts()).containsExactlyInAnyOrder(
            PatchStatsCount(listOf("TROPHY_1000_PLUS", 1L, 2L), listOf(1L, 0L, 0L, 1L)),
            PatchStatsCount(listOf("TROPHY_1000_PLUS", 2L, 1L), listOf(0L, 1L, 0L, 1L)),
        )
    }

    @Test
    fun `키에 이벤트가 없어 다른 이벤트 전투가 같은 줄에 합산된다`() {
        val teams = listOf(listOf(statsPlayer(1)), listOf(statsPlayer(2)))
        val sut = BrawlerEnemyBattleResultPatchStatsAggregator()
        sut.aggregate(statsResultBattle(teams, eventBrawlStarsId = 1))
        sut.aggregate(statsResultBattle(teams, eventBrawlStarsId = 2, battleKey = "other"))

        assertThat(sut.toCounts()).hasSize(2)
        assertThat(sut.toCounts().single { it.keys[1] == 1L && it.keys[2] == 2L }.counts)
            .containsExactly(2L, 0L, 0L, 2L)
    }

    @Test
    fun `티어는 기준 플레이어의 trophies 로 정한다`() {
        val sut = BrawlerEnemyBattleResultPatchStatsAggregator()
        sut.aggregate(statsResultBattle(listOf(listOf(statsPlayer(1, 2500)), listOf(statsPlayer(2, 1000)))))

        val counts = sut.toCounts()
        assertThat(counts.filter { it.keys[1] == 1L }.map { it.keys[0] })
            .containsExactlyInAnyOrder("TROPHY_1000_PLUS", "TROPHY_2000_PLUS")
        assertThat(counts.filter { it.keys[1] == 2L }.map { it.keys[0] }).containsExactly("TROPHY_1000_PLUS")
    }

    @Test
    fun `여러 날짜의 전투가 한 키로 합쳐진다`() {
        val teams = listOf(listOf(statsPlayer(1)), listOf(statsPlayer(2)))
        val sut = BrawlerEnemyBattleResultPatchStatsAggregator()
        sut.aggregate(statsResultBattle(teams, battleAt = OffsetDateTime.parse("2026-11-01T12:00:00+09:00").toInstant()))
        sut.aggregate(
            statsResultBattle(
                teams,
                result = BattleResult.DRAW.code,
                battleKey = "other",
                battleAt = OffsetDateTime.parse("2026-11-09T12:00:00+09:00").toInstant(),
            ),
        )

        assertThat(sut.toCounts().single { it.keys[1] == 1L }.counts).containsExactly(1L, 0L, 1L, 2L)
    }
}
