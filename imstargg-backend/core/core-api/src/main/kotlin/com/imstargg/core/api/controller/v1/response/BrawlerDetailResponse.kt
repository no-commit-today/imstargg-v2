package com.imstargg.core.api.controller.v1.response

import com.imstargg.core.domain.brawlstars.brawler.BrawlerDetail

data class BrawlerDetailResponse(
    val brawler: BrawlerResponse,
    val items: BrawlerItemsResponse,
) {
    companion object {
        fun of(detail: BrawlerDetail) =
            BrawlerDetailResponse(
                brawler = BrawlerResponse.of(detail.brawler),
                items = BrawlerItemsResponse.of(detail.items),
            )
    }
}
