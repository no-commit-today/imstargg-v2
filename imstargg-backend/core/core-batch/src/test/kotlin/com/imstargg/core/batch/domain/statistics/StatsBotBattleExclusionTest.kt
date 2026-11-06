package com.imstargg.core.batch.domain.statistics

import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.battle.StatsResultBattleEntity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * 결과형 집계기 네 개가 같은 봇 전투 판정을 공유하는지 고정한다.
 * 순위형 집계기는 [BrawlerBattleRankStatsAggregatorTest] 가 맡는다.
 */
class StatsBotBattleExclusionTest {
    @Test
    fun `결과형 집계기 네 개가 같은 봇 전투를 모두 건너뛴다`() {
        assertThat(entityCounts(threeVsThree(listOf(1000, 1100, 1200), listOf(1050, 1080, 1090))))
            .containsExactly(0, 0, 0, 0)
    }

    @Test
    fun `상대 트로피를 창 밖으로 옮기면 네 개 모두 집계한다`() {
        // 1인 6행, 카운터 3×3×2 = 18행, 2인 3 + 3 = 6행, 3인 2행
        assertThat(entityCounts(threeVsThree(listOf(1000, 1100, 1200), listOf(1150, 1180, 1190))))
            .containsExactly(6, 18, 6, 2)
    }

    @Test
    fun `2000 이상 슬롯이 낀 봇 전투도 통째로 건너뛴다`() {
        // S = 4500 → 창은 1425 이상 1525 미만
        assertThat(entityCounts(threeVsThree(listOf(1000, 1000, 2500), listOf(1450, 1460, 1470))))
            .containsExactly(0, 0, 0, 0)

        val aggregator = BrawlerBattleResultStatsAggregator()
        aggregator.aggregate(threeVsThree(listOf(1000, 1000, 2500), listOf(1550, 1560, 1570)))
        assertThat(aggregator.toEntities().map { it.tierRange }).contains(StatsTierRange.TROPHY_2000_PLUS)
    }

    @Test
    fun `경쟁전 전투는 집계한다`() {
        assertThat(entityCounts(threeVsThree(listOf(19, 19, 19), listOf(19, 19, 19), type = "soloRanked")))
            .allMatch { it > 0 }
    }

    private fun entityCounts(battle: StatsResultBattleEntity): List<Int> =
        listOf(
            BrawlerBattleResultStatsAggregator(),
            BrawlerEnemyBattleResultStatsAggregator(START, END),
            BrawlerPairBattleResultStatsAggregator(START, END),
            BrawlerTrioBattleResultStatsAggregator(START, END),
        ).map {
            it.aggregate(battle)
            it.toEntities().size
        }

    companion object {
        private val START = LocalDate.parse("2026-10-26")
        private val END = LocalDate.parse("2026-10-31")
    }
}
