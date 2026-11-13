package com.imstargg.core.batch.domain.statistics.patch

import com.imstargg.core.batch.domain.statistics.showdown
import com.imstargg.core.batch.domain.statistics.threeVsThree
import com.imstargg.core.enums.StatsEventModeType
import com.imstargg.infrastructure.db.core.statistics.battle.StatsResultBattleEntity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** 새 결과형 집계기 네 개와 순위 집계기가 같은 봇 전투 판정을 공유하는지 고정한다. */
class PatchStatsBotBattleExclusionTest {
    @Test
    fun `결과형 집계기 네 개가 같은 봇 전투를 모두 건너뛴다`() {
        assertThat(countSizes(threeVsThree(listOf(1000, 1100, 1200), listOf(1050, 1080, 1090))))
            .containsExactly(0, 0, 0, 0)
    }

    @Test
    fun `상대 트로피를 창 밖으로 옮기면 네 개 모두 집계한다`() {
        // 1인 6줄, 상대 3×3×2 = 18줄, 2인 3 + 3 = 6줄, 3인 2줄
        assertThat(countSizes(threeVsThree(listOf(1000, 1100, 1200), listOf(1150, 1180, 1190))))
            .containsExactly(6, 18, 6, 2)
    }

    @Test
    fun `2000 이상 슬롯이 낀 봇 전투도 통째로 건너뛴다`() {
        // S = 4500 → 창은 1425 이상 1525 미만
        assertThat(countSizes(threeVsThree(listOf(1000, 1000, 2500), listOf(1450, 1460, 1470))))
            .containsExactly(0, 0, 0, 0)

        val aggregator = BrawlerBattleResultPatchStatsAggregator()
        aggregator.aggregate(threeVsThree(listOf(1000, 1000, 2500), listOf(1550, 1560, 1570)))
        assertThat(aggregator.toCounts().map { it.keys[0] }).contains("TROPHY_2000_PLUS")
    }

    @Test
    fun `경쟁전 전투는 집계한다`() {
        assertThat(countSizes(threeVsThree(listOf(19, 19, 19), listOf(19, 19, 19), type = "soloRanked")))
            .allMatch { it > 0 }
    }

    @Test
    fun `봇 로비인 순위 전투는 건너뛴다`() {
        val sut = BrawlerBattleRankPatchStatsAggregator()
        sut.aggregate(
            showdown(
                StatsEventModeType.DUO_SHOWDOWN,
                listOf(listOf(1500, 2100), listOf(1020, 1030), listOf(1400, 1410), listOf(1600, 1610)),
            ),
        )

        assertThat(sut.toCounts()).isEmpty()
    }

    private fun countSizes(battle: StatsResultBattleEntity): List<Int> =
        listOf(
            BrawlerBattleResultPatchStatsAggregator(),
            BrawlerEnemyBattleResultPatchStatsAggregator(),
            BrawlerPairBattleResultPatchStatsAggregator(),
            BrawlerTrioBattleResultPatchStatsAggregator(),
        ).map {
            it.aggregate(battle)
            it.toCounts().size
        }
}
