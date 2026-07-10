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
    inner class Update {
        @Test
        fun `전달된 상태와 시각들로 필드를 덮어쓴다`() {
            // given
            val sut = aRenewalEntity().build()
            val requestedAt = Instant.parse("2025-01-18T10:00:00Z")
            val executedAt = Instant.parse("2025-01-18T10:00:05Z")
            val completedAt = Instant.parse("2025-01-18T10:00:10Z")

            // when
            sut.update(
                status = RenewalStatus.EXECUTING,
                requestedAt = requestedAt,
                executedAt = executedAt,
                completedAt = completedAt,
            )

            // then
            assertThat(sut.status).isEqualTo(RenewalStatus.EXECUTING)
            assertThat(sut.requestedAt).isEqualTo(requestedAt)
            assertThat(sut.executedAt).isEqualTo(executedAt)
            assertThat(sut.completedAt).isEqualTo(completedAt)
        }

        @Test
        fun `null 시각을 전달하면 null 로 덮어쓴다`() {
            // given
            val sut = aRenewalEntity().build()

            // when
            sut.update(
                status = RenewalStatus.REQUESTED,
                requestedAt = Instant.parse("2025-01-18T10:00:00Z"),
                executedAt = null,
                completedAt = null,
            )

            // then
            assertThat(sut.executedAt).isNull()
            assertThat(sut.completedAt).isNull()
        }
    }

    @Nested
    inner class ToPending {
        @Test
        fun `REQUESTED 상태에서 호출하면 PENDING으로 변경된다`() {
            // given
            val sut = aRenewalEntity().build()

            // when
            sut.toPending()

            // then
            assertThat(sut.status).isEqualTo(RenewalStatus.PENDING)
        }

        @Test
        fun `PENDING 상태에서 호출하면 IllegalStateException이 발생한다`() {
            // given
            val sut = aRenewalEntity().build()
            sut.toPending()

            // when
            val exception =
                catchThrowableOfType(IllegalStateException::class.java) {
                    sut.toPending()
                }

            // then
            assertThat(exception.message).contains("Cannot transition to PENDING from PENDING")
        }
    }

    @Nested
    inner class ToExecuting {
        @Test
        fun `PENDING 상태에서 호출하면 EXECUTING으로 변경되고 executedAt이 설정된다`() {
            // given
            val sut = aRenewalEntity().build()
            sut.toPending()

            // when
            sut.toExecuting(fixedClock)

            // then
            assertThat(sut.status).isEqualTo(RenewalStatus.EXECUTING)
            assertThat(sut.executedAt).isNotNull
        }

        @Test
        fun `REQUESTED 상태에서 호출하면 IllegalStateException이 발생한다`() {
            // given
            val sut = aRenewalEntity().build()

            // when
            val exception =
                catchThrowableOfType(IllegalStateException::class.java) {
                    sut.toExecuting(fixedClock)
                }

            // then
            assertThat(exception.message).contains("Cannot transition to EXECUTING from REQUESTED")
        }

        @Test
        fun `COMPLETED 상태에서 호출하면 IllegalStateException이 발생한다`() {
            // given
            val sut = aRenewalEntity().build()
            sut.toPending()
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
        fun `FAILED 상태에서 호출하면 IllegalStateException이 발생한다`() {
            // given
            val sut = aRenewalEntity().build()
            sut.toPending()
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
            sut.toPending()
            sut.toExecuting(fixedClock)

            // when
            sut.toCompleted(fixedClock)

            // then
            assertThat(sut.status).isEqualTo(RenewalStatus.COMPLETED)
            assertThat(sut.completedAt).isNotNull
        }

        @Test
        fun `REQUESTED 상태에서 호출하면 IllegalStateException이 발생한다`() {
            // given
            val sut = aRenewalEntity().build()

            // when
            val exception =
                catchThrowableOfType(IllegalStateException::class.java) {
                    sut.toCompleted(fixedClock)
                }

            // then
            assertThat(exception.message).contains("Cannot transition to COMPLETED from REQUESTED")
        }
    }

    @Nested
    inner class ToNotFound {
        @Test
        fun `EXECUTING 상태에서 호출하면 NOT_FOUND로 변경되고 completedAt이 설정된다`() {
            // given
            val sut = aRenewalEntity().build()
            sut.toPending()
            sut.toExecuting(fixedClock)

            // when
            sut.toNotFound(fixedClock)

            // then
            assertThat(sut.status).isEqualTo(RenewalStatus.NOT_FOUND)
            assertThat(sut.completedAt).isNotNull
        }

        @Test
        fun `REQUESTED 상태에서 호출하면 IllegalStateException이 발생한다`() {
            // given
            val sut = aRenewalEntity().build()

            // when
            val exception =
                catchThrowableOfType(IllegalStateException::class.java) {
                    sut.toNotFound(fixedClock)
                }

            // then
            assertThat(exception.message).contains("Cannot transition to NOT_FOUND from REQUESTED")
        }
    }

    @Nested
    inner class ToFailed {
        @Test
        fun `EXECUTING 상태에서 호출하면 FAILED로 변경되고 completedAt이 설정된다`() {
            // given
            val sut = aRenewalEntity().build()
            sut.toPending()
            sut.toExecuting(fixedClock)

            // when
            sut.toFailed(fixedClock)

            // then
            assertThat(sut.status).isEqualTo(RenewalStatus.FAILED)
            assertThat(sut.completedAt).isNotNull
        }

        @Test
        fun `REQUESTED 상태에서 호출하면 FAILED로 변경된다`() {
            // given
            val sut = aRenewalEntity().build()

            // when
            sut.toFailed(fixedClock)

            // then
            assertThat(sut.status).isEqualTo(RenewalStatus.FAILED)
        }
    }
}
