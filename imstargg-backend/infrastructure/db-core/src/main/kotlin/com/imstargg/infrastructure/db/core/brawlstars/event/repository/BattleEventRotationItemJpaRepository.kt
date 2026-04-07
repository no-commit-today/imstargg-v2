package com.imstargg.infrastructure.db.core.brawlstars.event.repository

import com.imstargg.infrastructure.db.core.brawlstars.event.BattleEventRotationItemEntity
import org.springframework.data.jpa.repository.JpaRepository

interface BattleEventRotationItemJpaRepository : JpaRepository<BattleEventRotationItemEntity, Long> {
    fun findAllByBattleEventRotationId(battleEventRotationId: Long): List<BattleEventRotationItemEntity>
}
