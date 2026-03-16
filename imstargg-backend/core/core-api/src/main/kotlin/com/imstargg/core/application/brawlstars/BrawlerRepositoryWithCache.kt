package com.imstargg.core.application.brawlstars

import com.imstargg.core.config.CacheNames
import com.imstargg.core.domain.brawlstars.brawler.Brawler
import com.imstargg.core.domain.brawlstars.brawler.Brawlers
import com.imstargg.core.enums.BrawlStarsEntityStatus
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Repository

@Repository
class BrawlerRepositoryWithCache(
    private val brawlerRepository: BrawlerRepository,
) {
    @Cacheable(cacheNames = [CacheNames.BRAWLER], key = "'all:initialized'")
    fun findBrawlers(): Brawlers =
        Brawlers(
            brawlerRepository.findAllByInStatus(BrawlStarsEntityStatus.initializedStatuses()),
        )

    @Cacheable(cacheNames = [CacheNames.BRAWLER], key = "'all:active'")
    fun findActiveBrawlers(): List<Brawler> =
        brawlerRepository
            .findAllByInStatus(listOf(BrawlStarsEntityStatus.ACTIVE))
            .sortedBy { it.id.value }
}
