package com.imstargg.core.enums

import java.time.LocalDate
import java.time.OffsetDateTime

enum class StatsPeriodType {
    WEEKLY,
    MONTHLY,
    ;

    fun dateOf(startAt: OffsetDateTime): LocalDate =
        when (this) {
            WEEKLY -> startAt.toLocalDate()
            MONTHLY -> startAt.toLocalDate().withDayOfMonth(1)
        }

    fun endAt(startAt: OffsetDateTime): OffsetDateTime =
        when (this) {
            WEEKLY -> startAt.plusWeeks(1)
            MONTHLY -> startAt.plusMonths(1)
        }
}
