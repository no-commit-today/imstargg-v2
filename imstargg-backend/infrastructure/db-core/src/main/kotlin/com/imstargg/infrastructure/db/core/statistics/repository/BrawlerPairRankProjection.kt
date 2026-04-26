package com.imstargg.infrastructure.db.core.statistics.repository

data class BrawlerPairRankProjection(
    val brawlerBrawlStarsId: Long,
    val pairBrawlerBrawlStarsId: Long,
    val totalCount: Long,
    val weightedRankSum: Long,
)
