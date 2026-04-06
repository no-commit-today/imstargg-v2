package com.imstargg.infrastructure.db.core.brawlstars.player.repository

import com.imstargg.infrastructure.db.core.brawlstars.player.PlayerBrawlerEntity
import com.imstargg.infrastructure.db.core.brawlstars.player.PlayerBrawlerHyperChargeEntity
import org.springframework.data.jpa.repository.JpaRepository

interface PlayerBrawlerHyperChargeJpaRepository : JpaRepository<PlayerBrawlerHyperChargeEntity, Long> {
    fun findAllByPlayerBrawlerIn(playerBrawlers: List<PlayerBrawlerEntity>): List<PlayerBrawlerHyperChargeEntity>
}
