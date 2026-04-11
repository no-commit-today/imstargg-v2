package com.imstargg.core.enums

import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

enum class StatsDateRange {
    TODAY,
    RECENT_3_DAYS,
    THIS_WEEK,
    SINCE_LAST_WEEK,
    LAST_WEEK,
    SINCE_TWO_WEEKS_AGO,
    TWO_WEEKS_AGO,
    THIS_MONTH,
    SINCE_LAST_MONTH,
    LAST_MONTH,
    SINCE_TWO_MONTHS_AGO,
    TWO_MONTHS_AGO,
    ;

    fun dateRange(clock: Clock): Pair<LocalDate, LocalDate> {
        val today = LocalDate.now(clock)
        return when (this) {
            TODAY -> today to today
            RECENT_3_DAYS -> today.minusDays(2) to today
            THIS_WEEK -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) to today
            SINCE_LAST_WEEK -> {
                val lastMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(1)
                lastMonday to today
            }
            LAST_WEEK -> {
                val lastMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(1)
                lastMonday to lastMonday.plusDays(6)
            }
            SINCE_TWO_WEEKS_AGO -> {
                val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(2)
                monday to today
            }
            TWO_WEEKS_AGO -> {
                val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(2)
                monday to monday.plusDays(6)
            }
            THIS_MONTH -> today.withDayOfMonth(1) to today
            SINCE_LAST_MONTH -> today.minusMonths(1).withDayOfMonth(1) to today
            LAST_MONTH -> {
                val lastMonth = today.minusMonths(1)
                lastMonth.withDayOfMonth(1) to lastMonth.withDayOfMonth(lastMonth.lengthOfMonth())
            }
            SINCE_TWO_MONTHS_AGO -> today.minusMonths(2).withDayOfMonth(1) to today
            TWO_MONTHS_AGO -> {
                val twoMonthsAgo = today.minusMonths(2)
                twoMonthsAgo.withDayOfMonth(1) to twoMonthsAgo.withDayOfMonth(twoMonthsAgo.lengthOfMonth())
            }
        }
    }
}
