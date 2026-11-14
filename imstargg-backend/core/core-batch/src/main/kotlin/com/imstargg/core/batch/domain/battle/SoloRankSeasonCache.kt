package com.imstargg.core.batch.domain.battle

import com.imstargg.core.enums.SoloRankSeason
import com.imstargg.infrastructure.db.core.KOREA_ZONE
import java.time.Instant
import java.time.OffsetDateTime

class SoloRankSeasonCache {
    private val seasons: MutableList<SoloRankSeason> = mutableListOf()

    /**
     * 시즌 사이 공백 구간의 시각은 어느 시즌의 [SoloRankSeason.startAt]~[SoloRankSeason.endAt] 에도 들지 않아
     * 매번 [SoloRankSeason.of] 로 다시 계산한다. 이때 같은 시즌을 다시 넣지 않아야 공백 구간 배틀이
     * 많은 날에도 캐시가 시즌 수만큼만 유지된다.
     */
    fun get(battleAt: Instant): SoloRankSeason {
        val battleTime = OffsetDateTime.ofInstant(battleAt, KOREA_ZONE)
        seasons.find { !battleTime.isBefore(it.startAt) && !battleTime.isAfter(it.endAt) }?.let { return it }

        val season = SoloRankSeason.of(battleTime)
        return seasons.find { it.key == season.key } ?: season.also { seasons.add(it) }
    }
}
