package com.imstargg.core.domain.renewal

import com.imstargg.core.domain.renewal.RenewalFixture.Companion.aRenewal
import com.imstargg.core.enums.RenewalStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.time.ZoneOffset

class RenewalTest {
    private val baseTime = OffsetDateTime.of(2025, 1, 17, 10, 0, 0, 0, ZoneOffset.UTC)

    @Nested
    inner class CanRenew {
        @Test
        fun `REQUESTED 상태에서 10초 이하면 갱신할 수 없다`() {
            // given
            val sut =
                aRenewal()
                    .withStatus(RenewalStatus.REQUESTED)
                    .withRequestedAt(baseTime)
                    .build()

            // when
            val result = sut.canRenew(baseTime.plusSeconds(10))

            // then
            assertThat(result).isFalse()
        }

        @Test
        fun `REQUESTED 상태에서 10초 초과하면 갱신할 수 있다`() {
            // given
            val sut =
                aRenewal()
                    .withStatus(RenewalStatus.REQUESTED)
                    .withRequestedAt(baseTime)
                    .build()

            // when
            val result = sut.canRenew(baseTime.plusSeconds(11))

            // then
            assertThat(result).isTrue()
        }

        @Test
        fun `PENDING 상태에서 10초 이하면 갱신할 수 없다`() {
            // given
            val sut =
                aRenewal()
                    .withStatus(RenewalStatus.PENDING)
                    .withRequestedAt(baseTime)
                    .build()

            // when
            val result = sut.canRenew(baseTime.plusSeconds(10))

            // then
            assertThat(result).isFalse()
        }

        @Test
        fun `PENDING 상태에서 10초 초과하면 갱신할 수 있다`() {
            // given
            val sut =
                aRenewal()
                    .withStatus(RenewalStatus.PENDING)
                    .withRequestedAt(baseTime)
                    .build()

            // when
            val result = sut.canRenew(baseTime.plusSeconds(11))

            // then
            assertThat(result).isTrue()
        }

        @Test
        fun `EXECUTING 상태에서 10초 이하면 갱신할 수 없다`() {
            // given
            val sut =
                aRenewal()
                    .withStatus(RenewalStatus.EXECUTING)
                    .withRequestedAt(baseTime)
                    .build()

            // when
            val result = sut.canRenew(baseTime.plusSeconds(10))

            // then
            assertThat(result).isFalse()
        }

        @Test
        fun `EXECUTING 상태에서 10초 초과하면 갱신할 수 있다`() {
            // given
            val sut =
                aRenewal()
                    .withStatus(RenewalStatus.EXECUTING)
                    .withRequestedAt(baseTime)
                    .build()

            // when
            val result = sut.canRenew(baseTime.plusSeconds(11))

            // then
            assertThat(result).isTrue()
        }

        @Test
        fun `COMPLETED 상태이면 갱신할 수 있다`() {
            // given
            val sut = aRenewal().withStatus(RenewalStatus.COMPLETED).build()

            // when
            val result = sut.canRenew(baseTime)

            // then
            assertThat(result).isTrue()
        }

        @Test
        fun `NOT_FOUND 상태이면 갱신할 수 있다`() {
            // given
            val sut = aRenewal().withStatus(RenewalStatus.NOT_FOUND).build()

            // when
            val result = sut.canRenew(baseTime)

            // then
            assertThat(result).isTrue()
        }

        @Test
        fun `FAILED 상태이면 갱신할 수 있다`() {
            // given
            val sut = aRenewal().withStatus(RenewalStatus.FAILED).build()

            // when
            val result = sut.canRenew(baseTime)

            // then
            assertThat(result).isTrue()
        }
    }
}
