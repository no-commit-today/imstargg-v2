package com.imstargg.core.application.brawlstars

import com.imstargg.core.config.CacheNames
import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.brawlstars.event.BattleEvent
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Repository

@Repository
class EventRepositoryWithCache(
    private val eventRepository: EventRepository,
) {
    @Cacheable(cacheNames = [CacheNames.EVENT], key = "#id.value")
    fun find(id: BrawlStarsId): BattleEvent? = eventRepository.find(id)
}
