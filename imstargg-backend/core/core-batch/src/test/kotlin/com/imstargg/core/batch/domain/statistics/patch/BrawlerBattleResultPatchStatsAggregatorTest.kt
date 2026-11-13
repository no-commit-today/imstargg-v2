package com.imstargg.core.batch.domain.statistics.patch

import com.imstargg.core.batch.domain.statistics.statsPlayer
import com.imstargg.core.batch.domain.statistics.statsResultBattle
import com.imstargg.core.enums.BattleResult
import com.imstargg.infrastructure.db.core.statistics.battle.StatsResultBattleEntity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime

class BrawlerBattleResultPatchStatsAggregatorTest {
    @Test
    fun `수집자 팀은 결과 그대로, 상대 팀은 반대로 집계한다`() {
        val counts = aggregate(statsResultBattle(listOf(listOf(statsPlayer(1)), listOf(statsPlayer(2)))))

        assertThat(counts).containsExactlyInAnyOrder(
            PatchStatsCount(listOf("TROPHY_1000_PLUS", 1L, 1L), listOf(1L, 0L, 0L, 1L, 0L)),
            PatchStatsCount(listOf("TROPHY_1000_PLUS", 1L, 2L), listOf(0L, 1L, 0L, 1L, 0L)),
        )
    }

    @Test
    fun `티어는 플레이어 본인의 trophies 로 정한다`() {
        val counts = aggregate(statsResultBattle(listOf(listOf(statsPlayer(1, 2500)), listOf(statsPlayer(2, 1000)))))

        assertThat(counts.filter { it.keys[2] == 1L }.map { it.keys[0] })
            .containsExactlyInAnyOrder("TROPHY_1000_PLUS", "TROPHY_2000_PLUS")
        assertThat(counts.filter { it.keys[2] == 2L }.map { it.keys[0] }).containsExactly("TROPHY_1000_PLUS")
    }

    @Test
    fun `스타 플레이어의 줄만 starPlayerCount 를 센다`() {
        val counts =
            aggregate(statsResultBattle(listOf(listOf(statsPlayer(1)), listOf(statsPlayer(2))), starPlayerBrawlStarsTag = "#TAG2"))

        assertThat(counts.single { it.keys[2] == 1L }.counts.last()).isZero()
        assertThat(counts.single { it.keys[2] == 2L }.counts.last()).isEqualTo(1L)
    }

    @Test
    fun `유형이나 결과가 알 수 없는 코드면 건너뛴다`() {
        val teams = listOf(listOf(statsPlayer(1)), listOf(statsPlayer(2)))
        assertThat(aggregate(statsResultBattle(teams, type = "unknown"))).isEmpty()
        assertThat(aggregate(statsResultBattle(teams, result = "unknown"))).isEmpty()
    }

    @Test
    fun `여러 날짜의 전투가 한 키로 합쳐진다`() {
        val teams = listOf(listOf(statsPlayer(1)), listOf(statsPlayer(2)))
        val sut = BrawlerBattleResultPatchStatsAggregator()
        sut.aggregate(statsResultBattle(teams, battleAt = OffsetDateTime.parse("2026-11-01T12:00:00+09:00").toInstant()))
        sut.aggregate(
            statsResultBattle(
                teams,
                result = BattleResult.DEFEAT.code,
                battleKey = "other",
                battleAt = OffsetDateTime.parse("2026-11-09T12:00:00+09:00").toInstant(),
            ),
        )

        assertThat(sut.toCounts()).containsExactlyInAnyOrder(
            PatchStatsCount(listOf("TROPHY_1000_PLUS", 1L, 1L), listOf(1L, 1L, 0L, 2L, 0L)),
            PatchStatsCount(listOf("TROPHY_1000_PLUS", 1L, 2L), listOf(1L, 1L, 0L, 2L, 0L)),
        )
    }

    private fun aggregate(battle: StatsResultBattleEntity) =
        BrawlerBattleResultPatchStatsAggregator().also { it.aggregate(battle) }.toCounts()
}
