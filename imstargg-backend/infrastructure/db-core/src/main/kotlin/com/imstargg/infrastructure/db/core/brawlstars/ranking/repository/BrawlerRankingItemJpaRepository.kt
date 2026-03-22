package com.imstargg.infrastructure.db.core.brawlstars.ranking

import org.springframework.data.jpa.repository.JpaRepository

interface BrawlerRankingItemJpaRepository : JpaRepository<BrawlerRankingItemEntity, Long> {
    fun findByBrawlerRankingId(brawlerRankingId: Long): List<BrawlerRankingItemEntity>
}
