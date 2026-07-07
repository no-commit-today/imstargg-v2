package com.imstargg.core.enums

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

object StatsWeekRange {
    fun weekRange(date: LocalDate): WeekRange {
        val monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val naturalSunday = monday.plusDays(6)
        val monthStart = date.withDayOfMonth(1)
        val monthEnd = date.with(TemporalAdjusters.lastDayOfMonth())
        val start = maxOf(monday, monthStart)
        val end = minOf(naturalSunday, monthEnd)
        return WeekRange(start, end)
    }

    fun currentAndPrevious(today: LocalDate): List<WeekRange> {
        val current = weekRange(today)
        val previous = weekRange(today.minusDays(1))
        return if (previous == current) listOf(current) else listOf(previous, current)
    }
}
