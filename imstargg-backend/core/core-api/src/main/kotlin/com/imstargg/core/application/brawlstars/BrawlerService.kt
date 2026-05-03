package com.imstargg.core.application.brawlstars

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.brawlstars.brawler.Brawler
import com.imstargg.core.domain.brawlstars.brawler.BrawlerDetail
import com.imstargg.core.domain.brawlstars.brawler.BrawlerItems
import com.imstargg.core.error.CoreException
import com.imstargg.core.error.ErrorType
import org.springframework.stereotype.Service

@Service
class BrawlerService(
    private val brawlerRepositoryWithCache: BrawlerRepositoryWithCache,
) {
    fun get(id: BrawlStarsId): Brawler =
        brawlerRepositoryWithCache.findAllBrawlers().find { it.id == id }
            ?: throw CoreException(ErrorType.NOT_FOUND, "id=$id")

    fun getAll(): List<Brawler> = brawlerRepositoryWithCache.findAllBrawlers()

    fun getDetail(id: BrawlStarsId): BrawlerDetail {
        val brawler = get(id)
        val items =
            brawlerRepositoryWithCache.findAllBrawlerItems()[id]
                ?: BrawlerItems(
                    gadgets = emptyList(),
                    gears = emptyList(),
                    starPowers = emptyList(),
                    hyperCharge = null,
                    buffies = null,
                )
        return BrawlerDetail(brawler, items)
    }

    fun search(name: String): List<Brawler> {
        val brawlers = brawlerRepositoryWithCache.findAllBrawlers()
        if (name.isBlank()) return brawlers
        return brawlers.filter { it.name.containsIgnoreCase(name) }
    }
}
