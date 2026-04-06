package com.imstargg.infrastructure.db.core.brawlstars.ranking.repository

import com.imstargg.infrastructure.db.core.brawlstars.ranking.PlayerDailyRankingItemEntity
import org.springframework.data.jpa.repository.JpaRepository

interface PlayerDailyRankingItemJpaRepository : JpaRepository<PlayerDailyRankingItemEntity, Long>
