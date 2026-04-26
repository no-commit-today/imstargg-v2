package com.imstargg.core.admin.request

import com.imstargg.core.enums.BattleEventResultType

data class BattleEventModeCreateRequest(
    val brawlStarsId: Long,
    val brawlStarsName: String,
    val resultType: BattleEventResultType = BattleEventResultType.RESULT,
)
