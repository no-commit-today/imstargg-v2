package com.imstargg.core.batch.domain.statistics

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class StatsBotMonitorMetricTest {
    @Test
    fun `total 이 0 이면 rate 는 null 이고 경고가 아니다`() {
        val metric = StatsBotMonitorMetric(name = "걸린 전투 승률", count = 0, total = 0, warnBelow = 0.90, minSample = 0)
        assertThat(metric.rate).isNull()
        assertThat(metric.isWarning).isFalse()
    }

    @Test
    fun `warnBelow 는 미만일 때만 경고다`() {
        assertThat(StatsBotMonitorMetric(name = "m", count = 899, total = 1000, warnBelow = 0.90, minSample = 1000).isWarning).isTrue()
        assertThat(StatsBotMonitorMetric(name = "m", count = 900, total = 1000, warnBelow = 0.90, minSample = 1000).isWarning).isFalse()
    }

    @Test
    fun `warnAbove 는 초과일 때만 경고다`() {
        assertThat(StatsBotMonitorMetric(name = "m", count = 2751, total = 5000, warnAbove = 0.55, minSample = 5000).isWarning).isTrue()
        assertThat(StatsBotMonitorMetric(name = "m", count = 2750, total = 5000, warnAbove = 0.55, minSample = 5000).isWarning).isFalse()
    }

    @Test
    fun `최소 표본 미만이면 경고가 아니고 표본 부족이다`() {
        val metric = StatsBotMonitorMetric(name = "m", count = 0, total = 999, warnBelow = 0.90, minSample = 1000)
        assertThat(metric.isWarning).isFalse()
        assertThat(metric.isSampleInsufficient).isTrue()
    }

    @Test
    fun `문턱이 없으면 경고도 표본 부족도 아니다`() {
        val metric = StatsBotMonitorMetric(name = "m", count = 1, total = 1)
        assertThat(metric.isWarning).isFalse()
        assertThat(metric.isSampleInsufficient).isFalse()
    }

    @Test
    fun `describe 는 값과 표본과 문턱을 한 줄로 적는다`() {
        assertThat(StatsBotMonitorMetric(name = "걸리는 비중", count = 1420, total = 5000).describe())
            .isEqualTo("걸리는 비중 28.4% (표본 5000건)")
        assertThat(StatsBotMonitorMetric(name = "걸린 전투 승률", count = 1088, total = 1234, warnBelow = 0.90, minSample = 1000).describe())
            .isEqualTo("걸린 전투 승률 88.2% (표본 1234건, 문턱 90.0% 미만)")
        assertThat(StatsBotMonitorMetric(name = "남은 전투 승률", count = 3360, total = 6000, warnAbove = 0.55, minSample = 5000).describe())
            .isEqualTo("남은 전투 승률 56.0% (표본 6000건, 문턱 55.0% 초과)")
        assertThat(StatsBotMonitorMetric(name = "걸린 전투 승률", count = 17, total = 20, warnBelow = 0.90, minSample = 1000).describe())
            .isEqualTo("걸린 전투 승률 85.0% (표본 20건, 문턱 90.0% 미만, 표본 부족)")
        assertThat(StatsBotMonitorMetric(name = "걸린 전투 승률", count = 0, total = 0, warnBelow = 0.90, minSample = 1000).describe())
            .isEqualTo("걸린 전투 승률 - (표본 0건, 문턱 90.0% 미만, 표본 부족)")
    }
}
