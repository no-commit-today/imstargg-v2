package com.imstargg.infrastructure.db.core.brawlstars.ranking.repository

import com.imstargg.infrastructure.db.core.brawlstars.ranking.ClubDailyRankingItemEntity
import org.springframework.data.jpa.repository.JpaRepository

interface ClubDailyRankingItemJpaRepository : JpaRepository<ClubDailyRankingItemEntity, Long>
