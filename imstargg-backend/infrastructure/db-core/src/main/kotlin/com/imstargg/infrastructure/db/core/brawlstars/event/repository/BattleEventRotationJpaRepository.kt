package com.imstargg.infrastructure.db.core.brawlstars.event.repository

import com.imstargg.infrastructure.db.core.brawlstars.event.BattleEventRotationEntity
import org.springframework.data.jpa.repository.JpaRepository

interface BattleEventRotationJpaRepository : JpaRepository<BattleEventRotationEntity, Long> {
    fun findTopByOrderByCollectedAtDesc(): BattleEventRotationEntity?
}
