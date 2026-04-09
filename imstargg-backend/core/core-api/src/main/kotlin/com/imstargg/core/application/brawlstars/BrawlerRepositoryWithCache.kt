package com.imstargg.core.application.brawlstars

import com.imstargg.core.config.CacheNames
import com.imstargg.core.domain.brawlstars.brawler.Brawler
import com.imstargg.core.domain.brawlstars.brawler.Brawlers
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Repository

@Repository
class BrawlerRepositoryWithCache(
    private val brawlerRepository: BrawlerRepository,
) {
    @Cacheable(cacheNames = [CacheNames.BRAWLER], key = "'v1:brawlers'")
    fun findBrawlers(): Brawlers =
        Brawlers(
            brawlerRepository.findAll(),
        )

    @Cacheable(cacheNames = [CacheNames.BRAWLER], key = "'v1:brawlerList'")
    fun findAllBrawlers(): List<Brawler> =
        brawlerRepository
            .findAll()
            .sortedBy { it.id.value }
}
