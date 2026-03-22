package com.imstargg.infrastructure.db.core.brawlstars.player.repository

import com.imstargg.infrastructure.db.core.brawlstars.player.PlayerBrawlerEntity
import com.imstargg.infrastructure.db.core.brawlstars.player.PlayerBrawlerGearEntity
import org.springframework.data.jpa.repository.JpaRepository

interface PlayerBrawlerGearJpaRepository : JpaRepository<PlayerBrawlerGearEntity, Long> {
    fun findAllByPlayerBrawlerIn(playerBrawlers: List<PlayerBrawlerEntity>): List<PlayerBrawlerGearEntity>
}
