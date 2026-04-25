package com.imstargg.core.enums

import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

enum class StatsDateRange {
    THIS_WEEK,
    SINCE_LAST_WEEK,
    LAST_WEEK,
    SINCE_TWO_WEEKS_AGO,
    TWO_WEEKS_AGO,
    THIS_MONTH,
    SINCE_LAST_MONTH,
    LAST_MONTH,
    ;

    fun dateRange(clock: Clock): Pair<LocalDate, LocalDate> {
        val today = LocalDate.now(clock)
        return when (this) {
            THIS_WEEK -> {
                val current = StatsWeekRange.weekRange(today)
                current.start to current.end
            }
            SINCE_LAST_WEEK -> {
                val lastMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(1)
                val current = StatsWeekRange.weekRange(today)
                lastMonday to current.end
            }
            LAST_WEEK -> {
                val lastMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(1)
                lastMonday to lastMonday.plusDays(6)
            }
            SINCE_TWO_WEEKS_AGO -> {
                val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(2)
                val current = StatsWeekRange.weekRange(today)
                monday to current.end
            }
            TWO_WEEKS_AGO -> {
                val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(2)
                monday to monday.plusDays(6)
            }
            THIS_MONTH -> today.withDayOfMonth(1) to today.with(TemporalAdjusters.lastDayOfMonth())
            SINCE_LAST_MONTH ->
                today.minusMonths(1).withDayOfMonth(1) to
                    today.with(TemporalAdjusters.lastDayOfMonth())
            LAST_MONTH -> {
                val lastMonth = today.minusMonths(1)
                lastMonth.withDayOfMonth(1) to lastMonth.with(TemporalAdjusters.lastDayOfMonth())
            }
        }
    }
}
