package com.imstargg.core.api.controller.v1.response

import com.imstargg.core.domain.brawlstars.brawler.BrawlerWithItems

data class BrawlerWithItemsResponse(
    val brawler: BrawlerResponse,
    val items: BrawlerItemsResponse,
) {
    companion object {
        fun of(brawlerWithItems: BrawlerWithItems) =
            BrawlerWithItemsResponse(
                brawler = BrawlerResponse.of(brawlerWithItems.brawler),
                items = BrawlerItemsResponse.of(brawlerWithItems.items),
            )
    }
}
