package com.imstargg.infrastructure.db.core.brawlstars.ranking.repository

import com.imstargg.core.enums.Country
import com.imstargg.infrastructure.db.core.brawlstars.ranking.BrawlerRankingEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
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

    fun findAllByCountryAndBrawlerBrawlStarsIdOrderByCollectedAtDesc(
        country: Country,
        brawlerBrawlStarsId: Long,
    ): List<BrawlerRankingEntity>

    @Query(
        """
        select br from BrawlerRankingEntity br
        where br.collectedAt = (
            select max(br2.collectedAt) from BrawlerRankingEntity br2
            where br2.country = br.country
              and br2.brawlerBrawlStarsId = br.brawlerBrawlStarsId
        )
        """,
    )
    fun findAllLatestPerCountryAndBrawler(): List<BrawlerRankingEntity>
}
