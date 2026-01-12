package com.imstargg.client.brawlstars.response

data class BattleResultResponse(
    val mode: String?,
    val type: String?,
    val result: String?,
    val duration: Int?,
    val rank: Int?,
    val trophyChange: Int?,
    val starPlayer: BattleResultPlayerResponse?,
    val teams: List<List<BattleResultPlayerResponse>>?,
    val players: List<BattleResultPlayerResponse>?,
)
