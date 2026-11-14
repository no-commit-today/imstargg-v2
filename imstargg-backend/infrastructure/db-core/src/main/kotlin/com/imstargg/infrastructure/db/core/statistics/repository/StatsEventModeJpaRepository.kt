package com.imstargg.infrastructure.db.core.statistics.repository

import com.imstargg.infrastructure.db.core.statistics.StatsEventModeEntity
import org.springframework.data.jpa.repository.JpaRepository

interface StatsEventModeJpaRepository : JpaRepository<StatsEventModeEntity, Long> {
    fun findByEventModeBrawlStarsId(eventModeBrawlStarsId: Long): StatsEventModeEntity?
}
