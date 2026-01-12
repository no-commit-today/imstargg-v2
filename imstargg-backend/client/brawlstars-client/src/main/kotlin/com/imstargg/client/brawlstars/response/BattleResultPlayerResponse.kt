package com.imstargg.client.brawlstars.response

data class BattleResultPlayerResponse(
    val tag: String,
    val name: String,
    val brawler: BattleResultBrawlerResponse?,
    val brawlers: List<BattleResultBrawlerResponse>?,
)
