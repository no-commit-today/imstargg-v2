package com.imstargg.core.enums

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class BrawlerTrophyRangeTest {
    @Nested
    inner class ResolveAll {
        @Test
        fun `트로피가 500 미만이면 빈 리스트를 반환한다`() {
            // when
            val result = BrawlerTrophyRange.resolveAll(499)

            // then
            assertThat(result).isEmpty()
        }

        @Test
        fun `트로피가 500 이상이면 TROPHY_500_PLUS를 포함한다`() {
            // when
            val result = BrawlerTrophyRange.resolveAll(500)

            // then
            assertThat(result).containsExactly(BrawlerTrophyRange.TROPHY_500_PLUS)
        }

        @Test
        fun `트로피가 1000 이상이면 500과 1000을 포함한다`() {
            // when
            val result = BrawlerTrophyRange.resolveAll(1000)

            // then
            assertThat(result).containsExactly(
                BrawlerTrophyRange.TROPHY_500_PLUS,
                BrawlerTrophyRange.TROPHY_1000_PLUS,
            )
        }

        @Test
        fun `트로피가 2000 이상이면 500, 1000, 2000을 포함한다`() {
            // when
            val result = BrawlerTrophyRange.resolveAll(2000)

            // then
            assertThat(result).containsExactly(
                BrawlerTrophyRange.TROPHY_500_PLUS,
                BrawlerTrophyRange.TROPHY_1000_PLUS,
                BrawlerTrophyRange.TROPHY_2000_PLUS,
            )
        }

        @Test
        fun `트로피가 3000 이상이면 모든 구간을 포함한다`() {
            // when
            val result = BrawlerTrophyRange.resolveAll(3000)

            // then
            assertThat(result).containsExactly(
                BrawlerTrophyRange.TROPHY_500_PLUS,
                BrawlerTrophyRange.TROPHY_1000_PLUS,
                BrawlerTrophyRange.TROPHY_2000_PLUS,
                BrawlerTrophyRange.TROPHY_3000_PLUS,
            )
        }
    }
}
