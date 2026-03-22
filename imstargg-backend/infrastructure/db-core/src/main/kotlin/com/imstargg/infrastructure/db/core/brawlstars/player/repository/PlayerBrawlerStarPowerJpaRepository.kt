package com.imstargg.infrastructure.db.core.brawlstars.player.repository

import com.imstargg.infrastructure.db.core.brawlstars.player.PlayerBrawlerEntity
import com.imstargg.infrastructure.db.core.brawlstars.player.PlayerBrawlerStarPowerEntity
import org.springframework.data.jpa.repository.JpaRepository

interface PlayerBrawlerStarPowerJpaRepository : JpaRepository<PlayerBrawlerStarPowerEntity, Long> {
    fun findAllByPlayerBrawlerIn(playerBrawlers: List<PlayerBrawlerEntity>): List<PlayerBrawlerStarPowerEntity>
}
