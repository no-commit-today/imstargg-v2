package com.imstargg.core.admin.controller.response

import com.imstargg.core.admin.response.BattleEventResponse
import com.imstargg.core.enums.Language

data class BattleEventAdminResponse(
    val brawlStarsId: Long,
    val modeBrawlStarsName: String?,
    val mapName: String?,
    val mapNames: Map<Language, String>,
) {
    companion object {
        fun from(response: BattleEventResponse) =
            BattleEventAdminResponse(
                brawlStarsId = response.event.brawlStarsId,
                modeBrawlStarsName = response.event.mode?.brawlStarsName,
                mapName = response.event.mapName,
                mapNames = response.mapNames,
            )
    }
}
