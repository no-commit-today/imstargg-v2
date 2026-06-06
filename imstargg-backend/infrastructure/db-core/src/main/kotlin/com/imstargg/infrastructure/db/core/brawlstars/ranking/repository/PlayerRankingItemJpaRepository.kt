package com.imstargg.infrastructure.db.core.brawlstars.ranking.repository

import com.imstargg.infrastructure.db.core.brawlstars.ranking.PlayerRankingEntity
import com.imstargg.infrastructure.db.core.brawlstars.ranking.PlayerRankingItemEntity
import org.springframework.data.jpa.repository.JpaRepository

interface PlayerRankingItemJpaRepository : JpaRepository<PlayerRankingItemEntity, Long> {
    fun findAllByPlayerRanking(playerRanking: PlayerRankingEntity): List<PlayerRankingItemEntity>

    fun findAllByPlayerRankingIn(playerRankings: Collection<PlayerRankingEntity>): List<PlayerRankingItemEntity>
}
