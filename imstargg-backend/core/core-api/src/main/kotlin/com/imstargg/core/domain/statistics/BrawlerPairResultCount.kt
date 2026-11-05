package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId

/**
 * 2인 조합 하나의 기간 합산 승·패·무. 조합은 순서가 없고, 저장 순서대로 `brawler1Id < brawler2Id` 다.
 */
data class BrawlerPairResultCount(
    val brawler1Id: BrawlStarsId,
    val brawler2Id: BrawlStarsId,
    val resultCount: ResultCount,
)
