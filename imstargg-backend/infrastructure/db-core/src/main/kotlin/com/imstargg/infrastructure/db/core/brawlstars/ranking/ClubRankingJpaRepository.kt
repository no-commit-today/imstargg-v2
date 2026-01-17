package com.imstargg.infrastructure.db.core.brawlstars.ranking

import com.imstargg.core.enums.Country
import org.springframework.data.jpa.repository.JpaRepository

interface ClubRankingJpaRepository : JpaRepository<ClubRankingEntity, Long> {
    fun findAllByCountry(country: Country): List<ClubRankingEntity>
}
