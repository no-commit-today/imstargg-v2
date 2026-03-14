package com.imstargg.infrastructure.db.core.statistics

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface StatsPlayerJpaRepository : JpaRepository<StatsPlayerEntity, Long> {
    fun findAllByBrawlStarsTagIn(brawlStarsTags: Collection<String>): List<StatsPlayerEntity>

    fun findTopByOrderByIdDesc(): StatsPlayerEntity?

    fun findAllByIdGreaterThanEqual(
        id: Long,
        pageable: Pageable,
    ): List<StatsPlayerEntity>
}
