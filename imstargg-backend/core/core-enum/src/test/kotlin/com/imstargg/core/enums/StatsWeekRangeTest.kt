package com.imstargg.core.enums

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class StatsWeekRangeTest {
    @Nested
    inner class CurrentWeek {
        @Test
        fun `월요일은 start=end=monday`() {
            // when
            val sut = StatsWeekRange.currentWeek(LocalDate.of(2026, 5, 4))

            // then
            assertThat(sut.start).isEqualTo(LocalDate.of(2026, 5, 4))
            assertThat(sut.end).isEqualTo(LocalDate.of(2026, 5, 4))
        }

        @Test
        fun `주 중간 수요일은 start=월요일 end=오늘`() {
            // when
            val sut = StatsWeekRange.currentWeek(LocalDate.of(2026, 5, 6))

            // then
            assertThat(sut.start).isEqualTo(LocalDate.of(2026, 5, 4))
            assertThat(sut.end).isEqualTo(LocalDate.of(2026, 5, 6))
        }

        @Test
        fun `일요일은 start=월요일 end=일요일`() {
            // when
            val sut = StatsWeekRange.currentWeek(LocalDate.of(2026, 5, 10))

            // then
            assertThat(sut.start).isEqualTo(LocalDate.of(2026, 5, 4))
            assertThat(sut.end).isEqualTo(LocalDate.of(2026, 5, 10))
        }

        @Test
        fun `월초 금요일은 start이 월1일로 clip`() {
            // when
            val sut = StatsWeekRange.currentWeek(LocalDate.of(2026, 5, 1))

            // then
            assertThat(sut.start).isEqualTo(LocalDate.of(2026, 5, 1))
            assertThat(sut.end).isEqualTo(LocalDate.of(2026, 5, 1))
        }

        @Test
        fun `월말 목요일은 end이 월말`() {
            // when
            val sut = StatsWeekRange.currentWeek(LocalDate.of(2026, 4, 30))

            // then
            assertThat(sut.start).isEqualTo(LocalDate.of(2026, 4, 27))
            assertThat(sut.end).isEqualTo(LocalDate.of(2026, 4, 30))
        }

        @Test
        fun `월 첫날이 일요일이면 start=end=그날`() {
            // given 2026-11-01 Sunday
            // when
            val sut = StatsWeekRange.currentWeek(LocalDate.of(2026, 11, 1))

            // then
            assertThat(sut.start).isEqualTo(LocalDate.of(2026, 11, 1))
            assertThat(sut.end).isEqualTo(LocalDate.of(2026, 11, 1))
        }

        @Test
        fun `윤년 2월 29일`() {
            // given 2028-02-29 Tue (2028 leap year)
            // when
            val sut = StatsWeekRange.currentWeek(LocalDate.of(2028, 2, 29))

            // then
            assertThat(sut.start).isEqualTo(LocalDate.of(2028, 2, 28))
            assertThat(sut.end).isEqualTo(LocalDate.of(2028, 2, 29))
        }
    }

    @Nested
    inner class FinalizedWeek {
        @Test
        fun `월말로 clip된 주는 end=월말`() {
            // when
            val sut = StatsWeekRange.finalizedWeek(LocalDate.of(2026, 4, 30))

            // then
            assertThat(sut.start).isEqualTo(LocalDate.of(2026, 4, 27))
            assertThat(sut.end).isEqualTo(LocalDate.of(2026, 4, 30))
        }

        @Test
        fun `정상 주는 end=일요일`() {
            // when
            val sut = StatsWeekRange.finalizedWeek(LocalDate.of(2026, 5, 6))

            // then
            assertThat(sut.start).isEqualTo(LocalDate.of(2026, 5, 4))
            assertThat(sut.end).isEqualTo(LocalDate.of(2026, 5, 10))
        }

        @Test
        fun `월초 clip된 주는 start=월1일 end=해당주 일요일`() {
            // when
            val sut = StatsWeekRange.finalizedWeek(LocalDate.of(2026, 5, 1))

            // then
            assertThat(sut.start).isEqualTo(LocalDate.of(2026, 5, 1))
            assertThat(sut.end).isEqualTo(LocalDate.of(2026, 5, 3))
        }
    }

    @Nested
    inner class CurrentAndPrevious {
        @Test
        fun `주 중간 오늘은 현재 주 하나만 반환`() {
            // when
            val sut = StatsWeekRange.currentAndPrevious(LocalDate.of(2026, 5, 6))

            // then
            assertThat(sut).hasSize(1)
            assertThat(sut[0].start).isEqualTo(LocalDate.of(2026, 5, 4))
            assertThat(sut[0].end).isEqualTo(LocalDate.of(2026, 5, 6))
        }

        @Test
        fun `월요일은 전 주 finalized도 포함`() {
            // when
            val sut = StatsWeekRange.currentAndPrevious(LocalDate.of(2026, 5, 4))

            // then
            assertThat(sut).hasSize(2)
            assertThat(sut[0].start).isEqualTo(LocalDate.of(2026, 5, 1))
            assertThat(sut[0].end).isEqualTo(LocalDate.of(2026, 5, 3))
            assertThat(sut[1].start).isEqualTo(LocalDate.of(2026, 5, 4))
            assertThat(sut[1].end).isEqualTo(LocalDate.of(2026, 5, 4))
        }

        @Test
        fun `월 경계 금요일은 전 월 partial 주도 포함`() {
            // when
            val sut = StatsWeekRange.currentAndPrevious(LocalDate.of(2026, 5, 1))

            // then
            assertThat(sut).hasSize(2)
            assertThat(sut[0].start).isEqualTo(LocalDate.of(2026, 4, 27))
            assertThat(sut[0].end).isEqualTo(LocalDate.of(2026, 4, 30))
            assertThat(sut[1].start).isEqualTo(LocalDate.of(2026, 5, 1))
            assertThat(sut[1].end).isEqualTo(LocalDate.of(2026, 5, 1))
        }
    }
}
