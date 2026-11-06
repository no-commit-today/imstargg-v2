package com.imstargg.core.api.controller.v1.response

data class BrawlerTrioResultStatisticsItemResponse(
    val brawler: BrawlerResponse?,
    val pairBrawlers: List<BrawlerResponse?>,
    val battleCount: Long,
    val pickRate: Double,
    val winRate: Double,
    val tierScore: Int,
    val tier: String,
)
