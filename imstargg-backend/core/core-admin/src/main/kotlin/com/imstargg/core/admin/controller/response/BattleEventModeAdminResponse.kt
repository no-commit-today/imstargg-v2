package com.imstargg.core.admin.controller.response

import com.imstargg.core.admin.response.BattleEventModeResponse
import com.imstargg.core.enums.Language

data class BattleEventModeAdminResponse(
    val brawlStarsId: Long,
    val brawlStarsName: String,
    val names: Map<Language, String>,
    val hasIcon: Boolean,
) {
    companion object {
        fun from(response: BattleEventModeResponse) =
            BattleEventModeAdminResponse(
                brawlStarsId = response.mode.brawlStarsId,
                brawlStarsName = response.mode.brawlStarsName,
                names = response.names,
                hasIcon = response.icon != null,
            )
    }
}
