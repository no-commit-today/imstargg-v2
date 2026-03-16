package com.imstargg.core.domain.brawlstars.brawler

import com.imstargg.core.domain.brawlstars.BrawlStarsId

data class Brawlers(
    private val brawlers: List<Brawler>,
) {
    fun find(id: BrawlStarsId): Brawler? = brawlers.find { it.id == id }
}
