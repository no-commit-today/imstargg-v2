package com.imstargg.infrastructure.db.core.brawlstars.ranking.repository

import com.imstargg.infrastructure.db.core.brawlstars.ranking.PlayerDailyRankingEntity
import org.springframework.data.jpa.repository.JpaRepository

interface PlayerDailyRankingJpaRepository : JpaRepository<PlayerDailyRankingEntity, Long>
