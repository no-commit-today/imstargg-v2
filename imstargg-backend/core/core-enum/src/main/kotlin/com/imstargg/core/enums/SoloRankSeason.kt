package com.imstargg.core.enums

import java.time.Clock
import java.time.DayOfWeek
import java.time.OffsetDateTime
import java.time.temporal.TemporalAdjusters

class SoloRankSeason private constructor(
    private val number: Int,
    private val month: Int,
    val startAt: OffsetDateTime,
    val endAt: OffsetDateTime,
) {
    val key: String = "$number-$month"

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

        fun of(time: OffsetDateTime): SoloRankSeason {
            val first = createFirst()
            require(!time.isBefore(first.startAt)) {
                "time must not be before the first season start: ${first.startAt}"
            }
            var season = first
            while (time.isAfter(season.endAt)) {
                season = season.next()
            }
            return season
        }

        fun current(clock: Clock): SoloRankSeason = of(OffsetDateTime.now(clock))

        fun ofKey(key: String): SoloRankSeason {
            val parts = key.split("-")
            require(parts.size == 2) { "Invalid SoloRankSeason key: $key" }
            val number =
                parts[0].toIntOrNull()?.takeIf { it >= 1 }
                    ?: throw IllegalArgumentException("Invalid SoloRankSeason key: $key")
            val month =
                parts[1].toIntOrNull()?.takeIf { it in 1..SEASON_MAX_MONTH }
                    ?: throw IllegalArgumentException("Invalid SoloRankSeason key: $key")
            val target = "$number-$month"
            var season = createFirst()
            while (season.key != target) {
                season = season.next()
            }
            return season
        }
    }
}
