package com.imstargg.infrastructure.db.core.brawlstars.ranking

import com.imstargg.core.enums.Country
import org.springframework.data.jpa.repository.JpaRepository
import java.time.OffsetDateTime

interface PlayerRankingJpaRepository : JpaRepository<PlayerRankingEntity, Long> {
    fun findTopByCountryOrderByCollectedAtDesc(country: Country): PlayerRankingEntity?

    fun findTopByCountryAndCollectedAtLessThanOrderByCollectedAtDesc(
        country: Country,
        collectedAt: OffsetDateTime,
    ): PlayerRankingEntity?
}
