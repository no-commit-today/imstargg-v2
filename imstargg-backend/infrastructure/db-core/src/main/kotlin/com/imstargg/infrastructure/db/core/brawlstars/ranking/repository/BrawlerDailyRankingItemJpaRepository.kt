package com.imstargg.infrastructure.db.core.brawlstars.ranking.repository

import com.imstargg.infrastructure.db.core.brawlstars.ranking.BrawlerDailyRankingItemEntity
import org.springframework.data.jpa.repository.JpaRepository

interface BrawlerDailyRankingItemJpaRepository : JpaRepository<BrawlerDailyRankingItemEntity, Long>
