package com.imstargg.core.batch.job.brawlstars

import com.imstargg.core.enums.SoloRankSeason
import com.imstargg.infrastructure.db.core.KOREA_ZONE
import java.time.Instant
import java.time.OffsetDateTime

class SoloRankSeasonCache {
    private val seasons: MutableList<SoloRankSeason> = mutableListOf()

    fun get(battleAt: Instant): SoloRankSeason {
        val battleTime = OffsetDateTime.ofInstant(battleAt, KOREA_ZONE)
        return seasons.find { !battleTime.isBefore(it.startAt) && !battleTime.isAfter(it.endAt) }
            ?: SoloRankSeason.of(battleTime).also { seasons.add(it) }
    }
}
