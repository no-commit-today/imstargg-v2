package com.imstargg.infrastructure.db.core.statistics.battle.repository

import com.imstargg.core.enums.StatsPlayerStatus
import com.imstargg.infrastructure.db.core.statistics.battle.StatsPlayerEntity
import org.springframework.data.jpa.repository.JpaRepository

interface StatsPlayerJpaRepository : JpaRepository<StatsPlayerEntity, Long> {
    fun findAllByBrawlStarsTagIn(brawlStarsTags: Collection<String>): List<StatsPlayerEntity>

    fun findTopByOrderByIdDesc(): StatsPlayerEntity?

    fun findFirstByIdGreaterThanEqualAndStatusOrderByIdAsc(
        id: Long,
        status: StatsPlayerStatus,
    ): StatsPlayerEntity?
}
