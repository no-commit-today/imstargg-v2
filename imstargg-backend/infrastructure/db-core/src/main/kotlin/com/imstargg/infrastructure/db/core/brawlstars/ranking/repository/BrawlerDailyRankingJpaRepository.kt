package com.imstargg.infrastructure.db.core.brawlstars.ranking.repository

import com.imstargg.infrastructure.db.core.brawlstars.ranking.BrawlerDailyRankingEntity
import org.springframework.data.jpa.repository.JpaRepository

interface BrawlerDailyRankingJpaRepository : JpaRepository<BrawlerDailyRankingEntity, Long>
