package com.imstargg.infrastructure.db.core.brawlstars.battle.repository

data class PlayerBrawlerResultProjection(
    val brawlerBrawlStarsId: Long,
    val result: String,
    val count: Long,
)
