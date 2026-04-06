package com.imstargg.infrastructure.db.core.brawlstars.ranking.repository

import com.imstargg.infrastructure.db.core.brawlstars.ranking.ClubDailyRankingEntity
import org.springframework.data.jpa.repository.JpaRepository

interface ClubDailyRankingJpaRepository : JpaRepository<ClubDailyRankingEntity, Long>
