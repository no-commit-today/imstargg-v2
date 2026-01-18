package com.imstargg.infrastructure.db.core.brawlstars.ranking

import org.springframework.data.jpa.repository.JpaRepository

interface ClubRankingItemJpaRepository : JpaRepository<ClubRankingItemEntity, Long> {
    fun findByClubRankingId(clubRankingId: Long): List<ClubRankingItemEntity>
}
