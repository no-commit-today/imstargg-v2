package com.imstargg.infrastructure.db.core.brawlstars.event

import org.springframework.data.jpa.repository.JpaRepository

interface BattleEventJpaRepository : JpaRepository<BattleEventEntity, Long> {
    fun findByBrawlStarsId(brawlStarsId: Long): BattleEventEntity?
}
