package com.imstargg.infrastructure.db.core.renewal

import com.imstargg.core.enums.RenewalStatus
import com.imstargg.infrastructure.db.core.test.fixture.RenewalEntityFixture.Companion.aRenewalEntity
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class RenewalEntityTest {
    private val fixedClock = Clock.fixed(Instant.parse("2025-01-17T10:00:00Z"), ZoneId.of("UTC"))

    @Nested
    inner class ToExecuting {
        @Test
        fun `PENDING 상태에서 호출하면 EXECUTING으로 변경되고 executedAt이 설정된다`() {
            // given
            val sut = aRenewalEntity().build()

            // when
            sut.toExecuting(fixedClock)

            // then
            assertThat(sut.status).isEqualTo(RenewalStatus.EXECUTING)
            assertThat(sut.executedAt).isNotNull
        }

        @Test
        fun `EXECUTING 상태에서 호출하면 EXECUTING을 유지하고 executedAt은 변경되지 않는다`() {
            // given
            val sut = aRenewalEntity().build()
            sut.toExecuting(fixedClock)
            val originalExecutedAt = sut.executedAt

            val laterClock = Clock.fixed(Instant.parse("2025-01-17T11:00:00Z"), ZoneId.of("UTC"))

            // when
            sut.toExecuting(laterClock)

            // then
            assertThat(sut.status).isEqualTo(RenewalStatus.EXECUTING)
            assertThat(sut.executedAt).isEqualTo(originalExecutedAt)
        }

        @Test
        fun `COMPLETED 상태에서 호출하면 IllegalStateException이 발생한다`() {
            // given
            val sut = aRenewalEntity().build()
            sut.toExecuting(fixedClock)
            sut.toCompleted(fixedClock)

            // when
            val exception =
                catchThrowableOfType(IllegalStateException::class.java) {
                    sut.toExecuting(fixedClock)
                }

            // then
            assertThat(exception.message).contains("Cannot transition to EXECUTING from COMPLETED")
        }

        @Test
        fun `NOT_FOUND 상태에서 호출하면 IllegalStateException이 발생한다`() {
            // given
            val sut = aRenewalEntity().build()
            sut.toExecuting(fixedClock)
            sut.toNotFound(fixedClock)

            // when
            val exception =
                catchThrowableOfType(IllegalStateException::class.java) {
                    sut.toExecuting(fixedClock)
                }

            // then
            assertThat(exception.message).contains("Cannot transition to EXECUTING from NOT_FOUND")
        }

        @Test
        fun `FAILED 상태에서 호출하면 IllegalStateException이 발생한다`() {
            // given
            val sut = aRenewalEntity().build()
            sut.toExecuting(fixedClock)
            sut.toFailed(fixedClock)

            // when
            val exception =
                catchThrowableOfType(IllegalStateException::class.java) {
                    sut.toExecuting(fixedClock)
                }

            // then
            assertThat(exception.message).contains("Cannot transition to EXECUTING from FAILED")
        }
    }

    @Nested
    inner class ToCompleted {
        @Test
        fun `EXECUTING 상태에서 호출하면 COMPLETED로 변경되고 completedAt이 설정된다`() {
            // given
            val sut = aRenewalEntity().build()
            sut.toExecuting(fixedClock)

            // when
            sut.toCompleted(fixedClock)

            // then
            assertThat(sut.status).isEqualTo(RenewalStatus.COMPLETED)
            assertThat(sut.completedAt).isNotNull
        }

        @Test
        fun `PENDING 상태에서 호출하면 IllegalStateException이 발생한다`() {
            // given
            val sut = aRenewalEntity().build()

            // when
            val exception =
                catchThrowableOfType(IllegalStateException::class.java) {
                    sut.toCompleted(fixedClock)
                }

            // then
            assertThat(exception.message).contains("Cannot transition to COMPLETED from PENDING")
        }
    }

    @Nested
    inner class ToNotFound {
        @Test
        fun `EXECUTING 상태에서 호출하면 NOT_FOUND로 변경되고 completedAt이 설정된다`() {
            // given
            val sut = aRenewalEntity().build()
            sut.toExecuting(fixedClock)

            // when
            sut.toNotFound(fixedClock)

            // then
            assertThat(sut.status).isEqualTo(RenewalStatus.NOT_FOUND)
            assertThat(sut.completedAt).isNotNull
        }

        @Test
        fun `PENDING 상태에서 호출하면 IllegalStateException이 발생한다`() {
            // given
            val sut = aRenewalEntity().build()

            // when
            val exception =
                catchThrowableOfType(IllegalStateException::class.java) {
                    sut.toNotFound(fixedClock)
                }

            // then
            assertThat(exception.message).contains("Cannot transition to NOT_FOUND from PENDING")
        }
    }

    @Nested
    inner class ToFailed {
        @Test
        fun `EXECUTING 상태에서 호출하면 FAILED로 변경되고 completedAt이 설정된다`() {
            // given
            val sut = aRenewalEntity().build()
            sut.toExecuting(fixedClock)

            // when
            sut.toFailed(fixedClock)

            // then
            assertThat(sut.status).isEqualTo(RenewalStatus.FAILED)
            assertThat(sut.completedAt).isNotNull
        }

        @Test
        fun `PENDING 상태에서 호출하면 IllegalStateException이 발생한다`() {
            // given
            val sut = aRenewalEntity().build()

            // when
            val exception =
                catchThrowableOfType(IllegalStateException::class.java) {
                    sut.toFailed(fixedClock)
                }

            // then
            assertThat(exception.message).contains("Cannot transition to FAILED from PENDING")
        }
    }
}
