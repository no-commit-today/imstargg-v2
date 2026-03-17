package com.imstargg.core.application.brawlstars

import com.imstargg.core.domain.brawlstars.brawler.Brawler
import org.springframework.stereotype.Service

@Service
class BrawlerService(
    private val brawlerRepositoryWithCache: BrawlerRepositoryWithCache,
) {
    fun getAll(): List<Brawler> = brawlerRepositoryWithCache.findAllBrawlers()
}
