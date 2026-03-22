package com.imstargg.infrastructure.db.core.brawlstars.ranking.repository

import com.imstargg.infrastructure.db.core.brawlstars.ranking.PlayerRankingItemEntity
import org.springframework.data.jpa.repository.JpaRepository

interface PlayerRankingItemJpaRepository : JpaRepository<PlayerRankingItemEntity, Long> {
    fun findByPlayerRankingId(playerRankingId: Long): List<PlayerRankingItemEntity>
}
