package com.imstargg.storage.db.core.brawlstars.player

import org.springframework.data.jpa.repository.JpaRepository

interface PlayerBrawlerGearJpaRepository : JpaRepository<PlayerBrawlerGearEntity, Long> {
    fun findAllByPlayerBrawlerIn(playerBrawlers: List<PlayerBrawlerEntity>): List<PlayerBrawlerGearEntity>
}
