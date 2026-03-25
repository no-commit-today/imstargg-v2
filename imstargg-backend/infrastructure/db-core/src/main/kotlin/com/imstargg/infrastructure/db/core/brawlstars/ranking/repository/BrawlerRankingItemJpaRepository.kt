package com.imstargg.infrastructure.db.core.brawlstars.ranking.repository

import com.imstargg.infrastructure.db.core.brawlstars.ranking.BrawlerRankingItemEntity
import org.springframework.data.jpa.repository.JpaRepository

interface BrawlerRankingItemJpaRepository : JpaRepository<BrawlerRankingItemEntity, Long> {
    fun findAllByBrawlerRankingId(brawlerRankingId: Long): List<BrawlerRankingItemEntity>
}
