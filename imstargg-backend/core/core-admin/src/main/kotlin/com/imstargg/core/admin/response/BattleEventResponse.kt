package com.imstargg.core.admin.response

import com.imstargg.core.enums.Language
import java.time.Instant

data class BattleEventResponse(
    val brawlStarsId: Long,
    val modeBrawlStarsName: String?,
    val mapName: String?,
    val mapNames: Map<Language, String>,
    val createdAt: Instant?,
)
