package com.imstargg.core.enums

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

class StatsDateRangeTest {
    private val zone = ZoneId.of("Asia/Seoul")

    private fun clockOf(date: String): Clock =
        Clock.fixed(
            LocalDate.parse(date).atStartOfDay(zone).toInstant(),
            zone,
        )

    // 2026-04-15 수요일 기준
    private val wednesdayClock = clockOf("2026-04-15")

    // 2026-04-13 월요일 기준
    private val mondayClock = clockOf("2026-04-13")

    @Nested
    inner class Today {
        @Test
        fun `오늘 날짜만 반환한다`() {
            // when
            val (startDate, endDate) = StatsDateRange.TODAY.dateRange(wednesdayClock)

            // then
            assertThat(startDate).isEqualTo(LocalDate.of(2026, 4, 15))
            assertThat(endDate).isEqualTo(LocalDate.of(2026, 4, 15))
        }
    }

    @Nested
    inner class Recent3Days {
        @Test
        fun `오늘 포함 3일 반환한다`() {
            // when
            val (startDate, endDate) = StatsDateRange.RECENT_3_DAYS.dateRange(wednesdayClock)

            // then
            assertThat(startDate).isEqualTo(LocalDate.of(2026, 4, 13))
            assertThat(endDate).isEqualTo(LocalDate.of(2026, 4, 15))
        }
    }

    @Nested
    inner class ThisWeek {
        @Test
        fun `이번주 월요일부터 오늘까지 반환한다`() {
            // when
            val (startDate, endDate) = StatsDateRange.THIS_WEEK.dateRange(wednesdayClock)

            // then
            assertThat(startDate).isEqualTo(LocalDate.of(2026, 4, 13))
            assertThat(endDate).isEqualTo(LocalDate.of(2026, 4, 15))
        }

        @Test
        fun `월요일이면 월요일부터 월요일까지 반환한다`() {
            // when
            val (startDate, endDate) = StatsDateRange.THIS_WEEK.dateRange(mondayClock)

            // then
            assertThat(startDate).isEqualTo(LocalDate.of(2026, 4, 13))
            assertThat(endDate).isEqualTo(LocalDate.of(2026, 4, 13))
        }
    }

    @Nested
    inner class SinceLastWeek {
        @Test
        fun `지난주 월요일부터 오늘까지 반환한다`() {
            // when
            val (startDate, endDate) = StatsDateRange.SINCE_LAST_WEEK.dateRange(wednesdayClock)

            // then
            assertThat(startDate).isEqualTo(LocalDate.of(2026, 4, 6))
            assertThat(endDate).isEqualTo(LocalDate.of(2026, 4, 15))
        }
    }

    @Nested
    inner class LastWeek {
        @Test
        fun `지난주 월요일부터 일요일까지 반환한다`() {
            // when
            val (startDate, endDate) = StatsDateRange.LAST_WEEK.dateRange(wednesdayClock)

            // then
            assertThat(startDate).isEqualTo(LocalDate.of(2026, 4, 6))
            assertThat(endDate).isEqualTo(LocalDate.of(2026, 4, 12))
        }
    }

    @Nested
    inner class SinceTwoWeeksAgo {
        @Test
        fun `지지난주 월요일부터 오늘까지 반환한다`() {
            // when
            val (startDate, endDate) = StatsDateRange.SINCE_TWO_WEEKS_AGO.dateRange(wednesdayClock)

            // then
            assertThat(startDate).isEqualTo(LocalDate.of(2026, 3, 30))
            assertThat(endDate).isEqualTo(LocalDate.of(2026, 4, 15))
        }
    }

    @Nested
    inner class TwoWeeksAgo {
        @Test
        fun `지지난주 월요일부터 일요일까지 반환한다`() {
            // when
            val (startDate, endDate) = StatsDateRange.TWO_WEEKS_AGO.dateRange(wednesdayClock)

            // then
            assertThat(startDate).isEqualTo(LocalDate.of(2026, 3, 30))
            assertThat(endDate).isEqualTo(LocalDate.of(2026, 4, 5))
        }
    }

    @Nested
    inner class ThisMonth {
        @Test
        fun `이번달 1일부터 오늘까지 반환한다`() {
            // when
            val (startDate, endDate) = StatsDateRange.THIS_MONTH.dateRange(wednesdayClock)

            // then
            assertThat(startDate).isEqualTo(LocalDate.of(2026, 4, 1))
            assertThat(endDate).isEqualTo(LocalDate.of(2026, 4, 15))
        }
    }

    @Nested
    inner class SinceLastMonth {
        @Test
        fun `지난달 1일부터 오늘까지 반환한다`() {
            // when
            val (startDate, endDate) = StatsDateRange.SINCE_LAST_MONTH.dateRange(wednesdayClock)

            // then
            assertThat(startDate).isEqualTo(LocalDate.of(2026, 3, 1))
            assertThat(endDate).isEqualTo(LocalDate.of(2026, 4, 15))
        }
    }

    @Nested
    inner class LastMonth {
        @Test
        fun `지난달 1일부터 마지막날까지 반환한다`() {
            // when
            val (startDate, endDate) = StatsDateRange.LAST_MONTH.dateRange(wednesdayClock)

            // then
            assertThat(startDate).isEqualTo(LocalDate.of(2026, 3, 1))
            assertThat(endDate).isEqualTo(LocalDate.of(2026, 3, 31))
        }

        @Test
        fun `2월이 지난달이면 28일까지 반환한다`() {
            // given
            val marchClock = clockOf("2026-03-15")

            // when
            val (startDate, endDate) = StatsDateRange.LAST_MONTH.dateRange(marchClock)

            // then
            assertThat(startDate).isEqualTo(LocalDate.of(2026, 2, 1))
            assertThat(endDate).isEqualTo(LocalDate.of(2026, 2, 28))
        }
    }

    @Nested
    inner class SinceTwoMonthsAgo {
        @Test
        fun `지지난달 1일부터 오늘까지 반환한다`() {
            // when
            val (startDate, endDate) = StatsDateRange.SINCE_TWO_MONTHS_AGO.dateRange(wednesdayClock)

            // then
            assertThat(startDate).isEqualTo(LocalDate.of(2026, 2, 1))
            assertThat(endDate).isEqualTo(LocalDate.of(2026, 4, 15))
        }
    }

    @Nested
    inner class TwoMonthsAgo {
        @Test
        fun `지지난달 1일부터 마지막날까지 반환한다`() {
            // when
            val (startDate, endDate) = StatsDateRange.TWO_MONTHS_AGO.dateRange(wednesdayClock)

            // then
            assertThat(startDate).isEqualTo(LocalDate.of(2026, 2, 1))
            assertThat(endDate).isEqualTo(LocalDate.of(2026, 2, 28))
        }
    }
}
