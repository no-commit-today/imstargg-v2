package com.imstargg.core.enums

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class StatsWeekRangeTest {
    @Nested
    inner class WeekRange {
        @Test
        fun `월요일은 start=월요일 end=일요일`() {
            // when
            val sut = StatsWeekRange.weekRange(LocalDate.of(2026, 5, 4))

            // then
            assertThat(sut.start).isEqualTo(LocalDate.of(2026, 5, 4))
            assertThat(sut.end).isEqualTo(LocalDate.of(2026, 5, 10))
        }

        @Test
        fun `주 중간 수요일은 start=월요일 end=일요일`() {
            // when
            val sut = StatsWeekRange.weekRange(LocalDate.of(2026, 5, 6))

            // then
            assertThat(sut.start).isEqualTo(LocalDate.of(2026, 5, 4))
            assertThat(sut.end).isEqualTo(LocalDate.of(2026, 5, 10))
        }

        @Test
        fun `일요일은 start=월요일 end=일요일`() {
            // when
            val sut = StatsWeekRange.weekRange(LocalDate.of(2026, 5, 10))

            // then
            assertThat(sut.start).isEqualTo(LocalDate.of(2026, 5, 4))
            assertThat(sut.end).isEqualTo(LocalDate.of(2026, 5, 10))
        }

        @Test
        fun `월초 금요일은 start이 월1일로 clip end는 해당 주 일요일`() {
            // when
            val sut = StatsWeekRange.weekRange(LocalDate.of(2026, 5, 1))

            // then
            assertThat(sut.start).isEqualTo(LocalDate.of(2026, 5, 1))
            assertThat(sut.end).isEqualTo(LocalDate.of(2026, 5, 3))
        }

        @Test
        fun `월말 목요일은 end이 월말`() {
            // when
            val sut = StatsWeekRange.weekRange(LocalDate.of(2026, 4, 30))

            // then
            assertThat(sut.start).isEqualTo(LocalDate.of(2026, 4, 27))
            assertThat(sut.end).isEqualTo(LocalDate.of(2026, 4, 30))
        }

        @Test
        fun `월 첫날이 일요일이면 start=end=그날`() {
            // given 2026-11-01 Sunday
            // when
            val sut = StatsWeekRange.weekRange(LocalDate.of(2026, 11, 1))

            // then
            assertThat(sut.start).isEqualTo(LocalDate.of(2026, 11, 1))
            assertThat(sut.end).isEqualTo(LocalDate.of(2026, 11, 1))
        }

        @Test
        fun `윤년 2월 29일`() {
            // given 2028-02-29 Tue (2028 leap year)
            // when
            val sut = StatsWeekRange.weekRange(LocalDate.of(2028, 2, 29))

            // then
            assertThat(sut.start).isEqualTo(LocalDate.of(2028, 2, 28))
            assertThat(sut.end).isEqualTo(LocalDate.of(2028, 2, 29))
        }
    }

    @Nested
    inner class `weekRange 가 같은 주 안의 어느 요일에서도 동일 결과를 반환` {
        @Test
        fun `월 경계가 없는 자연주 5월 첫째주 5_4 부터 5_10 까지 7일 모두 동일`() {
            val expected = WeekRange(LocalDate.of(2026, 5, 4), LocalDate.of(2026, 5, 10))
            (0L..6L).forEach { offset ->
                val date = LocalDate.of(2026, 5, 4).plusDays(offset)
                val sut = StatsWeekRange.weekRange(date)
                assertThat(sut).isEqualTo(expected)
            }
        }

        @Test
        fun `월말 경계 4_27 부터 4_30 까지 모두 동일하고 5_1 부터 5_3 까지 다른 weekRange`() {
            val expectedApr = WeekRange(LocalDate.of(2026, 4, 27), LocalDate.of(2026, 4, 30))
            val expectedMay = WeekRange(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 3))
            (0L..3L).forEach { offset ->
                val date = LocalDate.of(2026, 4, 27).plusDays(offset)
                assertThat(StatsWeekRange.weekRange(date)).isEqualTo(expectedApr)
            }
            (0L..2L).forEach { offset ->
                val date = LocalDate.of(2026, 5, 1).plusDays(offset)
                assertThat(StatsWeekRange.weekRange(date)).isEqualTo(expectedMay)
            }
        }
    }

    @Nested
    inner class CurrentAndPrevious {
        @Test
        fun `오늘과 어제의 weekRange 가 같으면 1개 반환`() {
            // 2026-04-29 (수). 어제 4-28 (화) 모두 같은 weekRange
            val sut = StatsWeekRange.currentAndPrevious(LocalDate.of(2026, 4, 29))
            assertThat(sut).hasSize(1)
        }

        @Test
        fun `월요일 (오늘과 어제의 weekRange 다름) 이면 2개 반환`() {
            // 2026-05-04 (월). 어제 5-3 (일) 은 5_4~5_10 이전 주
            val sut = StatsWeekRange.currentAndPrevious(LocalDate.of(2026, 5, 4))
            assertThat(sut).hasSize(2)
        }

        @Test
        fun `월초 첫날 (월말 경계로 weekRange 다름) 이면 2개 반환`() {
            // 2026-05-01 (금). 어제 4-30 은 4월 weekRange, 오늘은 5월 weekRange
            val sut = StatsWeekRange.currentAndPrevious(LocalDate.of(2026, 5, 1))
            assertThat(sut).hasSize(2)
        }

        @Test
        fun `주 중간 오늘은 현재 주 하나만 반환`() {
            // when
            val sut = StatsWeekRange.currentAndPrevious(LocalDate.of(2026, 5, 6))

            // then
            assertThat(sut).hasSize(1)
            assertThat(sut[0].start).isEqualTo(LocalDate.of(2026, 5, 4))
            assertThat(sut[0].end).isEqualTo(LocalDate.of(2026, 5, 10))
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
            assertThat(sut[1].end).isEqualTo(LocalDate.of(2026, 5, 10))
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
            assertThat(sut[1].end).isEqualTo(LocalDate.of(2026, 5, 3))
        }
    }
}
