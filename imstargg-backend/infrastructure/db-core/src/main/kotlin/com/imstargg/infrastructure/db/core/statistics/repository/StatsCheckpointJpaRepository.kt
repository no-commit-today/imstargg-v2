package com.imstargg.infrastructure.db.core.statistics.repository

import com.imstargg.core.enums.StatsType
import com.imstargg.infrastructure.db.core.statistics.StatsCheckpointEntity
import org.springframework.data.jpa.repository.JpaRepository

interface StatsCheckpointJpaRepository : JpaRepository<StatsCheckpointEntity, Long> {
    fun findAllByStatsType(statsType: StatsType): List<StatsCheckpointEntity>
}
