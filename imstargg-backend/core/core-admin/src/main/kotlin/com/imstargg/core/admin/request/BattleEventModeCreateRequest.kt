package com.imstargg.core.admin.request

import com.imstargg.core.enums.BattleEventModeResultType
import com.imstargg.core.enums.Language

data class BattleEventModeCreateRequest(
    val brawlStarsId: Long,
    val brawlStarsName: String,
    val resultType: BattleEventModeResultType = BattleEventModeResultType.RESULT,
    val names: Map<Language, String> = emptyMap(),
    // 아래 기본값은 BattleEventModeEntity 의 DEFAULT_PLAYER_COUNTS / DEFAULT_COLOR 와 일치
    val playerCounts: List<Int> = listOf(3, 3),
    val color: String = "#2A2E38",
)
