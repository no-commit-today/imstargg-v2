package com.imstargg.infrastructure.db.core.brawlstars.ranking.repository

import com.imstargg.core.enums.Country
import com.imstargg.infrastructure.db.core.brawlstars.ranking.PlayerRankingEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.time.OffsetDateTime

interface PlayerRankingJpaRepository : JpaRepository<PlayerRankingEntity, Long> {
    fun findTopByCountryOrderByCollectedAtDesc(country: Country): PlayerRankingEntity?

    fun findTopByCountryAndCollectedAtLessThanOrderByCollectedAtDesc(
        country: Country,
        collectedAt: OffsetDateTime,
    ): PlayerRankingEntity?

    fun findAllByCollectedAtGreaterThanEqualAndCollectedAtLessThan(
        from: OffsetDateTime,
        to: OffsetDateTime,
    ): List<PlayerRankingEntity>

    fun findAllByCountryAndCollectedAtGreaterThanEqualAndCollectedAtLessThan(
        country: Country,
        from: OffsetDateTime,
        to: OffsetDateTime,
    ): List<PlayerRankingEntity>
}
