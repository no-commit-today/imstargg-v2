package com.imstargg.infrastructure.db.core.brawlstars.event

import org.springframework.data.jpa.repository.JpaRepository
import java.time.OffsetDateTime

interface BattleEventRotationItemJpaRepository : JpaRepository<BattleEventRotationItemEntity, Long> {
    fun findAllByEndTimeAfter(endTime: OffsetDateTime): List<BattleEventRotationItemEntity>
}
