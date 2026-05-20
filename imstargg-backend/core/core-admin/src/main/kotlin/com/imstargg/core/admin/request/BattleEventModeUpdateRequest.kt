package com.imstargg.core.admin.request

import com.imstargg.core.enums.BattleEventModeResultType

data class BattleEventModeUpdateRequest(
    val resultType: BattleEventModeResultType,
    val playerCounts: List<Int>,
)
