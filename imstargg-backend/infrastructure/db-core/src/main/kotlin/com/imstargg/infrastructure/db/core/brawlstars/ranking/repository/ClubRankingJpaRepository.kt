package com.imstargg.infrastructure.db.core.brawlstars.ranking.repository

import com.imstargg.core.enums.Country
import com.imstargg.infrastructure.db.core.brawlstars.ranking.ClubRankingEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.time.OffsetDateTime

interface ClubRankingJpaRepository : JpaRepository<ClubRankingEntity, Long> {
    fun findTopByCountryOrderByCollectedAtDesc(country: Country): ClubRankingEntity?

    fun findTopByCountryAndCollectedAtLessThanOrderByCollectedAtDesc(
        country: Country,
        collectedAt: OffsetDateTime,
    ): ClubRankingEntity?

    fun findAllByCollectedAtGreaterThanEqualAndCollectedAtLessThan(
        from: OffsetDateTime,
        to: OffsetDateTime,
    ): List<ClubRankingEntity>

    fun findAllByCountryAndCollectedAtGreaterThanEqualAndCollectedAtLessThan(
        country: Country,
        from: OffsetDateTime,
        to: OffsetDateTime,
    ): List<ClubRankingEntity>
}
