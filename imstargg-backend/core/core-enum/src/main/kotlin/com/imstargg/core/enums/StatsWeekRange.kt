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

    /**
     * 주간 통계 잡이 재집계할 주간 범위(현재 주 + 직전일이 속한 주).
     * 최대 룩백은 기준일로부터 7일 — 원본 배틀 파티션 보존기간
     * (core-batch PartitionManageJobConfig.RETENTION_DAYS)이 이를 커버해야 한다.
     */
    fun currentAndPrevious(today: LocalDate): List<WeekRange> {
        val current = weekRange(today)
        val previous = weekRange(today.minusDays(1))
        return if (previous == current) listOf(current) else listOf(previous, current)
    }
}
