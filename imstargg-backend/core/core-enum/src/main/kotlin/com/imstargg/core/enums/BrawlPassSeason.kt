package com.imstargg.core.enums

import java.time.Clock
import java.time.DayOfWeek
import java.time.OffsetDateTime
import java.time.temporal.TemporalAdjusters

class BrawlPassSeason(
    val number: Int,
    val startAt: OffsetDateTime,
    val endAt: OffsetDateTime,
) {
    fun next(): BrawlPassSeason {
        val nextStartAt = endAt
        val nextEndAt =
            endAt
                .plusMonths(1)
                .withDayOfMonth(1)
                .with(TemporalAdjusters.dayOfWeekInMonth(1, DayOfWeek.THURSDAY))
        return BrawlPassSeason(number + 1, nextStartAt, nextEndAt)
    }

    companion object {
        fun createFirst(): BrawlPassSeason =
            BrawlPassSeason(
                number = 1,
                startAt = OffsetDateTime.parse("2022-04-07T18:00:00+09:00"),
                endAt = OffsetDateTime.parse("2022-05-05T18:00:00+09:00"),
            )

        fun current(clock: Clock): BrawlPassSeason = of(OffsetDateTime.now(clock))

        fun of(time: OffsetDateTime): BrawlPassSeason {
            var season = createFirst()
            while (!time.isBefore(season.endAt)) {
                season = season.next()
            }
            return season
        }

        fun all(clock: Clock): List<BrawlPassSeason> {
            val end = current(clock)
            val list = mutableListOf<BrawlPassSeason>()
            var s = createFirst()
            while (s.number != end.number) {
                list += s
                s = s.next()
            }
            list += end
            return list
        }

        fun ofNumber(number: Int): BrawlPassSeason {
            require(number >= 1) { "number must be >= 1: $number" }
            var season = createFirst()
            while (season.number < number) {
                season = season.next()
            }
            return season
        }
    }
}
