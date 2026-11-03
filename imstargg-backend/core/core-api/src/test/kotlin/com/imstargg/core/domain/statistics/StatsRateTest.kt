package com.imstargg.core.domain.statistics

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class StatsRateTest {
    @Nested
    inner class ResultGameCount {
        @Test
        fun `결과형 슬롯 합을 6 으로 나눈 값이다`() {
            // given
            val resultSlotSum = 900L

            // when
            val gameCount = StatsRate.resultGameCount(resultSlotSum)

            // then
            assertThat(gameCount).isEqualTo(150)
        }

        @Test
        fun `나머지는 버린다`() {
            // given
            val resultSlotSum = 905L

            // when
            val gameCount = StatsRate.resultGameCount(resultSlotSum)

            // then
            assertThat(gameCount).isEqualTo(150)
        }

        @Test
        fun `0 이면 0 이다`() {
            // given
            val resultSlotSum = 0L

            // when
            val gameCount = StatsRate.resultGameCount(resultSlotSum)

            // then
            assertThat(gameCount).isEqualTo(0)
        }
    }
}
