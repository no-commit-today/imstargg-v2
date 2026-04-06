package com.imstargg.infrastructure.db.core.brawlstars.player.repository

import com.imstargg.infrastructure.db.core.brawlstars.player.PlayerBrawlerBuffyEntity
import com.imstargg.infrastructure.db.core.brawlstars.player.PlayerBrawlerEntity
import org.springframework.data.jpa.repository.JpaRepository

interface PlayerBrawlerBuffyJpaRepository : JpaRepository<PlayerBrawlerBuffyEntity, Long> {
    fun findAllByPlayerBrawlerIn(playerBrawlers: List<PlayerBrawlerEntity>): List<PlayerBrawlerBuffyEntity>

    fun deleteAllByPlayerBrawler(playerBrawler: PlayerBrawlerEntity)
}
