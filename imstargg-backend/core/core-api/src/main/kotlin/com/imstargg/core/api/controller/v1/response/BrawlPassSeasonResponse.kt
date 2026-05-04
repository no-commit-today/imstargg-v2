package com.imstargg.core.api.controller.v1.response

import com.imstargg.core.enums.BrawlPassSeason
import java.time.OffsetDateTime

data class BrawlPassSeasonResponse(
    val number: Int,
    val startAt: OffsetDateTime,
    val endAt: OffsetDateTime,
) {
    companion object {
        fun of(season: BrawlPassSeason) =
            BrawlPassSeasonResponse(
                number = season.number,
                startAt = season.startAt,
                endAt = season.endAt,
            )
    }
}
