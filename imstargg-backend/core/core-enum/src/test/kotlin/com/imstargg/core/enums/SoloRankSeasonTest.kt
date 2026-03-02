package com.imstargg.core.enums

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.OffsetDateTime

class SoloRankSeasonTest {
    @Nested
    inner class CreateFirst {
        @Test
        fun `첫 시즌을 생성한다`() {
            // when
            val result = SoloRankSeason.createFirst()

            // then
            assertThat(result.key).isEqualTo("1-1")
            assertThat(result.startAt).isEqualTo(OffsetDateTime.parse("2025-02-25T18:00:00+09:00"))
            assertThat(result.endAt).isEqualTo(OffsetDateTime.parse("2025-04-16T18:00:00+09:00"))
        }
    }

    @Nested
    inner class Next {
        @Test
        fun `다음 시즌을 생성한다`() {
            // given
            val sut = SoloRankSeason.createFirst()

            // when
            val season1m2 = sut.next()
            val season1m3 = season1m2.next()
            val season1m4 = season1m3.next()

            // then
            assertThat(season1m2.key).isEqualTo("1-2")
            assertThat(season1m2.startAt).isEqualTo(OffsetDateTime.parse("2025-04-17T18:00:00+09:00"))
            assertThat(season1m2.endAt).isEqualTo(OffsetDateTime.parse("2025-05-14T18:00:00+09:00"))

            assertThat(season1m3.key).isEqualTo("1-3")
            assertThat(season1m3.startAt).isEqualTo(OffsetDateTime.parse("2025-05-15T18:00:00+09:00"))
            assertThat(season1m3.endAt).isEqualTo(OffsetDateTime.parse("2025-06-18T18:00:00+09:00"))

            assertThat(season1m4.key).isEqualTo("1-4")
            assertThat(season1m4.startAt).isEqualTo(OffsetDateTime.parse("2025-06-19T18:00:00+09:00"))
            assertThat(season1m4.endAt).isEqualTo(OffsetDateTime.parse("2025-07-16T18:00:00+09:00"))
        }

        @Test
        fun `4개월 이후 다음 시즌 번호가 증가한다`() {
            // given
            var sut = SoloRankSeason.createFirst()
            repeat(4) { sut = sut.next() }

            // when & then
            assertThat(sut.key).isEqualTo("2-1")
        }
    }

    @Nested
    inner class `시즌 간 날짜 차이` {
        @Test
        fun `연속 시즌의 startAt은 이전 시즌의 endAt 다음 날이다`() {
            // given
            val season1m1 = SoloRankSeason.createFirst()

            // when
            val season1m2 = season1m1.next()

            // then
            assertThat(season1m2.startAt).isEqualTo(season1m1.endAt.plusDays(1))
        }

        @Test
        fun `시즌 1의 4개월 동안 날짜가 1일 간격으로 연속된다`() {
            // given
            var prev = SoloRankSeason.createFirst()

            // when & then
            repeat(3) {
                val next = prev.next()
                assertThat(next.startAt).isEqualTo(prev.endAt.plusDays(1))
                prev = next
            }
        }

        @Test
        fun `시즌 1에서 시즌 2까지 4개월 간격으로 날짜가 연속된다`() {
            // given
            var prev = SoloRankSeason.createFirst()

            // when & then
            repeat(4) {
                val next = prev.next()
                assertThat(next.startAt).isEqualTo(prev.endAt.plusDays(1))
                prev = next
            }
            assertThat(prev.key).isEqualTo("2-1")
        }
    }

    @Nested
    inner class IsLast {
        @Test
        fun `현재 시간이 endAt 이전이면 false를 반환한다`() {
            // given
            val sut = SoloRankSeason.createFirst()
            val clock = fixedClock("2025-04-16T17:59:59+09:00")

            // when
            val result = sut.isLast(clock)

            // then
            assertThat(result).isFalse()
        }

        @Test
        fun `현재 시간이 endAt과 같으면 false를 반환한다`() {
            // given
            val sut = SoloRankSeason.createFirst()
            val clock = fixedClock("2025-04-16T18:00:00+09:00")

            // when
            val result = sut.isLast(clock)

            // then
            assertThat(result).isFalse()
        }

        @Test
        fun `현재 시간이 endAt 이후이면 true를 반환한다`() {
            // given
            val sut = SoloRankSeason.createFirst()
            val clock = fixedClock("2025-04-16T18:00:01+09:00")

            // when
            val result = sut.isLast(clock)

            // then
            assertThat(result).isTrue()
        }
    }

    @Nested
    inner class Current {
        @Test
        fun `첫 시즌 기간 중에는 첫 시즌을 반환한다`() {
            // given
            val clock = fixedClock("2025-03-15T12:00:00+09:00")

            // when
            val result = SoloRankSeason.current(clock)

            // then
            assertThat(result.key).isEqualTo("1-1")
            assertThat(result.startAt).isEqualTo(OffsetDateTime.parse("2025-02-25T18:00:00+09:00"))
            assertThat(result.endAt).isEqualTo(OffsetDateTime.parse("2025-04-16T18:00:00+09:00"))
        }

        @Test
        fun `시즌 endAt 시점에는 해당 시즌을 반환한다`() {
            // given
            val clock = fixedClock("2025-04-16T18:00:00+09:00")

            // when
            val result = SoloRankSeason.current(clock)

            // then
            assertThat(result.key).isEqualTo("1-1")
            assertThat(result.startAt).isEqualTo(OffsetDateTime.parse("2025-02-25T18:00:00+09:00"))
            assertThat(result.endAt).isEqualTo(OffsetDateTime.parse("2025-04-16T18:00:00+09:00"))
        }

        @Test
        fun `시즌 endAt 이후에는 다음 시즌을 반환한다`() {
            // given
            val clock = fixedClock("2025-04-16T18:00:01+09:00")

            // when
            val result = SoloRankSeason.current(clock)

            // then
            assertThat(result.key).isEqualTo("1-2")
            assertThat(result.startAt).isEqualTo(OffsetDateTime.parse("2025-04-17T18:00:00+09:00"))
            assertThat(result.endAt).isEqualTo(OffsetDateTime.parse("2025-05-14T18:00:00+09:00"))
        }

        @Test
        fun `시즌 2의 1개월차 기간 중에는 해당 시즌을 반환한다`() {
            // given
            val clock = fixedClock("2025-08-01T12:00:00+09:00")

            // when
            val result = SoloRankSeason.current(clock)

            // then
            assertThat(result.key).isEqualTo("2-1")
            assertThat(result.startAt).isEqualTo(OffsetDateTime.parse("2025-07-17T18:00:00+09:00"))
            assertThat(result.endAt).isEqualTo(OffsetDateTime.parse("2025-08-20T18:00:00+09:00"))
        }
    }

    private fun fixedClock(offsetDateTime: String): Clock {
        val odt = OffsetDateTime.parse(offsetDateTime)
        return Clock.fixed(odt.toInstant(), odt.offset)
    }
}
