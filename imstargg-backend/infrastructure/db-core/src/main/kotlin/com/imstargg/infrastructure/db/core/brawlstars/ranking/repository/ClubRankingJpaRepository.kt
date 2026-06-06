package com.imstargg.infrastructure.db.core.brawlstars.ranking.repository

import com.imstargg.core.enums.Country
import com.imstargg.infrastructure.db.core.brawlstars.ranking.ClubRankingEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant

interface ClubRankingJpaRepository : JpaRepository<ClubRankingEntity, Long> {
    fun findTopByCountryOrderByCollectedAtDesc(country: Country): ClubRankingEntity?

    fun findTopByCountryAndCollectedAtLessThanOrderByCollectedAtDesc(
        country: Country,
        collectedAt: Instant,
    ): ClubRankingEntity?

    fun findAllByCountryOrderByCollectedAtDesc(country: Country): List<ClubRankingEntity>
}
