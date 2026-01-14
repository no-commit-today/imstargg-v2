package com.imstargg.storage.db.core.brawlstars.ranking

import com.imstargg.core.enums.Country
import org.springframework.data.jpa.repository.JpaRepository

interface BrawlerRankingJpaRepository : JpaRepository<BrawlerRankingEntity, Long> {
    fun findAllByCountryAndBrawlerBrawlStarsId(country: Country, brawlerBrawlStarsId: Long): List<BrawlerRankingEntity>
}
