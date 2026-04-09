package com.imstargg.core.admin.controller.request

data class BattleEventCreateRequest(
    val brawlStarsId: Long,
    val modeId: Long?,
    val mapName: String?,
)
