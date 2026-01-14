package com.imstargg.storage.db.core.brawlstars.ranking

import com.imstargg.core.enums.Country
import org.springframework.data.jpa.repository.JpaRepository

interface PlayerRankingJpaRepository : JpaRepository<PlayerRankingEntity, Long> {
    fun findAllByCountry(country: Country): List<PlayerRankingEntity>
}
