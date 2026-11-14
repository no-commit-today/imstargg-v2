package com.imstargg.core.api.controller.v1.response

data class BrawlerPairResultStatisticsItemResponse(
    val brawler: BrawlerResponse?,
    val pairBrawler: BrawlerResponse?,
    val battleCount: Long,
    val pickRate: Double,
    val winRate: Double,
    val tierScore: Int,
    val tier: String,
)
