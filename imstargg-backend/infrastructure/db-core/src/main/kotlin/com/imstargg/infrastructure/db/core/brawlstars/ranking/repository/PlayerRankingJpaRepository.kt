package com.imstargg.infrastructure.db.core.brawlstars.ranking.repository

import com.imstargg.core.enums.Country
import com.imstargg.infrastructure.db.core.brawlstars.ranking.PlayerRankingEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant

interface PlayerRankingJpaRepository : JpaRepository<PlayerRankingEntity, Long> {
    fun findTopByCountryOrderByCollectedAtDesc(country: Country): PlayerRankingEntity?

    fun findTopByCountryAndCollectedAtLessThanOrderByCollectedAtDesc(
        country: Country,
        collectedAt: Instant,
    ): PlayerRankingEntity?

    fun findAllByCountryOrderByCollectedAtDesc(country: Country): List<PlayerRankingEntity>
}
