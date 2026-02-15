package com.imstargg.infrastructure.db.core.statistics

import org.springframework.data.jpa.repository.JpaRepository

interface StatsPlayerJpaRepository : JpaRepository<StatsPlayerEntity, Long> {
    fun findAllByBrawlStarsTagIn(brawlStarsTags: Collection<String>): List<StatsPlayerEntity>
}
