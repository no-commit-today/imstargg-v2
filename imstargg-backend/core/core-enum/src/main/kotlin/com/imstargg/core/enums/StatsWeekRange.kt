package com.imstargg.core.enums

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

data class WeekRange(
    val start: LocalDate,
    val end: LocalDate,
)

object StatsWeekRange {
    fun currentWeek(date: LocalDate): WeekRange {
        val monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val naturalSunday = monday.plusDays(6)
        val monthStart = date.withDayOfMonth(1)
        val monthEnd = date.with(TemporalAdjusters.lastDayOfMonth())
        val start = maxOf(monday, monthStart)
        val end = minOf(naturalSunday, monthEnd, date)
        return WeekRange(start, end)
    }

    fun finalizedWeek(date: LocalDate): WeekRange {
        val monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val naturalSunday = monday.plusDays(6)
        val monthStart = date.withDayOfMonth(1)
        val monthEnd = date.with(TemporalAdjusters.lastDayOfMonth())
        val start = maxOf(monday, monthStart)
        val end = minOf(naturalSunday, monthEnd)
        return WeekRange(start, end)
    }

    fun currentAndPrevious(today: LocalDate): List<WeekRange> {
        val current = currentWeek(today)
        val yesterday = today.minusDays(1)
        val yesterdayStart = currentWeek(yesterday).start
        return if (yesterdayStart != current.start) {
            listOf(finalizedWeek(yesterday), current)
        } else {
            listOf(current)
        }
    }
}
