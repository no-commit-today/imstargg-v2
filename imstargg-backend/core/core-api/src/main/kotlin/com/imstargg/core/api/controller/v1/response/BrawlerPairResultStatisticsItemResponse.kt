package com.imstargg.core.api.controller.v1.response

data class BrawlerPairResultStatisticsItemResponse(
    val brawler: BrawlerResponse?,
    val pairBrawler: BrawlerResponse?,
    val battleCount: Long,
    val pickRate: Double,
    val winRate: Double,
) {
    /** 구버전 프론트 호환용 (= [battleCount]). 프론트의 battleCount 전환 배포 후 제거 예정. */
    val totalBattleCount: Long
        get() = battleCount
}
