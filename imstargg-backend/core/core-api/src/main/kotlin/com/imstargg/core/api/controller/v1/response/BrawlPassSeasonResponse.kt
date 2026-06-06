package com.imstargg.core.api.controller.v1.response

import com.imstargg.core.enums.BrawlPassSeason
import java.time.Instant

data class BrawlPassSeasonResponse(
    val number: Int,
    val startAt: Instant,
    val endAt: Instant,
) {
    companion object {
        fun of(season: BrawlPassSeason) =
            BrawlPassSeasonResponse(
                number = season.number,
                startAt = season.startAt.toInstant(),
                endAt = season.endAt.toInstant(),
            )
    }
}
