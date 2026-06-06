package com.imstargg.infrastructure.db.core.brawlstars.ranking.repository

import com.imstargg.infrastructure.db.core.brawlstars.ranking.ClubRankingEntity
import com.imstargg.infrastructure.db.core.brawlstars.ranking.ClubRankingItemEntity
import org.springframework.data.jpa.repository.JpaRepository

interface ClubRankingItemJpaRepository : JpaRepository<ClubRankingItemEntity, Long> {
    fun findAllByClubRanking(clubRanking: ClubRankingEntity): List<ClubRankingItemEntity>

    fun findAllByClubRankingIn(clubRankings: Collection<ClubRankingEntity>): List<ClubRankingItemEntity>
}
