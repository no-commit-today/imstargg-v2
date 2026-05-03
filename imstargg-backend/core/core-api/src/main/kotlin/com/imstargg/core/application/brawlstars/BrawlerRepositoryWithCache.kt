package com.imstargg.core.application.brawlstars

import com.imstargg.core.config.CacheNames
import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.brawlstars.brawler.Brawler
import com.imstargg.core.domain.brawlstars.brawler.BrawlerItems
import com.imstargg.core.domain.brawlstars.brawler.Brawlers
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Repository

@Repository
class BrawlerRepositoryWithCache(
    private val brawlerRepository: BrawlerRepository,
) {
    @Cacheable(cacheNames = [CacheNames.BRAWLER], key = "'brawlers'")
    fun findBrawlers(): Brawlers = Brawlers(findAllBrawlers())

    @Cacheable(cacheNames = [CacheNames.BRAWLER], key = "'brawlerList'")
    fun findAllBrawlers(): List<Brawler> =
        brawlerRepository
            .findAll()
            .sortedBy { it.id.value }

    @Cacheable(cacheNames = [CacheNames.BRAWLER], key = "'brawlerItems'")
    fun findAllBrawlerItems(): Map<BrawlStarsId, BrawlerItems> = brawlerRepository.findAllBrawlerItems()
}
