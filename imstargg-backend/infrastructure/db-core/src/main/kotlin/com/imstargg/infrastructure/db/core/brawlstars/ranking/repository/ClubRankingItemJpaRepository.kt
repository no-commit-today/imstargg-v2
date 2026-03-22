package com.imstargg.infrastructure.db.core.brawlstars.ranking.repository

import com.imstargg.infrastructure.db.core.brawlstars.ranking.ClubRankingItemEntity
import org.springframework.data.jpa.repository.JpaRepository

interface ClubRankingItemJpaRepository : JpaRepository<ClubRankingItemEntity, Long> {
    fun findByClubRankingId(clubRankingId: Long): List<ClubRankingItemEntity>
}
