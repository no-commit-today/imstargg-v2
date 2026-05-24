package com.imstargg.core.admin.request

import com.imstargg.core.enums.BattleEventModeResultType

data class BattleEventModeCreateRequest(
    val brawlStarsId: Long,
    val brawlStarsName: String,
    val resultType: BattleEventModeResultType = BattleEventModeResultType.RESULT,
)
