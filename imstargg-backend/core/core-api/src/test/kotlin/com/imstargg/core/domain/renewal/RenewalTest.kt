package com.imstargg.core.domain.renewal

import com.imstargg.core.domain.renewal.RenewalFixture.Companion.aRenewal
import com.imstargg.core.enums.RenewalStatus
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant

class RenewalTest {
    private val baseTime = Instant.parse("2025-01-17T10:00:00Z")

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

    @Nested
    inner class Request {
        @Test
        fun `갱신 가능하면 REQUESTED 로 전이하고 requestedAt 갱신 및 executedAt·completedAt 을 초기화한다`() {
            // given - COMPLETED(canRenew=true) 이며 executedAt·completedAt 이 채워진 상태
            val sut =
                aRenewal()
                    .withStatus(RenewalStatus.COMPLETED)
                    .withRequestedAt(baseTime)
                    .withExecutedAt(baseTime.plusSeconds(1))
                    .withCompletedAt(baseTime.plusSeconds(2))
                    .build()
            val now = baseTime.plusSeconds(100)

            // when
            val result = sut.request(now)

            // then
            assertThat(result.status).isEqualTo(RenewalStatus.REQUESTED)
            assertThat(result.requestedAt).isEqualTo(now)
            assertThat(result.executedAt).isNull()
            assertThat(result.completedAt).isNull()
        }

        @Test
        fun `갱신 불가 상태에서 호출하면 IllegalStateException 이 발생한다`() {
            // given - REQUESTED 이며 아직 stale 하지 않음(canRenew=false)
            val sut =
                aRenewal()
                    .withStatus(RenewalStatus.REQUESTED)
                    .withRequestedAt(baseTime)
                    .build()

            // when
            val ex =
                catchThrowableOfType(IllegalStateException::class.java) {
                    sut.request(baseTime.plusSeconds(5))
                }

            // then
            assertThat(ex).isNotNull()
        }
    }
}
