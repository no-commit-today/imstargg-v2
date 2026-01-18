package com.imstargg.infrastructure.db.core.brawlstars.ranking

import org.springframework.data.jpa.repository.JpaRepository

interface PlayerRankingItemJpaRepository : JpaRepository<PlayerRankingItemEntity, Long> {
    fun findByPlayerRankingId(playerRankingId: Long): List<PlayerRankingItemEntity>
}
