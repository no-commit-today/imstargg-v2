package com.imstargg.infrastructure.db.core.statistics.player

import org.springframework.data.jpa.repository.JpaRepository

interface PlayerBrawlerStatsItemJpaRepository : JpaRepository<PlayerBrawlerStatsItemEntity, Long> {
    fun findAllByPlayerBrawlerStatsId(playerBrawlerStatsId: Long): List<PlayerBrawlerStatsItemEntity>

    fun findByPlayerBrawlerStatsIdAndBrawlerBrawlStarsId(
        playerBrawlerStatsId: Long,
        brawlerBrawlStarsId: Long,
    ): PlayerBrawlerStatsItemEntity?
}
