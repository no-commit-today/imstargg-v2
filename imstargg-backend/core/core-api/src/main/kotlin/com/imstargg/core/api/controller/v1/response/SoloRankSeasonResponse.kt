package com.imstargg.core.api.controller.v1.response

import com.imstargg.core.enums.SoloRankSeason
import java.time.OffsetDateTime

data class SoloRankSeasonResponse(
    val key: String,
    val number: Int,
    val month: Int,
    val startAt: OffsetDateTime,
    val endAt: OffsetDateTime,
) {
    companion object {
        fun of(season: SoloRankSeason) =
            SoloRankSeasonResponse(
                key = season.key,
                number = season.number,
                month = season.month,
                startAt = season.startAt,
                endAt = season.endAt,
            )
    }
}
