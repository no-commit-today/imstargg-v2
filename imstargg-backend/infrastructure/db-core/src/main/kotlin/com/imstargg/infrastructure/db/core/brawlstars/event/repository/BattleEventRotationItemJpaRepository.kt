package com.imstargg.infrastructure.db.core.brawlstars.event.repository

import com.imstargg.infrastructure.db.core.brawlstars.event.BattleEventRotationItemEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.time.OffsetDateTime

interface BattleEventRotationItemJpaRepository : JpaRepository<BattleEventRotationItemEntity, Long> {
    fun findAllByEndTimeAfter(endTime: OffsetDateTime): List<BattleEventRotationItemEntity>
}
