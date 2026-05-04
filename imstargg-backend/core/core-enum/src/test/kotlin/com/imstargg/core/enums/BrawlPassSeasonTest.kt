package com.imstargg.core.enums

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.OffsetDateTime

class BrawlPassSeasonTest {
    @Nested
    inner class CreateFirst {
        @Test
        fun `첫 시즌을 생성한다`() {
            // when
            val result = BrawlPassSeason.createFirst()

            // then
            assertThat(result.number).isEqualTo(1)
            assertThat(result.startAt).isEqualTo(OffsetDateTime.parse("2022-04-07T18:00:00+09:00"))
            assertThat(result.endAt).isEqualTo(OffsetDateTime.parse("2022-05-05T18:00:00+09:00"))
        }

        @Test
        fun `시즌 1에서 next를 36번 호출하면 시즌 37이 된다`() {
            // given
            var season = BrawlPassSeason.createFirst()

            // when
            repeat(36) { season = season.next() }

            // then
            assertThat(season.number).isEqualTo(37)
            assertThat(season.startAt).isEqualTo(OffsetDateTime.parse("2025-04-03T18:00:00+09:00"))
            assertThat(season.endAt).isEqualTo(OffsetDateTime.parse("2025-05-01T18:00:00+09:00"))
        }
    }

    @Nested
    inner class Next {
        @Test
        fun `시즌 37의 다음 시즌을 생성한다`() {
            // given
            var sut = BrawlPassSeason.createFirst()
            repeat(36) { sut = sut.next() }

            // when
            val result = sut.next()

            // then
            assertThat(result.number).isEqualTo(38)
            assertThat(result.startAt).isEqualTo(OffsetDateTime.parse("2025-05-01T18:00:00+09:00"))
            assertThat(result.endAt).isEqualTo(OffsetDateTime.parse("2025-06-05T18:00:00+09:00"))
        }

        @Test
        fun `연속으로 다음 시즌을 생성한다`() {
            // given
            var season37 = BrawlPassSeason.createFirst()
            repeat(36) { season37 = season37.next() }

            // when
            val season38 = season37.next()
            val season39 = season38.next()
            val season40 = season39.next()

            // then
            assertThat(season39.number).isEqualTo(39)
            assertThat(season39.startAt).isEqualTo(OffsetDateTime.parse("2025-06-05T18:00:00+09:00"))
            assertThat(season39.endAt).isEqualTo(OffsetDateTime.parse("2025-07-03T18:00:00+09:00"))

            assertThat(season40.number).isEqualTo(40)
            assertThat(season40.startAt).isEqualTo(OffsetDateTime.parse("2025-07-03T18:00:00+09:00"))
            assertThat(season40.endAt).isEqualTo(OffsetDateTime.parse("2025-08-07T18:00:00+09:00"))
        }
    }

    @Nested
    inner class `시즌 간 날짜 차이` {
        @Test
        fun `연속 시즌의 startAt은 이전 시즌의 endAt과 같다`() {
            // given
            val season1 = BrawlPassSeason.createFirst()

            // when
            val season2 = season1.next()

            // then
            assertThat(season2.startAt).isEqualTo(season1.endAt)
        }

        @Test
        fun `시즌 1부터 시즌 37까지 36시즌 동안 날짜가 끊김 없이 연속된다`() {
            // given
            var prev = BrawlPassSeason.createFirst()

            // when & then
            repeat(36) {
                val next = prev.next()
                assertThat(next.number).isEqualTo(prev.number + 1)
                assertThat(next.startAt).isEqualTo(prev.endAt)
                prev = next
            }
            assertThat(prev.number).isEqualTo(37)
            assertThat(prev.startAt).isEqualTo(OffsetDateTime.parse("2025-04-03T18:00:00+09:00"))
        }
    }

