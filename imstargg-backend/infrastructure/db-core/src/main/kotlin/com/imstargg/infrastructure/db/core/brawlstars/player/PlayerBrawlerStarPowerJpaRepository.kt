package com.imstargg.infrastructure.db.core.brawlstars.player

import org.springframework.data.jpa.repository.JpaRepository

interface PlayerBrawlerStarPowerJpaRepository : JpaRepository<PlayerBrawlerStarPowerEntity, Long> {
    fun findAllByPlayerBrawlerIn(playerBrawlers: List<PlayerBrawlerEntity>): List<PlayerBrawlerStarPowerEntity>
}
