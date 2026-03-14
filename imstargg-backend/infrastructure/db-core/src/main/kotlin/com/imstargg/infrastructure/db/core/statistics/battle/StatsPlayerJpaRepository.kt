package com.imstargg.infrastructure.db.core.statistics.battle

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface StatsPlayerJpaRepository : JpaRepository<StatsPlayerEntity, Long> {
    fun deleteByBrawlStarsTag(brawlStarsTag: String)

    fun findAllByBrawlStarsTagIn(brawlStarsTags: Collection<String>): List<StatsPlayerEntity>

    fun findTopByOrderByIdDesc(): StatsPlayerEntity?

    fun findAllByIdGreaterThanEqual(
        id: Long,
        pageable: Pageable,
    ): List<StatsPlayerEntity>
}