    @Nested
    inner class Current {
        @Test
        fun `시즌 37 기간 중에는 시즌 37을 반환한다`() {
            // given
            val clock = fixedClock("2025-04-15T12:00:00+09:00")

            // when
            val result = BrawlPassSeason.current(clock)

            // then
            assertThat(result.number).isEqualTo(37)
            assertThat(result.startAt).isEqualTo(OffsetDateTime.parse("2025-04-03T18:00:00+09:00"))
            assertThat(result.endAt).isEqualTo(OffsetDateTime.parse("2025-05-01T18:00:00+09:00"))
        }

        @Test
        fun `시즌 endAt 시점에는 다음 시즌을 반환한다`() {
            // given
            val clock = fixedClock("2025-05-01T18:00:00+09:00")

            // when
            val result = BrawlPassSeason.current(clock)

            // then
            assertThat(result.number).isEqualTo(38)
            assertThat(result.startAt).isEqualTo(OffsetDateTime.parse("2025-05-01T18:00:00+09:00"))
            assertThat(result.endAt).isEqualTo(OffsetDateTime.parse("2025-06-05T18:00:00+09:00"))
        }

        @Test
        fun `시즌 38 기간 중에는 시즌 38을 반환한다`() {
            // given
            val clock = fixedClock("2025-05-15T12:00:00+09:00")

            // when
            val result = BrawlPassSeason.current(clock)

            // then
            assertThat(result.number).isEqualTo(38)
            assertThat(result.startAt).isEqualTo(OffsetDateTime.parse("2025-05-01T18:00:00+09:00"))
            assertThat(result.endAt).isEqualTo(OffsetDateTime.parse("2025-06-05T18:00:00+09:00"))
        }
    }

    @Nested
    inner class Of {
        @Test
        fun `시즌 1 기간 중의 시각은 시즌 1을 반환한다`() {
            // given
            val time = OffsetDateTime.parse("2022-04-15T12:00:00+09:00")

            // when
            val result = BrawlPassSeason.of(time)

            // then
            assertThat(result.number).isEqualTo(1)
        }

        @Test
        fun `시즌 endAt 시점은 다음 시즌을 반환한다`() {
            // given
            val time = OffsetDateTime.parse("2022-05-05T18:00:00+09:00")

            // when
            val result = BrawlPassSeason.of(time)

            // then
            assertThat(result.number).isEqualTo(2)
        }

        @Test
        fun `과거 시즌 시각은 그 시점의 시즌을 반환한다`() {
            // given
            val time = OffsetDateTime.parse("2025-04-15T12:00:00+09:00")

            // when
            val result = BrawlPassSeason.of(time)

            // then
            assertThat(result.number).isEqualTo(37)
        }
    }

    @Nested
    inner class OfNumber {
        @Test
        fun `번호 1을 주면 첫 시즌을 반환한다`() {
            // when
            val result = BrawlPassSeason.ofNumber(1)

            // then
            assertThat(result.number).isEqualTo(1)
            assertThat(result.startAt).isEqualTo(OffsetDateTime.parse("2022-04-07T18:00:00+09:00"))
        }

        @Test
        fun `번호에 해당하는 시즌을 반환한다`() {
            // when
            val result = BrawlPassSeason.ofNumber(37)

            // then
            assertThat(result.number).isEqualTo(37)
            assertThat(result.startAt).isEqualTo(OffsetDateTime.parse("2025-04-03T18:00:00+09:00"))
        }

        @Test
        fun `번호가 1 미만이면 IllegalArgumentException을 던진다`() {
            // when & then
            org.assertj.core.api.Assertions
                .assertThatThrownBy { BrawlPassSeason.ofNumber(0) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Nested
    inner class All {
        @Test
        fun `반환 리스트의 크기는 current 시즌 number 와 같다`() {
            val clock = fixedClock("2026-05-01T00:00:00+09:00")
            val all = BrawlPassSeason.all(clock)
            val current = BrawlPassSeason.current(clock)
            assertThat(all).hasSize(current.number)
            assertThat(all.last().number).isEqualTo(current.number)
            assertThat(all.first().number).isEqualTo(1)
        }
    }

    private fun fixedClock(offsetDateTime: String): Clock {
        val odt = OffsetDateTime.parse(offsetDateTime)
        return Clock.fixed(odt.toInstant(), odt.offset)
    }
}
