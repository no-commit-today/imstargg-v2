package com.imstargg.infrastructure.db.core.brawlstars.ranking.repository

import com.imstargg.infrastructure.db.core.brawlstars.ranking.BrawlerRankingEntity
import com.imstargg.infrastructure.db.core.brawlstars.ranking.BrawlerRankingItemEntity
import org.springframework.data.jpa.repository.JpaRepository

interface BrawlerRankingItemJpaRepository : JpaRepository<BrawlerRankingItemEntity, Long> {
    fun findAllByBrawlerRanking(brawlerRanking: BrawlerRankingEntity): List<BrawlerRankingItemEntity>

    fun findAllByBrawlerRankingIn(brawlerRankings: Collection<BrawlerRankingEntity>): List<BrawlerRankingItemEntity>
}
