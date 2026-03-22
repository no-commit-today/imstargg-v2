package com.imstargg.infrastructure.db.core.brawlstars.player.repository

import com.imstargg.infrastructure.db.core.brawlstars.player.PlayerBrawlerEntity
import com.imstargg.infrastructure.db.core.brawlstars.player.PlayerBrawlerGadgetEntity
import org.springframework.data.jpa.repository.JpaRepository

interface PlayerBrawlerGadgetJpaRepository : JpaRepository<PlayerBrawlerGadgetEntity, Long> {
    fun findAllByPlayerBrawlerIn(playerBrawlers: List<PlayerBrawlerEntity>): List<PlayerBrawlerGadgetEntity>
}
