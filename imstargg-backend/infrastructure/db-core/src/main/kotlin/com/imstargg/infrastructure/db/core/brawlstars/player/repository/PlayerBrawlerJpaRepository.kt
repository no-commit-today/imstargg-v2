package com.imstargg.infrastructure.db.core.brawlstars.player

import org.springframework.data.jpa.repository.JpaRepository

interface PlayerBrawlerJpaRepository : JpaRepository<PlayerBrawlerEntity, Long> {
    fun findAllByPlayer(player: PlayerEntity): List<PlayerBrawlerEntity>
}
