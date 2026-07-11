package com.imstargg.core.domain.brawlstars.brawler

import com.imstargg.core.domain.brawlstars.BrawlStarsId

data class Brawlers(
    private val brawlers: List<Brawler>,
) {
    private val brawlersById: Map<BrawlStarsId, Brawler> = brawlers.associateBy { it.id }

    fun find(id: BrawlStarsId): Brawler? = brawlersById[id]
}
