package com.imstargg.core.batch.job.brawlstars

import com.imstargg.core.enums.SoloRankSeason
import java.time.OffsetDateTime

class SoloRankSeasonCache {
    private val seasons: MutableList<SoloRankSeason> = mutableListOf()

    fun get(battleAt: OffsetDateTime): SoloRankSeason =
        seasons.find { !battleAt.isBefore(it.startAt) && !battleAt.isAfter(it.endAt) }
            ?: SoloRankSeason.of(battleAt).also { seasons.add(it) }
}
