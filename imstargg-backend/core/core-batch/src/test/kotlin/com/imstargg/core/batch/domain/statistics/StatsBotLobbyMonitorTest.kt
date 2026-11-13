package com.imstargg.core.batch.domain.statistics

import com.imstargg.core.enums.BattleEventModeResultType
import com.imstargg.core.enums.StatsEventModeType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class StatsBotLobbyMonitorTest {
    private val monitor = StatsBotLobbyMonitor()

    private fun botDuo(rank: Int) =
        showdown(
            StatsEventModeType.DUO_SHOWDOWN,
            listOf(listOf(1500, 1500), listOf(1000, 1010), listOf(1020, 1030), listOf(1400, 1410), listOf(1600, 1610)),
            battleRank = rank,
        )

    private fun realDuo(rank: Int) =
        showdown(
            StatsEventModeType.DUO_SHOWDOWN,
            listOf(listOf(1500, 1500), listOf(1000, 1010), listOf(1200, 1210), listOf(1400, 1410), listOf(1600, 1610)),
            battleRank = rank,
        )

    private fun botSolo(rank: Int) =
        showdown(
            StatsEventModeType.SOLO_SHOWDOWN,
            listOf(listOf(1500)) + listOf(1000, 1010, 1020, 1030, 1040, 1200, 1400, 1600, 1800).map { listOf(it) },
            battleRank = rank,
        )

    private fun metric(name: String) = monitor.metrics().single { it.name == name }

    @Test
    fun `걸린 전투와 남은 전투의 1위 비율과 걸리는 비중을 모드별로 센다`() {
        listOf(botDuo(1), botDuo(2), realDuo(1), realDuo(3), realDuo(4), botSolo(1)).forEach { monitor.record(it) }

        assertThat(metric("듀오 걸린 전투 1위 비율")).extracting("count", "total").containsExactly(1L, 2L)
        assertThat(metric("듀오 남은 전투 1위 비율")).extracting("count", "total").containsExactly(1L, 3L)
        assertThat(metric("듀오 걸리는 비중")).extracting("count", "total").containsExactly(2L, 5L)
        assertThat(metric("솔로 걸린 전투 1위 비율")).extracting("count", "total").containsExactly(1L, 1L)
        assertThat(metric("트리오 걸리는 비중").total).isZero()
    }

    @Test
    fun `적용 범위 밖 전투는 세지 않는다`() {
        monitor.record(
            showdown(
                StatsEventModeType.DUO_SHOWDOWN,
                botDuo(1).teams.map { team -> team.map { it.brawler.trophies } },
                type = "soloRanked",
            ),
        )
        monitor.record(showdown(StatsEventModeType.SOLO_SHOWDOWN, listOf(listOf(1500)) + (0..7).map { listOf(1000 + it) }))

        assertThat(monitor.metrics()).allMatch { it.total == 0L }
    }

    @Test
    fun `지표는 솔로 듀오 트리오 순으로 모드마다 세 개이고 문턱은 스펙 값이다`() {
        assertThat(monitor.metrics().map { it.name }).containsExactly(
            "솔로 걸린 전투 1위 비율",
            "솔로 남은 전투 1위 비율",
            "솔로 걸리는 비중",
            "듀오 걸린 전투 1위 비율",
            "듀오 남은 전투 1위 비율",
            "듀오 걸리는 비중",
            "트리오 걸린 전투 1위 비율",
            "트리오 남은 전투 1위 비율",
            "트리오 걸리는 비중",
        )
        assertThat(metric("솔로 걸린 전투 1위 비율")).extracting("warnBelow", "minSample").containsExactly(0.50, 1000L)
        assertThat(metric("듀오 걸린 전투 1위 비율")).extracting("warnBelow", "minSample").containsExactly(0.70, 500L)
        assertThat(metric("트리오 걸린 전투 1위 비율")).extracting("warnBelow", "minSample").containsExactly(0.70, 300L)
        assertThat(metric("솔로 남은 전투 1위 비율")).extracting("warnAbove", "minSample").containsExactly(0.125, 3000L)
        assertThat(metric("듀오 남은 전투 1위 비율")).extracting("warnAbove", "minSample").containsExactly(0.24, 1000L)
        assertThat(metric("트리오 남은 전투 1위 비율")).extracting("warnAbove", "minSample").containsExactly(0.31, 1000L)
        assertThat(metric("솔로 걸리는 비중")).extracting("warnBelow", "warnAbove").containsExactly(null, null)
    }

    @Test
    fun `순위형 유형은 모두 지표에 세진다`() {
        // 새 순위형 유형을 SPECS 에 넣지 않거나 다른 유형으로 잘못 넣으면 그 유형의 전투는 record 에서 조용히 버려진다.
        // 정책의 판정 범위에서 빠진 유형도 여기서 함께 걸린다
        val uncounted =
            StatsEventModeType.entries
                .filter { it.resultType == BattleEventModeResultType.RANK }
                .filter { type ->
                    val sut = StatsBotLobbyMonitor()
                    sut.record(showdown(type, List(type.teamCount) { List(type.teamSize) { 1000 } }))
                    sut.metrics().none { it.total > 0 }
                }

        assertThat(uncounted).describedAs("감시 지표에 세지지 않는 순위형 유형").isEmpty()
    }

    @Test
    fun `아무것도 기록하지 않아도 summary 와 metrics 가 예외 없이 나온다`() {
        assertThat(monitor.metrics()).noneMatch { it.isWarning }
        assertThat(monitor.summary()).startsWith("솔로 걸린 전투 1위 비율 - (")
    }
}
