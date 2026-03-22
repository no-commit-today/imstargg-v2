package com.imstargg.infrastructure.db.core.brawlstars.ranking

import com.imstargg.core.enums.Country
import org.springframework.data.jpa.repository.JpaRepository
import java.time.OffsetDateTime

interface BrawlerRankingJpaRepository : JpaRepository<BrawlerRankingEntity, Long> {
    fun findTopByCountryAndBrawlerBrawlStarsIdOrderByCollectedAtDesc(
        country: Country,
        brawlerBrawlStarsId: Long,
    ): BrawlerRankingEntity?

    fun findTopByCountryAndBrawlerBrawlStarsIdAndCollectedAtLessThanOrderByCollectedAtDesc(
        country: Country,
        brawlerBrawlStarsId: Long,
        collectedAt: OffsetDateTime,
    ): BrawlerRankingEntity?
}
