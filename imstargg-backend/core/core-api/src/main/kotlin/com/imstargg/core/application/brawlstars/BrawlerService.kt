package com.imstargg.core.application.brawlstars

import com.imstargg.core.domain.brawlstars.brawler.Brawler
import org.springframework.stereotype.Service

@Service
class BrawlerService(
    private val brawlerRepositoryWithCache: BrawlerRepositoryWithCache,
) {
    fun getAll(): List<Brawler> = brawlerRepositoryWithCache.findAllBrawlers()

    fun search(name: String): List<Brawler> {
        val brawlers = brawlerRepositoryWithCache.findAllBrawlers()
        if (name.isBlank()) return brawlers
        return brawlers.filter { it.name.containsIgnoreCase(name) }
    }
}
