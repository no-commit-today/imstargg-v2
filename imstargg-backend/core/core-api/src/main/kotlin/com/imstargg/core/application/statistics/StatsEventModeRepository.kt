package com.imstargg.core.application.statistics

import com.imstargg.core.config.CacheNames
import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.statistics.StatsEventModeTypes
import com.imstargg.infrastructure.db.core.statistics.repository.StatsEventModeJpaRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class StatsEventModeRepository(
    private val statsEventModeJpaRepository: StatsEventModeJpaRepository,
) {
    @Cacheable(cacheNames = [CacheNames.STATS_EVENT_MODE], key = "'all'")
    @Transactional(readOnly = true)
    fun findAll(): StatsEventModeTypes =
        StatsEventModeTypes(
            statsEventModeJpaRepository
                .findAll()
                .associate { BrawlStarsId(it.eventModeBrawlStarsId) to it.type },
        )
}
