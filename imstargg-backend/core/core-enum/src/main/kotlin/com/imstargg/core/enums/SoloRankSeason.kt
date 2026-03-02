package com.imstargg.core.enums

import java.time.Clock
import java.time.DayOfWeek
import java.time.OffsetDateTime
import java.time.temporal.TemporalAdjusters

class SoloRankSeason(
    val number: Int,
    val month: Int,
    val startAt: OffsetDateTime,
    val endAt: OffsetDateTime,
) {
    fun next(): SoloRankSeason {
        val nextMonth = if (month < SEASON_MAX_MONTH) month + 1 else 1
        val nextNumber = if (nextMonth == 1) number + 1 else number
        val nextMonthThirdThursday =
            endAt
                .plusMonths(1)
                .withDayOfMonth(1)
                .with(TemporalAdjusters.dayOfWeekInMonth(3, DayOfWeek.THURSDAY))
        val nextStartAt = endAt.plusDays(1)
        val nextEndAt = nextMonthThirdThursday.minusDays(1)
        return SoloRankSeason(nextNumber, nextMonth, nextStartAt, nextEndAt)
    }

    fun isLast(clock: Clock): Boolean {
        val now = OffsetDateTime.now(clock)
        return now.isAfter(endAt)
    }

    companion object {
        private const val SEASON_MAX_MONTH = 4

        fun createFirst(): SoloRankSeason =
            SoloRankSeason(
                number = 1,
                month = 1,
                startAt = OffsetDateTime.parse("2025-02-25T18:00:00+09:00"),
                endAt = OffsetDateTime.parse("2025-04-16T18:00:00+09:00"),
            )

        fun current(clock: Clock): SoloRankSeason {
            val now = OffsetDateTime.now(clock)
            var season = createFirst()
            while (now.isAfter(season.endAt)) {
                season = season.next()
            }
            return season
        }
    }
}
