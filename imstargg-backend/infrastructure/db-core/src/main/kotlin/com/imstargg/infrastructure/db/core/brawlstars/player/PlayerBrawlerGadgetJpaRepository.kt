package com.imstargg.infrastructure.db.core.brawlstars.player

import org.springframework.data.jpa.repository.JpaRepository

interface PlayerBrawlerGadgetJpaRepository : JpaRepository<PlayerBrawlerGadgetEntity, Long> {
    fun findAllByPlayerBrawlerIn(playerBrawlers: List<PlayerBrawlerEntity>): List<PlayerBrawlerGadgetEntity>
}
