package com.imstargg.infrastructure.db.core.statistics.repository

import com.imstargg.infrastructure.db.core.statistics.BrawlerTeamEmbedded

data class BrawlerTeamRankProjection(
    val brawlerTeam: BrawlerTeamEmbedded,
    val totalCount: Long,
    val weightedRankSum: Long,
)
