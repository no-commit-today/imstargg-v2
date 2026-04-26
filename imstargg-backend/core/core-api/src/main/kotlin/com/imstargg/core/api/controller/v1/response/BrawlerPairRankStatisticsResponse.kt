package com.imstargg.core.api.controller.v1.response

data class BrawlerPairRankStatisticsResponse(
    val brawler: BrawlerResponse?,
    val pairBrawler: BrawlerResponse?,
    val totalBattleCount: Long,
    val averageRank: Double,
)
