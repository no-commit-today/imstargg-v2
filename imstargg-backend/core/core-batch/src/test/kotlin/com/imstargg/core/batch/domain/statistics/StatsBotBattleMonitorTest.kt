package com.imstargg.core.batch.domain.statistics

import com.imstargg.infrastructure.db.core.statistics.battle.StatsResultBattleEntity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class StatsBotBattleMonitorTest {
    private val monitor = StatsBotBattleMonitor()

    private fun bot(
        result: String = "victory",
        names: List<String> = listOf("Brawler", "Brawler", "Kim"),
    ) = threeVsThree(listOf(1000, 1100, 1200), listOf(1050, 1080, 1090), result = result, opponentNames = names)

    private fun real(result: String = "victory") = threeVsThree(listOf(1000, 1100, 1200), listOf(1150, 1180, 1190), result = result)

    private fun aboveLimit(result: String = "victory") = threeVsThree(listOf(2000, 2000, 1970), listOf(1950, 1960, 2000), result = result)

    private fun record(vararg battles: StatsResultBattleEntity) = battles.forEach { monitor.record(it) }

    private fun metric(name: String) = monitor.metrics().single { it.name == name }

    @Test
    fun `걸린 전투 승률은 무승부를 빼고 센다`() {
        record(bot("victory"), bot("victory"), bot("defeat"), bot("draw"))

        assertThat(monitor.targetCount).isEqualTo(4)
        assertThat(monitor.flaggedCount).isEqualTo(4)
        assertThat(metric("걸린 전투 승률")).extracting("count", "total").containsExactly(2L, 3L)
    }

    @Test
    fun `남은 전투 승률은 본인 1000~1999 이고 걸리지 않은 전투만 센다`() {
        // 뒤의 두 전투는 본인이 2100, 2000 이라 빠진다 (S = 4100, 4000 → 상대 1800대는 창 밖이라 걸리지 않음)
        record(
            real("victory"),
            real("defeat"),
            real("draw"),
            bot("victory"),
            threeVsThree(listOf(2100, 1000, 1000), listOf(1800, 1810, 1820)),
            threeVsThree(listOf(2000, 1000, 1000), listOf(1800, 1810, 1820)),
        )

        assertThat(metric("남은 전투 승률")).extracting("count", "total").containsExactly(1L, 2L)
        assertThat(metric("걸리는 비중")).extracting("count", "total").containsExactly(1L, 4L)
    }

    @Test
    fun `2000 이상 구간 승률은 창을 만족하고 상대에 2000 이상이 있는 전투만 센다`() {
        // 마지막 전투는 상대가 모두 2000 이상이지만 창(S = 5970 → 1915 이상 2015 미만) 밖이라 빠진다
        record(
            aboveLimit("victory"),
            aboveLimit("defeat"),
            aboveLimit("draw"),
            bot("victory"),
            real("victory"),
            threeVsThree(listOf(2000, 2000, 1970), listOf(2100, 2150, 2200)),
        )

        assertThat(metric("2000 이상 구간 승률")).extracting("count", "total").containsExactly(1L, 2L)
    }

    @Test
    fun `걸리는 비중은 본인 1000~1999 전투를 무승부까지 분모로 센다`() {
        // 마지막 전투는 봇 전투지만 본인이 2500 이라 분자·분모 모두에서 빠진다
        record(
            bot("victory"),
            bot("draw"),
            real("draw"),
            real("defeat"),
            threeVsThree(listOf(2500, 1000, 1000), listOf(1450, 1460, 1470)),
        )

        assertThat(monitor.flaggedCount).isEqualTo(3)
        assertThat(metric("걸리는 비중")).extracting("count", "total").containsExactly(2L, 4L)
    }

    @Test
    fun `기본 이름 비율은 걸린 전투의 상대 슬롯으로 센다`() {
        record(bot(names = listOf("Brawler", "Brawler", "Kim")), real())

        assertThat(metric("기본 이름 비율")).extracting("count", "total").containsExactly(2L, 3L)
    }

    @Test
    fun `result 가 알 수 없는 코드여도 던지지 않고 승률에는 넣지 않는다`() {
        record(bot("unknown"))

        assertThat(monitor.flaggedCount).isEqualTo(1)
        assertThat(metric("걸린 전투 승률").total).isZero()
        assertThat(metric("걸리는 비중")).extracting("count", "total").containsExactly(1L, 1L)
    }

    @Test
    fun `적용 범위 밖 전투는 어느 지표에도 세지 않는다`() {
        record(
            threeVsThree(listOf(19, 19, 19), listOf(19, 19, 19), type = "soloRanked"),
            threeVsThree(listOf(1000, 1100, 1200), listOf(1050, 1080, 1090), eventModeBrawlStarsId = 6),
            statsResultBattle(listOf(listOf(statsPlayer(1)), listOf(statsPlayer(2)))),
        )

        assertThat(monitor.targetCount).isZero()
        assertThat(monitor.metrics()).allMatch { it.total == 0L }
    }

    @Test
    fun `문턱과 최소 표본은 스펙 값이다`() {
        assertThat(monitor.metrics().map { it.name })
            .containsExactly("걸린 전투 승률", "남은 전투 승률", "2000 이상 구간 승률", "걸리는 비중", "기본 이름 비율")
        assertThat(metric("걸린 전투 승률"))
            .extracting("warnBelow", "warnAbove", "minSample")
            .containsExactly(0.90, null, 1000L)
        assertThat(metric("남은 전투 승률"))
            .extracting("warnBelow", "warnAbove", "minSample")
            .containsExactly(null, 0.55, 5000L)
        assertThat(metric("2000 이상 구간 승률"))
            .extracting("warnBelow", "warnAbove", "minSample")
            .containsExactly(null, 0.75, 300L)
        assertThat(metric("걸리는 비중")).extracting("warnBelow", "warnAbove").containsExactly(null, null)
        assertThat(metric("기본 이름 비율")).extracting("warnBelow", "warnAbove").containsExactly(null, null)
    }

    @Test
    fun `아무것도 기록하지 않아도 summary 와 metrics 가 예외 없이 나온다`() {
        assertThat(monitor.metrics()).noneMatch { it.isWarning }
        assertThat(monitor.summary()).startsWith("대상 0건, 걸린 전투 0건, 본인 1000~1999 0건 중 걸린 0건, 걸린 전투 승률 - (")
    }

    @Test
    fun `표본이 충분하고 문턱을 넘으면 그 지표만 경고다`() {
        repeat(300) { record(aboveLimit("victory")) }
        record(bot("defeat"))

        assertThat(monitor.metrics().filter { it.isWarning }.map { it.name }).containsExactly("2000 이상 구간 승률")
        assertThat(metric("걸린 전투 승률").isSampleInsufficient).isTrue()
    }
}
