package com.imstargg.infrastructure.db.core.statistics.repository

import com.imstargg.infrastructure.db.core.statistics.BrawlerTeamEmbedded

data class BrawlerTeamResultProjection(
    val brawlerTeam: BrawlerTeamEmbedded,
    val victoryCount: Long,
    val defeatCount: Long,
    val drawCount: Long,
)
