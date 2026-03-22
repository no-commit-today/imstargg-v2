package com.imstargg.infrastructure.db.core.brawlstars.player.repository

import com.imstargg.infrastructure.db.core.brawlstars.player.PlayerBrawlerEntity
import com.imstargg.infrastructure.db.core.brawlstars.player.PlayerEntity
import org.springframework.data.jpa.repository.JpaRepository

interface PlayerBrawlerJpaRepository : JpaRepository<PlayerBrawlerEntity, Long> {
    fun findAllByPlayer(player: PlayerEntity): List<PlayerBrawlerEntity>
}
